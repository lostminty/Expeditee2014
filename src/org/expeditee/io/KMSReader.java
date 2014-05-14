package org.expeditee.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.expeditee.gui.Frame;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Constraint;
import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Text;
import org.expeditee.stats.SessionStats;

/**
 * Reads in KMS format files and constructs the Frame and Item objects they
 * contain.
 * 
 * @author jdm18
 * 
 */
public class KMSReader extends DefaultFrameReader {

	private BufferedReader _reader = null;

	private static LinkedHashMap<String, Method> _ItemTags = null;

	private static LinkedHashMap<String, Method> _FrameTags = null;

	/**
	 * Does nothing, location must be set before use
	 */
	public KMSReader() {
		if (_ItemTags != null && _FrameTags != null)
			return;

		_ItemTags = new LinkedHashMap<String, Method>();
		_FrameTags = new LinkedHashMap<String, Method>();

		try {
			_FrameTags.put("A", Frame.class.getMethod("setName", pString));
			_FrameTags.put("V", Frame.class.getMethod("setVersion", pInt));
			_FrameTags
					.put("p", Frame.class.getMethod("setPermission", pPermission));
			_FrameTags.put("U", Frame.class.getMethod("setOwner", pString));
			_FrameTags.put("D", Frame.class
					.getMethod("setDateCreated", pString));
			_FrameTags.put("M", Frame.class.getMethod("setLastModifyUser",
					pString));
			_FrameTags.put("d", Frame.class.getMethod("setLastModifyDate",
					pString));
			_FrameTags
					.put("F", Frame.class.getMethod("setFrozenDate", pString));

			_FrameTags.put("O", Frame.class.getMethod("setForegroundColor",
					pColor));
			_FrameTags.put("B", Frame.class.getMethod("setBackgroundColor",
					pColor));

			_ItemTags.put("S", Item.class.getMethod("setID", pInt));
			_ItemTags.put("s", Item.class.getMethod("setDateCreated", pString));
			_ItemTags.put("d", Item.class.getMethod("setColor", pColor));
			_ItemTags.put("G", Item.class.getMethod("setBackgroundColor",
					pColor));
			_ItemTags.put("P", Item.class.getMethod("setPosition", pPoint));
			_ItemTags.put("F", Item.class.getMethod("setLink", pString));
			_ItemTags.put("J", Item.class.getMethod("setFormula", pString));
			
			_ItemTags.put("X", Item.class.getMethod("setActions", pList));
			_ItemTags.put("x", Item.class.getMethod("setActionMark", pBool));
			_ItemTags.put("U", Item.class.getMethod("setActionCursorEnter",
					pList));
			_ItemTags.put("V", Item.class.getMethod("setActionCursorLeave",
					pList));
			_ItemTags.put("W", Item.class.getMethod("setActionEnterFrame",
					pList));
			_ItemTags.put("Y", Item.class.getMethod("setActionLeaveFrame",
					pList));
			_ItemTags.put("D", Item.class.getMethod("setData", pList));
			_ItemTags.put("u", Item.class.getMethod("setHighlight", pBool));
			_ItemTags.put("e", Item.class.getMethod("setFillColor", pColor));
			_ItemTags.put("E", Item.class.getMethod("setGradientColor", pColor));
			_ItemTags.put("Q", Item.class.getMethod("setGradientAngle", pInt));
			
			_ItemTags.put("i", Item.class.getMethod("setFillPattern", pString));
			_ItemTags.put("o", Item.class.getMethod("setOwner", pString));
			_ItemTags.put("n", Item.class.getMethod("setLinkMark", pBool));
			_ItemTags
					.put("q", Item.class.getMethod("setLinkFrameset", pString));
			_ItemTags
					.put("y", Item.class.getMethod("setLinkTemplate", pString));
			_ItemTags.put("g", Item.class
					.getMethod("setLinePattern", pIntArray));

			_ItemTags.put("j", Item.class.getMethod("setArrow", pArrow));

			_ItemTags.put("f", Text.class.getMethod("setFont", pFont));
			_ItemTags.put("t", Text.class.getMethod("setSpacing", pFloat));
			_ItemTags.put("T", Text.class.getMethod("appendText", pString));

			_ItemTags.put("a", Text.class.getMethod("setWordSpacing", pInt));
			_ItemTags.put("b", Text.class.getMethod("setLetterSpacing", pFloat));
			_ItemTags.put("m", Text.class.getMethod("setInitialSpacing", pFloat));
			_ItemTags.put("w", Text.class.getMethod("setWidth", pIntO));
			_ItemTags.put("k", Text.class.getMethod("setJustification", pJustification));

			_ItemTags.put("h", Item.class.getMethod("setThickness", pFloat));
			_ItemTags.put("l", Item.class.getMethod("setLineIDs", pString));
			_ItemTags.put("c", Item.class
					.getMethod("setConstraintIDs", pString));

			// Lines and constraints are created differently
			_ItemTags.put("L", Line.class.getMethod("setStartItem", pItem));
			// _ItemTags.put("g", null);

			_ItemTags.put("C", Constraint.class.getMethod("getID",
					(Class[]) null));
			// _ItemTags.put("s2", null);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Determines whether a string begins with a KMS tag.
	 * 
	 * @param s
	 *            a line of text
	 * @return true if s begins with a KMS tag
	 */
	private static boolean isValidLine(String s) {
		return s.length() >= 3 && s.charAt(0) == '+' && s.charAt(2) == '+'
				&& Character.isLetter(s.charAt(1));
	}

	/**
	 * Reads a KMS file with the given name from disk.
	 * 
	 * @param frameName
	 *            the name of the Frame to read in from a file.
	 * @return A new Frame object that contains any Items described in the file.
	 * @throws IOException
	 *             Any exceptions occured by the BufferedReader.
	 */
	public Frame readFrame(BufferedReader reader) throws IOException {
		_reader = reader;
		Frame newFrame = null;
		String next = "";
		boolean header = true;
		// clear hashmap and line list
		_data.clear();
		_linePoints.clear();
		// First read the header
		try {
			while (_reader.ready()) {
				next = _reader.readLine();
				if (isValidLine(next)) {
					if (header) {
						// Create the frame when the first +S+ is reached
						// And set the flag to indicate the end of the header
						if (getTag(next).equals("S")) {
							newFrame = readFrameClass();
							header = false;
							_data.clear();
							processBodyLine(newFrame, next);
						} else
							processHeaderLine(next);
					} else
						processBodyLine(newFrame, next);
				} else if (next.startsWith(SessionStats.ACTIVE_TIME_ATTRIBUTE)) {
					try {
						String value = next.substring(SessionStats.ACTIVE_TIME_ATTRIBUTE.length()).trim();
						newFrame.setActiveTime(value);
					} catch (Exception e) {
					}

				} else if (next.startsWith(SessionStats.DARK_TIME_ATTRIBUTE)) {
					try {
						String value = next.substring(SessionStats.DARK_TIME_ATTRIBUTE.length()).trim();
						newFrame.setDarkTime(value);
					} catch (Exception e) {
					}

				}
			}
		} catch (Exception e) {
			System.out.println("Error reading bodyLine: " + next + " "
					+ e.getMessage());
			e.printStackTrace();
		}

		// if the frame contains no items (Default.0 does this)
		if (newFrame == null)
			newFrame = readFrameClass();

		_reader.close();
		if (newFrame != null) {
			//newFrame.refreshItemPermissions();
			newFrame.setChanged(false);
		}
		return newFrame;
	}

	// stores the data of the Item to be created next
	private LinkedHashMap<String, String> _data = new LinkedHashMap<String, String>(
			20);

	// creates an item from the last batch of data read from the file
	private Item createItem() {
		// creates an item based on the information in the hashmap
		try {
			return readItemClass();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	// Stores points used when constructing lines
	private HashMap<Integer, Item> _linePoints = new HashMap<Integer, Item>();

	/**
	 * Adds any lines stored in _lineStarts and _lineEnds to the given Frame.
	 * 
	 * @param frame
	 *            The Frame to add the newly created Line objects to.
	 */
	private Line createLine() {
		String s = _data.get("L");
		_data.remove("L");

		// get the line ID and type
		java.awt.Point idtype = separateValues(s);

		// get the end points
		s = _data.get("s");
		_data.remove("s");

		java.awt.Point startend = separateValues(s);

		int start = startend.x;
		int end = startend.y;

		if (_linePoints.get(start) != null && _linePoints.get(end) != null) {
			Line line = new Line(_linePoints.get(start), _linePoints.get(end),
					idtype.x);
			return line;
		}

		// System.out.println(_linePoints.size());
		// for (int i : _linePoints.keySet())
		// System.out.println(i + " - "
		// + _linePoints.get(i).getClass().getSimpleName());
		//
		// System.out.println("Error: Line " + idtype.x
		// + " has missing end point(s) + (Looking for: " + start + " , "
		// + end + ")");
		return null;
	}

	/*
	 * Creates a constraint from the data containted in _data
	 */
	private void createConstraint() {
		java.awt.Point idtype = separateValues(_data.get("C"));
		java.awt.Point startend = separateValues(_data.get("s"));

		Item a = _linePoints.get(startend.x);
		Item b = _linePoints.get(startend.y);

		new Constraint(a, b, idtype.x, idtype.y);
	}

	/**
	 * Processes the body section of the KMS file, which contains all Items
	 * 
	 * @param frame
	 *            The Frame to add any created Items to.
	 * @param line
	 *            The line of text read in from the file to process.
	 */
	private void processBodyLine(Frame frame, String line) {
		// separate the tag from the value
		String tag = getTag(line);
		line = getValue(line);

		// if this is the start of a new item, a line, or a constraint
		// then add the last item to the frame
		if (tag.charAt(0) == 'S' || tag.charAt(0) == 'L'
				|| tag.charAt(0) == 'C') {
			frame.addItem(createItem());

			_data.clear();
			_data.put(tag, line);
			return;
		}

		// if this is the end of the file, then add the last item to the frame
		if (tag.charAt(0) == 'Z') {
			frame.addItem(createItem());
			_data.clear();

			return;
		}

		// check for duplicate tags (multiple lines of text)
		String newtag = tag;
		int i = 0;

		// only executes if there are duplicate tags for this Item
		while (_data.containsKey(newtag)) {
			newtag = tag + i;
			i++;
		}

		// All values are stored in a HashMap until the end of the Item is
		// found.
		_data.put(newtag, line);
	}

	private static String getTag(String line) {
		assert (line.charAt(0) == '+');
		assert (line.length() > 2);
		return line.substring(1, 2);
	}

	private static String getValue(String line) {
		assert (line.charAt(0) == '+');
		if (line.length() > 4)
			return line.substring(4);
		else
			return "";
	}

	/**
	 * Reads the header section of the file, which contains information about
	 * the Frame.
	 * 
	 * @param frame
	 *            The Frame to assign the read values to.
	 * @param line
	 *            The line from the file to process.
	 * @return False if the end of the header has been reached, True otherwise.
	 */
	private void processHeaderLine(String line) throws IOException {
		// first separate the tag from the text
		String tag = getTag(line);
		String value = getValue(line);

		if (tag.equals("Z"))
			return;

		if (_FrameTags.get(tag) == null) {
			if (!tag.equals("t") && !tag.equals("v"))
				MessageBay.errorMessage("Tag '" + tag + "' in '" + line
						+ "' is not supported.");
			return;
		}

		_data.put(tag, value);
	}

	// returns two ints separated by a space from the given String
	private java.awt.Point separateValues(String line) {
		int x = Integer.parseInt(line.substring(0, line.indexOf(" ")));
		int y = Integer.parseInt(line.substring(line.indexOf(" ") + 1));

		return new java.awt.Point(x, y);
	}

	private Frame readFrameClass() throws IOException {
		if (_data.size() == 0) {
			MessageBay
					.errorMessage("IO Error: File contains no valid KMS lines.");
			return null;
		}

		Frame toMake = new Frame();
		Iterator<String> it = _data.keySet().iterator();

		while (it.hasNext()) {
			String datatag = it.next();

			Method toRun = _FrameTags.get(datatag);
			Object[] vals = Conversion.Convert(toRun, _data.get(datatag));
			try {
				toRun.invoke(toMake, vals);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return toMake;
	}

	@SuppressWarnings("unchecked")
	private Item readItemClass() throws IOException {
		Iterator<String> tags = _data.keySet().iterator();
		Class toCreate = null;

		// loop through all attributes read in for this item
		while (tags.hasNext()) {
			String next = tags.next();
			Method m = _ItemTags.get(next);
			if (m != null && _ItemTags.get(next + "2") == null) {
				if (!m.getDeclaringClass().getSimpleName().equals("Item")) {
					toCreate = m.getDeclaringClass();
					break;
				}
			}
		}

		if (toCreate == null) {
			if (_data.size() > 0)
				toCreate = Dot.class;
			else
				return null;
		}

		Item toMake = null;

		try {
			if (toCreate == Line.class)
				toMake = createLine();
			else if (toCreate == Constraint.class) {
				createConstraint();
				return null;
			} else if (toCreate == Item.class && _data.size() == 2) {
				toCreate = Dot.class;
			} else if (toCreate == Item.class) {
				for (String s : _data.keySet())
					System.out.println(s + " -> " + _data.get(s));
				return null;
			} else {
				Object[] params = { -1 };
				Constructor con = toCreate.getConstructor(int.class);
				toMake = (Item) con.newInstance(params);
			}
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (toMake == null)
			return null;

		Iterator<String> it = _data.keySet().iterator();
		String last = "";

		int count = 0;

		while (it.hasNext()) {
			String tag = it.next();
			if (_data.containsKey("" + tag.charAt(0) + count)) {
				if (last.length() == 0)
					last = _data.get(tag);
				else
					last += "\n" + _data.get(tag);

				count++;
			} else {
				Method toRun = _ItemTags.get("" + tag.charAt(0));
				if (toRun == null){
					System.out.println("Error accessing tag method: "
							+ tag.charAt(0));
					
				}
				Object[] vals;
				if (last.length() > 0)
					vals = Conversion.Convert(toRun, last + "\n"
							+ _data.get(tag));
				else
					vals = Conversion.Convert(toRun, _data.get(tag));

				try {
					if (vals != null) {
						toRun.invoke(toMake, vals);
					}
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassCastException e) {
					System.out.println(toRun.getName());
					e.printStackTrace();
				}

				count = 0;
				last = "";
			}

		}

		if (!(toMake instanceof Line))
			_linePoints.put(toMake.getID(), toMake);

		return toMake;
	}

	public static int getVersion(String fullpath) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fullpath));
			String next = "";
			// First read the header lines until we get the version number
			while (reader.ready() && (next = reader.readLine()) != null) {
				if (isValidLine(next)) {
					char tag = getTag(next).charAt(0);
					String value = getValue(next);
					if (tag == 'V')
						return Integer.parseInt(value);
					else if (tag == 'Z')
						return 0;
				}
			}
		} catch (Exception e) {
		}
		return -1;
	}
}
