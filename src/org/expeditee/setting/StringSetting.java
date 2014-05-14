package org.expeditee.setting;

public class StringSetting extends GenericSetting<String> {

	public StringSetting(String tooltip, String value) {
		super(String.class, tooltip, value);
	}
}
