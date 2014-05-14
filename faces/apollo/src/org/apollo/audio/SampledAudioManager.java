package org.apollo.audio;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Line.Info;

import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.ApolloSystemLog;

/**
 * Manages all audio services. Initializes and shutsdown the audio system when
 * Expeditee opens/closes.
 * 
 * Handles audio formats.
 * 
 * @author Brook Novak
 *
 */
public final class SampledAudioManager extends AbstractSubject {
	
	/** All internal formats have the same sample rate. */
	public static final float PLAYBACK_SAMPLE_RATE = 44100.0f; // Audacitys default
	
	//public static final float PLAYBACK_SAMPLE_RATE = 22050.0f; // Meldexes internal rate .. todo: fix conversions to use better rate

	// Used for describing the ideal default format for recorded audio and converting un-supported 
	// imported audio to... The actual formats may differ depending on the data lines used
	
	// note: Must be PCM, Mono, 16 bit.
	private static final AudioFormat DESIRED_FORMAT = new AudioFormat( // Linear PCM Encoding
			PLAYBACK_SAMPLE_RATE, // Always conform to PLAYBACK_SAMPLE_RATE
			16, // bits per sample. Must be 16. Audacitys default
			1, // Always use mono. Audacitys default
			false, // ALWAYS USED SIGNED FOR BEST PERFORMACE - JAVA DOES NOT HAVE UNSIGNED TYPES
			true // Byte order
			);

	private List<Mixer.Info> inputMixers = new LinkedList<Mixer.Info>();
	
	private Mixer inputMixer = null; // capture
	private Mixer outputMixer = null; // playback
	
	// Loaded with all supported playback formats on construction
	private AudioFormat[] supportedPlaybackFormats = null;
	private AudioFormat defaultPlaybackFormat = null;
	private AudioFormat defaultCaptureFormat = null;
	
	private static SampledAudioManager instance = new SampledAudioManager(); // single design pattern

	/**
	 * @return The singleton instance of the SampledAudioManager
	 */
	public static SampledAudioManager getInstance() { // single design pattern
		return instance;
	}

	private SampledAudioManager() { // singleton design pattern
		LoadMixers();
	}

	/**
	 * Detects all input mixers currently installed on system.  Selects the first input mixer it finds.
	 * The output mixer is set to java software sound engine.
	 */
	private void LoadMixers() {
		
		// Set output mixer as java software mixer
		outputMixer = AudioSystem.getMixer(null);
		
		if (outputMixer == null) {
			ApolloSystemLog.printError("Could not find output audio device");
			
		} else { // Create default output audio format based on output mixer
			
			ApolloSystemLog.println("Using " + outputMixer + " as output mixer");
			
			// Discover all supported playback formats.
			loadSupportedPlaybackFormats();
			
			// Select the default playback format.
			defaultPlaybackFormat = getBestMatchedSupportedFormat(supportedPlaybackFormats, DESIRED_FORMAT);
		
			// Safety
			if (defaultPlaybackFormat == null) {
				defaultPlaybackFormat = DESIRED_FORMAT;
				ApolloSystemLog.printError("Unable to find a suitable default format for audio playback");
			} 
			
			ApolloSystemLog.println("Default playback format: " + defaultPlaybackFormat);
			
		}

		// Get all installed mixers on the system
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();

		Mixer.Info inputMixerInfo = null;
		
		// Determine which mixers can record / playback.
		for (Mixer.Info mi : mixers) {

			Mixer mixer = AudioSystem.getMixer(mi);
			if (mixer.getTargetLineInfo().length > 0) {
				inputMixers.add(mi);
				if (inputMixerInfo ==  null) { // select a default mixer
					inputMixerInfo = mi;
				}
			}
		}

		// Set the current input mixer
		inputMixer = null;
		if (inputMixerInfo != null) {
			setCurrentInputMixure(inputMixerInfo); // this will load the new capture audio format
		} else {
			ApolloSystemLog.printError("Could not find input audio device");
		}
		
	}
	
