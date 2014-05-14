package org.apollo.meldex;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;

import org.apollo.io.AudioIO;
import org.apollo.widgets.SampledTrack;

/**
 * A helpful util for conversion in the apollo project
 * 
 * @author Brook Novak
 */
public class MeldexConversion {
	
	private MeldexConversion() {}

	/**
	 * 
	 * @param trackWidget
	 * @return
	 * @throws IOException
	 * 		If conversion fails.
	 */
	public static Melody toMelody(SampledTrack trackWidget) throws IOException {
		
		assert(trackWidget != null);
		
		byte[] bytes = trackWidget.getAudioBytes();
		AudioFormat format = trackWidget.getAudioFormat();
		
		if (bytes != null && format != null) {
			return toMelody(bytes, format);
		}
		
		String path = trackWidget.getLatestSavedAudioPath();
		assert(path != null);
	
		File waveFile = new File(path);
		if (!waveFile.exists()) return null;
		
		WavSample sampleTrack = new WavSample();

		if (!sampleTrack.loadFromFile(waveFile)) {
			return null;
		}
		
		return toMelody(sampleTrack);
		
	}
	
	/**
	 * 
	 * @param bytes
	 * @param format
	 * @return
	 * @throws IOException
	 * 		If conversion fails.
	 */
	public static Melody toMelody(byte[] bytes, AudioFormat format)  throws IOException {
		assert(bytes != null && bytes.length > 0);
		assert(format != null);
		WavSample sampleTrack = new WavSample(bytes, format);
		return toMelody(sampleTrack);
	}
	
	/**
	 * 
	 * @param sampleTrack
	 * @return
	 * 
	 * @throws IOException
	 * 		If conversion fails.
	 */
	public static Melody toMelody(WavSample sampleTrack) throws IOException {
		assert(sampleTrack != null);

		Transcriber transcriber = new Transcriber();
		
		RogTrack transcribedQuery = transcriber.transcribeSample(sampleTrack);
		System.out.println("toMelody: " + transcribedQuery);
		
		if (transcribedQuery != null) {
			return transcribedQuery.toMelody();
		}
		
		return null;
		
	}
	
	/**
	 * Converts PCM audio bytes to the corerct format for transcribing raw audio.
	 * 
	 * @param source
	 * @param sourceFormat
	 * @return
	 * @throws IOException
	 */
	public static byte[] toStandardizedFormat(byte[] source, AudioFormat sourceFormat, float sampleRate) 
		throws IOException {
		
		assert(source != null);
		assert(sourceFormat != null);
		

		// Get the standardised unsigned 8-bit mono data
		AudioFormat partialStandardizedFormat = new AudioFormat(
				sampleRate,
				sourceFormat.getSampleSizeInBits(),
				1, // mono
				sourceFormat.getSampleSizeInBits() != 8, 
				true); 
		
		byte[] stdData = AudioIO.convertAudioBytes(
				source, 
				sourceFormat, 
				partialStandardizedFormat);
		
		if (stdData == null) {
		    return null;
		}
		
		// Perform conversion to 8-bit unsigned
		if (partialStandardizedFormat.getSampleSizeInBits() != 8) {
			byte[] preData = stdData;
			stdData = null;
			
			if (partialStandardizedFormat.getSampleSizeInBits() == 16) {
				
				int newSize = preData.length >> 1;
				stdData = new byte[newSize];
				int sample;
				float fSample;
				
				for (int i = 0; i < newSize; i++) {
					sample = preData[(i << 1)] << 8; // MSB - big endian
					sample |= (preData[(i << 1) + 1] & 0xFF); // LSB - big endian
					
					fSample = sample;
					fSample /= 256.0f;
					fSample += Byte.MAX_VALUE;
					sample = (int)fSample;
					stdData[i] = (byte)(sample & 0xFF);

					//System.out.println(sample + " -> " + fSample + " -> " + stdData[i]);
				}
				
				preData = null;
			}
			
		}
		
		return stdData;
	}
	
	
	

}
