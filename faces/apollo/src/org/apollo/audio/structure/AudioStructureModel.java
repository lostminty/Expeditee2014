package org.apollo.audio.structure;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;

import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.io.AudioIO;
import org.apollo.io.AudioPathManager;
import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.ExpediteeFileTextSearch;
import org.apollo.util.Mutable;
import org.apollo.util.TextItemSearchResult;
import org.apollo.widgets.LinkedTrack;
import org.apollo.widgets.SampledTrack;
import org.apollo.widgets.TrackWidgetCommons;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.settings.UserSettings;
import org.expeditee.io.Conversion;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.settings.folders.FolderSettings;

/**
 * A thread safe model of the heirarchical structure of a track graph...
 * abstracted from Expeditee frames and audio widgets.
 * 
 * The track widgets notifty this model for keeping the structure consistent with
 * expeditees data.
 * 
 * 
 * <b>Thread safe convention:</b>
 * OverdubbedFrame's and TrackGraphInfo are always handled on the swing thread.
 * This class provides asynch loading routines but that is purely for loading - not
 * handling!
 * 
 * The track graph is a loop-free directed graph.
 * 
 * A graph root is a graph of OverdubbedFrame's
 * 
 * Although a {@link TrackGraphNode} or a {@link LinkedTracksGraphNode} may reside
 * in multiple graphs, there is always only one instance of them - they
 * are just shared.
 * 
 * There are multiple graphs because Graphs can be mutually exlusive.
 * Or some frames cannot be reached unless they are a starting point (aka root)
 * 
 * @author Brook Novak
 *
 */
public class AudioStructureModel extends AbstractSubject {
	
	/** 
	 * A loop-free directed graph. 
	 * For each graph root, there can be no other graph root that is
	 * reachable (other than itself).
	 */
	private Set<OverdubbedFrame> graphRoots = new HashSet<OverdubbedFrame>(); // SHARED RESOURCE
	
	/** All overdubbed frames loaded in memory. MAP = Framename -> Overdub instance */
	private Map<String, OverdubbedFrame> allOverdubbedFrames = new HashMap<String, OverdubbedFrame>(); // SHARED RESOURCE
	
	/** For resources {@link AudioStructureModel#graphRoots} and {@link AudioStructureModel#allOverdubbedFrames} */
	private Object sharedResourceLocker = new Object();
	
	private boolean cancelFetch = false;
	
	private DelayedModelUpdator delayedModelUpdator = null;

	private static AudioStructureModel instance = new AudioStructureModel();
	
	private AudioStructureModel() { }
	
	public static AudioStructureModel getInstance() {
		return instance;
	}
	
	/**
	 * <b>MUST NOT BE IN THE EXPEDITEE THREAD! OTHERWISE WILL DEFINITELY DEADLOCK</b>
	 * 
	 * Same as {@link #fetchGraph(String)} but waits for any updates to finish before
	 * the fetch.
	 * 
	 * Intention: really to highlight the need to call waitOnUpdates prior to this ... but
	 * may not bee neccessary...
	 * 
	 * @param rootFrameName
	 * 		refer to #fetchGraph(String)
	 * @return 
	 * 		refer to #fetchGraph(String)
	 * 
	 * @throws NullPointerException
	 * 		if rootFrameName is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If rootFrameName is not a valid framename.
	 * 
	 * @throws InterruptedException
	 * 		refer to #fetchGraph(String)
	 * 
	 * @throws TrackGraphLoopException
	 * 		refer to #fetchGraph(String) - 
	 * 		<B>However</B> - it could also be that the reason was due to the thread being interupted while
	 * 		waiting on a update to finish. Thus may want to manually wait for updates prior - see
	 * 		{@link #waitOnUpdates()}.
	 * 
	 * @see #fetchGraph(String)
	 */
	public synchronized OverdubbedFrame fetchLatestGraph(String rootFrameName) 
		throws InterruptedException, TrackGraphLoopException {
		waitOnUpdates();
		return fetchGraph(rootFrameName);
	}
	
	/**
	 * May have to read from file if not yet loaded in memory.
	 * Thus it could take some time.
	 * 
	 * Thread safe.
	 * 
	 * NOTE: The intention is that this is called on a dedicated thread... other than
	 * swings thread. However once a OverdubbedFrame is returned be sure
	 * to only use it on the swing thread by convention. 
	 * 
	 * <b>MUST NOT NE IN THE EXPEDITEE THREAD! OTHERWISE WILL DEFINITELY DEADLOCK</b>
	 * 
	 * @param rootFrameName
	 * 		Must not be null. Mustn't be a link - must be the <i>framename</i>
	 * 
	 * @return
	 * 		The overdubbed frame.
	 * 		Null if the frame does not exist - or if it does exist but there
	 * 		are no track / linked-track widgets on it. Not that it can return
	 * 		a overdubbed frame that contains a heirarchy of linked overdubbed frames
	 *      but no actual playable tracks.
	 * 
	 * @throws NullPointerException
	 * 		if rootFrameName is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If rootFrameName is not a valid framename.
	 * 
	 * @throws InterruptedException
	 * 		If the fetch request was cancelled because the model
	 * 		has changed in some way during the read. Must retry the fetch.
	 * 
	 * @throws TrackGraphLoopException
	 * 		If the requested root introduces loops to the current graph state.
	 * 		The loop trace will be provided in the exception. 
	 * 
	 * @see TrackGraphLoopException#getFullLoopTrace()
	 * 
	 */
	public synchronized OverdubbedFrame fetchGraph(String rootFrameName) 
		throws InterruptedException, TrackGraphLoopException {
		
		if(rootFrameName == null) throw new NullPointerException("rootFrameName");
		if(!FrameIO.isValidFrameName(rootFrameName))
			throw new IllegalArgumentException("the rootFrameName \"" 
					+ rootFrameName +"\" is not a valid framename");
		
		// reset flag
		cancelFetch = false; // note that race conditions here is beside the point...meaningless
		
		OverdubbedFrame rootODFrame = null;
		synchronized(sharedResourceLocker) {
			rootODFrame = allOverdubbedFrames.get(rootFrameName.toLowerCase()); 
		}
		
		if (rootODFrame != null) {
			return rootODFrame;
		}
		
		// There is no overdub frame loaded for the requested frame.
		// Thus create a new root
		Map<String, OverdubbedFrame> newGraph = new HashMap<String, OverdubbedFrame> ();
		rootODFrame = buildGraph(rootFrameName, newGraph); // throws InterruptedException's

		// There exists no such frame or is not an actual overdubbed frame
		if (rootODFrame == null || rootODFrame.isEmpty()) return null;
		
		// Must run on swing thread for checking for loops before commiting the new graph to 
		// the model.
		NewGraphCommitor commit = new NewGraphCommitor(rootODFrame, newGraph);
		try {
			SwingUtilities.invokeAndWait(commit);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			assert(false);
		}
		
		// Check if commit aborted
		if (commit.abortedCommit) {
			
			if (cancelFetch) { // Due to cancel request?
				throw new InterruptedException();
			} 
			
			// Must be due to loop
			assert (!commit.loopTrace.isEmpty());
			throw new TrackGraphLoopException(commit.loopTrace);
		}
		
		return rootODFrame;
	}
	
