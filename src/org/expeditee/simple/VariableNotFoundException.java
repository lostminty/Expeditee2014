package org.expeditee.simple;

public class VariableNotFoundException extends RuntimeException {
	static final long serialVersionUID = -7034897190745766939L;

	public VariableNotFoundException(String s) {
		super(s);
	}
}
