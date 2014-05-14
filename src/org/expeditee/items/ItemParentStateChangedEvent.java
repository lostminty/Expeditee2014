package org.expeditee.items;

import org.expeditee.gui.Frame;

/**
 * Raised whenever the items parent (Frame) changes - when the frame is no longer in view
 * or becomes in view, or if the item has no parent / has a new parent.
 * 
 * @author Brook Novak
 *
 */
public class ItemParentStateChangedEvent {
	
	private Frame _src;
	private UserAppliedPermission _overlayLevel;
	private int _eventType;
	
	public static final int EVENT_TYPE_SHOWN = 1;
	public static final int EVENT_TYPE_SHOWN_VIA_OVERLAY = 2;
	public static final int EVENT_TYPE_HIDDEN = 3;
	public static final int EVENT_TYPE_ADDED = 4;
	public static final int EVENT_TYPE_ADDED_VIA_OVERLAY = 5;
	public static final int EVENT_TYPE_REMOVED = 6;
	public static final int EVENT_TYPE_REMOVED_VIA_OVERLAY = 7;
	
	
	public ItemParentStateChangedEvent(Frame src, int eventType) {
		this(src, eventType, UserAppliedPermission.none);
	}
	
	public ItemParentStateChangedEvent(Frame src, int eventType, UserAppliedPermission overlayLevel) {
		_src = src;
		_overlayLevel = overlayLevel;
		_eventType = eventType;
	}

	/**
	 * 
	 * @return
	 */
	public UserAppliedPermission getOverlayLevel() {
		return _overlayLevel;
	}

	/**
	 * The parent that the item has been added to, removed from, shown on or hidden on. 
	 * @return
	 */
	public Frame getSource() {
		return _src;
	}
	
	public int getEventType() {
		return _eventType;
	}
	
	

}
