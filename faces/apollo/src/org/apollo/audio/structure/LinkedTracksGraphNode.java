package org.apollo.audio.structure;

/**
 * Immutable.
 * 
 * @see {@link AudioStructureModel} for thread safe convention.
 * 
 * @author Brook Novak
 *
 */
public class LinkedTracksGraphNode extends AbstractTrackGraphNode {

	private OverdubbedFrame linkedFrame;
	
	private String virtualFilename; // immutable - assigned on construction
	
	/**
	 * 
	 * @param linkedFrame
	 * 
	 * @param virtualFilename
	 * 
  	 * @param initiationTime
  	 *		In milliseconds. Can be negative - relative.
  	 *
	 * @param name
	 * 		The name to set this to. Can be null
	 * 
	 * @throws NullPointerException
	 * 		if linkedFrame is null.
	 */
	LinkedTracksGraphNode(long initiationTime, OverdubbedFrame linkedFrame, String virtualFilename, String name, int ypixelPosition) {
		super(initiationTime, name, ypixelPosition);
		if (linkedFrame == null) throw new NullPointerException("linkedFrame");
		this.linkedFrame = linkedFrame;
		this.virtualFilename = virtualFilename;
	}
	
	public OverdubbedFrame getLinkedFrame() {
		return linkedFrame;
	}

	public String getVirtualFilename() {
		return virtualFilename;
	}

	/**
	 * @see OverdubbedFrame#calculateRunningTime()
	 * 
	 * @return
	 * 		The linked frames running time
	 */
	@Override
	public long getRunningTime() {
		return linkedFrame.calculateRunningTime();
	}
	
	
}
