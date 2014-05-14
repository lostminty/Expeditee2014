package org.expeditee.gui;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.expeditee.items.Circle;
import org.expeditee.items.Dot;
import org.expeditee.items.DotType;
import org.expeditee.items.FrameBitmap;
import org.expeditee.items.FrameImage;
import org.expeditee.items.Item;
import org.expeditee.items.Item.HighlightMode;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.JSItem;
import org.expeditee.items.Line;
import org.expeditee.items.PermissionPair;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.items.UserAppliedPermission;
import org.expeditee.items.XRayable;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.InteractiveWidgetInitialisationFailedException;
import org.expeditee.items.widgets.InteractiveWidgetNotAvailableException;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;
import org.expeditee.settings.Settings;
import org.expeditee.settings.UserSettings;
import org.expeditee.settings.templates.TemplateSettings;
import org.expeditee.stats.Logger;
import org.expeditee.stats.SessionStats;

public class FrameUtils {

    private static final int COLUMN_WIDTH = 50;

    /**
     * Provides a way to monitor the time elapsed between button-down and the
     * finished painting.
     */
    public static TimeKeeper ResponseTimer = new TimeKeeper();

    private static float _ResponseTimeSum = 0;

    private static float _LastResponse = 0;

    private static Text LastEdited = null;

    public static int MINIMUM_INTERITEM_SPACING = -6;

    public static float getResponseTimeTotal() {
	return _ResponseTimeSum;
    }

    public static float getLastResponseTime() {
	return _LastResponse;
    }

    /**
     * The list of known start pages framesets which will have prepopulated
     * links in the home frame.
     */
    public static final String[] startPages = { "exploratorysearch",
	    "webbrowser" };

    /**
     * Checks if the given top Item is above the given bottom Item, allowing for
     * the X coordinates to be off by a certain width...
     * 
     * @param item1
     *            The Item to check is above the other Item
     * @param item2
     *            The Item to check is below the top Item
     * @return True if top is above bottom, False otherwise.
     */
    public static boolean inSameColumn(Item item1, Item item2) {
	if (!(item1 instanceof Text) || !(item2 instanceof Text))
	    return false;

	if (item1.getID() < 0 || item2.getID() < 0)
	    return false;

	int minX = item2.getX();
	int maxX = item2.getX() + item2.getBoundsWidth();

	int startX = item1.getX();
	int endX = item1.getX() + item1.getBoundsWidth();

	// Check that the two items left values are close
	if (Math.abs(item1.getX() - item2.getX()) > COLUMN_WIDTH)
	    return false;

	// Ensure the two items
	if ((minX >= startX && minX <= endX)
		|| (maxX >= startX && maxX <= endX)
		|| (startX >= minX && startX <= maxX)
		|| (endX >= minX && endX <= maxX))
	    return true;

	return false;
    }

    public static boolean sameBulletType(String bullet1, String bullet2) {
	if (bullet1 == null || bullet2 == null)
	    return false;

	if (bullet1.equals("") || bullet2.equals(""))
	    return false;

	if (Character.isLetter(bullet1.charAt(0))
		&& Character.isLetter(bullet2.charAt(0)))
	    return true;

	if (Character.isDigit(bullet1.charAt(0))
		&& Character.isDigit(bullet2.charAt(0)))
	    return true;

	// TODO make this more sofisticated

	return false;
    }

    private static boolean needsRenumbering(String s) {
	if (s == null || s.equals(""))
	    return false;
	if (!Character.isLetterOrDigit(s.charAt(0)))
	    return false;

	s = s.trim();
	// if its all letters then we dont want to auto adjust
	if (s.length() > 2) {
	    for (int i = 0; i < s.length() - 1; i++) {
		if (!Character.isLetter(s.charAt(i)))
		    return true;
	    }
	} else
	    return true;

	return false;
    }

    /**
     * 
     * @param toAlign
     * @param moveAll
     * @param adjust
     * @return
     */
    public static int Align(List<Text> toAlign, boolean moveAll, int adjust,
	    List<Item> changedItems) {
	Collections.sort(toAlign);

	/*
	 * Single items dont need alignment But if there are two items we may
	 * still want to format them... ie if they are too close together.
	 */
	if (toAlign.size() < 1)
	    return 0;

	// get the first item
	Text from = toAlign.get(0);
	if (from.getParent() == null)
	    from = toAlign.get(1);
	int x = from.getX();

	Frame curr = from.getParent();
	Text above = curr.getTextAbove(from);

	String lastBullet = "";

	if (above != null && curr.isNormalTextItem(above))
	    lastBullet = FrameKeyboardActions.getAutoBullet(above.getText());
	else {
	    lastBullet = FrameKeyboardActions.getBullet(toAlign.get(0)
		    .getText());
	}
	if (needsRenumbering(lastBullet)) {
	    // renumber...
	    for (int i = 0; i < toAlign.size(); i++) {

		Text currentText = toAlign.get(i);
		String currentBullet = FrameKeyboardActions
			.getAutoBullet(currentText.getText());

		if (sameBulletType(lastBullet, currentBullet)) {
		    String oldText = currentText.getText();

		    currentText.stripFirstWord();

		    currentText.setText(lastBullet + currentText.getText());
		    lastBullet = FrameKeyboardActions.getAutoBullet(currentText
			    .getText());

		    // if we changed the item, add to changedItems list
		    if (changedItems != null
			    && oldText != currentText.getText()
			    && !changedItems.contains(currentText)) {
			Item copy = currentText.copy();
			copy.setID(currentText.getID());
			copy.setText(oldText);
			changedItems.add(copy);
		    }
		}
	    }
	}

	// work out the spacing between the first item and the one above it

	int space = 10 + adjust;

	// if we are dropping from the title make the space a little bigger
	// than normal

	// If there are only two items get the gap from the start item on the
	// zero frame if there is one
	if (above == curr.getTitleItem()) {
	    Frame zero = FrameIO.LoadFrame(curr.getFramesetName() + '0');
	    String strGap = zero.getAnnotationValue("start");
	    if (strGap != null) {
		try {
		    int gap = Integer.parseInt(strGap);
		    space = gap;
		} catch (NumberFormatException nfe) {

		}
	    }
	} else if (above != null) {
	    // Make the gap between all items the same as the gap between
	    // the first two
	    space = (int) (from.getPolygon().getBounds().getMinY() - above
		    .getPolygon().getBounds().getMaxY());

	    if (space < MINIMUM_INTERITEM_SPACING)
		space = MINIMUM_INTERITEM_SPACING;

	    if (UserSettings.FormatSpacingMax.get() != null) {
		double maxSpace = UserSettings.FormatSpacingMax.get()
			* above.getSize();
		if (maxSpace < space) {
		    space = (int) Math.round(maxSpace);
		}
	    }

	    if (UserSettings.FormatSpacingMin.get() != null) {
		double minSpace = UserSettings.FormatSpacingMin.get()
			* above.getSize();
		if (minSpace > space) {
		    space = (int) Math.round(minSpace);
		}
	    }

	    // Need to do things differently for FORMAT than for DROPPING
	    if (moveAll && above != curr.getNameItem()
		    && above != curr.getTitleItem()) {
		x = above.getX();
		int y = (int) above.getPolygon().getBounds().getMaxY()
			+ space
			+ ((int) (from.getY() - from.getPolygon().getBounds()
				.getMinY()));

		if (changedItems != null
			&& (from.getX() != x || from.getY() != y)
			&& !changedItems.contains(from)) {
		    Item copy = from.copy();
		    copy.setID(from.getID());
		    changedItems.add(copy);
		}
		from.setPosition(x, y);
	    } else {
		x = from.getX();
	    }

	    space += adjust;
	}
	for (int i = 1; i < toAlign.size(); i++) {
	    Item current = toAlign.get(i);
	    Item top = toAlign.get(i - 1);

	    // The bottom of the previous item
	    int bottom = (int) top.getPolygon().getBounds().getMaxY();

	    // the difference between the current item's Y coordinate and
	    // the top of the highlight box
	    int diff = (int) (current.getY() - current.getPolygon().getBounds()
		    .getMinY());

	    int newPos = bottom + space + diff;

	    if (changedItems != null
		    && ((moveAll && current.getX() != x) || current.getY() != newPos)
		    && !changedItems.contains(current)) {
		Item copy = current.copy();
		copy.setID(current.getID());
		changedItems.add(copy);
	    }

	    if (moveAll) {
		current.setPosition(x, newPos);
	    } else if (newPos > current.getY()) {
		current.setY(newPos);
	    }

	}

	// if (insert != null)
	// return insert.getY();

	// Michael thinks we return the y value for the next new item??
	int y = from.getY() + from.getBoundsHeight() + space;
	return y;
    }

