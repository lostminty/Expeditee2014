package org.expeditee.gui;

public class NoSuchAttributeException extends AttributeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NoSuchAttributeException(String attribute, String simpleName) {
		super(simpleName, attribute, " does not exist.");
	}

}
