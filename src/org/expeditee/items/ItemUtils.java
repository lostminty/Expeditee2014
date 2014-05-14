package org.expeditee.items;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FreeItems;
import org.expeditee.gui.Vector;
import org.expeditee.items.Item.HighlightMode;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.InteractiveWidgetInitialisationFailedException;
import org.expeditee.items.widgets.InteractiveWidgetNotAvailableException;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;
import org.expeditee.network.FrameShare;
import org.expeditee.settings.folders.FolderSettings;

//Static methods that provide functions for the objects\
//mostly to transform values (string -> color etc).

/**
 * Static methods that provide functions for use in Items.
 */
public class ItemUtils {
	// Tag constants
	public static final int TAG_SORT = 0;

	public static final int TAG_JOIN = 1;

	public static final int TAG_INDENT = 2;

	public static final int TAG_OVERLAY = 3;

	public static final int TAG_ACTIVE_OVERLAY = 4;

	public static final int TAG_IMAGE = 5;

	public static final int TAG_ITEM_TEMPLATE = 6;

	public static final int TAG_ANNOTATION_TEMPLATE = 7;

	public static final int TAG_CODE_COMMENT_TEMPLATE = 8;

	public static final int TAG_MENU = 9;

	public static final int TAG_MENU_NEXT = 10;

	public static final int TAG_PARENT = 11;

	public static final int TAG_LITERAL = 12;

	public static final int TAG_FRAME_IMAGE = 13;

	public static final int TAG_BACKUP = 14;

	public static final int TAG_POINTTYPE = 15;

	// Brook: Im claiming this number!
	public static final int TAG_IWIDGET = 16;

	public static final int TAG_DOT_TEMPLATE = 17;

	public static final int TAG_STAT_TEMPLATE = 18;

	public static final int TAG_VECTOR = 19;

	public static final int TAG_ACTIVE_VECTOR = 21;

	public static final int TAG_BITMAP_IMAGE = 20;

	public static final int TAG_MIN = 0;

	public static final int TAG_MAX = 21;

	/**
	 * Determines if the given List of Items contains an Item that is one of the
	 * pre-defined tags.
	 * 
	 * @param items
	 *            The list of Items to search through
	 * @param tag
	 *            The Tag to search for, this should correspond to one of the
	 *            predefined constants in this class
	 * @return True if an Item was found that is the given Tag, False otherwise.
	 */
	public static boolean ContainsTag(Collection<Item> items, int tag) {
		return ContainsTag(items, GetTag(tag));
	}

	public static boolean ContainsTag(Collection<Item> items, String tag) {
		return (FindTag(items, tag) != null);
	}

	public static boolean ContainsExactTag(Collection<Item> items, int tag) {
		return ContainsExactTag(items, GetTag(tag));
	}

	public static boolean ContainsExactTag(Collection<Item> items, String tag) {
		return (FindExactTag(items, tag) != null);
	}

	/**
	 * Searches the given List of Items for an Item that is one of the
	 * pre-defined tags.
	 * 
	 * @param items
	 *            The list of Items to search through
	 * @param tag
	 *            The Tag to search for, this should correspond to one of the
	 *            predefined constants in this class
	 * @return The Item that is the given tag if one is found, or False if none
	 *         is found
	 */
	public static Item FindTag(List<Item> items, int tag) {
		return FindTag(items, GetTag(tag));
	}

	/**
	 * Searches the given List of Items for an Item that is the given tag
	 * 
	 * @param items
	 *            The list of Items to search through
	 * @param toFind
	 *            The Tag to search for, this should include the at (@) symbol
	 * @return The Item that is the given tag if one is found, or False if none
	 *         is found
	 */
	public static Text FindTag(Collection<Item> items, String toFind) {
		for (Item i : items) {
			if (i instanceof Text && i.isAnnotation())
				if (((Text) i).startsWith(toFind))
					return (Text) i;
		}
		return null;
	}

	public static Item FindExactTag(Collection<Item> items, String toFind) {
		for (Item i : items) {
			if (i instanceof Text && i.isAnnotation())
				if (((Text) i).getText().trim().equalsIgnoreCase(toFind))
					return (Item) i;
		}

		return null;
	}

	public static Item FindExactTag(List<Item> items, int tag) {
		return FindExactTag(items, GetTag(tag));
	}

