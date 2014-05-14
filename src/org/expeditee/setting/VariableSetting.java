package org.expeditee.setting;

public abstract class VariableSetting extends Setting {
	
	public VariableSetting(String tooltip) {
		super(tooltip);
	}
	
	public abstract void reset();

}
