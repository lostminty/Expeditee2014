package org.expeditee.gui;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Time;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.expeditee.actions.Actions;
import org.expeditee.agents.ExistingFramesetException;
import org.expeditee.io.Conversion;
import org.expeditee.io.ExpReader;
import org.expeditee.io.ExpWriter;
import org.expeditee.io.FrameReader;
import org.expeditee.io.FrameWriter;
import org.expeditee.io.KMSReader;
import org.expeditee.io.KMSWriter;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.PermissionPair;
import org.expeditee.items.Text;
import org.expeditee.items.UserAppliedPermission;
import org.expeditee.network.FrameShare;
import org.expeditee.settings.UserSettings;
import org.expeditee.settings.folders.FolderSettings;
import org.expeditee.settings.templates.TemplateSettings;
import org.expeditee.stats.Formatter;
import org.expeditee.stats.Logger;
import org.expeditee.stats.SessionStats;

/**
 * This class provides static methods for all saving and loading of Frames
 * to\from disk. This class also handles any caching of previously loaded
 * Frames.
 * 
 * @author jdm18
 * 
 */
public class FrameIO {

	private static final char FRAME_NAME_LAST_CHAR = 'A';

	public static void changeParentFolder(String newFolder) {
		PARENT_FOLDER = newFolder;
		PUBLIC_PATH = PARENT_FOLDER + "public" + File.separator;
		FRAME_PATH = PARENT_FOLDER + "framesets" + File.separator;
		MESSAGES_PATH = PARENT_FOLDER + "messages" + File.separator;
		TRASH_PATH = PARENT_FOLDER + "trash" + File.separator;
		IMAGES_PATH = PARENT_FOLDER + IMAGES_FOLDER;
		HELP_PATH = PARENT_FOLDER + "documentation" + File.separator;
		DICT_PATH = PARENT_FOLDER + "dict" + File.separator;
		FONT_PATH = PARENT_FOLDER + "fonts" + File.separator;
		PROFILE_PATH = PARENT_FOLDER + "profiles" + File.separator;
		EXPORTS_DIR = PARENT_FOLDER + "exports" + File.separator;
		STATISTICS_DIR = PARENT_FOLDER + "statistics" + File.separator;
		LOGS_DIR = PARENT_FOLDER + "logs" + File.separator;
	}

	/**
	 * The default location for storing the framesets. Each frameset has its own
	 * subdirectory in this directory.
	 */
	public static String IMAGES_FOLDER = "images" + File.separator;

	public static String TRASH_PATH;

	public static String PARENT_FOLDER;

	public static String FRAME_PATH;

	public static String MESSAGES_PATH;

	public static String PUBLIC_PATH;

	public static String IMAGES_PATH;

	public static String HELP_PATH;

	public static String FONT_PATH;
	
	public static String TEMPLATES_PATH;
	
	public static String DICT_PATH;

	public static String PROFILE_PATH;

	public static String EXPORTS_DIR;

	public static String STATISTICS_DIR;

	public static String LOGS_DIR;

	private static final String INF_FILENAME = "frame.inf";

	public static final String ILLEGAL_CHARS = ";:\\/?";

	public static final int MAX_NAME_LENGTH = 64;

	public static final int MAX_CACHE = 100;

	private static HashMap<String, Frame> _Cache = new FrameCache();

	// private static HashMap<String, String> _FramesetNameCache = new
	// HashMap<String, String>();

	private static boolean ENABLE_CACHE = true;

	private static boolean _UseCache = true;

	private static boolean _SuspendedCache = false;

	// All methods are static, this should not be instantiated
	private FrameIO() {
	}

	public static boolean isCacheOn() {
		return _UseCache && ENABLE_CACHE;
	}

	public static void Precache(String framename) {
		// if the cache is turned off, do nothing
		if (!isCacheOn())
			return;

		// if the frame is already in the cache, do nothing
		if (_Cache.containsKey(framename.toLowerCase()))
			return;

		// otherwise, load the frame and put it in the cache
		Logger.Log(Logger.SYSTEM, Logger.LOAD, "Precaching " + framename + ".");

		// do not display errors encountered to the user
		// (they will be shown at load time)
		MessageBay.suppressMessages(true);
		// loading automatically caches the frame is caching is turned on
		LoadFromDisk(framename, null, false);
		MessageBay.suppressMessages(false);
	}

