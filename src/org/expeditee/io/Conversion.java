package org.expeditee.io;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.expeditee.actions.Actions;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

import com.lowagie.text.FontFactory;

/**
 * This class provides various methods for converting values to\from Java
 * objects and Expeditee file values.
 * 
 * @author jdm18
 * 
 */
public class Conversion {

	public static final int RGB_MAX = 255;

	private static final float RGB_CONVERSION_FACTOR = 2.55F;

	/**
	 * Returns the Color corresponding to the given Expeditee color code. For
	 * example: <br>
	 * green4 = 0% red, 40% green, 0% blue.
	 * 
	 * @param colorCode
	 *            The Expeditee color code to convert
	 * @return The Color object corresponding to the given code
	 */
	public static Color getColor(String colorCode, Color current) {
		if (colorCode == null) {
			return null;
		}
		// The if we dont do this then we end up getting black
		colorCode = colorCode.toLowerCase();
		if (colorCode.equals("null"))
			return null;

		// check if its a normal rgb code ie. 100 0 40
		Color rgb = getRGBColor(colorCode, current);
		if (rgb != null)
			return rgb;

		// separate percentage from color (if necessary)
		String num = "";
		int last = colorCode.length() - 1;

		char c = colorCode.charAt(last);

		while (Character.isDigit(c)) {
			num = c + num;
			if (last <= 0)
				break;

			c = colorCode.charAt(--last);
		}

		final float MAX_AMOUNT = 10F;
		float amount = MAX_AMOUNT;
		if (num.length() > 0)
			amount = Float.parseFloat(num);

		if (amount > MAX_AMOUNT)
			amount = MAX_AMOUNT;

		float color[] = { 0, 0, 0 };
		// Assert.assertTrue(color.length == 3);

		if (colorCode.startsWith("red"))
			color[0] = amount / MAX_AMOUNT;
		else if (colorCode.startsWith("green"))
			color[1] = amount / MAX_AMOUNT;
		else if (colorCode.startsWith("blue"))
			color[2] = amount / MAX_AMOUNT;
		else
			return null;

		return new Color(color[0], color[1], color[2]);
	}

	private static Color getRGBColor(String colorCode, Color current) {
		int color[] = new int[4];
		// Assert.assertTrue(color.length == 3);

		try {
			String[] values = colorCode.trim().split("\\s+");
			// For now no transparency only RGB
			if (values.length > color.length)
				return null;

			String r = values.length > 0 ? values[0] : "0";
			String g = values.length > 1 ? values[1] : "0";
			String b = values.length > 2 ? values[2] : "0";
			String a = values.length > 3 ? values[3] : "100";

			int red = (current == null ? 0 : toColorPercent(current.getRed()));
			int green = (current == null ? 0 : toColorPercent(current.getGreen()));
			int blue = (current == null ? 0 : toColorPercent(current.getBlue()));
			int alpha = (current == null ? 0 : toColorPercent(current.getAlpha()));
			
			color[0] = (Integer) Convert(int.class, r, red);
			color[1] = (Integer) Convert(int.class, g, green);
			color[2] = (Integer) Convert(int.class, b, blue);
			color[3] = (Integer) Convert(int.class, a, alpha);

			for (int i = 0; i < color.length; i++) {
				color[i] = toRGB(color[i]);
			}
			return new Color(color[0], color[1], color[2], color[3]);
		} catch (Exception e) {
			return null;
		}
	}

	private static Integer toColorPercent(int rgb) {
		assert (rgb >= 0);
		assert (rgb <= RGB_MAX);

		int percent = (int) Math.round(rgb / RGB_CONVERSION_FACTOR);

		// Dont need to do the checking below because this method will always be
		// called with good values
		// if (percent > PERCENT_MAX)
		// percent = PERCENT_MAX;
		// else if (percent < 0)
		// percent = 0;

		return percent;
	}

