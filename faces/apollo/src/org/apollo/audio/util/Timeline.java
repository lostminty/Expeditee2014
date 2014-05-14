package org.apollo.audio.util;

/**
 * 
 * A Timeline for a Frame contains two componants:
 * 
 * <ul>
 * 		<li>The initation point: This is the smallest initiation time 
 * 			<b>(not neccessarily zero)</b> in the frame - at its X pixel.
 * 		<li>The end point: This is how long the frame runs for - and the
 * 			x-pixel position of the last played track in the frame.
 * </ul>
 * 
 * They are simple imutable objects thus are not kept consistant with model.
 * 
 * @author Brook Novak
 *
 */
public class Timeline {
	
	private long firstInitiationTime;
	private long runningTime;
	private int initiationXPixel;
	private int pixelWidth;
	
	/**
	 * 
	 * @param firstInitiationTime
	 * 
	 * @param runningTime
	 * 		Must be larger than zero.
	 * 
	 * @param initiationXPixel
	 * 		Relative can be anything
	 * 
	 * @param pixelWidth
	 * 		Must be larger than zero
	 * 
	 */
	public Timeline(long firstInitiationTime, long runningTime, int initiationXPixel, int pixelWidth) {
		assert(runningTime > 0);
		assert(pixelWidth > 0);
		
		this.firstInitiationTime = firstInitiationTime;
		this.runningTime = runningTime;
		this.initiationXPixel = initiationXPixel;
		this.pixelWidth = pixelWidth;
	}


	public long getFirstInitiationTime() {
		return firstInitiationTime;
	}


	public int getInitiationXPixel() {
		return initiationXPixel;
	}


	public long getRunningTime() {
		return runningTime;
	}
	
	public int getPixelWidth() {
		return pixelWidth;
	}
	
	/**
	 * @return
	 * 		The amount of time in milliseconds per pixel.
	 */
	public double getTimePerPixel() {
		double msPerPixel = runningTime;
		return msPerPixel /= (double)pixelWidth;
	}

	/**
	 * @return
	 * 		The amount of time in milliseconds per pixel.
	 */
	public double getPixelPerTime() {
		double pxPerMS = (double)pixelWidth;
		return pxPerMS /= runningTime;
	}
	
	/**
	 * Calcautes the a time in milliseconds from a given x-pixel position.
	 * 
	 * @param x
	 * 		The X pixel position.
	 * 
	 * @return
	 * 		An extrapolated time value. <B>CAN BE NEGATIVE</B> - if the x position is to the left
	 * 		of the initiation x pixel and happens to be far enough from the pixel such 
	 * 		that the ms-diff is larget than the initiation time.
	 */
	public long getMSTimeAtX(int x) {
		double msPerPixel = getTimePerPixel();
		return firstInitiationTime + (long)(msPerPixel * (x - initiationXPixel));
	}
	
	/**
	 * Calculates the x-pixel position in a timeline from a given ms time.
	 * 
	 * @param time
	 * 		The time in ms.
	 * 
	 * @return
	 * 		The x pixel.
	 */
	public int getXAtMSTime(long time) {
		
		long diffTime = time - firstInitiationTime;
		
		return initiationXPixel + (int)(diffTime / getTimePerPixel());
	}


	@Override
	public String toString() {
		return 	"firstInitiationTime=" + firstInitiationTime
				+ ", runningTime=" + runningTime
				+ ", initiationXPixel=" + initiationXPixel
				+ ", pixelWidth=" + pixelWidth;
	}
	
	

}