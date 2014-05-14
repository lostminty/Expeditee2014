package org.expeditee.settings.experimental;

import org.expeditee.setting.BooleanSetting;

public class ExperimentalFeatures {

	public static final BooleanSetting AutoWrap = new BooleanSetting("Enable auto wrapping of text", false);
	
	public static final BooleanSetting MousePan = new BooleanSetting("Enable panning of the frame by shift-click and dragging the mouse", false);
	
}
