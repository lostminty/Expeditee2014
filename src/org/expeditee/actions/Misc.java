package org.expeditee.actions;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import javax.imageio.ImageIO;

import org.expeditee.gui.AttributeUtils;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameKeyboardActions;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.FreeItems;
import org.expeditee.gui.MessageBay;
import org.expeditee.gui.MessageBay.Progress;
import org.expeditee.gui.Reminders;
import org.expeditee.gui.TimeKeeper;
import org.expeditee.importer.FrameDNDTransferHandler;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Line;
import org.expeditee.items.Text;
import org.expeditee.items.XRayable;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;
import org.expeditee.math.ExpediteeJEP;
import org.expeditee.settings.UserSettings;
import org.expeditee.simple.SString;
import org.expeditee.stats.CometStats;
import org.expeditee.stats.DocumentStatsFast;
import org.expeditee.stats.SessionStats;
import org.expeditee.stats.StatsLogger;
import org.expeditee.stats.TreeStats;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;



/**
 * A list of miscellaneous Actions and Actions specific to Expeditee
 * 
 */
public class Misc {
	
	/**
	 * Causes the system to beep
	 */
	public static void beep() {
		java.awt.Toolkit.getDefaultToolkit().beep();
	}
	
	/**
	 * Returns an Item located at the specified position.
	 * kgas1 - 23/01/2012	
	 * @param x
	 * @param y
	 * @return
	 */
	public static Item getItemAtPosition(int x, int y, Frame f)
	{
		Frame current = f;
		List<Item> allItems = current.getItems();
		
		for(Item i : allItems)
		{
			if(i.getX() == x && i.getY() == y)
				return i;
		}
		
		return null;
	}
	
