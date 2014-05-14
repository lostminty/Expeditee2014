package org.apollo.audio.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apollo.audio.util.SoundDesk;
import org.apollo.mvc.AbstractSubject;
import org.apollo.util.Mutable;

/**
 * Immutable outside package.
 * 
 * @see {@link AudioStructureModel} for thread safe convention.
 * 
 * @author Brook Novak
 *
 */
public class OverdubbedFrame extends AbstractSubject {

	private String frameName; // immutable
	
	private Set<TrackGraphNode> tracks = new HashSet<TrackGraphNode>();
	
	private Set<LinkedTracksGraphNode> linkedTracks = new HashSet<LinkedTracksGraphNode>();
	
	/**
	 * 
	 * @param frameName
	 * 
	 * @throws NullPointerException
	 * 		if frameName is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		if frameName is empty.
	 */
	OverdubbedFrame(String frameName) {
		if (frameName == null) throw new NullPointerException("frameName");
		else if (frameName.length() == 0) throw new IllegalArgumentException("frameName.length() == 0");
		
		this.frameName = frameName;
	}
	
	/**
	 * @return
	 * 		The immutable framename for this overdubbed frame.
	 * 		Never null / empty.
	 */
	public String getFrameName() {
		return frameName;
	}

	/**
	 * @return 
	 * 		A copy of the linked tracks. Never null.
	 * 		All unique (from a set)
	 */
	public ArrayList<LinkedTracksGraphNode> getLinkedTracksCopy() {
		return new ArrayList<LinkedTracksGraphNode>(linkedTracks);
	}
	
	/**
	 * @return
	 * 		An unmodifiable set of all the linked tracks. Never null.
	 */
	public Set<LinkedTracksGraphNode> getUnmodifiableLinkedTracks() {
		return Collections.unmodifiableSet(linkedTracks);
	}

	/**
	 * @return 
	 * 		A copy of the tracks. Never null.
	 * 		All unique (from a set)
	 */
	public ArrayList<TrackGraphNode> getTracksCopy() {
		return new ArrayList<TrackGraphNode>(tracks);
	}
	
	/**
	 * @return
	 * 		An unmodifiable set of all the tracks. Never null.
	 */
	public Set<TrackGraphNode> getUnmodifiableTracks() {
		return Collections.unmodifiableSet(tracks);
	}
	
	/**
	 * Must be on swing thread.
	 * 
	 * @param localFilename
	 * 		The track to get. Must not be null.
	 * 
	 * @return
	 * 		The track with the given local filename. Null if does not exist.
	 * 
	 * @throws NullPointerException
	 * 		If localFilename is null.
	 */
	public TrackGraphNode getTrack(String localFilename) {
		if (localFilename == null) throw new NullPointerException("localFilename");
		
		for (TrackGraphNode track : tracks) {
			if (track.getLocalFilename().equals(localFilename)) {
				return track;
			}
		}
		
		return null;
	}
	
