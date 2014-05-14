package org.expeditee.io;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.expeditee.items.Item;
import org.expeditee.items.Justification;
import org.expeditee.items.Text;

public class ConversionTest extends TestCase {

	public final void testGetColor() {
		assertEquals(null, Conversion.getColor("TrAnSpArEnT", Color.white));
		assertEquals(null, Conversion.getColor("Clear", Color.white));
		assertEquals(null, Conversion.getColor("Auto", Color.white));
		assertEquals(null, Conversion.getColor("Default", Color.white));
		assertEquals(null, Conversion.getColor("adf", Color.red));
		assertEquals(null, Conversion.getColor("none", Color.red));
		assertEquals(null, Conversion.getColor("null", Color.red));
		assertEquals(null, Conversion.getColor(null, Color.red));
		assertEquals(null, Conversion.getColor("a0a0a0", Color.white));

		assertEquals(Color.red, Conversion.getColor("100 0 0", Color.white));
		assertEquals(Color.green, Conversion.getColor("0 100 0", Color.white));
		assertEquals(Color.blue, Conversion.getColor("0 0 100", Color.white));
		assertEquals(Color.white, Conversion.getColor("100 100 100",
				Color.white));
		assertEquals(Color.gray, Conversion.getColor("50 50 50", Color.white));
		assertEquals(Color.green, Conversion.getColor("-100 +0 -100",
				Color.white));
		assertEquals(Color.green, Conversion.getColor("-10 +100 +0",
				Color.black));
		assertEquals(Color.green, Conversion.getColor("+0 +110 +0", null));

		assertEquals(Conversion.getColor("red9", Color.white), new Color(0.9F,
				0.0F, 0.0F));
		assertEquals(Conversion.getColor("BlUe15", Color.white), new Color(
				0.0F, 0.0F, 1.0F));
		assertEquals(Conversion.getColor("GREEN0", Color.white), new Color(
				0.0F, 0.0F, 0.0F));
		assertEquals(Conversion.getColor("GREEN3", Color.white), new Color(
				0.0F, 0.3F, 0.0F));
		assertEquals(Conversion.getColor("Blue1", Color.white), new Color(0.0F,
				0.0F, 0.1F));
	}

	public final void testGetExpediteeColorCode() {
		assertTrue(Conversion.getExpediteeColorCode(Color.red)
				.equals("100 0 0"));
		assertTrue(Conversion.getExpediteeColorCode(Color.green).equals(
				"0 100 0"));
		assertTrue(Conversion.getExpediteeColorCode(Color.blue).equals(
				"0 0 100"));
	}

	public final void testGetExpediteeFontCode() {
		Font testFont = new Font(Text.FONT_WHEEL[1], Font.BOLD, 18);
		Font testFont2 = new Font(Text.FONT_WHEEL[4], Font.ITALIC, 20);
		Font testFont3 = new Font("xxx123", Font.ITALIC | Font.BOLD, 10);
		Font testFont4 = new Font(Text.FONT_WHEEL[0], Font.PLAIN, 2);

		String result = Text.FONT_CHARS[1] + "b18";
		assertEquals(result, Conversion.getExpediteeFontCode(testFont));

		result = Text.FONT_CHARS[4] + "i20";
		assertEquals(result, Conversion.getExpediteeFontCode(testFont2));

		result = "dp10";
		assertEquals(result, Conversion.getExpediteeFontCode(testFont3));

		result = Text.FONT_CHARS[0] + "r2";
		assertEquals(result, Conversion.getExpediteeFontCode(testFont4));
	}

	public final void testGetFont() {
		Font testFont = new Font(Text.FONT_WHEEL[1], Font.BOLD, 18);
		Font testFont2 = new Font(Text.FONT_WHEEL[4], Font.ITALIC, 20);
		Font testFont3 = new Font(Text.FONT_WHEEL[3], Font.ITALIC | Font.BOLD,
				10);
		Font testFont4 = new Font(Text.FONT_WHEEL[0], Font.PLAIN, 2);

		String code = Text.FONT_CHARS[1] + "b18";
		assertEquals(testFont, Conversion.getFont(code));

		code = Text.FONT_CHARS[4] + "i20";
		assertEquals(testFont2, Conversion.getFont(code));

		code = Text.FONT_CHARS[3] + "p10";
		assertEquals(testFont3, Conversion.getFont(code));

		code = Text.FONT_CHARS[0] + "r2";
		assertEquals(testFont4, Conversion.getFont(code));
	}

	public final void testGetFrameNumber() {
		assertEquals(1, Conversion.getFrameNumber("a1"));
		assertEquals(91, Conversion.getFrameNumber("a91"));
		assertEquals(1, Conversion.getFrameNumber("a1C1"));
	}

	public final void testGetFrameset() {
		assertTrue(Conversion.getFramesetName("a1").equals("a"));
		assertTrue(Conversion.getFramesetName("a91").equals("a"));
		assertTrue(Conversion.getFramesetName("Aa1C1").equals("aa1c"));
		assertTrue(Conversion.getFramesetName("Aa1C1", false).equals("Aa1C"));
		assertTrue(Conversion.getFramesetName("Abc1C1", true).equals(
				Conversion.getFramesetName("Abc1C1")));
	}

