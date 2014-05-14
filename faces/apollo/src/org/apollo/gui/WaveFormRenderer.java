package org.apollo.gui;

/**
 * Provides waveform rendering services.
 * 
 * @author Brook Novak
 */
public interface WaveFormRenderer {

	/**
	 * Renders waveforms in a given array of audio samples - producing an array of wave-form 
	 * amplitutes ranging for -1.0 to 1.0.
	 * 
	 * A single height is calculated for one or more frames, which is specified by the
	 * aggregationSize. The way in which waveforms are rendered is implementation dependant.
	 * 
	 * @param audioBytes
	 * 		The array of pure samples.
	 * 
	 * @param startFrame
	 * 		The starting frame to begin rendering
	 * 
	 * @param frameLength
	 * 		The amount of frames to consider for rendering.
	 * 
	 * @param aggregationSize
	 * 		The amout of frames to aggregate.
	 * 
	 * @return 
	 * 		An array of wave-form amplitutes ranging for -1.0 to 1.0.
	 * 		Note that this will be empty if aggregationSize > frameLength.
	 */
	public float[] getSampleAmplitudes(byte[] audioBytes, int startFrame, int frameLength, int aggregationSize);
}
