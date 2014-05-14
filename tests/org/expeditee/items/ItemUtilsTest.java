package org.expeditee.items;

import junit.framework.TestCase;

public class ItemUtilsTest extends TestCase {

	public final void testContainsTagListOfItemString() {
		// fail("Not yet implemented");
	}

	public final void testContainsExactTagListOfItemString() {
		// fail("Not yet implemented");
	}

	public final void testFindTagListOfItemString() {
		// fail("Not yet implemented");
	}

	public final void testFindExactTagListOfItemInt() {
		// fail("Not yet implemented");
	}

	public final void testIsTagItemIntBoolean() {
		// fail("Not yet implemented");
	}

	public final void testIsTagItemStringBoolean() {
		Text text = new Text("");
		String[] valid = new String[] { "@i", "@I:", "   @i:  ",
				"@i: 111 111 111" };
		String[] invalid = new String[] { "", "@isTest", "i:", "@i : 111","@ i: 111",
				"@i; 111" };

		for (int i = 0; i < valid.length; i++) {
			text.setText(valid[i]);
			assertTrue(ItemUtils.startsWithTag(text, "@i"));
		}
		for (int i = 0; i < invalid.length; i++) {
			text.setText(invalid[i]);
			assertFalse(ItemUtils.startsWithTag(text, "@i"));
		}
		text.setText("@LongItem: test test");
		assertTrue(ItemUtils.startsWithTag(text, "@lOnGiTeM"));
	}

	public final void testStripTagStringString() {
		// fail("Not yet implemented");
	}
	
	public final void testStripTagSymbol() {
		String[][] params = new String[][] { { "@item", "item" },
				{ "@", "" }, { null, null }, { "", "" } };
		for (int i = 0; i < params.length; i++) {
			assertEquals(params[i][1], ItemUtils.StripTagSymbol(params[i][0]));
		}
	}

	public final void testGetTag() {
		// fail("Not yet implemented");
	}

}