	/**
	 * Assumption: that this is called from another thread other than the swing thread.
	 * 
	 * @param rootFrameName
	 * 
	 * @param newNodes
	 * 
	 * @return
	 * 		Null if rootFrameName does not exist
	 * 
	 * @throws InterruptedException
	 */
	private OverdubbedFrame buildGraph(String rootFrameName, Map<String, OverdubbedFrame> newNodes)
		throws InterruptedException
	{
		// Must be a frame name, not a link
		assert(rootFrameName != null);
		assert(FrameIO.isValidFrameName(rootFrameName));
		
		// If cancelled the immediatly abort fetch
		if (cancelFetch) { // check for cancel request
			throw new InterruptedException();
		}

		// Look for existing node on previously loaded graph...
		OverdubbedFrame oframe = null;
		synchronized(sharedResourceLocker) {
			oframe = allOverdubbedFrames.get(rootFrameName.toLowerCase());
		}
	
		// Check to see if not already created this node during this recursive call
		if (oframe == null)
			oframe = newNodes.get(rootFrameName.toLowerCase());

	    if (oframe != null) return oframe; 

		// There are no existing nodes ... thus create a new node from searching for
		// the frame from file
		
		// But first .. look in expeditee's cache since the cache might not be consistent with the file system yet
		// .. or in the common case - the root could be the current frame.
		ExpediteeCachedTrackInfoFetcher cacheFetch = new ExpediteeCachedTrackInfoFetcher(rootFrameName);
		
		try {
			SwingUtilities.invokeAndWait(cacheFetch);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			assert(false);
		}
		
		/* Localfilename -> TrackModelData. */
		Map<String, TrackModelData> tracks = null;

		/* VirtualFilename -> LinkedTrackModelData. */
		Map<String, LinkedTrackModelData> linkedTracks = null;

		if (cacheFetch.tracks != null) {
			
			tracks = cacheFetch.tracks;
			assert(cacheFetch.linkedTracks != null);
			linkedTracks = cacheFetch.linkedTracks;
			
		} else { // search on file system
		
			String trackPrefix = ItemUtils.GetTag(ItemUtils.TAG_IWIDGET) + ": ";
			String linkedTrackPrefix = trackPrefix;
			
			trackPrefix += SampledTrack.class.getName();
			linkedTrackPrefix += LinkedTrack.class.getName();

			String fullPath = null;
			for (int i = 0; i < FolderSettings.FrameDirs.get().size(); i++) { // RISKY CODE - IN EXPEDITEE SPACE FROM RANDOM TRHEAD
				String possiblePath = FolderSettings.FrameDirs.get().get(i);
				fullPath = FrameIO.getFrameFullPathName(possiblePath, rootFrameName);
				if (fullPath != null)
					break;
			}
			
			// does even exist?
			if (fullPath == null) {
				return null;
			}
			
			try {
				
				// Perform prefix search
				List<TextItemSearchResult> results = ExpediteeFileTextSearch.prefixSearch(
						fullPath, 
						new String[] {trackPrefix, linkedTrackPrefix});
				
				// Parse search results
				tracks = new HashMap<String, TrackModelData>();
				linkedTracks = new HashMap<String, LinkedTrackModelData>();
				
				for (TextItemSearchResult result : results) {
				
					// Track widget
					if (result.text.startsWith(trackPrefix)) {
						
						String name = null;
						String localFileName = null;
						Mutable.Long initiationTime = null;
						
						for (String data : result.data) { // read data lines
							data = data.trim();
							
							if (data.startsWith(SampledTrack.META_LOCALNAME_TAG) &&
									data.length() > SampledTrack.META_LOCALNAME_TAG.length()) {
								localFileName = data.substring(SampledTrack.META_LOCALNAME_TAG.length());
							
							} else if (data.startsWith(TrackWidgetCommons.META_INITIATIONTIME_TAG) 
									&& data.length() > TrackWidgetCommons.META_INITIATIONTIME_TAG.length()) {
								
								try {
									initiationTime = Mutable.createMutableLong(Long.parseLong(
											data.substring(TrackWidgetCommons.META_INITIATIONTIME_TAG.length())));
								} catch (NumberFormatException e) { /* Consume */ }
							
							} else if (data.startsWith(TrackWidgetCommons.META_NAME_TAG) 
									&& data.length() > TrackWidgetCommons.META_NAME_TAG.length()) {
								
								name = data.substring(TrackWidgetCommons.META_NAME_TAG.length());
							
							}
						}
						
						// Add track to map
						if (localFileName != null) {
							tracks.put(localFileName, new TrackModelData(
									initiationTime, -1, name, result.position.y)); // pass -1 for running time to signify that must be read from audio file
						}
						
					// Linked track widget
					} else {
						
						assert(result.text.startsWith(linkedTrackPrefix));
			
						// If the linked track infact has a link
						if (result.explink != null && result.explink.length() > 0) {
						
							Mutable.Long initiationTime = null;
							String virtualFilename = null;
							String name = null;
							
							for (String data : result.data) { // read data lines
								data = data.trim();
								
								// OK OK, Smell in code here - duplicated from above. - sorta
								if (data.startsWith(LinkedTrack.META_VIRTUALNAME_TAG) &&
										data.length() > LinkedTrack.META_VIRTUALNAME_TAG.length()) {
									virtualFilename = data.substring(LinkedTrack.META_VIRTUALNAME_TAG.length());
								
								} else if (data.startsWith(TrackWidgetCommons.META_INITIATIONTIME_TAG) 
										&& data.length() > TrackWidgetCommons.META_INITIATIONTIME_TAG.length()) {
									
									try {
										initiationTime = Mutable.createMutableLong(Long.parseLong(
												data.substring(TrackWidgetCommons.META_INITIATIONTIME_TAG.length())));
									} catch (NumberFormatException e) { /* Consume */ }
								
								} else if (data.startsWith(TrackWidgetCommons.META_NAME_TAG) 
										&& data.length() > TrackWidgetCommons.META_NAME_TAG.length()) {
									
									name = data.substring(TrackWidgetCommons.META_NAME_TAG.length());
								
								}
							}
							
							// Add linked track to map
							if (virtualFilename != null) {
								linkedTracks.put(virtualFilename, new LinkedTrackModelData(
										initiationTime, result.explink, name, result.position.y)); // pass -1 for running time to signify that must be read from audio file
							}
						
						}
						
		
					}
					
				} // Proccess next result
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}

		}
		
		if (cancelFetch) { // check for cancel request
			throw new InterruptedException();
		}
		
		assert(tracks != null);
		assert(linkedTracks != null);
		
		// Add new node (avoid infite recursion)
		oframe = new OverdubbedFrame(rootFrameName);
		newNodes.put(rootFrameName.toLowerCase(), oframe);
		
		// Load track times from file
		for (String localFilename : tracks.keySet()) {
			
			if (cancelFetch) { // check for cancel request
				throw new InterruptedException();
			}
			
			TrackModelData tmodel = tracks.get(localFilename);
			if (tmodel.runningTimeMS <= 0) {
				try {
					
					tmodel.runningTimeMS = AudioIO.getRunningTime(AudioPathManager.AUDIO_HOME_DIRECTORY + localFilename);
				
				} catch (IOException e) {
					e.printStackTrace();
				} catch (UnsupportedAudioFileException e) {
					e.printStackTrace();
				}
			}
			
			// If was able to get the runnning time ... then include in model
			if (tmodel.runningTimeMS > 0) {
				
				if (tmodel.initiationTime == null) {
					tmodel.initiationTime = Mutable.createMutableLong(oframe.getFirstInitiationTime());
					// Remember: initiation times are relative, so setting to zero
					// could be obscure.
				}
				
				oframe.addTrack(new TrackGraphNode(
						tmodel.initiationTime.value,
						localFilename, 
						tmodel.runningTimeMS,
						tmodel.name,
						tmodel.ypos));

			} // otheriwse omit from model.
			
		}
		
		// Get linked OverdubbedFrame's.. recurse
		for (String virtualFilename : linkedTracks.keySet()) {
			
			LinkedTrackModelData linkedTrackModel = linkedTracks.get(virtualFilename);
			
			// At this point, the link may / maynot be absolute,
			String linkedFrameName = linkedTrackModel.frameLink;
			assert(linkedFrameName != null);
			
			if (linkedFrameName.length() == 0) continue;
			
			if (!(Character.isLetter(linkedFrameName.charAt(0)))) { // relative link
				// Heres a trick: to get the frameset of the relative link - its just the
				// current root framenames frameset
				linkedFrameName = Conversion.getFramesetName(rootFrameName) + linkedFrameName; // WARNING: IN EXPEDITEE THREAD
			}

			OverdubbedFrame od = buildGraph(linkedFrameName, newNodes); // Recurse
			
			if (od == null) { // bad link or empty frame
				od = new OverdubbedFrame(linkedFrameName);
			}
			
			if (linkedTrackModel.initiationTime == null) {
				linkedTrackModel.initiationTime = Mutable.createMutableLong(oframe.getFirstInitiationTime());
				// Remember: initiation times are relative, so setting to zero
				// could be obscure.
			}
			
			oframe.addLinkedTrack(new LinkedTracksGraphNode(
					linkedTrackModel.initiationTime.value, 
					od, 
					virtualFilename,
					linkedTrackModel.name,
					linkedTrackModel.ypos));
			
		}
	
		return oframe;
	}
	
