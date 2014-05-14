package org.apollo.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFileFormat.Type;

import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.SampledAudioManager;
import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.Observer;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.AudioMath;

/**
 * Provides audio IO and conversion operations.
 * 
 * @author Brook Novak
 *
 */
public class AudioIO {
	
	private AudioIO () {}
	private static AudioIO innerclassCreator = new AudioIO();
	
	/**
	 * Determines the running length of a audio file.
	 * 
	 * @param path
	 * 		The path to the audio file.
	 * 
	 * @return
	 * 		The running time of the sound file in milliseconds.
	 * 		-1 if the audio frame count is not specified due to the encoding method.
	 * 
	 * @throws IOException
	 * 		If failed to create file for saving, or an error occured while writing audio bytes.
	 * 
	 * @throws UnsupportedAudioFileException
	 * 		If audio format is unsupported or the encoding is not PCM.
	 * 
	 * @throws NullPointerException
	 * 			If path is null.
	 * 
	 */
	public static long getRunningTime(String path) throws IOException, UnsupportedAudioFileException {
		if (path == null) throw new NullPointerException("path");
		
		AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(new File(path));
		
		if (fileFormat.getFrameLength() == AudioSystem.NOT_SPECIFIED)
			return -1;
		
		return AudioMath.framesToMilliseconds(fileFormat.getFrameLength(), fileFormat.getFormat());
	}
	

