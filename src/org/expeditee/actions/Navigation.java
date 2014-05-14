package org.expeditee.actions;

import java.util.List;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.settings.UserSettings;

/**
 * Provides the Navigation related action procedures
 * 
 * @author jdm18
 * 
 */
public class Navigation {

	public static void setLastNavigationItem(Item i) {
		_LastItemUsed = i;
		if (i.getParent() != null) {
			_Parent = i.getParent().getName();
		}
	}

	/**
	 * Performs a back operation from the current Frame. If the back-stack is
	 * empty, then nothing happens.
	 */
	public static void Back() {
		DisplayIO.Back();
	}

	public static void Forward() {
		DisplayIO.Forward();
	}

	/**
	 * Displays the user's home frame
	 */
	public static void GotoHome() {
		FrameUtils.DisplayFrame(UserSettings.HomeFrame.get());
	}
	
	/**
	 * Displays the user's first settings frame - should be `username`2
	 */
	public static void GotoSettings() {
		String framename = UserSettings.HomeFrame.get();
		framename = framename.substring(0, framename.length() - 1) + '2';
		FrameUtils.DisplayFrame(framename);
	}

	public static void GotoZero() {
		FrameUtils
				.DisplayFrame(DisplayIO.getCurrentFrame().getFramesetName() + 0);
	}

	/**
	 * Displays the user's profile frame (if there is one)
	 */
	public static void GotoProfile() {
		FrameUtils.DisplayFrame(UserSettings.ProfileName.get() + '1');
	}

	/**
	 * Loads the Frame in the current frameset with the given number and
	 * displays it
	 * 
	 * @param value
	 *            The number of the Frame to load
	 */
	public static void Goto(Integer value) {
		FrameUtils.DisplayFrame(DisplayIO.getCurrentFrame().getFramesetName()
				+ value);
	}

	/**
	 * Loads the Frame with the given FrameName and displays it
	 * 
	 * @param frameName
	 *            The name of the Frame to load
	 */
	public static void Goto(String frameName) {
		FrameUtils.DisplayFrame(frameName);
	}
	
	/**
	 * Goto a frame without adding it to history
	 */
	public static void GotoQuiet(String frameName) {
		FrameUtils.DisplayFrame(frameName, false, true);
	}

	/**
	 * Loads the Frame linked to by the Item that has this action, if there is
	 * one
	 * 
	 * @param source
	 *            The Item that has a link to the Frame to load and display
	 */
	public static void GotoLink(Item source) {
		FrameUtils.DisplayFrame(source.getLink());
	}

	/**
	 * Turns TwinFrames off if it is on, otherwise does nothing
	 */
	public static void Large() {
		if (DisplayIO.isTwinFramesOn())
			DisplayIO.ToggleTwinFrames();
	}

	/**
	 * Navigates to the Frame with the next highest frame number in the current
	 * frameset. If the current frame is the last frame in the frameset, nothing
	 * happens.
	 */
	public static void NextFrame() {
		NextFrame(true);
	}

	public static void NextFrame(boolean addToBack) {
		addToBack = adjustAddToBack(addToBack);
		Frame next = FrameIO.LoadNext();
		FrameUtils.DisplayFrame(next, addToBack, true);
	}

	/**
	 * Navigates to the last frame in the frameset.
	 * 
	 */
	public static void LastFrame() {
		Frame last = FrameIO.LoadLast();
		FrameUtils.DisplayFrame(last, true, true);
	}

	public static void ZeroFrame() {
		Frame zeroFrame = FrameIO.LoadZero();
		FrameUtils.DisplayFrame(zeroFrame, true, true);
	}

	public static void Next() {
		NextFrame();
	}

	public static void Previous() {
		PreviousFrame();
	}

	public static void PreviousFrame(boolean addToBack) {
		addToBack = adjustAddToBack(addToBack);
		Frame prev = FrameIO.LoadPrevious();
		FrameUtils.DisplayFrame(prev, addToBack, true);
	}

	public static void PreviousFrame() {
		PreviousFrame(true);
	}

	private static String _Parent = null;

	private static Item _LastItemUsed = null;

	public static void NextChild(Frame source) {
		String back = DisplayIO.peekFromBackUpStack();
		// if there is no parent frame (i.e. the user is on the home frame) //
		if (back == null) { // No frame was on the Backup stack
			MessageBay.displayMessage("No Parent Frame Found.");
			_LastItemUsed = null; // ByRob: what is reason for setting
			// this to null, who is going to use it????
			_Parent = null; // ByRob: what is reason for setting this to
			// null, who is going to use it
			return;
		}

		// Ensure the parent variable has been initialised
		if (_Parent == null || !back.equals(_Parent)) { // ByRob: what the heck
			// is the code doing?
			// What is it setting
			// up???????
			_Parent = back;
			_LastItemUsed = null;
		}

		Frame parent = FrameIO.LoadFrame(_Parent);

		// find the next item to visit
		List<Item> items = parent.getItems(); // getItems method gets us the
		// FirstItem on the frame
		int parentItemLinkedToSource = 0; // ByMike: Will be set to the
		// index of the item on the parent which follows the item linked to the
		// child frame we are currently viewing.

		// ByMike: if 'Next' has been clicked previously get the index of the
		// item FOLLOWING the ParentItem linked to the source
		if (_LastItemUsed != null) {
			parentItemLinkedToSource = items.indexOf(_LastItemUsed);
		}

		// ByMike: If the 'Next' child is being sought for the 1st time...
		String sourceName = source.getName().toLowerCase();

		// ByMike: Find the first occurence of a ParentItem linked to the source
		while (parentItemLinkedToSource < items.size()
				&& (items.get(parentItemLinkedToSource).getAbsoluteLink() == null || !items
						.get(parentItemLinkedToSource).getAbsoluteLink()
						.toLowerCase().equals(sourceName))) {
			parentItemLinkedToSource++; // ByRob: this increments to the next
			// item
		}

		// Find the next ParentItem linked to the next frame to be displayed
		for (int i = parentItemLinkedToSource + 1; i < items.size(); i++) {
			if (items.get(i).isLinkValid()
					&& !items.get(i).isAnnotation()
					&& !items.get(i).getAbsoluteLink().equalsIgnoreCase(
							source.getName())) {
				_LastItemUsed = items.get(i);
				FrameUtils.DisplayFrame(_LastItemUsed.getAbsoluteLink(), false,
						true);
				return;
			} // ByRob: end of if

		} // ByRob: End of For

		MessageBay.displayMessage("No more child frames found.");
	}

	/**
	 * Turns TwinFrames on if it is off, otherwise does nothing
	 * 
	 */
	// ByRob: Rob doesn't like notion of turning TwinFrames on and off,
	// better to say more explicitly/directly what the mode is!!!!
	public static void Small() {
		if (!DisplayIO.isTwinFramesOn())
			DisplayIO.ToggleTwinFrames();
	}

	/*
	 * When display frame is called with addToBack set to false multiple
	 * times... Only the first frame is added to the backup stack. This flag
	 * stores the state for the addToBack parameter the last time this method
	 * was called.
	 */
	private static boolean _lastAddToBack = true;

	public static void ResetLastAddToBack() {
		_lastAddToBack = true;
	}

	private static boolean adjustAddToBack(boolean addToBack) {
		boolean originalAddToBack = addToBack;

		if (!addToBack && _lastAddToBack) {
			// This adds the first frame to the backup stack when the user
			// navigates through a bunch of frames with the keyboard!
			addToBack = true;
		}

		_lastAddToBack = originalAddToBack;

		return addToBack;
	}
}
