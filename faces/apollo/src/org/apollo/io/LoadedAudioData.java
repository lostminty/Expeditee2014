package org.apollo.io;

import javax.sound.sampled.AudioFormat;

/**
 * Immutable class storing loaded audio information.
 * 
 * @author Brook Novak
 *
 */
public final class LoadedAudioData {
	
	private byte[] audioBytes;
	private AudioFormat audioFormat;
	private boolean wasConverted;
	
	public LoadedAudioData(byte[] audioBytes, AudioFormat audioFormat, boolean wasConverted) {
		this.audioBytes = audioBytes;
		this.audioFormat = audioFormat;
		this.wasConverted = wasConverted;
	}

	public byte[] getAudioBytes() {
		return audioBytes;
	}

	public AudioFormat getAudioFormat() {
		return audioFormat;
	}

	public boolean wasConverted() {
		return wasConverted;
	}
	
	
}
