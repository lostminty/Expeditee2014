package org.expeditee.setting;

public class IntegerSetting extends GenericSetting<Integer> {

	public IntegerSetting(String tooltip, Integer value) {
		super(Integer.class, tooltip, value);
	}
}