	/**
	 * Determines if the given Item is one of the pre-defined tags in this class
	 * 
	 * @param toCheck
	 *            The Item to check
	 * @param tag
	 *            The tag to check the Item against, this should correspond to
	 *            one of the constants defined in this class
	 * @return True if the Item matches the given tag, false otherwise
	 */
	public static boolean startsWithTag(Item toCheck, int tag) {
		return startsWithTag(toCheck, GetTag(tag));
	}

	public static boolean startsWithTag(Item toCheck, int tag, boolean hasValue) {
		return startsWithTag(toCheck, GetTag(tag), hasValue);
	}

	/**
	 * Checks if the given Item begins with the desired tag (case insensitive).
	 * 
	 * @param toCheck
	 *            The Item to check for the given tag
	 * @param tag
	 *            The tag to check for in the given Item
	 * @param tagOnly
	 *            True if the tag does not have a value
	 * @return True if the tag is found in the given Item, False otherwise.
	 */
	public static boolean startsWithTag(Item toCheck, String tag,
			boolean valueAllowed) {
		if (!(toCheck instanceof Text))
			return false;

		Text txt = (Text) toCheck;
		String value = ItemUtils.StripTag(txt.getText(), tag);

		if (value == null)
			return false;
		return valueAllowed || value.equals("");
	}

	/**
	 * Checks if the item begins with the desired tag.
	 * 
	 * @param toCheck
	 * @param tag
	 * @return
	 */
	public static boolean startsWithTag(Item toCheck, String tag) {
		return startsWithTag(toCheck, tag, true);
	}

	/**
	 * Strips off the given tag from the given String, and returns wathever is
	 * left. Dont put the colon after tags as it is not needed.
	 * 
	 * @param toStrip
	 *            The String to strip the Tag from
	 * @param tag
	 *            The tag to remove from the String
	 * @return The String that results from removing the given Tag from the
	 *         given String, or null if the given String is not the given Tag
	 */
	public static String StripTag(String toStrip, String tag) {
		if (toStrip == null)
			return null;
		toStrip = toStrip.trim();
		if (!toStrip.toLowerCase().startsWith(tag.toLowerCase()))
			return null;

		if (toStrip.length() == tag.length())
			return "";
		// remove tag and ensure the char is the tag separator
		char separator = toStrip.charAt(tag.length());
		if (separator != ':')
			return null;

		if (toStrip.length() == tag.length() + 1)
			return "";

		return toStrip.substring(tag.length() + 1).trim();
	}

	/**
	 * Strips the first character from a string if it is the tag symbol and
	 * returns the remainder.
	 * 
	 * @param toStrip
	 *            the string to be stripped
	 * @return the stripped version of the string.
	 */
	public static String StripTagSymbol(String toStrip) {
		// there must be something left after stripping
		if (toStrip != null) {
			toStrip = toStrip.trim();
			if (toStrip.length() > 0) {
				if (toStrip.charAt(0) == '@') {
					return toStrip.substring(1);
				}
			}
		}
		return toStrip;
	}

	/**
	 * Converts the given int to the String tag. The int should correspond to
	 * one of the constants in this class, if it does not this method will
	 * return null.
	 * 
	 * @param tag
	 *            The int corresponding to the constants in this class of which
	 *            tag to return
	 * @return The String representation of the given Tag, or null if the given
	 *         value does not have a tag associated
	 */
	public static String GetTag(int tag) {
		// TODO refactor so that this uses a map for INT to tags
		switch (tag) {
		case TAG_SORT:
			return "@sort";
		case TAG_JOIN:
			return "@join";
		case TAG_INDENT:
			return "@indent";
		case TAG_OVERLAY:
			return "@o";
		case TAG_VECTOR:
			return "@v";
		case TAG_ACTIVE_VECTOR:
			return "@av";
		case TAG_ACTIVE_OVERLAY:
			return "@ao";
		case TAG_IMAGE:
			return "@i";
		case TAG_ITEM_TEMPLATE:
			return "@itemtemplate";
		case TAG_ANNOTATION_TEMPLATE:
			return "@annotationtemplate";
		case TAG_STAT_TEMPLATE:
			return "@stattemplate";
		case TAG_CODE_COMMENT_TEMPLATE:
			return "@commenttemplate";
		case TAG_MENU:
			return "@menu";
		case TAG_MENU_NEXT:
			return "@nextmenu";
		case TAG_PARENT:
			return "@parent";
		case TAG_LITERAL:
			return "@lit";
		case TAG_FRAME_IMAGE:
			return "@f";
		case TAG_BITMAP_IMAGE:
			return "@b";
		case TAG_BACKUP:
			return "@old";
		case TAG_POINTTYPE:
			return "@pointtype";
		case TAG_IWIDGET:
			return "@iw";
		case TAG_DOT_TEMPLATE:
			return "@dottemplate";
		default:
			return null;
		}
	}