	private static Integer toRGB(int percent) {
		int rgb = Math.round(percent * RGB_CONVERSION_FACTOR);
		if (rgb > RGB_MAX)
			rgb = RGB_MAX;
		else if (rgb < 0)
			rgb = 0;

		return rgb;
	}

	/**
	 * Converts the given Color object to the corresponding Expeditee color code
	 * 
	 * @param color
	 *            The Color to be turned into Expeditee color code.
	 * @return A String containing the Expeditee color code, NULL if the color
	 *         is black.
	 */
	public static String getExpediteeColorCode(Color color) {
		if (color == null)
			return null;

		int r = (int) Math.round((color.getRed() / RGB_CONVERSION_FACTOR));
		int g = (int) Math.round((color.getGreen() / RGB_CONVERSION_FACTOR));
		int b = (int) Math.round((color.getBlue() / RGB_CONVERSION_FACTOR));
		int a = (int) Math.round((color.getAlpha() / RGB_CONVERSION_FACTOR));

		return r + " " + g + " " + b + " " + a;
	}

	/**
	 * Converts the given Font to a corresponding Expeditee font code.
	 * 
	 * @param font
	 *            The Font to convert to a code.
	 * @return The Expeditee font code that corresponds to the given Font.
	 */
	public static String getExpediteeFontCode(Font font) {
		String fontName = font.getFamily();
		String code = font.getFamily() + '_';

		for (int i = 0; i < Text.FONT_WHEEL.length; i++) {
			if (Text.FONT_WHEEL[i].equalsIgnoreCase(fontName)) {
				code = "" + Text.FONT_CHARS[i];
				break;
			}
		}

		switch (font.getStyle()) {
		case Font.BOLD:
			code += "b";
			break;
		case Font.PLAIN:
			code += "r";
			break;
		case Font.ITALIC:
			code += "i";
			break;
		default:
			code += "p";
			break;
		}

		code += font.getSize();
		return code;
	}

	/**
	 * Converts the given Expeditee font code to a Font object.<br>
	 * For example: <br>
	 * tr16 = times, roman, 16pt
	 * 
	 * @param fontCode
	 *            The font code to convert to a Font
	 * @return the Font that corresponds to the given font code.
	 */
	public static Font getFont(String fontCode) {
		assert (fontCode != null);

		int separator = fontCode.indexOf('_');
		String code = Text.FONT_WHEEL[0];
		if (separator > 0) {
			code = Actions.getCapitalizedFontName(fontCode.substring(0,
					separator)) + '-';
			fontCode = fontCode.substring(separator);
		} else {
			char c = fontCode.charAt(0);
			for (int i = 0; i < Text.FONT_CHARS.length; i++) {
				if (c == Text.FONT_CHARS[i]) {
					code = Text.FONT_WHEEL[i] + '-';
					break;
				}
			}
		}

		switch (fontCode.charAt(1)) {
		case 'r':
			code += "Plain";
			break;
		case 'b':
			code += "Bold";
			break;
		case 'i':
			code += "Italic";
			break;
		case 'p':
			code += "BoldItalic";
			break;
		}

		code += "-";

		Font font = null;

		try {
			int size = Integer.parseInt(fontCode.substring(2));
			// double dsize = size * FONT_SCALE;
			// if (dsize - ((int) dsize) > FONT_ROUNDING)
			// dsize = Math.ceil(dsize);

			// code += (int) dsize;
			code += size;

			font = Font.decode(code);
		} catch (NumberFormatException nfe) {
			font = Font.decode(fontCode);
		}

		return font;
	}

	/**
	 * Returns the number portion (Frame Number) of the given Frame name.
	 * 
	 * @param framename
	 *            The Frame name to extract the number from
	 * @return The Frame number
	 */
	public static int getFrameNumber(String framename) {
		String num = null;
		// The framename must end in a digit
		assert (Character.isDigit(framename.charAt(framename.length() - 1)));
		// And start with a letter
		assert (Character.isLetter(framename.charAt(0)));
		// start at the end and find the first non digit char
		for (int i = framename.length() - 2; i >= 0; i--) {
			if (!Character.isDigit(framename.charAt(i))) {
				num = framename.substring(i + 1);
				break;
			}
		}
		return Integer.parseInt(num);
	}

