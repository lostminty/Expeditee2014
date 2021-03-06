package org.expeditee.settings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameCreator;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.Password;
import org.expeditee.reflection.PackageLoader;
import org.expeditee.setting.GenericSetting;
import org.expeditee.setting.Setting;
import org.expeditee.setting.VariableSetting;

public abstract class Settings {

	public static final String SETTINGS_PACKAGE_PARENT = "org.expeditee.";
	public static final String SETTINGS_PACKAGE = SETTINGS_PACKAGE_PARENT + "settings.";
	
	private static final class PageDescriptor {
		
		private final HashMap<String, Setting> settings = new HashMap<String, Setting>();
		private final List<String> orderedEntries = new LinkedList<String>();
		private final List<VariableSetting> settingsList = new LinkedList<VariableSetting>();
		private final Method onParsed;
		
		private PageDescriptor(Class<?> clazz) {
			
			// populate map of settings
			for(Field f : clazz.getFields()) {
				// only allow valid types
				if(! Setting.class.isAssignableFrom(f.getType())) {
					continue;
				}
				try {
					Setting s = (Setting) f.get(null);
	                settings.put(f.getName().toLowerCase(), s);
	                if(s instanceof VariableSetting) {
	                	settingsList.add((VariableSetting) s);
	                }
	                orderedEntries.add(f.getName());
                } catch (Exception e) {
	                e.printStackTrace();
                }
			}
			Method m = null;
			try {
				m = clazz.getMethod("onParsed", Text.class);
			} catch(Exception e) {
				// System.err.println(clazz.getName() + " has no onParsed(Text t) callback");
			}
			this.onParsed = m;
		}
	}
	private static HashMap<String, PageDescriptor> _pages = new HashMap<String, PageDescriptor>();

