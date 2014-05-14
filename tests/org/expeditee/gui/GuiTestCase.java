package org.expeditee.gui;

import junit.framework.TestCase;

public class GuiTestCase extends TestCase {
	
	static Browser b;

	protected void setUp() throws Exception {
		b = Browser.initializeForTesting();
		super.setUp();
		
		/* Just incase previous tests left something on the cursor clear it now! */
		FreeItems.getInstance().clear();
	}
	
	protected Frame showBlankFrame(){
		Frame testFrame = FrameIO.LoadFrame("ExpediteeTest1");
		testFrame.clear(false);
		DisplayIO.setCurrentFrame(testFrame, true);
		return testFrame;
	}
	
	protected Frame showFrame(String frameName){
		Frame testFrame = FrameIO.LoadFrame(frameName);
		testFrame.clear(false);
		DisplayIO.setCurrentFrame(testFrame, true);
		return testFrame;
	}
}
