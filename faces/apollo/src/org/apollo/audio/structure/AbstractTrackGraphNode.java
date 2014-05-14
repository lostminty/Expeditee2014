package org.apollo.audio.structure;

/**
 * Immutable outside package.
 * 
 * @see {@link AudioStructureModel} for thread safe convention.
 * 
 * @author Brook Novak
 *
 */
public abstract class AbstractTrackGraphNode {

	private long initiationTime;
	
	private String name = null;
	
	private int ypixelPosition;
	
	/**
	 * Constructor.
	 * 
	 * @param initiationTime
	 * 		In milliseconds. Can be negative - relative.
	 * 
	 * @param name
	 * 		The name to set this to. Can be null
	 * 
	 * @param ypixelPosition
	 * 		The y-pixel position of the track on the frame
	 */
	protected AbstractTrackGraphNode(long initiationTime, String name, int ypixelPosition) {
		setInitiationTime(initiationTime);
		this.name = name;
		this.ypixelPosition = ypixelPosition;
	}

	/**
	 * The start time in milliseconds that the track begins to play.
	 * This start time is relative to the tracks parent frame - and can be
	 * expressed in a negative time.
	 * 
	 * @return
	 * 		The relative initiation time.
	 */
	public long getInitiationTime() {
		return initiationTime;
	}

	/**
	 * @param initiationTime
	 * 		In milliseconds. Can be negative - relative.
	 * 
	 */
	void setInitiationTime(long initiationTime) {
		this.initiationTime = initiationTime;
	}
	
	public abstract long getRunningTime();

	/**
	 * @return
	 * 		The name given to this track. This can be null. 
	 * 		It is not gauranteed to be unique.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 * 		The name to set this to. Can be null
	 */
	void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return
	 * 		The Y-pixel positition of the track (spatial qaility)
	 */
	public int getYPixelPosition() {
		return ypixelPosition;
	}

	void setYPixelPosition(int val) {
		ypixelPosition = val;
	}
	
}
