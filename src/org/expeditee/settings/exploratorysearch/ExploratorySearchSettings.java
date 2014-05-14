package org.expeditee.settings.exploratorysearch;

import org.expeditee.items.Text;
import org.expeditee.setting.BooleanSetting;
import org.expeditee.setting.IntegerSetting;
import org.expeditee.setting.StringSetting;

/**
 * The settings for the exploratory search user interface.
 * @author csl14
 */
public abstract class ExploratorySearchSettings {
	
	// Horz and Vert offset for the JfxBrowser's position to work with the Exploratory Search web browser overlay
	public static final int BROWSER_HORZ_OFFSET = 140;
	public static final int BROWSER_VERT_OFFSET = 50;
	
	public static final BooleanSetting BrowserFullScreen = new BooleanSetting("Start Exploratory Search browser in fullscreen", true);
	
	public static final IntegerSetting BrowserDefaultWidth = new IntegerSetting("Default Browser width", 800);
	
	public static final IntegerSetting BrowserDefaultHeight = new IntegerSetting("Default Browser height", 600);
	
	public static final IntegerSetting BrowserLeftMargin = new IntegerSetting("Size of left hand margin for Browser", 0);
	
	public static final IntegerSetting BrowserRightMargin = new IntegerSetting("Size of right hand margin for Browser", 0);
	
	public static final IntegerSetting BrowserTopMargin = new IntegerSetting("Size of Top margin for Browser", 0);
	
	public static final IntegerSetting BrowserBottomMargin = new IntegerSetting("Size of bottom margin for Browser", 0);
}