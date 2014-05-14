package org.apollo.audio;

import java.io.PipedInputStream;

import javax.sound.sampled.AudioFormat;

/**
 * Provides audio capture classes for reading captured audio.
 * 
 * @author Brook Novak
 *
 */
public final class AudioCapturePipe {
	
	private PipedInputStream pin;
	private AudioFormat audioFormat;
	private int bufferSize;
	
	public AudioCapturePipe(PipedInputStream pin, AudioFormat audioFormat, int bufferSize) {
		this.pin = pin;
		this.audioFormat = audioFormat;
		this.bufferSize = bufferSize;
	}
	
	public AudioFormat getAudioFormat() {
		return audioFormat;
	}

	public PipedInputStream getPin() {
		return pin;
	}
	
	public int getBufferSize() {
		return bufferSize;
	}
}
