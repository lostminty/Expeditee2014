package org.expeditee.gui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.expeditee.items.Item;

public class FrameKeyboardsActionsTest extends GuiTestCase {
	
	public static int DELAY = 200;

	public final void testTypeText() throws Exception {
		Frame testFrame = showBlankFrame();
		DisplayIO.typeKey(KeyEvent.VK_ESCAPE);
		DisplayIO.typeText("hello");
		// Pick the item up in the middle
		Item currentItem = FrameUtils.getCurrentItem();
		assertNotNull(currentItem);
		DisplayIO.setCursorPosition(currentItem.getX()
				+ currentItem.getBoundsWidth() / 2,
				FrameMouseActions.MouseY - 2);
		Thread.sleep(DELAY);
		DisplayIO.clickMouse(InputEvent.BUTTON2_MASK);
		Thread.sleep(DELAY);
		DisplayIO.setCursorPosition(
				Math.round(FrameGraphics.getMaxFrameSize().getWidth() / 2),
				FrameMouseActions.MouseY);
		Thread.sleep(DELAY);
		DisplayIO.typeKey(KeyEvent.VK_ESCAPE);
		Thread.sleep(DELAY);
		DisplayIO.clickMouse(InputEvent.BUTTON2_MASK);
		Thread.sleep(DELAY);

		// Check that the current position is the origin of the item
		assertEquals(currentItem.getX(), FrameMouseActions.getX());

		DisplayIO.typeKey(KeyEvent.VK_ESCAPE);

		DisplayIO.typeStringDirect(0.05, "Hello Again!");

		DisplayIO.typeKey(KeyEvent.VK_ESCAPE);

		DisplayIO.typeText("Hello Again");

		assertEquals(4, testFrame.getItems(false).size());

	}
}
