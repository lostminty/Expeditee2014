package org.expeditee.setting;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.items.Text;

public abstract class FunctionSetting extends Setting {
	
	public FunctionSetting(String tooltip) {
		super(tooltip);
	}
	
	/**
	 * The code to run
	 * Must be overridden
	 */
	protected abstract void run();
	
	/**
	 * runs the setting specific code if the text item has a value of true
	 */
	public boolean setSetting(Text text) {
		AttributeValuePair avp = new AttributeValuePair(text.getText(), false);
		if(avp.getValue() != null && avp.getValue().trim().length() != 0 && avp.getBooleanValue()) {
			run();
			return true;
		}
		return false;
	}
}