	/**
	 * Must be on swing thread.
	 * 
	 * @param virtualFilename
	 * 		The linked track to get. Must not be null.
	 * 
	 * @return
	 * 		The linked track with the given virtual filename. Null if does not exist.
	 * 
	 * @throws NullPointerException
	 * 		If virtualFilename is null.
	 */
	public LinkedTracksGraphNode getLinkedTrack(String virtualFilename) {
		if (virtualFilename == null) throw new NullPointerException("virtualFilename");
		
		for (LinkedTracksGraphNode ltrack : linkedTracks) {
			if (ltrack.getVirtualFilename().equals(virtualFilename)) {
				return ltrack;
			}
		}
		
		return null;
	}
	
	
	/**
	 * Must be on swing thread.
	 * Note: Only a shallow check.
	 * 
	 * @param localFilename
	 * 		The track to check for. Must not be null.
	 * 
	 * @return
	 * 		True if this OverdubbedFrame owns the track. False otherwise.
	 * 
	 * @throws NullPointerException
	 * 		If localFilename is null.
	 */
	public boolean containsTrack(String localFilename) {
		if (localFilename == null) throw new NullPointerException("localFilename");
		
		for (TrackGraphNode track : tracks) {
			if (track.getLocalFilename().equals(localFilename)) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Must be on swing thread.
	 * Note: Only a shallow check.
	 * 
	 * @param virtualFilename
	 * 		The linked track to check for. Must not be null.
	 * 
	 * @return
	 * 		True if this OverdubbedFrame owns the linked track. False otherwise.
	 * 
	 * @throws NullPointerException
	 * 		If virtualFilename is null.
	 */
	public boolean containsLinkedTrack(String virtualFilename) {
		if (virtualFilename == null) throw new NullPointerException("virtualFilename");
		
		for (LinkedTracksGraphNode ltrack : linkedTracks) {
			if (ltrack.getVirtualFilename().equals(virtualFilename)) {
				return true;
			}
		}
		
		return false;
	}
	
	void addTrack(TrackGraphNode track) {
		assert(track != null);
		tracks.add(track);
	}
	
	void addAllTracks(Collection<TrackGraphNode> track) {
		assert(track != null);
		tracks.addAll(track);
	}
	
	void addLinkedTrack(LinkedTracksGraphNode linkedTrack) {
		assert(linkedTrack != null);
		linkedTracks.add(linkedTrack);
	}
	
	void addAllLinkedTracks(Collection<LinkedTracksGraphNode> linkedTracks) {
		assert(linkedTracks != null);
		linkedTracks.addAll(linkedTracks);
	}
	
	boolean removeTrack(TrackGraphNode track) {
		assert(track != null);
		return tracks.remove(track);
	}
	
	TrackGraphNode removeTrack(String localFilename) {
		assert(localFilename != null);
		
		TrackGraphNode track = getTrack(localFilename);
		
		if (track != null && tracks.remove(track)) return track;
		
		return null;
	}
	
	boolean removeLinkedTrack(LinkedTracksGraphNode ltrack) {
		assert(ltrack != null);
		return linkedTracks.remove(ltrack);
	}
	
	/**
	 * @see #trackExistsDeep
	 * 
	 * @return
	 * 		True is there is no tracks or linked tracks.
	 */
	public boolean isEmpty() {
		
		return linkedTracks.isEmpty() && tracks.isEmpty();
	}
	
	/**
	 * Checks if there are any tracks on the frame
	 * 
	 * @see #trackExistsDeep()
	 * 
	 * @return
	 * 		True if there exists a track (NOT LINKED-TRACK) in this frame
	 */
	public boolean trackExists() {
		return !tracks.isEmpty();
	}
	
	/**
	 * Intention: to see if this overdub frame contains anything to play.
	 * 
	 * @return
	 * 		True if there exists a track (NOT LINKED-TRACK) in this frame or its
	 * 		descendant frames.
	 */
	public boolean trackExistsDeep() {
		return doesTrackExist(new HashSet<OverdubbedFrame>());
	}
	
	private boolean doesTrackExist(Set<OverdubbedFrame> visited) {
		if (!tracks.isEmpty()) return true;
		else if (visited.contains(this)) return false;
		
		// Remember this node
		visited.add(this);

		// Recurse step
		for (LinkedTracksGraphNode linkedTrack : linkedTracks) {
			if (linkedTrack.getLinkedFrame().doesTrackExist(visited)) 
				return true;
		}
		
		return false;
	}
	
	/**
	 * Gets a child frame with the given name... if exists / is reachable from this node.
	 * 
	 * @param targetFrameName
	 * 		THe target frame to get.
	 * 
	 * @throws NullPointerException
	 * 		If targetFrameName is null.
	 * 
	 * @return
	 * 		The target OverdubbedFrame. Null if none existed ...
	 */
	public OverdubbedFrame getChild(String targetFrameName) {
		return getChild(targetFrameName, new HashSet<OverdubbedFrame>());
	}
	
	private OverdubbedFrame getChild(String targetFrameName, Set<OverdubbedFrame> visited) {
		if(targetFrameName == null) throw new NullPointerException("targetFrameName");

		// Base cases
		if (targetFrameName.equalsIgnoreCase(frameName)) return this;
		else if (visited.contains(this)) return null;
		
		// Remember this node
		visited.add(this);
		
		// Recurse step
		for (LinkedTracksGraphNode linkedTrack : linkedTracks) {
			OverdubbedFrame odFrame = linkedTrack.getLinkedFrame().getChild(targetFrameName, visited);
			if (odFrame != null) return odFrame;
		}
		
		// Reached end of this nodes links - found no match
		return null;
	}
	

	/**
	 * Recursivly calculates the total running time for this frame.
	 * 
	 * @return
	 * 		The running time in milliseconds. Always positive. Zero if this frame has no tracks/linked tracks.
	 */
	public long calculateRunningTime() {
		return calculateRunningTime(new HashMap<OverdubbedFrame, Long>());
	}
	
	/**
	 * Calculates the running time of this overdubbed frame in milliseconds.
	 * Avoids infinit recursion.
	 * 
	 * @param visited
	 * 		The visitive nodes
	 * 
	 * @return
	 * 		The running time. Always positive. Zero if this frame has no tracks/linked tracks.
	 */
	private long calculateRunningTime(Map<OverdubbedFrame, Long> visited) {
		
		Long rt = visited.get(this);
		if (rt != null) { // already considered - re-use value
			return rt;
		}
		
		long runTime = 0;
		
		if (!tracks.isEmpty() || !linkedTracks.isEmpty()) {
			
			// Calculate the running time for this frame
			long initTime = getFirstInitiationTime();
			long relativeEndTime = Long.MIN_VALUE;
			
			for (TrackGraphNode track : tracks) {
				long ret = track.getInitiationTime() + track.getRunningTime();
				if (ret > relativeEndTime) relativeEndTime = ret;
			}
	
			for (LinkedTracksGraphNode linkedTrack : linkedTracks) {
				
				long ret = linkedTrack.getInitiationTime() + 
					linkedTrack.getLinkedFrame().calculateRunningTime(visited);
				
				if (ret > relativeEndTime) relativeEndTime = ret;
			}
	
			assert (initTime <= relativeEndTime);
			
			runTime = relativeEndTime - initTime;
			
		}
		
		// Save this ODFrames rt
		visited.put(this, new Long(runTime));
		
		return runTime;
	}
	
	/**
	 * 
	 * @return
	 * 		The first initiation time of a track widget or track link on this frame.
	 * 		Note that these are relative and can be negative.
	 * 
	 * 		Defaults to zero if there are no tracks/track links on this frame.
	 * 
	 */
	public long getFirstInitiationTime() { // relative to this frame - i.e. first initiation time on this frame
		
		long smallest = 0;
		boolean hasSet = false;
		
		for (LinkedTracksGraphNode linkedTrack : linkedTracks) {
			if (!hasSet || linkedTrack.getInitiationTime() < smallest) {
				smallest = linkedTrack.getInitiationTime();
				hasSet = true;
			}
		}
		
		for (TrackGraphNode track : tracks) {
			if (!hasSet || track.getInitiationTime() < smallest) {
				smallest = track.getInitiationTime();
				hasSet = true;
			}
		}
		
		return smallest;
	}

	@Override
	public String toString() {
		return frameName;
	}
	
	/**
	 * @return
	 * 		All the TrackGraphInfo's in this frames descendants, including this frame.
	 * 		Never null. Can of coarse be empty.
	 */
	public List<TrackGraphNode> getTracksDeep() {
		List<TrackGraphNode> res = new LinkedList<TrackGraphNode>();
		allAddTracksDeep(res, new HashSet<OverdubbedFrame>());
		return res;
	}
	
	private void allAddTracksDeep(List<TrackGraphNode> tlist, Set<OverdubbedFrame> visited) {
		if (visited.contains(this)) return; // already considered
		visited.add(this);
		
		tlist.addAll(tracks);

		for (LinkedTracksGraphNode linkedTrack : linkedTracks) {
			linkedTrack.getLinkedFrame().allAddTracksDeep(tlist, visited);
		}
	}
	
	/**
	 * @param masterMixID
	 * 		The master mix for all the absolute tacks nodes to use. Must not be null or empty.
	 * 
	 * @return
	 * 		The list of tracks and their ABS times according to <i>this frame</i>
	 * 		starting from ms time 0. Never null - can be empty. The tracks are ordered
	 * 		ascending by virtual y position
	 */
	public List<AbsoluteTrackNode> getAbsoluteTrackLayoutDeep(String masterMixID) {
		assert(masterMixID != null);
		assert(masterMixID.length() > 0);
		
		List<AbsoluteTrackNode> res = new LinkedList<AbsoluteTrackNode>();
		Stack<String> virtualPath = new Stack<String>();
		virtualPath.add(masterMixID);
		
		addAbsTrackNode(res, 0, new Stack<OverdubbedFrame>(), virtualPath, Mutable.createMutableInteger(0));
		
		return res;
	}
	
	private void addAbsTrackNode(
			List<AbsoluteTrackNode> abslist, 
			long currentTime, 
			Stack<OverdubbedFrame> visited,
			Stack<String> virtualPath,
			Mutable.Integer currentVirtualYPosition) {
		
		assert(currentTime >= 0);
		
		// Base case
		if (visited.contains(this)) return; // already considered - loop safety
		
		visited.push(this);
		
		LinkedList<AbstractTrackGraphNode> ordered = new LinkedList<AbstractTrackGraphNode>();
		LinkedList<AbstractTrackGraphNode> yOrderedTracks = new LinkedList<AbstractTrackGraphNode>(tracks);
		LinkedList<AbstractTrackGraphNode> yOrderedLinkedTracks = new LinkedList<AbstractTrackGraphNode>(linkedTracks);
		
		// Order all linked/non-linked tracks by Y-position
		Collections.sort(yOrderedTracks, new YPixelComparator<AbstractTrackGraphNode>());
		
		// Order all linked/non-linked tracks by Y-position
		Collections.sort(yOrderedLinkedTracks, new YPixelComparator<AbstractTrackGraphNode>());
		
		Iterator<AbstractTrackGraphNode> trackItor = yOrderedTracks.iterator();
		Iterator<AbstractTrackGraphNode> linkTrackItor = yOrderedLinkedTracks.iterator();
		
		// Interleave ordered tracks into one consolidated ordered colection
		while(trackItor.hasNext() || linkTrackItor.hasNext()) {
			if (trackItor.hasNext() && linkTrackItor.hasNext()) {
				
				AbstractTrackGraphNode t = trackItor.next();
				AbstractTrackGraphNode lt = linkTrackItor.next();
				
				if (t.getYPixelPosition() < lt.getYPixelPosition()) {
					ordered.add(t); ordered.add(lt);
				} else {
					ordered.add(lt); ordered.add(t);
				}
				
			} else if (trackItor.hasNext()) {
				ordered.add(trackItor.next());
			} else {
				ordered.add(linkTrackItor.next());
			}
		}

		assert(ordered.size() == (tracks.size() + linkedTracks.size()));
		
		// Get offset - align all tracks on this frame to first track/linked track...
		long firstInitTime = getFirstInitiationTime();
		
		for (AbstractTrackGraphNode node : ordered) {
			
			// Increase the virtual Y position - in an emulated call-by-reference fashion.
			currentVirtualYPosition.value ++;
		
			if (node instanceof TrackGraphNode) {
				TrackGraphNode tnode = (TrackGraphNode)node;
				abslist.add(new AbsoluteTrackNode(
						tnode, 
						currentTime + (tnode.getInitiationTime() - firstInitTime), 
						SoundDesk.createIndirectLocalChannelID(
								virtualPath, tnode.getLocalFilename()),
						frameName, currentVirtualYPosition.value));
			} else {
				
				LinkedTracksGraphNode ltnode = (LinkedTracksGraphNode)node;
				
				// Recurse: with a new current time according to the linked track init time
				virtualPath.push(ltnode.getVirtualFilename()); // build vpath
				
				ltnode.getLinkedFrame().addAbsTrackNode(
						abslist, 
						currentTime + (ltnode.getInitiationTime() - firstInitTime),
						visited,
						virtualPath,
						currentVirtualYPosition);
				
				virtualPath.pop(); // maintain vpath
			}
		}

		visited.pop();
	}
	
//	private void addAbsTrackNode(
//			List<AbsoluteTrackNode> abslist, 
//			long currentTime, 
//			Stack<OverdubbedFrame> visited,
//			Stack<String> virtualPath) {
//		
//		assert(currentTime >= 0);
//		
//		// Base case
//		if (visited.contains(this)) return; // already considered - loop safety
//		
//		visited.push(this);
//		
//		// Get offset - align all tracks on this frame to first track/linked track...
//		long firstInitTime = getFirstInitiationTime();
//		
//		// Add track info and init times for this frame
//		for (TrackGraphNode tnode : tracks) {
//			abslist.add(new AbsoluteTrackNode(
//					tnode, 
//					currentTime + (tnode.getInitiationTime() - firstInitTime), 
//					SoundDesk.createIndirectLocalChannelID(
//							virtualPath, tnode.getLocalFilename()),
//					frameName));
//		}
//
//		// Recurse: with a new current time according to the linked track init time
//		for (LinkedTracksGraphNode linkedTrack : linkedTracks) {
//			virtualPath.push(linkedTrack.getVirtualFilename()); // build vpath
//			
//			linkedTrack.getLinkedFrame().addAbsTrackNode(
//					abslist, 
//					currentTime + (linkedTrack.getInitiationTime() - firstInitTime),
//					visited,
//					virtualPath);
//			
//			virtualPath.pop(); // maintain vpath
//		}
//		
//		visited.pop();
//	}
	
	/**
	 * Gets all frame positions that occur in a target child frame, from this frame...
	 * <b>WARNING</b>: Infinite recursion if contains loops in structure.
	 * 
	 * @param childFrameName
	 * 		Must not be null. The frame to get all the translated frame positions 
	 * 
	 * @param relativeMSPosition
	 * 		A relative ms position for this frame
	 * 
	 * @return
	 * 		A list of ms positions that have been translated ... empty
	 * 		if there are no occurances.
	 */
	public List<Integer> getMSPositions(String childFrameName, long relativeMSPosition) {
		assert(childFrameName != null);
		assert(childFrameName.length() > 0);
		
		List<Integer> positions = new LinkedList<Integer>();
		
		addMSPosition(
				positions, 
				childFrameName, 
				relativeMSPosition, 
				new HashMap<OverdubbedFrame, CachedODFrameInfo>());
		
		return positions;
	
	}

	private class CachedODFrameInfo {
		
		long runningTime;
		long firstInitTime;
		
		public CachedODFrameInfo(long runningTime, long firstInitTime) {
			this.runningTime = runningTime;
			this.firstInitTime = firstInitTime;
		}
	}
	
	/**
	 * <b>WARNING</b>: Infinite recursion if contains loops in structure.
	 * A better safety Solution: Keep track of all virtual paths - NOT FRAMES
	 * 
	 * @param msPositions
	 * @param childFrameName
	 * @param currentRelativePosition
	 * @param cachedCalcs
	 * @param visited
	 */
	private void addMSPosition(
			List<Integer> msPositions, 
			String childFrameName, 
			long currentRelativePosition,
			Map<OverdubbedFrame, CachedODFrameInfo> cachedCalcs) {
		
		// Avoid recomputation of calcs
		CachedODFrameInfo calcCache = cachedCalcs.get(this);
		
		if (calcCache == null) {
			
			calcCache = new CachedODFrameInfo(
					calculateRunningTime(),
					getFirstInitiationTime());
			
			cachedCalcs.put(this, calcCache);
			
		}
		
		assert(calcCache != null);

		// Is requested frame position in range of this overdub frame?
		if (childFrameName.equalsIgnoreCase(frameName) &&
				currentRelativePosition >= calcCache.firstInitTime 
				&& currentRelativePosition <= (calcCache.firstInitTime + calcCache.runningTime)) {
			msPositions.add(new Integer((int)(currentRelativePosition - calcCache.firstInitTime)));
		}

		// Recurse - even if re-visiting visited frames... risky ... will
		// get stack overflow if the structure contains loops
		for (LinkedTracksGraphNode linkedTrack : linkedTracks) {
			
			CachedODFrameInfo linkedFramesCachedCalc = cachedCalcs.get(linkedTrack.getLinkedFrame());
			
			if (linkedFramesCachedCalc == null) {
				linkedFramesCachedCalc = new CachedODFrameInfo(
						linkedTrack.getLinkedFrame().calculateRunningTime(),
						linkedTrack.getLinkedFrame().getFirstInitiationTime());
				
				cachedCalcs.put(linkedTrack.getLinkedFrame(), linkedFramesCachedCalc);
			}
			
			// Only recuse down a link if the cuurent position is over top of the link
			if (currentRelativePosition >= linkedTrack.getInitiationTime() &&
					currentRelativePosition <= (linkedTrack.getInitiationTime() + linkedFramesCachedCalc.runningTime)) {
		
				linkedTrack.getLinkedFrame().addMSPosition(
					msPositions, 
					childFrameName, 
					linkedFramesCachedCalc.firstInitTime + (currentRelativePosition - linkedTrack.getInitiationTime()),
					cachedCalcs);
			}
		}

	}
	
	/**
	 * 
	 * @param framename
	 * 		The child frame
	 * 
	 * @return
	 * 		The initiation time of a child frame with respect to the first initiation time of
	 * 		this frame.
	 * 		Null if child not reachable from this frame.
	 */
	public Mutable.Long getChildFrameInitiationTime(String childFramename) {
		
		Mutable.Long deltaInitTime = Mutable.createMutableLong(0);
		
		if (findChildFrameInitiationTime(
				childFramename, 0, 
				deltaInitTime, new HashSet<OverdubbedFrame>())) {
		
			return deltaInitTime;
		}
		
		return null;

	}
	
	private boolean findChildFrameInitiationTime(
			String childFramename, 
			long currentInitiationTime, 
			Mutable.Long childInitTime,
			HashSet<OverdubbedFrame> visited) {
		
		if (visited.contains(this)) return false;
		visited.add(this);
		
		if (frameName.equalsIgnoreCase(childFramename)) {
			childInitTime.value = currentInitiationTime;
			return true;
		}
		
		long firstInitTime = getFirstInitiationTime();
		
		for (LinkedTracksGraphNode linkedTrack : linkedTracks) {
			
			if (linkedTrack.getLinkedFrame().findChildFrameInitiationTime(
					childFramename,
					currentInitiationTime + (linkedTrack.getInitiationTime() - firstInitTime), 
					childInitTime,
					visited))
				return true;
			
		}
		
		return false;
		
	}
	
	
	
	private class YPixelComparator<T> implements Comparator<AbstractTrackGraphNode> {

		public int compare(AbstractTrackGraphNode o1, AbstractTrackGraphNode o2) {
			int y1 = o1.getYPixelPosition();

			int y2 = o2.getYPixelPosition();

			if( y1 > y2 ) return 1;
			else if( y1 < y2 ) return -1;

			return 0;
		}
		
	}
	
	
}