    public static int Align(List<Text> toAlign, boolean moveAll, int adjust) {
	return Align(toAlign, moveAll, adjust, null);
    }

    public static boolean LeavingFrame(Frame current) {
	checkTDFCItemWaiting(current);
	// active overlay frames may also require saving if they have been
	// changed
	for (Overlay o : current.getOverlays())
	    if (!SaveCheck(o.Frame))
		return false;

	// if the check fails there is no point continuing
	if (!SaveCheck(current))
	    return false;

	for (Item i : current.getItems())
	    i.setHighlightMode(Item.HighlightMode.None);
	return true;
    }

    private static boolean SaveCheck(Frame toSave) {
	// don't bother saving frames that haven't changed
	if (!toSave.hasChanged())
	    return true;

	// if the frame has been changed, then save it
	if (DisplayIO.isTwinFramesOn()) {
	    Frame opposite = DisplayIO.getOppositeFrame();

	    String side = "left";
	    if (DisplayIO.getCurrentSide() == 0)
		side = "right";

	    // if the two frames both have changes, prompt the user for the
	    // next move
	    if (opposite.hasChanged() && opposite.equals(toSave)) {
		if (DisplayIO.DisplayConfirmDialog(
			"Leaving this frame will discard changes made in the "
				+ side + " Frame. Continue?", "Changes",
			DisplayIO.TYPE_WARNING, DisplayIO.OPTIONS_OK_CANCEL,
			DisplayIO.RESULT_OK)) {
		    FrameIO.SaveFrame(toSave);
		    DisplayIO.Reload(DisplayIO.FrameOnSide(opposite));
		    return true;
		} else
		    return false;
	    } else if (opposite.hasOverlay(toSave)) {
		if (toSave.hasChanged())
		    if (DisplayIO.DisplayConfirmDialog(
			    "Leaving this frame will discard changes made in the "
				    + side + " Frame. Continue?", "Changes",
			    DisplayIO.TYPE_WARNING,
			    DisplayIO.OPTIONS_OK_CANCEL, DisplayIO.RESULT_OK)) {
			FrameIO.SaveFrame(toSave);
			DisplayIO.Reload(DisplayIO.FrameOnSide(opposite));
			return true;
		    } else
			return false;
	    }

	    // save the current frame and restore the other side
	    FrameIO.SaveFrame(toSave);
	    return true;
	}

	// single-frame mode can just save and return
	FrameIO.SaveFrame(toSave);
	return true;
    }

    /**
     * Displays the given Frame on the display. If the current frame has changed
     * since the last save then it will be saved before the switch is made. The
     * caller can also dictate whether the current frame is added to the
     * back-stack or not.
     * 
     * @param toDisplay
     *            The Frame to display on the screen
     * @param addToBack
     *            True if the current Frame should be added to the back-stack,
     *            False otherwise
     */
    public static void DisplayFrame(Frame toDisplay, boolean addToBack,
	    boolean incrementStats) {
	if (toDisplay == null)
	    return;

	Frame current = DisplayIO.getCurrentFrame();

	// Dont need to do anything if the frame to display is already being
	// displayed
	if (current.equals(toDisplay))
	    return;

	// move any anchored connected items
	if (FreeItems.itemsAttachedToCursor()) {
	    List<Item> toAdd = new ArrayList<Item>();
	    List<Item> toCheck = new ArrayList<Item>(FreeItems.getInstance());

	    while (toCheck.size() > 0) {
		Item i = toCheck.get(0);
		Collection<Item> connected = i.getAllConnected();

		// // Only move completely enclosed items
		// if (!toCheck.containsAll(connected)) {
		// connected.retainAll(FreeItems.getInstance());
		// FreeItems.getInstance().removeAll(connected);
		// toCheck.removeAll(connected);
		// FrameMouseActions.anchor(connected);
		// } else {
		// toCheck.removeAll(connected);
		// }

		// Anchor overlay items where they belong
		if (i.getParent() != null && i.getParent() != current) {
		    FreeItems.getInstance().removeAll(connected);
		    toCheck.removeAll(connected);
		    FrameMouseActions.anchor(connected);
		} else {
		    // Add stuff that is partially enclosed
		    // remove all the connected items from our list to check
		    toCheck.removeAll(connected);
		    // Dont add the items that are free
		    connected.removeAll(FreeItems.getInstance());
		    toAdd.addAll(connected);
		}
	    }

	    current.removeAllItems(toAdd);

	    boolean oldChange = toDisplay.hasChanged();
	    toDisplay.updateIDs(toAdd);
	    toDisplay.addAllItems(toAdd);
	    toDisplay.setChanged(oldChange);
	}

	if (addToBack && current != toDisplay) {
	    FrameIO.checkTDFC(current);
	}

	// if the saving happened properly, we can continue
	if (!LeavingFrame(current)) {
	    MessageBay.displayMessage("Navigation cancelled");
	    return;
	}

	if (addToBack && current != toDisplay) {
	    DisplayIO.addToBack(current);
	}

	Parse(toDisplay);
	DisplayIO.setCurrentFrame(toDisplay, incrementStats);
	FrameMouseActions.updateCursor();
	// FrameMouseActions.getInstance().refreshHighlights();
	// update response timer
	_LastResponse = ResponseTimer.getElapsedSeconds();
	_ResponseTimeSum += _LastResponse;
	DisplayIO.UpdateTitle();
    }

    /**
     * Loads and displays the Frame with the given framename, and adds the
     * current frame to the back-stack if required.
     * 
     * @param framename
     *            The name of the Frame to load and display
     * @param addToBack
     *            True if the current Frame should be added to the back-stack,
     *            false otherwise
     */
    public static void DisplayFrame(String frameName, boolean addToBack,
	    boolean incrementStats) {
	Frame newFrame = getFrame(frameName);

	if (newFrame != null)
	    // display the frame
	    DisplayFrame(newFrame, addToBack, incrementStats);
    }

