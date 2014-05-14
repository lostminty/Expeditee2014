package org.expeditee.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.io.Conversion;
import org.expeditee.items.DotType;
import org.expeditee.items.Item;
import org.expeditee.items.Justification;
import org.expeditee.items.PermissionPair;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.simple.IncorrectTypeException;

/**
 * This class provides the methods to extract and set attributes of Items and
 * Frames. These methods are called when a user merges a text item with
 * <code>Attribute: Value</code> pairs.
 * 
 * @author jdm18
 * 
 */
public class AttributeUtils {

	public static final class Attribute {
		public final String displayName;
		public final Method getter;
		public final Method setter;
		
		public Attribute(String displayName, Method getter, Method setter) {
			this.displayName = displayName;
			this.getter = getter;
			this.setter = setter;
		}
	}
	
	public static final class AttributeSet {
		
		// the internal hashmap
		private final HashMap<String, Attribute> map;
		// a list of keys in the order they were added (used to make attribute extraction consistent)
		public final List<String> keys;
		
		public AttributeSet(int size) {
			map = new HashMap<String, Attribute>(size);
			keys = new LinkedList<String>();
		}
		
		public void put(String attributeName, Method getter, Method setter) {
			if(map.containsKey(attributeName.toLowerCase())) {
				System.err.println(this + " already contains key '" + attributeName + "', overwriting value!");
			} else {
				// we keep an ordered list of attributes for extraction
				keys.add(attributeName.toLowerCase());
			}
			map.put(attributeName.toLowerCase(), new Attribute(attributeName, getter, setter));
		}
		
		// Create a second reference the the same Attribute, using a different name
		// Does not modify the list of keys
		public void alias(String alias, String name) {
			if(map.containsKey(name.toLowerCase())) {
				map.put(alias.toLowerCase(), map.get(name.toLowerCase()));
			} else {
				System.err.println("Cannot add alias '" + alias + "', because key '" + name + "' does not exist!");
			}
		}
		
		public boolean containsKey(String key) {
			return map.containsKey(key);
		}
		
		public Attribute get(String key) {
			return map.get(key);
		}
	}
	
	public static final AttributeSet _Attrib = new AttributeSet(128);
	public static final AttributeSet _FrameAttrib = new AttributeSet(16);


	// List of attributes which are ignored when extracting attributes
	private static List<String> _IgnoreGet = null;
	// List of attributes which are ignored when setting attributes,
	// if multiple attributes are being set at once
	private static List<String> _IgnoreSet = null;

	/***************************************************************************
	 * List of method names to show in extraced lists even when they return null
	 * (Null is often used to indicate the default value is used)
	 **************************************************************************/
	private static List<Method> _AllowNull = null;

	// private static HashMap<String, String> _Abbreviations = null;
	
	public static void ensureReady() {
		if(_IgnoreSet == null) {
			initLists();
		}
	}

