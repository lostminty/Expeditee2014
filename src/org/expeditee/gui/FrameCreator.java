package org.expeditee.gui;

import java.awt.Color;

import org.expeditee.agents.ExistingFramesetException;
import org.expeditee.items.Item;
import org.expeditee.items.PermissionPair;
import org.expeditee.items.Text;
import org.expeditee.items.UserAppliedPermission;

public class FrameCreator {
	public static final int INDENT_FROM_TITLE = 20;

	private int START_Y;

	private int START_X;

	private Frame _current = null;

	private int _lastX;

	private int _maxX = 0;

	private int _lastY;

	// Master copy of next & previous links
	private Item _Mnext;

	private Item _Mprev;

	private Item _Mfirst;

	// Next and previous links for the current frame
	// private Item _next;

	// private Item _prev;

	private Frame _firstFrame;

	private boolean _multiColumn;

	public FrameCreator(String frameTitle) {
		this(DisplayIO.getCurrentFrame().getFramesetName(), DisplayIO
				.getCurrentFrame().getPath(), frameTitle, false, false);
	}

	/**
	 * Creates a text item that looks like a clickable button.
	 * 
	 * @param text
	 *            the caption for the button
	 * @param x
	 *            the x position for the button. Null if the button is anchored
	 *            to the right of the screen.
	 * @param y
	 *            the y position for the button. Null if the button is anchored
	 *            to the bottom of the scree.
	 * @param right
	 *            the distance the button is anchored from the right of this
	 *            screen. Null if an absolute position is used.
	 * @param bottom
	 *            the distance the button is to be anchored from the bottom of
	 *            the screen. Null if the button is given an absolute position.
	 * @return the newly created button.
	 */
	public static Item createButton(String text, Float x, Float y, Float right,
			Float bottom) {
		Text button = new Text(text);

		button.setBackgroundColor(Color.LIGHT_GRAY);
		button.setBorderColor(Color.DARK_GRAY);
		button.setThickness(2.0F);
		if (bottom != null)
			button.setAnchorBottom(bottom);
		if (x != null)
			button.setX(x);
		if (right != null)
			button.setAnchorRight(right);
		if (y != null)
			button.setY(y);

		button.updatePolygon();

		return button;
	}

