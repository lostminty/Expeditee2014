package org.expeditee.simple;

public class AboveMaxParametreCountException extends RuntimeException {
	static final long serialVersionUID = -7034897190745766939L;

	public AboveMaxParametreCountException(int paramCount) {
		super("Expected at most " + paramCount + " params");
	}
}
