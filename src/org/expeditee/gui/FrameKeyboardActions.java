package org.expeditee.gui;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.actions.Actions;
import org.expeditee.actions.Misc;
import org.expeditee.actions.Navigation;
import org.expeditee.actions.Simple;
import org.expeditee.agents.ScaleFrameset;
import org.expeditee.io.ItemSelection;
import org.expeditee.items.Circle;
import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Line;
import org.expeditee.items.Text;
import org.expeditee.items.UserAppliedPermission;
import org.expeditee.items.XRayable;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;
import org.expeditee.settings.UserSettings;
import org.expeditee.settings.templates.TemplateSettings;
import org.expeditee.stats.Formatter;
import org.expeditee.stats.Logger;
import org.expeditee.stats.SessionStats;

public class FrameKeyboardActions implements KeyListener {

	private static FrameKeyboardActions _instance = new FrameKeyboardActions();

	protected FrameKeyboardActions() {
	}

	public static FrameKeyboardActions getInstance() {
		return _instance;
	}

	private static Text _toRemove = null;

	private static Collection<Item> _enclosedItems = null;

	public static void resetEnclosedItems() {
		_enclosedItems = null;
	}

	public synchronized void keyTyped(KeyEvent e) {

		if (Simple.isProgramRunning()) {
			if (e.isControlDown()
					&& (e.getKeyChar() == KeyEvent.VK_ESCAPE || e.getKeyChar() == KeyEvent.VK_C)) {
				Simple.stop();
				return;
			} else if (e.isControlDown() && e.getKeyChar() == KeyEvent.VK_SPACE) {
				Simple.nextStatement();
				return;
			} else {
				Simple.KeyStroke(e.getKeyChar());
			}
			if (Simple.consumeKeyboardInput())
				return;
		}

		// ignore escape character and control characters
		if (e.getKeyChar() == KeyEvent.VK_ESCAPE || e.isControlDown()) {
			return;
		}

		// Deal with splitting text items when typing too fast
		// Mike: thinks this problem may have been solved and was due to
		// rounding errors in the text class...
		// It may have been fixed by changing to the use of floats instead of
		// ints for text positioning etc
		// if (FrameMouseActions.isWaitingForRobot()) {
		// System.out.println("Waiting: " + e.getKeyChar());
		// return;
		// }
		e.consume();
		char ch = e.getKeyChar();
		// System.out.println(ch);

		if (e.isAltDown()) {

		} else {
			processChar(ch, e.isShiftDown());
		}
		// FrameGraphics.Repaint();
	}

	public static void processChar(char ch, boolean isShiftDown) {
		Navigation.ResetLastAddToBack();
		Item on = FrameUtils.getCurrentItem();

		// permission check
		if (on != null && !on.hasPermission(UserAppliedPermission.full)) {
			MessageBay
					.displayMessage("Insufficient permission to edit this item");
			return;
		}

		if (_toRemove != null && on != _toRemove) {
			assert (_toRemove.getLength() == 0);
			// This line is to protect mistaken removal of items if there is a
			// bug...
			if (_toRemove.getLength() == 0)
				DisplayIO.getCurrentFrame().removeItem(_toRemove);
		}
		_toRemove = null;

		// ignore delete and backspace if in free space
		if ((on == null || !(on instanceof Text))
				&& (ch == KeyEvent.VK_BACK_SPACE || ch == KeyEvent.VK_TAB || ch == KeyEvent.VK_DELETE))
			return;

		SessionStats.TypedChar(ch);

		// check for dot's being replaced with text
		if (on != null && on instanceof Dot && !(on instanceof WidgetCorner)) {
			if (ch == KeyEvent.VK_BACK_SPACE || ch == KeyEvent.VK_DELETE) {
				return;
			}
			replaceDot((Item) on, ch);
			return;
		}

		// only text can interact with keyboard events
		if (on != null && !(on instanceof Text))
			on = null;

		// DisplayIO.UpdateTitle();

		Text text = (Text) on;
		// if this text is empty but has not been removed (such as from
		// ESC-pushdown)
		if (text != null && text.isEmpty()
				&& (ch == KeyEvent.VK_BACK_SPACE || ch == KeyEvent.VK_DELETE)) {
			if (text.getLines().size() > 0)
				replaceText(text);
			else {
				DisplayIO.setCursor(Item.DEFAULT_CURSOR);
			}
			return;
		}

		// if the user is in free space, create a new text item
		/*
		 * MikeSays: Why do we have to check is highlighted... doing so causes
		 * problems if you type characters to fast, they turn into multiple text
		 * items. ie. JK together on the Linux laptop.
		 */
		if (on == null /* || !on.isHighlighted() */) {
			// DisplayIO.UpdateTitle();
			text = createText(ch);
			text.justify(false);

			FrameUtils.setLastEdited(text);
			DisplayIO.setTextCursor(text, Text.NONE);
			return;
		} else {
			FrameUtils.setLastEdited(text);
		}

		float oldY = FrameMouseActions.MouseY;

		DisplayIO.setTextCursor(text, Text.NONE);
		Point2D.Float newMouse = null;
		if (ch == '\t') {
			if (isShiftDown) {
				newMouse = text.removeTab(ch, DisplayIO.getFloatMouseX(),
						FrameMouseActions.MouseY);
			} else {
				newMouse = text.insertTab(ch, DisplayIO.getFloatMouseX(),
						FrameMouseActions.MouseY);
			}
		} else {
			newMouse = text.insertChar(ch, DisplayIO.getFloatMouseX(),
					FrameMouseActions.MouseY);
		}
		/*
		 * check if the user hit enter and the cursor is now on another text
		 * item
		 */
		if (oldY < newMouse.y) {
			// float diff = newMouse.y - oldY;
			// System.out.print("c");
			Rectangle rect = text.getPolygon().getBounds();

			// Text lastEdited = FrameUtils.getLastEdited();
			// FrameUtils.setLastEdited(null);

			Item justBelow = FrameUtils.onItem(DisplayIO.getCurrentFrame(),
					text.getX() + 10, rect.y + rect.height + 1, false);

			// FrameUtils.setLastEdited(lastEdited);

			// Dont drop unless
			if (justBelow != null && justBelow instanceof Text
					&& justBelow != on) {
				// Drop all the items below it down!
				// Get the list of items that must be dropped
				List<Text> column = DisplayIO.getCurrentFrame().getColumn(on);
				FrameUtils.Align(column, false, 0);
			}
		}

		DisplayIO.setCursorPosition(newMouse.x, newMouse.y, false);

		// This repaint is needed for WINDOWS only?!?!? Mike is not sure why!
		if (ch == KeyEvent.VK_DELETE)
			FrameGraphics.requestRefresh(true);

		// a change has occured to the Frame
		text.getParent().setChanged(true);

		// check that the Text item still exists (hasn't been deleted\backspaced
		// away)
		if (text.isEmpty()) {
			_toRemove = text;

			if (text.hasAction())
				text.setActionMark(true);
			else if (text.getLink() != null)
				text.setLinkMark(true);
			else if (text.getLines().size() > 0)
				replaceText(text);
			else {
				// DisplayIO.getCurrentFrame().removeItem(text);
				DisplayIO.setCursor(Item.DEFAULT_CURSOR);
			}
		}
	}

	public static Text replaceDot(Item dot, char ch) {
		Text text = createText(ch);
		Item.DuplicateItem(dot, text);
		FrameUtils.setLastEdited(text);

		// Copy the lines list so it can be modified
		List<Line> lines = new LinkedList<Line>(dot.getLines());
		for (Line line : lines)
			line.replaceLineEnd(dot, text);
		Frame current = dot.getParentOrCurrentFrame();
		current.removeItem(dot);
		ItemUtils.EnclosedCheck(current.getItems());
		return text;
	}

	/**
	 * Replaces the given text item with a dot
	 */
	public static Item replaceText(Item text) {
		Item dot = new Dot(text.getX(), text.getY(), text.getID());
		Item.DuplicateItem(text, dot);

		List<Line> lines = new LinkedList<Line>();
		lines.addAll(text.getLines());
		if (lines.size() > 0)
			dot.setColor(lines.get(0).getColor());
		for (Line line : lines) {
			line.replaceLineEnd(text, dot);
		}
		text.delete();
		Frame current = text.getParentOrCurrentFrame();
		current.addItem(dot);
		DisplayIO.setCursor(Item.DEFAULT_CURSOR);
		ItemUtils.EnclosedCheck(current.getItems());
		return dot;
	}

