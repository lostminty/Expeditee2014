package org.expeditee.settings.folders;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameUtils;
import org.expeditee.items.Text;
import org.expeditee.setting.ListSetting;
import org.expeditee.setting.Setting;
import org.expeditee.settings.UserSettings;

public class FolderSettings {

	public static final ListSetting<String> FrameDirs = new ListSetting<String>("Directories to look in for frames") {
		@Override
		public boolean setSetting(Text text) {
			_value.addAll(FrameUtils.getDirs(text));
			return true;
		}
	};
	public static final Setting FramesetDir = new Setting("Adds a directory to look in for frames") {
		@Override
		public boolean setSetting(Text text) {
			if(text.getText().indexOf(':') == -1) {
				return false;
    		}
    		AttributeValuePair avp = new AttributeValuePair(text.getText());
    		if(avp.getValue().trim().length() != 0) {
    			String dir = FrameUtils.getDir(avp.getValue());
    			if (dir != null) {
    				FolderSettings.FrameDirs.get().add(dir);
    				return true;
    			}
    		}
    		return false;
		}
	};

	public static ListSetting<String> ImageDirs = new ListSetting<String>("Directories to look in for images") {
		@Override
		public boolean setSetting(Text text) {
			_value.addAll(FrameUtils.getDirs(text));
			return true;
		}
	};
	public static final Setting ImageDir = new Setting("Adds a directory to look in for images") {
		@Override
		public boolean setSetting(Text text) {
    		if(text.getText().indexOf(':') == -1) {
    			return false;
    		}
    		AttributeValuePair avp = new AttributeValuePair(text.getText());
    		if(avp.getValue().trim().length() != 0) {
    			String dir = FrameUtils.getDir(avp.getValue());
    			if(dir != null) {
    				FolderSettings.ImageDirs.get().add(0, dir);
    				return true;
    			}
    		}
    		return false;
		}
	};
	
	public static final Setting LogDir = new Setting("Set the directory to save logs") {
		@Override
		public boolean setSetting(Text text) {
			if(text.getText().indexOf(':') == -1) {
    			return false;
    		}
    		AttributeValuePair avp = new AttributeValuePair(text.getText());
    		if(avp.getValue().trim().length() != 0) {
    			FrameIO.LOGS_DIR = FrameUtils.getDir(avp.getValue());
    		}
    		return true;
		}
	};
}
