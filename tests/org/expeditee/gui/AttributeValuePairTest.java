package org.expeditee.gui;

import java.awt.Color;

import junit.framework.TestCase;

import org.expeditee.items.Text;

public class AttributeValuePairTest extends TestCase {

	public final void testExtractAttributes() {
		assertEquals(null, AttributeUtils.extractAttributes(null));
	}

	public final void testSetAttribute() {
		Text attributes = new Text("");
		Text text = new Text("Test");

		assertFalse(AttributeUtils.setAttribute(text, attributes));

		// Do a test with no attribute value pairs
		attributes.setText("NO_AVP");
		assertFalse(AttributeUtils.setAttribute(text, attributes));

		assertFalse(AttributeUtils.setAttribute(null, attributes));
		assertFalse(AttributeUtils.setAttribute(attributes, null));

		attributes.setText("s:50");
		assertTrue(AttributeUtils.setAttribute(attributes, attributes));
		assertEquals(50.0F, attributes.getSize());
		attributes.setText("size:25");
		assertTrue(AttributeUtils.setAttribute(attributes, attributes));
		assertEquals(25.0F, attributes.getSize());

		attributes.setText("c:red");
		assertTrue(AttributeUtils.setAttribute(attributes, attributes));
		assertEquals(Color.red, attributes.getColor());
		attributes.setText("color:green");
		assertTrue(AttributeUtils.setAttribute(attributes, attributes));
		assertEquals(Color.green, attributes.getColor());
	}

	public final void testGetValue() {
		String[][] data = new String[][] {
				{ "Att:Value1", "Value1" },
				{ "  Att  :  Value2  ", "Value2" },
				{ "@NoValue", "" },
				{ " @NoVaLuE ", "" },
				{ "@Value   : : : value3", "" },
				{ "@Value:  value3", "value3" },
				{ "   ", "" },
				{
						"Att: value4 " + (char) Character.LINE_SEPARATOR
								+ "A comment", "value4" } };
		for (int i = 0; i < data.length; i++) {
			AttributeValuePair avp = new AttributeValuePair(data[i][0]);
			assertEquals(data[i][1], avp.getValue());
		}
	}

	public final void testGetAttribute() {
		String[][] data = new String[][] { { "AtT1: Value1", "AtT1" },
				{ "  Att2  :  Value2  ", "Att2" }, { "@NoValue", "NoValue" },
				{ "@Att3 : Value", null }, { "@ Att3: Value", null },
				{ " @Att3: Value ", "Att3" }, { ": value3", null },
				{ "@1Test: value3", "1Test" }, { "@t est: value3", null },
				{ "Att4 : test : value4", "Att4 : test" }, { "@", null },
				{ "a", null }, { "*!*", null }, { "*!*: @%@", "*!*" } };
		for (int i = 0; i < data.length; i++) {
			// System.out.println("i = " + i);
			AttributeValuePair avp = new AttributeValuePair(data[i][0]);
			assertEquals(data[i][1], avp.getAttribute());
		}
	}

	public final void testReplaceValue() {
		String[][] data = new String[][] {
				{ "AtT1: Value1", "NewValue", "AtT1: NewValue" },
				{ "  Att2  :  Value2  ", "Att2", "Att2: Att2" },
				{ " @NoValue ", " 15", "@NoValue: 15" },
				{ " 54 ", " 15", "54: 15" } };
		Text test = new Text(-1);

		for (int i = 0; i < data.length; i++) {
			test.setText(data[i][0]);
			AttributeUtils.replaceValue(test, data[i][1]);
			assertEquals(data[i][2], test.getText());
		}
	}

	public final void testGetDoubleValue() {
		Object[][] validData = new Object[][] { { "  Att2  :  3.5  ", 3.5 } };
		for (int i = 0; i < validData.length; i++) {
			AttributeValuePair avp = new AttributeValuePair(validData[i][0]
					.toString());
			assertEquals(validData[i][1], avp.getDoubleValue());
		}
		Object[][] invalidData = new Object[][] { { "AtT1: Value1", null },
				{ "@NoValue", null } };
		for (int i = 0; i < invalidData.length; i++) {
			try {
				AttributeValuePair avp = new AttributeValuePair(
						invalidData[i][0].toString());
				avp.getDoubleValue();
			} catch (Exception e) {
				continue;
			}
			fail();
		}
	}

}