    /**
     * Loads and displays the Frame with the given framename and adds the
     * current frame to the back-stack. This is the same as calling
     * DisplayFrame(framename, true)
     * 
     * @param framename
     *            The name of the Frame to load and display
     */
    public static void DisplayFrame(String framename) {
	DisplayFrame(framename, true, true);
    }

    public static Frame getFrame(String frameName) {
	// if the new frame does not exist then tell the user
	Frame f = FrameIO.LoadFrame(frameName);

	if (f == null) {
	    MessageBay.errorMessage("Frame '" + frameName
		    + "' could not be found.");
	}

	return f;
    }

    /**
     * Creates a new Picture Item from the given Text source Item and adds it to
     * the given Frame.
     * 
     * @return True if the image was created successfully, false otherwise
     */
    private static boolean createPicture(Frame frame, Text txt) {
	// attempt to create the picture
	Picture pic = ItemUtils.CreatePicture(txt, frame);

	// if the picture could not be created successfully
	if (pic == null) {
	    String imagePath = txt.getText();
	    assert (imagePath != null);
	    imagePath = new AttributeValuePair(imagePath).getValue().trim();
	    if (imagePath.length() == 0) {
		return false;
		// MessageBay.errorMessage("Expected image path after @i:");
	    } else {
		MessageBay.errorMessage("Image " + imagePath
			+ " could not be loaded");
	    }
	    return false;
	}
	frame.addItem(pic);

	return true;
    }

    /**
     * Creates an interactive widget and adds it to a frame. If txt has no
     * parent the parent will be set to frame.
     * 
     * @param frame
     *            Frame to add widget to. Must not be null.
     * 
     * @param txt
     *            Text to create the widget from. Must not be null.
     * 
     * @return True if created/added. False if coul not create.
     * 
     * @author Brook Novak
     */
    private static boolean createWidget(Frame frame, Text txt) {

	if (frame == null)
	    throw new NullPointerException("frame");
	if (txt == null)
	    throw new NullPointerException("txt");

	// Safety
	if (txt.getParent() == null)
	    txt.setParent(frame);

	InteractiveWidget iw = null;

	try {

	    iw = InteractiveWidget.createWidget(txt);

	} catch (InteractiveWidgetNotAvailableException e) {
	    e.printStackTrace();
	    MessageBay.errorMessage("Cannot create iWidget: " + e.getMessage());
	} catch (InteractiveWidgetInitialisationFailedException e) {
	    e.printStackTrace();
	    MessageBay.errorMessage("Cannot create iWidget: " + e.getMessage());
	} catch (IllegalArgumentException e) {
	    e.printStackTrace();
	    MessageBay.errorMessage("Cannot create iWidget: " + e.getMessage());
	}

	if (iw == null)
	    return false;

	frame.removeItem(txt);

	frame.addAllItems(iw.getItems());

	return true;
    }

    public static Collection<String> ParseProfile(Frame profile) {
	Collection<String> errors = new LinkedList<String>();
	if (profile == null)
	    return errors;

	/*
	 * Make sure the correct cursor shows when turning off the custom cursor
	 * and reparsing the profile frame
	 */
	FreeItems.getCursor().clear();
	DisplayIO.setCursor(Item.HIDDEN_CURSOR);
	DisplayIO.setCursor(Item.DEFAULT_CURSOR);

	// check for settings tags
	for (Text item : profile.getBodyTextItems(true)) {
	    try {

		AttributeValuePair avp = new AttributeValuePair(item.getText());
		String attributeFullCase = avp.getAttributeOrValue();

		if (attributeFullCase == null) {
		    continue;
		}
		String attribute = attributeFullCase.trim().toLowerCase()
			.replaceAll("^@", "");

		if (attribute.equals("settings")) {
		    Settings.parseSettings(item);
		}

	    } catch (Exception e) {
		if (e.getMessage() != null) {
		    errors.add(e.getMessage());
		} else {
		    e.printStackTrace();
		    errors.add("Error parsing [" + item.getText() + "] on "
			    + profile.getName());
		}
	    }
	}

	return errors;
    }

    /**
     * Sets the first frame to be displayed.
     * 
     * @param profile
     */
    public static void loadFirstFrame(Frame profile) {
	if (UserSettings.HomeFrame.get() == null)
	    UserSettings.HomeFrame.set(profile.getName());

	Frame firstFrame = FrameIO.LoadFrame(UserSettings.HomeFrame.get());
	if (firstFrame == null) {
	    MessageBay.warningMessage("Home frame not found: "
		    + UserSettings.HomeFrame);
	    UserSettings.HomeFrame.set(profile.getName());
	    DisplayIO.setCurrentFrame(profile, true);
	} else {
	    DisplayIO.setCurrentFrame(firstFrame, true);
	}

    }

    public static Color[] getColorWheel(Frame frame) {
	if (frame != null) {
	    List<Text> textItems = frame.getBodyTextItems(false);
	    Color[] colorList = new Color[textItems.size() + 1];
	    for (int i = 0; i < textItems.size(); i++) {
		colorList[i] = textItems.get(i).getColor();
	    }
	    // Make the last item transparency or default for forecolor
	    colorList[colorList.length - 1] = null;

	    return colorList;
	}
	return new Color[] { Color.black, Color.white, null };
    }

    public static String getLink(Item item, String alt) {
	if (item == null || !(item instanceof Text))
	    return alt;

	AttributeValuePair avp = new AttributeValuePair(item.getText());
	assert (avp != null);

	if (avp.hasPair() && avp.getValue().trim().length() != 0) {
	    item.setLink(avp.getValue());
	    return avp.getValue();
	} else if (item.getLink() != null) {
	    return item.getAbsoluteLink();
	}

	return alt;
    }

    public static String getDir(String name) {
	if (name != null) {
	    File tester = new File(name);
	    if (tester.exists() && tester.isDirectory()) {
		if (name.endsWith(File.separator))
		    return name;
		else
		    return name + File.separator;
	    } else {
		throw new RuntimeException("Directory not found: " + name);
	    }
	}
	throw new RuntimeException("Missing value for profile attribute" + name);
    }

    public static ArrayList<String> getDirs(Item item) {
	ArrayList<String> dirsToAdd = new ArrayList<String>();
	String dirListFrameName = item.getAbsoluteLink();
	if (dirListFrameName != null) {
	    Frame dirListFrame = FrameIO.LoadFrame(dirListFrameName);
	    if (dirListFrame != null) {
		for (Text t : dirListFrame.getBodyTextItems(false)) {
		    String dirName = t.getText().trim();
		    File tester = new File(dirName);
		    if (tester.exists() && tester.isDirectory()) {
			if (dirName.endsWith(File.separator))
			    dirsToAdd.add(dirName);
			else
			    dirsToAdd.add(dirName + File.separator);
		    }
		}
	    }
	}

	return dirsToAdd;
    }

    public static void Parse(Frame toParse) {
	Parse(toParse, false);
    }

    /**
     * Checks for any special Annotation items and updates the display as
     * necessary. Special Items: Images, overlays, sort.
     * 
     */
    public static void Parse(Frame toParse, boolean firstParse) {
	Parse(toParse, firstParse, false);
    }