	/**
	 * Returns an item containing a specified piece of data.
	 * kgas1 - 7/06/2012
	 * @param s
	 * @param f
	 * @return
	 */
	public static Item getItemContainingData(String s, Frame f){
		
		Frame current = f;
		
		List<Item> allItems = current.getItems();
		
		
		for(Item i : allItems){
			
			
			if(i.getData() != null && i.getData().size() > 0){
				if(i.getData().contains(s)){
					return i;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Forces a repaint of the current Frame
	 */
	public static void display() {
		FrameGraphics.refresh(false);
	}
	
	public static String getWindowSize() {
		return Browser.getWindows()[0].getSize().toString();
	}

	/**
	 * Restores the current frame to the last saved version currently on the
	 * hard disk
	 */
	public static void restore() {
		FrameIO.Reload();
		// MessageBay.displayMessage("Restoration complete.");
	}

	/**
	 * Toggles AudienceMode on or off
	 */
	public static void toggleAudienceMode() {
		FrameGraphics.ToggleAudienceMode();
	}

	/**
	 * Toggles TwinFrames mode on or off
	 */
	public static void toggleTwinFramesMode() {
		DisplayIO.ToggleTwinFrames();
	}

	/**
	 * If the given Item is a Text Item, then the text of the Item is
	 * interpreted as actions, if not this method does nothing.
	 * 
	 * @param current
	 *            The Item to read the Actions from
	 */
	public static void runItem(Item current) throws Exception {
		if (current instanceof Text) {
			List<String> actions = ((Text) current).getTextList();
			for (String action : actions) {
				if (!action.equalsIgnoreCase("runitem")) {
					Actions.PerformAction(DisplayIO.getCurrentFrame(), current,
							action);
				}
			}
		} else {
			MessageBay.errorMessage("Item must be a text item.");
		}
	}

	/**
	 * Prompts the user to confirm deletion of the current Frame, and deletes if
	 * the user chooses. After deletion this action calls back(), to ensure the
	 * deleted frame is not still being shown
	 * 
	 */
	public static void DeleteFrame(Frame toDelete) {
		String deletedFrame = toDelete.getName();
		String deletedFrameNameLowercase = deletedFrame.toLowerCase();
		String errorMessage = "Error deleting " + deletedFrame;
		try {
			String deletedFrameName = FrameIO.DeleteFrame(toDelete);
			if (deletedFrameName != null) {
				DisplayIO.Back();
				// Remove any links on the previous frame to the one being
				// deleted
				Frame current = DisplayIO.getCurrentFrame();
				for (Item i : current.getItems())
					if (i.getLink() != null
							&& i.getAbsoluteLink().toLowerCase().equals(
									deletedFrameNameLowercase)) {
						i.setLink(null);
					}
				MessageBay.displayMessage(deletedFrame + " renamed "
						+ deletedFrameName);
				// FrameGraphics.Repaint();
				return;
			}
		} catch (IOException ioe) {
			if (ioe.getMessage() != null)
				errorMessage += ". " + ioe.getMessage();
		} catch (SecurityException se) {
			if (se.getMessage() != null)
				errorMessage += ". " + se.getMessage();
		} catch (Exception e) {
			e.printStackTrace();
		}
		MessageBay.errorMessage(errorMessage);
	}

	/**
	 * Loads the Frame linked to by the given Item. The first Item on the Frame
	 * that is not the title or name is then placed on the cursor. If the given
	 * Item has no link, or no item is found then this is a no-op.
	 * 
	 * @param current
	 *            The Item that links to the Frame that the Item will be loaded
	 *            from.
	 */
	public static Item GetItemFromChildFrame(Item current) {
		return getFromChildFrame(current, false);
	}

	public static void GetItemsFromChildFrame(Item current) {
		getItemsFromChildFrame(current, false);
	}

	/**
	 * Loads the Frame linked to by the given Item. The first Text Item on the
	 * Frame that is not the title or name is then placed on the cursor. If the
	 * given Item has no link, or no item is found then this is a no-op.
	 * 
	 * @param current
	 *            The Item that links to the Frame that the Item will be loaded
	 *            from.
	 */
	public static Item GetTextFromChildFrame(Item current) {
		return getFromChildFrame(current, true);
	}

	private static Item getFromChildFrame(Item current, boolean textOnly) {
		Item item = getFirstBodyItemOnChildFrame(current, textOnly);
		// if no item was found
		if (item != null) {
			// copy the item and switch
			item = item.copy();
			item.setPosition(DisplayIO.getMouseX(), FrameMouseActions.getY());
		}
		return item;
	}

	private static void getItemsFromChildFrame(Item current, boolean textOnly) {
		Collection<Item> items = getItemsOnChildFrame(current, textOnly);
		// if no item was found
		if (items == null || items.size() == 0) {
			return;
		}

		// copy the item and switch
		Collection<Item> copies = ItemUtils.CopyItems(items);
		Item first = items.iterator().next();
		float deltaX = DisplayIO.getMouseX() - first.getX();
		float deltaY = FrameMouseActions.getY() - first.getY();
		for (Item i : copies) {
			if (i.isVisible())
				i.setXY(i.getX() + deltaX, i.getY() + deltaY);
			i.setParent(null);
		}
		FrameMouseActions.pickup(copies);
		FrameGraphics.Repaint();
	}

	/**
	 * Sets the given Item to have the Given Color. Color can be null (for
	 * default)
	 * 
	 * @param toChange
	 *            The Item to set the Color.
	 * @param toUse
	 *            The Color to give the Item.
	 */
	public static void SetItemBackgroundColor(Item toChange, Color toUse) {
		if (toChange == null)
			return;

		toChange.setBackgroundColor(toUse);
		FrameGraphics.Repaint();
	}

	/**
	 * Sets the given Item to have the Given Color. Color can be null (for
	 * default)
	 * 
	 * @param toChange
	 *            The Item to set the Color.
	 * @param toUse
	 *            The Color to give the Item.
	 */
	public static void SetItemColor(Item toChange, Color toUse) {
		if (toChange == null)
			return;

		toChange.setColor(toUse);
		FrameGraphics.Repaint();
	}

	/**
	 * Creates a new Text Object containing general statistics for the current
	 * session. The newly created Text Object is then attached to the cursor via
	 * FrameMouseActions.pickup(Item)
	 */
	public static void GetSessionStats() {
		attachStatsToCursor(SessionStats.getCurrentStats());
	}

	/**
	 * Creates a new Text Object containing statistics for the current tree.
	 */
	public static String GetCometStats(Frame frame) {
		TimeKeeper timer = new TimeKeeper();
		MessageBay.displayMessage("Computing comet stats...");
		CometStats cometStats = new CometStats(frame);
		String result = cometStats.toString();
		MessageBay.overwriteMessage("Comet stats time: "
				+ timer.getElapsedStringSeconds());
		return result;
	}

	public static String GetTreeStats(Frame frame) {
		TimeKeeper timer = new TimeKeeper();
		MessageBay.displayMessage("Computing tree stats...");

		TreeStats treeStats = new TreeStats(frame);
		String result = treeStats.toString();
		MessageBay.overwriteMessage("Tree stats time: "
				+ timer.getElapsedStringSeconds());
		return result;

	}

	public static String GetDocumentStats(Frame frame) {
		TimeKeeper timer = new TimeKeeper();
		MessageBay.displayMessage("Computing document stats...");
		FrameIO.ForceSaveFrame(frame);
		DocumentStatsFast docStats = new DocumentStatsFast(frame.getName(),
				frame.getTitle());
		String result = docStats.toString();

		MessageBay.overwriteMessage("Document stats time: "
				+ timer.getElapsedStringSeconds());
		return result;

	}

	/**
	 * Creates a text item and attaches it to the cursor.
	 * 
	 * @param itemText
	 *            the text to attach to the cursor
	 */
	public static void attachStatsToCursor(String itemText) {
		SessionStats.CreatedText();
		Frame current = DisplayIO.getCurrentFrame();
		Item text = current.getStatsTextItem(itemText);
		FrameMouseActions.pickup(text);
		FrameGraphics.Repaint();
	}

	public static void attachTextToCursor(String itemText) {
		SessionStats.CreatedText();
		Frame current = DisplayIO.getCurrentFrame();
		Item text = current.getTextItem(itemText);
		FrameMouseActions.pickup(text);
		FrameGraphics.Repaint();
	}
	
	/**
	 * Creates a new Text Object containing statistics for moving, deleting and
	 * creating items in the current session. The newly created Text Object is
	 * then attached to the cursor via FrameMouseActions.pickup(Item)
	 */
	public static String getItemStats() {
		return SessionStats.getItemStats();
	}

	/**
	 * Creates a new Text Object containing statistics for the time between
	 * events triggered by the user through mouse clicks and key presses. The
	 * newly created Text Object is then attached to the cursor via
	 * FrameMouseActions.pickup(Item)
	 */
	public static String getEventStats() {
		return SessionStats.getEventStats();
	}

	/**
	 * Creates a new Text Object containing the contents of the current frames
	 * file.
	 */
	public static String getFrameFile(Frame frame) {
		return FrameIO.ForceSaveFrame(frame);
	}

	/**
	 * Creates a new Text Object containing the available fonts.
	 */
	public static String getFontNames() {
		Collection<String> availableFonts = Actions.getFonts().values();
		StringBuilder fontsList = new StringBuilder();
		for (String s : availableFonts) {
			fontsList.append(s).append(Text.LINE_SEPARATOR);
		}
		fontsList.deleteCharAt(fontsList.length() - 1);

		return fontsList.toString();
	}

	public static String getUnicodeCharacters(int start, int finish) {
		if (start < 0 && finish < 0) {
			throw new RuntimeException("Parameters must be non negative");
		}
		// Swap the start and finish if they are inthe wrong order
		if (start > finish) {
			start += finish;
			finish = start - finish;
			start = start - finish;
		}
		StringBuilder charList = new StringBuilder();
		int count = 0;
		charList.append(String.format("Unicode block 0x%x - 0x%x", start,
				finish));
		System.out.println();
		// charList.append("Unicode block: ").append(String.format(format,
		// args))
		for (char i = (char) start; i < (char) finish; i++) {
			if (Character.isDefined(i)) {
				if (count++ % 64 == 0)
					charList.append(Text.LINE_SEPARATOR);
				charList.append(Character.valueOf(i));
			}
		}
		return charList.toString();
	}

	/**
	 * Gets a single block of Unicode characters.
	 * 
	 * @param start
	 *            the start of the block
	 */
	public static String getUnicodeCharacters(int start) {
		return getUnicodeCharacters(start, start + 256);
	}

	public static String getMathSymbols() {
		return getUnicodeCharacters('\u2200', '\u2300');
	}

	/**
	 * Resets the statistics back to zero.
	 */
	public static void repaint() {
		StatsLogger.WriteStatsFile();
		SessionStats.resetStats();
	}

	/**
	 * Loads a frame with the given name and saves it as a JPEG image.
	 * 
	 * @param framename
	 *            The name of the Frame to save
	 */
	public static void jpegFrame(String framename) {
		ImageFrame(framename, "JPEG");
	}

	/**
	 * Saves the current frame as a JPEG image. This is the same as calling
	 * JpegFrame(currentFrame.getName())
	 */
	public static void jpegFrame() {
		ImageFrame(DisplayIO.getCurrentFrame().getName(), "JPEG");
	}

	public static void jpgFrame() {
		jpegFrame();
	}

	/**
	 * Loads a frame with the given name and saves it as a PNG image.
	 * 
	 * @param framename
	 *            The name of the Frame to save
	 */
	public static void PNGFrame(String framename) {
		ImageFrame(framename, "PNG");
	}

	/**
	 * Saves the current frame as a PNG image. This is the same as calling
	 * PNGFrame(currentFrame.getName())
	 */
	public static void PNGFrame(Frame frame) {
		ImageFrame(frame.getName(), "PNG");
	}

	public static String SaveImage(BufferedImage screen, String format,
			String directory, String fileName) {
		String suffix = "." + format.toLowerCase();
		String shortFileName = fileName;
		// Check if we need to append the suffix
		if (fileName.indexOf('.') < 0)
			fileName += suffix;
		else
			shortFileName = fileName.substring(0, fileName.length() - suffix.length());

		try {
			int count = 2;
			// set up the file for output
			File out = new File(directory + fileName);
			while (out.exists()) {
				fileName = shortFileName + "_" + count++ + suffix;
				out = new File(directory + fileName);
			}

			if (!out.getParentFile().exists())
				out.mkdirs();

			// If the image is successfully written out return the fileName
			if (ImageIO.write(screen, format, out))
				return fileName;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String ImageFrame(Frame frame, String format, String directory) {
		assert (frame != null);

		Image oldBuffer = frame.getBuffer();
		frame.setBuffer(null);
		// Jpeg only works properly with volitile frames
		// Png transparency only works with bufferedImage form
		Image frameBuffer = FrameGraphics.getBuffer(frame, false, format
				.equalsIgnoreCase("jpeg"));
		// Make sure overlay stuff doesnt disapear on the frame visible on the
		// screen
		frame.setBuffer(oldBuffer);
		BufferedImage screen = null;

		if (frameBuffer instanceof VolatileImage) {
			// If its the current frame it will be a volitive image
			screen = ((VolatileImage) frameBuffer).getSnapshot();
		} else {
			assert (frameBuffer instanceof BufferedImage);
			screen = (BufferedImage) frameBuffer;
		}
		return SaveImage(screen, format, directory, frame.getExportFileName());
	}

	/**
	 * Saves the Frame with the given Framename as an image of the given format.
	 * 
	 * @param framename
	 *            The name of the Frame to save as an image
	 * @param format
	 *            The Image format to use (i.e. "PNG", "BMP", etc)
	 */
	public static void ImageFrame(String framename, String format) {
		Frame loaded = FrameIO.LoadFrame(framename);

		// if the frame was loaded successfully
		if (loaded != null) {
			String path = FrameIO.EXPORTS_DIR;
			String frameName = ImageFrame(loaded, format, path);
			if (frameName != null)
				MessageBay.displayMessage("Frame successfully saved to " + path
						+ frameName);
			else
				MessageBay.errorMessage("Could not find image writer for "
						+ format + " format");
			// if the frame was not loaded successfully, alert the user
		} else {
			MessageBay.displayMessage("Frame '" + framename
					+ "' could not be found.");
		}
	}

	public static void MessageLn(Item message) {
		if (message instanceof Text)
			MessageBay.displayMessage((Text) message);
	}

	/**
	 * Displays a message in the message box area.
	 * 
	 * @param message
	 *            the message to display
	 */
	public static void MessageLn(String message) {
		MessageBay.displayMessage(message);
	}

	public static void MessageLn2(String message, String message2) {
		MessageBay.displayMessage(message + " " + message2);
	}

	public static void CopyFile(String existingFile, String newFileName) {
		try {
			// TODO is there a built in method which will do this faster?

			MessageBay.displayMessage("Copying file " + existingFile + " to "
					+ newFileName + "...");
			FrameIO.copyFile(existingFile, newFileName);
			MessageBay.displayMessage("File copied successfully");
		} catch (FileNotFoundException e) {
			MessageBay.displayMessage("Error opening file: " + existingFile);
		} catch (Exception e) {
			MessageBay.displayMessage("File could not be copied");
		}
	}

	/**
	 * Runs two methods alternatively a specified number of times and reports on
	 * the time spent running each method.
	 * 
	 * @param fullMethodNameA
	 * @param fullMethodNameB
	 * @param repsPerTest
	 *            the number of time each method is run per test
	 * @param tests
	 *            the number of tests to conduct
	 * 
	 */
	public static void CompareMethods(String fullMethodNameA,
			String fullMethodNameB, int repsPerTest, int tests) {
		try {
			String classNameA = getClassName(fullMethodNameA);
			String classNameB = getClassName(fullMethodNameB);
			String methodNameA = getMethodName(fullMethodNameA);
			String methodNameB = getMethodName(fullMethodNameB);

			Class<?> classA = Class.forName(classNameA);
			Class<?> classB = Class.forName(classNameB);
			Method methodA = classA.getDeclaredMethod(methodNameA,
					new Class[] {});
			Method methodB = classB.getDeclaredMethod(methodNameB,
					new Class[] {});
			TimeKeeper timeKeeper = new TimeKeeper();
			long timeA = 0;
			long timeB = 0;
			// Run the tests
			for (int i = 0; i < tests; i++) {
				// Test methodA
				timeKeeper.restart();
				for (int j = 0; j < repsPerTest; j++) {
					methodA.invoke((Object) null, new Object[] {});
				}
				timeA += timeKeeper.getElapsedMillis();
				timeKeeper.restart();
				// Test methodB
				for (int j = 0; j < repsPerTest; j++) {
					methodB.invoke((Object) null, new Object[] {});
				}
				timeB += timeKeeper.getElapsedMillis();
			}

			float aveTimeA = timeA * 1000F / repsPerTest / tests;
			float aveTimeB = timeB * 1000F / repsPerTest / tests;
			// Display Results
			MessageBay.displayMessage("Average Execution Time");
			MessageBay.displayMessage(methodNameA + ": "
					+ TimeKeeper.Formatter.format(aveTimeA) + "us");
			MessageBay.displayMessage(methodNameB + ": "
					+ TimeKeeper.Formatter.format(aveTimeB) + "us");
		} catch (Exception e) {
			MessageBay.errorMessage(e.getClass().getSimpleName() + ": "
					+ e.getMessage());
		}
	}

	public static String getClassName(String fullMethodName) {
		assert (fullMethodName != null);
		assert (fullMethodName.length() > 0);
		int lastPeriod = fullMethodName.lastIndexOf('.');
		if (lastPeriod > 0 && lastPeriod < fullMethodName.length() - 1)
			return fullMethodName.substring(0, lastPeriod);
		throw new RuntimeException("Invalid method name: " + fullMethodName);
	}

	public static String getMethodName(String methodName) {
		assert (methodName != null);
		assert (methodName.length() > 0);
		int lastPeriod = methodName.lastIndexOf('.');
		if (lastPeriod > 0 && lastPeriod < methodName.length() - 1)
			return methodName.substring(1 + lastPeriod);
		throw new RuntimeException("Invalid method name: " + methodName);
	}

	/**
	 * Loads the Frame linked to by the given Item. The first Item on the Frame
	 * that is not the title or name is then placed on the current frame. The
	 * item that was clicked on is placed on the frame it was linked to and the
	 * link is switched to the item from the child frame. If the given Item has
	 * no link, or no item is found then this is a no-op.
	 * 
	 * @param current
	 *            The Item that links to the Frame that the Item will be loaded
	 *            from.
	 */
	public static void SwapItemWithItemOnChildFrame(Item current) {
		Item item = getFirstBodyItemOnChildFrame(current, false);
		// if no item was found
		if (item == null) {
			return;
		}

		// swap the items parents
		Frame parentFrame = current.getParent();
		Frame childFrame = item.getParent();
		current.setParent(childFrame);
		item.setParent(parentFrame);

		// swap the items on the frames
		parentFrame.removeItem(current);
		childFrame.removeItem(item);
		parentFrame.addItem(item);
		childFrame.addItem(current);

		// swap the items links
		item.setActions(current.getAction());
		item.setLink(childFrame.getName());
		current.setLink(parentFrame.getName());
		// current.setLink(null);
		current.setActions(null);

		FrameGraphics.Repaint();
	}

	private static Item getFirstBodyItemOnChildFrame(Item current,
			boolean textOnly) {
		// the item must link to a frame
		if (current.getLink() == null) {
			MessageBay
					.displayMessage("Cannot get item from child - this item has no link");
			return null;
		}

		Frame child = FrameIO.LoadFrame(current.getAbsoluteLink());

		// if the frame could not be loaded
		if (child == null) {
			MessageBay.errorMessage("Could not load child frame.");
			return null;
		}

		// find the first non-title and non-name item
		List<Item> body = new ArrayList<Item>();
		if (textOnly)
			body.addAll(child.getBodyTextItems(false));
		else
			body.addAll(child.getItems());
		Item item = null;

		for (Item i : body)
			if (i != child.getTitleItem() && !i.isAnnotation()) {
				item = i;
				break;
			}

		// if no item was found
		if (item == null) {
			MessageBay.displayMessage("No item found to copy");
			return null;
		}

		return item;
	}

	private static Collection<Item> getItemsOnChildFrame(Item current,
			boolean textOnly) {
		// the item must link to a frame
		if (current.getLink() == null) {
			MessageBay
					.displayMessage("Cannot get item from child - this item has no link");
			return null;
		}
		Frame child = FrameIO.LoadFrame(current.getAbsoluteLink());

		// if the frame could not be loaded
		if (child == null) {
			MessageBay.errorMessage("Could not load child frame.");
			return null;
		}

		// find the first non-title and non-name item
		Collection<Item> body = new ArrayList<Item>();
		if (textOnly)
			body.addAll(child.getBodyTextItems(false));
		else
			body.addAll(child.getItems());

		return body;
	}

	public static void calculate(Frame frame, Item toCalculate) {
		if (toCalculate instanceof Text) {
			Text text = (Text) toCalculate;
			ExpediteeJEP myParser = new ExpediteeJEP();
			myParser.addVariables(frame);
			String linkedFrame = toCalculate.getAbsoluteLink();
			if (linkedFrame != null) {
				myParser.addVariables(FrameIO.LoadFrame(linkedFrame));
			}
			myParser.resetObserver();

			// Do the calculation
			String formulaFullCase = text.getText().replace('\n', ' ');
			String formula = formulaFullCase.toLowerCase();

			try {
				Node node = myParser.parse(formula);
				Object result = myParser.evaluate(node);
				text.setText(result.toString(), true);
				text.setFormula(formulaFullCase);
				if (text.isFloating()) {
					text.setPosition(FrameMouseActions.MouseX,
							FrameMouseActions.MouseY);
					FrameMouseActions.resetOffset();
				} else {
					text.getParentOrCurrentFrame().change();
				}
			} catch (ParseException e) {
				MessageBay.errorMessage("Parse error "
						+ e.getMessage().replace("\n", ""));
			} catch (Exception e) {
				MessageBay.errorMessage("evaluation error "
						+ e.getMessage().replace("\n", ""));
				e.printStackTrace();
			}
		}
	}

	/**
	 * Attach an item to the cursor.
	 * 
	 * @param item
	 */
	public static void attachToCursor(Item item) {
		item.setParent(null);
		FrameMouseActions.pickup(item);
		FrameGraphics.Repaint();
	}

	public static void attachToCursor(Collection<Item> items) {
		for (Item i : items) {
			i.setParent(null);
			i.invalidateAll();
		}
		FrameMouseActions.pickup(items);
		// TODO figure out why this isnt repainting stuff immediately
		// All of text item doesnt repaint until the cursor is moved
		FrameGraphics.requestRefresh(true);
	}

	public static void importFiles(Item item) {
		List<File> files = new LinkedList<File>();
		for (String s : item.getText().split("\\s+")) {
			File file = new File(s.trim());
			if (file.exists()) {
				files.add(file);
			}
		}
		try {
			FrameDNDTransferHandler.getInstance().importFileList(files,
					FrameMouseActions.getPosition());
		} catch (Exception e) {
		}
	}

	public static void importFile(Item item) {
		File file = new File(item.getText().trim());
		if (file.exists()) {
			try {
				FrameDNDTransferHandler.getInstance().importFile(file,
						FrameMouseActions.getPosition());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static Item createPolygon(Item item, int sides) {
		if (item instanceof Text) {
			try {
				SString s = new SString(item.getText());
				sides = s.integerValue().intValue();
			} catch (NumberFormatException e) {
			}
		}

		if (sides < 3) {
			MessageBay.errorMessage("Shapes must have at least 3 sides");
		}
		double angle = -(180 - ((sides - 2) * 180.0F) / sides);
		double curAngle = 0;
		double size = 50F;
		if (item.isLineEnd() && item.getLines().size() > 0) {
			item = item.getLines().get(0);
		}
		// Use line length to determine the size of the shape
		if (item instanceof Line) {
			size = ((Line) item).getLength();
		}

		float curX = FrameMouseActions.MouseX;
		float curY = FrameMouseActions.MouseY;

		Collection<Item> newItems = new LinkedList<Item>();
		Item[] d = new Item[sides];
		// create dots
		Frame current = DisplayIO.getCurrentFrame();
		for (int i = 0; i < d.length; i++) {
			d[i] = current.createDot();
			newItems.add(d[i]);
			d[i].setPosition(curX, curY);
			curX += (float) (Math.cos((curAngle) * Math.PI / 180.0) * size);
			curY += (float) (Math.sin((curAngle) * Math.PI / 180.0) * size);

			curAngle += angle;
		}
		// create lines
		for (int i = 1; i < d.length; i++) {
			newItems.add(new Line(d[i - 1], d[i], current.getNextItemID()));
		}
		newItems.add(new Line(d[d.length - 1], d[0], current.getNextItemID()));

		current.addAllItems(newItems);
		if (item instanceof Text) {
			for (Item i : item.getAllConnected()) {
				if (i instanceof Line) {
					item = i;
					break;
				}
			}
		}

		Color newColor = item.getColor();
		if (newColor != null) {
			d[0].setColor(item.getColor());
			if (item instanceof Text && item.getBackgroundColor() != null) {
				d[0].setFillColor(item.getBackgroundColor());
			} else {
				d[0].setFillColor(item.getFillColor());
			}
		}
		float newThickness = item.getThickness();
		if (newThickness > 0) {
			d[0].setThickness(newThickness);
		}

		ItemUtils.EnclosedCheck(newItems);
		FrameGraphics.refresh(false);

		return d[0];
	}

	public static void StopReminder() {
		Reminders.stop();
	}

	public static void print(String file) {
		try {
			if (Browser._theBrowser.isMinimumVersion6()) {
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().print(new File(file));
				}
			}
		} catch (Exception e) {
			MessageBay.errorMessage("Printing error: " + e.getMessage());
		}
	}

	public static int wordCount(String paragraph) {
		return paragraph.trim().split("\\s+").length + 1;
	}

	public static int wordCount(Frame frame) {
		int count = 0;

		for (Text t : frame.getBodyTextItems(false)) {
			count += wordCount(t.getText());
		}

		return count;
	}

	public static void moveToPublic(Frame frame) {
		FrameIO.moveFrameset(frame.getFramesetName(), FrameIO.PUBLIC_PATH);
	}

	public static void moveToPrivate(Frame frame) {
		FrameIO.moveFrameset(frame.getFramesetName(), FrameIO.FRAME_PATH);
	}

	/**
	 * Returns the value of a specified item attribute.
	 * 
	 * @param item
	 *            from which to extract the value
	 * @param attribute
	 *            name of an items attribute
	 * @return the value of the attribute
	 */
	public static String extract(Item item, String attribute) {
		return AttributeUtils.getAttribute(item, attribute);
	}
	
	/**
	 * Launches items.widgets.Browser and uses Text item as URL.
	 * @param text Text item which passes contents as URL for browser.
	 * @throws Exception
	 */
	public static void startLoboBrowser(Item text) throws Exception {
		if (!(text instanceof Text)) {
			MessageBay.errorMessage("Must be a text item.");
			return;
		}
		if(text.getLink() != null) {
			MessageBay.errorMessage("Text item cannot have link.");
			return;
		}
		
		FreeItems.getInstance().clear();								// remove url text from cursor
			
		Text wt = new Text("@iw:org.expeditee.items.widgets.Browser");	// create new text item for browser widget
		wt.setParent(DisplayIO.getCurrentFrame());						// set parent of text source for InteractiveWidget.createWidget()
		wt.setXY(FrameMouseActions.getX(), FrameMouseActions.getY());
		// create widget from text item
		org.expeditee.items.widgets.Browser browser = (org.expeditee.items.widgets.Browser) InteractiveWidget.createWidget(wt);
			
		if(FreeItems.textOnlyAttachedToCursor()) {						// navigates to url specified by the text item
			browser.navigate(text.getText());
		} else {
			browser.navigate("http://www.waikato.ac.nz");
		}
			
		FrameMouseActions.pickup(browser.getItems());					// attach browser widget to mouse
	}
	
	/**
	 * Text item becomes link to new frame containing items.widgets.Browser and uses Text item as URL for browser.
	 * @param text Text item which passes contents as URL for browser and becomes link to the browser's new frame.
	 * @throws Exception
	 */
	public static void startLoboBrowserNewFrame(Item text) throws Exception {
		if (!(text instanceof Text)) {
			MessageBay.errorMessage("Must be a text item.");
			return;
		}
		if(text.getLink() != null) {											// text item can't already have a link
			MessageBay.errorMessage("Text item already has link.");
			return;
		}
		
		// Create new frame and text item for browser widget and parse created frame; loads browser widget
		Frame frame = FrameIO.CreateNewFrame(text);
		frame.addText(0, 50, "@iw:org.expeditee.items.widgets.Browser", null);
		FrameUtils.Parse(frame);
		
		for(InteractiveWidget iw : frame.getInteractiveWidgets()) {	// may be other widgets on frame
			if(iw instanceof org.expeditee.items.widgets.Browser) {
				// Set browser to 'full screen'
				iw.setSize(-1, -1, -1, -1, Browser._theBrowser.getWidth(), Browser._theBrowser.getHeight() 
						- MessageBay.MESSAGE_BUFFER_HEIGHT - 80);
				
				// If there is a text item attached to cursor use it as url for browser
				if(FreeItems.textOnlyAttachedToCursor()) {
					text.setLink("" + frame.getNumber());
					((org.expeditee.items.widgets.Browser)iw).navigate(text.getText());
				} else {
					// Navigate to www.waikato.ac.nz by default if no url supplied and create new text item to be the link
					((org.expeditee.items.widgets.Browser)iw).navigate("http://www.waikato.ac.nz");
					Text t = new Text("http://www.waikato.ac.nz");
					t.setParent(DisplayIO.getCurrentFrame());		// set parent of text source for InteractiveWidget.createWidget()
					t.setXY(FrameMouseActions.getX(), FrameMouseActions.getY());
					t.setLink("" + frame.getNumber());				// link url text to new browser frame
					FrameMouseActions.pickup(t);					// Attach new text link to cursor
				}
			}
		}
		
		FrameIO.SaveFrame(frame);									// save frame to disk
	}
	
	private static boolean startWidget(String name) throws Exception {
		String fullName = Actions.getClassName(name);
		if(fullName == null) {
			return false;
		}
		MessageBay.displayMessage("Creating new \"" + fullName + "\"");
		
		FreeItems.getInstance().clear();
		Text wt = new Text("@iw:" + fullName);							// create new text item for browser widget
		wt.setParent(DisplayIO.getCurrentFrame());						// set parent of text source for InteractiveWidget.createWidget()
		wt.setXY(FrameMouseActions.getX(), FrameMouseActions.getY());	// move to the mouse cursor
		InteractiveWidget widget = InteractiveWidget.createWidget(wt);
		FrameMouseActions.pickup(widget.getItems());
		
		return true;
	}
	
	private static void runUnknown(String command) throws Exception {
		if(startWidget(command)) {
			return;
		}
		
		Actions.PerformAction(DisplayIO.getCurrentFrame(), null, command);
	}
	
	public static void run(String command) throws Exception {
		if(command == null) {
			MessageBay.warningMessage("Please provide a command to run");
			return;
		}
		int firstSpace = command.indexOf(" ");
		if(firstSpace == -1) {
			runUnknown(command);
			return;
		}
		String argLower = command.toLowerCase();
		String name = argLower.substring(0, firstSpace).trim();	// first word
		String args = argLower.substring(firstSpace).trim();	// remainder after first word
		if(name == "action" || name == "agent") {
			if(args.length() > 0) {
				Actions.PerformAction(DisplayIO.getCurrentFrame(), null, args);
			} else {
				MessageBay.displayMessage("Please specify an action/agent name");
			}
		} else if(name == "widget") {
			if(args.length() > 0) {
				if(!startWidget(args)) {
					MessageBay.displayMessage("Widget \"" + name + "\" does not exist");
				}
			} else {
				MessageBay.displayMessage("Please specify a widget name");
			}
		} else {
			runUnknown(command);
		}
	}
	
	public static void run(Item item) throws Exception {
		if(item == null) {
			MessageBay.warningMessage("Please provide a command to run");
			return;
		}
		run(((Text)item).getText());
	}
	
	/**
	 * Rebuilds the home frame restoring its original presentation.
	 * Basically removes all items on the frame and reruns FrameUtils.CreateDefaultProfile().
	 */
	public static void resetHomeFrame() {
		Frame homeFrame = FrameIO.LoadFrame(UserSettings.HomeFrame.get());
		homeFrame.removeAllItems(homeFrame.getItems());
		homeFrame.addText(0, 0, "title", null);
		FrameUtils.CreateDefaultProfile(UserSettings.UserName.get(), homeFrame);
	}
	
	/**
	 * Loads and runs an executable jar file in a new Thread
	 * @param jar path to the jar file to run
	 */
	public static void runJar(String jar) throws Exception {
		File jf = new File(jar);
		if(!jf.exists()) {
			System.err.println("jar '" + jar + "' could not be found");
			return;
		}
		JarFile jarFile = new JarFile(jf);
		
		String mainClassName = (String) jarFile.getManifest().getMainAttributes().get(new Attributes.Name("Main-Class"));
		if(mainClassName == null) {
        	System.err.println("jar '" + jar + "' does not have a Main-Class entry");
        	jarFile.close();
			return;
        }
		jarFile.close();
		System.out.println("Main-Class = " + mainClassName);
		
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();

		Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		addURL.setAccessible(true);
		addURL.invoke(classLoader, jf.toURI().toURL());

		final Class<?> jarClass = classLoader.loadClass(mainClassName);
		
		final Method main = jarClass.getDeclaredMethod("main", String[].class);
		
		new Thread(new Runnable() {
			public void run() {
				try {
	                main.invoke(jarClass, new Object[] {new String[0]});
                } catch (Exception e) {
                	System.out.println("Failed to start jar");
	                e.printStackTrace();
                }
			}
		}).start();
	}
	
	public static void pan(Frame frame, int x, int y) {
		for (Item i : frame.getAllItems()) {
			if (i instanceof WidgetEdge || i instanceof WidgetCorner) {
				continue;
			} 
			else {
				int new_x = i.getX();
				int new_y = i.getY();

				if (!i.isAnchoredX()) {
					new_x += x;
				}

				if (!i.isAnchoredY()) {
					new_y += y;
				}

				if(i instanceof XRayable) {
					i.setPosition(new_x,new_y);
				}
				else {
					i.setXY(new_x,new_y);
				}
			}
			// update the polygon, otherwise stuff moves but leaves it's outline behind
			i.updatePolygon();
		}

		for (InteractiveWidget iw : frame.getInteractiveWidgets()) {

			int new_x = iw.getX();
			int new_y = iw.getY();

			if (!iw.isAnchoredX()) {
				new_x += x;
			}

			if (!iw.isAnchoredY()) {
				new_y += y;
			}

			iw.setPosition(new_x,new_y);
	
		}

		// make sure we save the panning of the frame
		frame.change();
		// redraw everything
		FrameKeyboardActions.Refresh();
	}
	
	public static void pan(Frame frame, String pan) {
		String[] split = pan.split("\\s+");
		int x = 0;
		int y = 0;
		try {
			if(split.length != 2) throw new Exception();
			x = Integer.parseInt(split[0]);
			y = Integer.parseInt(split[1]);
		} catch(Exception e) {
			MessageBay.errorMessage("Panning takes 2 integer arguments");
			return;
		}
		pan(frame, x, y);
	}
	
	public static void pan(Frame frame, Text pan) {
		pan(frame, pan.getText());
	}
	
	public static String exec(String cmd) throws Exception {
		
		String[] command;
		
		// run command through sh if possible
		if(System.getProperty("os.name").toLowerCase().indexOf("win") == -1) {
			command = new String[] { "sh", "-c", cmd };
		} else {
			command = cmd.split("\\s+");
		}
		
		ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process ps = pb.start();
    
        BufferedReader in = new BufferedReader(new InputStreamReader(ps.getInputStream()));
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line).append('\n');
        }
        ps.waitFor();
        in.close();
        
        if(sb.length() > 0) {
        	sb.deleteCharAt(sb.length() - 1);
        }
		return sb.toString();
	}
	
	public static void testProgress() {
		new Thread(new Runnable() {

			@Override
            public void run() {
				Progress p = MessageBay.displayProgress("Loading something");
				for(int i = 1; i <= 100; i++) {
    				try {
    	                Thread.sleep(100);
    	                p.set(i);
                    } catch (Exception e) {
    	                e.printStackTrace();
                    }
				}
            }
			
		}).start();
	}
	
	public static void getIDs(Frame f) {
		for(Item i : f.getAllItems()) {
			System.out.println(i + " (" + i.getID() + ")");
		}
	}
	
	public static void flushResources() {
		FrameUtils.extractResources(true);
		MessageBay.displayMessage("Re-extracted resources, Expeditee may need to be restarted for certain resources to be reloaded");
	}
}
