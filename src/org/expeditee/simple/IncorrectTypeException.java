package org.expeditee.simple;

public class IncorrectTypeException extends RuntimeException {
	static final long serialVersionUID = -7034897190745766939L;
	
	public IncorrectTypeException(String type, int no) {
		super("Expected param " + no + " to be " + type);
	}

	public IncorrectTypeException(String var, String type) {
		super("Expected param " + var + " to be " + type);
	}
	
	public IncorrectTypeException(String message) {
		super(message);
	}
}