	public final void testConvertClassString() {
		assertEquals(null, Conversion.Convert(Class.class, null));

		assertEquals("  Test   ", Conversion.Convert(String.class, "  Test   "));
		assertEquals(null, Conversion.Convert(String.class, ""));

		// Test fonts
		assertEquals(Conversion.getFont("tR16"), Conversion.Convert(Font.class,
				"Tr16"));
		assertEquals(Conversion.getFont("tR16"), Conversion.Convert(Font.class,
				"Tr16"));

		// Test Color Conversion
		assertEquals(Color.darkGray, Conversion.Convert(Color.class,
				"dark_gray"));
		assertEquals(Color.darkGray, Conversion
				.Convert(Color.class, "DARKGRAY"));
		assertEquals(Color.red, Conversion.Convert(Color.class, "red"));
		assertEquals(Color.red, Conversion.Convert(Color.class, "Red"));
		assertEquals(Color.red, Conversion.Convert(Color.class, "RED"));
		assertEquals(Color.red, Conversion.Convert(Color.class, " 100 "));
		assertEquals(Color.green, Conversion.Convert(Color.class, "0  100"));
		assertEquals(Color.blue, Conversion.Convert(Color.class, "0 0 100"));
		// Test Boolean Conversion
		assertEquals(true, Conversion.Convert(boolean.class, ""));
		assertEquals(true, Conversion.Convert(boolean.class, "tRuE"));
		assertEquals(true, Conversion.Convert(boolean.class, "T"));
		assertEquals(true, Conversion.Convert(boolean.class, "yes"));
		assertEquals(true, Conversion.Convert(boolean.class, "Y"));
		assertEquals(false, Conversion.Convert(boolean.class, "FALSE"));
		assertEquals(false, Conversion.Convert(boolean.class, "no"));
		assertEquals(false, Conversion.Convert(boolean.class, "n"));
		assertEquals(false, Conversion.Convert(boolean.class, "f"));
		assertEquals(false, Conversion.Convert(boolean.class, "XXX"));
	}

	public final void testConvertClassStringObject() {
		assertEquals(new Color(255, 0, 255), Conversion.Convert(Color.class,
				"+100 +0 +100", null));
		assertEquals(Color.red, Conversion.Convert(Color.class, "+100 +0 -100",
				Color.blue));
		assertEquals(Color.black, Conversion.Convert(Color.class, "-0 +0 -100",
				Color.blue));
		// Float test
		assertEquals(2.0F, Conversion.Convert(float.class, "2.0", null));
		assertEquals(-8.0F, Conversion.Convert(float.class, "+2.0", -10.0F));
		assertEquals(8.0F, Conversion.Convert(float.class, "-2.0", 10.0F));
//		 Float test
		assertEquals(2.0F, Conversion.Convert(Float.class, "2.0", null));
		assertEquals(-8.0F, Conversion.Convert(Float.class, "+2.0", -10.0F));
		assertEquals(8.0F, Conversion.Convert(Float.class, "-2.0", 10.0F));
		assertEquals(null, Conversion.Convert(Float.class, "", 10.0F));
		assertEquals(null, Conversion.Convert(Float.class, "null", 10.0F));
		// Double test
		assertEquals(2.0, Conversion.Convert(double.class, "2.0", null));
		assertEquals(-8.0, Conversion.Convert(double.class, "+2.0", -10.0));
		assertEquals(8.0, Conversion.Convert(double.class, "-2.0", 10.0));
		// Integer test
		assertEquals(255, Conversion.Convert(int.class, "0xFF", null));
		assertEquals(5, Conversion.Convert(int.class, "+0xF", -10));
		assertEquals(2, Conversion.Convert(int.class, "2", null));
		assertEquals(-8, Conversion.Convert(int.class, "+2", -10));
		assertEquals(8, Conversion.Convert(int.class, "-2", 10));
		assertEquals(-1, Conversion.Convert(int.class, "null", 10));

		// Integer Array test
		int[] expectedResult = { 2, 10, 4, 5 };
		int[] actualResult = (int[]) Conversion.Convert(int[].class,
				"2 10 4 5", null);
		for (int i = 0; i < expectedResult.length; i++)
			assertEquals(expectedResult[i], actualResult[i]);

		// Point test
		assertEquals(new Point(-2, 20), Conversion.Convert(Point.class,
				" -2  20  ", null));
		assertEquals(new Point(-12, 20), Conversion.Convert(Point.class,
				"-2 20", new Point(-10, -20)));
		assertEquals(new Point(-8, -30), Conversion.Convert(Point.class,
				"+2 -10", new Point(-10, -20)));
	}

