package org.expeditee.simple;

public class BelowMinParametreCountException extends RuntimeException {
	static final long serialVersionUID = -7034897190745766939L;

	public BelowMinParametreCountException(int paramCount) {
		super("Expected at least " + paramCount + " params");
	}
}