    /**
     * 
     * @param toParse
     * @param firstParse
     * @param ignoreAnnotations
     *            used to prevent infinate loops such as when performing TDFC
     *            with an ao tag linked to a frame with an frameImage of a frame
     *            which also has an ao tag on it.
     */
    public static void Parse(Frame toParse, boolean firstParse,
	    boolean ignoreAnnotations) {
	// TODO check why we are getting toParse == null... when profile frame
	// is being created and change the lines below
	if (toParse == null)
	    return;
	// System.out.println(firstParse);
	if (firstParse)
	    ItemUtils.EnclosedCheck(toParse.getItems());
	List<Item> items = toParse.getItems();

	// if XRayMode is on, replace pictures with their underlying text
	if (FrameGraphics.isXRayMode()) {

	    // BROOK: Must handle these a little different
	    List<InteractiveWidget> widgets = toParse.getInteractiveWidgets();

	    for (Item i : items) {
		if (i instanceof XRayable) {
		    toParse.removeItem(i);
		    // Show the items
		    for (Item item : ((XRayable) i).getConnected()) {
			item.setVisible(true);
			item.removeEnclosure(i);
		    }
		} else if (i instanceof WidgetCorner) {
		    toParse.removeItem(i);
		} else if (i instanceof WidgetEdge) {
		    toParse.removeItem(i);
		} else if (i.hasFormula()) {
		    i.setText(i.getFormula());
		} else if (i.hasOverlay()) {
		    i.setVisible(true);
		    // int x = i.getBoundsHeight();
		}
	    }

	    for (InteractiveWidget iw : widgets) {
		toParse.addItem(iw.getSource());
	    }
	}

	// Text title = null;
	// Text template = UserSettingsTemplate.copy();

	List<Overlay> overlays = new ArrayList<Overlay>();
	List<Vector> vectors = new ArrayList<Vector>();

	// disable reading of cached overlays if in twinframes mode
	if (DisplayIO.isTwinFramesOn())
	    FrameIO.SuspendCache();

	DotType pointtype = DotType.square;
	boolean filledPoints = true;

	UserAppliedPermission permission = toParse.getUserAppliedPermission();
	toParse.clearAnnotations();

	// check for any new overlay items
	for (Item i : toParse.getItems()) {
	    try {
		// reset overlay permission
		i.setOverlayPermission(null);
		// i.setPermission(permission);
		if (i instanceof WidgetCorner) {
		    // TODO improve efficiency so it only updates once... using
		    // observer design pattern
		    i.update();
		} else if (i instanceof Text) {
		    if (i.isAnnotation()) {
			if (ItemUtils.startsWithTag(i, ItemUtils.TAG_POINTTYPE)) {
			    Text txt = (Text) i;
			    String line = txt.getFirstLine();
			    line = ItemUtils.StripTag(line,
				    ItemUtils.GetTag(ItemUtils.TAG_POINTTYPE));

			    if (line != null) {
				line = line.toLowerCase();
				if (line.indexOf(" ") > 0) {
				    String fill = line.substring(line
					    .indexOf(" ") + 1);
				    if (fill.startsWith("nofill"))
					filledPoints = false;
				    else
					filledPoints = true;
				}

				if (line.startsWith("circle"))
				    pointtype = DotType.circle;
				else
				    pointtype = DotType.square;
			    }
			}// check for new VECTOR items
			else if (!FrameGraphics.isXRayMode()
				&& ItemUtils.startsWithTag(i,
					ItemUtils.TAG_VECTOR)
				&& i.getLink() != null) {
			    if (!i.getAbsoluteLink().equals(toParse.getName()))
				addVector(vectors, UserAppliedPermission.none,
					permission, i);
			} else if (!FrameGraphics.isXRayMode()
				&& ItemUtils.startsWithTag(i,
					ItemUtils.TAG_ACTIVE_VECTOR)
				&& i.getLink() != null) {
			    if (!i.getAbsoluteLink().equals(toParse.getName()))
				addVector(vectors,
					UserAppliedPermission.followLinks,
					permission, i);
			}
			// check for new OVERLAY items
			else if (!ignoreAnnotations
				&& ItemUtils.startsWithTag(i,
					ItemUtils.TAG_OVERLAY)
				&& i.getLink() != null) {
			    if (i.getAbsoluteLink().equalsIgnoreCase(
				    toParse.getName())) {
				// This frame contains an active overlay which
				// points to itself
				MessageBay
					.errorMessage(toParse.getName()
						+ " contains an @o which links to itself");
				continue;
			    }

			    Frame overlayFrame = FrameIO.LoadFrame(i
				    .getAbsoluteLink());
			    // Parse(overlay);
			    if (overlayFrame != null
				    && Overlay.getOverlay(overlays,
					    overlayFrame) == null)
				overlays.add(new Overlay(overlayFrame,
					UserAppliedPermission.none));
			}
			// check for ACTIVE_OVERLAY items
			else if (!ignoreAnnotations
				&& ItemUtils.startsWithTag(i,
					ItemUtils.TAG_ACTIVE_OVERLAY)
				&& i.getLink() != null) {
			    String link = i.getAbsoluteLink();
			    if (link.equalsIgnoreCase(toParse.getName())) {
				// This frame contains an active overlay which
				// points to itself
				MessageBay
					.errorMessage(toParse.getName()
						+ " contains an @ao which links to itself");
				continue;
			    }
			    Frame overlayFrame = null;

			    Frame current = DisplayIO.getCurrentFrame();
			    if (current != null) {
				for (Overlay o : current.getOverlays()) {
				    if (o.Frame.getName()
					    .equalsIgnoreCase(link))
					overlayFrame = o.Frame;
				}
			    }
			    if (overlayFrame == null)
				overlayFrame = FrameIO.LoadFrame(link);

			    // get level if specified
			    String level = new AttributeValuePair(i.getText())
				    .getValue();
			    // default permission (if none is specified)
			    PermissionPair permissionLevel = new PermissionPair(
				    level, UserAppliedPermission.followLinks);

			    if (overlayFrame != null) {
				Overlay existingOverlay = Overlay.getOverlay(
					overlays, overlayFrame);
				// If it wasn't in the list create it and add
				// it.
				if (existingOverlay == null) {
				    Overlay newOverlay = new Overlay(
					    overlayFrame,
					    permissionLevel
						    .getPermission(overlayFrame
							    .getOwner()));
				    i.setOverlay(newOverlay);
				    overlays.add(newOverlay);
				} else {
				    existingOverlay.Frame
					    .setPermission(permissionLevel);
				}
			    }
			}
			// check for Images and widgets
			else {
			    if (!FrameGraphics.isXRayMode()) {
				if (ItemUtils.startsWithTag(i,
					ItemUtils.TAG_IMAGE, true)) {
				    if (!i.hasEnclosures()) {
					createPicture(toParse, (Text) i);
				    }
				    // check for frame images
				} else if (ItemUtils.startsWithTag(i,
					ItemUtils.TAG_FRAME_IMAGE)
					&& i.getLink() != null
					&& !i.getAbsoluteLink()
						.equalsIgnoreCase(
							toParse.getName())) {
				    XRayable image = null;
				    if (i.hasEnclosures()) {
					// i.setHidden(true);
					// image =
					// i.getEnclosures().iterator().next();
					// image.refresh();
				    } else {
					image = new FrameImage((Text) i,
						toParse, null);
				    }
				    // TODO Add the image when creating new
				    // FrameImage
				    toParse.addItem(image);
				} else if (ItemUtils.startsWithTag(i,
					ItemUtils.TAG_BITMAP_IMAGE)
					&& i.getLink() != null
					&& !i.getAbsoluteLink()
						.equalsIgnoreCase(
							toParse.getName())) {
				    XRayable image = null;
				    if (i.hasEnclosures()) {
					// image =
					// i.getEnclosures().iterator().next();
					// image.refresh();
					// i.setHidden(true);
				    } else {
					// If a new bitmap is created for a
					// frame which already has a bitmap dont
					// recreate the bitmap
					image = new FrameBitmap((Text) i,
						toParse, null);
				    }
				    toParse.addItem(image);
				} else if (ItemUtils.startsWithTag(i, "@c")) {
				    // Can only have a @c
				    if (!i.hasEnclosures()
					    && i.getLines().size() == 1) {
					toParse.addItem(new Circle((Text) i));
				    }
				    // Check for JSItem
				} else if(ItemUtils.startsWithTag(i, "@js")) {
					toParse.addItem(new JSItem((Text) i));
				    // Check for interactive widgets
				} else if (ItemUtils.startsWithTag(i,
					ItemUtils.TAG_IWIDGET)) {
				    createWidget(toParse, (Text) i);
				}
			    }
			    // TODO decide exactly what to do here!!
			    toParse.addAnnotation((Text) i);
			}
		    } else if (!FrameGraphics.isXRayMode() && i.hasFormula()) {
			i.calculate(i.getFormula());
		    }
		}
	    } catch (Exception e) {
		Logger.Log(e);
		e.printStackTrace();
		MessageBay.warningMessage("Exception occured when loading "
			+ i.getClass().getSimpleName() + "(ID: " + i.getID()
			+ ") " + e.getMessage() != null ? e.getMessage() : "");
	    }
	}

	/*
	 * for (Item i : items) { if (i instanceof Dot) { ((Dot)
	 * i).setPointType(pointtype); ((Dot) i).useFilledPoints(filledPoints);
	 * } }
	 */

	FrameIO.ResumeCache();

	toParse.clearOverlays();
	toParse.clearVectors();
	toParse.addAllOverlays(overlays);
	toParse.addAllVectors(vectors);

    }