	/**
	 * Returns the frameset poriton of the given Frame name (frame number
	 * removed) converted to lower case.
	 * 
	 * @param frame
	 *            The full name to extract the Frameset name from
	 * @return the name of the Frameset extracted from the given Frame name.
	 */
	public static String getFramesetName(String frame) {
		return getFramesetName(frame, true);
	}

	public static String getFramesetName(String framename,
			boolean convertToLower) {
		String set = null;
		assert (Character.isDigit(framename.charAt(framename.length() - 1)));
		// And start with a letter
		assert (Character.isLetter(framename.charAt(0)));
		// start at the end and find the first non digit char
		for (int i = framename.length() - 2; i >= 0; i--) {
			if (!Character.isDigit(framename.charAt(i))) {
				set = framename.substring(0, i + 1);
				break;
			}
		}

		if (convertToLower)
			return set.toLowerCase();

		return set;
	}

	public static Object Convert(Class type, String value) {
		return Convert(type, value, null);
	}

	// Will convert from Expeditee color values to RGB...
	public static Object Convert(Class type, String value, Object orig) {
		// System.out.println("Orig: " + orig);
		assert (type != null);

		if (value == null)
			return null;

		String fullCaseValue = value;
		value = fullCaseValue.trim().toLowerCase();

		if (type == Font.class) {
			return getFont(value);
		}

		if (type.equals(Color.class)) {
			if (value.length() == 0)
				return null;

			try {
				// Try to decode the string as a hex or octal color code
				return Color.decode(value);
			} catch (NumberFormatException nfe) {
				try {
					// Try to find the field in the Color class with the same name as the given string
					Field[] fields = java.awt.Color.class.getFields();
					Field field = null;
					for (int i = 0; i < fields.length; i++) {
						if (fields[i].getName().equalsIgnoreCase(value)) {
							field = fields[i];
							break;
						}
					}
					return (Color) field.get(null);
				} catch (Exception e) {
					return getColor(value, (Color) orig);
				}
			}
		}

		if (type.equals(int.class)) {
			if (orig instanceof Integer
					&& (value.startsWith("+") || value.startsWith("-"))) {
				value = value.replace("+", "");

				return ((Integer) orig) + Integer.decode(value);
			}

			if (value.length() == 0 || value.equals("null"))
				return Item.DEFAULT_INTEGER;

			return Integer.decode(value);
		}

		if (type.equals(float.class)) {
			if (orig instanceof Float
					&& (value.startsWith("+") || value.startsWith("-"))) {
				value = value.replace("+", "");

				return ((Float) orig) + Float.parseFloat(value);
			}

			return Float.parseFloat(value);
		}

		if (type.equals(Float.class)) {
			if (orig instanceof Float
					&& (value.startsWith("+") || value.startsWith("-"))) {
				value = value.replace("+", "");

				return ((Float) orig) + Float.parseFloat(value);
			}

			if (value.length() == 0 || value.equals("null"))
				return null;

			return Float.parseFloat(value);
		}

		if (type.equals(Integer.class)) {
			if (orig instanceof Integer
					&& (value.startsWith("+") || value.startsWith("-"))) {
				value = value.replace("+", "");

				Integer newValue = ((Integer) orig) + Integer.parseInt(value);
				if (newValue <= 0)
					return null;
				return newValue;
			}

			if (value.length() == 0 || value.equals("null"))
				return null;

			return Integer.parseInt(value);
		}

		if (type.equals(double.class)) {
			if (orig instanceof Double
					&& (value.startsWith("+") || value.startsWith("-"))) {
				value = value.replace("+", "");

				return ((Double) orig) + Double.parseDouble(value);
			}

			return Double.parseDouble(value);
		}

		if (type.equals(int[].class)) {
			StringTokenizer st = new StringTokenizer(value, " ");
			int[] param = new int[st.countTokens()];
			for (int i = 0; i < param.length; i++) {
				try {
					param[i] = Integer.parseInt(st.nextToken());
				} catch (Exception e) {
					return null;
				}
			}

			return param;
		}

		if (type.equals(Font.class)) {
			return Conversion.getFont(value);
		}

		if (type.equals(boolean.class)) {
			if (value.equals("t") || value.equals("true")
					|| value.equals("yes") || value.equals("y")
					|| value.equals(""))
				return true;
			return false;

		}

		if (type.equals(Point.class)) {
			Point p = new Point();
			String xPos = value.substring(0, value.indexOf(" "));
			String yPos = value.substring(value.indexOf(" ") + 1);

			if (orig == null) {
				p.x = Integer.parseInt(xPos.trim());
				p.y = Integer.parseInt(yPos.trim());
			} else {
				assert (orig instanceof Point);
				Point originalPoint = (Point) orig;
				p.x = (Integer) Convert(int.class, xPos, originalPoint.x);
				p.y = (Integer) Convert(int.class, yPos, originalPoint.y);
			}
			return p;
		}

		assert (type == String.class);
		if (value.equals(""))
			return null;
		return fullCaseValue;
	}

