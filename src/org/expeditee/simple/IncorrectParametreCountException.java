package org.expeditee.simple;

public class IncorrectParametreCountException extends RuntimeException {
	static final long serialVersionUID = -7034897190745766939L;

	public IncorrectParametreCountException(int paramCount) {
		super("Expected exactly " + paramCount + " params");
	}
}
