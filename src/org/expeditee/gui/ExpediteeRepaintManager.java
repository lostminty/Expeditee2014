package org.expeditee.gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.RepaintManager;

public class ExpediteeRepaintManager extends RepaintManager {
	
	private static ExpediteeRepaintManager _instance = null;
	private ExpediteeRepaintManager() {
		super();
		setDoubleBufferingEnabled(false); // use as less resources as possible
	}
	
	public static ExpediteeRepaintManager getInstance() {
		if (_instance == null) {
			_instance = new ExpediteeRepaintManager();
		}
		return _instance;
	}
	
	
    public synchronized void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
 
    	if (c != null) {
    		
    		if (Browser._theBrowser == null || c == Browser._theBrowser.getContentPane()) return;

    		Rectangle dirty = new Rectangle();
    		convertRectangleOrigin(c, new Rectangle(x, y, w, h), dirty);
			FrameGraphics.invalidateArea(dirty);
			FrameGraphics.requestRefresh(true); // ALWAYS REQUEST: Avoids AWT locks
    		
    	/*	if (convertRectangleOrigin(c, new Rectangle(x, y, w, h), dirty)) {
        		FrameGraphics.invalidateArea(dirty);
        		FrameGraphics.refresh(true);
    		} else {
    			FrameGraphics.invalidateArea(dirty);
    			FrameGraphics.requestRefresh(true);
    		}*/

    		// DEBUGGING
    		//Browser._theBrowser.g.setColor(Color.ORANGE);
    		//Browser._theBrowser.g.fillRect(dirty.x, dirty.y, dirty.width, dirty.height);
    		
    	}
    }
    
	/*@Override
	public void addDirtyRegion(Applet applet, int x, int y, int w, int h) {
		// Ignore
	}

	@Override
	public void addDirtyRegion(Window window, int x, int y, int w, int h) {
		// Ignore
	}*/
    
    /**
     * Converts a rectangle from swing/awt space to expeditee space
     * @param c
     * @param dirty
     * @return
     */
    private boolean convertRectangleOrigin(JComponent c, Rectangle dirty, Rectangle dest) {
    	Point p = new Point(0,0);
    	boolean isInContentPane = getPointInContentPane(c, p);
    	dest.setBounds(dirty);
    	dest.translate(p.x, p.y);
    	return isInContentPane;
    }
    
    private boolean getPointInContentPane(Component c, Point p) {
    	
    	if (c == Browser._theBrowser.getContentPane()) 
    		return true;
    	else if (c == null || 
    			c == Browser._theBrowser.getLayeredPane()) {
    		return false;
    	}

    	p.translate(c.getX(), c.getY());
    	
    	return getPointInContentPane(c.getParent(), p);
    }
    



}