	public static Object[] Convert(Method method, String value) {
		return Convert(method, value, null);
	}

	/**
	 * Converts parameters for setting an attribute from a string form into an
	 * object array form. The object array can then be passed when invoke the
	 * set method via reflection.
	 * 
	 * @param method
	 *            a method which sets an attribute
	 * @param value
	 *            new value for the attribute
	 * @param current
	 *            current value of the attribute
	 * @return
	 */
	public static Object[] Convert(Method method, String value, Object current) {

		if (method == null) {
			System.out.println("Error converting null method");
			return null;
		}

		String name = method.getName();
		Class[] types = method.getParameterTypes();

		String fullValue = value;
		value = value.trim();

		if ((method.getParameterTypes()[0].isEnum()) || (name.matches("setPermission"))) {
			Method convertString;
			Object[] objects = new Object[1];
			try {
				convertString = method.getParameterTypes()[0].getMethod(
						"convertString", new Class[] { String.class });
				objects[0] = convertString.invoke(null, new Object[] { value });
			} catch (Exception e) {
				e.printStackTrace();
			}
			return objects;
		}

		if (name.endsWith("Arrow")) {
			if (value.indexOf(" ") < 0)
				return null;

			Float length = null;
			Double ratio = null;
			Double nib_perc = null;
			
			if (current == null) {
				length = getArrowLength(value);
				ratio = getArrowRatio(value);
				nib_perc = getArrowNibPerc(value);
			} else {
				assert (current instanceof String);
				float oldLength = getArrowLength(current.toString());
				double oldRatio = getArrowRatio(current.toString());
				double oldNibPerc = getArrowNibPerc(current.toString());
				
				int first_space_pos = value.indexOf(" ");
				String args23 = value.substring(first_space_pos).trim();
				int second_space_pos = args23.indexOf(" ");
				
				if (second_space_pos<0) {
					// two argument form of arrow data (no nib value)
					second_space_pos = args23.length();
				}
				
				length   = (Float)  Convert(float.class, value.substring(0, value.indexOf(" ")), oldLength);
				ratio    = (Double) Convert(double.class, args23.substring(0,second_space_pos).trim(), oldRatio);
				
				if (second_space_pos==args23.length()) {	
					nib_perc = oldNibPerc;
				}
				else {
					nib_perc = (Double) Convert(double.class, args23.substring(second_space_pos).trim(), oldNibPerc);
				}
			
			}

			Object[] vals = new Object[3];
			vals[0] = length;
			vals[1] = ratio;
			vals[2] = nib_perc;
			return vals;
		}

		if (types.length == 1 && types[0] == List.class) {
			StringTokenizer st = new StringTokenizer(value, "\n");
			List<String> list = new LinkedList<String>();
			while (st.hasMoreTokens())
				list.add(st.nextToken());

			Object[] vals = new Object[1];
			vals[0] = list;
			return vals;
		}

		assert (types.length == 1);

		Object o[] = new Object[1];
		o[0] = Convert(types[0], fullValue, current);
		return o;
	}