	/**
	 * 
	 * @author Brook Novak
	 *
	 */
	private class TrackModelData {
		
		TrackModelData(Mutable.Long initiationTime, long runningTimeMS, String name, int ypos) {
			this.initiationTime = initiationTime;
			this.runningTimeMS = runningTimeMS;
			this.name = name;
			this.ypos = ypos;
		}
		
		/** Null if unavilable */
		Mutable.Long initiationTime;
		
		/** negative if unavailable */
		long runningTimeMS; 
		
		/** Can be null. Trackname */
		String name;
		
		int ypos;
	}

	/**
	 * 
	 * @author Brook Novak
	 *
	 */
	private class LinkedTrackModelData {
		
		LinkedTrackModelData(Mutable.Long initiationTime, String frameLink, String name, int ypos) {
			assert(frameLink != null);
			this.initiationTime = initiationTime;
			this.frameLink = frameLink;
			this.name = name;
			this.ypos = ypos;
		}
		/** Null if unavilable */
		Mutable.Long initiationTime;
		
		/** The framename */
		String frameLink;
		
		/** Can be null. Trackname */
		String name;
		
		/** The y pixel position */
		int ypos;
	}
	
	/**
	 * Used for fetching track info in expeditees cache
	 * 
	 * @author Brook Novak
	 *
	 */
	private class ExpediteeCachedTrackInfoFetcher implements Runnable
	{
		private String rootFrameName;
		
		/** Localfilename -> TrackModelData. Not null if frame was in memory. */
		Map<String, TrackModelData> tracks = null;

		/** VirtualFilename -> LinkedTrackModelData. Not null if frame was in memory. */
		Map<String, LinkedTrackModelData> linkedTracks = null;

		ExpediteeCachedTrackInfoFetcher(String rootFrameName)
		{
			assert(rootFrameName != null);
			this.rootFrameName = rootFrameName;
		}
		
