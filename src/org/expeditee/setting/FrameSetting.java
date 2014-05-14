package org.expeditee.setting;

import org.expeditee.gui.Frame;
import org.expeditee.items.Text;

public abstract class FrameSetting extends Setting {

	public FrameSetting(String tooltip) {
		super(tooltip);
	}
	
	/**
	 * The code to run
	 * Must be overridden
	 */
	protected abstract void run(Frame frame);
	
	/**
	 * runs the setting specific code if the text item has a child frame
	 */
	public boolean setSetting(Text text) {
		if(text.getChild() != null) {
			run(text.getChild());
			return true;
		}
		return false;
	}

}
