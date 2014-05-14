package org.apollo.audio;

/**
 * An audio format is not supported by a specific mixer configuration.
 * 
 * @author Brook Novak
 */
public class AudioFormatNotSupportedException extends Exception {
	protected static final long serialVersionUID = 0L;
	
	public AudioFormatNotSupportedException() {
		super();
	}
	
	public AudioFormatNotSupportedException(String message) {
		super(message);
	}
	
	public AudioFormatNotSupportedException(String message, Exception inner) {
		super(message, inner);
	}
}
