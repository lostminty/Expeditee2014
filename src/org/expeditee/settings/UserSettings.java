package org.expeditee.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.agents.SearchGreenstone;
import org.expeditee.agents.mail.MailSession;
import org.expeditee.agents.wordprocessing.JSpellChecker;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Text;
import org.expeditee.setting.BooleanSetting;
import org.expeditee.setting.FloatSetting;
import org.expeditee.setting.FrameSetting;
import org.expeditee.setting.FunctionSetting;
import org.expeditee.setting.IntegerSetting;
import org.expeditee.setting.ListSetting;
import org.expeditee.setting.StringSetting;
import org.expeditee.settings.folders.FolderSettings;

/**
 * Central class to contain all values that can be set by the user on their
 * profile frame. These values should be updated only by
 * FrameUtils.ParseProfile.
 */
public abstract class UserSettings {
	
	public final static String DEFAULT_PROFILE_NAME = "default";
	
	public static final IntegerSetting Gravity = new IntegerSetting("Distance the cursor has to be from a text item to select the text item", 3);
	
	public static final StringSetting StartFrame = new StringSetting("The frame to go to when Expeditee is started (defaults to the profile frame)", null);
	
	/*
	 * Stuff that goes first
	 */
	public static final StringSetting HomeFrame = new StringSetting("The home frame", null) {
		@Override
		public boolean setSetting(Text text) {
    		if(text.getText().indexOf(':') == -1 || !text.hasLink()) {
    			text.setLink(FrameIO.LoadProfile(UserSettings.ProfileName.get()).getName());
    		}
    		String first = FrameUtils.getLink(text, UserSettings.HomeFrame.get());
    		// do not use non-existant frames as the first frame
    		if (FrameIO.isValidFrameName(first)) {
    			_value = first;
    		}
    		// warn the user
    		else {
    			// MessageBay.warningMessage("Home frame: " + first
    			// + " is not a valid frame.");
    			_value = FrameIO.LoadProfile(UserSettings.ProfileName.get()).getName();
    		}
    		return true;
		}
	};
	public static final IntegerSetting InitialWidth = new IntegerSetting("Initial width of Expeditee window", 1024);
	
	public static final IntegerSetting InitialHeight = new IntegerSetting("Initial height of Expeditee window", 768);
	
	/*
	 * General settings (no setter functions)
	 */
	
	public static final FloatSetting ScaleFactor = new FloatSetting("Scale Factor for drawing (TODO: does this even do anything?)", 1F);
	
	public static final FloatSetting FormatSpacingMin = new FloatSetting("Minimum spacing ratio", null);
	
	public static final FloatSetting FormatSpacingMax = new FloatSetting("Maximum spacing ratio", null);

	public static final IntegerSetting LineStraightenThreshold = new IntegerSetting("Threshold for straightening a line (TODO: does this even do anything?)", 15);

	public static final IntegerSetting NoOpThreshold = new IntegerSetting("Distance the cursor may be dragged while clicking before the operation is cancelled", 60);
	
	public static final IntegerSetting TitlePosition = new IntegerSetting("Position of title item in frame (TODO: find whether this is x-offset or y-offset)", 150);
	
	public static final StringSetting ProfileName = new StringSetting("Profile name", FrameIO.ConvertToValidFramesetName(System.getProperty("user.name")));
	
	public static final StringSetting UserName = new StringSetting("User name", ProfileName.get());
	
	public static final BooleanSetting AntiAlias = new BooleanSetting("Whether anti-aliasing should be enabled", false);

	public static final BooleanSetting LineHighlight = new BooleanSetting("Whether lines should be highlighted", false);

	public static final BooleanSetting Logging = new BooleanSetting("Whether logging should be enabled", false);

	public static final BooleanSetting LogStats = new BooleanSetting("Whether stats should be logged", true);

	public static final BooleanSetting Threading = new BooleanSetting("Whether threading should be enabled", true);
	
	
	/*
	 * Frames
	 */
	
	public static final StringSetting StatisticsFrameset = new StringSetting("The statistics frameset", null);

	public static final StringSetting MenuFrame = new StringSetting("The menu frame", null);
	
