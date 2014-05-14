package org.expeditee.gui;

public class ReadOnlyAttributeException extends AttributeException {

	public ReadOnlyAttributeException(String attribute, String simpleName) {
		super(attribute, simpleName, " isReadOnly.");
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
