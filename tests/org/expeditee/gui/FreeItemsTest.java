package org.expeditee.gui;


public class FreeItemsTest extends GuiTestCase {

	/**
	 * Test the picking up and putting down of objects on a frame.
	 * 
	 * @throws Exception
	 */
	public final void testAttachedToCursor() throws Exception {
		Frame testFrame = showFrame("ExpediteeTest2");

		FrameMouseActions.pickup(testFrame.getTitleItem());
		assertTrue(FreeItems.itemsAttachedToCursor());
		assertTrue(FreeItems.textOnlyAttachedToCursor());

		FrameMouseActions.anchor(FreeItems.getInstance());
		assertFalse(FreeItems.itemsAttachedToCursor());
		assertFalse(FreeItems.textOnlyAttachedToCursor());

		FrameMouseActions.pickup(testFrame.getAllItems());
		assertTrue(FreeItems.itemsAttachedToCursor());
		assertFalse(FreeItems.textOnlyAttachedToCursor());
		assertEquals(8, FreeItems.getTextItems().size());

		assertEquals(1, FreeItems.getGroupedText().get("").size());
		assertEquals(2, FreeItems.getGroupedText().get("Label").size());
		assertEquals(3, FreeItems.getGroupedText().get("Label2").size());
		assertEquals(null, FreeItems.getGroupedText().get("non existing label"));

		FrameMouseActions.anchor(FreeItems.getInstance());
		assertFalse(FreeItems.itemsAttachedToCursor());
		assertFalse(FreeItems.textOnlyAttachedToCursor());
	}
}
