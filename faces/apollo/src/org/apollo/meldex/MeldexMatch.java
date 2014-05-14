package org.apollo.meldex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * Calculates a "Score" on how well two given
 *
 */
class MeldexMatch {

	final protected int MIC_FREQUENCY = 8;

	protected DataLine.Info micInfo_;

	protected AudioFormat micFormat_;

	protected TargetDataLine micLine_;

	protected Transcriber transcriber_ = null;

	public MeldexMatch() {
		transcriber_ = new Transcriber();
	}

	protected void establishMicInfo() {
		AudioFormat micFormat22050, micFormat11025, micFormat8000, micFormat8096;
		DataLine.Info micInfo22050, micInfo11025, micInfo8000, micInfo8096;

		// Define the format for our microphone data line
		AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_UNSIGNED;
		int sampleSize = 8;
		int channels = 1;
		boolean bigEndian = false;
		float rate;

		// Define a variety of formats, each with a different sampling rate
		rate = 22050F;
		micFormat22050 = new AudioFormat(encoding, rate, sampleSize, channels,
				(sampleSize / 8) * channels, rate, bigEndian);
		rate = 11025F;
		micFormat11025 = new AudioFormat(encoding, rate, sampleSize, channels,
				(sampleSize / 8) * channels, rate, bigEndian);
		rate = 8000F;
		micFormat8000 = new AudioFormat(encoding, rate, sampleSize, channels,
				(sampleSize / 8) * channels, rate, bigEndian);
		rate = 8096F;
		micFormat8096 = new AudioFormat(encoding, rate, sampleSize, channels,
				(sampleSize / 8) * channels, rate, bigEndian);

		// Describe a variety of data lines, each with a different sampling rate
		micInfo22050 = new DataLine.Info(TargetDataLine.class, micFormat22050);
		micInfo11025 = new DataLine.Info(TargetDataLine.class, micFormat11025);
		micInfo8000 = new DataLine.Info(TargetDataLine.class, micFormat8000);
		micInfo8096 = new DataLine.Info(TargetDataLine.class, micFormat8096);

		// Try to find a microphone line that we can use
		if (AudioSystem.isLineSupported(micInfo22050)) {
			micInfo_ = micInfo22050;
			micFormat_ = micFormat22050;
		} else if (AudioSystem.isLineSupported(micInfo11025)) {
			micInfo_ = micInfo11025;
			micFormat_ = micFormat11025;
		} else if (AudioSystem.isLineSupported(micInfo8000)) {
			micInfo_ = micInfo8000;
			micFormat_ = micFormat8000;
		} else if (AudioSystem.isLineSupported(micInfo8096)) {
			micInfo_ = micInfo8096;
			micFormat_ = micFormat8096;
		} else {
			micInfo_ = null;
			micFormat_ = null;
		}
	}

	protected boolean initMic() {
		establishMicInfo();

		// Get the microphone line
		try {
			micLine_ = (TargetDataLine) AudioSystem.getLine(micInfo_);
		} catch (Exception ex) {
			System.err
					.println("Exception occurred getting the microphone line.\n\n"
							+ ex);
			return false;
		}

		// Calculate the size of the microphone buffer required
		int frameSize = (micFormat_.getChannels() * (micFormat_
				.getSampleSizeInBits() / 8));
		int bytesPerSecond = (int) (micFormat_.getSampleRate() * frameSize);
		int micBufferSize = (int) (bytesPerSecond / MIC_FREQUENCY);

		// Open the microphone data line for recording
		try {
			micLine_.open(micFormat_, micBufferSize);
		} catch (Exception ex) {
			System.err
					.println("Exception occurred opening the microphone line.\n\n"
							+ ex);
			return false;
		}

		return true;
	}

	protected void record_prompt(int p, int fixed) {
		System.out.print("Recording: [");
		for (int i = 0; i < p; i++) {
			System.out.print(".");
		}
		for (int i = p; i < fixed; i++) {
			System.out.print(" ");
		}

		System.out.print("]\r");
		System.out.flush();
	}