	/*
	 * Other
	 */
	public static final ListSetting<Text> Style = new ListSetting<Text>("Set the style (TODO: what does this do?)") {		
		@Override
		public boolean setSetting(Text text) {
			Frame child = text.getChild();
    		if (child == null) {
    			_value = new LinkedList<Text>();
    			return true;
    		}
    		
    
    		List<Text> style = new ArrayList<Text>(8);
    		for (int i = 0; i < 10; i++) {
    			style.add(null);
    		}
    
    		for (Text t : child.getBodyTextItems(false)) {
    			String type = t.getText();
    			char lastChar = type.charAt(type.length() - 1);
    			if (Character.isDigit(lastChar)) {
    				style.set(lastChar - '0', t);
    			} else {
    				style.set(0, t);
    			}
    		}
    		_value = style;
    		return true;
		}
	};
	
	public static final FunctionSetting SpellChecker = new FunctionSetting("Enables the dictionary with the default dictionary") {
		@Override
		public void run() {
			try {
    			JSpellChecker.create();
    		} catch (FileNotFoundException e) {
    			MessageBay.errorMessage("Could not find dictionary: " + e.getMessage());
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
		}
	};
	public static final FrameSetting Spelling = new FrameSetting("Enables the dictionary and adds the items in the child frame to the dictionary") {
		@Override
		public void run(Frame frame) {
       		try {
       	        JSpellChecker.create(frame);
       		} catch (Exception e) {
       			e.printStackTrace();
			}
		}
	};
	
	public static final FrameSetting GreenstoneSettings = new FrameSetting("Greenstone settings (TODO: What are these for?)") {
		@Override
		public void run(Frame frame) {
			SearchGreenstone.init(frame);
		}
	};
	
	public static final FrameSetting Reminders = new FrameSetting("Reminders (TODO: What are these for?)") {
		@Override
		public void run(Frame frame) {
			org.expeditee.gui.Reminders.init(frame);
		}
	};
	
	public static final FrameSetting MailSettings = new FrameSetting("Mail Settings (TODO: How does this work?)") {
		@Override
		public void run(Frame frame) {
			MailSession.init(frame);
		}
	};

	// add default values
	static {
		String expeditee_home = System.getProperty("expeditee.home");
		if (expeditee_home != null) {
		    FrameIO.changeParentFolder(expeditee_home + File.separator);
		} else {
		    FrameIO.changeParentFolder(getSaveLocation());
		}
	  
		FolderSettings.FrameDirs.get().add(FrameIO.FRAME_PATH);
		FolderSettings.FrameDirs.get().add(FrameIO.PUBLIC_PATH);
		FolderSettings.FrameDirs.get().add(FrameIO.PROFILE_PATH);
		FolderSettings.FrameDirs.get().add(FrameIO.HELP_PATH);
		FolderSettings.FrameDirs.get().add(FrameIO.MESSAGES_PATH);
		FolderSettings.FrameDirs.setDefault(FolderSettings.FrameDirs.get());
		FolderSettings.ImageDirs.get().add(FrameIO.IMAGES_PATH);
		FolderSettings.ImageDirs.setDefault(FolderSettings.ImageDirs.get());
	}

	/**
	 * Find the appropriate directory to store application settings in for
	 * the current OS.
	 * This has only been tested on Linux so far, so if it doesn't work it
	 * may need to be modified or reverted. Should return:
	 * 	Linux: ~/.expeditee
	 * 	Windows: %appdata%\.expeditee
	 * 	Mac: ~/Library/Application\ Support/.expeditee
	 * @return the path to store expeditee's settings
	 */
	public static String getSaveLocation() {
		String OS=System.getProperty("os.name").toLowerCase();
		if(OS.indexOf("win")>=0) {			//windoze
			return System.getenv("APPDATA")+File.separator+".expeditee"+File.separator;
		} else if(OS.indexOf("mac")>=0) {	//mac
			return System.getProperty("user.home")+File.separator+"Library"+File.separator+"Application Support"+File.separator+".expeditee"+File.separator;
		} else {							//linux or other
			return System.getProperty("user.home")+File.separator+".expeditee"+File.separator;
		}
	}
}