	/**
	 * Checks if a string is a representation of a positive integer.
	 * 
	 * @param s
	 * @return true if s is a positive integer
	 */
	public static boolean isPositiveInteger(String s) {
		if (s == null || s.length() == 0)
			return false;

		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i)))
				return false;
		}
		return true;
	}

	public static Frame LoadFrame(String frameName) {
		return LoadFrame(frameName, null, false);
	}

	public static Frame LoadFrame(String frameName, String path) {
		return LoadFrame(frameName, path, false);
	}

	public static Frame LoadFrame(String frameName, String path,
			boolean ignoreAnnotations) {
		if (!isValidFrameName(frameName))
			return null;

		String frameNameLower = frameName.toLowerCase();
		// first try reading from cache
		if (isCacheOn() && _Cache.containsKey(frameNameLower)) {
			Logger.Log(Logger.SYSTEM, Logger.LOAD, "Loading " + frameName
					+ " from cache.");
			Frame frame = _Cache.get(frameNameLower);
			return frame;
		}

		Logger.Log(Logger.SYSTEM, Logger.LOAD, "Loading " + frameName
				+ " from disk.");

		return LoadFromDisk(frameName, path, ignoreAnnotations);
	}

	public static BufferedReader LoadPublicFrame(String frameName) {
		String fullPath = FrameIO.getFrameFullPathName(PUBLIC_PATH, frameName);

		if (fullPath == null)
			return null;

		File frameFile = new File(fullPath);
		if (frameFile.exists() && frameFile.canRead()) {
			try {
				return new BufferedReader(new FileReader(frameFile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static Frame LoadFromDisk(String framename, String knownPath,
			boolean ignoreAnnotations) {
		Frame loaded = null;

		if (knownPath != null) {
			loaded = LoadKnowPath(knownPath, framename);
		} else {

			for (String path : FolderSettings.FrameDirs.get()) {
				loaded = LoadKnowPath(path, framename);
				if (loaded != null) {
					break;
				}
			}
		}

		if (loaded == null && FrameShare.getInstance() != null) {
			loaded = FrameShare.getInstance().loadFrame(framename, knownPath);
		}

		if (loaded != null) {
			FrameUtils.Parse(loaded, true, ignoreAnnotations);
		}

		return loaded;
	}

	/**
	 * Gets a list of all the framesets available to the user
	 * 
	 * @return a string containing a list of all the available framesets on
	 *         separate lines
	 */
	public static String getFramesetList() {
		StringBuffer list = new StringBuffer();

		for (String path : FolderSettings.FrameDirs.get()) {
			File files = new File(path);
			if (!files.exists())
				continue;
			for (File f : (new File(path)).listFiles()) {
				if (f.isDirectory()) {
					list.append(f.getName()).append('\n');
				}
			}
		}
		// remove the final new line char
		list.deleteCharAt(list.length() - 1);
		return list.toString();
	}

	/**
	 * Gets the full path and file name of the frame.
	 * 
	 * @param path-
	 *            the directory in which to look for the frameset containing the
	 *            frame.
	 * @param frameName-
	 *            the name of the frame for which the path is being requested.
	 * @return null if the frame can not be located.
	 */
	public static synchronized String getFrameFullPathName(String path,
			String frameName) {
		String source = path + Conversion.getFramesetName(frameName)
				+ File.separator;

		File tester = new File(source);
		if (!tester.exists())
			return null;

		// check for the new file name format
		String fullPath = source + Conversion.getFrameNumber(frameName)
				+ ExpReader.EXTENTION;
		tester = new File(fullPath);

		if (tester.exists())
			return fullPath;

		// check for oldfile name format
		fullPath = source + Conversion.getFramesetName(frameName) + "."
				+ Conversion.getFrameNumber(frameName);
		tester = new File(fullPath);

		if (tester.exists())
			return fullPath;

		return null;
	}

	public static boolean canAccessFrame(String frameName) {
		Frame current = DisplayIO.getCurrentFrame();
		// Just in case the current frame is not yet saved...
		if (frameName.equals(current.getName())) {
			FrameIO.SaveFrame(current, false, false);
			current.change();
			return true;
		}

		for (String path : FolderSettings.FrameDirs.get()) {
			if (getFrameFullPathName(path, frameName) != null)
				return true;
		}
		return false;
	}

	public static Collection<String> searchFrame(String frameName,
			String pattern, String path) {
		String fullPath = null;
		if (path == null) {
			for (String possiblePath : FolderSettings.FrameDirs.get()) {
				fullPath = getFrameFullPathName(possiblePath, frameName);
				if (fullPath != null)
					break;
			}
		} else {
			fullPath = getFrameFullPathName(path, frameName);
		}
		// If the frame was not located return null
		if (fullPath == null)
			return null;
		Collection<String> results = new LinkedList<String>();
		// Open the file and search the text items
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fullPath));
			String next;
			while (reader.ready() && ((next = reader.readLine()) != null)) {
				if (next.startsWith("T")) {
					String toSearch = next.substring(2);
					if (toSearch.toLowerCase().contains(pattern))
						results.add(toSearch);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return results;
	}

	private static Frame LoadKnowPath(String path, String frameName) {
		String fullPath = getFrameFullPathName(path, frameName);
		if (fullPath == null)
			return null;

		try {
			FrameReader reader;

			if (fullPath.endsWith(ExpReader.EXTENTION)) {
				reader = new ExpReader(frameName);
			} else {
				reader = new KMSReader();
			}
			Frame frame = reader.readFrame(fullPath);

			if (frame == null) {
				MessageBay.errorMessage("Error: " + frameName
						+ " could not be successfully loaded.");
				return null;
			}

			frame.setPath(path);

			// do not put 0 frames or virtual frames into the cache
			// Why are zero frames not put in the cache
			if (_Cache.size() > MAX_CACHE)
				_Cache.clear();

			if (frame.getNumber() > 0 && isCacheOn())
				_Cache.put(frameName.toLowerCase(), frame);

			return frame;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			Logger.Log(ioe);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.Log(e);
			MessageBay.errorMessage("Error: " + frameName
					+ " could not be successfully loaded.");
		}

		return null;
	}

	public static void Reload() {
		// disable cache
		boolean cache = _UseCache;

		_UseCache = false;
		Frame fresh = FrameIO.LoadFrame(DisplayIO.getCurrentFrame().getName());
		_UseCache = cache;
		if (_Cache.containsKey(fresh.getName().toLowerCase()))
			addToCache(fresh);
		DisplayIO.setCurrentFrame(fresh, false);
	}

	public static Frame LoadPrevious(Frame current) {
		checkTDFC(current);

		// the current name and number
		String name = current.getFramesetName();
		int num = current.getNumber() - 1;

		// loop until a frame that exists is found
		for (; num >= 0; num--) {
			Frame f = LoadFrame(name + num, current.getPath());
			if (f != null)
				return f;
		}

		// if we did not find another Frame then this one must be the last one
		// in the frameset
		MessageBay
				.displayMessageOnce("This is the first frame in the frameset");
		return null;
	}

	/**
	 * Returns the next Frame in the current Frameset (The Frame with the next
	 * highest Frame number) If the current Frame is the last one in the
	 * Frameset, or an error occurs then null is returned.
	 * 
	 * @return The Frame after this one in the current frameset, or null
	 */
	public static Frame LoadNext(Frame current) {
		checkTDFC(current);

		// the current name and number
		int num = current.getNumber() + 1;
		int max = num + 1;
		String name = current.getFramesetName();

		// read the maximum from the INF file
		try {
			max = ReadINF(current.getPath(), current.getFramesetName(), false);
		} catch (IOException ioe) {
			MessageBay.errorMessage("Error loading INF file for frameset '"
					+ name + "'");
			return null;
		}

		// loop until a frame that exists is found
		for (; num <= max; num++) {
			Frame f = LoadFrame(name + num, current.getPath());
			if (f != null)
				return f;
		}

		// if we did not find another Frame then this one must be the last one
		// in the frameset
		MessageBay.displayMessageOnce("This is the last frame in the frameset");
		return null;
	}

	/**
	 * This method checks if the current frame has just been created with TDFC.
	 * If it has the frame is saved regardless of whether it has been edited or
	 * not and the TDFC item property is cleared. This is to ensure that the
	 * link is saved on the parent frame.
	 * 
	 * @param current
	 */
	public static void checkTDFC(Frame current) {
		if (FrameUtils.getTdfcItem() != null) {
			FrameUtils.setTdfcItem(null);
			current.change();
		}
	}

	public static Frame LoadLast(String framesetName, String path) {
		// read the maximum from the INF file
		int max;
		try {
			max = ReadINF(path, framesetName, false);
		} catch (IOException ioe) {
			MessageBay.errorMessage("Error loading INF file for frameset '"
					+ framesetName + "'");
			return null;
		}

		// loop backwards until a frame that exists is found
		for (int num = max; num > 0; num--) {
			Frame f = LoadFromDisk(framesetName + num, path, false);
			if (f != null)
				return f;
		}

		// if we did not find another Frame then this one must be the last one
		// in the frameset
		MessageBay.displayMessage("This is the last frame in the frameset");
		return null;
	}

	public static Frame LoadZero(String framesetName, String path) {
		return LoadFrame(framesetName + 0);
	}

	public static Frame LoadZero() {
		Frame current = DisplayIO.getCurrentFrame();
		return LoadZero(current.getFramesetName(), current.getPath());
	}

	public static Frame LoadLast() {
		Frame current = DisplayIO.getCurrentFrame();
		return LoadLast(current.getFramesetName(), current.getPath());
	}

	public static Frame LoadNext() {
		return LoadNext(DisplayIO.getCurrentFrame());
	}

	public static Frame LoadPrevious() {
		return LoadPrevious(DisplayIO.getCurrentFrame());
	}

	/**
	 * Deletes the given Frame on disk and removes the cached Frame if there is
	 * one. Also adds the deleted frame into the deletedFrames frameset.
	 * 
	 * @param toDelete
	 *            The Frame to be deleted
	 * @return The name the deleted frame was changed to, or null if the delete
	 *         failed
	 */
	public static String DeleteFrame(Frame toDelete) throws IOException,
			SecurityException {
		if (toDelete == null)
			return null;

		// Dont delete the zero frame
		if (toDelete.getNumber() == 0) {
			throw new SecurityException("Deleting a zero frame is illegal");
		}

		// Dont delete the zero frame
		if (!toDelete.isLocal()) {
			throw new SecurityException("Attempted to delete remote frame");
		}

		SaveFrame(toDelete);

		// Copy deleted frames to the DeletedFrames frameset
		// get the last used frame in the destination frameset
		final String DELETED_FRAMES = "DeletedFrames";
		int lastNumber = FrameIO.getLastNumber(DELETED_FRAMES);
		String framePath;
		try {
			// create the new frameset
			Frame one = FrameIO.CreateFrameset(DELETED_FRAMES, toDelete
					.getPath());
			framePath = one.getPath();
			lastNumber = 0;
		} catch (Exception e) {
			Frame zero = FrameIO.LoadFrame(DELETED_FRAMES + "0");
			framePath = zero.getPath();
		}

		// get the fill path to determine which file version it is
		String source = getFrameFullPathName(toDelete.getPath(), toDelete
				.getName());

		String oldFrameName = toDelete.getName().toLowerCase();
		// Now save the frame in the new location
		toDelete.setFrameset(DELETED_FRAMES);
		toDelete.setFrameNumber(lastNumber + 1);
		toDelete.setPath(framePath);
		ForceSaveFrame(toDelete);

		if (_Cache.containsKey(oldFrameName))
			_Cache.remove(oldFrameName);

		File del = new File(source);

		java.io.FileInputStream ff = new java.io.FileInputStream(del);
		ff.close();

		if (del.delete()) {
			return toDelete.getName();
		}

		return null;
	}

	/**
	 * Creates a new Frame in the given frameset and assigns it the given Title,
	 * which can be null. The newly created Frame is a copy of the frameset's .0
	 * file with the number updated based on the last recorded Frame name in the
	 * frameset's INF file.
	 * 
	 * @param frameset
	 *            The frameset to create the new Frame in
	 * @param frameTitle
	 *            The title to assign to the newly created Frame (can be NULL).
	 * @return The newly created Frame.
	 */
	public static synchronized Frame CreateFrame(String frameset,
			String frameTitle, String templateFrame) throws RuntimeException {

		if (!FrameIO.isValidFramesetName(frameset)) {
			throw new RuntimeException(frameset
					+ " is not a valid frameset name");
		}

		int next = -1;

		// disable caching of 0 frames
		// Mike says: Why is caching of 0 frames being disabled?
		/*
		 * Especially since 0 frames are not event put into the cache in the
		 * frist place
		 */
		// SuspendCache();
		/*
		 * Suspending the cache causes infinate loops when trying to load a zero
		 * frame which has a ao which contains an v or av which contains a link
		 * to the ao frame
		 */

		String zeroFrameName = frameset + "0";
		Frame destFramesetZero = LoadFrame(zeroFrameName);
		if (destFramesetZero == null) {
			throw new RuntimeException(zeroFrameName + " could not be found");
		}

		Frame template = null;
		if (templateFrame == null) {
			// load in frame.0
			template = destFramesetZero;
		} else {
			template = LoadFrame(templateFrame);
			if (template == null) {
				throw new RuntimeException("LinkTemplate " + templateFrame
						+ " could not be found");
			}
		}

		ResumeCache();

		// read the next number from the INF file
		try {
			next = ReadINF(destFramesetZero.getPath(), frameset, true);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException("INF file could not be read");
		}

		// Remove the old frame from the cashe then add the new one
		// TODO figure out some way that we can put both in the cache
		_Cache.remove(template.getName().toLowerCase());
		// set the number and title of the new frame
		template.setName(frameset, ++next);
		template.setTitle(frameTitle);
		// _Cache.put(template.getName().toLowerCase(), template);

		Logger.Log(Logger.SYSTEM, Logger.TDFC, "Creating new frame: "
				+ template.getName() + " from TDFC");

		template.setOwner(UserSettings.UserName.get());
		template.reset();
		template.resetDateCreated();

		for (Item i : template.getItems()) {
			if (ItemUtils.startsWithTag(i, ItemUtils.TAG_PARENT))
				i.setLink(null);
		}

		// do auto shrinking of the title IF not in twin frames mode
		Item titleItem = template.getTitleItem();

		if (!DisplayIO.isTwinFramesOn()) {
			if ((titleItem.getX() + 1) < template.getNameItem().getX()) {
				while (titleItem.getSize() > Text.MINIMUM_FONT_SIZE
						&& titleItem.getBoundsWidth() + titleItem.getX() > template
								.getNameItem().getX()) {
					titleItem.setSize(titleItem.getSize() - 1);

				}
			} else {
				System.out.println("Bad title x position: " + titleItem.getX());
			}
		}

		// Assign a width to the title.
		titleItem.setRightMargin(template.getNameItem().getX(), true);

		return template;
	}

	public static void DisableCache() {
		_UseCache = false;
	}

	public static void EnableCache() {
		_UseCache = true;
	}

	public static void SuspendCache() {
		if (_UseCache) {
			DisableCache();
			_SuspendedCache = true;
		} else {
			_SuspendedCache = false;
		}
	}

	public static void ResumeCache() {
		if (_SuspendedCache) {
			EnableCache();
			_SuspendedCache = false;
		}
	}

	public static void RefreshCasheImages() {
		SuspendCache();
		for (Frame f : _Cache.values())
			f.setBuffer(null);
		ResumeCache();
	}

	/**
	 * Creates a new frameset using the given name. This includes creating a new
	 * subdirectory in the <code>FRAME_PATH</code> directory, Copying over the
	 * default.0 frame from the default frameset, copying the .0 Frame to make a
	 * .1 Frame, and creating the frameset's INF file.
	 * 
	 * @param frameset
	 *            The name of the Frameset to create
	 * @return The first Frame of the new Frameset (Frame.1)
	 */
	public static Frame CreateFrameset(String frameset, String path)
			throws Exception {
		return CreateFrameset(frameset, path, false);
	}

	/**
	 * Tests if the given String is a 'proper' framename, that is, the String
	 * must begin with a character, end with a number with 0 or more letters and
	 * numbers in between.
	 * 
	 * @param frameName
	 *            The String to test for validity as a frame name
	 * @return True if the given framename is proper, false otherwise.
	 */
	public static boolean isValidFrameName(String frameName) {

		if (frameName == null || frameName.length() < 2)
			return false;

		int lastCharIndex = frameName.length() - 1;
		// String must begin with a letter and end with a digit
		if (!Character.isLetter(frameName.charAt(0))
				|| !Character.isDigit(frameName.charAt(lastCharIndex)))
			return false;

		// All the characters between first and last must be letters
		// or digits
		for (int i = 1; i < lastCharIndex; i++) {
			if (!isValidFrameNameChar(frameName.charAt(i)))
				return false;
		}
		return true;
	}

	private static boolean isValidFrameNameChar(char c) {
		return Character.isLetterOrDigit(c) || c == '-';
	}

	/**
	 * Saves the given Frame to disk in the corresponding frameset directory.
	 * This is the same as calling SaveFrame(toSave, true)
	 * 
	 * @param toSave
	 *            The Frame to save to disk
	 */
	public static String SaveFrame(Frame toSave) {
		return SaveFrame(toSave, true);
	}

	/**
	 * Saves a frame.
	 * 
	 * @param toSave
	 *            the frame to save
	 * @param inc
	 *            true if the frames counter should be incremented
	 * @return the text content of the frame
	 */
	public static String SaveFrame(Frame toSave, boolean inc) {
		return SaveFrame(toSave, inc, true);
	}

	/**
	 * Saves the given Frame to disk in the corresponding frameset directory, if
	 * inc is true then the saved frames counter is incremented, otherwise it is
	 * untouched.
	 * 
	 * @param toSave
	 *            The Frame to save to disk
	 * @param inc
	 *            True if the saved frames counter should be incremented, false
	 *            otherwise.
	 * @param checkBackup
	 *            True if the frame should be checked for the back up tag
	 */
	public static String SaveFrame(Frame toSave, boolean inc,
			boolean checkBackup) {

		// TODO When loading a frame maybe append onto the event history too-
		// with a
		// break to indicate the end of a session

		if (toSave == null || !toSave.hasChanged() || toSave.isSaved()) {
			return "";
		}

		// Dont save if the frame is protected and it exists
		if (checkBackup && toSave.isReadOnly()) {
			_Cache.remove(toSave.getName().toLowerCase());
			return "";
		}

		/* Dont save the frame if it has the noSave tag */
		if (toSave.hasAnnotation("nosave")) {
			Actions.PerformActionCatchErrors(toSave, null, "Restore");
			return "";
		}

		// Save frame that is not local through the Networking classes
		// TODO
		if (!toSave.isLocal()) {
			return FrameShare.getInstance().saveFrame(toSave);
		}

		/* Format the frame if it has the autoFormat tag */
		if (toSave.hasAnnotation("autoformat")) {
			Actions.PerformActionCatchErrors(toSave, null, "Format");
		}

		/**
		 * Get the full path only to determine which format to use for saving
		 * the frame. At this stage use Exp format for saving Exp frames only.
		 * Later this will be changed so that KMS frames will be updated to the
		 * Exp format.
		 */
		String fullPath = getFrameFullPathName(toSave.getPath(), toSave
				.getName());

		// Check if the frame exists
		if (checkBackup && fullPath == null) {
			// The first time a frame with the backup tag is saved, dont back it
			// up
			checkBackup = false;
		}

		FrameWriter writer = null;
		int savedVersion;
		try {
			// if its a new frame or an existing Exp frame...
			if (fullPath == null || fullPath.endsWith(ExpReader.EXTENTION)) {
				writer = new ExpWriter();
				savedVersion = ExpReader.getVersion(fullPath);
			} else {
				writer = new KMSWriter();
				savedVersion = KMSReader.getVersion(fullPath);
			}

			// Check if the frame doesnt exist
			// if (savedVersion < 0) {
			// /*
			// * This will happen if the user has two Expeditee's running at
			// * once and closes the first. When the second one closes the
			// * messages directory will have been deleted.
			// */
			// MessageBay
			// .errorMessage("Could not save frame that does not exist: "
			// + toSave.getName());
			// return null;
			// }

			// Check if we are trying to save an out of date version
			if (savedVersion > toSave.getVersion()
					&& !toSave.getFramesetName().equalsIgnoreCase(
							MessageBay.MESSAGES_FRAMESET_NAME)) {
				// remove this frame from the cache if it is there
				// This will make sure links to the original are set correctly
				_Cache.remove(toSave.getName().toLowerCase());
				int nextnum = ReadINF(toSave.getPath(), toSave
						.getFramesetName(), false) + 1;
				SuspendCache();
				Frame original = LoadFrame(toSave.getName());
				toSave.setFrameNumber(nextnum);
				ResumeCache();
				// Put the modified version in the cache
				addToCache(toSave);
				// Show the messages alerting the user
				Text originalMessage = new Text(-1);
				originalMessage.setColor(MessageBay.ERROR_COLOR);
				originalMessage.setText(original.getName()
						+ " was updated by another user.");
				originalMessage.setLink(original.getName());
				Text yourMessage = new Text(-1);
				yourMessage.setColor(MessageBay.ERROR_COLOR);
				yourMessage.setText("Your version was renamed "
						+ toSave.getName());
				yourMessage.setLink(toSave.getName());
				MessageBay.displayMessage(originalMessage);
				MessageBay.displayMessage(yourMessage);
			} else if (checkBackup
					&& ItemUtils.ContainsExactTag(toSave.getItems(),
							ItemUtils.TAG_BACKUP)) {
				SuspendCache();
				String oldFramesetName = toSave.getFramesetName() + "-old";

				Frame original = LoadFrame(toSave.getName());
				if (original == null)
					original = toSave;
				int orignum = original.getNumber();

				int nextnum = -1;
				try {
					nextnum = ReadINF(toSave.getPath(), oldFramesetName, false) + 1;
				} catch (RuntimeException e) {
					try {
						CreateFrameset(oldFramesetName, toSave.getPath());
						nextnum = 1;
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					// e.printStackTrace();
				}

				if (nextnum > 0) {
					original.setFrameset(oldFramesetName);
					original.setFrameNumber(nextnum);
					original.setPermission(new PermissionPair(UserAppliedPermission.copy));
					original.change();
					SaveFrame(original, false, false);
				}

				Item i = ItemUtils.FindExactTag(toSave.getItems(),
						ItemUtils.TAG_BACKUP);
				i.setLink(original.getName());
				toSave.setFrameNumber(orignum);
				ResumeCache();
			}
			// Update general stuff about frame
			setSavedProperties(toSave);

			// int oldMode = FrameGraphics.getMode();
			// if (oldMode != FrameGraphics.MODE_XRAY)
			// FrameGraphics.setMode(FrameGraphics.MODE_XRAY, true);
			writer.writeFrame(toSave);
			// FrameGraphics.setMode(oldMode, true);
			toSave.setSaved();
			if (inc) {
				SessionStats.SavedFrame(toSave.getName());
			}

			// avoid out-of-sync frames (when in TwinFrames mode)
			if (_Cache.containsKey(toSave.getName().toLowerCase()))
				addToCache(toSave);

			Logger.Log(Logger.SYSTEM, Logger.SAVE, "Saving " + toSave.getName()
					+ " to disk.");

			// check that the INF file is not out of date
			int last = ReadINF(toSave.getPath(), toSave.getFramesetName(),
					false);
			if (last <= toSave.getNumber())
				WriteINF(toSave.getPath(), toSave.getFramesetName(), toSave
						.getName());

			// check if this was the profile frame (and thus needs
			// re-parsing)
			if (isProfileFrame(toSave)) {
				Frame profile = FrameIO.LoadFrame(toSave.getFramesetName()
						+ "1");
				assert (profile != null);
				FrameUtils.ParseProfile(profile);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			ioe.getStackTrace();
			Logger.Log(ioe);
			return null;
		}

		return writer.getFileContents();
	}

	/**
	 * @param toAdd
	 */
	public static void addToCache(Frame toAdd) {
		_Cache.put(toAdd.getName().toLowerCase(), toAdd);
	}

	/**
	 * Checks if a frame is in the current user profile frameset.
	 * 
	 * @param toCheck
	 *            the frame to check
	 * @return true if the frame is in the current user profile frameset
	 */
	public static boolean isProfileFrame(Frame toCheck) {
		if (toCheck.getNumber() == 0)
			return false;
		return toCheck.getPath().equals(PROFILE_PATH);
		// return toCheck.getFramesetName()
		// .equalsIgnoreCase(UserSettings.ProfileName);
	}

	public static Frame LoadProfile(String userName) {
		return LoadFrame(userName + "1");
	}

	public static Frame CreateNewProfile(String username) throws Exception {
		Frame profile = CreateFrameset(username, PROFILE_PATH, true);
		FrameUtils.CreateDefaultProfile(username, profile);
		return profile;
	}

	/**
	 * Reads the INF file that corresponds to the given Frame name
	 * 
	 * @param framename
	 *            The Frame to lookup the INF file for
	 * @throws IOException
	 *             Any exceptions encountered by the BufferedReader used to read
	 *             the INF.
	 */
	public static int ReadINF(String path, String frameset, boolean update)
			throws IOException {
		assert (!frameset.endsWith("."));
		try {
			// read INF
			BufferedReader reader;
			try {
				// Check on the local drive
				reader = new BufferedReader(new FileReader(path
						+ frameset.toLowerCase() + File.separator
						+ INF_FILENAME));
			} catch (Exception e) {
				reader = new BufferedReader(new FileReader(path
						+ frameset.toLowerCase() + File.separator
						+ frameset.toLowerCase() + ".inf"));
			}
			String inf = reader.readLine();
			reader.close();

			int next = Conversion.getFrameNumber(inf);
			// update INF file
			if (update) {
				try {
					WriteINF(path, frameset, frameset + (next + 1));
				} catch (IOException ioe) {
					ioe.printStackTrace();
					Logger.Log(ioe);
				}
			}
			return next;
		} catch (Exception e) {
		}

		// Check peers
		return FrameShare.getInstance().getInfNumber(path, frameset, update);
	}

	/**
	 * Writes the given String out to the INF file corresponding to the current
	 * frameset.
	 * 
	 * @param toWrite
	 *            The String to write to the file.
	 * @throws IOException
	 *             Any exception encountered by the BufferedWriter.
	 */
	public static void WriteINF(String path, String frameset, String frameName)
			throws IOException {
		try {
			assert (!frameset.endsWith("."));

			path += frameset.toLowerCase() + File.separator + INF_FILENAME;

			BufferedWriter writer = new BufferedWriter(new FileWriter(path));
			writer.write(frameName);
			writer.close();
		} catch (Exception e) {

		}
	}

	public static boolean FrameIsCached(String name) {
		return _Cache.containsKey(name);
	}

	/**
	 * Gets a frame from the cache.
	 * 
	 * @param name
	 *            The frame to get from the cache
	 * 
	 * @return The frame from cache. Null if not cached.
	 */
	public static Frame FrameFromCache(String name) {
		return _Cache.get(name);
	}

	public static String ConvertToValidFramesetName(String toValidate) {
		assert (toValidate != null && toValidate.length() > 0);

		StringBuffer result = new StringBuffer();

		if (Character.isDigit(toValidate.charAt(0))) {
			result.append(FRAME_NAME_LAST_CHAR);
		}

		boolean capital = false;
		for (int i = 0; i < toValidate.length()
				&& result.length() < MAX_NAME_LENGTH; i++) {
			char cur = toValidate.charAt(i);

			// capitalize all characters after spaces
			if (Character.isLetterOrDigit(cur)) {
				if (capital) {
					capital = false;
					result.append(Character.toUpperCase(cur));
				} else
					result.append(cur);
			} else {
				capital = true;
			}
		}
		assert (result.length() > 0);
		int lastCharIndex = result.length() - 1;
		if (!Character.isLetter(result.charAt(lastCharIndex))) {
			if (lastCharIndex == MAX_NAME_LENGTH - 1)
				result.setCharAt(lastCharIndex, FRAME_NAME_LAST_CHAR);
			else
				result.append(FRAME_NAME_LAST_CHAR);
		}

		assert (isValidFramesetName(result.toString()));
		return result.toString();
	}

	public static Frame CreateNewFrame(Item linker) throws RuntimeException {
		String title = linker.getName();

		String templateLink = linker.getAbsoluteLinkTemplate();
		String framesetLink = linker.getAbsoluteLinkFrameset();
		String frameset = (framesetLink != null ? framesetLink : DisplayIO
				.getCurrentFrame().getFramesetName());

		Frame newFrame = FrameIO.CreateFrame(frameset, title, templateLink);
		return newFrame;
	}
	
	public static Frame CreateNewFrame(Item linker, OnNewFrameAction action) throws RuntimeException {
	    Frame newFrame = FrameIO.CreateNewFrame(linker);
	    if(action != null) action.exec(linker, newFrame);
	    return newFrame;
	}

	/**
	 * Creates a new Frameset on disk, including a .0, .1, and .inf files. The
	 * Default.0 frame is copied to make the initial .0 and .1 Frames
	 * 
	 * @param name
	 *            The Frameset name to use
	 * @return The name of the first Frame in the newly created Frameset (the .1
	 *         frame)
	 */
	public static Frame CreateNewFrameset(String name) throws Exception {
		String path = DisplayIO.getCurrentFrame().getPath();

		// if current frameset is profile directory change it to framesets
		if (path.equals(FrameIO.PROFILE_PATH)) {
			path = FrameIO.FRAME_PATH;
		}

		Frame newFrame = FrameIO.CreateFrameset(name, path);

		if (newFrame == null) {
			// Cant create directories if the path is readonly or there is no
			// space available
			newFrame = FrameIO.CreateFrameset(name, FrameIO.FRAME_PATH);
		}

		if (newFrame == null) {
			// TODO handle running out of disk space here
		}

		return newFrame;
	}

	/**
	 * 
	 * @param frameset
	 * @return
	 */
	public static int getLastNumber(String frameset) { // Rob thinks it might
		// have been
		// GetHighestNumExFrame
		// TODO minimise the number of frames being read in!!
		int num = -1;

		Frame zero = LoadFrame(frameset + "0");

		// the frameset does not exist (or has no 0 frame)
		if (zero == null)
			return -1;

		try {
			num = ReadINF(zero.getPath(), frameset, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		/*
		 * Michael doesnt think the code below is really needed... it will just
		 * slow things down when we are reading frames over a network***** for (;
		 * num >= 0; num--) { System.out.println("This code is loading frames to
		 * find the highest existing frame..."); if (LoadFrame(frameset + num) !=
		 * null) break; }
		 */

		return num;
	}

	/**
	 * Checks if a given frameset is accessable.
	 * 
	 * @param framesetName
	 * @return
	 */
	public static Boolean canAccessFrameset(String framesetName) {
		framesetName = framesetName.toLowerCase();
		for (String path : FolderSettings.FrameDirs.get()) {
			if ((new File(path + framesetName)).exists())
				return true;
		}
		return false;
	}

	public static Frame CreateFrameset(String frameset, String path,
			boolean recreate) throws Exception {
		String conversion = frameset + " --> ";

		if (!isValidFramesetName(frameset)) {
			throw new Exception("Invalid frameset name");
		}

		if (!recreate && FrameIO.canAccessFrameset(frameset)) {
			throw new ExistingFramesetException(frameset);
		}

		conversion += frameset;
		Logger.Log(Logger.SYSTEM, Logger.NEW_FRAMESET, "Frameset Name: "
				+ conversion);
		conversion = frameset;

		/**
		 * TODO: Update this to exclude any\all invalid filename characters
		 */
		// ignore annotation character
		if (frameset.startsWith("@"))
			frameset = frameset.substring(1);

		conversion += " --> " + frameset;
		Logger.Log(Logger.SYSTEM, Logger.NEW_FRAMESET, "Name: " + conversion);

		// create the new Frameset directory
		File dir = new File(path + frameset.toLowerCase() + File.separator);

		// If the directory doesnt already exist then create it...
		if (!dir.exists()) {
			// If the directory couldnt be created, then there is something
			// wrong... ie. The disk is full.
			if (!dir.mkdirs()) {
				return null;
			}
		}

		// create the new INF file
		try {
			WriteINF(path, frameset, frameset + '1');
		} catch (IOException ioe) {
			ioe.printStackTrace();
			Logger.Log(ioe);
		}

		SuspendCache();
		// copy the default .0 and .1 files
		Frame base = null;
		try {
			base = LoadFrame(TemplateSettings.DefaultFrame.get());
		} catch (Exception e) {
		}
		// The frame may not be accessed for various reasons... in all these
		// cases just create a new one
		if (base == null) {
			base = new Frame();
		}

		ResumeCache();

		base.reset();
		base.resetDateCreated();
		base.setFrameset(frameset);
		base.setFrameNumber(0);
		base.setTitle(base.getFramesetName() + "0");
		base.setPath(path);
		base.change();
		base.setOwner(UserSettings.UserName.get());
		SaveFrame(base, false);

		base.reset();
		base.resetDateCreated();
		base.setFrameNumber(1);
		base.setTitle(frameset);
		base.change();
		base.setOwner(UserSettings.UserName.get());
		SaveFrame(base, true);

		Logger.Log(Logger.SYSTEM, Logger.NEW_FRAMESET, "Created new frameset: "
				+ frameset);

		return base;
	}

	/**
	 * Tests if a frameset name is valid. That is it must begin and end with a
	 * letter and contain only letters and digits in between.
	 * 
	 * @param frameset
	 *            the name to be tested
	 * @return true if the frameset name is valid
	 */
	public static boolean isValidFramesetName(String frameset) {
		if (frameset == null) {
			return false;
		}

		int nameLength = frameset.length();
		if (frameset.length() <= 0 || nameLength > MAX_NAME_LENGTH) {
			return false;
		}

		int lastCharIndex = nameLength - 1;

		if (!Character.isLetter(frameset.charAt(0))
				|| !Character.isLetter(frameset.charAt(lastCharIndex)))
			return false;

		for (int i = 1; i < lastCharIndex; i++) {
			if (!isValidFrameNameChar(frameset.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean deleteFrameset(String framesetName) {
		return moveFrameset(framesetName, FrameIO.TRASH_PATH);
	}

	public static boolean moveFrameset(String framesetName,
			String destinationFolder) {
		if (!FrameIO.canAccessFrameset(framesetName))
			return false;
		// Clear the cache
		_Cache.clear();

		// Search all the available directories for the directory
		for (String path : FolderSettings.FrameDirs.get()) {
			String source = path + framesetName.toLowerCase() + File.separator;
			File framesetDirectory = new File(source);
			// Once we have found the directory move it
			if (framesetDirectory.exists()) {
				String destPath = destinationFolder
						+ framesetName.toLowerCase();
				int copyNumber = 1;
				File dest = new File(destPath + File.separator);
				// Create the destination folder if it doesnt already exist
				if (!dest.getParentFile().exists())
					dest.mkdirs();
				// If a frameset with the same name is already in the
				// destination add
				// a number to the end
				while (dest.exists()) {
					dest = new File(destPath + ++copyNumber + File.separator);
				}
				if (!framesetDirectory.renameTo(dest)) {
					for (File f : framesetDirectory.listFiles()) {
						if (!f.delete())
							return false;
					}
					if (!framesetDirectory.delete())
						return false;
				}
				return true;
			}
		}
		return false;
	}

	public static boolean CopyFrameset(String framesetToCopy,
			String copiedFrameset) throws Exception {
		if (!FrameIO.canAccessFrameset(framesetToCopy))
			return false;
		if (FrameIO.canAccessFrameset(copiedFrameset))
			return false;
		// search through all the directories to find the frameset we are
		// copying
		for (String path : FolderSettings.FrameDirs.get()) {
			String source = path + framesetToCopy.toLowerCase()
					+ File.separator;
			File framesetDirectory = new File(source);
			if (framesetDirectory.exists()) {
				// copy the frameset
				File copyFramesetDirectory = new File(path
						+ copiedFrameset.toLowerCase() + File.separator);
				if (!copyFramesetDirectory.mkdirs())
					return false;
				// copy each of the frames
				for (File f : framesetDirectory.listFiles()) {
					// Ignore hidden files
					if (f.getName().charAt(0) == '.')
						continue;
					String copyPath = copyFramesetDirectory.getAbsolutePath()
							+ File.separator + f.getName();
					FrameIO.copyFile(f.getAbsolutePath(), copyPath);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Copies a file from one location to another.
	 * 
	 * @param existingFile
	 * @param newFileName
	 * @throws Exception
	 */
	public static void copyFile(String existingFile, String newFileName)
			throws Exception {
		FileInputStream is = new FileInputStream(existingFile);
		FileOutputStream os = new FileOutputStream(newFileName, false);
		int data;
		while ((data = is.read()) != -1) {
			os.write(data);
		}
		os.flush();
		os.close();
		is.close();
	}

	/**
	 * Saves a frame regardless of whether or not the frame is marked as having
	 * been changed.
	 * 
	 * @param frame
	 *            the frame to save
	 * @return the contents of the frame or null if it could not be saved
	 */
	public static String ForceSaveFrame(Frame frame) {
		frame.change();
		return SaveFrame(frame, false);
	}

	public static boolean isValidLink(String frameName) {
		return frameName == null || isPositiveInteger(frameName)
				|| isValidFrameName(frameName);
	}

	public static void SavePublicFrame(String peerName, String frameName,
			int version, BufferedReader packetContents) {
		// TODO handle versioning - add version to the header
		// Remote user uploads version based on an old version

		// Remove it from the cache so that next time it is loaded we get the up
		// todate version
		_Cache.remove(frameName.toLowerCase());

		// Save to file
		String filename = PUBLIC_PATH + Conversion.getFramesetName(frameName)
				+ File.separator + Conversion.getFrameNumber(frameName)
				+ ExpReader.EXTENTION;

		File file = new File(filename);
		// Ensure the file exists
		if (file.exists()) {
			// Check the versions
			int savedVersion = ExpReader.getVersion(filename);

			if (savedVersion > version) {
				// remove this frame from the cache if it is there
				// This will make sure links to the original are set correctly
				// _Cache.remove(frameName.toLowerCase());

				int nextNum = 0;
				try {
					nextNum = ReadINF(PUBLIC_PATH, Conversion
							.getFramesetName(frameName), false) + 1;
				} catch (IOException e) {
					e.printStackTrace();
				}

				String newName = Conversion.getFramesetName(frameName)
						+ nextNum;
				filename = PUBLIC_PATH + Conversion.getFramesetName(frameName)
						+ File.separator + nextNum + ExpReader.EXTENTION;

				// Show the messages alerting the user
				Text originalMessage = new Text(-1);
				originalMessage.setColor(MessageBay.ERROR_COLOR);
				originalMessage.setText(frameName + " was edited by "
						+ peerName);
				originalMessage.setLink(frameName);
				Text yourMessage = new Text(-1);
				yourMessage.setColor(MessageBay.ERROR_COLOR);
				yourMessage.setText("Their version was renamed " + newName);
				yourMessage.setLink(newName);
				MessageBay.displayMessage(originalMessage);
				MessageBay.displayMessage(yourMessage);

				Frame editedFrame = FrameIO.LoadFrame(frameName);

				FrameShare.getInstance().sendMessage(
						frameName + " was recently edited by "
								+ editedFrame.getLastModifyUser(), peerName);
				FrameShare.getInstance().sendMessage(
						"Your version was renamed " + newName, peerName);
			}
		}

		// Save the new version
		try {
			// FileWriter fw = new FileWriter(file);

			// Open an Output Stream Writer to set encoding
			OutputStream fout = new FileOutputStream(file);
			OutputStream bout = new BufferedOutputStream(fout);
			Writer fw = new OutputStreamWriter(bout, "UTF-8");

			String nextLine = null;
			while ((nextLine = packetContents.readLine()) != null) {
				fw.write(nextLine + '\n');
			}
			fw.flush();
			fw.close();
			MessageBay.displayMessage("Saved remote frame: " + frameName);
		} catch (IOException e) {
			MessageBay.errorMessage("Error remote saving " + frameName + ": "
					+ e.getMessage());
			e.printStackTrace();
		}
		// } else {
		//			
		//			
		//			
		// MessageBay
		// .errorMessage("Recieved save request for unknown public frame: "
		// + frameName);
		// }
	}

	public static void setSavedProperties(Frame toSave) {
		toSave.setLastModifyDate(Formatter.getDateTime());
		toSave.setLastModifyUser(UserSettings.UserName.get());
		toSave.setVersion(toSave.getVersion() + 1);
		Time darkTime = new Time(SessionStats.getFrameDarkTime().getTime()
				+ toSave.getDarkTime().getTime());
		Time activeTime = new Time(SessionStats.getFrameActiveTime().getTime()
				+ toSave.getActiveTime().getTime());
		toSave.setDarkTime(darkTime);
		toSave.setActiveTime(activeTime);
	}

}