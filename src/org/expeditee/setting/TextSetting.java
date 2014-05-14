package org.expeditee.setting;

import org.expeditee.items.Text;

public abstract class TextSetting extends GenericSetting<Text> {

	public TextSetting(String tooltip) {
		super(Text.class, tooltip, null);
		_default = generateText();
		_value = _default;
	}
	
	@Override
	public Text get() {
		return _value == null ? null : _value.copy();
	}
	
	@Override
	public boolean setSetting(Text text) {
		_value = text.getTemplateForm();
		return true;
	}
	
	public abstract Text generateText();

}
