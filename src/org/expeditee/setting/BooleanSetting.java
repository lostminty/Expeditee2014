package org.expeditee.setting;

public class BooleanSetting extends GenericSetting<Boolean> {

	public BooleanSetting(String tooltip, Boolean value) {
		super(Boolean.class, tooltip, value);
	}
}
