package org.expeditee.gui;

import java.awt.event.InputEvent;
import java.util.Collection;

import org.expeditee.items.Item;

public class DisplayIOTest extends GuiTestCase {

	public final void testToggleTwinFrames() {
		showBlankFrame();

		DisplayIO.ToggleTwinFrames();
		assertTrue(DisplayIO.isTwinFramesOn());
		DisplayIO.ToggleTwinFrames();
		assertFalse(DisplayIO.isTwinFramesOn());
	}

	public final void testMouse() throws Exception{
		Frame testFrame = showBlankFrame();
		drawLine();
		
		//Draw a rectangle
		DisplayIO.setCursorPosition(250, 250);
		Thread.sleep(100);
		DisplayIO.clickMouse(InputEvent.BUTTON3_MASK);
		Thread.sleep(100);
		DisplayIO.setCursorPosition(500, 500);
		Thread.sleep(100);
		DisplayIO.clickMouse(InputEvent.BUTTON3_MASK);
		Thread.sleep(1000);
		//Check that the line is there
		Collection<Item> items = testFrame.getVisibleItems();
		assertTrue(items.size() == 12);
		//Map<String, Integer> itemCounts = getItemCount(items);
		
	}

	/**
	 * @throws InterruptedException
	 */
	public static void drawLine() throws InterruptedException {
		//Draw a line
		DisplayIO.setCursorPosition(100, 100);
		Thread.sleep(500);
		DisplayIO.clickMouse(InputEvent.BUTTON2_MASK);
		Thread.sleep(100);
		DisplayIO.setCursorPosition(200, 200);
		Thread.sleep(100);
		DisplayIO.clickMouse(InputEvent.BUTTON2_MASK);
		Thread.sleep(100);
	}

//	private Map<String, Integer> getItemCount(Collection<Item> items) {
//		Map<String, Integer> itemCounts = new HashMap<String,Integer>();
//		for(Item i : items){
//			String className = i.getClass().toString();
//			if(itemCounts.containsKey(className)){
//				itemCounts.put(className, itemCounts.get(className)+1);
//			}else{
//				itemCounts.put(className, 1);
//			}
//			
//		}
//		return itemCounts;
//	}
	
	// public final void testReleaseMouse() {
	// fail("Not yet implemented");
	// }
	//
	// public final void testClickMouse() {
	// fail("Not yet implemented");
	// }

}
