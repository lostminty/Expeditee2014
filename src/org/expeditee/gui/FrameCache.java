package org.expeditee.gui;

import java.util.HashMap;

public class FrameCache extends HashMap<String, Frame> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	 public Frame remove(String key) {
		 Frame toRemove = super.remove(key);
		 toRemove.dispose();
		 return toRemove;
	 }
}
