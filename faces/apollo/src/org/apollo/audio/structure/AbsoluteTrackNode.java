package org.apollo.audio.structure;

/**
 * An immutable class that points to a TrackGraphInfo and gives
 * a absolute initiation time.
 * 
 * @see OverdubbedFrame#getAbsoluteTrackLayoutDeep()
 * 
 * @author Brook Novak
 *
 */
public class AbsoluteTrackNode {
	
	private TrackGraphNode tnode;
	private long absStartTime;
	private String channelID;
	private String parentFrameName;
	private int virtualY;
	
	AbsoluteTrackNode(TrackGraphNode tnode, long absStartTime, String channelID, String parentFrameName, int virtualY) {
		assert(tnode != null);
		assert(channelID != null);
		assert(absStartTime >= 0);
		assert(parentFrameName != null);
		
		this.tnode = tnode;
		this.absStartTime = absStartTime;
		this.channelID = channelID;
		this.parentFrameName = parentFrameName;
		this.virtualY = virtualY;
	}

	public long getABSStartTime() {
		return absStartTime;
	}

	public TrackGraphNode getTrackNode() {
		return tnode;
	}
	
	public String getChannelID() {
		return channelID;
	}
	
	public String getParentFrameName() {
		return parentFrameName;
	}

	/**
	 * Virtual Y is the tracks Y position in an abosulute flat version of a heirarchical structure
	 * 
	 * @return
	 * 		The virtual Y value
	 */
	public int getVirtualY() {
		return virtualY;
	}
	
	public String toString() {
		return parentFrameName + ":" + tnode.getName();
	}
}