	public FrameCreator(String name, String path, String frameTitle,
			boolean recreate, boolean multiColumn) {
		_multiColumn = multiColumn;
		_Mnext = createButton("@Next", null, null, 10F, 15F);

		_Mprev = createButton("@Previous", null, null, _Mnext.getBoundsWidth()
				+ _Mnext.getAnchorRight() + 20F, 15F);

		_Mfirst = createButton("@First", null, null, _Mprev.getBoundsWidth()
				+ _Mprev.getAnchorRight() + 20F, 15F);

		Frame toUse = null;
		try {
			toUse = FrameIO.CreateFrameset(name, path, recreate);
		} catch (ExistingFramesetException efe) {
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (toUse == null) {
			toUse = FrameIO.CreateFrame(name, frameTitle, null);
		}

		resetGlobals(toUse);
		_firstFrame = toUse;

		// set positions of next\prev frame links
		// _Mnext.setPosition(FrameGraphics.getMaxSize().width - 100,
		// FrameGraphics.getMaxSize().height - 15);
		// _Mprev.setPosition(50, FrameGraphics.getMaxSize().height - 15);
	}

	public String getName() {
		return _firstFrame.getName();
	}

	/**
	 * Creates the next frame in the frameset, with a previous button already
	 * added and linked to the last frame. _current then gets updated to point
	 * at the newly created Frame, and _lastY is reset
	 */
	public boolean createNextFrame() {
		try {
			Frame newFrame = FrameIO.CreateFrame(_current.getFramesetName(),
					_current.getTitle(), null);

			// add link to previous frame
			// _prev =
			addPreviousButton(newFrame, _current.getName());

			// add link to new frame
			// _next =
			addNextButton(_current, newFrame.getName());

			// add link to new frame
			addFirstButton(newFrame, _firstFrame.getName());

			FrameIO.SaveFrame(_current, false);

			resetGlobals(newFrame);
			_maxX = 0;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void resetGlobals(Frame toUse) {
		Text title = toUse.getTitleItem();
		START_X = INDENT_FROM_TITLE + title.getX();
		START_Y = getYStart(title);
		_lastY = START_Y;
		_lastX = START_X;
		// Check for @Start
		for (Item it : toUse.getItems()) {
			if (it instanceof Text) {
				Text t = (Text) it;
				if (t.getText().toLowerCase().equals("@start")
						|| t.getText().toLowerCase().startsWith("@start:")) {
					t.stripFirstWord();

					if (t.getText().equals("")) {
						_lastY = t.getY();
						_lastX = t.getX();
						t.delete();
						break;
					}
				}
			}
		}
		_current = toUse;
	}

	public boolean addItem(Item toAdd, boolean bSave) {
		try {
			// if we have reached the end of the Y axis, try moving over on the
			// X axis
			if (_lastY >= _Mprev.getY() - _Mprev.getBoundsHeight()) {
				_lastX = _maxX + 10;
				_lastY = START_Y;

				// if there is no more room on the X axis, we have to start a
				// new frame
				if (!_multiColumn
						|| toAdd.getBoundsWidth() + _lastX > FrameGraphics
								.getMaxSize().width) {
					// Make sure text items that are created on the current
					// frame are removed
					_current.removeItem(toAdd);
					createNextFrame();
				}
			}

			toAdd.setPosition(_lastX, _lastY + toAdd.getBoundsHeight() / 2);
			toAdd.setOffset(0, 0);
			toAdd.setID(_current.getNextItemID());
			toAdd.setRightMargin(FrameGraphics.getMaxFrameSize().width, true);

			_current.addItem(toAdd);
			// _current.addAllItems(items);
			if (bSave)
				save();

			_lastY = toAdd.getY() + toAdd.getBoundsHeight() / 2;
			_maxX = Math.max(toAdd.getX() + toAdd.getBoundsWidth(), _maxX);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public Text addText(String toAdd, Color c, String link, String action,
			boolean bSave) {
		Text text = _current.createNewText(toAdd);
		if (c != null)
			text.setColor(c);
		text.setLink(link);
		text.setAction(action);

		addItem(text, bSave);

		return text;
	}

	public void save() {
		FrameIO.ForceSaveFrame(_current);
	}
	
	public int getLastY() {
		return _lastY;
	}
	
	public void setLastY(int lastY) {
		_lastY = lastY;
	}
	
	public Frame getCurrentFrame() {
		return _current;
	}

	/**
	 * Returns the Frame name of the current frame for the FrameCreator
	 * 
	 * @return The current frame for the FrameCreator
	 */
	public String getCurrent() {
		if (_current == null)
			return null;

		return _current.getName();
	}

	public Frame getFirstFrame() {
		return _firstFrame;
	}

	public Item addNextButton(Frame current, String link) {
		return addButton(_Mnext, current, link);
	}

	public Item addPreviousButton(Frame current, String link) {
		return addButton(_Mprev, current, link);
	}

	public Item addFirstButton(Frame current, String link) {
		return addButton(_Mfirst, current, link);
	}

	public Item addButton(Item template, Frame current, String link) {
		// add link to new frame
		Item previousButton = template.copy();
		previousButton.setID(current.getNextItemID());
		previousButton.setLink(link);
		previousButton.setLinkHistory(false);
		previousButton.setLinkMark(false);
		// previousButton.setPermission(new PermissionPair(UserAppliedPermission.followLinks));
		current.addItem(previousButton);

		return previousButton;
	}

	public void addSpace(int space) {
		_lastY += space;
	}

	public static int getYStart(Item title) {
		return title.getY() + title.getBoundsHeight();
	}

	public void setTitle(String titleText) {
		_current.setTitle(titleText);
	}
}