    /**
     * @param vectors
     * @param permission
     * @param i
     */
    private static void addVector(List<Vector> vectors,
	    UserAppliedPermission defaultPermission,
	    UserAppliedPermission framePermission, Item i) {
	// TODO It is possible to get into an infinate loop if a
	// frame contains an @ao which leads to a frame with an
	// @v which points back to the frame with the @ao
	Frame vector = FrameIO.LoadFrame(i.getAbsoluteLink());

	// Get the permission from off the vector frame
	UserAppliedPermission vectorPermission = UserAppliedPermission
		.getPermission(vector.getAnnotationValue("permission"),
			defaultPermission);
	// If the frame permission is lower, use that
	vectorPermission = UserAppliedPermission.min(vectorPermission,
		framePermission);
	// Highest permissable permission for vectors is copy
	vectorPermission = UserAppliedPermission.min(vectorPermission,
		UserAppliedPermission.copy);
	if (vector != null) {
	    String scaleString = new AttributeValuePair(i.getText()).getValue();
	    Float scale = 1F;
	    try {
		scale = Float.parseFloat(scaleString);
	    } catch (Exception e) {
	    }
	    Vector newVector = new Vector(vector, vectorPermission, scale, i);
	    i.setOverlay(newVector);
	    i.setVisible(false);
	    vectors.add(newVector);
	}
    }

    public static Item onItem(float floatX, float floatY,
	    boolean changeLastEdited) {
	return onItem(DisplayIO.getCurrentFrame(), floatX, floatY,
		changeLastEdited);
    }

    /**
     * Searches through the list of items on this frame to find one at the given
     * x,y coordinates.
     * 
     * @param x
     *            The x coordinate
     * @param y
     *            The y coordinate
     * @return The Item at the given coordinates, or NULL if none is found.
     */
    public static Item onItem(Frame toCheck, float floatX, float floatY,
	    boolean bResetLastEdited) {
	// System.out.println("MouseX: " + floatX + " MouseY: " + floatY);
	int x = Math.round(floatX);
	int y = Math.round(floatY);
	if (toCheck == null)
	    return null;

	List<Item> possibles = new ArrayList<Item>(0);

	// if the mouse is in the message area
	if (y > FrameGraphics.getMaxFrameSize().getHeight()) {
	    // check the individual message items
	    for (Item message : MessageBay.getMessages()) {
		if (message != null) {
		    if (message.contains(x, y)) {
			message.setOverlayPermission(UserAppliedPermission.copy);
			possibles.add(message);
		    } else {
			// Not sure why but if the line below is removed then
			// several items can be highlighted at once
			message.setHighlightMode(Item.HighlightMode.None);
		    }
		}
	    }

	    // check the link to the message frame
	    if (MessageBay.getMessageLink() != null) {
		if (MessageBay.getMessageLink().contains(x, y)) {
		    MessageBay.getMessageLink().setOverlayPermission(
			    UserAppliedPermission.copy);
		    possibles.add(MessageBay.getMessageLink());
		}
	    }

	    // this is taken into account in contains
	    // y -= FrameGraphics.getMaxFrameSize().height;
	    // otherwise, the mouse is on the frame
	} else {
	    if (LastEdited != null) {
		if (LastEdited.contains(x, y)
			&& !FreeItems.getInstance().contains(LastEdited)
			&& LastEdited.getParent() == DisplayIO
				.getCurrentFrame()
			&& LastEdited.getParent().getItems()
				.contains(LastEdited)) {
		    LastEdited.setOverlayPermission(UserAppliedPermission.full);
		    return LastEdited;
		} else if (bResetLastEdited) {
		    setLastEdited(null);
		}
	    }
	    ArrayList<Item> checkList = new ArrayList<Item>();
	    checkList.addAll(toCheck.getInteractableItems());
	    checkList.add(toCheck.getNameItem());
	    for (Item i : checkList) {
		// do not check annotation items in audience mode
		if (i.isVisible()
			&& !(FrameGraphics.isAudienceMode() && i.isAnnotation())) {
		    if (i.contains(x, y)
			    && !FreeItems.getInstance().contains(i)) {
			possibles.add(i);
		    }
		}
	    }
	}

	// if there are no possible items, return null
	if (possibles.size() == 0)
	    return null;

	// if there is only one possibility, return it
	if (possibles.size() == 1)
	    return possibles.get(0);

	// return closest x,y pair to mouse
	Item closest = possibles.get(0);
	int distance = (int) Math.round(Math.sqrt(Math.pow(
		Math.abs(closest.getX() - x), 2)
		+ Math.pow(Math.abs(closest.getY() - y), 2)));

	for (Item i : possibles) {
	    int d = (int) Math.round(Math.sqrt(Math.pow(Math.abs(i.getX() - x),
		    2) + Math.pow(Math.abs(i.getY() - y), 2)));

	    // System.out.println(d);
	    if (d <= distance) {
		distance = d;

		// dots take precedence over lines
		if ((!(closest instanceof Dot && i instanceof Line))
			&& (!(closest instanceof Text && i instanceof Line)))
		    closest = i;

	    }

	}

	return closest;
    }