	private static float getArrowLength(String args123) {
		return Float.parseFloat(args123.substring(0, args123.indexOf(" ")));
	}

	private static double getArrowRatio(String args123) {
		int first_space_pos = args123.indexOf(" ");
		String args23 = args123.substring(first_space_pos).trim();
		int second_space_pos = args23.indexOf(" ");
		
		if (second_space_pos<0) {
			// two argument form of arrow data (no nib value)
			second_space_pos = args23.length();
		}
		return Double.parseDouble(args23.substring(0,second_space_pos).trim());
	}
	
	private static double getArrowNibPerc(String args123) {
		int first_space_pos = args123.indexOf(" ");
		String args23 = args123.substring(first_space_pos).trim();
		int second_space_pos = args23.indexOf(" ");
		
		double nib_perc = Item.DEFAULT_ARROWHEAD_NIB_PERC;
		
		if (second_space_pos>0) {
			String nib_perc_str = args23.substring(second_space_pos).trim();
			nib_perc = Double.parseDouble(nib_perc_str);
		}
		
		return nib_perc;
		
	}

	public static Object ConvertToExpeditee(Method method, Object output) {
		if (output == null)
			return null;

		assert (method != null);

		String name = method.getName();

		if (output instanceof List)
			return output;

		if (name.endsWith("Text")) {
			List<String> list = new LinkedList<String>();
			for (String s : output.toString().split("\n")) {
				list.add(s);
			}
			return list;
		}

		if ((method.getReturnType().isEnum()) || (name.equals("getPermission"))) {
			try {
				return output.getClass().getMethod("getCode", new Class[] {})
						.invoke(output, new Object[] {});
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		// strings can be returned immediately
		if (output instanceof String)
			return (String) output;

		// For int's... negative numbers signal NULL
		/*
		 * TODO change so that all items use Integer class... where null,
		 * signals null
		 */
		if (method.getReturnType().equals(int.class)) {
			if ((Integer) output >= 0)
				return output + "";
			return null;
		}

		// integers can also(Float) output >= 0 be returned immediately
		if (output instanceof Integer)
			return "" + output;

		// floats can also be returned immediately
		if (output instanceof Float) // && (Float) output >= 0) // Removed checking if >0, as some floats (e.g. letter spacing) can be negative
			return "" + output;

		// convert fonts
		if (output instanceof Font)
			return getExpediteeFontCode((Font) output);

		// convert colors
		if (output instanceof Color)
			return getExpediteeColorCode((Color) output);

		// covert points
		if (output instanceof Point)
			return ((Point) output).x + " " + ((Point) output).y;

		if (output instanceof Boolean)
			if ((Boolean) output)
				return null;
			else
				return "F";

		if (output instanceof int[]) {
			int[] out = (int[]) output;
			String res = "";
			for (int i : out)
				res += i + " ";

			res = res.trim();
			if (res.length() > 0)
				return res;
		}
		// default
		return null;
	}

	public static String getPdfFont(String family) {
		family = family.toLowerCase();
		if (family.equals(Text.FONT_WHEEL[0])) {
			return FontFactory.HELVETICA;
		} else if (family.equals(Text.FONT_WHEEL[2])) {
			return FontFactory.TIMES_ROMAN;
		}
		return FontFactory.COURIER;
	}

	public static String getCssColor(Color c) {
		assert (c != null);
		return "rgb(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue()
				+ ")";
	}

	public static String getCssFontFamily(String family) {
		family = family.toLowerCase();
		if (family.equals("monospaced") || family.equals("dialog")) {
			return "courier";
		} else if (family.equals("sansserif")) {
			return "sans-serif";
		} else {
			return family;
		}
	}
}