	/**
	 * Discovers all supported playback audio formats.
	 */
	private void loadSupportedPlaybackFormats() {
		
		LinkedList<AudioFormat> supported = new LinkedList<AudioFormat>();

		for (Info inf : outputMixer.getSourceLineInfo()) {
			
			if (inf instanceof DataLine.Info) {
				DataLine.Info dinf = (DataLine.Info)inf;
				
				for (AudioFormat format : dinf.getFormats()) {
					supported.add(format);
				}
			}
		}
		
		supportedPlaybackFormats = supported.toArray(new AudioFormat[0]);

	}
	
	/**
	 * Loads the best fitting capture format supported by the input mixer. Only format that
	 * are supported for playback are considered.
	 * 
	 * Sets defaultCaptureFormat... Will be null if current input mixer does not support
	 * the right formats. Or if no input mixer is set.
	 */
	private void loadSupportedCaptureFormat() {

		defaultCaptureFormat = null;
		
		//AudioSystemLog.println("Detecting supported capture formats:");
		
		if (inputMixer == null) return;

		LinkedList<AudioFormat> supported = new LinkedList<AudioFormat>();

		for (Info inf : inputMixer.getTargetLineInfo()) {
			
			if (inf instanceof DataLine.Info) {
				DataLine.Info dinf = (DataLine.Info)inf;
				
				for (AudioFormat format : dinf.getFormats()) {
					if (isFormatSupportedForPlayback(format)) { // only consider if can playback
						supported.add(format);
						//AudioSystemLog.println(format);
					}
					
				}
			}
		}
		
		//AudioSystemLog.println("*End of format list");
		
		// Select the best fitting format supported by the input mixer
		defaultCaptureFormat = getBestMatchedSupportedFormat(
				supported.toArray(new AudioFormat[0]), 
				DESIRED_FORMAT);
		
		ApolloSystemLog.println("Using audio format for capturing: " + defaultCaptureFormat);
	}
		
	/**
	 * @return The default playback format for this system. 
	 * Never null, even if output mixer unavailable.
	 */
	public AudioFormat getDefaultPlaybackFormat() {
		return defaultPlaybackFormat;
	}
	
	/**
	 * @return The default capture format for this system. Null if no input mixer set/available
	 */
	AudioFormat getDefaultCaptureFormat() {
		return defaultCaptureFormat;
	}
	
	/**
	 * Finds the best matching format. 
	 * 
	 * All formats that are not PCM encoded and does not use mono channels are excluded.
	 * 
	 * @param targets
	 * 
	 * @param toMatch
	 * 
	 * @return 
	 * 		The best matched and supported Audio format. 
	 * 		Null there were no supported formats.
	 */
	private AudioFormat getBestMatchedSupportedFormat(AudioFormat[] targets, AudioFormat toMatch) {
		assert(targets != null);
		
		assert(toMatch != null);
		
		int bestIndex = -1;
		float bestScore = -1.0f;
		
		for (int i = 0; i < targets.length; i++) {
			
			AudioFormat candiate = targets[i];

			// Not cadidate if not in appollos format.
			if (!candiate.getEncoding().toString().startsWith("PCM") 
					|| candiate.getChannels() != 1
					|| candiate.getSampleSizeInBits() != 16
					|| (candiate.getSampleRate() != AudioSystem.NOT_SPECIFIED &&
							candiate.getSampleRate() != PLAYBACK_SAMPLE_RATE))
				continue;
			
			float score = 0.0f;
			
			// Compute match score
			if (candiate.isBigEndian() == toMatch.isBigEndian()) {
				score += 0.5f;
			}
			
			if (candiate.getSampleSizeInBits() == toMatch.getSampleSizeInBits()) {
				score += 2.0f;
			}
			
			if (candiate.getSampleRate() == toMatch.getSampleRate() 
					|| candiate.getSampleRate() == AudioSystem.NOT_SPECIFIED) {
				score += 2.0f;
			}
			
			if (candiate.getEncoding() == toMatch.getEncoding()) { // there are different PCM encodings
				score += 6.0f;
			}
			
			if (bestIndex == -1 || score > bestScore) {
				bestIndex = i;
				bestScore = score;
			}
		}
		
		if (bestIndex == -1) return null;
		
		// Be sure to specificy the sample rate if not specified
		AudioFormat bestMatch = targets[bestIndex];
		if (bestMatch.getSampleRate() == AudioSystem.NOT_SPECIFIED) {
			bestMatch = new AudioFormat(
					bestMatch.getEncoding(),
					toMatch.getSampleRate(),
					bestMatch.getSampleSizeInBits(),
					bestMatch.getChannels(),
					bestMatch.getFrameSize(),
					toMatch.getFrameRate(),
					bestMatch.isBigEndian()
					);
		}
		
		return bestMatch;
	}
	

