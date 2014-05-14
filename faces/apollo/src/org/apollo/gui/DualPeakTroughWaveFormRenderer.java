package org.apollo.gui;

import javax.sound.sampled.AudioFormat;

import org.apollo.audio.SampledAudioManager;

/**
 * A WaveFormRenderer where the peaks and troughs are always chosen for every chunk of aggregated frames.
 * 
 * @author Brook Novak
 *
 */
public class DualPeakTroughWaveFormRenderer implements WaveFormRenderer {
	
	private int sampleSize;
	private boolean isBigEndian;
	private boolean isSigned;
	
	/**
	 * Constructor.
	 * 
	 * @param audioFormat
	 * 			The format of the audio bytes to be rendered.
	 * 
	 * @throws NullPointerException
	 * 			If audio format is null.
	 * 
	 * @throws IllegalArgumentException
	 * 			If audioformat is not supported. See SampledAudioManager.isFormatSupportedForPlayback
	 */
	public DualPeakTroughWaveFormRenderer(AudioFormat audioFormat) {
		if (audioFormat == null) throw new NullPointerException("audioFormat");
		
		if (!SampledAudioManager.getInstance().isFormatSupportedForPlayback(audioFormat)) 
			throw new IllegalArgumentException();

		sampleSize = audioFormat.getSampleSizeInBits();
		isSigned = audioFormat.getEncoding().toString().startsWith("PCM_SIGN");
		isBigEndian = audioFormat.isBigEndian();

	}

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
	 * 		If aggregationSize is one, then the returned array should be rendered
	 * 		as joint lines. Otherwise and implicit 2D array is returned: where
	 * 		the peak is an even index and a trough is a odd index (elements are
	 * 		interleaves in rendering order)
	 */
	public float[] getSampleAmplitudes(byte[] audioBytes, int startFrame, int frameLength, int aggregationSize) {
		assert(audioBytes != null);
		assert(startFrame >= 0);
		assert((startFrame + frameLength) <= (audioBytes.length / (sampleSize / 8)));
		
		int aggregationCount = frameLength / aggregationSize;
		
		float[] amplitudes = (aggregationSize == 1) ? 
			new float[aggregationCount] :
				new float[aggregationCount * 2];
		
		if (sampleSize == 16) {
					 
			for (int i = 0; i < aggregationCount; i++) {
				
				int max = 0, min = 0, sample; // could use short, but int avoids casting everywhere
				
				int startFrameIndex = (startFrame + (i * aggregationSize)) << 1;
				int endFrameIndex = startFrameIndex + (aggregationSize << 1);
				
				for (int k = startFrameIndex; k < endFrameIndex; k+=2) {
			
					int lsb, msb;
								 
					if (isBigEndian) {
						
						// First byte is MSB (high order)
						msb = (int)audioBytes[k];
						 
						 // Second byte is LSB (low order)
						lsb = (int)audioBytes[k + 1];
					
					 } else {
						// First byte is LSB (low order)
						lsb = (int)audioBytes[k];
						 
						// Second byte is MSB (high order)
						msb = (int)audioBytes[k + 1];
					}
					
					sample = (msb << 0x8) | (0xFF & lsb);

					if (sample > max)
						max = sample;
					else if (sample < min) 
						min = sample;

				}
				
				if (aggregationSize == 1) {
					amplitudes[i] = ((float)max) / 32768.0f;
				} else {
					amplitudes[(2 * i)] = ((float)max) / 32768.0f;
					amplitudes[(2 * i) + 1] = ((float)min) / 32768.0f;
				}
				
			}

		} else if (sampleSize == 8) {
			
			if (isSigned) {
		  
				// Find the peak within the block of aggregated frames
				for (int i = 0; i < amplitudes.length; i++) {
							
					byte max = 0, absmax = -1, sample, abssample;

					int startFrameIndex = startFrame + (i * aggregationSize);
					int endFrameIndex = startFrameIndex + aggregationSize;
					
					for (int k = startFrameIndex; k < endFrameIndex; k++) {
						
						sample = audioBytes[k];
						abssample = (sample < 0) ? (byte)(sample * -1) : sample;
						
						if (abssample > absmax) {
							max = sample;
							absmax = abssample;
						}
					}

					amplitudes[i] = ((float)max) / 128.0f;
					
				}

			} else { // unsigned

				// Find the peak within the block of aggregated frames
				for (int i = 0; i < amplitudes.length; i++) {
							
					int max = 0, absmax = -1, sample, abssample; // could use short, but int avoid casting everywhere

					int startFrameIndex = startFrame + (i * aggregationSize);
					int endFrameIndex = startFrameIndex + aggregationSize;
					
					for (int k = startFrameIndex; k < endFrameIndex; k++) {
						
						sample = (audioBytes[k] & 0xFF) - 128;
						abssample = Math.abs(sample);
						
						if (abssample > absmax) {
							max = sample;
							absmax = abssample;
						}
					}

					amplitudes[i] = ((float)max) / 128.0f;
					
				}
				
			}
			    		 
		}

		return amplitudes;
	}

}