    public synchronized static Item getCurrentItem() {
	return onItem(DisplayIO.getCurrentFrame(), DisplayIO.getMouseX(),
		FrameMouseActions.getY(), true);
    }

    public static Polygon getEnlosingPolygon() {
	Collection<Item> enclosure = getEnclosingLineEnds();
	if (enclosure == null || enclosure.size() == 0)
	    return null;

	return enclosure.iterator().next().getEnclosedShape();
    }

    /**
     * 
     * @param currentItem
     * @return
     */
    public static Collection<Item> getCurrentItems() {
	return getCurrentItems(getCurrentItem());
    }

    public static Collection<Item> getCurrentItems(Item currentItem) {

	Collection<Item> enclosure = getEnclosingLineEnds();
	if (enclosure == null || enclosure.size() == 0)
	    return null;

	Item firstItem = enclosure.iterator().next();

	Collection<Item> enclosed = getItemsEnclosedBy(
		DisplayIO.getCurrentFrame(), firstItem.getEnclosedShape());

	// Brook: enclosed widgets are to be fully enclosed, never partially
	/*
	 * MIKE says: but doesnt this mean that widgets are treated differently
	 * from ALL other object which only need to be partially enclosed to be
	 * picked up
	 */
	List<InteractiveWidget> enclosedWidgets = new LinkedList<InteractiveWidget>();
	for (Item i : enclosed) {
	    // Don't want to lose the highlighting from the current item
	    if (i == currentItem || enclosure.contains(i)) {
		continue;
	    }
	    // Don't want to lose the highlighting of connected Dots
	    if (i instanceof Dot
		    && i.getHighlightMode() == HighlightMode.Connected) {
		for (Line l : i.getLines()) {
		    if (l.getOppositeEnd(i).getHighlightMode() == HighlightMode.Normal) {
			continue;
		    }
		}
	    }
	    if (i instanceof WidgetCorner) {
		if (!enclosedWidgets.contains(((WidgetCorner) i)
			.getWidgetSource()))
		    enclosedWidgets.add(((WidgetCorner) i).getWidgetSource());
	    }
	    i.setHighlightMode(Item.HighlightMode.None);
	}

	for (InteractiveWidget iw : enclosedWidgets) {
	    for (Item i : iw.getItems()) {
		if (!enclosed.contains(i)) {
		    enclosed.add(i);
		}
	    }
	}

	return enclosed;
    }

    public static Collection<Item> getEnclosingLineEnds() {
	return getEnclosingLineEnds(new Point(DisplayIO.getMouseX(),
		FrameMouseActions.getY()));
    }

    public static Collection<Item> getEnclosingLineEnds(Point position) {
	// update enclosed shapes
	Frame current = DisplayIO.getCurrentFrame();
	List<Item> items = current.getItems();

	// Remove all items that are connected to freeItems
	List<Item> freeItems = new ArrayList<Item>(FreeItems.getInstance());
	while (freeItems.size() > 0) {
	    Item item = freeItems.get(0);
	    Collection<Item> connected = item.getAllConnected();
	    items.removeAll(connected);
	    freeItems.removeAll(connected);
	}

	List<Item> used = new ArrayList<Item>(0);

	while (items.size() > 0) {
	    Item i = items.get(0);
	    items.remove(i);
	    if (i.isEnclosed()) {
		Polygon p = i.getEnclosedShape();
		if (p.contains(position.x, position.y)) {
		    used.add(i);
		    items.removeAll(i.getEnclosingDots());
		}
	    }
	}

	if (used.size() == 0)
	    return null;

	// if there is only one possibility, return it
	if (used.size() == 1) {
	    return used.get(0).getEnclosingDots();
	    // otherwise, determine which polygon is closest to the cursor
	} else {
	    Collections.sort(used, new Comparator<Item>() {
		public int compare(Item d1, Item d2) {
		    Polygon p1 = d1.getEnclosedShape();
		    Polygon p2 = d2.getEnclosedShape();

		    int closest = Integer.MAX_VALUE;
		    int close2 = Integer.MAX_VALUE;

		    int mouseX = DisplayIO.getMouseX();
		    int mouseY = FrameMouseActions.getY();

		    for (int i = 0; i < p1.npoints; i++) {
			int diff = Math.abs(p1.xpoints[i] - mouseX)
				+ Math.abs(p1.ypoints[i] - mouseY);
			int diff2 = Integer.MAX_VALUE;

			if (i < p2.npoints)
			    diff2 = Math.abs(p2.xpoints[i] - mouseX)
				    + Math.abs(p2.ypoints[i] - mouseY);

			if (diff < Math.abs(closest)) {
			    close2 = closest;
			    closest = diff;
			} else if (diff < Math.abs(close2))
			    close2 = diff;

			if (diff2 < Math.abs(closest)) {
			    close2 = closest;
			    closest = -diff2;
			} else if (diff2 < Math.abs(close2))
			    close2 = diff2;
		    }

		    if (closest > 0 && close2 > 0)
			return -10;

		    if (closest < 0 && close2 < 0)
			return 10;

		    if (closest > 0)
			return -10;

		    return 10;
		}

	    });

	    return used.get(0).getEnclosingDots();
	}
    }

    // TODO Remove this method!!
    // Can just getItemsWithin be used?
    public static Collection<Item> getItemsEnclosedBy(Frame frame, Polygon poly) {
	Collection<Item> contained = frame.getItemsWithin(poly);

	Collection<Item> results = new LinkedHashSet<Item>(contained.size());

	// check for correct permissions
	for (Item item : contained) {
	    // if the item is on the frame
	    if (item.getParent() == frame || item.getParent() == null) {
		// item.Permission = Permission.full;
		results.add(item);
		// otherwise, it must be on an overlay frame
	    } else {
		for (Overlay overlay : frame.getOverlays()) {
		    if (overlay.Frame == item.getParent()) {
			item.setOverlayPermission(overlay.permission);
			results.add(item);
			break;
		    }
		}
	    }
	}

	return results;
    }

