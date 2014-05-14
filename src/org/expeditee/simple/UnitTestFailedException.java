package org.expeditee.simple;

public class UnitTestFailedException extends RuntimeException {
	static final long serialVersionUID = -7034897190745766939L;

	public UnitTestFailedException(String expected, String result) {
		super("Assert expected [" + expected + "] got [" + result + "]");
	}
}