	/**
	 * Creates a new Text Item whose text contains the given character. This
	 * method also moves the mouse cursor to be pointing at the newly created
	 * Text Item ready to insert the next character.
	 * 
	 * @param start
	 *            The character to use as the initial text of this Item.
	 * @return The newly created Text Item
	 */
	private static Text createText(char start) {
		Text t = DisplayIO.getCurrentFrame().createBlankText("" + start);

		Point2D.Float newMouse = t.insertChar(start, DisplayIO.getMouseX(),
				FrameMouseActions.getY());
		DisplayIO.setCursorPosition(newMouse.x, newMouse.y, false);

		return t;
	}

	/**
	 * Creates a new Text Item with no text. The newly created Item is a copy of
	 * any ItemTemplate if one is present, and inherits all the attributes of
	 * the Template
	 * 
	 * @return The newly created Text Item
	 */
	private static Text createText() {
		return DisplayIO.getCurrentFrame().createNewText();
	}

	private void move(int direction, boolean isShiftDown, boolean isCtrlDown) {
		Item on = FrameUtils.getCurrentItem();

		if (on == null) {
			navigateFrame(direction);
			return;
		}

		if (on instanceof Text) {
			Text text = (Text) on;
			// When the user hits the left and right button with mouse
			// positions over the the frame name navigation occurs
			if (text.isFrameName()) {
				navigateFrame(direction);
				return;
			} else {
				FrameUtils.setLastEdited(text);
				DisplayIO.setTextCursor(text, direction, false, isShiftDown,
						isCtrlDown, true);
			}
		}
	}

	private void navigateFrame(int direction) {
		switch (direction) {
		case Text.RIGHT:
		case Text.PAGE_UP:
			Navigation.NextFrame(false);
			break;
		case Text.LEFT:
		case Text.PAGE_DOWN:
			Navigation.PreviousFrame(false);
			break;
		case Text.HOME:
		case Text.LINE_HOME:
			Navigation.ZeroFrame();
			break;
		case Text.END:
		case Text.LINE_END:
			Navigation.LastFrame();
			break;
		}
	}

	/**
	 * Receives and processes any Function, Control, and Escape key presses
	 * 
	 * @param e
	 *            The KeyEvent received from the keyboard
	 */
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();

		if (keyCode != KeyEvent.VK_F1 && keyCode != KeyEvent.VK_F2) {
			resetEnclosedItems();
		}

		SessionStats.AddFrameEvent("k" + KeyEvent.getKeyText(keyCode));

		FrameUtils.ResponseTimer.restart();
		// e.consume();

		if (Actions.isAgentRunning()) {
			if (keyCode == KeyEvent.VK_ESCAPE)
				Actions.stopAgent();
			else
				Actions.interruptAgent();
			return;
		} else if (Simple.consumeKeyboardInput()) {
			return;
		}

		if (keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12) {
			functionKey(FunctionKey.values()[keyCode - KeyEvent.VK_F1 + 1], e
					.isShiftDown(), e.isControlDown());
			return;
		} else if (e.isAltDown()) {
			int distance = e.isShiftDown() ? 1 : 20;
			switch (keyCode) {
			case KeyEvent.VK_1:
				FrameMouseActions.leftButton();
				// DisplayIO.clickMouse(InputEvent.BUTTON1_MASK);
				break;
			case KeyEvent.VK_2:
				// DisplayIO.clickMouse(InputEvent.BUTTON2_MASK);
				FrameMouseActions.middleButton();
				break;
			case KeyEvent.VK_3:
				// DisplayIO.clickMouse(InputEvent.BUTTON3_MASK);
				FrameMouseActions.rightButton();
				break;
			case KeyEvent.VK_LEFT:
				DisplayIO.translateCursor(-distance, 0);
				break;
			case KeyEvent.VK_RIGHT:
				DisplayIO.translateCursor(distance, 0);
				break;
			case KeyEvent.VK_UP:
				DisplayIO.translateCursor(0, -distance);
				break;
			case KeyEvent.VK_DOWN:
				DisplayIO.translateCursor(0, distance);
				break;
			}
			return;
		}
		switch (keyCode) {
		case KeyEvent.VK_CONTROL:
			FrameMouseActions.control(e);
			break;
		case KeyEvent.VK_SHIFT:
			FrameMouseActions.shift(e);
			break;
		}

		if (e.isControlDown()) {
			controlChar(e.getKeyCode(), e.isShiftDown());
			return;
		}