	public final void testConvertMethodStringObject() {
		try {
			Method appendLine = Text.class.getMethod("appendLine",
					new Class[] { String.class });
			Method setPosition = Item.class.getMethod("setPosition",
					new Class[] { Point.class });
			Method setJustification = Text.class.getMethod("setJustification",
					new Class[] { Justification.class });
			Method setArrow = Text.class.getMethod("setArrow", new Class[] {
					float.class, double.class });
			Method setActions = Text.class.getMethod("setActions",
					new Class[] { List.class });

			// Test that the two methods return the same value
			assertEquals(Conversion.Convert(setPosition, " 10  20 ")[0],
					Conversion.Convert(setPosition, " 10   20", null)[0]);
			Object[] result = Conversion.Convert(setPosition, " 10   20", null);
			assertEquals(1, result.length);
			assertEquals(new Point(10, 20), result[0]);

			result = Conversion.Convert(setPosition, "-10 20", null);
			assertEquals(1, result.length);
			assertEquals(new Point(-10, 20), result[0]);

			// Test justification method
			String justificationCode = "  f  ";
			result = Conversion.Convert(setJustification, justificationCode,
					null);
			assertEquals(1, result.length);
			assertEquals(Justification.convertString(justificationCode),
					result[0]);

			justificationCode = "f";
			result = Conversion
					.Convert(setJustification, justificationCode, "");
			assertEquals(1, result.length);
			assertEquals(Justification.convertString(justificationCode),
					result[0]);

			// Test the setting of Arrows
			result = Conversion.Convert(setArrow, " 20  10.5 ", null);
			assertEquals(2, result.length);
			assertEquals(20.0F, result[0]);
			assertEquals(10.5, result[1]);

			result = Conversion.Convert(setArrow, "+20 9.5", "10 10.5");
			assertEquals(30.0F, result[0]);
			assertEquals(9.5, result[1]);

			result = Conversion.Convert(setArrow, " 20  +2.5 ", "10 10.5");
			assertEquals(20.0F, result[0]);
			assertEquals(13.0, result[1]);

			assertEquals(null, Conversion.Convert(setArrow, " 23423 "));

			// Test a setMethod with list parameter
			String actionsString = "action1\naction2\naction3";
			result = Conversion.Convert(setActions, actionsString, null);
			assertTrue(result[0] instanceof List);
			List resultList = (List) result[0];
			assertEquals(3, resultList.size());
			assertEquals("action1", resultList.get(0));
			assertEquals("action2", resultList.get(1));
			assertEquals("action3", resultList.get(2));

			// Test the setting of text
			String text = "   + BulletTest ";
			result = Conversion.Convert(appendLine, text, null);
			assertEquals(1, result.length);
			assertEquals(text, result[0]);

		} catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}

	public final void testConvertToExpeditee() {
		try {
			java.lang.reflect.Method getPosition = Item.class.getMethod(
					"getPosition", new Class[] {});
			java.lang.reflect.Method getJustification = Text.class.getMethod(
					"getJustification", new Class[] {});

			assertEquals('C', Conversion.ConvertToExpeditee(getJustification,
					Justification.center));

			assertEquals("Test", Conversion.ConvertToExpeditee(getPosition,
					(Object) "Test"));
			assertEquals("0", Conversion.ConvertToExpeditee(getPosition,
					(Object) 0));
			assertEquals("0.0", Conversion.ConvertToExpeditee(getPosition,
					(Object) 0.0F));
			assertEquals(null, Conversion.ConvertToExpeditee(getPosition,
					(new Integer(-1))));
			assertEquals(null, Conversion.ConvertToExpeditee(getPosition,
					(new Float(-0.1F))));
			assertEquals(null, Conversion.ConvertToExpeditee(getPosition,
					(new Boolean(true))));
			assertEquals("F", Conversion.ConvertToExpeditee(getPosition,
					(new Boolean(false))));
			assertEquals(null, Conversion.ConvertToExpeditee(getPosition,
					(new int[] {})));
			assertEquals("1 22 333", Conversion.ConvertToExpeditee(getPosition,
					(new int[] { 1, 22, 333 })));

			assertEquals("22 44", Conversion.ConvertToExpeditee(getPosition,
					(new Point(22, 44))));

			assertEquals("100 0 0", Conversion.ConvertToExpeditee(getPosition,
					Color.red));

			assertEquals(null, Conversion.ConvertToExpeditee(getPosition, null));
			List<String> testList = new ArrayList<String>();
			testList.add("test");

			assertEquals(testList, Conversion.ConvertToExpeditee(getPosition,
					testList));

			Font testFont = new Font(Text.FONT_WHEEL[1], Font.BOLD, 18);
			String result = Text.FONT_CHARS[1] + "b18";
			assertEquals(result, Conversion.ConvertToExpeditee(getPosition,
					testFont));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public final void testGetCssColor() {
		assertEquals("rgb(255,0,0)", Conversion.getCssColor(Color.red));
		assertEquals("rgb(255,0,255)", Conversion.getCssColor(Color.magenta));
		assertEquals("rgb(128,128,128)", Conversion.getCssColor(Color.gray));
	}
}
