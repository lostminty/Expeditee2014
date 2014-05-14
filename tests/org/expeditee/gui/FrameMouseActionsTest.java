package org.expeditee.gui;

import java.awt.event.InputEvent;

import org.expeditee.actions.Navigation;
import org.expeditee.items.Text;

public class FrameMouseActionsTest extends GuiTestCase {

	public final void testLeftHighlight() throws Exception {
		Frame testFrame = showBlankFrame();
		
		// LeftRange the title text for unlinked title
		Text title = testFrame.getTitleItem();
		title.setLink(null);
		testLeftRanging(title);

		String newFrameName = DisplayIO.getCurrentFrame().getName();
		Navigation.Back();

		// Make sure the newly created frame is not saved when the user goes
		// back
		assertNull(FrameIO.LoadFrame(newFrameName));

		// LeftRange the linked title text
		testLeftRanging(title);

		Thread.sleep(500);
		
		DisplayIOTest.drawLine();
		
		newFrameName = DisplayIO.getCurrentFrame().getName();
		Navigation.Back();
		
		Thread.sleep(500);
		// Make sure the newly created frame is saved when the user goes back
		assertNotNull(FrameIO.LoadFrame(newFrameName));
	}

	/**
	 * @param title
	 * @return
	 * @throws InterruptedException
	 */
	private void testLeftRanging(Text title) throws InterruptedException {
		int y = title.getY() - 2;
		DisplayIO.setCursorPosition(title.getX(), y);
		Thread.sleep(500);
		DisplayIO.pressMouse(InputEvent.BUTTON1_MASK);
		Thread.sleep(100);
		DisplayIO.setCursorPosition(title.getX() + title.getBoundsWidth() / 2,
				y);
		Thread.sleep(500);
		DisplayIO.releaseMouse(InputEvent.BUTTON1_MASK);
		Thread.sleep(100);

		// Make sure TDFC was NOT performed
		assertNull(title.getLink());

		// Highlight then unhighlight
		DisplayIO.setCursorPosition(title.getX(), y);
		Thread.sleep(500);
		DisplayIO.pressMouse(InputEvent.BUTTON1_MASK);
		Thread.sleep(100);
		DisplayIO.setCursorPosition(title.getX() + title.getBoundsWidth() / 2,
				y);
		Thread.sleep(500);
		DisplayIO.setCursorPosition(title.getX(), y);
		Thread.sleep(500);
		DisplayIO.releaseMouse(InputEvent.BUTTON1_MASK);
		Thread.sleep(500);

		// Make sure TDFC was performed
		assertNotNull(title.getLink());
	}
}