	private static boolean _init = false;
	public static void Init() {
		if(_init) return;
		_init = true;
		try {
			for(Class<?> clazz : PackageLoader.getClassesNew(SETTINGS_PACKAGE)) {
				// Ignore this class since it's the controller
				if(clazz.equals(Settings.class)) {
					continue;
				}
				String settingsPage = clazz.getPackage().getName().toLowerCase().substring(SETTINGS_PACKAGE_PARENT.length());
				// System.out.println(settingsPage + " : " + clazz.getName());
				_pages.put(settingsPage, new PageDescriptor(clazz));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		FrameUtils.ParseProfile(FrameIO.LoadProfile(UserSettings.ProfileName.get()));
	}
	
	/**
	 * Parses the settings tree, then resets any settings that were not set
	 */
	public static void parseSettings(Text text) {
		List<VariableSetting> set = parseSettings(text, "");
		List<VariableSetting> toDefault = new LinkedList<VariableSetting>();
		for(PageDescriptor pd : _pages.values()) {
			toDefault.addAll(pd.settingsList);
		}
		toDefault.removeAll(set);
		for(VariableSetting s : toDefault) {
			try {
				// System.out.println("Resetting " + s.getTooltip());
	            s.reset();
	            // System.out.println("Set " + f.getName() + " to default value " + f.get(null));
            } catch (Exception e) {
	            e.printStackTrace();
            }
		}
	}
	
	/**
	 * Sets all the simple settings.
	 * 
	 * @param text
	 * @param prefix
	 * 
	 * @return List of VariableSettings that were changed
	 */
	private static List<VariableSetting> parseSettings(Text text, String prefix) {
		
		List<VariableSetting> set = new LinkedList<VariableSetting>();
		Frame child = text.getChild();
		if(child == null) {
			return set;
		}
		String settingsPage = prefix + text.getText().trim().toLowerCase().replaceAll("^@", "");
		PageDescriptor pd = _pages.get(settingsPage);
		if(pd == null) {
			return set;
		}
		try {
			// set the fields
			List<Text> items = child.getBodyTextItems(false);
			List<Text> annotations = new LinkedList<Text>(child.getAnnotationItems());
			List<Frame> seen = new LinkedList<Frame>(); // to avoid getting stuck in a loop
			seen.add(child);
			// find all the frames for this settings page
			while(!annotations.isEmpty()) {
				Text annotation = annotations.remove(0);
				Frame next = annotation.getChild();
				if(next != null && !seen.contains(next)) {
					items.addAll(next.getBodyTextItems(false));
					annotations.addAll(next.getAnnotationItems());
					seen.add(next);
				}
			}
			// parse all the settings items on this page
			for(Text t : items) {				
				AttributeValuePair avp = new AttributeValuePair(t.getText(), false);
				try {
					String settingName = avp.getAttributeOrValue().trim().toLowerCase();
					// System.out.println(avp.getAttributeOrValue().trim().toLowerCase().replaceAll("^@", ""));
					Setting s = pd.settings.get(settingName);//.replaceAll("^@", ""));
					if(s == null) {
						if(settingName.startsWith("//") || settingName.startsWith("@")) {
							continue;
						}
						// System.out.println("Couldn't find setting \"" + settingName + "\"");
						List<String> validPages = new LinkedList<String>();
						// if the setting isn't found on the current page, check all child pages
						for(String k : _pages.keySet()) {
							if(k.startsWith(settingsPage) && k.length() > settingsPage.length()) {
								// System.out.println(k + " is a child of " + settingsPage);
								PageDescriptor cpd = _pages.get(k);
								Setting tmp = cpd.settings.get(settingName);
								if(tmp != null) {
									// System.out.println("Found setting in subpage: " + k);
									s = tmp;
									validPages.add(k);
								}
							}
						}
						// System.out.println(s);
						if(s == null) {
							continue;
						}
						if(validPages.size() > 1) {
							StringBuffer warnMessage = new StringBuffer("Found multiple matching settings in the following settings subpages: ");
							String lastPage = "";
							for(String page : validPages) {
								warnMessage.append("\"" + page + "\",");
								lastPage = page;
							}
							warnMessage.deleteCharAt(warnMessage.length() - 1);
							warnMessage.append(" - choosing " + lastPage);
							MessageBay.warningMessage(warnMessage.toString());
						}
					}
					if(s.setSetting(t) && s instanceof VariableSetting) {
						set.add((VariableSetting) s);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// call the onParsed method if one exists
			if(pd.onParsed != null) {
				pd.onParsed.invoke(null, text);
			}
        } catch (Exception e) {
            e.printStackTrace();
            return set;
        }
		// if the page was a settings page, check if it has any subpages
		for(Text t : child.getTextItems()) {
			set.addAll(parseSettings(t, settingsPage + "."));
		}
		return set;
	}
	
	public static void generateSettingsTree(Text link) {
		generateSettingsTree("settings", link);
	}
	
	/**
	 * 
	 * Generates settings tree
	 * 
	 */
	private static void generateSettingsTree(String page, Text text) {
		FrameCreator frames = new FrameCreator(text.getParentOrCurrentFrame().getFramesetName(), text.getParentOrCurrentFrame().getPath(), page, false, false);
		// Frame frame = FrameIO.CreateFrame(text.getParentOrCurrentFrame().getFramesetName(), page, null);
		text.setLink(frames.getName());
		
		// add subpages of the current page
		for(String k : _pages.keySet()) {
			if(k.startsWith(page.toLowerCase()) && !k.equals(page)) {
				String name = k.substring(page.length() + 1);
				if(name.indexOf('.') != -1) {
					continue;
				}
				System.out.println("Generating " + name);
				generateSettingsTree(k, frames.addText(name.substring(0, 1).toUpperCase() + name.substring(1), null, null, null, false));
			}
		}
		
		frames.setLastY(frames.getLastY() + 20);
		
		// add settings of the current page
		PageDescriptor pd = _pages.get(page);
		if(pd != null) {
			for(String str : pd.orderedEntries) {
				String key = str.toLowerCase();
				Setting s = pd.settings.get(key);
				if(s == null) {
					continue;
				}
				String name = str.substring(0, 1).toUpperCase() + str.substring(1);
				String value = "";
				if(s instanceof GenericSetting && ((GenericSetting<?>) s).isPrimitive()) {
					if(((GenericSetting<?>) s).get() != null) {
						value = ": " + ((GenericSetting<?>) s).get();
					} else {
						value = ": ";
					}
				}
				int x = 0, y = 0;
				Text t;
				if(key.equals("pass")) {
					t = frames.addText("iw: org.expeditee.items.widgets.Password", null, null, null, false);
					Password pw = new Password(t, null);
					pw.setPassword((String) value);
					frames.getCurrentFrame().removeItem(t);
					frames.getCurrentFrame().addAllItems(pw.getItems());
					x = pw.getX() + pw.getWidth();
					y = pw.getY();
				} else {
					if(s instanceof GenericSetting && ((GenericSetting<?>) s).getType().equals(Text.class)) {
    					t = (Text) ((GenericSetting<?>)s).get();
    					if(t == null) {
    						System.err.println("Failed to get Text setting \"" + str + "\"");
    						continue;
    					}
    					t = t.copy();
    					t.setID(frames.getCurrentFrame().getNextItemID());
    					t.setText(name);
    					frames.addItem(t, false);
    				} else {
    					t = frames.addText(name + value, null, null, null, false);
    				}
					x = t.getX() + t.getBoundsWidth();
					y = t.getY();
				}
				x = Math.max(250, x + 20);
				Text tt = frames.getCurrentFrame().addText(x, y, "// " + s.getTooltip(), null);
				// rebuild to get the correct height since setWidth() doesn't immediately rebuild
				tt.rebuild(true);
				if(tt.getY() + tt.getBoundsHeight() > frames.getLastY()) {
					frames.setLastY(tt.getY() + tt.getBoundsHeight());
				}
			}
		}
		
		frames.save();
		//FrameIO.SaveFrame(frame);
	}
}
