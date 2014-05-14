package org.apollo.audio.structure;

/**
 * Immutable outside package.
 * 
 * @see {@link AudioStructureModel} for thread safe convention.
 * 
 * @author Brook Novak
 *
 */
public class TrackGraphNode extends AbstractTrackGraphNode {

	private String localFilename; // immutable

	private long runningTime; // ms
	
	/**
	 * Friendly constructor.
	 * 
	 * @param localFilename
	 * 		Must not be null or empty.
	 * 
	 * @param initiationTime
	 * 		Must be positive. In milliseconds
	 * 
	 * @param runningMS
	 * 		Must be larger than zero. In milliseconds.
	 * 
	 * @param name
	 * 		The name to set this to. Can be null
	 * 
	 * @param ypixelPosition
	 * 		The y pixel positon of the track in a frame
	 * 
	 * @throws NullPointerException
	 * 		if localFilename is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		if ms is smaller or equal to zero.
	 * 		or if localFilename is empty.
	 * 		or if initiationTime is less than zero
	 */
	TrackGraphNode(long initiationTime, String localFilename, long runningMS, String name, int ypixelPosition) {
		super(initiationTime, name, ypixelPosition);
		
		if (localFilename == null) throw new NullPointerException("localFilename");
		else if (localFilename.length() == 0) throw new IllegalArgumentException("localFilename.length() == 0");
		
		setRunningTime(runningMS);
		
		this.localFilename = localFilename;
	}
	
	
	public String getLocalFilename() {
		return localFilename;
	}

	/**
	 * Precomputed and maintained by the graph model.
	 * Always larger or equal to zero.
	 * 
	 * @return 
	 * 		The running time in milliseconds.
	 */
	@Override
	public long getRunningTime() {
		return runningTime;
	}
	
	/**
	 * 
	 * @param runningMS
	 * 		Must be larger than zero. IN milliseconds
	 * 
	 * @throws IllegalArgumentException
	 * 		if ms is smaller or equal to zero.
	 * 
	 */
	void setRunningTime(long runningMS) {
		if (runningMS <= 0) throw new IllegalArgumentException("runningMS = " + runningMS);
		this.runningTime = runningMS;
	}

}