	/**
	 * Creates a picture object from the information stored in the given Text
	 * object. <br>
	 * The paths searched are in the following order:<br>
	 * /images/<br>
	 * the source text as a relative path (from program root folder). <br>
	 * the source text as an absolute path <br>
	 * <br>
	 * If the Image file cannot be found on disk null is returned.
	 * 
	 * @param source
	 *            The Text file containing the Picture infomation
	 * @return The Picture object representing the file, or Null if the file is
	 *         not found.
	 */
	public static Picture CreatePicture(Text source, ImageObserver observer, boolean tryRemote) {
		String text = source.getText();
		String path = "";
		String fileName = "";
		String size = "";

		try {
			// remove @i tag
			text = text.replaceFirst("@i:", "");
			text = text.replaceAll("\n", "");
			text = text.trim();

			int fileSuffixChar = text.indexOf('.');
			if (fileSuffixChar < 0)
				return null;
			int endOfFileName = text.indexOf(' ', fileSuffixChar);
			if (endOfFileName < 0) {
				path = text;
				size = "";
			} else {
				path = text.substring(0, endOfFileName);
				size = text.substring(endOfFileName).trim();
			}
			fileName = path;

			// try images subdirectory
			File file = null;

			for (String dir : FolderSettings.ImageDirs.get()) {
				file = new File(dir + path);
				if (file.exists() && !file.isDirectory())
					break;
			}

			if (file == null || !file.exists() || file.isDirectory())
				file = new File(path);

			// try relative path
			if (!file.exists() || file.isDirectory()) {
				URL picture = new Object().getClass().getResource(path);

				// decode to remove %20 in windows folder names
				if (picture != null) {
					try {
						path = URLDecoder.decode(picture.getFile(), "UTF-8");
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} else
				path = file.getPath();

			// if the image isn't found by now, try remote servers
			file = new File(path);
			if (!file.exists() || file.isDirectory()) {
       			if(tryRemote && FrameShare.getInstance().loadImage(fileName, null)) {
       				// call CreatePicture again, but with tryRemote set to false so we won't get into an infinite loop
       				// if something goes wrong with finding the downloaded image
       				return CreatePicture(source, observer, false);
       			}
				return null;
			}

		} catch (Exception e) {
			return null;
		}

		try {
			Picture pic = new Picture(source, fileName, path, size, observer);

			return pic;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
	
	public static Picture CreatePicture(Text source, ImageObserver observer) {
		return CreatePicture(source, observer, true);
	}

	/**
	 * Creates a deep copy of the given List of Items.
	 * 
	 * @param toCopy
	 *            The list of Items to copy
	 * @return A list containing a copy of all Items in the given List
	 */
	public static List<Item> CopyItems(Collection<Item> toCopy) {
		return CopyItems(toCopy, false, null);
	}

	public static List<Item> CopyItems(Collection<Item> toCopy, Vector v) {
		return CopyItems(toCopy, false, v);
	}

	public static List<Item> CopyItems(Collection<Item> toCopy, boolean extrude) {
		return CopyItems(toCopy, extrude, null);
	}

	public static List<Item> CopyItems(Collection<Item> toCopy,
			boolean extrude, Vector v) {
		// The copies to return
		List<Item> copies = new ArrayList<Item>();

		// list of dots at the end of lines
		Collection<Item> lineEnds = new LinkedHashSet<Item>();
		Collection<Line> lines = new LinkedHashSet<Line>();
		Collection<XRayable> xrayables = new LinkedHashSet<XRayable>();
		Collection<Constraint> constraints = new LinkedHashSet<Constraint>();

		Collection<Item> singles = new LinkedHashSet<Item>();

		Map<Item, Item> lineEndMap = new HashMap<Item, Item>();

		// Widgets are super special
		List<InteractiveWidget> widgets = new ArrayList<InteractiveWidget>();

		for (Item i : toCopy) {
			// Dont copy parts of a vector
			if (i == null || !i.hasPermission(UserAppliedPermission.copy))
				continue;

			// BROOK
			if (i instanceof WidgetCorner) { // dont add these
				if (!widgets.contains(((WidgetCorner) i).getWidgetSource()))
					widgets.add(((WidgetCorner) i).getWidgetSource());
				// BROOK
			} else if (i instanceof WidgetEdge) { // dont add these
				// lines are recreated later
			} else if (i instanceof Line) {
				lines.add((Line) i);
			} else if (i instanceof XRayable) {
				xrayables.add((XRayable) i);
			} else {
				if (i.isLineEnd()) {
					lineEnds.add(i);
					constraints.addAll(i.getConstraints());
				} else {
					singles.add(i);
				}
			}
		}

		// Dont copy the other items that are part of the circle
		for (XRayable x : xrayables) {
			Collection<Item> connected = x.getConnected();
			singles.removeAll(connected);
			lineEnds.removeAll(connected);
			lines.removeAll(connected);
			Item xCopy = x.copy();
			copies.addAll(xCopy.getConnected());
			// Scale items that are from a vector frame
			if (v != null) {
				scaleItem(v, xCopy);
			}
		}

		// copy all single items
		for (Item i : singles) {
			Item copy = i.copy();
			Frame parent = i.getParent();
			if (parent != null) {
				// Items copied from overlay will be anchored onto the current
				// frame
				copy.setParent(parent);
				// if this is the frame name, make sure the frame is saved (in
				// case it is a TDFC frame)
				if (i.isFrameName()) {
					parent.setChanged(true);
				}// check if the item is being copied from a vector
				else if (v != null) {
					// Find the vector this item is from
					assert (v.Frame == parent);
					scaleItem(v, copy);
				}
			}
			copies.add(copy);
		}

		// replace line ends with their copies
		// this is done here so that copied lines can still share end points
		for (Item i : lineEnds) {
			// create a copy of the line end
			Item copy = i.copy();
			copy.removeAllLines();
			copy.removeAllConstraints();

			if (extrude) {
				Frame frame = i.getParentOrCurrentFrame();
				Line newLine = new Line(i, copy, frame.getNextItemID());
				// make sure overlay items are put back on the overlay
				newLine.setParent(frame);
				frame.addItem(newLine);
				copies.add(newLine);
			}
			copies.add(copy);
			lineEndMap.put(i, copy);
			// Scale items that are from a vector frame
			if (v != null) {
				scaleItem(v, copy);
			}
		}

		// recreate lines
		for (Line line : lines) {
			Line lineCopy = line.copy();
			// get the lineEnd we copied above if it is in the MAPPING
			Item originalLineEnd = line.getEndItem();
			Item actualLineEnd = lineEndMap.get(originalLineEnd);
			if (actualLineEnd == null)
				lineCopy.setEndItem(originalLineEnd);
			else
				lineCopy.setEndItem(actualLineEnd);

			Item originalLineStart = line.getStartItem();
			Item actualLineStart = lineEndMap.get(originalLineStart);
			if (actualLineStart == null)
				lineCopy.setStartItem(originalLineStart);
			else
				lineCopy.setStartItem(actualLineStart);

			copies.add(lineCopy);
		}

		// recreate constraints
		for (Constraint c : constraints) {
			Item start = lineEndMap.get(c.getStart());
			Item end = lineEndMap.get(c.getEnd());
			int id = DisplayIO.getCurrentFrame().getNextItemID();
			if (start != null && end != null) {
				new Constraint(start, end, id, c.getType());
			}
		}

		// BROOK
		for (InteractiveWidget iw : widgets) {
			try {

				InteractiveWidget icopy = iw.copy();
				copies.addAll(icopy.getItems());

			} catch (InteractiveWidgetNotAvailableException e) {
				e.printStackTrace();
			} catch (InteractiveWidgetInitialisationFailedException e) {
				e.printStackTrace();
			}

		}

		// Make sure filled rectangles are shown filled on vector overlays
		if (v != null)
			EnclosedCheck(copies);

		return copies;
	}

	/**
	 * Attempts to create a new line that starts from the given Item
	 * ('unreeling'). The Item must already have at least one line, and not be a
	 * line itself to be unreeled from.
	 * 
	 * @param toUnreelFrom
	 *            The Item that will be one end point of the new line
	 * @return A List containing the newly created Item and Line that unreel
	 *         from the given Item, or null if this Item cannot be unreeled
	 *         from.
	 */
	public static List<Item> UnreelLine(Item toUnreelFrom, boolean constrain) {
		// the Item must already have one line to be unreeled from
		if (toUnreelFrom == null || toUnreelFrom.getLines().size() < 1)
			return null;

		List<Item> unreel = new ArrayList<Item>(2);
		unreel.add(toUnreelFrom);
		unreel.addAll(toUnreelFrom.getLines());
		return UnreelLine(unreel, constrain);
	}

	/**
	 * Attempts to create a new line that starts from the given list of Items
	 * ('unreeling'). The List must contain only one non-line Item. The non-line
	 * Item must already have at least one line to be unreeled from.
	 * 
	 * @param toUnreel
	 *            The List containing the Item that will be one end point of the
	 *            new line
	 * @return A List of the newly created Item and Line that unreel from the
	 *         Item in the given List, or null if this List cannot be unreeled
	 *         from.
	 */
	public static List<Item> UnreelLine(List<Item> toUnreel, boolean constrain) {
		Item origEnd = null;
		// find the end being unreeled from
		for (Item item : toUnreel) {
			// we dont want to unreel anything other than lines
			if (item.hasEnclosures()
					|| !(item.isLineEnd() || item instanceof Line)) {
				return null;
			}
			// find the dot to unreel from
			if (item.isLineEnd()) {
				// if there are multiple ends in the list, return
				if (origEnd != null)
					return null;

				origEnd = item;
			}
		}

		// copy the original endpoint
		Item copy = origEnd.copy();
		origEnd.setHighlightMode(HighlightMode.None);
		copy.removeAllLines();
		copy.removeAllConstraints();

		for (Line l : origEnd.getLines()) {
			l.invalidateAll();
		}

		// create a new line
		Frame currentFrame = DisplayIO.getCurrentFrame();
		Line line = new Line(origEnd, copy, currentFrame.getNextItemID());
		// if the previous line was constrained then make the new line
		// constrained if it was a single line
		// TODO add later a diagonal constraint if getLines() == 3 or 4
		Collection<Constraint> constraints = origEnd.getConstraints();
		if (constrain && constraints.size() > 0
				&& origEnd.getLines().size() == 2) {
			Integer type = null;
			for (Constraint c : constraints) {
				if (c.getType() == Constraint.HORIZONTAL) {
					type = Constraint.VERTICAL;
				} else if (c.getType() == Constraint.VERTICAL) {
					type = Constraint.HORIZONTAL;
				}
				if (c.getType() == Constraint.DIAGONAL_NEG) {
					type = Constraint.DIAGONAL_POS;
				} else if (c.getType() == Constraint.DIAGONAL_POS) {
					type = Constraint.DIAGONAL_NEG;
				}
			}
			if (type != null) {
				new Constraint(origEnd, copy, currentFrame.getNextItemID(),
						type);
			}
		}

		// copy.setFloating(true);
		origEnd.setArrowheadLength(0);
		// copy.setArrowheadLength(0);

		List<Item> toReturn = new LinkedList<Item>();
		toReturn.add(copy);
		toReturn.add(line);
		return toReturn;
	}

	public static void New() {
		EnclosedCheck(DisplayIO.getCurrentFrame().getItems());
	}

	public static void Old() {
		OldEnclosedCheck(DisplayIO.getCurrentFrame().getItems());
	}

	/**
	 * Updates the connectedToAnnotation flags for all items
	 */
	public static void UpdateConnectedToAnnotations(Collection<Item> items) {
		// get all lineEnds on the Frame
		Collection<Item> lineEnds = new LinkedHashSet<Item>();
		for (Item i : items) {
			i.setConnectedToAnnotation(false);
			if (i.isLineEnd()) {
				lineEnds.add(i);
			}
		}

		// if there are no line endpoints on the Frame, then there can't be an
		// enclosure
		if (lineEnds.size() == 0)
			return;

		// Now find go through line ends and see if any are annotation items
		while (lineEnds.size() > 0) {
			Item item = lineEnds.iterator().next();
			// If its an annotation item then set the flag for all its connected
			// items
			if (item.isAnnotation()) {
				Collection<Item> connected = item.getAllConnected();
				for (Item i : connected)
					i.setConnectedToAnnotation(true);
				lineEnds.removeAll(connected);
			}
			lineEnds.remove(item);
		}
	}

	/**
	 * Checks through all Lines and Dots on the current Frame to detect if any
	 * form an enclosure, which can then be used to manipulate items within the
	 * polygon. If an enclosure is found, then the dots will have their
	 * enclosure value set to true, and a List is created that contains all the
	 * Dots in the order they were processed. Actual calculation of the Polygon
	 * is done dynamically (to account for Dots being moved).
	 */
	public static void EnclosedCheck(Collection<Item> items) {
		// get all lineEnds on the Frame
		List<Item> lineEnds = new LinkedList<Item>();
		for (Item i : items) {
			if (i.isLineEnd()) {
				i.setEnclosedList(null);
				// Add line ends joined to 2 other lines
				if (i.getLines().size() == 2)
					lineEnds.add(i);
			}
		}

		// if there are no line endpoints on the Frame, then there can't be an
		// enclosure
		if (lineEnds.size() == 0)
			return;

		// New approach
		while (lineEnds.size() > 0) {
			Item item = lineEnds.get(0);
			// Get the lineEnds connected to this item
			Collection<Item> connected = item.getAllConnected();
			Collection<Item> connectedLineEnds = new LinkedHashSet<Item>();
			for (Item itemToCheck : connected) {
				if (itemToCheck.isLineEnd())
					connectedLineEnds.add(itemToCheck);
			}
			// Check that all the line ends are in our lineEnds list
			int oldSize = lineEnds.size();
			// Remove all the items from our line ends list
			lineEnds.removeAll(connectedLineEnds);
			int newSize = lineEnds.size();
			int connectedSize = connectedLineEnds.size();
			// Check if all the connectedItems were in the lineEnds collection
			if (oldSize == newSize + connectedSize) {
				// Set them to be the enclosed list for each of the items
				for (Item enclosedLineEnd : connectedLineEnds) {
					enclosedLineEnd.setEnclosedList(connectedLineEnds);
				}
			}
		}
	}

	/**
	 * Checks through all Lines and Dots on the current Frame to detect if any
	 * form an enclosure, which can then be used to manipulate items within the
	 * polygon. If an enclosure is found, then the dots will have their
	 * enclosure value set to true, and a List is created that contains all the
	 * Dots in the order they were processed. Actual calculation of the Polygon
	 * is done dynamically (to account for Dots being moved).
	 */
	public static void OldEnclosedCheck(Collection<Item> items) {
		_seen.clear();

		// get all lineEnds on the Frame
		List<Item> lineEnds = new ArrayList<Item>(0);
		for (Item i : items) {
			if (i.isLineEnd()) {
				i.setEnclosedList(null);

				if (i.getLines().size() == 2)
					lineEnds.add(i);
			}
		}

		// if there are no line endpoints on the Frame, then there can't be an
		// enclosure
		if (lineEnds.size() == 0)
			return;

		// TODO optimise this code!!
		// iterate through all the lineEnds
		for (Item searchFor : lineEnds) {
			_seen.clear();

			for (Line l : searchFor.getLines()) {
				_seen.add(l);
				if (traverse(searchFor, l.getOppositeEnd(searchFor))) {
					_path.add(l.getOppositeEnd(searchFor));

					for (Item i : _path)
						i.setEnclosedList(_path);

					_path = new ArrayList<Item>(0);

					break;
				}
			}
		}
	}

	private static List<Line> _seen = new ArrayList<Line>();

	private static List<Item> _path = new ArrayList<Item>();

	private static boolean traverse(Item toFind, Item searchFrom) {
		if (toFind == null || searchFrom == null || !searchFrom.isLineEnd())
			return false;

		if (searchFrom.getLines().size() != 2)
			return false;

		if (toFind == searchFrom)
			return true;

		for (Line l : searchFrom.getLines()) {
			if (!(_seen.contains(l))) {
				_seen.add(l);
				if (traverse(toFind, l.getOppositeEnd(searchFrom))) {
					_path.add(l.getOppositeEnd(searchFrom));
					return true;
				}
			}

		}

		return false;
	}

	/**
	 * Determines if an item is visible from a the current frame(s). If the item
	 * is free then it is considered visible.
	 * 
	 * @param i
	 *            The item to check
	 * @return True if visible/free from given frame.
	 */
	public static boolean isVisible(Item i) {
		if (DisplayIO.isTwinFramesOn()) {
			if (!isVisible(DisplayIO.getFrames()[0], i)) {
				return isVisible(DisplayIO.getFrames()[1], i);
			} else {
				return true;
			}
		} else {
			return isVisible(DisplayIO.getCurrentFrame(), i);
		}
	}

	/**
	 * Determines if an item is visible from a given frame. If the item is free
	 * then it is considered visible.
	 * 
	 * @param fromFrame
	 *            The frame to check from.
	 * @param i
	 *            The item to check
	 * @return True if visible/free from given frame.
	 */
	public static boolean isVisible(Frame fromFrame, Item i) {
		if (fromFrame == null)
			return false;

		Frame parent = i.getParent();

		if (parent == fromFrame)
			return true;
		
		else if (parent == null)
			return FreeItems.getInstance().contains(i)
					|| FreeItems.getCursor().contains(i);

		return fromFrame.getAllItems().contains(i) && i.isVisible();
	}

	public static Rectangle expandRectangle(Rectangle r, int n) {
		return new Rectangle(r.x - (n >> 1), r.y - (n >> 1), r.width + n,
				r.height + n);
	}

	/*
	 * FrameMouseActions while (!copies.isEmpty()) { Iterator<Item> iterator =
	 * copies.iterator(); Item item = iterator.next(); // Dont paint annotation
	 * items for @v if (!item.isVisible() || item.isAnnotation()) {
	 * iterator.remove(); continue; }
	 * 
	 * if (!(item instanceof Line)) { item.setThickness(item.getThickness() *
	 * scale); if (item instanceof XRayable || item.hasEnclosures()) {
	 * scaleItem(scale, defaultForeground, defaultBackground, origin.x,
	 * origin.y, item); items.add(item); copies.remove(item); } else {
	 * Collection<Item> connected = item.getAllConnected(); // Get all the
	 * connected items because we can only set the // thickness ONCE for (Item i :
	 * connected) { scaleItem(scale, defaultForeground, defaultBackground,
	 * origin.x, origin.y, i); } items.addAll(connected);
	 * copies.removeAll(connected); } } else { iterator.remove(); } }
	 */

	/**
	 * @param scale
	 * @param defaultForeground
	 * @param defaultBackground
	 * @param originX
	 * @param originY
	 * @param item
	 */
	private static void scaleItem(Vector v, Item item) {
		Float scale = v.Scale;
		int originX = v.Origin.x;
		int originY = v.Origin.y;
		Color defaultForeground = v.Foreground;
		Color defaultBackground = v.Background;
		UserAppliedPermission permission = v.permission;
		// TODO should this be checking if the frame has the
		// same permissions as the vector
		// and if so don't set the item's permissions?
		item.setOverlayPermission(permission);

		// TODO encapsulate this somewhere inside of circle class!
		// if(item instanceof Circle){
		// scaleItem(v, ((Circle)item).getCenter());
		// }

		if (!(item instanceof Line)) {
			if (item.getColor() == null) {
				item.setColor(defaultForeground);
			}
			if (item.getBackgroundColor() == null) {
				item.setBackgroundColor(defaultBackground);
			}
			if (item.getFillColor() == null) {
				item.setFillColor(defaultBackground);
			}

			if (permission.equals(UserAppliedPermission.none)) {
				item.setLinkMark(false);
				item.setActionMark(false);
			}

			item.scale(scale, originX, originY);
		}
	}

	/**
	 * Extracts widgets from an item list.
	 * 
	 * @param items
	 *            Items to extract from. Must not be null.
	 * 
	 * @return List of (unique)widgets in items. Never null.
	 */
	public static List<InteractiveWidget> extractWidgets(List<Item> items) {
		assert (items != null);

		List<InteractiveWidget> iWidgets = new LinkedList<InteractiveWidget>();

		for (Item i : items) {
			if (i instanceof WidgetEdge) {
				WidgetEdge we = (WidgetEdge) i;
				if (!iWidgets.contains(we.getWidgetSource()))
					iWidgets.add(we.getWidgetSource());
			} else if (i instanceof WidgetCorner) {
				WidgetCorner wc = (WidgetCorner) i;
				if (!iWidgets.contains(wc.getWidgetSource()))
					iWidgets.add(wc.getWidgetSource());
			}
		}

		return iWidgets;
	}
}