	/**
	 * Determines if an audio format requires conversion in order to be used 
	 * in Apollos. 
	 * 
	 * Audio formats must be in PCM, mono, 16-bit sample-size,
	 * SampledAudioManager#PLAYBACK_SAMPLE_RATE sample-rate and be supported
	 * by the output mixer.
	 * 
	 * @param format
	 * 		The format to test. Must not be null.
	 * 
	 * @return
	 * 		True if the given format requires formatting.
	 * 
	 * @throws NullPointerException
	 * 		If format is null.
	 */
	public synchronized boolean isFormatSupportedForPlayback(AudioFormat format) {
		
		if (format == null) throw new NullPointerException("format");
		
		if(!format.getEncoding().toString().startsWith("PCM") || format.getChannels() != 1
				|| format.getSampleSizeInBits() != 16
				|| (format.getSampleRate() != AudioSystem.NOT_SPECIFIED &&
						format.getSampleRate() != PLAYBACK_SAMPLE_RATE)) {
			return false;
		}
		
		// Check that the format is supported by the output mixer
		for (AudioFormat supported : supportedPlaybackFormats) {
			if (supported.getChannels() != 1) continue;
			if (
					format.getEncoding() == supported.getEncoding()
					
					&& format.getSampleSizeInBits() == supported.getSampleSizeInBits()
					
					&& format.isBigEndian() == supported.isBigEndian()

					&& 		(supported.getSampleRate() == AudioSystem.NOT_SPECIFIED
							|| format.getSampleRate() == supported.getSampleRate())
					
			) {
				return true;
			}
		}
		
		return false;
	}
	
	
	
	/**
	 * Sets the current input mixure. Returns immediatly if equal to current input mixer.
	 * Fires a AudioSubjectChangedEvent.INPUT_MIXER event.
	 * 
	 * @param mi The Mixer.Info of the Mixer to set as the new mixer for input.
	 * 
	 * @throws IllegalArgumentException 
	 * 			if the info object does not represent a mixer installed on the system
	 * 
	 * @throws NullPointerException 
	 * 			if mi is null.
	 */
	public void setCurrentInputMixure(Mixer.Info mi) {
		if (mi == null)
			throw new NullPointerException("mi");
		else if (!inputMixers.contains(mi))
			throw new IllegalArgumentException("Mixer not supported");
		
		Mixer newMixer = AudioSystem.getMixer(mi); // also throws IllegalArgumentException
		
		if (newMixer.equals(inputMixer)) return;

		inputMixer = newMixer;
		
		// Determine new capture format according to new input mixer
		loadSupportedCaptureFormat();
		
		fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.INPUT_MIXER));

	}
	
	/**
	 * @return The current input mixer. Null if none is supported.
	 */
	Mixer getCurrentInputMixure() {
		return inputMixer;
	}

	/**
	 * @return The current output mixer. Null if none is supported.
	 */
	Mixer getOutputMixure() {
		return outputMixer;
	}
	
	/**
	 * @return The current input mixer. Null if none is supported.
	 */
	public Mixer.Info getCurrentInputMixureInfo() {
		return inputMixer.getMixerInfo();
	}

	/**
	 * @return A copy of the list of all supported input mixers. Can be empty if none is supported.
	 */
	public List<Mixer.Info> getSupportedInputMixures() {
		return new ArrayList<Mixer.Info>(this.inputMixers);
	}

}