		switch (keyCode) {
		case KeyEvent.VK_ESCAPE:
			// Do escape after control so Ctl+Escape does not perform DropDown
			functionKey(FunctionKey.DropDown, e.isShiftDown(), e
					.isControlDown());
			SessionStats.Escape();
			break;
		case KeyEvent.VK_LEFT:
			move(Text.LEFT, e.isShiftDown(), e.isControlDown());
			break;
		case KeyEvent.VK_RIGHT:
			move(Text.RIGHT, e.isShiftDown(), e.isControlDown());
			break;
		case KeyEvent.VK_PAGE_DOWN:
			navigateFrame(Text.PAGE_DOWN);
			break;
		case KeyEvent.VK_PAGE_UP:
			navigateFrame(Text.PAGE_UP);
			break;
		case KeyEvent.VK_UP:
			if (e.isControlDown()) {
				NextTextItem(FrameUtils.getCurrentItem(), false);
			} else {
				move(Text.UP, e.isShiftDown(), e.isControlDown());
			}
			break;
		case KeyEvent.VK_DOWN:
			if (e.isControlDown()) {
				NextTextItem(FrameUtils.getCurrentItem(), true);
			} else {
				move(Text.DOWN, e.isShiftDown(), e.isControlDown());
			}
			break;
		case KeyEvent.VK_END:
			if (e.isControlDown())
				move(Text.END, e.isShiftDown(), e.isControlDown());
			else
				move(Text.LINE_END, e.isShiftDown(), e.isControlDown());
			break;
		case KeyEvent.VK_HOME:
			if (e.isControlDown())
				move(Text.HOME, e.isShiftDown(), e.isControlDown());
			else
				move(Text.LINE_HOME, e.isShiftDown(), e.isControlDown());
			break;
		// TODO remove this when upgrading Java
		// This is a patch because Java6 wont trigger KeyTyped event for
		// Shift+Tab
		case KeyEvent.VK_TAB:
			if (e.isShiftDown()) {
				e.setKeyChar('\t');
				keyTyped(e);
			}
			break;
		}
	}

	/**
	 * Moves the cursor to the next text item on the frame
	 * 
	 * @param currentItem
	 * @param direction
	 *            move up if direction is negative, down if direction is
	 *            positive
	 */
	public static void NextTextItem(Item currentItem, boolean down) {
		// Move the cursor to the next text item
		Frame current = DisplayIO.getCurrentFrame();
		Text title = current.getTitleItem();

		Collection<Text> currentItems = FrameUtils.getCurrentTextItems();
		List<Text> textItems = new ArrayList<Text>();
		// Move to the next text item in the box if
		if (currentItems.contains(currentItem)) {
			textItems.addAll(currentItems);
		} else {
			if (title != null)
				textItems.add(title);
			textItems.addAll(current.getBodyTextItems(true));
		}

		Collections.sort(textItems);

		if (textItems.size() == 0) {
			// If there are no text items on the frame its a NoOp
			if (title == null)
				return;
			if (title != null)
				DisplayIO.MoveCursorToEndOfItem(title);
			FrameGraphics.Repaint();
			return;
		}

		// If the user is mouse wheeling in free space...
		if (currentItem == null) {
			// find the nearest item in the correct direction
			int currY = FrameMouseActions.getY();
			for (int i = 0; i < textItems.size(); i++) {
				Item t = textItems.get(i);
				if (currY < t.getY()) {
					if (down) {
						DisplayIO.MoveCursorToEndOfItem(t);
					} else {
						if (i == 0) {
							DisplayIO.MoveCursorToEndOfItem(current
									.getTitleItem());
						} else {
							DisplayIO.MoveCursorToEndOfItem(textItems
									.get(i - 1));
						}
					}
					FrameGraphics.Repaint();
					return;
				}
			}
			// If we are at the botton of the screen and the user scrolls down
			// then scroll backup to the title
			if (textItems.size() > 0) {
				DisplayIO.MoveCursorToEndOfItem(textItems
						.get(textItems.size() - 1));
			}
			return;
		}

		// Find the current item... then move to the next item
		int i = textItems.indexOf(currentItem);

		int nextIndex = i + (down ? 1 : -1);
		if (nextIndex >= 0 && nextIndex < textItems.size()) {
			DisplayIO.MoveCursorToEndOfItem(textItems.get(nextIndex));
		} else {
			DisplayIO.MoveCursorToEndOfItem(currentItem);
		}
		return;

	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
			FrameMouseActions.control(e);
		} else if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			FrameMouseActions.shift(e);
		} else if (e.isAltDown() || e.isControlDown()) {
			// switch (e.getKeyCode()) {
			// case KeyEvent.VK_1:
			// DisplayIO.releaseMouse(InputEvent.BUTTON1_MASK);
			// break;
			// case KeyEvent.VK_2:
			// DisplayIO.releaseMouse(InputEvent.BUTTON2_MASK);
			// break;
			// case KeyEvent.VK_3:
			// DisplayIO.releaseMouse(InputEvent.BUTTON3_MASK);
			// break;
			// }
		}
	}

	private static void copyItemToClipboard(Item on) {
		if (on == null || !(on instanceof Text))
			return;

		Text text = (Text) on;
		String string = text.copySelectedText();

		if (string == null || string.length() == 0)
			string = text.getText();

		// add the text of the item to the clipboard
		StringSelection selection = new StringSelection(string);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection,
				null);
	}

	/**
	 * Processes all control character keystrokes. Currently Ctrl+C and Ctrl+V
	 * are copy and paste, all other keystrokes are ignored.
	 * 
	 * @param ch
	 *            The character being pressed along with the control key
	 */
	private void controlChar(int key, boolean isShiftDown) {
		Logger.Log(Logger.USER, Logger.CONTROL_CHAR, "User pressing: Ctrl+"
				+ KeyEvent.getKeyText(key));
		//
		// if (FrameUtils.getCurrentItem() == null
		// && !Frame.itemAttachedToCursor()) {
		// Item t = DisplayIO.getCurrentFrame().createNewText(ch + ": ");
		// FrameMouseActions.pickup(t);
		// } else {
		// remove the link for ctrl+l
		Item current = FrameUtils.getCurrentItem();
		Frame currentFrame = DisplayIO.getCurrentFrame();
		int distance = isShiftDown ? 1 : 20;
		switch (key) {
		case KeyEvent.VK_HOME:
			if (current != null && current instanceof Text) {
				move(Text.HOME, isShiftDown, true);
			} else {
				while (DisplayIO.Back())
					;
			}
			break;
		case KeyEvent.VK_END:
			if (current != null && current instanceof Text) {
				move(Text.END, isShiftDown, true);
			} else {
				while (DisplayIO.Forward())
					;
			}
			break;
		case KeyEvent.VK_PAGE_UP:
			DisplayIO.Back();
			break;
		case KeyEvent.VK_PAGE_DOWN:
			DisplayIO.Forward();
			break;
		case KeyEvent.VK_TAB:
			calculateItem(current);
			break;
		case KeyEvent.VK_ESCAPE:
			// Do escape after control so Ctl+Escape does not perform DropDown
			functionKey(FunctionKey.DropDown, isShiftDown, true);
			SessionStats.Escape();
			break;
		case KeyEvent.VK_1:
			FrameMouseActions.leftButton();
			// DisplayIO.clickMouse(InputEvent.BUTTON1_MASK);
			break;
		case KeyEvent.VK_2:
			FrameMouseActions.middleButton();
			// DisplayIO.clickMouse(InputEvent.BUTTON2_MASK);
			break;
		case KeyEvent.VK_3:
			FrameMouseActions.rightButton();
			// DisplayIO.clickMouse(InputEvent.BUTTON3_MASK);
			break;
		case KeyEvent.VK_LEFT:
			if (current instanceof Text) {
				DisplayIO.setTextCursor((Text) current, Text.LEFT, false,
						isShiftDown, true, true);
			} else {
				DisplayIO.translateCursor(-distance, 0);
			}
			break;
		case KeyEvent.VK_RIGHT:
			if (current instanceof Text) {
				DisplayIO.setTextCursor((Text) current, Text.RIGHT, false,
						isShiftDown, true, true);
			} else {
				DisplayIO.translateCursor(distance, 0);
			}
			break;
		case KeyEvent.VK_UP:
			// if (current instanceof Text) {
			NextTextItem(FrameUtils.getCurrentItem(), false);
			// } else {
			// DisplayIO.translateCursor(0, -distance);
			// }
			break;
		case KeyEvent.VK_DOWN:
			// if (current instanceof Text) {
			NextTextItem(FrameUtils.getCurrentItem(), true);
			// } else {
			// DisplayIO.translateCursor(0, distance);
			// }
			break;
		case KeyEvent.VK_L:
			// If its not linked then link it to its self
			if (current instanceof Text && current.getLink() == null) {
				String text = ((Text) current).getText();
				// Ignore the annotation if there is one
				if (text.charAt(0) == '@')
					text = text.substring(1);

				if (FrameIO.isValidFrameName(text)) {
					current.setLink(text);
				} else if (FrameIO.isValidFramesetName(text)) {
					current.setLink(text + '1');
				}
			} else if (current != null) {
				// If its linked remove the link
				current.setLink(null);
			}
			break;
		case KeyEvent.VK_G:
			// If its not linked then link it to its self
			if (current instanceof Text) {
				String text = ((Text) current).getText();
				if (text.charAt(0) == '@')
					text = text.substring(1);

				if (FrameIO.isValidFrameName(text)) {
					current.setLink(text);
				} else if (FrameIO.isValidFramesetName(text)) {
					current.setLink(text + '1');
				}
			}
			if (current != null && current.getLink() != null) {
				Navigation.Goto(current.getAbsoluteLink());
				return;
			}
			break;
		case KeyEvent.VK_A:
			// If its not linked then link it to its self
			if (current instanceof Text) {
				if (!current.hasAction()) {
					String text = ((Text) current).getText().trim();
					// first trim the annotation
					if (text.startsWith("@")) {
						text = text.substring(1).trim();
					}
					// then trim the action
					String lowerCaseText = text.toLowerCase();
					if (lowerCaseText.startsWith("a:")) {
						text = text.substring("a:".length()).trim();
					} else if (lowerCaseText.startsWith("action:")) {
						text = text.substring("action:".length()).trim();

					}
					current.setAction(text);
				} else {
					// If its linked remove the link
					current.setActions(null);
				}
			}
			break;
		case KeyEvent.VK_B:
			if (current instanceof Text) {
				((Text) current).toggleBold();
			}
			break;
		case KeyEvent.VK_I:
			if (current instanceof Text) {
				((Text) current).toggleItalics();
			}
			break;
		case KeyEvent.VK_V:
			ItemSelection.paste();
			return;
		case KeyEvent.VK_C:
			if(FreeItems.itemsAttachedToCursor()) {
				ItemSelection.copyClone();
				return;
			}
			if(current instanceof Text) {
				copyItemToClipboard(current);
			}
			Text item = null;
			// Check if its a line to be turned into a circle
			if (current instanceof Dot && current.getLines().size() == 1) {
				item = replaceDot(current, '@');
			} else if (current instanceof Line
					&& current.getAllConnected().size() == 3) {
				Item end = ((Line) current).getEndItem();
				if (end instanceof Dot) {
					item = replaceDot(end, '@');
				} else if (end instanceof Text) {
					item = (Text) end;
				}
			}
			if (item != null) {
				item.setText("@c");
				DisplayIO.setCursorPosition(item.getX(), item.getY());
				FrameUtils.setLastEdited(null);
				Refresh();
			}
			return;
		case KeyEvent.VK_X:
			ItemSelection.cut();
			return;
		case KeyEvent.VK_M:
			if (current == null)
				return;
			if (current != null && !current.hasPermission(UserAppliedPermission.full)) {
				MessageBay
						.displayMessage("Insufficient permission toggle the items mark");
				return;
			}
			boolean newValue = !(current.getLinkMark() || current
					.getActionMark());
			current.setLinkMark(newValue);
			current.setActionMark(newValue);
			break;
		case KeyEvent.VK_Z:
			if(isShiftDown) {
				DisplayIO.getCurrentFrame().redo();
			} else {
				DisplayIO.getCurrentFrame().undo();
			}
			return;
		case KeyEvent.VK_D:
			// perform a delete operation
			processChar((char) KeyEvent.VK_DELETE, isShiftDown);
			break;
		case KeyEvent.VK_DELETE:
			// perform a delete operation
			FrameMouseActions.delete(current);
			break;
		case KeyEvent.VK_SPACE:
			if (isShiftDown) {
				FrameMouseActions.rightButton();
			} else {
				FrameMouseActions.middleButton();
			}
			break;
		case KeyEvent.VK_F:
			// perform a format operation
			if (isShiftDown) {
				Actions.PerformActionCatchErrors(currentFrame, null, "HFormat");
			} else {
				Actions.PerformActionCatchErrors(currentFrame, null, "Format");
			}
			return;
		case KeyEvent.VK_J:
			Text text = getCurrentTextItem();
			if (text == null) {
				for (Text t : currentFrame.getBodyTextItems(false)) {
					t.justify(true);
				}

				return;
			}

			// if (text.getWidth() < 0)
			// text.setWidth(text.getBoundsWidth() - Item.MARGIN_RIGHT
			// - UserSettings.Gravity);
			text.justify(true);
			break;

		case KeyEvent.VK_R:
			Text textCurrent = getCurrentTextItem();
			if (textCurrent == null) {
				for (Text t : currentFrame.getBodyTextItems(false)) {
					t.setWidth(null);
					t.justify(true);
				}

				return;
			}
			textCurrent.setWidth(null);
			textCurrent.justify(true);
			break;
		case KeyEvent.VK_S:
			/*
			 * Only split when shift is down... it is too easy to accidentally
			 * hit Ctrl+S after completing a paragraph because this is the
			 * shortcut for saving a document in most word processors and text
			 * editors!
			 * 
			 */
			if (!isShiftDown) {
				Save();
				return;
			}
			Text text2 = getCurrentTextItem();
			// split the current text item
			if (text2 == null)
				return;
			List<String> textLines = text2.getTextList();
			if (textLines.size() <= 1)
				return;
			// remove all except the first line of text from the item being
			// split
			text2.setText(textLines.get(0));
			int y = text2.getY();
			for (int i = 1; i < textLines.size(); i++) {
				Text newText = text2.copy();
				newText.setText(textLines.get(i));
				y += newText.getBoundsHeight();
				newText.setY(y);
				// update the items ID to prevent conflicts with the current
				// frame
				newText.setID(currentFrame.getNextItemID());
				currentFrame.addItem(newText);
			}
			break;
		case KeyEvent.VK_ENTER:
			FrameMouseActions.leftButton();
			break;
		case KeyEvent.VK_BACK_SPACE:
			DisplayIO.Back();
			break;
		}
		FrameGraphics.Repaint();
	}

	/**
	 * Gets the currently selected item if the user is allowed to modify it.
	 * 
	 * @return null if the currently selected item is not a Text item that the
	 *         user has permission to modify
	 */
	private static Text getCurrentTextItem() {
		Item item = FrameUtils.getCurrentItem();

		if (item != null && !item.hasPermission(UserAppliedPermission.full)) {
			MessageBay
					.displayMessage("Insufficient permission to copy that item");
			return null;
		}

		Item on = null;
		if (item != null)
			on = item;

		if (on == null || !(on instanceof Text))
			return null;

		return (Text) on;
	}

	public static void functionKey(FunctionKey key, boolean isShiftDown,
			boolean isControlDown) {
		functionKey(key, 1, isShiftDown, isControlDown);
	}

	/**
	 * Called when a Function key has been pressed, and performs the specific
	 * action based on the key.
	 */
	public static void functionKey(FunctionKey key, int repeat,
			boolean isShiftDown, boolean isControlDown) {
		// get whatever the user is pointing at
		Item on = FrameUtils.getCurrentItem();
		String displayMessage = "F" + key.ordinal() + ": " + key.toString();
		// check for enclosed mode
		if (on == null && key.ordinal() < FunctionKey.AudienceMode.ordinal()) {
			Collection<Item> enclosed = FrameUtils.getCurrentItems(on);

			if (enclosed != null && enclosed.size() > 0) {
				// ensure only one dot\line is present in the list
				Collection<Item> lineEnds = FrameUtils.getEnclosingLineEnds();
				Item firstConnected = lineEnds.iterator().next();
				Collection<Item> connected = firstConnected.getAllConnected();

				switch (key) {
				case DropDown:
					// Get the last text item and drop from in
					Item lastText = null;
					for (Item i : enclosed) {
						if (i instanceof Text) {
							lastText = i;
						}
					}
					// Drop from the item if there was a text item in the box
					if (lastText != null) {
						Drop(lastText, false);
					} else {
						// Move to the top of the box
						Rectangle rect = firstConnected.getEnclosedShape()
								.getBounds();
						int newX = rect.x + Text.MARGIN_LEFT;
						int newY = Text.MARGIN_LEFT
								+ rect.y
								+ DisplayIO.getCurrentFrame().getItemTemplate()
										.getBoundsHeight();
						moveCursorAndFreeItems(newX, newY);
						// TODO can resetOffset be put inside
						// moveCursorAndFreeItems
						FrameMouseActions.resetOffset();
					}
					break;
				case SizeUp:
					SetSize(firstConnected, repeat, false, true, isControlDown);
					break;
				case SizeDown:
					SetSize(firstConnected, -repeat, false, true, isControlDown);
					break;
				case ChangeColor:
					if (connected.size() > 0) {
						for (Item d : lineEnds) {
							if (isControlDown)
								SetGradientColor(d, isShiftDown);
							else
								SetFillColor(d, isShiftDown);
							break;
						}
					}
					break;
				case ToggleAnnotation:
					ToggleAnnotation(firstConnected);
					break;
				case ChangeFontStyle:
					ToggleFontStyle(firstConnected);
					break;
				case ChangeFontFamily:
					ToggleFontFamily(firstConnected);
					break;
				case InsertDate:
					AddDate(firstConnected);
					break;
				case Save:
					Save();
					MessageBay.displayMessage(displayMessage);
					break;
				}
				return;
			}
		}
		// Show a description of the function key pressed if the user is in free
		// space and return for the F keys that dont do anything in free space.
		if (on == null) {
			switch (key) {
			// These function keys still work in free space
			case DropDown:
			case InsertDate:
			case XRayMode:
			case AudienceMode:
			case Refresh:
			case Save:
				break;
			case SizeDown:
				zoomFrame(DisplayIO.getCurrentFrame(), 0.909090909f);
				DisplayIO.getCurrentFrame().refreshSize();
				FrameKeyboardActions.Refresh();
				return;
//				if (isControlDown) {
//					UserSettings.ScaleFactor.set(UserSettings.ScaleFactor.get() - 0.05f);
//					Misc.repaint();
//					return;
//				}
			case SizeUp:
				zoomFrame(DisplayIO.getCurrentFrame(), 1.1f);
				DisplayIO.getCurrentFrame().refreshSize();
				FrameKeyboardActions.Refresh();
				return;
//				if (isControlDown) {
//					UserSettings.ScaleFactor.set(UserSettings.ScaleFactor.get() + 0.05f);
//					Misc.repaint();
//					return;
//				}
			default:
				MessageBay.displayMessageOnce(displayMessage);
				return;
			}
		}

		switch (key) {
		case DropDown:
			if (isShiftDown || isControlDown) {
				if (on != null) {
					calculateItem(on);
				}
			}
			Drop(on, false);
			return;
		case SizeUp:
			SetSize(on, repeat, true, false, isControlDown);
			if (on instanceof Text) {
				DisplayIO.setTextCursor((Text) on, Text.NONE, true, false,
						false, true);
			}
			break;
		case SizeDown:
			SetSize(on, -repeat, true, false, isControlDown);
			if (on instanceof Text) {
				DisplayIO.setTextCursor((Text) on, Text.NONE, true, false,
						false, true);
			}
			break;
		case ChangeColor:
			SetColor(on, isShiftDown, isControlDown);
			break;
		case ToggleAnnotation:
			ToggleAnnotation(on);
			break;
		case ChangeFontStyle:
			ToggleFontStyle(on);
			break;
		case ChangeFontFamily:
			ToggleFontFamily(on);
			break;
		case InsertDate:
			AddDate(on);
			return;
		case NewFrameset:
			CreateFrameset(on);
			break;
		case XRayMode:
			FrameGraphics.ToggleXRayMode();
			break;
		case AudienceMode:
			FrameGraphics.ToggleAudienceMode();
			break;
		case Refresh:
			Refresh();
			break;
		case Save:
			Save();
			break;
		}
		on = FrameUtils.getCurrentItem();
		Collection<Item> enclosed = FrameUtils.getCurrentItems(on);
		if (on == null && (enclosed == null || enclosed.size() == 0))
			MessageBay.displayMessage(displayMessage);
	}

	private static void calculateItem(Item toCalculate) {
		if (toCalculate == null)
			return;

		if (!toCalculate.update()) {
			toCalculate.setFormula(null);
			MessageBay.errorMessage("Can not calculate formula ["
					+ toCalculate.getText() + ']');
		}
	}

	private static void Save() {
		Frame current = DisplayIO.getCurrentFrame();
		current.change();
		FrameIO.SaveFrame(current, true, true);
	}

	public static final String DEFAULT_NEW_ITEM_TEXT = "";

	/**
	 * Performs the dropping action: If the cursor is in free space then: the
	 * cursor is repositioned below the last non-annotation text item. If the
	 * cursor is on an item, and has items attached then: the cusor is
	 * positioned below the pointed to item, and the items below are 'pushed
	 * down' to make room.
	 * 
	 * @param toDropFrom
	 *            The Item being pointed at by the mouse, may be null to
	 *            indicate the cursor is in free space.
	 */
	public static boolean Drop(Item toDropFrom, boolean bPasting) {
		try {
			FrameUtils.setLastEdited(null);

			String newItemText = DEFAULT_NEW_ITEM_TEXT;

			// if a line is being rubber-banded, this is a no-op
			if (Frame.rubberbandingLine())
				return false; // No-op

			// if the cursor is in free space then the drop will happen from the
			// last non annotation text item on the frame
			if (toDropFrom == null) {
				toDropFrom = DisplayIO.getCurrentFrame()
						.getLastNonAnnotationTextItem();
			}

			// if no item was found, return
			if (toDropFrom == null) {
				MessageBay.errorMessage("No item could be found to drop from");
				return false;
			}

			if (!(toDropFrom instanceof Text)) {
				MessageBay
						.displayMessage("Only text items can be dropped from");
				return false;
			}

			// Get the list of items that must be dropped
			List<Text> column = DisplayIO.getCurrentFrame().getColumn(
					toDropFrom);

			if (column == null) {
				MessageBay.errorMessage("No column found to align items to");
				return false;
			}

			Item title = DisplayIO.getCurrentFrame().getTitleItem();

			// We wont do auto bulleting when dropping from titles
			if (!bPasting && toDropFrom != title) {
				newItemText = getAutoBullet(((Text) toDropFrom).getFirstLine());
			}

			Text dummyItem = null;
			if (!bPasting && FreeItems.textOnlyAttachedToCursor()) {
				dummyItem = (Text) FreeItems.getItemAttachedToCursor();
				String autoBullet = getAutoBullet(dummyItem.getText());

				if (autoBullet.length() > 0)
					newItemText = "";
				dummyItem.setText(newItemText + dummyItem.getText());
			}

			dummyItem = createText();
			if (FreeItems.textOnlyAttachedToCursor()) {
				Text t = (Text) FreeItems.getItemAttachedToCursor();
				dummyItem.setSize(t.getSize());
				int lines = t.getTextList().size();
				for (int i = 0; i < lines; i++) {
					newItemText += '\n';
				}
			}

			dummyItem.setText(newItemText);

			// If the only item on the frame is the title and the frame name
			// goto the zero frame and drop to the @start if there is one
			// or a fixed amount if there is not
			if (column.size() == 0) {
				Frame current = DisplayIO.getCurrentFrame();
				// Item itemTemplate = current.getItemTemplate();
				int xPos = title.getX() + FrameCreator.INDENT_FROM_TITLE;
				int yPos = FrameCreator.getYStart(title);
				// Check for @start on the zero frame
				Frame zero = FrameIO.LoadFrame(current.getFramesetName() + '0');
				Text start = zero.getAnnotation("start");
				if (start != null) {
					xPos = start.getX();
					yPos = start.getY();
				}

				dummyItem.setPosition(xPos, yPos);
				// DisplayIO.setCursorPosition(xPos, yPos);

				checkMovingCursor(dummyItem);
			} else {
				int yPos = column.get(0).getY() + 1;
				int xPos = column.get(0).getX();
				// Either position the new item below the title or just above
				// the first item below the title
				if (toDropFrom == title && column.get(0) != title) {
					// If dropping from the title position just above top item
					yPos = column.get(0).getY() - 1;

					Frame current = DisplayIO.getCurrentFrame();
					// Check for @start on the zero frame
					Frame zero = FrameIO
							.LoadFrame(current.getFramesetName() + '0');
					Text start = zero.getAnnotation("start");
					if (start != null) {
						yPos = Math.min(yPos, start.getY());
					}
				}
				dummyItem.setPosition(xPos, yPos);
				column.add(dummyItem);
				FrameUtils.Align(column, false, 0);
				// Check if it will be outside the frame area
				if (dummyItem.getY() < 0
						|| dummyItem.getY() > FrameGraphics.getMaxFrameSize()
								.getHeight()) {
					// Check for the 'next' tag!
					Frame current = DisplayIO.getCurrentFrame();
					Item next = current.getAnnotation("next");
					Item prev = current.getAnnotation("previous");
					// Check for an unlinked next tag
					if ((next != null && !next.hasLink())
							|| (prev != null && prev.hasLink())) {
						Frame firstFrame = current;
						if (next != null)
							next.delete();
						FrameCreator frameCreator = new FrameCreator(null);
						// Add the next button
						next = frameCreator.addNextButton(current, null);

						// Create the new frame linked to the next tag
						boolean mouseMoved = FrameMouseActions.tdfc(next);
						Frame moreFrame = DisplayIO.getCurrentFrame();

						// Add previous button to the new frame
						frameCreator.addPreviousButton(moreFrame, firstFrame
								.getName());
						Item first = current.getAnnotation("first");
						if (first != null) {
							frameCreator.addFirstButton(moreFrame, first
									.getLink());
						} else {
							frameCreator.addFirstButton(moreFrame, firstFrame
									.getName());
						}
						// Add the @next if we are pasting
						// if (bPasting) {
						// Item copy = next.copy();
						// copy.setLink(null);
						// moreFrame.addItem(copy);
						// }

						moreFrame.setTitle(firstFrame.getTitleItem().getText());
						// need to move the mouse to the top of the frame if
						// there wasnt an @start on it
						if (!mouseMoved) {
							Item moreTitle = moreFrame.getTitleItem();
							moreTitle.setOverlayPermission(UserAppliedPermission.full);
							Drop(moreTitle, bPasting);
						}
						// Add the bullet text to the item
						dummyItem.setPosition(DisplayIO.getMouseX(),
								FrameMouseActions.getY());
					} else {
						MessageBay
								.warningMessage("Can not create items outside the frame area");
						// ensures correct repainting when items don't move
						DisplayIO.setCursorPosition(DisplayIO.getMouseX(),
								FrameMouseActions.getY());
						return false;
					}
				}
				if (!FreeItems.textOnlyAttachedToCursor()
						&& !dummyItem.isEmpty()) {
					DisplayIO.getCurrentFrame().addItem(dummyItem);
				}

				checkMovingCursor(dummyItem);
			}
			if (dummyItem.getText().length() == 0
					|| FreeItems.itemsAttachedToCursor()) {
				dummyItem.getParentOrCurrentFrame().removeItem(dummyItem);
				dummyItem.setRightMargin(FrameGraphics.getMaxFrameSize().width,
						false);
			} else {
				dummyItem.setWidth(toDropFrom.getWidth());
			}

			DisplayIO.resetCursorOffset();
			FrameGraphics.Repaint();
		} catch (RuntimeException e) {
			// MessageBay.errorMessage(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * @param dummyItem
	 */
	private static void checkMovingCursor(Text dummyItem) {
		// Move the item to the cursor position
		if (FreeItems.itemsAttachedToCursor()) {
			moveCursorAndFreeItems(dummyItem.getX(), dummyItem.getY());
		} else {
			DisplayIO.MoveCursorToEndOfItem(dummyItem);
		}
	}

	/**
	 * @param dummyItem
	 */
	public static void moveCursorAndFreeItems(int x, int y) {
		int oldX = FrameMouseActions.getX();
		int oldY = FrameMouseActions.getY();

		if (oldX == x && oldY == y)
			return;

		DisplayIO.setCursorPosition(x, y);
		Item firstItem = FreeItems.getItemAttachedToCursor();

		if (firstItem == null) {
			firstItem = null;
			return;
		}

		int deltaX = firstItem.getX() - x;
		int deltaY = firstItem.getY() - y;

		for (Item i : FreeItems.getInstance()) {
			i.setPosition(i.getX() - deltaX, i.getY() - deltaY);
		}
	}

	/**
	 * Gets the next letter sequence for a given string to be used in auto
	 * lettering.
	 * 
	 * @param s
	 *            a sequence of letters
	 * @return the next sequence of letters
	 */
	static private String nextLetterSequence(String s) {
		if (s.length() > 1)
			return s;

		if (s.equals("z"))
			return "a";

		return (char) ((int) s.charAt(0) + 1) + "";
	}

	public static String getBullet(String s) {
		return getBullet(s, false);
	}

	public static String getAutoBullet(String s) {
		return getBullet(s, true);
	}

	private static String getBullet(String s, boolean nextBullet) {
		String newItemText = DEFAULT_NEW_ITEM_TEXT;

		if (s == null)
			return newItemText;
		/*
		 * Item i = ItemUtils.FindTag(DisplayIO.getCurrentFrame().getItems(),
		 * "@NoAutoBullets"); if (i != null) return newItemText;
		 */
		// Separate the space at the start of the text item
		String preceedingSpace = "";
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isSpaceChar(s.charAt(i))) {
				preceedingSpace = s.substring(0, i);
				s = s.substring(i);
				break;
			}
		}

		// figure out the type of the text item
		// This allows us to do auto bulleting
		if (s != null && s.length() > 1) {
			// First check for text beginning with * @ # etc
			// These are simple auto bullets
			if (!Character.isLetterOrDigit(s.charAt(0))
					&& !Character.isSpaceChar(s.charAt(0))) {
				if (Text.isBulletChar(s.charAt(0))) {
					int nonSpaceIndex = 1;
					// Find the end of the bullet and space after the bullet
					while (nonSpaceIndex < s.length()
							&& s.charAt(nonSpaceIndex) == ' ') {
						nonSpaceIndex++;
					}
					// we must have a special char followed by >= 1 space
					if (nonSpaceIndex > 1)
						newItemText = s.substring(0, nonSpaceIndex);
				}
				// Auto numbering and lettering
			} else {
				if (Character.isDigit(s.charAt(0))) {
					newItemText = getAutoNumber(s, nextBullet);
					// Auto lettering
				} else if (Character.isLetter(s.charAt(0))) {
					newItemText = getAutoLetter(s, nextBullet);
				}
			}
		}
		return preceedingSpace + newItemText;
	}

	private static boolean isAutoNumberOrLetterChar(char c) {
		return c == ':' || c == '-' || c == '.' || c == ')' || c == '>';
	}

	/**
	 * Gets the string to be used to start the next auto numbered text item.
	 * 
	 * @param s
	 *            the previous text item
	 * @return the beginning of the next auto numbered text item
	 */
	private static String getAutoNumber(String s, boolean nextBullet) {
		String newItemText = DEFAULT_NEW_ITEM_TEXT;

		int nonDigitIndex = 1;
		while (Character.isDigit(s.charAt(nonDigitIndex))) {
			nonDigitIndex++;

			if (nonDigitIndex + 1 >= s.length())
				return DEFAULT_NEW_ITEM_TEXT;
		}

		if (isAutoNumberOrLetterChar(s.charAt(nonDigitIndex))) {

			// we must have a number followed one non letter
			// then one or more spaces
			int nonSpaceIndex = nonDigitIndex + 1;
			while (nonSpaceIndex < s.length() && s.charAt(nonSpaceIndex) == ' ') {
				nonSpaceIndex++;
			}

			if (nonSpaceIndex > nonDigitIndex + 1) {
				if (nextBullet)
					newItemText = (Integer.parseInt(s.substring(0,
							nonDigitIndex)) + 1)
							+ s.substring(nonDigitIndex, nonSpaceIndex);
				else
					newItemText = s.substring(0, nonSpaceIndex);
			}
		}
		return newItemText;
	}

	/**
	 * Gets the string to be used to start the next auto lettered text item.
	 * 
	 * @param s
	 *            the previous text items
	 * @return the initial text for the new text item
	 */
	private static String getAutoLetter(String s, boolean nextBullet) {
		String newItemText = DEFAULT_NEW_ITEM_TEXT;

		int nonLetterIndex = 1;

		if (isAutoNumberOrLetterChar(s.charAt(nonLetterIndex))) {

			// Now search for the next non space character
			int nonSpaceIndex = nonLetterIndex + 1;
			while (nonSpaceIndex < s.length() && s.charAt(nonSpaceIndex) == ' ') {
				nonSpaceIndex++;
			}

			// If there was a space then we have reached the end of our auto
			// text
			if (nonSpaceIndex > nonLetterIndex + 1) {
				if (nextBullet)
					newItemText = nextLetterSequence(s.substring(0,
							nonLetterIndex))
							+ s.substring(nonLetterIndex, nonSpaceIndex);
				else
					newItemText = s.substring(0, nonSpaceIndex);
			}
		}
		return newItemText;
	}
	
	private static boolean refreshAnchors(Collection<Item> items) {
		boolean bReparse = false;
		
		for(Item i : items) {
			Float anchorLeft   = i.getAnchorLeft();
			Float anchorRight  = i.getAnchorRight();
			Float anchorTop    = i.getAnchorTop();
			Float anchorBottom = i.getAnchorBottom();
	
			
			if (anchorLeft != null) {
				i.setAnchorLeft(anchorLeft);
				if (i.hasVector()) {
					bReparse = true;
				}
			}
			
			if (anchorRight != null) {
				i.setAnchorRight(anchorRight);
				if (i.hasVector()) {
					bReparse = true;
				}
			}
			
			if (anchorTop != null) {
				i.setAnchorTop(anchorTop);
				if (i.hasVector()) {
					bReparse = true;
				}
			}
			
			if (anchorBottom != null) {
				i.setAnchorBottom(anchorBottom);
				if (i.hasVector()) {
					bReparse = true;
				}
			}
		}
		return bReparse;
	}
	
	private static void zoomFrame(Frame frame, double scaleFactor) {
		
		int x = FrameMouseActions.getX(), y = FrameMouseActions.getY();
		
		if (frame == null) {
			return;
		}
		
		Collection<Item> items = frame.getVisibleItems();
		
		for(Item item : items) {
			if(item instanceof Text && item.getSize() <= Text.MINIMUM_FONT_SIZE && scaleFactor < 1) {
				return;
			}
		}
		
		for (Vector v : frame.getVectors()) {
			v.Source.scale((float) scaleFactor, x, y);
		}
		
		for (Item item : items) {
			// This line is only needed for circles!!
			// Need to really fix up the way this works!!
			if (item.hasEnclosures())
				continue;
			if (!item.hasPermission(UserAppliedPermission.full))
				continue;
			item.invalidateAll();
			if (!(item instanceof Line)) {
				item.scale((float) scaleFactor, x, y);
			}
		}

		for (Item item : items) {
			if (!item.hasPermission(UserAppliedPermission.full))
				continue;
			// if (!(item instanceof Line))
			item.updatePolygon();

			if (item instanceof Line) {
				((Line) item).refreshStroke(item.getThickness());
			}

			item.invalidateAll();
		}
	}

	/**
	 * Adjusts the size of the given Item, by the given amount. Note: The amount
	 * is relative and can be positive or negative.
	 * 
	 * @param toSet
	 *            The Item whose size is to be adjusted
	 * @param diff
	 *            The amount to adjust the Item's size by
	 * @param moveCursor
	 *            true if the cursor position should be automatically adjusted
	 *            with resizing
	 */
	public static void SetSize(Item item, int diff, boolean moveCursor,
			boolean insideEnclosure, boolean isControlDown) {
		Collection<Item> toSize = new HashSet<Item>();
		Collection<InteractiveWidget> widgets = new HashSet<InteractiveWidget>();
		// the mouse is only moved when the Item is on the frame, not free
		// boolean moveMouse = false;
		Item toSet = null;

		// if the user is not pointing to any item
		if (item == null) {
			if (FreeItems.itemsAttachedToCursor())
				toSize.addAll(FreeItems.getInstance());
			else {
				MessageBay
						.displayMessage("There are no Items selected on the Frame or on the Cursor");
				return;
			}
		} else {
			if (item.isFrameName()) {
				// scale the entire frame
				if(diff != 0) {
					zoomFrame(DisplayIO.getCurrentFrame(), diff > 0 ? 1.1 : 0.909090909);
					DisplayIO.getCurrentFrame().refreshSize();
					FrameKeyboardActions.Refresh();
				}
				// MessageBay.displayMessage("Can not resize the frame name");
				return;
			}
			// check permissions
			if (!item.hasPermission(UserAppliedPermission.full)) {
				Item editTarget = item.getEditTarget();
				if (editTarget != item
						&& editTarget.hasPermission(UserAppliedPermission.full)) {
					item = editTarget;
				} else {
					MessageBay
							.displayMessage("Insufficient permission to change the size of that item");
					return;
				}
			}
			toSet = item;
			// For resizing enclosures pick up everything that is attached to
			// items partly in the enclosure
			// TODO make this only pick up stuff COMPLETELY enclosed... if we
			// change copying to copy only the stuff completely enclosed
			if (insideEnclosure) {
				if (_enclosedItems == null) {
					for (Item i : FrameUtils.getCurrentItems(toSet)) {
						if (i.hasPermission(UserAppliedPermission.full)
								&& !toSize.contains(i))
							toSize.addAll(i.getAllConnected());
					}
					_enclosedItems = toSize;
				} else {
					toSize = _enclosedItems;
				}

			}// Enclosed circle centers are resized with the center as origin
			// Just add the circle center to the list of items to size
			else if (!toSet.hasEnclosures() && !(toSet instanceof Text)
					&& toSet.isLineEnd()) {
				toSize.addAll(toSet.getLines());
			} else if (toSet instanceof Line) {

				Line line = (Line) toSet;

				if (!(toSet instanceof WidgetEdge)
						|| ((WidgetEdge) toSet).getWidgetSource()
								.isWidgetEdgeThicknessAdjustable()) {

					float current = Math.abs(line.getThickness());
					current = Math.max(current + diff, Item.MINIMUM_THICKNESS);
					line.setThickness(current);
					FrameGraphics.Repaint();
					return;

				}

			} else {
				toSize.add(toSet);
			}
		}
		
		// add widgets to notify
		for(Item i : toSize) {
			if(i instanceof WidgetEdge) {
				widgets.add(((WidgetEdge) i).getWidgetSource());
			} else if(i instanceof WidgetCorner) {
				widgets.add(((WidgetCorner) i).getWidgetSource());
			}
		}

		Point2D origin = new Point2D.Float(FrameMouseActions.MouseX,
				FrameMouseActions.MouseY);
		// Inside enclosures increase the size of the enclosure
		double ratio = (100.0 + diff * 2) / 100.0;
		if (insideEnclosure) {
			Collection<Item> done = new HashSet<Item>();
			// adjust the size of all the items
			for (Item i : toSize) {
				if (done.contains(i))
					continue;

				if (i.isLineEnd()) {

					if (!(i instanceof WidgetCorner)
							|| !((WidgetCorner) i).getWidgetSource()
									.isFixedSize()) { // don't size fixed
						// widgets

						Collection<Item> allConnected = i.getAllConnected();
						done.addAll(allConnected);
						for (Item it : allConnected) {
							it.translate(origin, ratio);
							it.setArrowheadLength((float) (it
									.getArrowheadLength() * ratio));
						}
						i.setThickness((float) (i.getThickness() * ratio));
					}
				} else if (i instanceof XRayable) {
					XRayable xRay = (XRayable) i;
					Text source = xRay.getSource();
					// Ensure that the source is done before the XRayable
					if (!done.contains(source)) {
						scaleText(insideEnclosure, origin, ratio, done, source);
					}

					i.translate(origin, ratio);
					i.setThickness((float) (i.getThickness() * ratio));
					done.add(i);
				} else if (i.hasVector()) {
					// TODO Improve the effiency of resizing vectors... ie...
					// dont want to have to reparse all the time
					assert (i instanceof Text);
					Text text = (Text) i;
					AttributeValuePair avp = new AttributeValuePair(text
							.getText());
					double scale = 1F;
					try {
						scale = avp.getDoubleValue();
					} catch (Exception e) {
					}
					scale *= ratio;
					NumberFormat nf = Vector.getNumberFormatter();
					text.setAttributeValue(nf.format(scale));
					text.translate(origin, ratio);
					item.getParent().parse();
				} else if (i instanceof Text) {
					scaleText(insideEnclosure, origin, ratio, done, (Text) i);
				}
			}
			// refresh anchored items
    		if(refreshAnchors(toSize)) {
    			FrameUtils.Parse(DisplayIO.getCurrentFrame(), false);
    		}
			// notify widgets they were resized
			for(InteractiveWidget iw : widgets) {
				iw.onResized();
			}
			FrameGraphics.refresh(true);
			return;
		}

		// adjust the size of all the items
		for (Item i : toSize) {
			// Lines and dots use thickness, not size
			if (i.hasEnclosures()) {
				Circle c = (Circle) i.getEnclosures().iterator().next();
				c.setSize(c.getSize() * (float) ratio);
			} else if (i instanceof Line || i instanceof Circle
					&& !insideEnclosure) {
				float current = Math.abs(i.getThickness());
				current = Math.max(current + diff, Item.MINIMUM_THICKNESS);
				i.setThickness(current);
			} else if (i instanceof Dot) {
				Item dot = (Item) i;
				float current = Math.abs(dot.getThickness());
				current = Math.max(current + diff, Item.MINIMUM_THICKNESS);
				dot.setThickness(current);
			} else if (i.hasVector()) {
				assert (item instanceof Text);
				Text text = (Text) item;
				AttributeValuePair avp = new AttributeValuePair(text.getText());
				double scale = 1F;
				try {
					scale = avp.getDoubleValue();
				} catch (Exception e) {
				}
				scale *= ratio;
				NumberFormat nf = Vector.getNumberFormatter();
				text.setAttributeValue(nf.format(scale));
				text.translate(origin, ratio);
				item.getParent().parse();
			} else {
				float oldSize = Math.abs(i.getSize());
				float newSize = Math
						.max(oldSize + diff, Item.MINIMUM_THICKNESS);
				float resizeRatio = newSize / oldSize;
				// Set size for Picture also translates
				i.setSize(newSize);
				if (i instanceof Text && i.getSize() != oldSize) {
					if (toSize.size() == 1 && !isControlDown) {
						moveCursorAndFreeItems(i.getX(), i.getY());
					} else {
						i.translate(origin, resizeRatio);
						if (i.isLineEnd()) {
							i.setPosition(i.getPosition());
						}
					}
				}
			}
		}

		if (toSet != null)
			toSet.getParent().setChanged(true);
		
		// refresh anchored items
		if(refreshAnchors(toSize)) {
			FrameUtils.Parse(DisplayIO.getCurrentFrame(), false);
		}

		// notify widgets they were resized
		for(InteractiveWidget iw : widgets) {
			iw.onResized();
		}
		FrameGraphics.refresh(true);
	}

	/**
	 * @param origin
	 * @param ratio
	 * @param done
	 * @param source
	 */
	private static void scaleText(boolean insideEnclosure, Point2D origin,
			double ratio, Collection<Item> done, Text source) {
		if (insideEnclosure)
			source.setWidth(Math.round((float) (source.getWidth() * ratio)));
		source.translate(origin, ratio);
		source.setSize((float) (source.getSize() * ratio));
		done.add(source);
	}

	private static void SetFillColor(Item item, boolean setTransparent) {
		if (item == null)
			return;

		if (!item.hasPermission(UserAppliedPermission.full)) {
			MessageBay
					.displayMessage("Insufficient permission to change fill color");
			return;
		}

		Item toSet = item;
		Color color = toSet.getFillColor();
		if (setTransparent)
			color = null;
		else
			color = ColorUtils.getNextColor(color, TemplateSettings.FillColorWheel.get(), toSet
					.getGradientColor());

		// if (color == null) {
		// MessageBay.displayMessage("FillColor is now transparent");
		// }

		toSet.setFillColor(color);
		toSet.getParent().setChanged(true);

		FrameGraphics.Repaint();
	}

	private static void SetGradientColor(Item item, boolean setTransparent) {
		if (item == null)
			return;

		if (!item.hasPermission(UserAppliedPermission.full)) {
			MessageBay
					.displayMessage("Insufficient permission to change gradient color");
			return;
		}

		Item toSet = item;
		Color color = toSet.getGradientColor();
		if (setTransparent)
			color = null;
		else
			color = ColorUtils.getNextColor(color, TemplateSettings.ColorWheel.get(), toSet
					.getFillColor());

		// if (color == null) {
		// MessageBay.displayMessage("FillColor is now transparent");
		// }

		toSet.setGradientColor(color);
		toSet.getParent().setChanged(true);

		FrameGraphics.Repaint();
	}

	/**
	 * Sets the colour of the current Item based on its current colour. The
	 * colours proceed in the order stored in COLOR_WHEEL.
	 * 
	 * @param toSet
	 *            The Item whose colour is to be changed
	 */
	private static void SetColor(Item item, boolean setTransparent,
			boolean setBackgroundColor) {
		// first determine the next color
		Color color = null;
		Frame currentFrame = DisplayIO.getCurrentFrame();
		if (item == null) {
			if (FreeItems.itemsAttachedToCursor()) {
				color = FreeItems.getInstance().get(0).getColor();
			} else {
				return;
			}
			// change the background color if the user is pointing on the
			// frame name
		} else if (item == currentFrame.getNameItem()) {
			// check permissions
			if (!item.hasPermission(UserAppliedPermission.full)) {
				MessageBay
						.displayMessage("Insufficient permission to the frame's background color");
				return;
			}
			if (setTransparent)
				currentFrame.setBackgroundColor(null);
			else
				currentFrame.toggleBackgroundColor();
			// Display a message if the color has changed to transparent
			// if (currentFrame.getBackgroundColor() == null)
			// FrameGraphics
			// .displayMessage("Background color is now transparent");
			FrameGraphics.Repaint();
			return;
		} else {
			// check permissions
			if (!item.hasPermission(UserAppliedPermission.full)) {
				Item editTarget = item.getEditTarget();
				if (editTarget != item
						&& editTarget.hasPermission(UserAppliedPermission.full)) {
					item = editTarget;
				} else {
					MessageBay
							.displayMessage("Insufficient permission to change color");
					return;
				}
			}
			// Toggling color of circle center changes the circle fill color
			if (item.hasEnclosures()) {
				if (setBackgroundColor) {
					SetGradientColor(item.getEnclosures().iterator().next(),
							setTransparent);
				} else {
					SetFillColor(item.getEnclosures().iterator().next(),
							setTransparent);
				}
			} else if (setBackgroundColor) {
				color = item.getPaintBackgroundColor();
			} else {
				color = item.getPaintColor();
			}
		}
		if (setTransparent)
			color = null;
		else if (setBackgroundColor) {
			color = ColorUtils.getNextColor(color, TemplateSettings.FillColorWheel.get(), item
					.getPaintColor());
		} else {
			color = ColorUtils.getNextColor(color, TemplateSettings.ColorWheel.get(),
					currentFrame.getPaintBackgroundColor());
		}
		// if (currentFrame.getPaintForegroundColor().equals(color))
		// color = null;

		// if color is being set to default display a message to indicate that
		// if (color == null) {
		// MessageBay.displayMessage("Color is set to default");
		// }

		if (setBackgroundColor) {
			if (item == null && FreeItems.itemsAttachedToCursor()) {
				for (Item i : FreeItems.getInstance())
					i.setBackgroundColor(color);
			} else {
				item.setBackgroundColor(color);
				item.getParent().setChanged(true);
			}
		} else {
			if (item == null && FreeItems.itemsAttachedToCursor()) {
				for (Item i : FreeItems.getInstance())
					i.setColor(color);
			} else {
				item.setColor(color);
				item.getParent().setChanged(true);
			}
		}
		FrameGraphics.Repaint();
	}

	/**
	 * Toggles the given Item's annotation status on\off.
	 * 
	 * @param toToggle
	 *            The Item to toggle
	 */
	private static void ToggleAnnotation(Item toToggle) {
		if (toToggle == null) {
			MessageBay.displayMessage("There is no Item selected to toggle");
			return;
		}

		// check permissions
		if (!toToggle.hasPermission(UserAppliedPermission.full)) {
			MessageBay
					.displayMessage("Insufficient permission to toggle that item's annotation");
			return;
		}
		toToggle.setAnnotation(!toToggle.isAnnotation());

		toToggle.getParent().setChanged(true);
		FrameGraphics.Repaint();
	}

	/**
	 * Toggles the face style of a text item
	 * 
	 * @param toToggle
	 *            The Item to toggle
	 */
	private static void ToggleFontStyle(Item toToggle) {
		if (toToggle == null) {
			MessageBay.displayMessage("There is no Item selected to toggle");
			return;
		}

		// check permissions
		if (!toToggle.hasPermission(UserAppliedPermission.full)) {
			MessageBay
					.displayMessage("Insufficient permission to toggle that item's annotation");
			return;
		}

		if (toToggle instanceof Text) {
			Text text = (Text) toToggle;
			text.toggleFontStyle();

			text.getParent().setChanged(true);
			FrameGraphics.Repaint();
		}
	}

	/**
	 * Toggles the face style of a text item
	 * 
	 * @param toToggle
	 *            The Item to toggle
	 */
	private static void ToggleFontFamily(Item toToggle) {
		if (toToggle == null) {
			MessageBay.displayMessage("There is no Item selected to toggle");
			return;
		}

		// check permissions
		if (!toToggle.hasPermission(UserAppliedPermission.full)) {
			MessageBay
					.displayMessage("Insufficient permission to toggle that item's annotation");
			return;
		}

		if (toToggle instanceof Text) {
			Text text = (Text) toToggle;
			text.toggleFontFamily();

			text.getParent().setChanged(true);
			FrameGraphics.Repaint();
		}
	}

	/**
	 * If the given Item is null, then a new Text item is created with the
	 * current date If the given Item is not null, then the current date is
	 * prepended to the Item's text
	 * 
	 * @param toAdd
	 *            The Item to prepend the date to, or null
	 */
	public static void AddDate(Item toAdd) {
		String date1 = Formatter.getDateTime();
		String date2 = Formatter.getDate();
		final String leftSeparator = " :";
		final String rightSeparator = ": ";
		String dateToAdd = date1 + rightSeparator;
		boolean prepend = false;
		boolean append = false;

		// if the user is pointing at an item, add the date where ever the
		// cursor is pointing
		if (toAdd != null && toAdd instanceof Text) {
			// permission check
			if (!toAdd.hasPermission(UserAppliedPermission.full)) {
				MessageBay
						.displayMessage("Insufficicent permission to add the date to that item");
				return;
			}

			Text textItem = (Text) toAdd;

			String text = textItem.getText();

			// check if the default date has already been put on this item
			if (text.startsWith(date1 + rightSeparator)) {
				textItem.removeText(date1 + rightSeparator);
				dateToAdd = date2 + rightSeparator;
				prepend = true;
			} else if (text.startsWith(date2 + rightSeparator)) {
				textItem.removeText(date2 + rightSeparator);
				dateToAdd = leftSeparator + date2;
				append = true;
			} else if (text.endsWith(leftSeparator + date2)) {
				textItem.removeEndText(leftSeparator + date2);
				append = true;
				dateToAdd = leftSeparator + date1;
			} else if (text.endsWith(leftSeparator + date1)) {
				textItem.removeEndText(leftSeparator + date1);
				if (textItem.getLength() > 0) {
					dateToAdd = "";
					prepend = true;
				} else {
					// use the default date format
					prepend = true;
				}
			}

			if (prepend) {
				// add the date to the text item
				textItem.prependText(dateToAdd);
				if (dateToAdd.length() == textItem.getLength())
					DisplayIO.setCursorPosition(textItem
							.getParagraphEndPosition());
			} else if (append) {
				textItem.appendText(dateToAdd);
				if (dateToAdd.length() == textItem.getLength())
					DisplayIO.setCursorPosition(textItem.getPosition());
			} else {
				for (int i = 0; i < date1.length(); i++) {
					processChar(date1.charAt(i), false);
				}
			}

			textItem.getParent().setChanged(true);
			FrameGraphics.Repaint();
			// } else {
			// MessageBay
			// .displayMessage("Only text items can have the date prepended to
			// them");
			// }
			// otherwise, create a new text item
		} else {
			Text newText = createText();
			newText.setText(dateToAdd);
			DisplayIO.getCurrentFrame().addItem(newText);
			DisplayIO.getCurrentFrame().setChanged(true);
			FrameGraphics.Repaint();

			DisplayIO.setCursorPosition(newText.getParagraphEndPosition());
		}

	}

	/**
	 * Creates a new Frameset with the name given by the Item
	 * 
	 * @param name
	 */
	private static void CreateFrameset(Item item) {
		if (item == null) {
			MessageBay
					.displayMessage("There is no selected item to use for the frameset name");
			return;
		}

		if (!(item instanceof Text)) {
			MessageBay
					.displayMessage("Framesets can only be created from text items");
			return;
		}

		// dont create frameset if the item is linked
		if (item.getLink() != null) {
			MessageBay
					.displayMessage("A frameset can not be created from a linked item");
			return;
		}

		// check permissions
		if (!item.hasPermission(UserAppliedPermission.full)) {
			MessageBay
					.displayMessage("Insufficient permission to create a frameset from this item");
			return;
		}

		Text text = (Text) item;
		try {
			// create the new frameset
			Frame linkTo = FrameIO.CreateNewFrameset(text.getFirstLine());
			DisplayIO.setCursor(Item.DEFAULT_CURSOR);
			text.setLink(linkTo.getName());
			text.getParent().setChanged(true);
			FrameUtils.DisplayFrame(linkTo, true, true);
			linkTo.moveMouseToDefaultLocation();
			// this needs to be done if the user doesnt move the mouse before
			// doing Tdfc while the cursor is set to the text cursor
			DisplayIO.setCursor(Item.DEFAULT_CURSOR);
		} catch (Exception e) {
			MessageBay.errorMessage(e.getMessage());
		}
	}

	/**
	 * Forces a re-parse and repaint of the current Frame.
	 */
	public static void Refresh() {
		Frame currentFrame = DisplayIO.getCurrentFrame();
		
		if(FrameMouseActions.isShiftDown()) {
			currentFrame.refreshSize();
		}

		// Refresh widgets that use its self as a data source
		currentFrame.notifyObservers(true);

		if (FrameIO.isProfileFrame(currentFrame)) {
			// TODO ensure that users can not delete the first frame in a
			// frameset...
			// TODO handle the case when users manually delete the first frame
			// in a frameset from the filesystem
			Frame profile = FrameIO.LoadFrame(currentFrame.getFramesetName()
					+ "1");
			assert (profile != null);
			FrameUtils.Parse(currentFrame);
			FrameUtils.ParseProfile(profile);
		} else {
			FrameUtils.Parse(currentFrame);
		}
		// Need to update the cursor for when text items change to @b pictures
		// etc and the text cursor is showing
		FrameMouseActions.updateCursor();
		FrameMouseActions.getInstance().refreshHighlights();
		FrameGraphics.ForceRepaint();
	}
}