    /**
     * Fills the given Frame with default profile tags
     */
    public static void CreateDefaultProfile(String username, Frame profile) {
	Text title = profile.getTitleItem();
	if (username.equals(UserSettings.DEFAULT_PROFILE_NAME)) {
	    title.setText("Default Profile Frame");
	} else {
	    // if this profile is not the default profile, copy it from the default profile instead of generating a new profile
		// (this allows the possibility of modifying the default profile and having any new profiles get those modifications)
		Frame nextDefault = FrameIO.LoadProfile(UserSettings.DEFAULT_PROFILE_NAME);
		if (nextDefault == null) {
			try {
				nextDefault = FrameIO.CreateNewProfile(UserSettings.DEFAULT_PROFILE_NAME);
			} catch (Exception e) {
				// TODO tell the user that there was a problem creating the
				// profile frame and close nicely
				e.printStackTrace();
			}
		}
		// load profile frame and set title correctly
		profile.reset();
		profile.removeAllItems(profile.getAllItems());
		// set relative link on all items so their links will correctly point to the page on the current profile rather than on the default profile
		for(Item i : nextDefault.getAllItems()) {
			i.setRelativeLink();
		}
		profile.addAllItems(ItemUtils.CopyItems(nextDefault.getAllItems()));
		profile.setTitle(username + "'s Profile Frame");
		FrameIO.SaveFrame(profile);
		
		Frame nextProfile = profile;
		MessageBay.suppressMessages(true);
		while((nextDefault = FrameIO.LoadNext(nextDefault)) != null) {
			// in case there are gaps in the frame numbering of the default profile (e.g. if a user has edited it),
			// we need to replicate those gaps in the copied profile so the links will work correctly
			while(nextProfile.getNumber() < nextDefault.getNumber()) {
				nextProfile = FrameIO.CreateFrame(profile.getFramesetName(), null, null);
			}
			// if the new profile has a frame number higher than the current frame number in the default profile,
			// the new profile must already exist, so just exit
			// (TODO: should we wipe the frames instead?)
			if(nextProfile.getNumber() > nextDefault.getNumber()) {
				break;
			}
			nextProfile.reset();
    		nextProfile.removeAllItems(nextProfile.getAllItems());
    		// set relative link on all items so their links will correctly point to the page on the current profile rather than on the default profile
    		for(Item i : nextDefault.getAllItems()) {
    			i.setRelativeLink();
    		}
    		nextProfile.addAllItems(ItemUtils.CopyItems(nextDefault.getAllItems()));
    		FrameIO.SaveFrame(nextProfile);
		}
		MessageBay.suppressMessages(false);
		
		return;
	}

	// int spacing = 50;
	final int intialYPos = 75;
	int xPos = 75;
	int yPos = intialYPos;

	// yPos += spacing;
	// profile.addText(xPos, yPos, "@HomeFrame", null, profile.getName());
	// yPos += spacing;
	// String defaultFrameName = profile.getFramesetName() + "0";
	// profile.addText(xPos, yPos, "@DefaultFrame", null, defaultFrameName);
	// yPos += spacing;
	//
	// profile.addText(xPos, yPos, "@InitialWidth: "
	// + UserSettings.InitialWidth, null);
	// yPos += spacing;
	//
	// profile.addText(xPos, yPos, "@InitialHeight: "
	// + UserSettings.InitialHeight, null);
	// yPos += spacing;
	//
	// Text t = profile.addText(xPos, yPos, "@ItemTemplate", null);
	// t.setColor(null);
	//
	// yPos += spacing;
	// t = profile.addText(xPos, yPos, "@AnnotationTemplate", null);
	// t.setColor(Color.gray);
	//
	// yPos += spacing;
	// t = profile.addText(xPos, yPos, "@CommentTemplate", null);
	// t.setColor(Color.green.darker());
	//
	// yPos += spacing;
	// t = profile.addText(xPos, yPos, "@StatsTemplate", null);
	// t.setColor(Color.BLACK);
	// t.setBackgroundColor(new Color(0.9F, 0.9F, 0.9F));
	// t.setFamily(Text.MONOSPACED_FONT);
	// t.setSize(14);

	Text t;

	xPos = 300;
	// yPos = intialYPos + spacing;
	yPos = 100;

	// Add documentation links
	File helpDirectory = new File(FrameIO.HELP_PATH);
	if (helpDirectory != null) {
	    File[] helpFramesets = helpDirectory.listFiles();
	    if (helpFramesets != null) {

		// Add the title for the help index
		Text help = profile.addText(xPos, yPos, "@Expeditee Help", null);
		help.setSize(25);
		help.setFontStyle("Bold");
		help.setFamily("SansSerif");
		help.setColor(TemplateSettings.ColorWheel.get()[3]);

		xPos += 25;
		System.out.println("Installing frameset: ");

		boolean first_item = true;

		for (File helpFrameset : helpFramesets) {
		    String framesetName = helpFrameset.getName();
		    if (!FrameIO.isValidFramesetName(framesetName)) {
			continue;
		    }

		    if (first_item) {
			System.out.print("  " + framesetName);
			first_item = false;
		    }
		    else {
			System.out.print(", " + framesetName);
		    }
		    System.out.flush();

		    Frame indexFrame = FrameIO.LoadFrame(framesetName + '1');
		    // Look through the folder for help index pages
		    if (indexFrame != null
			    && ItemUtils.FindTag(indexFrame.getItems(),
				    "@HelpIndex") != null) {
			// yPos += spacing;
			yPos += 30;
			t = profile.addText(xPos, yPos,
				'@' + indexFrame.getFramesetName(), null);
			t.setLink(indexFrame.getName());
			t.setColor(Color.gray);
		    }
		}
		System.out.println();
	    }
	}

	xPos = 50;
	yPos = 100;

	// Populate Start Pages and Settings
	File framesetDirectory = new File(FrameIO.FRAME_PATH);

	if (framesetDirectory.exists()) {
	    File[] startpagesFramesets = framesetDirectory.listFiles();

	    if (startpagesFramesets != null) {
		// Add Start Page title
		Text templates = profile.addText(xPos, yPos, "@Start Pages",
			null);
		templates.setSize(25);
		templates.setFontStyle("Bold");
		templates.setFamily("SansSerif");
		templates.setColor(TemplateSettings.ColorWheel.get()[3]);

		xPos += 25;

		// Start Pages should be the first frame in its own frameset +
		// frameset name should be present in FrameUtils.startPages[].
		for (File startpagesFrameset : startpagesFramesets) {
		    String framesetName = startpagesFrameset.getName();

		    // Only add link if frameset is a startpage
		    for (int i = 0; i < startPages.length; i++) {
			if (framesetName.equals(startPages[i])) {
			    Frame indexFrame = FrameIO
				    .LoadFrame(framesetName + '1');

			    // Add start page link
			    if (indexFrame != null) {
				yPos += 30;
				t = profile.addText(xPos, yPos,
					'@' + indexFrame.getFramesetName(),
					null);
				t.setLink(indexFrame.getName());
				t.setColor(Color.gray);
			    }
			}
		    }
		}
	    }
	}

	FrameIO.SaveFrame(profile);

	// Populate settings frameset
	Settings.Init();
	t = profile.addText(550, 100, "@Settings", null);
	t.setSize((float) 25.0);
	t.setFamily("SansSerif");
	t.setFontStyle("Bold");
	t.setColor(Color.gray);
	Settings.generateSettingsTree(t);

	FrameIO.SaveFrame(profile);
    }

    private static void checkTDFCItemWaiting(Frame currentFrame) {
	Item tdfcItem = FrameUtils.getTdfcItem();
	// if there is a TDFC Item waiting
	if (tdfcItem != null) {
	    boolean change = currentFrame.hasChanged();
	    boolean saved = currentFrame.isSaved();
	    // Save the parent of the item if it has not been saved
	    if (!change && !saved) {
		tdfcItem.setLink(null);
		tdfcItem.getParent().setChanged(true);
		FrameIO.SaveFrame(tdfcItem.getParent());
		FrameGraphics.Repaint();
	    } else {
		SessionStats.CreatedFrame();
	    }

	    setTdfcItem(null);
	}
    }

    public static void setTdfcItem(Item _tdfcItem) {
	FrameUtils._tdfcItem = _tdfcItem;
    }

    public static Item getTdfcItem() {
	return FrameUtils._tdfcItem;
    }

    private static Item _tdfcItem = null;

