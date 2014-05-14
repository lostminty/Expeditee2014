package org.apollo.util;

import javax.sound.sampled.AudioFormat;

/**
 * Misc audio routines.
 * 
 * @author Brook Novak
 *
 */
public class AudioMath {
	private AudioMath() {}
    
	public static float volToDb(float volume) {
		return (float) (Math.log(volume==0.0 ? 0.0001 : volume) / Math.log(10.0) * 20.0);
	}

	public static float dBToVol(float dB) {
		return (float) Math.pow(10.0, dB / 20.0);
	}
	
	/**
	 * 
	 * @param ms
	 * @param format
	 * @return How many frames are in the given ms time
	 */
	public static int millisecondsToFrames(long ms, AudioFormat format) {
		double tmp = ms * format.getFrameRate();
		tmp /= 1000.0;
		return (int)tmp;
	}
	
	public static long framesToMilliseconds(long frames, AudioFormat format) {
		double tmp = frames * 1000.0;
		tmp /= (double)format.getFrameRate();
		return (long)tmp;
	}
	
	public static int framesToSeconds(long frames, AudioFormat format) {
		return (int)(frames / format.getFrameRate());
	}
}
