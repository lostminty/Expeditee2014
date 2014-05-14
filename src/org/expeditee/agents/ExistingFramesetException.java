package org.expeditee.agents;

public class ExistingFramesetException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9139069750643242508L;

	public ExistingFramesetException(String framesetName) {
		super("A frameset called " + framesetName
		+ " already exists.");
	}
}
