package org.expeditee.settings.network.proxy;

import org.expeditee.gui.Browser;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.Password;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;
import org.expeditee.setting.StringSetting;

public abstract class ProxySettings {

	private static boolean _warned = false;
	public static final StringSetting Host = new StringSetting("The host for the proxy", null);
	public static final StringSetting Port = new StringSetting("The port for the proxy (e.g. port 80)", "80");
	public static final StringSetting User = new StringSetting("Your username for the proxy", null);
	public static final StringSetting Pass = new StringSetting("Your password for the proxy", null);

	public static void onParsed(Text text) {

		// standard settings parsing won't find the password,
		// so check if there's a password widget on the frame
		Password passwordWidget = null;

		for (Item i : text.getChild().getAllItems()) {
			if (i instanceof Text) {
				String str = i.getText().trim();
				String strLower = str.toLowerCase();
				if (strLower.startsWith("@iw:")) { // widget
					// check if it's a password widget
					if (strLower.substring(4).trim().startsWith("org.expeditee.items.widgets.password")) {
						if (passwordWidget == null) {
							try {
								passwordWidget = (Password) InteractiveWidget.createWidget((Text) i);
								if (Pass.get() != null) {
									// MessageBay.displayMessage("pass was defined multiple times!", Color.ORANGE);
								}
								Pass.set(passwordWidget.getPassword());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			} else if(i instanceof WidgetCorner || i instanceof WidgetEdge) {
				if(passwordWidget == null) {
					InteractiveWidget iw;
					if(i instanceof WidgetCorner) {
						iw = ((WidgetCorner)i).getWidgetSource();
					} else {
						iw = ((WidgetEdge)i).getWidgetSource();
					}
					if(iw instanceof Password) {
						passwordWidget = (Password) iw;
					}
					if(Pass.get() != null) {
						// MessageBay.displayMessage("pass was defined multiple times!", Color.ORANGE);
					}
					Pass.set(passwordWidget.getPassword());
				}
			}
		}
		
		if(Host.get() == null) {
			if(!_warned) {
				MessageBay.warningMessage("@Settings: Network->Proxy->Host was not defined -- assuming direct Internet connection");
				_warned = true;
			}
			return;
		}
		if(Port.get() == null) {
			if(!_warned) {
				MessageBay.warningMessage("@Settings: Network->Proxy->Port was not defined (defaulted to 80)");
				_warned = true;
			}
			Port.set("80");
		}
		if(User.get() == null) {
			if(!_warned) {
				MessageBay.warningMessage("@Settings: Network->Proxy->User was not defined");
				_warned = true;
			}
			return;
		}
		if(Pass.get() == null) {
			if(!_warned) {
				MessageBay.warningMessage("proxy pass was not defined");
				_warned = true;
			}
			return;
		}
		// TODO: Is it possible to have different host/port for http/https protocols for a proxy server?
		System.setProperty("http.proxyHost", Host.get());
		System.setProperty("http.proxyPort", Port.get());
		System.setProperty("https.proxyHost", Host.get());
		System.setProperty("https.proxyPort", Port.get());
		Browser.proxyAuth.setup(User.get(), Pass.get());
		
		// System.out.println("proxy parsed");
	}
}
