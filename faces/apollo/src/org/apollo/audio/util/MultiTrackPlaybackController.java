package org.apollo.audio.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.SwingUtilities;

import org.apollo.audio.ApolloPlaybackMixer;
import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.SampledAudioManager;
import org.apollo.audio.SampledTrackModel;
import org.apollo.audio.TrackSequence;
import org.apollo.audio.structure.AbsoluteTrackNode;
import org.apollo.audio.structure.AudioStructureModel;
import org.apollo.audio.structure.OverdubbedFrame;
import org.apollo.audio.structure.TrackGraphLoopException;
import org.apollo.audio.structure.TrackGraphNode;
import org.apollo.io.AudioPathManager;
import org.apollo.io.AudioIO.AudioFileLoader;
import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.AudioMath;
import org.apollo.util.StringEx;
import org.apollo.util.TrackModelHandler;
import org.apollo.util.TrackModelLoadManager;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.DisplayIOObserver;


public class MultiTrackPlaybackController 
	extends AbstractSubject 
	implements TrackModelHandler, Observer, DisplayIOObserver {
	
	/** Can be a framename or virtual filename (e.g. from a linked track) */
	private String rootFrameName = null; // Shared resource
	
	private String masterChannelID = null; // Shared resource
	
	private boolean markedAsPaused = false; // by user convention .. just centralized model data
	
	// Track-sequence like data for multitrack
	private int suspendedFramePosition = 0;
	private long initiationFramePosition = 0;
	private int startFramePosition = 0;
	private int endFramePosition = 0;
	
	private boolean canInstantlyResume = false;
	
	/** A flag set when playback is commenced and resets when the first track begins to play.
	 * .. or if the commence failed.*/
	private boolean isPlaybackPending = false;
	
	private MultiTrackPlaybackLoader loaderThread = null;
	
	/** The tracks that are being loaded, played or stopped ... kept for resuming / fast reloading */
	private List<Overdub> currentOverdubs = new LinkedList<Overdub>(); // SHARED RESOURCE
	private Set<TrackSequence> currentTrackSequences = null;
	
	private OverdubbedFrame currentODFrame = null;

	private int cacheExpiryCounter = 0;
	private static final int CACHE_DEPTH = 5;
	
	/**
	 * Singleton design pattern
	 */
	private static MultiTrackPlaybackController instance = new MultiTrackPlaybackController();
	public static MultiTrackPlaybackController getInstance() {
		return instance;
	}
	
	/**
	 * Singleton constructor:
	 * Sets up perminant observed subjects.
	 */
	private MultiTrackPlaybackController() {
		
		// Since track groups are cached .. for a awhile
		TrackModelLoadManager.getInstance().addTrackModelHandler(this);
		
		// Dynamically adjust group of tracks while loading or playing
		AudioStructureModel.getInstance().addObserver(this);
		
		// After a certain amount of frame changes since the last multi playback has occured
		// the cached track models are freed to stop consuming all the memory.
		DisplayIO.addDisplayIOObserver(this);
		
		// For whenever a track sequence is created - must observe the created track sequences...
		SoundDesk.getInstance().addObserver(this);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public Subject getObservedSubject() {
		return null; // many!
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modelChanged(Subject source, SubjectChangedEvent event) {
		
		TrackSequence ts;
		
		switch (event.getID()) {
		case ApolloSubjectChangedEvent.GRAPH_TRACK_REMOVED: // TODO: Remove while loading!
		case ApolloSubjectChangedEvent.GRAPH_TRACK_POSITION_CHANGED:
			
			if (isPlaying()) {
				// Does the removed track belong to the current group?
				TrackGraphNode tnode = (TrackGraphNode)event.getState();
				
				synchronized(currentOverdubs) {
					
					for (Overdub od : currentOverdubs) {
						
						if (od.getTrackModel().getLocalFilename().equals(tnode.getLocalFilename())) {
							
							TrackSequence trackSeq = 
								SoundDesk.getInstance().getTrackSequence(od.getChannelID());
							
							if (trackSeq != null) { // && trackSeq.isPlaying()) {
								ApolloPlaybackMixer.getInstance().stop(trackSeq);
							}
							
						}
						
					}
					
				}
				
			}
			canInstantlyResume = false;
			break;

		case ApolloSubjectChangedEvent.GRAPH_LINKED_TRACK_POSITION_CHANGED:
		case ApolloSubjectChangedEvent.GRAPH_LINKED_TRACK_REMOVED: // TODO: Remove while loading!
			
			if (event.getID() != ApolloSubjectChangedEvent.GRAPH_TRACK_REMOVED) {
				if (isPlaying()) {
					
					// Does the removed track belong to the current group?
					String virtualFilename = (String)event.getState();
					
					synchronized(currentOverdubs) {
						
						for (Overdub od : currentOverdubs) {
							
							if (od.getChannelID().indexOf(virtualFilename) >= 0) { // TODO: THIS IS TEMP - NOT RIGHT!! - 99% OK.. - Must revise this all anyway
								
								TrackSequence trackSeq = 
									SoundDesk.getInstance().getTrackSequence(od.getChannelID());
								
								if (trackSeq != null) { // && trackSeq.isPlaying()) {
									ApolloPlaybackMixer.getInstance().stop(trackSeq);
								}
								
							}
							
						}
						
					}
					
				}
			}
			
			canInstantlyResume = false;
			break;
				
		case ApolloSubjectChangedEvent.GRAPH_LINKED_TRACK_ADDED:
		case ApolloSubjectChangedEvent.GRAPH_TRACK_ADDED:
		case ApolloSubjectChangedEvent.GRAPH_TRACK_EDITED: 
			this.canInstantlyResume = false;
			break;
			
		case ApolloSubjectChangedEvent.TRACK_SEQUENCE_CREATED:
			assert(source == SoundDesk.getInstance());
			
			String channelID = (String) event.getState();
			
			if (!isPlaybackPending) break;
			boolean doesBelong = false;
			
			// Does the track sequence belong to this set?
			synchronized(currentOverdubs) {
				for (Overdub od : currentOverdubs) {
					if (od.getChannelID().equals(channelID)) {
						doesBelong = true;
						break;
					}
				}
			}
			
			if (!doesBelong) break;
			
			ts = SoundDesk.getInstance().getTrackSequence(channelID);
			assert(ts != null);
			
			if (currentTrackSequences == null)
				currentTrackSequences = new HashSet<TrackSequence>();
			
			assert(!currentTrackSequences.contains(ts));

			ts.addObserver(this);
			currentTrackSequences.add(ts);
			
			// Note: if fails to start payback or is stopped before is playing
			// then it does not need to unregister that the subject will be eventually freed.
			
			// the currentTrackSequences set is nullified if the playback fails anyway...
			break;
			
		case ApolloSubjectChangedEvent.PLAYBACK_STARTED:
			// Can get many of these over time in one playback call... so make sure that a event is raised
			// on the first event received...
			if (isPlaybackPending) {
				isPlaybackPending = false;
				initiationFramePosition = ((TrackSequence)source).getCommencedFrame();
				suspendedFramePosition = 0;
				fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.PLAYBACK_STARTED));
			}
			break;
			
			// Keep track of what is/isn't playing. Note that currentTrackSequences is cleared explicity
			// before stop events occur when playback is commences while already playing back
		case ApolloSubjectChangedEvent.PLAYBACK_STOPPED:

			if (currentTrackSequences != null && !currentTrackSequences.isEmpty()) {
				
				currentTrackSequences.remove(source);
				//if (currentTrackSequences.remove(source)) isPlaybackPending = false;
				
				ts = (TrackSequence)source;
				// Calculate suspended frame position .. this could be the last track stopped that actually has started playback
				if (ts.getCurrentFrame() > ts.getStartFrame()) {
					int susFrame = ts.getSuspendedFrame() + ts.getRelativeInitiationFrame() + startFramePosition;
					
					if (susFrame > suspendedFramePosition)
						suspendedFramePosition = susFrame;

				}
		
				if (currentTrackSequences.isEmpty()) {
					fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.PLAYBACK_STOPPED));
				}
			}
			
			break;
			

		}
		
		
	}
	

	/**
	 * {@inheritDoc}
	 */
	public void setObservedSubject(Subject parent) {
		// Ignore: many subjects observed
	}
	
	/**
	 * {@inheritDoc}
	 */
	public SampledTrackModel getSharedSampledTrackModel(String localfilename) {
		if (localfilename == null) return null;
		
		// The track group is updated at every loaded track
		synchronized(currentOverdubs) {
			// Search cached tracks
			for (Overdub od : currentOverdubs) {
				if (od.getTrackModel().getFilepath() != null &&
						localfilename.equals(od.getTrackModel().getLocalFilename())) {
					return od.getTrackModel();
				}
			}
		}

		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void frameChanged() {
		if (hasTrackCacheExpired() || isPlaying() || isLoading() || DisplayIO.getCurrentFrame() == null) return; // already expired - or is playing - so don't expire!
		
		String currentFrameName = DisplayIO.getCurrentFrame().getName();
		
		// Free track group every so often.. 
		
		if (currentODFrame != null && rootFrameName != null &&
				currentODFrame.getFrameName().equals(this.rootFrameName)) {
			// Reset if traversed through a frame that belongs to the current group
			if (currentODFrame.getChild(currentFrameName) != null) { 
				cacheExpiryCounter = CACHE_DEPTH; // reset
				return;
			}
		}
	
		if (currentFrameName.equals(rootFrameName)) {
			cacheExpiryCounter = CACHE_DEPTH; // resets cache - ok since cache has no expired at this point
		} else {
			cacheExpiryCounter--;
			if (hasTrackCacheExpired()) {
				freeCurrentTrackGroup();
			}
		}
	}
	
	/**
	 * @return
	 * 		True if there are no cached tracks
	 */
	private boolean hasTrackCacheExpired() {
		return cacheExpiryCounter <= 0;
	}

	/**
	 * 	Releases all resource consumed by this. Stops any threads. Non-blocking.
	 *  Intention: for shutting down Apollo. But can use if need to get rid of 
	 *  any resources consumed by this.
	 *  
	 *  Note that this does not attempt to stop any tracks being played.
	 *  
	 */
	public void releaseResources() {
		
		if (isLoading())
			loaderThread.cancel();
		
		if (isPlaying())
			stopPlayback();
	}
	
	/**
	 * 
	 * @return
	 * 		True if currently loading tracks.
	 */
	public boolean isLoading() {
		return (loaderThread != null && !loaderThread.loadFinished);
	}
	
	/**
	 * 
	 * @param rootFrameName
	 * 
	 * @param masterMix
	 * 
	 * @return
	 * 		True if currently loading tracks with the given mix/frame.
	 */
	public boolean isLoading(String rootFrameName, String masterMix) {
		return (isLoading() && isCurrentPlaybackSubject(rootFrameName, masterMix));
	}
	
	/**
	 * 
	 * @return
	 * 		True if currently playing
	 */
	public boolean isPlaying() {
		return currentTrackSequences != null && !currentTrackSequences.isEmpty();
	}
	
	/**
	 * 
	 * @param rootFrameName
	 * 
	 * @param masterMix
	 * 
	 * @return
	 * 		True if currently playing the given mix/frame.
	 */
	public boolean isPlaying(String rootFrameName, String masterMix) {
		return (isPlaying() && isCurrentPlaybackSubject(rootFrameName, masterMix));

	}
	
	/**
	 * Sets a paused mark - centralized paused info for multiple viewers.
	 * Fires a {@link ApolloSubjectChangedEvent#PAUSE_MARK_CHANGED} event.
	 * 
	 * @param isMarked
	 * 		True if to become marked. False to reset.
	 */
	public void setPauseMark(boolean isMarked) {
		this.markedAsPaused = isMarked;
		fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.PAUSE_MARK_CHANGED));
	}
	
	/**
	 * @return
	 * 		True if the multplayback controller is marked as paused.
	 */
	public boolean isMarkedAsPaused() {
		return this.markedAsPaused;
	}
	
	/**
	 * 
	 * @param rootFrameName
	 * 
	 * @param masterMix
	 * 
	 * @return
	 * 		True if currently playing the given mix/frame.
	 */
	public boolean isMarkedAsPaused(String rootFrameName, String masterMix) {
		return (markedAsPaused && isCurrentPlaybackSubject(rootFrameName, masterMix));
	}
	
	public boolean isCurrentPlaybackSubject(String rootFrameName, String masterMix) {
		return (StringEx.equals(this.rootFrameName, rootFrameName) &&
				StringEx.equals(this.masterChannelID, masterMix));
	}


	/**
	 * In order to explicitly release the references of track models kept in the cache.
	 * Note that releasing is handled internally.
	 * 
	 * If the track group is currently playing then nothing will result from this call
	 * 
	 */
	public void freeCurrentTrackGroup() {
		if (isPlaying()) return;
		
		synchronized(currentOverdubs) {
			currentOverdubs.clear();
		}
		cacheExpiryCounter = -1;
	}
	
	
	/**
	 * Adds a listener to the loader for recieving notifications about the load progress.
	 * 
	 * @param listener
	 * 		The listener to attack. Must not be null.
	 * 		If already on the listener list then it wont be added twice
	 * 
	 * @return
	 * 		Null if did not attack due to not loading.
	 * 		Otherwise all of the currently loaded tracks.
	 * 		Note, may still receive notifications for some of the returned
	 * 		loadeds tracks ... as the events could currently on the swing queue
	 */
	public List<String> attachLoadListener(MultitrackLoadListener listener) {
		assert(listener != null);
		if (isLoading()) {
			
			List<String> loaded = new LinkedList<String>();
			
			loaderThread.addLoadListener(listener);
			synchronized (currentOverdubs) {
				
				for (Overdub od : currentOverdubs) {
					loaded.add(od.getTrackModel().getLocalFilename());
				}
			}
			
			return loaded;
		}
		
		return null;
	}
		
	/**
	 * Cancels loading phase of playback for the given frame/mix .. if
	 * there is anything currently loading.
	 * 
	 * @param rootFrameName
	 * 
	 * @param masterMix
	 * 
	 */
	public void cancelLoad(String rootFrameName, String masterMix) {
		if (isCurrentPlaybackSubject(rootFrameName, masterMix) && isLoading()) {
			loaderThread.cancel();
		}
	}
	
	/**
	 * Stops the tracks from playing as soon as possible.
	 */
	public void stopPlayback() {
		ApolloPlaybackMixer.getInstance().stop(currentTrackSequences);
		// Notes: eventually their stop events will be invoked on this thread.
		
	}
	
	
	/**
	 * Asynchronously plays a frame of a linked track. Must be invoked from the swing thread.
	 * Non-blocking - but sends feedback messages <i>on a dedicated thread</i> via the 
	 * given load listener.
	 * 
	 * Raises {@link ApolloSubjectChangedEvent#MULTIPLAYBACK_LOADING} event when loading comences.
	 * Raises playback events on success.
	 * 
	 * If this is currentlly in a loading state it will be cancelled and a the playback
	 * call will halt until its cancelled (in acceptable amount of time for the user to interactive with).
	 * 
	 * @param listener
	 * 		The listener used for callbacks of load progress.
	 * 		Must never be null.
	 * 
	 * @param rootFrameName
	 * 		The root frame from where all the tracks are to be loaded from.
	 * 		Must never be null.
	 * 
	 * @param masterMixID
	 * 		The master mix ID.
	 * 
	 * @param resume
	 * 		True if resuming - the last-played files will be resumed instantly iff 
	 * 		the last played set of tracks don't require reloading... you can resume with
	 * 		false but by resuming with when you want to resume it could load faster.
	 * 
	 * @param relativeStartFrame
	 * 		The start frame from when all the tracks should commence in the playback mixer.
	 * 		Must be positive.
	 * 
	 * @param startFrame
	 * 		The start from from <i>within</i> the group of tracks when playback should begin.
	 * 		Must be positive. Clamped.
	 * 
	 * @param endFrame
	 * 		Must be larger than start from. Clamped.
	 * 		
	 */
	public void playFrame(
			MultitrackLoadListener listener,
			String rootFrameName,
			String masterMixID,
			boolean resume,
			int startFrame,
			int endFrame) {
		
		assert(startFrame >= 0);
		assert(endFrame > startFrame);
		assert(listener != null);
		assert(rootFrameName != null);
		assert(masterMixID != null);

		// Check if curently loading:
		if(loaderThread != null) {
			loaderThread.cancel(); // non blocking
			// NOTES: If the load thread is finished and there is an event waiting to be proccessed
			// from the load thread (sometime after this event has finished proccessing) then explicitly
			// cancel so that the waiting play event will definitly be aborted.
			
			// Also: cannot wait on it to finish because it may be waiting on some things to
			// proccess on the swing thread... must leave it to die in its won time.
		}
		
		// Check if currently playing
		if (isPlaying()) {
			stopPlayback();
		}
		
		// Must clear the current track sequences even though eventually they will be removed
		// due to the stop call... because in order for them to remove a away event on the queue behind
		// this call will do so... hence it is impossible to wait on the stop events to raise since
		// they are waiting for this operation to finish. Thus must explicity clear the track sequences 
		// in order for playback to commence:
		if (currentTrackSequences != null && !currentTrackSequences.isEmpty()) {
			currentTrackSequences.clear();
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.PLAYBACK_STOPPED));
		}
		
		this.rootFrameName = rootFrameName;
		this.masterChannelID = masterMixID;
		
		List<MultitrackLoadListener> loadListeners = new LinkedList<MultitrackLoadListener>();
		loadListeners.add(listener);

		// Note that the a group of track resuming may have to reload due to the cache expiring
		if (resume 
				&& !hasTrackCacheExpired() 
				&& canInstantlyResume 
				&& isCurrentPlaybackSubject(rootFrameName, masterMixID)) {

			// Notify listener that load phase has instantly completed
			notifyListeners(loadListeners, MultitrackLoadListener.LOAD_COMPLETE, null, false);

			// Play back ..s
			commencePlayback(startFrame, endFrame, loadListeners);
			
			
		} else {
	
			// Load track and their start positions
			loaderThread = new MultiTrackPlaybackLoader(startFrame, endFrame, loadListeners);
			
			// Notify when in loading state.
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.MULTIPLAYBACK_LOADING));
			
			loaderThread.start(); // will call commencePlayback once loaded.
			
		}

	}


	/**
	 * @return
	 * 		The last frame within all of the tracks that was rendered.
	 * 		Can be make no sence if the audio structure has dramatically changed.
	 */
	public int getLastSuspendedFrame() {
		return this.suspendedFramePosition;
	}
	
	/**
	 * @return
	 * 		The last time a group of overdubs were played this is the first frame 
	 * 		<i>within the apollo mixers timeline that the group began.</i>
	 */
	public long getLastInitiationFrame() {
		return this.initiationFramePosition;
	}
	
	/**
	 * @return
	 * 		The last time a group of overdubs were played this is the starting frame from
	 * 		<i>within the track-groups combined timeline.</i>
	 */
	public int getLastStartFrame() {
		return this.startFramePosition;
	}
	
	/**
	 * @return
	 * 		The last time a group of overdubs were played this is the ending frame from
	 * 		<i>within the track-groups combined timeline.</i>
	 */
	public int getLastEndFrame() {
		return this.endFramePosition;
	}
	
	/**
	 * Begins playback for the current set of overdubs. 
	 * 
	 * Must not be in a playing state. MUST BE ON THE SWING THREAD.
	 * 
	 * @param startFrame
	 * 		The start from from <i>within</i> the group of tracks when playback should begin.
	 * 		Must be positive. Clamped.
	 * 
	 * @param endFrame
	 * 		Must be larger than start from. Clamped.
	 * 
	 * @param loadListeners
	 * 		The listeners to receive status reports. Must not be shared. Must not be null or empty.
	 */
	private void commencePlayback(
			int startFrame,
			int endFrame,
			List<MultitrackLoadListener> loadListeners) {

		assert(!isPlaying());
		assert(loadListeners != null);
		assert(!loadListeners.isEmpty());
		assert(!isPlaybackPending);
		
		startFramePosition = startFrame;
		endFramePosition = endFrame;
		
		// If got to this stage then instant resume is defiitly supported.
		canInstantlyResume = true;
		
		// Reset the cache now that playing
		cacheExpiryCounter = CACHE_DEPTH;

		// Construct a list that is not shared by other threads and that
		// only contains overdubs in the playing range.
		List<Overdub> dubList = new LinkedList<Overdub>();
		 
		 
		long totalFrames = 0;
		
		synchronized(currentOverdubs) {
				
			// Prepare each overdub - defining start, end and initation times.
			for (Overdub od : currentOverdubs) {
				
				// Keep track of total frames in track for clamping later
				long odEndFrame = od.getABSInitiationFrame() + od.getTrackModel().getFrameCount();
				if (odEndFrame > totalFrames) totalFrames = odEndFrame;
				
				if (startFrame >= (od.getABSInitiationFrame() + od.getTrackModel().getFrameCount()) ||
						endFrame <= od.getABSInitiationFrame()) {
					// Exclude this track - its not in range

				} else { // play this track - it is in range
					
					od.relativeInitiationFrame = (int)(od.getABSInitiationFrame() - startFrame);
					
					od.startFrame = (int)((startFrame > od.getABSInitiationFrame()) ? 
							startFrame - od.getABSInitiationFrame() : 0);
					
					od.endFrame = (int)((endFrame < odEndFrame) ?
							endFrame - od.getABSInitiationFrame() : od.getTrackModel().getFrameCount() - 1);
				
					if (od.startFrame < od.endFrame)
						// Include in playlist
						dubList.add(od);
				}

			}

		}
		
		// Clamp:
		if (startFramePosition > totalFrames)
			startFramePosition = (int)totalFrames;
		
		if (endFramePosition > totalFrames)
			endFramePosition = (int)totalFrames;
	
		if (dubList.isEmpty()) {
			
			notifyListeners(loadListeners, MultitrackLoadListener.NOTHING_TO_PLAY, null, false);
			
		} else {

			boolean succeeded = false;
			try {
				
				// Commence playback...
				isPlaybackPending = true;
				SoundDesk.getInstance().playOverdubs(dubList); // fires events which are relayed to observers
				succeeded = true;
				
			} catch (LineUnavailableException e) {
				
				notifyListeners(loadListeners, MultitrackLoadListener.LOAD_FAILED_PLAYBACK, e, false);
				
			} finally {
				
				if (!succeeded) { // reset flag and clear track sequence list
					currentTrackSequences = null;
					isPlaybackPending = false;
				}
			}
		}
	}
	
	/**
	 * Notifies the load listeners .. must notify on swing thread to
	 * remember to pass flag in need to.
	 * 
	 * @param listeners
	 * 		when accessed if invoteLaterOnSwing
	 * 
	 * @param id
	 * 
	 * @param state
	 * 
	 * @param invoteLaterOnSwing
	 */
	private void notifyListeners(
			List<MultitrackLoadListener> listeners, 
			int id, 
			Object state,
			boolean invoteLaterOnSwing) {
		
		if (invoteLaterOnSwing) {
			
			final List<MultitrackLoadListener> listeners1 = listeners;
			final int id1 = id; 
			final Object state1 = state;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					synchronized(listeners1) {
						notifyListeners(listeners1, id1, state1, false);
					}
				}
			});
			
		} else {
			
			for (MultitrackLoadListener listener : listeners)
				listener.multiplaybackLoadStatusUpdate(id, state);
		}
	}
	
	/**
	 * Loads all tracks reachable from a given frame.
	 * 
	 * @author Brook Novak
	 *
	 */
	private class MultiTrackPlaybackLoader extends Thread {
		
		/** Set to true once finished running. */
		private boolean cancelRequested = false;
		private boolean loadFinished = false; // Once playback has commenced - or failed/aborted... not like isAlive
		
		private OverdubbedFrame rootODFrame = null;
		private int startFrame;
		private int endFrame;
		private List<MultitrackLoadListener> loadListeners = null; // reference immutable, contents not. Shared resource
		
		MultiTrackPlaybackLoader(
				int startFrame,
				int endFrame,
				List<MultitrackLoadListener> loadListeners) {
			
			super("Multitrack Loader");
			
			this.loadListeners = loadListeners;
			this.startFrame = startFrame;
			this.endFrame = endFrame;
		}
		
		/**
		 * Cancels the load stage.
		 * NOTE: May actually succeed... but when it comes to commencing the playback then
		 * then the play will abort.
		 *
		 */
		public void cancel() {
			cancelRequested = true;
		}
		
		public void addLoadListener(MultitrackLoadListener ll) {
			synchronized(loadListeners) {
				if (!loadListeners.contains(ll)) loadListeners.add(ll);
			}
		}
		
		public void run() {
	
			assert(rootFrameName != null);
			assert(masterChannelID != null);
			
			synchronized(loadListeners) {
				assert(loadListeners != null);
				assert(!loadListeners.isEmpty());
			}
			
			boolean hasSucceeded = false;
			
			try {

				if (cancelRequested) {
					notifyListeners(loadListeners, 
							MultitrackLoadListener.LOAD_CANCELLED, 
							null, true);
					return;
				}
	
				// First fetch the graph for the rootframe to play

				boolean hasUpdated = false;
				do {
					try {
						AudioStructureModel.getInstance().waitOnUpdates();
						hasUpdated = true;
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;
					}
				} while (!hasUpdated);
				
				if (cancelRequested) {
					notifyListeners(loadListeners, 
							MultitrackLoadListener.LOAD_CANCELLED, 
							null, true);
					return;
				}
	
				boolean hasFetched = false;
				do {
					try {
						rootODFrame = AudioStructureModel.getInstance().fetchGraph(rootFrameName);
						hasFetched = true;
					} catch (InterruptedException e) { // cancelled
						/* Consume */
					} catch (TrackGraphLoopException e) { // contains loop
						notifyListeners(loadListeners, MultitrackLoadListener.LOAD_FAILED_BAD_GRAPH, e, true);
						return;
						
					}
				} while (!hasFetched);	
				
				if (cancelRequested) {
					notifyListeners(loadListeners, 
							MultitrackLoadListener.LOAD_CANCELLED, 
							null, true);
					return;
				}
		
				// Was there anything to play... (or does the frame even exist?)
				if (rootODFrame == null) {
					notifyListeners(loadListeners, MultitrackLoadListener.NOTHING_TO_PLAY, null, true);
					return;
				}
				
				// Get the absolute layout of the track graph.... i.e. a flattened view with
				// all absolute initiation times stating from ms time 0.
				ABSTrackGraphRetreiver absTrackGraphRetreiver = new ABSTrackGraphRetreiver(
						rootODFrame, masterChannelID);
	
				try {
					SwingUtilities.invokeAndWait(absTrackGraphRetreiver);
				} catch (Exception e) {
					notifyListeners(loadListeners, MultitrackLoadListener.LOAD_FAILED_GENERIC, e, true);
					return;
				}
				
				if (cancelRequested) {
					notifyListeners(loadListeners, 
							MultitrackLoadListener.LOAD_CANCELLED, 
							null, true);
					return;
				}
				
				assert(absTrackGraphRetreiver.absGraph != null);
				
				// There are no track to play...
				if (absTrackGraphRetreiver.absGraph.isEmpty()) {
					notifyListeners(loadListeners, MultitrackLoadListener.NOTHING_TO_PLAY, null, true);
					return;
				}
				
				// First pass: get the list of all filenames to load...
				
				// Throw away tracks in the current track group that aren't in the new track group...
				// keeping tracks that are already loaded.
				// For the tracks that are kept ... re-create there overdubbed frame info
				List<Overdub> transferred = new LinkedList<Overdub>();
				
				synchronized(currentOverdubs) {
					
					// Important to chck while locking currentOverdubs since this thread could be
					// cancelled and another one of these threads loading and  wanting to also change the
					// current overdubs.
					if (cancelRequested) {
						notifyListeners(loadListeners, 
								MultitrackLoadListener.LOAD_CANCELLED, 
								null, true);
						return;
					}
	
					for (Overdub od : currentOverdubs) {
						
						for (int i = 0; i < absTrackGraphRetreiver.absGraph.size(); i++) {
							
							AbsoluteTrackNode absNode = absTrackGraphRetreiver.absGraph.get(i);
							
							if (od.getTrackModel().getLocalFilename().equals(absNode.getTrackNode().getLocalFilename())) {
								transferred.add(new Overdub(
										od.getTrackModel(), 
										absNode.getChannelID(), 
										AudioMath.millisecondsToFrames(absNode.getABSStartTime(),
												SampledAudioManager.getInstance().getDefaultPlaybackFormat())));
								absTrackGraphRetreiver.absGraph.remove(i);
								i --;
							}
	
						}
					}
					
					currentOverdubs.clear();
					currentOverdubs.addAll(transferred);
			
				}
				
				// Notify load handlers of transferred tracks
				for (Overdub od : transferred) {
					notifyListeners(loadListeners, MultitrackLoadListener.TRACK_LOADED, 
							od.getTrackModel().getLocalFilename(), true);
				}
				
				// Go through and load each track one by one.
				// Note: loadedTrackModels exludes transferred models because they have been dealt with
				// already. Maps Localfilename - > track model
				Map<String, SampledTrackModel> loadedTrackModels = new HashMap<String, SampledTrackModel>();
	
				// Load the tracks one by one... Update the track group incrementally
				for (AbsoluteTrackNode absNode : absTrackGraphRetreiver.absGraph) {
					
					SampledTrackModel stm = loadedTrackModels.get(absNode.getTrackNode().getLocalFilename());
					
					if (stm != null) {
						assert(stm.getLocalFilename().equals(absNode.getTrackNode().getLocalFilename()));
					} else { // must load / retreive from somewhere the new track model
						
						try {
							stm = TrackModelLoadManager.getInstance().load(
									AudioPathManager.AUDIO_HOME_DIRECTORY + absNode.getTrackNode().getLocalFilename(), 
									absNode.getTrackNode().getLocalFilename(), 
									new Observer() { // opps, my confusing design pattern gone wrong!
	
										public Subject getObservedSubject() {
											return null;
										}
	
										/**
										 * Cancel the load operation if a cancel request is opening
										 */
										public void modelChanged(Subject source, SubjectChangedEvent event) {
											assert(event.getID() == ApolloSubjectChangedEvent.LOAD_STATUS_REPORT);
											if (cancelRequested) {
												((AudioFileLoader)source).cancelLoad();
											}
										}
	
										public void setObservedSubject(Subject parent) {
										}
										
									}, // No need to observe
									true); // Search around in expeditee memory for the track
		
						} catch (Exception e) {
							e.printStackTrace();
							notifyListeners(loadListeners, 
									MultitrackLoadListener.TRACK_LOAD_FAILED_IO, 
									absNode.getTrackNode().getLocalFilename(),
									true);
							continue;
						} 
	
						// stm must only be null if a cancel was actually requested.
						assert(stm != null || (stm == null && cancelRequested));
						
						if (stm == null || cancelRequested) {
							notifyListeners(loadListeners, 
									MultitrackLoadListener.LOAD_CANCELLED, 
									null, true);
							return;
						}
						
					} 
					
					synchronized(currentOverdubs) {
						// Important to chck while locking currentOverdubs since this thread could be
						// cancelled and another one of these threads loading and  wanting to also change the
						// current overdubs.
						if (cancelRequested) {
							notifyListeners(loadListeners, 
									MultitrackLoadListener.LOAD_CANCELLED, 
									null, true);
							return;
						}
						currentOverdubs.add(new Overdub(stm, absNode.getChannelID(), AudioMath.millisecondsToFrames(absNode.getABSStartTime(),
								SampledAudioManager.getInstance().getDefaultPlaybackFormat())));
					}
					
					// Notify of loaded track
					notifyListeners(loadListeners, 
							MultitrackLoadListener.TRACK_LOADED, 
							absNode.getTrackNode().getLocalFilename(),
							true);
	
				} // load next track
				
				// It is possible that all of the loads failed that now left with
				// nothing to play... so do a check before continuing
				boolean isEmpty = false;
				synchronized(currentOverdubs) {
					isEmpty = currentOverdubs.isEmpty();
				}
				
				if (isEmpty) {
					notifyListeners(loadListeners, MultitrackLoadListener.NOTHING_TO_PLAY, null, true);
					return;
				}
				
				hasSucceeded = true;
				
				// Reset the cache now that playing (safety precaution)
				cacheExpiryCounter = CACHE_DEPTH;
				
			} finally {
				// Set flag for load currect state
				if (!hasSucceeded) loadFinished = true;
			}

			// Must commence on the swing thread
			SwingUtilities.invokeLater(new MultiTrackPlaybackCommencer());
		}
		
		/**
		 * Commences playback from the swing thread.
		 * @author Brook Novak
		 */
		private class MultiTrackPlaybackCommencer implements Runnable {


			public void run() {

				try {
					
					// Note: loadListeners etc.. won't change since this is inner class created from
					// parent...
					assert(loadListeners != null);
					assert(!loadListeners.isEmpty());
					
					// This is called from the loader thread once set to play. 
					// However during the time when the load thread scheduled this on the AWT Queue
					// and this actually running another AWT Event might have either directly started playback
					// OR began loading a new set of tracks... either all must discard this redundant request
					if (isPlaying() || MultiTrackPlaybackLoader.this != loaderThread || cancelRequested) {
						// Notice the reference compare with loaderThread: since there could be another
						// thread now starting... which implies that this should actually cancel!
						
						notifyListeners(loadListeners, 
								MultitrackLoadListener.LOAD_CANCELLED, 
								null, false);
				
						return; // discard redundant request
					}
				
				} finally {
					// Set load state for c
					loadFinished = true;
				}


				// Notify listener that load phase has completed
				notifyListeners(loadListeners, MultitrackLoadListener.LOAD_COMPLETE, null, false);
				
				// Remmember root frame of new playback
				currentODFrame = rootODFrame;

				commencePlayback(startFrame, endFrame, loadListeners);
				
	
				
			}
				
		}
		
		/**
		 * Retreives the overdub initiation times / channel ids from the swing thread.
		 * 
		 * @author Brook Novak
		 */
		private class ABSTrackGraphRetreiver implements Runnable {
			
			private String masterMixID;
			private OverdubbedFrame rootODFrame;
			private List<AbsoluteTrackNode> absGraph = null;
			
			ABSTrackGraphRetreiver(OverdubbedFrame rootODFrame, String masterMixID) {
				this.rootODFrame = rootODFrame;
				this.masterMixID = masterMixID;
			}
			
			public void run() {
				assert(rootODFrame != null);
				assert(masterMixID != null);
				absGraph = rootODFrame.getAbsoluteTrackLayoutDeep(masterMixID);
			}
		}
		
	}
	
	/**
	 * A callback interface.
	 * 
	 * @author Brook Novak
	 */
	public interface MultitrackLoadListener {
		
		/**
		 * Due to explicit cancel. New playback group request. 
		 * Graph Model change while loading (after fetch).
		 * Load/play operation aborted.
		 */
		public static final int LOAD_CANCELLED = 1;
		
		/**
		 * Failed due to IO issue. State = localfilename of failed track
		 * Load/play operation <b.not</b> aborted - it will play what it can, if 
		 * thereends up being nothing to play then evenetually a {@link #NOTHING_TO_PLAY}
		 * event will be raised.
		 */
		public static final int TRACK_LOAD_FAILED_IO = 2;
		
		/**
		 * Failed due to graph containing loop. State = loop exception
		 * Load/play operation aborted
		 */
		public static final int LOAD_FAILED_BAD_GRAPH = 6;
		
		/**
		 * Failed due to playback issue. State = exception
		 * Load/play operation aborted
		 */
		public static final int LOAD_FAILED_PLAYBACK = 3;
		
		/**
		 * All overdubs are loaded into memory and are about to play.
		 */
		public static final int LOAD_COMPLETE = 4;
		
		/**
		 * Load/play operation aborted because there is nothing to play.
		 */
		public static final int NOTHING_TO_PLAY = 7;
		
		/**
		 * Failed - generic case. State = exception
		 * Load/play operation aborted
		 */
		public static final int LOAD_FAILED_GENERIC = 8;
		
		/**
		 * A track has been loaded. State = localfilename
		 */
		public static final int TRACK_LOADED = 9;

		/**
		 * A callback method that <i>is invoked from the swing thread</i> 
		 * 
		 * @param id
		 * 		A code that describes the event being raised.
		 * 		For example {@link #TRACK_LOADED}
		 * 
		 * @param state
		 * 		Any state information passed. See id documentations for specific info. 
		 */
		public void multiplaybackLoadStatusUpdate(int id, Object state);

	}

	public OverdubbedFrame getCurrentODFrame() {
		return currentODFrame;
	}
	
	public String getCurrentMasterChannelID() {
		return masterChannelID;
	}
	

	

}