	/**
	 * Initialises the _Ignore and _AllowNull lists.
	 */
	private static void initLists() {

		try {
			
			Class<?>[] pPoint = { Point.class };
			Class<?>[] pString = { String.class };
			Class<?>[] pInt = { int.class };
			Class<?>[] pIntO = { Integer.class };
			Class<?>[] pFloat = { float.class };
			Class<?>[] pFloatO = { Float.class };
			Class<?>[] pColor = { Color.class };
			Class<?>[] pBool = { boolean.class };
			//Class[] pDouble = { double.class };
			//Class[] pDoubleO = { Double.class };
			Class<?>[] pArrow = { float.class, double.class, double.class };
			Class<?>[] pList = { List.class };
			Class<?>[] pIntArray = { int[].class };
			Class<?>[] pJustification = { Justification.class };
			Class<?>[] pPermission = { PermissionPair.class };
			Class<?>[] pDotType = { DotType.class };
			
			_IgnoreSet = new LinkedList<String>();
			_IgnoreGet = new LinkedList<String>();
			_AllowNull = new LinkedList<Method>();
			
			// TODO load these in with reflection...
			// Set the shortcuts with annotation tags on the methods
			_IgnoreSet.add("date");
			_IgnoreSet.add("datecreated");
			_IgnoreSet.add("d");
			_IgnoreSet.add("link");
			_IgnoreSet.add("l");
			_IgnoreSet.add("action");
			_IgnoreSet.add("a");
			_IgnoreSet.add("position");
			_IgnoreSet.add("pos");
			_IgnoreSet.add("p");
			_IgnoreSet.add("x");
			_IgnoreSet.add("y");
			
			_IgnoreGet.add("x");
			_IgnoreGet.add("y");
			_IgnoreGet.add("text");
			_IgnoreGet.add("gradientangle");

			_AllowNull.add(Item.class.getMethod("getColor"));
			_AllowNull.add(Item.class.getMethod("getBackgroundColor"));

			_AllowNull.add(Frame.class.getMethod("getBackgroundColor"));
			_AllowNull.add(Frame.class.getMethod("getForegroundColor"));

			/*
			 * Populate the backing lists of attributes
			 */
			
			// Frames
			_FrameAttrib.put("Permission",      Frame.class.getMethod("getPermission"),
			                                    Frame.class.getMethod("setPermission", pPermission));
			_FrameAttrib.put("Owner",           Frame.class.getMethod("getOwner"),
			                                    Frame.class.getMethod("setOwner", pString));
			_FrameAttrib.put("DateCreated",     Frame.class.getMethod("getDateCreated"),
			                                    null);
			_FrameAttrib.put("LastModifyUser",  Frame.class.getMethod("getLastModifyUser"),
			                                    null);
			_FrameAttrib.put("LastModifyDate",  Frame.class.getMethod("getLastModifyDate"),
			                                    null);
			_FrameAttrib.put("ForegroundColor", Frame.class.getMethod("getForegroundColor"),
			                                    Frame.class.getMethod("setForegroundColor", pColor));
			_FrameAttrib.put("BackgroundColor", Frame.class.getMethod("getBackgroundColor"),
			                                    Frame.class.getMethod("setBackgroundColor", pColor));
			
			// aliases for attribute setting
			_FrameAttrib.alias("fgc",        "foregroundcolor");
			_FrameAttrib.alias("bgc",        "backgroundcolor");
			_FrameAttrib.alias("p",          "permission");
			
			
			// Generic Items
			_Attrib.put("DateCreated",          Item.class.getMethod("getDateCreated"),
			                                    Item.class.getMethod("setDateCreated", pString));
			_Attrib.put("Color",                Item.class.getMethod("getColor"),
			                                    Item.class.getMethod("setColor", pColor));
			_Attrib.put("BackgroundColor",      Item.class.getMethod("getBackgroundColor"),
			                                    Item.class.getMethod("setBackgroundColor", pColor));
			_Attrib.put("BorderColor",          Item.class.getMethod("getBorderColor"),
			                                    Item.class.getMethod("setBorderColor", pColor));
			_Attrib.put("AnchorLeft",           Item.class.getMethod("getAnchorLeft"),
			                                    Item.class.getMethod("setAnchorLeft", pFloatO));
			_Attrib.put("AnchorRight",          Item.class.getMethod("getAnchorRight"),
			                                    Item.class.getMethod("setAnchorRight", pFloatO));
			_Attrib.put("AnchorTop",            Item.class.getMethod("getAnchorTop"),
			                                    Item.class.getMethod("setAnchorTop", pFloatO));
			_Attrib.put("AnchorBottom",         Item.class.getMethod("getAnchorBottom"),
			                                    Item.class.getMethod("setAnchorBottom", pFloatO));
			_Attrib.put("Position",             Item.class.getMethod("getPosition"),
			                                    Item.class.getMethod("setPosition", pPoint));
			_Attrib.put("Link",                 Item.class.getMethod("getLink"),
			                                    Item.class.getMethod("setLink", pString));
			_Attrib.put("AddToHistory",         Item.class.getMethod("getLinkHistory"),
			                                    Item.class.getMethod("setLinkHistory", pBool));
			_Attrib.put("Action",               Item.class.getMethod("getAction"),
			                                    Item.class.getMethod("setActions", pList));
			_Attrib.put("ActionMark",           Item.class.getMethod("getActionMark"),
			                                    Item.class.getMethod("setActionMark", pBool));
			_Attrib.put("ActionCursorEnter",    Item.class.getMethod("getActionCursorEnter"),
			                                    Item.class.getMethod("setActionCursorEnter", pList));
			_Attrib.put("ActionCursorLeave",    Item.class.getMethod("getActionCursorLeave"),
			                                    Item.class.getMethod("setActionCursorLeave", pList));
			_Attrib.put("ActionEnterFrame",     Item.class.getMethod("getActionEnterFrame"),
			                                    Item.class.getMethod("setActionEnterFrame", pList));
			_Attrib.put("ActionLeaveFrame",     Item.class.getMethod("getActionLeaveFrame"),
			                                    Item.class.getMethod("setActionLeaveFrame", pList));
			_Attrib.put("Data",                 Item.class.getMethod("getData"),
			                                    Item.class.getMethod("setData", pList));
			_Attrib.put("Highlight",            Item.class.getMethod("getHighlight"),
			                                    Item.class.getMethod("setHighlight", pBool));
			_Attrib.put("FillColor",            Item.class.getMethod("getFillColor"),
			                                    Item.class.getMethod("setFillColor", pColor));
			_Attrib.put("GradientColor",        Item.class.getMethod("getGradientColor"),
			                                    Item.class.getMethod("setGradientColor", pColor));
			_Attrib.put("GradientAngle",        Item.class.getMethod("getGradientAngle"),
			                                    Item.class.getMethod("setGradientAngle", pInt));
			_Attrib.put("FillPattern",          Item.class.getMethod("getFillPattern"),
			                                    Item.class.getMethod("setFillPattern", pString));
			_Attrib.put("Owner",                Item.class.getMethod("getOwner"),
			                                    Item.class.getMethod("setOwner", pString));
			_Attrib.put("LinkMark",             Item.class.getMethod("getLinkMark"),
			                                    Item.class.getMethod("setLinkMark", pBool));
			_Attrib.put("LinkFrameset",         Item.class.getMethod("getLinkFrameset"),
			                                    Item.class.getMethod("setLinkFrameset", pString));
			_Attrib.put("LinkTemplate",         Item.class.getMethod("getLinkTemplate"),
			                                    Item.class.getMethod("setLinkTemplate", pString));
			_Attrib.put("LinePattern",          Item.class.getMethod("getLinePattern"),
			                                    Item.class.getMethod("setLinePattern", pIntArray));
			_Attrib.put("Arrow",                Item.class.getMethod("getArrow"),
			                                    Item.class.getMethod("setArrow", pArrow));
			_Attrib.put("DotType",              Item.class.getMethod("getDotType"),
			                                    Item.class.getMethod("setDotType", pDotType));
			_Attrib.put("Filled",               Item.class.getMethod("getFilled"),
			                                    Item.class.getMethod("setFilled", pBool));
			_Attrib.put("Formula",              Item.class.getMethod("getFormula"),
			                                    Item.class.getMethod("setFormula", pString));
			_Attrib.put("Thickness",            Item.class.getMethod("getThickness"),
			                                    Item.class.getMethod("setThickness", pFloat));
//			_Attrib.put("LineIDs",              Item.class.getMethod("getLineIDs"),
//			                                    Item.class.getMethod("setLineIDs", pString));
//			_Attrib.put("ConstraintIDs",        Item.class.getMethod("getConstraintIDs"),
//			                                    Item.class.getMethod("setConstraintIDs", pString));
			_Attrib.put("Size",                 Item.class.getMethod("getSize"),
			                                    Item.class.getMethod("setSize", pFloat));
			_Attrib.put("Save",                 Item.class.getMethod("getSave"),
			                                    Item.class.getMethod("setSave", pBool));
			_Attrib.put("AutoStamp",            Item.class.getMethod("getAutoStamp"),
			                                    Item.class.getMethod("setAutoStamp", pFloatO));
			_Attrib.put("Width",                Item.class.getMethod("getWidthToSave"),
			                                    Item.class.getMethod("setWidth", pIntO));
			_Attrib.put("X",                    null,
			                                    Item.class.getMethod("setX", pFloat));
			_Attrib.put("Y",                    null,
			                                    Item.class.getMethod("setY", pFloat));
			_Attrib.put("Tooltip",              Item.class.getMethod("getTooltip"),
			                                    Item.class.getMethod("setTooltips", pList));
			_Attrib.put("Permission",           Item.class.getMethod("getPermission"),
			                                    Item.class.getMethod("setPermission", pPermission));
			
			// Text Items
			_Attrib.put("Family",               Text.class.getMethod("getFamily"),
			                                    Text.class.getMethod("setFamily", pString));
			_Attrib.put("FontStyle",            Text.class.getMethod("getFontStyle"),
			                                    Text.class.getMethod("setFontStyle", pString));
			_Attrib.put("Justification",        Text.class.getMethod("getJustification"),
			                                    Text.class.getMethod("setJustification", pJustification));
			_Attrib.put("AutoWrap",             Text.class.getMethod("getAutoWrapToSave"),
			                                    Text.class.getMethod("setAutoWrap", pBool));
			
			_Attrib.put("LineSpacing",			Text.class.getMethod("getSpacing"), 
												Text.class.getMethod("setSpacing", pFloat));
			
			_Attrib.put("LetterSpacing",		Text.class.getMethod("getLetterSpacing"), 
												Text.class.getMethod("setLetterSpacing", pFloat));
			
			// Aliases for attribute setting
			_Attrib.alias("pos",                "position");
			_Attrib.alias("p",                  "position");
			_Attrib.alias("xy",                 "position");
			_Attrib.alias("a",                  "action");
			_Attrib.alias("d",                  "data");
			_Attrib.alias("f",                  "formula");
			_Attrib.alias("font",               "family");
			_Attrib.alias("s",                  "size");
			_Attrib.alias("l",                  "link");
			_Attrib.alias("at",                 "anchortop");
			_Attrib.alias("ab",                 "anchorbottom");
			_Attrib.alias("al",                 "anchorleft");
			_Attrib.alias("ar",                 "anchorright");
			_Attrib.alias("t",                  "thickness");
			// _Attrib.alias("c",                  "color"); // breaks circle creation
			_Attrib.alias("bgc",                "backgroundcolor");
			_Attrib.alias("bc",                 "bordercolor");
			_Attrib.alias("fc",                 "fillcolor");
			_Attrib.alias("gc",                 "gradientcolor");
			_Attrib.alias("ga",                 "gradientangle");
			_Attrib.alias("fp",                 "fillpattern");
			_Attrib.alias("lm",                 "linkmark");
			_Attrib.alias("am",                 "actionmark");
			_Attrib.alias("dt",                 "dottype");
			_Attrib.alias("fill",               "filled");
			_Attrib.alias("lp",                 "linepattern");
			_Attrib.alias("lf",                 "linkframeset");
			_Attrib.alias("lt",                 "linktemplate");
			_Attrib.alias("face",               "fontstyle");
			_Attrib.alias("j",                  "justification");
			_Attrib.alias("w",                  "width");
			_Attrib.alias("as",                 "autostamp");
			

		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Extracts a list of attributes from the given Item. Any method that
	 * starts with <code>get</code>, takes no arguments and is not found in
	 * the Ignore list will be run, All the attributes are then put into a Text
	 * Item of the form <Name>:<Value> If the value returned by the get method
	 * is null, then the attribute will not be included, unless the name of the
	 * method is found in the AllowNull list.
	 * 
	 * @param toExtract
	 *            The Object from which to extract the attributes
	 * @return A Text Item containing the extracted Attributes.
	 */
	public static Item extractAttributes(Object toExtract) {
		
		// System.out.println(toExtract);
		
		if (toExtract == null) {
			return null;
		}
		
		// Ensure the lists are populated
		ensureReady();

		AttributeSet attribSet = null;
		if(toExtract instanceof Frame) {
			attribSet = _FrameAttrib;
		} else if(toExtract instanceof Item) {
			attribSet = _Attrib;
		} else {
			throw new IncorrectTypeException("toExtract", "Item | Frame");
		}

		// StringBuffer to store all the extracted Attribute:Value pairs
		StringBuffer attributes = new StringBuffer();

		// iterate through the list of methods
		for (String prop : attribSet.keys) {

			Attribute a = attribSet.get(prop);
			// Make sure the classes of the methods match the item
			if (a != null && a.getter != null && a.getter.getDeclaringClass().isAssignableFrom(toExtract.getClass())) {
				
				try {
					String s = getValue(prop, a, toExtract, true);
					if (s == null)
						continue;
					// Append the attributes
					attributes.append(a.displayName)
							.append(AttributeValuePair.SEPARATOR_STRING)
							.append(s).append('\n');
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// if no attributes were extracted
		if (attributes.length() <= 0)
			return null;

		while (attributes.charAt(attributes.length() - 1) == '\n')
			attributes.delete(attributes.length() - 1, attributes.length());

		// create the text Item
		Frame current = DisplayIO.getCurrentFrame();
		Item attribs = current.getStatsTextItem(attributes.toString());
		return attribs;
	}

	/**
	 * Gets a string form of the value for a given item get method.
	 * @param method
	 * @param item
	 * @param ignore true if the attributes in the IGNORE list should be ignored
	 * @return
	 */
	private static String getValue(String name, Attribute a, Object item, boolean ignore) {
		// assert(method.getName().startsWith("get"));
		
		Object o = null;
		try {
			o = a.getter.invoke(item, (Object[]) null);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}

		if (o == null) {
			// methods that return null are only included if they
			// are in the AllowNull list
			if (_AllowNull.contains(a.getter)) {
				if (name.equals("color"))
					o = "default";
				else if (name.equals("backgroundcolor"))
					o = "transparent";
				else if (name.equals("foregroundcolor"))
					o = "auto";
				else
					o = "";
			} else {
				return null;
			}
		}
		// skip methods that are in the ignore lists
		if (ignore && _IgnoreGet.contains(name)) {
			return null;
		}
		
		if (o instanceof Integer) {
			Integer i = (Integer) o;
			if (i == Item.DEFAULT_INTEGER)
				return null;
			if (a.getter.getName().endsWith("Justification")
					&& ((Justification) o).toString() != null)
				o = ((Justification) o).toString();
			// -1 indicates default value
			else
				o = i;
		} else if (o instanceof Float) {
			if (((Float) o) < -0.0001)
				return null;
			// Null indicates default
			// o = Math.round((Float) o);
		} else if (o instanceof Double) {
			// -1 indicates default value
			if (((Double) o) < 0.0001)
				return null;
		} else if (o instanceof Color) {
			// converts the color to the Expeditee code
			o = Conversion.getExpediteeColorCode((Color) o);
			if (o == null)
				return null;
		} else if (o instanceof Point) {
			Point p = (Point) o;
			o = Math.round(p.getX()) + " " + Math.round(p.getY());
		} else if (o instanceof Font) {
			Font f = (Font) o;

			String s = f.getName() + "-";
			if (f.isPlain())
				s += "Plain";

			if (f.isBold())
				s += "Bold";

			if (f.isItalic())
				s += "Italic";

			s += "-" + f.getSize();
			o = s;
		} else if (o instanceof Text) {
			o = ((Text) o).getFirstLine();
		} else if (o instanceof List) {
			List list = (List) o;
			StringBuffer sb = new StringBuffer();
			for (Object ob : list)
				// TODO check that this works ok
				if (sb.length() == 0) {
					sb.append(ob);
				} else {
					sb.append('\n').append(a.displayName).append(AttributeValuePair.SEPARATOR_STRING).append(ob);
				}
			return sb.toString();
		} else if (o instanceof int[]) {
			StringBuffer sb = new StringBuffer();
			int[] values = (int[]) o;
			for (int i = 0; i < values.length; i++) {
				sb.append(values[i]).append(' ');
			}
			sb.deleteCharAt(sb.length() - 1);
			o = sb.toString();
		} else if (o instanceof Boolean) {
			// true is the default for boolean values
			if (((Boolean) o).booleanValue())
				return null;
		}
		return o.toString();
	}

	/**
	 * Attempts to set the attribute in the given attribute: value pair. The
	 * value string should be formatted as follows:
	 * <code> Attribute: Value </code> Multiple values can be used if they are
	 * separated by spaces
	 * 
	 * @param toSet
	 *            The Item or Frame to set the attribute of
	 * @param attribs
	 *            The Text item that contains the list of attributes to set
	 * @return True if the attribute(s) were sucessfully set, false otherwise
	 */
	public static boolean setAttribute(Object toSet, Text attribs) {
		return setAttribute(toSet, attribs, 1);
	}

	public static boolean setAttribute(Object toSet, Text attribs,
			int minAttributeLength) {
		// error checking
		if (toSet == null || attribs == null)
			return false;

		ensureReady();
		
		AttributeSet attribSet = null;
		if(toSet instanceof Frame) {
			attribSet = _FrameAttrib;
		} else if(toSet instanceof Item) {
			attribSet = _Attrib;
		} else {
			throw new IncorrectTypeException("toExtract", "Item | Frame");
		}

		// if(attribs.isAnnotation())
		// return false;

		// get the list of attribute: value pairs
		List<String> values = attribs.getTextList();
		// if no pairs exist, we are done
		if (values == null || values.size() == 0) {
			return false;
		}

		// loop through all attribute: value pairs
		for (int i = 0; i < values.size(); i++) {
			AttributeValuePair avp = new AttributeValuePair(values.get(i),
					false);

			// If the first is not an attribute value pair then don't do
			// attribute merging
			if (!avp.hasAttribute()
					|| avp.getAttribute().length() < minAttributeLength)
				return false;

			// check if the next string is another attribute to merge or a
			// continuation
			for (; i < values.size() - 1; i++) {
				AttributeValuePair nextAvp = new AttributeValuePair(values
						.get(i + 1), false);

				// if the next String has a colon, then it may be another
				// attribute
				if (nextAvp.hasAttribute()) {
					// if the attribute is the same as v, then it is a
					// continuation
					if (nextAvp.getAttribute().equals(avp.getAttribute())) {
						// strip the attribute from next
						avp.appendValue(nextAvp.getValue() + "\n");

						// if the attribute is not the same, then it may be a
						// new method
					} else {
						break;
					}
				}

				// v.append("\n").append(next);
			}

			try {
				if (!setAttribute(toSet, avp, values.size() > 1)) {

					String stripped = avp.getAttribute();
					if (!avp.hasPair()) {
						// This happens when there is an attribute at the start
						// Then a bunch of plain text
						return false;
					} else if (_IgnoreSet.contains(stripped)) {
						return false;
					} else {
						Attribute a = attribSet.get(stripped);
						if(a == null || a.setter == null) {
							return false;
						}
						String types = "";
						for (Class<?> c : a.setter.getParameterTypes()) {
							types += c.getSimpleName() + " ";
						}
						MessageBay.warningMessage("Wrong arguments for: '"
								+ avp.getAttribute() + "' expecting "
								+ types.trim() + " found '" + avp.getValue() + "'");
					}
				}
			} catch (AttributeException e) {
				MessageBay.errorMessage(e.getMessage());
			}
		}

		return true;
	}

	/**
	 * Sets a single attrubute of a frame or item.
	 * 
	 * @param toSet
	 * @param avp
	 * @param isAttributeList
	 *            some properties are ignored when attribute list are injected
	 *            into an item. These properties are ignored if this param is
	 *            true
	 * @return
	 * @throws NoSuchAttributeException 
	 */
	private static boolean setAttribute(Object toSet, AttributeValuePair avp,
			boolean isAttributeList) throws AttributeException {

		assert (avp.hasAttribute());

		// separate attribute and value from string
		String attribute = avp.getAttribute().toLowerCase();

		String value = avp.getValue();
		assert (value != null);
		
		AttributeSet attribSet = null;
		if(toSet instanceof Frame) {
			attribSet = _FrameAttrib;
		} else if(toSet instanceof Item) {
			attribSet = _Attrib;
		} else {
			throw new IncorrectTypeException("toExtract", "Item | Frame");
		}

		// Some properties are ignored when multiple attributes are being set on
		// an item at the same time
		if (isAttributeList && _IgnoreSet.contains(attribute)) {
			// System.out.println("Attribute ignored: " + attribute);
			return true;
		}

		// Separate multiple values if required
		
		Attribute a = attribSet.get(attribute);
		// if this is not the name of a method, it may be the name of an agent
		if (a == null || a.setter == null) {
			// System.out.println("Attrib not found for: " + attribute);
			return false;
		}

		// if there are duplicate methods with the same name
		List<Method> possibles = new LinkedList<Method>();
		if (a.setter.getDeclaringClass().isInstance(toSet))
			possibles.add(a.setter);
		int i = 0;
		while (attribSet.containsKey(attribute + i)) {
			Method m = attribSet.get(attribute + i).setter;
			if(m == null) {
				break;
			}
			if (m.getDeclaringClass().isAssignableFrom(toSet.getClass())) {
				possibles.add(m);
			}
			i++;
		}

		for (Method possible : possibles) {
			Object current = invokeAttributeGetMethod(avp.getAttribute(), toSet);
			// find the corresponding get method for this set method
			// and get the current value of the attribute

			try {
				Object[] params = Conversion.Convert(possible, value, current);

				try {
					possible.invoke(toSet, params);
					return true;
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					MessageBay.displayMessage(toSet.getClass().getSimpleName()
							+ " type does not support that attribute.");
					// e.printStackTrace();
				}
			} catch (NumberFormatException e) {

			}
		}

		if(possibles.size() == 0){
			if(invokeAttributeGetMethod(avp.getAttribute(), toSet) == null)
				throw new NoSuchAttributeException(avp.getAttribute(), toSet.getClass().getSimpleName());
			throw new ReadOnlyAttributeException(avp.getAttribute(), toSet.getClass().getSimpleName());
		}
		
		return false;
	}

	private static Object invokeAttributeGetMethod(String name, Object toSet) {
		
		AttributeSet attribSet = null;
		if(toSet instanceof Frame) {
			attribSet = _FrameAttrib;
		} else if(toSet instanceof Item) {
			attribSet = _Attrib;
		} else {
			throw new IncorrectTypeException("toExtract", "Item | Frame");
		}
		
		Attribute a = attribSet.get(name.toLowerCase());
		if(a == null) {
			return null;
		}
		try {
			return a.getter.invoke(toSet);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Replaces the current value for the text item with the new value.
	 * 
	 * @param text
	 *            the item whos value is to be changed
	 * @param newValue
	 *            the new value for the item
	 */
	public static void replaceValue(Text text, String newValue) {
		assert (newValue != null);

		AttributeValuePair avp = new AttributeValuePair(text.getText());

		if (avp.getAttribute() == null) {
			avp.setAttribute(avp.getValue());
		}
		avp.setValue(newValue);
		text.setText(avp.toString());
	}

	public static String getAttribute(Item item, String attribute) {

		// ensure the lists are populated
		ensureReady();

		// separate attribute and value from string
		String lowerAttribute = attribute.trim().toLowerCase();

		Attribute a = _Attrib.get(lowerAttribute);
		if(a == null) {
			MessageBay.errorMessage("Could no extract unknown attribute value: " + attribute);
			return null;
		}
		return a.displayName + AttributeValuePair.SEPARATOR_STRING + getValue(lowerAttribute, a, item, false);
	}
}