		public void run()
		{
			assert(rootFrameName != null);
			
			// Check if the current frame
			Frame rootFrame = null;
			if (DisplayIO.getCurrentFrame() != null && DisplayIO.getCurrentFrame().getName() != null
					&& DisplayIO.getCurrentFrame().getName().equals(rootFrameName)) {
				rootFrame = DisplayIO.getCurrentFrame();
			} else {
				// Check if in cache
				rootFrame = FrameIO.FrameFromCache(rootFrameName);
			}
			
			// Frame exists in memory... rummage around for track meta data
			if (rootFrame != null) {
				tracks = new HashMap<String, TrackModelData>();
				linkedTracks =  new HashMap<String, LinkedTrackModelData>();
				
				for (InteractiveWidget iw : rootFrame.getInteractiveWidgets()) {
					
					if (iw instanceof SampledTrack) {
						SampledTrack sampledTrackWidget = (SampledTrack)iw;

						TrackGraphNode tinf = 
							AudioStructureModel.getInstance().getTrackGraphInfo(
									sampledTrackWidget.getLocalFileName(), 
									rootFrameName);
						
						Mutable.Long initTime = (tinf != null) ? Mutable.createMutableLong(tinf.getInitiationTime()) :
							sampledTrackWidget.getInitiationTimeFromMeta();

						long runningTime = sampledTrackWidget.getRunningMSTimeFromRawAudio();
						if (runningTime <= 0) runningTime = -1; // signify to load from file
						
						tracks.put(sampledTrackWidget.getLocalFileName(), 
								new TrackModelData(
										initTime,
										runningTime,
										sampledTrackWidget.getName(),
										sampledTrackWidget.getY()));
						
						// NOTE: If the audio was sotred in a recovery file - then the widget would
						// be deleted - thus no need to consider recovery files.
						
					} else if (iw instanceof LinkedTrack) {
						LinkedTrack linkedTrackWidget = (LinkedTrack)iw;
			
						LinkedTracksGraphNode ltinf = 
							AudioStructureModel.getInstance().getLinkedTrackGraphInfo(
									linkedTrackWidget.getVirtualFilename(), 
									rootFrameName);
			
						Mutable.Long initTime = (ltinf != null) ? Mutable.createMutableLong(ltinf.getInitiationTime()) :
							linkedTrackWidget.getInitiationTimeFromMeta();
				
						// Don't consider track-links without any links
						if (linkedTrackWidget.getLink() != null) {
							linkedTracks.put(linkedTrackWidget.getVirtualFilename(), 
									new LinkedTrackModelData(
											initTime,
											linkedTrackWidget.getLink(),
											linkedTrackWidget.getName(),
											linkedTrackWidget.getY()));
						}

					}
					
				}
				
			}
		}
	}
	

	/**
	 * Used for commiting a new graph in a thread-safe way.
	 * Checks for loops in the graph before commiting.
	 * 
	 * If did not commit, then {@link #abortedCommit} will be true. Otherwise it will be false.
	 * 
	 * if {@link #loopTrace} is not empty after the run, then the commit
	 * was aborted because there was a loop. (And {@link #loopTrace} contains the trace).
	 * The only other reason for aborting the commit was if a cancel was requested 
	 * (@see TrackGraphModel#cancelFetch)
	 * 
	 * @author Brook Novak
	 * 
	 *
	 */
	private class NewGraphCommitor implements Runnable
	{
		private final OverdubbedFrame rootODFrame;
		private final Map<String, OverdubbedFrame> newGraph;
		
		private boolean abortedCommit = false;
		
		/** If not empty after the run, then the commit */
		Stack<OverdubbedFrame> loopTrace = new Stack<OverdubbedFrame>();

		NewGraphCommitor(OverdubbedFrame rootODFrame, Map<String, OverdubbedFrame> newGraph)
		{
			assert(rootODFrame != null);
			assert(newGraph != null);
			
			this.rootODFrame = rootODFrame;
			this.newGraph = newGraph;
		}
		
		public void run()
		{
			
			// Theoretically this would never need to be called ... but for super safety
			// keep sync block - since dealing with shared resources.
			synchronized(sharedResourceLocker) {
				
				// At anytime during this point, the new graph could become invalid
				// during this time while officially adding it to the "consistant" model..
				// which is obviously BAD.
				if (cancelFetch) {
					abortedCommit = true;
					return; // important: while locked shared resources
				}
				
				// Check that the graph is loop free.
				boolean ilf = isLoopFree(rootODFrame, loopTrace);
				if (ilf) { // check from existing roots POV's
					for (OverdubbedFrame existingRoot : graphRoots) {
						loopTrace.clear();
						ilf = isLoopFree(existingRoot, loopTrace);
						if (!ilf) break;
					}
				}
				
				if (!ilf) {
					assert (!loopTrace.isEmpty()); // loopTrace will contain the trace
					abortedCommit = true;
					return;
				} else {
					assert (loopTrace.isEmpty());
				}
				
				// Since we are creating a new root, existing roots might
				// be reachable from the new root, thus remove reachable
				// roots ... since they are no longer roots ...
				List<OverdubbedFrame> redundantRoots = new LinkedList<OverdubbedFrame>();
				for (OverdubbedFrame existingRoot : graphRoots) {
					if (rootODFrame.getChild(existingRoot.getFrameName()) != null) {
						redundantRoots.add(existingRoot); // this root is reachable from the new root
					}
				}
	
				// Get rid of the redundant roots
				graphRoots.removeAll(redundantRoots);
				
				// Commit the new loop-free graph to the model
				graphRoots.add(rootODFrame); // add the mutex or superceeding root
				allOverdubbedFrames.putAll(newGraph); // Include new graph into all allOverdubbedFrames
			}
		}
	}
//	
//	/**
//	 * Determines if a node is reachable from a given node.
//	 * 
//	 * @param source
//	 * 		Must not be null.
//	 * 
//	 * @param target
//	 * 		Must not be null.
//	 * 
//	 * @param visited
//	 * 		Must be empty. Must not be null.
//	 * 
//	 * @return
//	 * 			True if target is reachable via source.
//	 */
//	private boolean isReachable(OverdubbedFrame source, OverdubbedFrame target, Set<OverdubbedFrame> visited) {
//		assert(source != null);
//		assert(target != null);
//		assert(visited != null);
//		
//		// Base cases
//		if (source == target) return true;
//		else if (visited.contains(source)) return false;
//		
//		// Remember visited node
//		visited.add(source);
//		
//		// Recurse
//		for (Iterator<LinkedTracksGraphInfo> itor = source.getLinkedTrackIterator(); itor.hasNext();) {
//			LinkedTracksGraphInfo lti = itor.next();
//			
//			if (isReachable(lti.getLinkedFrame(), target, visited)) {
//				return true;
//			}
//		}
//		
//		// Reached end of this nodes links - found no match
//		return false;
//	}

	/**
	 * Determines if a graph is loop free from a starting point.
	 * <B>MUST BE ON THE SWING THREAD</B>
	 * 
	 * @param current
	 * 		Where to look for loops from.	
	 * 
	 * @param visitedStack
	 * 		Must be empty. Used internally. If the
	 * 		function returns false, then the stack will provde a trace of the loop.
	 * 
	 * @return
	 * 		True if the graph at current is loop free.
	 */
	private boolean isLoopFree(OverdubbedFrame current, Stack<OverdubbedFrame> visitedStack) {
		
		if (visitedStack.contains(current)) {  // found a loop
			visitedStack.add(current); // add to strack for loop trace.
			return false;
		}

		visitedStack.push(current); // save current node to stack

		for (LinkedTracksGraphNode tl : current.getLinkedTracksCopy()) {
			
			boolean ilf = isLoopFree(tl.getLinkedFrame(), visitedStack);
			
			if (!ilf) return false;
			
		}
		
		visitedStack.pop(); // pop the current node
		
		return true;
		
	}
	
