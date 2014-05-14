package org.expeditee.setting;

import org.expeditee.items.Text;

public abstract class Setting {
	
	private String _tooltip;
	
	public Setting(String tooltip) {
		this._tooltip = tooltip;
	}
	
	public final String getTooltip() {
		return _tooltip;
	}
	
	/**
	 * Sets the value from the text item
	 * @return true if the setting was set, false if it failed
	 */
	public abstract boolean setSetting(Text text);
}