	/**
	 * Determines whether or not a file can be imported. Depending on the
	 * file exists and is an audio file with a supported format (or conversion).
	 * 
	 * @param f
	 * 		The file to check.
	 * 
	 * @return
	 * 		True if the file can be imported. Otherwise false.
	 */
	public static boolean canImportFile(File f) {
		
		if (f == null || !f.exists() || !f.canRead()) return false;
		
		AudioFileFormat fileFormat;
		try {
			fileFormat = AudioSystem.getAudioFileFormat(f);
		} catch (UnsupportedAudioFileException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		
		return (SampledAudioManager.getInstance().isFormatSupportedForPlayback(fileFormat.getFormat()) ||
				AudioSystem.isConversionSupported(
						SampledAudioManager.getInstance().getDefaultPlaybackFormat(),
						fileFormat.getFormat()));
	}
	
	/**
	 * Converts audio bytes into a target format. ...If needs to.
	 * 
	 * @param sourceBytes
	 * 		The bytes to convert. Must not be null.
	 * 
	 * @param sourceFormat
	 * 		The format of the bytes that will be converted. Must not be null.
	 * 
	 * @param targetFormat
	 * 		The format to convert the bytes to. Must not be null.
	 * 
	 * @return
	 * 		The converted bytes. If the source and target formats match then sourceBytes is returned.
	 * 
	 * @throws IOException
	 * 		If an error occured while converting
	 * 
	 * @throws NullPointerException
	 * 		If any of the arguments are null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If the conversion is not supported.
	 * 
	 */
	public static byte[] convertAudioBytes(byte[] sourceBytes, AudioFormat sourceFormat, AudioFormat targetFormat) 
		throws IOException {
		
		if (sourceBytes == null) throw new NullPointerException("sourceBytes");
		if (sourceFormat == null) throw new NullPointerException("sourceFormat");
		if (targetFormat == null) throw new NullPointerException("targetFormat");
		if (sourceBytes.length == 0) throw new IllegalArgumentException("sourceBytes length is zero");
		
		if (!AudioSystem.isConversionSupported(targetFormat, sourceFormat)) 
			throw new IllegalArgumentException("Conversion not supported");
		
		// Need converting?
		if (targetFormat.equals(sourceFormat)) return sourceBytes;
		
		int frameCount = sourceBytes.length / sourceFormat.getFrameSize();
		
		// Create AudioInputStream to stream source bytes
		AudioInputStream aisOriginal = new AudioInputStream(
				new ByteArrayInputStream(sourceBytes), 
				sourceFormat,
				frameCount);
		
		// Chain stream with a series of conversion streams. Note that you cannot just
		// use one conversion stream to process the audio all at once.
		AudioInputStream aisConverter = aisOriginal;
		
		// First convert to the correct PCM format so can carry out appropriate conversions
		//if (!sourceFormat.getEncoding().equals(targetFormat.getEncoding())) {
//		AudioFormat.Encoding conversionEncoding = null;
//		
//		if (targetFormat.getEncoding().toString().startsWith("PCM")) // convert signed/unsigned if need to
//			conversionEncoding = targetFormat.getEncoding();
//		
//		else if (!targetFormat.getEncoding().toString().startsWith("PCM"))
//			conversionEncoding =
//				(sourceFormat.getSampleSizeInBits() == 8) ?
//						AudioFormat.Encoding.PCM_UNSIGNED :
//						AudioFormat.Encoding.PCM_SIGNED;
//		
//		if (conversionEncoding != null) {
//			aisConverter = AudioSystem.getAudioInputStream(conversionEncoding, aisConverter);
//		}
//		
//	}
		
		if (!sourceFormat.getEncoding().toString().startsWith("PCM")) {
			
			AudioFormat.Encoding conversionEncoding = 
				(sourceFormat.getSampleSizeInBits() == 8) ?
				AudioFormat.Encoding.PCM_UNSIGNED :
				AudioFormat.Encoding.PCM_SIGNED;
			
			aisConverter = AudioSystem.getAudioInputStream(conversionEncoding, aisConverter);

		}
		
		// Next convert number of channels
		if (sourceFormat.getChannels() != targetFormat.getChannels()) {
			
			AudioFormat chainedFormat = aisConverter.getFormat();
			
			aisConverter = AudioSystem.getAudioInputStream(
					new AudioFormat(
							chainedFormat.getEncoding(),
							chainedFormat.getSampleRate(),
							chainedFormat.getSampleSizeInBits(),
							targetFormat.getChannels(),
							targetFormat.getChannels() * ((chainedFormat.getSampleSizeInBits() + 7) / 8),
							chainedFormat.getFrameRate(),
							chainedFormat.isBigEndian()),
					aisConverter);
		}
		
		
		// Next convert sample size AND endianess.
		if ((aisConverter.getFormat().getSampleSizeInBits() != targetFormat.getSampleSizeInBits())
				|| (aisConverter.getFormat().isBigEndian() != targetFormat.isBigEndian())) {
			
			AudioFormat chainedFormat = aisConverter.getFormat();

			aisConverter = AudioSystem.getAudioInputStream(
					new AudioFormat(
						chainedFormat.getEncoding(),
						chainedFormat.getSampleRate(),
						targetFormat.getSampleSizeInBits(),
						chainedFormat.getChannels(),
						chainedFormat.getChannels() * ((targetFormat.getSampleSizeInBits() + 7) / 8),
						chainedFormat.getFrameRate(),
						targetFormat.isBigEndian()),
					aisConverter);

		}

		// convert sample rate - this relies on a plugin. I.E. tritonous' PCM2PCM SPI
		final float DELTA = 1E-9F;
		if (Math.abs(aisConverter.getFormat().getSampleRate() - targetFormat.getSampleRate()) >= DELTA) {

			AudioFormat chainedFormat = aisConverter.getFormat();

			aisConverter = AudioSystem.getAudioInputStream(
					new AudioFormat(
						chainedFormat.getEncoding(),
						targetFormat.getSampleRate(),
						chainedFormat.getSampleSizeInBits(),
						chainedFormat.getChannels(),
						chainedFormat.getFrameSize(),
						targetFormat.getFrameRate(),
						chainedFormat.isBigEndian()),
					aisConverter);

		}

		// convert to back to non-PCM encoding, or to different PCM encoding, if need to.
		if (!targetFormat.getEncoding().equals(aisConverter.getFormat().getEncoding())) {
			aisConverter = AudioSystem.getAudioInputStream(targetFormat.getEncoding(), aisConverter);
		}
		
		LinkedList<byte[]> convertedBytes = new LinkedList<byte[]>();
		int BUFFERSIZE = 50000 * sourceFormat.getFrameSize();
		
		// Read the bytes and convert
		int bytesRead = 0;
		
		while(bytesRead != -1) {
			
			int bytePosition = 0;
			bytesRead = 0;
			byte[] buffer = new byte[BUFFERSIZE];
			
			while (bytePosition < BUFFERSIZE) {

				bytesRead = aisConverter.read(buffer, bytePosition, buffer.length - bytePosition);
				
				if (bytesRead == -1) break;
				
				bytePosition += bytesRead;
				
			}
			
			if (bytePosition > 0 && bytesRead > 0) {
				
				convertedBytes.add(buffer);
				
			} else if (bytePosition > 0) { // last chunk
				byte[] finalChunk = new byte[bytePosition];
				System.arraycopy(buffer, 0, finalChunk, 0, bytePosition);
				convertedBytes.add(finalChunk);
			}
			
		}
		
		int size = 0;
		
		for (byte[] ba : convertedBytes) size += ba.length;
		
		byte[] converted = new byte[size];
		int pos = 0;
		
		// Assemble
		for (byte[] ba : convertedBytes) {
			System.arraycopy(
					ba, 
					0, 
					converted, 
					pos, 
					ba.length);
			
			pos += ba.length;
		}

		
		return converted;
	}
	
	/**
	 * Writes PCM encoded audio bytes as a wave file.
	 * 
	 * @param path
	 * 		The path of the wave file to write to. This will be overriden.
	 * 
	 * @param bytes
	 * 		The PCM encoded audio bytes to write
	 * 
	 * @param format
	 * 		The format of the audio bytes. Must be in PCM.
	 * 
	 * @throws IOException
	 * 		If failed to create file for saving, or an error occured while writing audio bytes.
	 * 
	 * @throws UnsupportedAudioFileException
	 * 		If audio format is unsupported or the encoding is not PCM.
	 * 
	 * @throws NullPointerException
	 * 			If path, bytes or format is null.
	 */
	public static synchronized void savePCMAudioToWaveFile(String path, byte[] bytes, AudioFormat format) 
		throws IOException, UnsupportedAudioFileException {
		
		if (bytes == null) throw new NullPointerException("bytes");
		if (format == null) throw new NullPointerException("format");
		
		if (!format.getEncoding().toString().startsWith("PCM"))
			throw new UnsupportedAudioFileException();
		
		saveAudioToFile(path, bytes, format, Type.WAVE, bytes.length / format.getFrameSize());
	}
	
	/**
	 * Writes audio bytes to file.
	 * 
	 * @param path
	 * 		The path of the audio file to write to. This will be overriden.
	 * 
	 * @param bytes
	 * 		The audio bytes to write
	 * 
	 * @param format
	 * 		The format of the audio bytes.
	 * 
	 * @param fileType
	 * 		The type of file to output to.
	 * 
	 * @param length
	 * 		The amount of audio bytes to write
	 * 
	 * @throws IOException
	 * 		If failed to create file for saving, or an error occured while writing audio bytes.
	 * 
	 * @throws UnsupportedAudioFileException
	 * 		If audio format is unsupported
	 * 
	 * @throws NullPointerException
	 * 			If path, bytes, format or fileType is null.
	 */
	public static synchronized void saveAudioToFile(String path, byte[] bytes, 
			AudioFormat format, Type fileType, long length) 
		throws IOException, UnsupportedAudioFileException {
		
		if (path == null) throw new NullPointerException("path");
		if (bytes == null) throw new NullPointerException("bytes");
		if (format == null) throw new NullPointerException("format");
		if (fileType == null) throw new NullPointerException("fileType");

		// Create file out stream
		File f = new File(path);
		FileOutputStream out = new FileOutputStream(f);
	
		// Create audio input stream
		AudioInputStream aistream = new AudioInputStream(
				new ByteArrayInputStream(bytes),
				format,
				length);
		
		// Write audio file
		AudioSystem.write(aistream, fileType, out);

	}

	/**
	 * Loads an audio file into memory. The loaded bytes are converted if they need to be
	 * in order to be used in apollos.
	 * 
	 * This operation can be cancelled via cancelLoad. 
	 * 
	 * @param file 
	 * 		The file to load. must not be null and the file must exist.
	 * 
	 * @param statusObserver
	 * 		If given (can be null), then the statusObserver will be notified with 
	 * 		LOAD_STATUS_REPORT AudioSubjectChangedEvent events (on the calling thread).
	 * 		The state will be a float with the current percent (between 0.0 and 1.0 inclusive).
	 * 		The subject source will be this instance.
	 * 
	 * @return
	 * 		The loaded audio - in a format that can be used in apollos.
	 * 		Null if cancelled.
	 * 
	 * @throws IOException
	 * 		If an exception occured while loade audio bytes.
	 * 		
	 * @throws UnsupportedAudioFileException
	 * `	if the File does not point to valid audio file data recognized by the system.
	 * 
	 */
	public static synchronized LoadedAudioData loadAudioFile(File file, Observer statusObserver) 
		throws IOException, UnsupportedAudioFileException {
		
		return (innerclassCreator.new AudioFileLoader()).loadAudioFile(file, statusObserver);
		
	}
	


	/**
	 * Loads audio files into memory - converts to format ready to be used in apollos.
	 * 
	 * This is also a subject - it optionally reports the load status while loading.
	 * The loading operatoin is also cancellable.
	 *  
	 * @author Brook Novak
	 *
	 */
	public class AudioFileLoader extends AbstractSubject {
		
		/**
		 * Util constructor
		 */
		private AudioFileLoader() {}
	
		private boolean cancelLoad = false;
		
		/**
		 * Cancels current load operation if any.
		 */
		public void cancelLoad() {
			cancelLoad = true;
		}

		/**
		 * Loads an audio file into memory. The loaded bytes are converted if they need to be
		 * in order to be used in apollos.
		 * 
		 * This operation can be cancelled via cancelLoad. 
		 * 
		 * @param file 
		 * 		The file to load. must not be null and the file must exist.
		 * 
		 * @param statusObserver
		 * 		If given (an be null), then the statusObserver will be notified with 
		 * 		LOAD_STATUS_REPORT AudioSubjectChangedEvent events (on the calling thread).
		 * 		The state will be a float with the current percent (between 0.0 and 1.0 inclusive).
		 * 		The subject source will be this instance.
		 * 
		 * @return
		 * 		The loaded audio - in a format that can be used in apollos.
		 * 		Null if cancelled.
		 * 
		 * @throws IOException
		 * 		If an exception occured while loade audio bytes.
		 * 		
		 * @throws UnsupportedAudioFileException
		 * `	if the File does not point to valid audio file data recognized by the system.
		 * 
		 */
		public LoadedAudioData loadAudioFile(File file, Observer statusObserver) throws IOException, UnsupportedAudioFileException {
			assert(file != null && file.exists());
			
			cancelLoad = false;
			
			AudioInputStream fileStream = null; // stream directly from file
			AudioInputStream decodeStream = null; // stream decoding fileStream
			
			try {
				
				// Get the audio form information
				AudioFileFormat fformat = AudioSystem.getAudioFileFormat(file);
				
				// Get the file stream
				fileStream = AudioSystem.getAudioInputStream(file);
				
				AudioInputStream sampleStream;
				AudioFormat sampleFormat;
				boolean isConverting;
				
				// Check if file needs to convert
				if (!SampledAudioManager.getInstance().isFormatSupportedForPlayback(fformat.getFormat())) {

					sampleFormat = SampledAudioManager.getInstance().getDefaultPlaybackFormat();
					
					if (!AudioSystem.isConversionSupported(sampleFormat, fformat.getFormat())) {
						throw new UnsupportedAudioFileException("Audio not supported");
					}
					
					decodeStream = AudioSystem.getAudioInputStream(sampleFormat, fileStream);
					sampleStream = decodeStream;
					isConverting = true;

					
				} else { // otherwise read bytes directly from file stream
					sampleStream = fileStream;
					sampleFormat = fformat.getFormat();
					isConverting = false;
				}
				
				assert (SampledAudioManager.getInstance().isFormatSupportedForPlayback(sampleFormat));
	
				// Initialize the ByteBuffer - and size if possible (not possible for vairable frame size encoding)
				ByteArrayOutputStream loadedBytes = (fformat.getFrameLength() != AudioSystem.NOT_SPECIFIED) ?
						new ByteArrayOutputStream(fformat.getFrameLength() * sampleFormat.getFrameSize()) : 
						new ByteArrayOutputStream();

				byte[] buffer = new byte[sampleFormat.getFrameSize() * (int)sampleFormat.getFrameRate()];
				int bytesRead = 0;
				int totalBytesRead = 0;
				float percent = 0.0f;
				
				while (!cancelLoad) { // keep loading until cancelled
					
					// Report current percent to given observer (if any)
					if (statusObserver != null) {
						statusObserver.modelChanged(this, new SubjectChangedEvent(
								ApolloSubjectChangedEvent.LOAD_STATUS_REPORT, new Float(percent)));
					}
	
					// Read bytes from the stream into memory
					bytesRead = sampleStream.read(buffer, 0, buffer.length);
					
					if (bytesRead == -1) break; // complete
					
					loadedBytes.write(buffer, 0, bytesRead);
					
					// Update percent complete
					totalBytesRead += bytesRead;
					percent = (float)totalBytesRead / (float)fformat.getByteLength();
				}
	
				loadedBytes.close();
				
				// if incomplete - then cancelled.
				if (bytesRead != -1) {
					assert(cancelLoad);
					return null;
				}
	
				// otherwise return the loaded audio
				return new LoadedAudioData(loadedBytes.toByteArray(), sampleFormat, isConverting);
			
			} finally { // Always ensure that decoder and file streams are closed

				if (decodeStream != null) {
					decodeStream.close();
				}
				
				if (fileStream != null) {
					fileStream.close();
				}
	
			}
		}
	
	}
	
	/**
	 * For manual testing purposes
	 * 
	 * @param args
	 * 	1 Arg - the file to convert
	 */
	public static void main(String[] args) {
		if (args.length == 1) {
			
			System.out.println("Testing with " + args[0]);
			
			try {
				LoadedAudioData loaded = loadAudioFile(new File(args[0]), null);
				if (loaded != null && loaded.getAudioBytes() != null) {
					
					System.out.println("Loaded file into internal format: " + loaded.getAudioFormat());
					
					if (loaded.wasConverted()) {
						savePCMAudioToWaveFile(
							args[0] + "_pre-converted.wav", 
							loaded.getAudioBytes(),
							loaded.getAudioFormat());
					}
					

					AudioFormat lowQualFormat = new AudioFormat(
							22050,
							8, // 8-bit
							1, // mono
							loaded.getAudioFormat().getEncoding().toString().endsWith("SIGNED"), 
							true); // Big endian 

					byte[] stdData = AudioIO.convertAudioBytes(
							loaded.getAudioBytes(), 
							loaded.getAudioFormat(),
							lowQualFormat);
					
					if (stdData != null) {
						
						System.out.println("Saving file into low-quality format: " + lowQualFormat);
						
						savePCMAudioToWaveFile(
								args[0] + "_converted.wav", 
								stdData,
								lowQualFormat);
						
						System.out.println("Conversion succeeded");
						return;
						
					
					}

					
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (UnsupportedAudioFileException e) {
				e.printStackTrace();
			}
			
			System.err.println("Conversion failed");
			return;
			
		}
		
		System.err.println("Must supply 1 argument: the file path to convert");
		
		
		
	}

}