	/**
	 * Gets a TrackGraphInfo. <b>MUST BE ON EXPEDITEE THREAD</b>
	 * 
	 * @param localFilename
	 * 			Must not be null.
	 * 
	 * @param parentFrameName
	 * 			If null - the search will be slower.
	 * 
	 * @return
	 * 		The TrackGraphInfo for the given track. Null if not in model
	 */
	public TrackGraphNode getTrackGraphInfo(String localFilename, String parentFrameName) {
		
		synchronized(sharedResourceLocker) { 
			
			assert(localFilename != null);
			
			if (parentFrameName != null) {
				OverdubbedFrame odframe = allOverdubbedFrames.get(parentFrameName.toLowerCase());
				
		
				if (odframe != null) { 
					return odframe.getTrack(localFilename);
				}
				
			} else { // parentFrameName is null .. do search
				
				for (OverdubbedFrame odframe : allOverdubbedFrames.values()) {
					TrackGraphNode tinf = odframe.getTrack(localFilename);
					if (tinf != null) return tinf;
				}
			}
		}
			
		return null;
	}
	

	/**
	 * Gets a TrackGraphInfo. <b>MUST BE ON EXPEDITEE THREAD</b>
	 * 
	 * @param virtualFilename
	 * 			Must not be null.
	 * 
	 * @param parentFrameName
	 * 			If null - the search will be slower.
	 * 
	 * @return
	 * 		The LinkedTracksGraphInfo for the given virtualFilename. Null if not in model
	 */
	public LinkedTracksGraphNode getLinkedTrackGraphInfo(String virtualFilename, String parentFrameName) {
		
		synchronized(sharedResourceLocker) { 
			
			assert(virtualFilename != null);
			
			if (parentFrameName != null) {
				OverdubbedFrame odframe = allOverdubbedFrames.get(parentFrameName.toLowerCase());
				
		
				if (odframe != null) { 
					return odframe.getLinkedTrack(virtualFilename);
				}
				
			} else {
				
				for (OverdubbedFrame odframe : allOverdubbedFrames.values()) {
					LinkedTracksGraphNode ltinf = odframe.getLinkedTrack(virtualFilename);
					if (ltinf != null) return ltinf;
				}
				
			}
		}
			
		return null;
	}
	
	
	/**
	 * <B>MUST BE ON SWING THREAD</B>.
	 * Gets the parent ODFrame of a track - given the tracks local filename.
	 * 
	 * @param localFilename
	 * 		The local filename of the track to get the frame for. Must not be null.
	 * 
	 * @return
	 * 		The parent. Null if no parent exists.
	 */
	public OverdubbedFrame getParentOverdubbedFrame(String localFilename) {
		assert(localFilename != null);
		
		synchronized (allOverdubbedFrames) {
			for (OverdubbedFrame odf : allOverdubbedFrames.values()) {
				if (odf.containsTrack(localFilename)) {
					return odf;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Gets an overdubbed frame representation of a given frame.
	 * 
	 * @param framename
	 * 		The name of the frame.
	 * 
	 * @return
	 * 		The OverdubbedFrame for the given framename. Null if does not exist.
	 */
	public OverdubbedFrame getOverdubbedFrame(String framename) {
		assert(framename != null);
		
		synchronized (allOverdubbedFrames) {
			return allOverdubbedFrames.get(framename.toLowerCase());
		}
	}
	
	/**
	 * <B>MUST BE ON SWING THREAD</B>.
	 * Keeps model consistent with expeditee.
	 * 
	 * @param localFilename
	 * 		Must not be null.
	 * 
	 * @param parentFrameName
	 * 		Can be null.
	 * 
	 * @param currentRunningTime
	 * 		I.e. the new running time after the edit. Must be larger than zero. In milliseconds. 
	 */
	public void onTrackWidgetAudioEdited(String localFilename, String parentFrameName, long currentRunningTime) {
		
		boolean doNotify = false;
		TrackGraphNode tinf = null;
		
		synchronized(sharedResourceLocker) { // IMPORTANT: Must wait for new graphs to be added to the shared resources
			
			assert(localFilename != null);
			assert (currentRunningTime > 0);

			// Locate parent frame
			OverdubbedFrame odframe = null;
			
			if (parentFrameName != null) odframe = allOverdubbedFrames.get(parentFrameName.toLowerCase());
			else odframe = getParentOverdubbedFrame(localFilename);

			// adjust running time in model
			if (odframe != null) { // is loaded?
				tinf = odframe.getTrack(localFilename);
				assert(tinf != null); // due to the assumption that the model is consistent
				if (tinf.getRunningTime() != currentRunningTime) {
					tinf.setRunningTime(currentRunningTime);
					// Note: a fetch might be waiting on this - i.e in progress. Thus must be cancelled.
					// It will die in its own time - and will always be cancelled because it locks
					// the current locked object.
					cancelFetch = doNotify = true;
				}
			}

	
		}
		
		// Notify observers.
		if (doNotify)
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.GRAPH_TRACK_EDITED, tinf));
	}
	
	/**
	 * <B>MUST BE ON SWING THREAD</B>.
	 * Keeps model consistent with expeditee.
	 * 
	 * @param localFilename
	 * 		Must not be null.
	 * 
	 * @param parentFrameName
	 * 		Can be null.
	 * 
	 * @param newInitiationTime
	 * 		In milliseconds.Relative - i.e. can be negative
	 * 
	 * @param name
	 * 		The name given to the linked track. Can be null if there is no name...
	 * 
	 * @param ypos
	 * 		The Y-pixel position of the track.
	 * 
	 * @param currentRunningTime
	 * 		Must be larger than zero. In milliseconds. Used in case the model has not been create for the widget.
	 */
	public void onTrackWidgetAnchored(
			String localFilename, String parentFrameName, 
			long newInitiationTime, long currentRunningTime,
			String name, int ypos) {
		
		boolean doNotify = false;
		TrackGraphNode tinf = null;
		
		synchronized(sharedResourceLocker) { // IMPORTANT: Must wait for new graphs to be added to the shared resources
			
			assert(localFilename != null);

			// Locate parent frame
			OverdubbedFrame odframe = null;
			
			if (parentFrameName != null) odframe = allOverdubbedFrames.get(parentFrameName.toLowerCase());
			else odframe = getParentOverdubbedFrame(localFilename);
			
			// adjust initiation time in model
			if (odframe != null) { // is loaded?
				tinf = odframe.getTrack(localFilename);
				
				if (tinf != null && tinf.getInitiationTime() != newInitiationTime) {
					tinf.setInitiationTime(newInitiationTime);
					cancelFetch = doNotify = true;
					
				} else { // if there is no model but overdub frame is in memory - then create new model for this track.
					
					tinf = new TrackGraphNode(
							newInitiationTime, 
							localFilename, 
							currentRunningTime,
							name,
							ypos);
					
					odframe.addTrack(tinf); // safe because on swing thread 
					
					// Note: a fetch might be waiting on this - i.e in progress. Thus must be cancelled.
					cancelFetch = doNotify = true;
					// It will die in its own time - and will always be cancelled because it locks
					// the current locked object.
				}
				

			}

		}
		
		// Notify observers.
		if (doNotify)
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.GRAPH_TRACK_ADDED, tinf));
		
	}
	