    public static void setLastEdited(Text lastEdited) {

	// If the lastEdited is being changed then check if its @i
	Frame toReparse = null;
	Frame toRecalculate = null;
	Frame toUpdateObservers = null;

	if (LastEdited == null) {
	    // System.out.print("N");
	} else if (LastEdited != null) {
	    // System.out.print("T");
	    Frame parent = LastEdited.getParentOrCurrentFrame();

	    if (lastEdited != LastEdited) {
		if (LastEdited.startsWith("@i")) {
		    // Check if its an image that can be resized to fit a box
		    // around it
		    String text = LastEdited.getText();
		    if (text.startsWith("@i:")
			    && !Character
				    .isDigit(text.charAt(text.length() - 1))) {
			Collection<Item> enclosure = FrameUtils
				.getEnclosingLineEnds(LastEdited.getPosition());
			if (enclosure != null) {
			    for (Item i : enclosure) {
				if (i.isLineEnd() && i.isEnclosed()) {
				    DisplayIO.getCurrentFrame().removeAllItems(
					    enclosure);
				    Rectangle rect = i.getEnclosedRectangle();
				    LastEdited
					    .setText(LastEdited.getText()
						    + " "
						    + Math.round(rect
							    .getWidth()));
				    LastEdited.setPosition(new Point(rect.x,
					    rect.y));
				    LastEdited.setThickness(i.getThickness());
				    LastEdited.setBorderColor(i.getColor());
				    break;
				}
			    }
			    FrameMouseActions.deleteItems(enclosure, false);
			}
		    }
		    toReparse = parent;
		} else if (LastEdited.recalculateWhenChanged()) {
		    toRecalculate = parent;
		}

		if (parent.hasObservers()) {
		    toUpdateObservers = parent;
		}
		// Update the formula if in XRay mode
		if (FrameGraphics.isXRayMode() && LastEdited.hasFormula()) {
		    LastEdited.setFormula(LastEdited.getText());
		}
	    }
	    if (lastEdited != LastEdited && LastEdited.getText().length() == 0) {
		parent.removeItem(LastEdited);
	    }
	}
	LastEdited = lastEdited;

	if (!FrameGraphics.isXRayMode()) {
	    if (toReparse != null) {
		Parse(toReparse, false, false);
	    } else {
		if (toRecalculate != null) {
		    toRecalculate.recalculate();
		}

		if (toUpdateObservers != null) {
		    toUpdateObservers.notifyObservers(false);
		}
	    }
	}
    }

    /**
     * Extracts files/folders from the assets/resources folder directly into
     * ${PARENT_FOLDER} (~/.expeditee)
     * 
     * @param force if true, resources will be extracted even if they have already been extracted before
     */
    public static void extractResources(boolean force) {
    	File check = new File(FrameIO.PARENT_FOLDER + ".res");
    	if(!force && check.exists()) {
    		return;
    	}
		System.out.println("Extracting/Installing resources:");
		try	{
			check.getParentFile().mkdirs();
	        check.createNewFile();
	        
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			URL docURL = classLoader.getResource("org/expeditee/assets/resources");
			
			// copy files from the jar file to the profile folder
			if (docURL.getProtocol().equals("jar")) {
				JarURLConnection ju_connection=(JarURLConnection)docURL.openConnection();
				JarFile jf =ju_connection.getJarFile();
				Enumeration<JarEntry> jarEntries = jf.entries();
				String res = "org/expeditee/assets/resources/";
				int resLength = res.length();
				
				ZipEntry ze;
				
				while(jarEntries.hasMoreElements()) {
					ze = jarEntries.nextElement();
					if(!ze.getName().startsWith(res)) {
						continue;
					}
					File out = new File(FrameIO.PARENT_FOLDER + ze.getName().substring(resLength));
					// System.out.println("Didn't crash here " + out.getPath());
//					if(out.exists()) {
//						continue;
//					}
					if(ze.isDirectory()) {
						// System.out.println(out.getPath() + " IS DIRECTORY");
						out.mkdirs();
						continue;
					}
					FileOutputStream fOut = null;
					InputStream fIn = null;
					try {
						// System.out.println(out.getPath());
						fOut =  new FileOutputStream(out);
						fIn = classLoader.getResourceAsStream(ze.getName());
						byte[] bBuffer = new byte[1024];
						int nLen;
						while ((nLen = fIn.read(bBuffer)) > 0) {
							fOut.write(bBuffer, 0, nLen);
						}
						fOut.flush();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if(fOut != null) {
							fOut.close();
						}
						if(fIn != null) {
							fIn.close();
						}
					}
				}
				
			// Copy files from the source folder to the profile folder
			} else if (docURL.getProtocol().equals("bundleresource")) {
				final URLConnection urlConnection = docURL.openConnection();
				final Class<?> c = urlConnection.getClass();
				final java.lang.reflect.Method toInvoke = c.getMethod("getFileURL");
				final URL fileURL = (URL)toInvoke.invoke(urlConnection);
				BryceSaysPleaseNameMe(new File(fileURL.getPath()));
			} else {
				File folder = new File(docURL.toURI().getPath());
				BryceSaysPleaseNameMe(folder);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

    private static void BryceSaysPleaseNameMe(File folder) throws IOException {
	LinkedList<File> items = new LinkedList<File>();
	items.addAll(Arrays.asList(folder.listFiles()));
	LinkedList<File> files = new LinkedList<File>();
	String res = "org" + File.separator + "expeditee" + File.separator + "assets" + File.separator + "resources";
	int resLength = res.length();
	
	while (items.size() > 0) {
		File file = items.remove(0);
		if(file.isFile()) {
			if(!file.getName().contains(".svn")) {
				files.add(file);
			}
		} else {
			if (!file.getName().contains(".svn")) {
				items.addAll(Arrays.asList(file.listFiles()));
			}
		}
	}
	for (File file : files) {
		System.out.println(file.getPath());
		File out = new File(FrameIO.PARENT_FOLDER + file.getPath().substring(file.getPath().indexOf(res) + resLength));
//		if(out.exists()) {
//			continue;
//		}
		copyFile(file, out, true);
	}
    }
    
    /**
     * @param src
     * @param dst
     * @throws IOException
     */
    public static void copyFile(File src, File dst, boolean overWrite) throws IOException {
    	if(!overWrite && dst.exists())
    		return;
    	dst.getParentFile().mkdirs();
		FileOutputStream fOut = null;
		FileInputStream fIn = null;
		try {
			// System.out.println(out.getPath());
			fOut = new FileOutputStream(dst);
			fIn = new FileInputStream(src);
			byte[] bBuffer = new byte[1024];
			int nLen;
			while ((nLen = fIn.read(bBuffer)) > 0) {
				fOut.write(bBuffer, 0, nLen);
			}
			fOut.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(fOut != null) {
				fOut.close();
			}
			if(fIn != null) {
				fIn.close();
			}
		}
    }

    public static Text getLastEdited() {
	return LastEdited;
    }

    public static Collection<Text> getCurrentTextItems() {
	Collection<Text> currentTextItems = new LinkedHashSet<Text>();
	Collection<Item> currentItems = getCurrentItems(null);
	if (currentItems != null) {
	    for (Item i : getCurrentItems(null)) {
		if (i instanceof Text && !i.isLineEnd()) {
		    currentTextItems.add((Text) i);
		}
	    }
	}
	return currentTextItems;
    }
}
