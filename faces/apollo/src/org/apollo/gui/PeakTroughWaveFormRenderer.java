package org.apollo.gui;

import javax.sound.sampled.AudioFormat;

import org.apollo.audio.SampledAudioManager;

/**
 * A WaveFormRenderer where the peaks and troughs are always chosen for every chunk of aggregated frames.
 * 
 * @author Brook Novak
 *
 */
public class PeakTroughWaveFormRenderer implements WaveFormRenderer {
	
	private static final int DEFAULT_PEAK_TOLERANCE = 20;

	private int sampleSize;
	private boolean isBigEndian;
	private boolean isSigned;
	
	// Alternate
	private boolean lastSampleHigh = false;
	
	/** When frames becomes more aggregated - the selected peaks are alternated evenly
	 *  with a given tolerance*/
	private int peakTolerance;

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
	public PeakTroughWaveFormRenderer(AudioFormat audioFormat) {
		if (audioFormat == null) throw new NullPointerException("audioFormat");
		
		if (!SampledAudioManager.getInstance().isFormatSupportedForPlayback(audioFormat)) 
			throw new IllegalArgumentException();

		sampleSize = audioFormat.getSampleSizeInBits();
		isSigned = audioFormat.getEncoding().toString().startsWith("PCM_SIGN");
		isBigEndian = audioFormat.isBigEndian();
		
		peakTolerance = DEFAULT_PEAK_TOLERANCE * audioFormat.getFrameSize();
	}

	/**
	 * {@inheritDoc}
	 */
	public float[] getSampleAmplitudes(byte[] audioBytes, int startFrame, int frameLength, int aggregationSize) {
		assert(audioBytes != null);
		assert(startFrame >= 0);
		assert((startFrame + frameLength) <= (audioBytes.length / (sampleSize / 8)));
		
		float[] amplitudes = new float[frameLength / aggregationSize];
		
		if (sampleSize == 16) {
					 
			for (int i = 0; i < amplitudes.length; i++) {
				
				int max = 0, absmax = -1, sample, abssample; // could use short, but int avoid casting everywhere

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
					
					abssample = Math.abs(sample);

					if (lastSampleHigh && sample < 0) {
						abssample += peakTolerance;
					} else if (!lastSampleHigh && sample > 0) {
						abssample += peakTolerance;
					}  
					
					if (abssample > absmax) {
						max = sample;
						absmax = abssample;
					}

				}
				
				lastSampleHigh = max > 0;

				amplitudes[i] = ((float)max) / 32768.0f;
				
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