	/**
	 * <B>MUST BE ON SWING THREAD</B>.
	 * Keeps model consistent with expeditee.
	 * 
	 * Note: Invoke when picked up - not when a frame changes... also if it
	 * is removed due to XRaymode - in that case the whole model will be released.
	 * 
	 * @param localFilename
	 * 		Must not be null.
	 * 
	 * @param parentFrameName
	 * 		Can be null. If given will be faster.
	 * 
	 */
	public void onTrackWidgetRemoved(String localFilename, String parentFrameName) {
		TrackGraphNode tinf = null;
		boolean doNotify = false;
		
		synchronized(sharedResourceLocker) { // IMPORTANT: Must wait for new graphs to be added to the shared resources

			if (FrameGraphics.isXRayMode()) { // discard whole model

				// Neccessary because if the user goes into xray then moves to a new frame
				// then it wil screw everything up. Also note they can delete the text source items
				// in xray mode.
				// This basically will cause short load times to occur again...
				allOverdubbedFrames.clear();
				graphRoots.clear();

				// Note: a fetch might be waiting on this - i.e in progress. Thus must be cancelled.
				cancelFetch = doNotify = true;
				// It will die in its own time - and will always be cancelled because it locks
				// the current locked object.
				
			} else {
				
				// Locate parent frame
				OverdubbedFrame odframe = null;
				
				if (parentFrameName != null) odframe = allOverdubbedFrames.get(parentFrameName.toLowerCase());
				else odframe = getParentOverdubbedFrame(localFilename);
				
				if (odframe != null) {
					tinf = odframe.removeTrack(localFilename);
					//assert(tinf != null);
					cancelFetch = doNotify = true;
				}
				
			}

		}
			
		// Notify observers.
		if (doNotify)
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.GRAPH_TRACK_REMOVED, tinf));
	}
	
	/**
	 * <B>MUST BE ON SWING THREAD</B>.
	 * Keeps model consistent with expeditee.
	 * 
	 * @param localFilename
	 * 		Must not be null.
	 * 
	 * @param parentFrameName
	 * 		Can be null.
	 * 
	 * @param currentRunningTime
	 * 		I.e. the new running time after the edit. Must be larger than zero. In milliseconds. 
	 */
	public void onTrackWidgetNameChanged(String localFilename, String parentFrameName, String newName) {

		boolean doNotify = false;
		
		synchronized(sharedResourceLocker) { // IMPORTANT: Must wait for new graphs to be added to the shared resources
			
			assert(localFilename != null);

			// Locate parent frame
			OverdubbedFrame odframe = null;
			
			if (parentFrameName != null) odframe = allOverdubbedFrames.get(parentFrameName.toLowerCase());
			else odframe = getParentOverdubbedFrame(localFilename);
			
			// adjust name in model
			if (odframe != null) { // is loaded?
				AbstractTrackGraphNode tinf = odframe.getTrack(localFilename);
				assert(tinf != null); // due to the assumption that the model is consistent
				if (tinf.getName() != newName) {
					tinf.setName(newName);
					// Note: a fetch might be waiting on this - i.e in progress. Thus must be cancelled.
					cancelFetch = doNotify = true;
					// It will die in its own time - and will always be cancelled because it locks
					// the current locked object.
				} 
			}

		}
		
		// Notify observers.
		if (doNotify)
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.NAME_CHANGED, localFilename));
	}
	
	
	/**
	 * <B>MUST BE ON SWING THREAD</B>.
	 * Keeps model consistent with expeditee.
	 * 
	 * @param localFilename
	 * 		Must not be null.
	 * 
	 * @param parentFrameName
	 * 		Can be null.
	 * 
	 * @param initTime
	 * 		The initiation time in ms
	 * 
	 * @param yPos
	 * 		The y pixel position.
	 * 
	 */
	public void onTrackWidgetPositionChanged(String localFilename, String parentFrameName, long initTime, int yPos) {

		boolean doNotify = false;
		AbstractTrackGraphNode tinf = null;
		
		synchronized(sharedResourceLocker) { // IMPORTANT: Must wait for new graphs to be added to the shared resources
			
			assert(localFilename != null);

			// Locate parent frame
			OverdubbedFrame odframe = null;

			if (parentFrameName != null) odframe = allOverdubbedFrames.get(parentFrameName.toLowerCase());
			else odframe = getParentOverdubbedFrame(localFilename);
			
			// adjust name in model
			if (odframe != null) { // is loaded?
				tinf = odframe.getTrack(localFilename);
				if (tinf != null && 
						(tinf.getInitiationTime() != initTime || tinf.getYPixelPosition() != yPos)) {
					tinf.setInitiationTime(initTime);
					tinf.setYPixelPosition(yPos);
					// Note: a fetch might be waiting on this - i.e in progress. Thus must be cancelled.
					cancelFetch = doNotify = true;
					// It will die in its own time - and will always be cancelled because it locks
					// the current locked object.
				} 
			}

		}
		
		// Notify observers.
		if (doNotify)
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.GRAPH_TRACK_POSITION_CHANGED, tinf));
	}


	/**
	 * <B>MUST BE ON SWING THREAD</B>.
	 * Keeps model consistent with expeditee.
	 * 
	 * @param virtualFilename
	 * 		Must not be null.
	 * 
	 * @param parentFrameName
	 * 		Can be null.
	 * 
	 * @param initTime
	 * 		The initiation time in ms
	 * 
	 * @param yPos
	 * 		The y pixel position.
	 * 
	 */
	public void onLinkedTrackWidgetPositionChanged(String virtualFilename, String parentFrameName, long initTime, int yPos) {

		boolean doNotify = false;
		AbstractTrackGraphNode tinf = null;
		
		synchronized(sharedResourceLocker) { // IMPORTANT: Must wait for new graphs to be added to the shared resources
			
			assert(virtualFilename != null);

			// Locate parent frame
			OverdubbedFrame odframe = null;
			
			if (parentFrameName != null) odframe = allOverdubbedFrames.get(parentFrameName.toLowerCase());
			else odframe = getParentOverdubbedFrame(virtualFilename);
			
			// adjust name in model
			if (odframe != null) { // is loaded?
				tinf = odframe.getLinkedTrack(virtualFilename);
				if (tinf != null && 
						(tinf.getInitiationTime() != initTime || tinf.getYPixelPosition() != yPos)) {
					
					tinf.setInitiationTime(initTime);
					tinf.setYPixelPosition(yPos);
					
					// Note: a fetch might be waiting on this - i.e in progress. Thus must be cancelled.
					cancelFetch = doNotify = true;
					// It will die in its own time - and will always be cancelled because it locks
					// the current locked object.
				} 
			}
		}
		
		// Notify observers.
		if (doNotify)
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.GRAPH_LINKED_TRACK_POSITION_CHANGED, tinf));
	}
	
	/**
	 * <B>MUST BE ON SWING THREAD</B>.
	 * Keeps model consistent with expeditee.
	 * 
	 * @param virtualFilename
	 * 		Must not be null.
	 * 
	 * @param parentFrameName
	 * 		Can be null.
	 * 
	 * @param currentRunningTime
	 * 		I.e. the new running time after the edit. Must be larger than zero. In milliseconds. 
	 */
	public void onLinkedTrackWidgetNameChanged(String virtualFilename, String parentFrameName, String newName) {

		boolean doNotify = false;
		
		synchronized(sharedResourceLocker) { // IMPORTANT: Must wait for new graphs to be added to the shared resources
			
			assert(virtualFilename != null);

			// Locate parent frame
			OverdubbedFrame odframe = null;
			
			if (parentFrameName != null) odframe = allOverdubbedFrames.get(parentFrameName.toLowerCase());
			else odframe = getParentOverdubbedFrame(virtualFilename);
			
			// adjust name in model
			if (odframe != null) { // is loaded?
				AbstractTrackGraphNode tinf = odframe.getLinkedTrack(virtualFilename);
				if (tinf != null && tinf.getName() != newName) {
					tinf.setName(newName);
					// Note: a fetch might be waiting on this - i.e in progress. Thus must be cancelled.
					cancelFetch = doNotify = true;
					// It will die in its own time - and will always be cancelled because it locks
					// the current locked object.
				} 
			}
		}
		
		// Notify observers.
		if (doNotify)
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.NAME_CHANGED, virtualFilename));
	}
	
	
	/**
	 * <B>MUST BE ON SWING THREAD</B>.
	 * Keeps model consistent with expeditee.
	 * 
	 * Doe not use for changing a the widgets link. Must first remove, then acnhor with new link - 
	 * tracklinks links are immutable.
	 * 
	 * @param virtualFilename
	 * 		Must not be null.
	 * 
	 * @param parentFrameName
	 * 		Must not be null.
	 * 
	 * @param newInitiationTime
	 * 		In milliseconds.Relative - i.e. can be negative
	 * 
	 * @param absoluteLinkedFrame
	 * 		Must not be null. Must be a valid framename (absolute).
	 * 
	 * @param name
	 * 		The name given to the linked track. Can be null if there is no name...
	 * 
	 * @param ypos
	 * 		The Y-pixel position of the track.
	 * 
	 */
	public void onLinkedTrackWidgetAnchored(
			String virtualFilename, String parentFrameName, 
			long newInitiationTime, String absoluteLinkedFrame,
			String name, int ypos) {
		
		boolean doNotify = false;
		
		synchronized(sharedResourceLocker) { // IMPORTANT: Must wait for new graphs to be added to the shared resources
			
			assert(virtualFilename != null);
			assert(parentFrameName != null);
			assert(absoluteLinkedFrame != null);
			assert(FrameIO.isValidFrameName(absoluteLinkedFrame));
			
			// Locate parent frame
			OverdubbedFrame odframe = allOverdubbedFrames.get(parentFrameName.toLowerCase());
			
			if (odframe != null) { // is loaded?
				
				LinkedTracksGraphNode linkInf = odframe.getLinkedTrack(virtualFilename);

				if (linkInf != null) {
					
					// Update the initation time
					linkInf.setInitiationTime(newInitiationTime);
					
					// The link should be consistant - check for miss-use of procedure call
					assert(linkInf.getLinkedFrame().getFrameName().equalsIgnoreCase(absoluteLinkedFrame));
										
				} else { // if there is no model but overdub frame is in memory - then create new model for this track.
					
					updateLater(new LinkedTrackUpdate(
							newInitiationTime, absoluteLinkedFrame,
							virtualFilename, parentFrameName, name, ypos));
					
				}
				
				// Note: a fetch might be waiting on this - i.e in progress. Thus must be cancelled.
				cancelFetch = doNotify = true;
				// It will die in its own time - and will always be cancelled because it locks
				// the current locked object.
			}

		}
		
		// Notify observers.
		if (doNotify)
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.GRAPH_LINKED_TRACK_ADDED, virtualFilename));
		
	}
	
	/**
	 * <B>MUST BE ON SWING THREAD</B>.
	 * Keeps model consistent with expeditee.
	 * 
	 * Note: Invoke when picked up - not when a frame changes... also if it
	 * is removed due to XRaymode - in that case the whole model will be released.
	 * 
	 * <b>Also</b> invoke if the track tracks link has changed.
	 * 
	 * @param virtualFilename
	 * 		Must not be null.
	 * 
	 * @param parentFrameName
	 * 		Can be null. If given will be faster.
	 * 
	 */
	public void onLinkedTrackWidgetRemoved(String virtualFilename, String parentFrameName) {
		
		boolean doNotify = false;
		
		synchronized(sharedResourceLocker) { // IMPORTANT: Must wait for new graphs to be added to the shared resources

	
			if (FrameGraphics.isXRayMode()) { // discard whole model

				// Neccessary because if the user goes into xray then moves to a new frame
				// then it wil screw everything up. Also note they can delete the text source items
				// in xray mode.
				// This basically will cause short load times to occur again...
				allOverdubbedFrames.clear();
				graphRoots.clear();
				
				
				// Note: a fetch might be waiting on this - i.e in progress. Thus must be cancelled.
				cancelFetch = doNotify = true;
				// It will die in its own time - and will always be cancelled because it locks
				// the current locked object.
			} else {
				
				// Locate parent frame
				OverdubbedFrame odframe = null;
				
				if (parentFrameName != null) {
					odframe = allOverdubbedFrames.get(parentFrameName.toLowerCase());
				} else { 
					for (OverdubbedFrame odf : allOverdubbedFrames.values()) {
						if (odf.containsLinkedTrack(virtualFilename)) {
							odframe = odf;
							break;
						}
					}
				}
				
				if (odframe != null) {
					LinkedTracksGraphNode linkInf = odframe.getLinkedTrack(virtualFilename);
					
					if (linkInf != null) {
						boolean didRemove = odframe.removeLinkedTrack(linkInf);
						assert(didRemove);
						
						// Track links are the actual links in the directed graph. THus if they are
						// removed then the graph must be checked for creating new root nodes.
						// That is, the removed link may have isolated a node (or group of nodes)
						// for which must be reachable via their own start state...
						boolean isReachable = false;
						for (OverdubbedFrame existingRoot : graphRoots) {
							if (existingRoot.getChild(linkInf.getLinkedFrame().getFrameName()) != null) {
								isReachable = true;
								break;
							}
							
						}
						
						// Ensure that the frame is reachable
						if (!isReachable) {
							graphRoots.add(linkInf.getLinkedFrame());
						}
						
						// Note: a fetch might be waiting on this - i.e in progress. Thus must be cancelled.
						cancelFetch = doNotify = true;
						// It will die in its own time - and will always be cancelled because it locks
						// the current locked object.
					
					}
					
					
				}
				
			}
		
		}
			
		// Notify observers.
		if (doNotify)
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.GRAPH_LINKED_TRACK_REMOVED, virtualFilename));
	}
	
	/**
	 * @return
	 * 			True if the graph model is updating.
	 */
	public boolean isUpdating() {
		return (delayedModelUpdator != null && delayedModelUpdator.isAlive());
	}

	
	/**
	 * Waits for updates to finish.
	 * 
	 * <b>MUST NOT BE ON SWING THREAD - OR MAY DEADLOCK</b>
	 * 
	 * @throws InterruptedException
	 * 		if any thread has interrupted the current thread
	 */
	public void waitOnUpdates() throws InterruptedException {
		if (delayedModelUpdator != null && delayedModelUpdator.isAlive()) {
			delayedModelUpdator.join();
		}
	}
	
	/**
	 * Queues an update for the consistant model to be updated later... since it may take some time.
	 * 
	 * @param update
	 * 		The update to do. Must not be null.
	 */
	private void updateLater(LinkedTrackUpdate update) {
		
		synchronized(updateQueue) {
			
			assert(update != null);

			// Add the update tot he queu
			updateQueue.add(update);
		
			// Ensure that the update thread is alive
			if (delayedModelUpdator == null || !delayedModelUpdator.isAlive()) {
				delayedModelUpdator = new DelayedModelUpdator();
				delayedModelUpdator.start();
			}
			
		}	
			
	}

	private Queue<LinkedTrackUpdate> updateQueue = new LinkedList<LinkedTrackUpdate>(); // SHARED RESOURCE
	
	/**
	 * Used for queuing update data.
	 * 
	 * 
	 * @author Brook Novak
	 *
	 */
	private class LinkedTrackUpdate extends LinkedTrackModelData {
		
		LinkedTrackUpdate(
				long initiationTime, 
				String absoluteLink, 
				String virtualFilename, 
				String parentFrameToAddTo,
				String name,
				int ypos) {
			
			super(Mutable.createMutableLong(initiationTime), absoluteLink, name, ypos);
			
			assert(virtualFilename != null);
			assert(parentFrameToAddTo != null);
			assert(FrameIO.isValidFrameName(absoluteLink));
			
			this.virtualFilename = virtualFilename;
			this.parentFrameToAddTo = parentFrameToAddTo;
		}
		
		String virtualFilename;
		String parentFrameToAddTo;
		
	}
	
	/**
	 * 
	 * @author Brook Novak
	 *
	 */
	private class DelayedModelUpdator extends Thread {
		
		public void run() {
			while (true) {
				LinkedTrackUpdate update;
				synchronized(updateQueue) {
					if (updateQueue.isEmpty()) return; // important: only quits when updateQueue is synched
					update = updateQueue.poll();
				}
				
				assert(update != null);
				
				// Keep trying the update until success or encountered loops
				while (true) {
					try {
						
						OverdubbedFrame odframe = fetchGraph(update.frameLink);
						if (odframe == null) { // does not exist or is empty...
							odframe = new OverdubbedFrame(update.frameLink);
						}
						

						// add to parent only if does not already exist - also being aware of loops
						final LinkedTrackUpdate updateData = update;
						final OverdubbedFrame linkedODFrame = odframe;
						
						try {
							SwingUtilities.invokeAndWait(new Runnable() {
								
								public void run() { // on swing thread

									synchronized(sharedResourceLocker) {
										
										try { // The fetch will be cancelled after this block - within the lock
																	
											OverdubbedFrame parentFrame = allOverdubbedFrames.get(updateData.parentFrameToAddTo.toLowerCase());
											if (parentFrame == null) {
												// No need to both adding - because model does not even reference this new link...
												// must have cleared while updating... e.g. user could have switched into xray mode.
												return;
											}
											
											assert(updateData.initiationTime != null);
											
											// Create the linked track instance
											LinkedTracksGraphNode linktInf = new LinkedTracksGraphNode(
													updateData.initiationTime.value, 
													linkedODFrame, 
													updateData.virtualFilename,
													updateData.name,
													updateData.ypos);

											// Check that the graph will be loop free
											parentFrame.addLinkedTrack(linktInf); // ADDING TEMPORARILY

											// Check from parent frame
											Stack<OverdubbedFrame> loopTrace = new Stack<OverdubbedFrame>();
											if (!isLoopFree(parentFrame, loopTrace)) {
												parentFrame.removeLinkedTrack(linktInf);
												// Ignore link - has loop
												return;
											}
											
											// Check for all existing graph roots
											for (OverdubbedFrame existingRoot : graphRoots) {
												loopTrace.clear();
												if (!isLoopFree(existingRoot, loopTrace)) {
													parentFrame.removeLinkedTrack(linktInf);
													// Ignore link - has loop
													 return;
												}
											}
			
											// Otherwise leave the link in its place 
											
											// Ensure that the linked frame is in the allOverdubbedFrames set
											allOverdubbedFrames.put(linkedODFrame.getFrameName().toLowerCase(), linkedODFrame);
										
										} finally { // IMPORTANT: CANCEL WHILE LOCKED
											
											// Cancel any fetches
											cancelFetch = true;
										}	
										
									}  // release lock
								
										
								}
							});
						} catch (InvocationTargetException e) {
							e.printStackTrace();
							assert(false);
						}
		
						// Done with this update...
						break;
						
						
					} catch (InterruptedException e) { // Canceled
						// Consume and retry
						
					} catch (TrackGraphLoopException e) { // bad link
						// Consume - since is fine - the model will just ignore the link
						break;
					}
				} // retry fetch
				
			} // proccess next update
			
		} // finished updating
	}
	
}