	public AudioInputStream record() {
		// Make sure that we found a supported microphone line
		if (micInfo_ == null) {
			System.err
					.println("Sorry, a suitable microphone line could not be found.");
			return null;
		}

		// Create a buffer for the recorded data
		byte[] micBuffer = new byte[micLine_.getBufferSize()];

		// Create an output stream to save the recorded data
		ByteArrayOutputStream micOut = new ByteArrayOutputStream();

		// Start the microphone actually recording
		micLine_.start();

		// Continue recording until the Stop button is pressed
		int fixed = 30;
		int i = 0;
		while (i <= fixed) {
			record_prompt(i, fixed);

			// Read some data from the microphone line...
			int numBytesRead = micLine_.read(micBuffer, 0, micLine_
					.getBufferSize());

			// ...save it to the output stream
			micOut.write(micBuffer, 0, numBytesRead);

			i++;
		}
		System.out.println("");

		// We have finished recording, so stop and close the microphone line
		micLine_.stop();
		micLine_.close();

		// Flush and close the output stream
		try {
			micOut.flush();
			micOut.close();
		} catch (Exception ex) {
			System.err
					.println("Exception occurred flushing/closing output stream.\n\n"
							+ ex);
			return null;
		}

		// Load the data recorded into the audio input stream ready for playback/processing
		byte audioBytes[] = micOut.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
		AudioInputStream audioData = new AudioInputStream(bais, micFormat_,
				audioBytes.length / micFormat_.getFrameSize());

		return audioData;
	}

	public void doMatch(WavSample sample_query, WavSample sample_track) throws IOException {
		
		RogTrack transcribed_query = transcriber_
				.transcribeSample(sample_query);
		
		RogTrack transcribed_track = transcriber_
				.transcribeSample(sample_track);

		System.out.println("Query");
		System.out.println(transcribed_query);
		System.out.println("Track");
		System.out.println(transcribed_track);

		Melody melody_query = transcribed_query.toMelody();
		Melody melody_track = transcribed_track.toMelody();

		DynamicProgrammingAlgorithm dpa = new McNabMongeauSankoffAlgorithm();
		// int match_mode = DynamicProgrammingAlgorithm.MATCH_AT_START;
		int match_mode = DynamicProgrammingAlgorithm.MATCH_ANYWHERE;

		float score = dpa
				.matchToPattern(melody_query, melody_track, match_mode);

		System.out.println("Edit distance = " + score);

	}

	public void liveQuery(String audiotrack) {
		File input_file = new File(audiotrack);

		WavSample sample_track = new WavSample();

		if (sample_track.loadFromFile(input_file) != true) {
			System.err.println("Error: Unable to load wav file: " + audiotrack);
		} else {
			initMic();
			AudioInputStream audioData = record();

			WavSample sample_query = new WavSample(audioData);

			try {
				doMatch(sample_query, sample_track);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void cannedQuery(String audioquery, String audiotrack) {
		File file_query = new File(audioquery);
		File file_track = new File(audiotrack);

		WavSample sample_query = new WavSample();
		WavSample sample_track = new WavSample();

		if (sample_query.loadFromFile(file_query) != true) {
			System.err.println("Error: Unable to load wav file: " + audioquery);
		} else if (sample_track.loadFromFile(file_track) != true) {
			System.err.println("Error: Unable to load wav file: " + audiotrack);
		} else {
			try {
				doMatch(sample_query, sample_track);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String args[]) {
		MeldexMatch meldex = new MeldexMatch();

		int argc = args.length;
		if (argc == 1) {
			meldex.liveQuery(args[0]);
		} else if (argc == 2) {
			meldex.cannedQuery(args[0], args[1]);
		} else {
			System.err.println("Usage: MeldexMatch wav-file1 [wav-file2]");
			System.exit(-1);
		}
	}
}
