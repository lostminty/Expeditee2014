package org.expeditee.gui;

public abstract class AttributeException extends Exception {

	public AttributeException(String simpleName, String attribute, String details) {
		super(simpleName + " attribute " + attribute + details);
	}

}
