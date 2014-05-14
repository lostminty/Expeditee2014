package org.apollo.audio.util;

import javax.sound.sampled.AudioFormat;

/**
 * Some basic audio proccessing algorothms for audio amplification
 * 
 * @author Brook Novak
 */
public final class AmplifyPCM {
	
	private AmplifyPCM() {
	}
	
	/**
	 * Searches for the maximum sample of some audio
	 * 
	 * @param samples
	 * 		The audio samples to inspect
	 * 
	 * @param audioFormat
	 * 		The format of samples. Must be 16-bit.
	 * 
	 * @return
	 * 		A float ranging from 0-1 - describing the maximum sample. e.g. 1 would mean that
	 * 		there was a sample which was clipping.
	 * 
	 * @throws IllegalArgumentException
	 * 		If audioform is not 16 (not supported)
	 */
	public static float findMaxSample(byte[] samples, AudioFormat audioFormat) {
		assert(samples != null);
		assert(samples.length > 0);
		assert(audioFormat != null);
		if (audioFormat.getSampleSizeInBits() != 16) 
			throw new IllegalArgumentException("audioFormat is not in 16 bits");
		assert((samples.length % 2) == 0);
		
		int maxABSSample = 0;
		
		for (int i = 0; i < samples.length; i+=2) {

			int lsb, msb;
						 
			if (audioFormat.isBigEndian()) {
				
				// First byte is MSB (high order)
				msb = (int)samples[i];
				 
				 // Second byte is LSB (low order)
				lsb = (int)samples[i + 1];
			
			 } else {
				// First byte is LSB (low order)
				lsb = (int)samples[i];
				 
				// Second byte is MSB (high order)
				msb = (int)samples[i + 1];
			}
			
			int sample = (msb << 0x8) | (0xFF & lsb);
			sample = Math.abs(sample);
			if (sample > maxABSSample) maxABSSample = sample;
			
			if (maxABSSample >= 32768) break; // max possible sample found
		}
			
		return ((float)maxABSSample) / 32768.0f;
	
	}
	
	/**
	 * Amplifies some given audio content. Beware of clipping.
	 * 
	 * @see #findMaxSample(byte[], AudioFormat) for avoiding clipping.
	 * 
	 * @param samples
	 * 		The audio samples to amplify
	 * 
	 * @param audioFormat
	 * 		The format of samples. Must be 16-bit.
	 * 
	 * @param factor
	 * 		How much to multiply. Can be negitive... to dampen sound levels.
	 */
	public static void amplify(byte[] samples, AudioFormat audioFormat, float factor) {
		assert(samples != null);
		assert(samples.length > 0);
		assert(audioFormat != null);
		if (audioFormat.getSampleSizeInBits() != 16) 
			throw new IllegalArgumentException("audioFormat is not in 16 bits");
		assert((samples.length % 2) == 0);
	
		for (int i = 0; i < samples.length; i+=2) {

			int lsb, msb;
						 
			// Unpack bytes
			if (audioFormat.isBigEndian()) {
				
				// First byte is MSB (high order)
				msb = (int)samples[i];
				 
				 // Second byte is LSB (low order)
				lsb = (int)samples[i + 1];
			
			 } else {
				// First byte is LSB (low order)
				lsb = (int)samples[i];
				 
				// Second byte is MSB (high order)
				msb = (int)samples[i + 1];
			}
			
			int sample = (msb << 0x8) | (0xFF & lsb);
			
			// Ignore silence
			if (sample == 0) continue;
			
			float amplifiedSample = sample * factor;
			sample = (int)amplifiedSample;
			
			// Apply clipping
			if (sample > 32768) sample = 32768;
			else if (sample < -32768) sample = -32768;
				
			lsb = (0xFF & sample);
			msb = (0xFF & (sample >> 0x8));
			
			// Pack bytes
			if (audioFormat.isBigEndian()) {
				samples[i] = (byte)msb;
				samples[i + 1] = (byte)lsb;
			} else {
				samples[i] = (byte)lsb;
				samples[i + 1] = (byte)msb;
			}
		}

	}
}
