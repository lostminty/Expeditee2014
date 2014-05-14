package org.expeditee.settings.network;


import org.expeditee.gui.Frame;
import org.expeditee.items.Text;
import org.expeditee.setting.FrameSetting;
import org.expeditee.setting.IntegerSetting;
import org.expeditee.setting.StringSetting;

public abstract class NetworkSettings {
	
	// The URL to prepend to web searches
	public static final StringSetting SearchEngine = new StringSetting("The search engine for the JfxBrowser", "https://duckduckgo.com/?q=");
	
	public static final StringSetting HomePage = new StringSetting("The home page for the JfxBrowser", "https://duckduckgo.com");
	
	public static final IntegerSetting FrameShareTimeout = new IntegerSetting("Timeout for FrameShare socket, in milliseconds", 1000);
	
	public static final FrameSetting FrameShare = new FrameSetting("Enable accessing remote frames") {
		@Override
		public void run(Frame frame) {
			org.expeditee.network.FrameShare.init(frame);
		}
	};
	
	public static void onParsed(Text text) {
		// System.out.println("network parsed");
	}
}
