package org.expeditee.gui;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.Timer;

import org.expeditee.actions.Actions;
import org.expeditee.actions.Misc;
import org.expeditee.actions.Navigation;
import org.expeditee.io.ExpClipReader;
import org.expeditee.io.ItemSelection;
import org.expeditee.io.ItemSelection.ExpDataHandler;
import org.expeditee.items.Circle;
import org.expeditee.items.Constraint;
import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.Item.HighlightMode;
import org.expeditee.items.ItemAppearence;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Line;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.items.UserAppliedPermission;
import org.expeditee.items.XRayable;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;
import org.expeditee.settings.UserSettings;
import org.expeditee.settings.experimental.ExperimentalFeatures;
import org.expeditee.stats.SessionStats;

public class FrameMouseActions implements MouseListener, MouseMotionListener,
		MouseWheelListener {

	private static int _lastMouseClickModifiers = 0;

	private static MouseEvent _lastMouseDragged;

	private boolean _autoStamp = false;

	private FrameMouseActions() {
	}

	private static FrameMouseActions _instance = null;

	public static FrameMouseActions getInstance() {
		if (_instance == null)
			_instance = new FrameMouseActions();
		return _instance;
	}

	private static final int RECTANGLE_CORNERS = 4;

	// TODO say where/how used
	private static final int MOUSE_WHEEL_THRESHOLD = 2;

	private static final int MINIMUM_RANGE_DEPRESS_TIME = 250;

	private static final int RECTANGLE_TO_POINT_THRESHOLD = 20;

	private static Date _lastMouseClickDate = new Date();

	public static final int LITTLE_MOUSE_PAUSE = 500;

	public static final int ZERO_MOUSE_PAUSE = 0;

	public static final int BIG_MOUSE_PAUSE = 750;

	public static final int CONTEXT_FREESPACE = 0;

	public static final int CONTEXT_AT_TEXT = 1;

	public static final int CONTEXT_AT_LINE = 2;

	public static final int CONTEXT_AT_DOT = 3;

	public static final int CONTEXT_AT_ENCLOSURE = 4;

	public static int _alpha = -1;

	/**
	 * The last known mouse X coordinate
	 */
	public static float MouseX;

	/**
	 * The last known mouse Y coordinate. Relative to the top of the
	 * application.
	 */
	public static float MouseY;

	// Distance of mouse cursor from the origin of the item that was picked up
	// The are used in the move method to calculate the distance moved by the
	// cursor
	private static int _offX;

	private static int _offY;

	// Keeps track of mouse button events when a delete occurs
	private static boolean _isDelete = false;

	// Keeps track of mouse button events when the user extracts attributes
	// occurs
	private static boolean _isAttribute = false;

	/**
	 * A flag to indicate that the last mouseUp event was part of a two button
	 * click sequence hence the next mouse up should be ignored*
	 */
	private static boolean _wasDouble = false;

	private static boolean _isNoOp = false;

	private static boolean _extrude = false;

	// keeps track of the last highlighted Item
	private static Item _lastHighlightedItem = null;

	// keeps track of the item being 'ranged out' if there is one.
	private static Text _lastRanged = null;

	// keeps track of the picture being cropped if there is one
	private static Picture _lastCropped = null;

	// true if lastItem only has highlighting removed when a new item is
	// highlighted
	private static boolean _lastHoldsHighlight = false;

	private static boolean _forceArrowCursor = true;

	// the current context of the cursor
	private static int _context = 0;

	public static void setForceArrow(boolean val) {
		_forceArrowCursor = val;
	}

	public static int getContext() {
		return _context;
	}

	static int _mouseDown = 0;

	private static MouseEvent _lastMouseClick = null;

	private static Item _lastClickedOn = null;

	private static Collection<Item> _lastClickedIn = null;

	private static boolean _pulseOn = false;

	private static final int PULSE_AMOUNT = 2;

	private static Timer _MouseTimer = new Timer(LITTLE_MOUSE_PAUSE,
			new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					// check if we are in free space
					if (_lastClickedOn == null
							&& FreeItems.getInstance().size() == 0) {
						// System.out.println("SuperBack!");
						_MouseTimer.setDelay(ZERO_MOUSE_PAUSE);
						back();
					} else {
						if (FrameUtils.getCurrentItem() == null) {
							// Check if we are toggling arrowhead
							if (FreeItems.getInstance().size() <= 2) {
								for (Item i : FreeItems.getInstance()) {
									if (i instanceof Line) {
										((Line) i).toggleArrow();
									}
								}
								FrameGraphics.Repaint();
							}
						}
						_MouseTimer.stop();
					}
				}
			});

	private static void setPulse(boolean pulseOn) {
		if (_pulseOn == pulseOn) {
			return;
		}
		int amount = PULSE_AMOUNT;
		if (!pulseOn) {
			amount *= -1;
			}
		_pulseOn = pulseOn;

		
		if (_lastClickedOn != null) {
			for (Item i : _lastClickedOn.getAllConnected()) {
				if (i instanceof Line) {
					Line line = (Line) i;
					line.setThickness(line.getThickness() + amount);
				}
			}
		}
		FrameGraphics.Repaint();
	}

	private static Timer _ExtrudeMouseTimer = new Timer(BIG_MOUSE_PAUSE,
			new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					setPulse(true);
					_extrude = true;
					_ExtrudeMouseTimer.stop();
				}
			});

	public void mouseClicked(MouseEvent e) {
	}

	/**
	 * Each Item on the Frame is checked to determine if the mouse x,y
	 * coordinates are on the Item (or within the Shape surrounding it). If the
	 * coordinates are on the Item then the Item is checked for a link, if it
	 * has a link the link is followed, if not, nothing is done.
	 */
	public void mousePressed(MouseEvent e) {
		ProccessMousePressedEvent(e, e.getModifiersEx());
	}

	public void ProccessMousePressedEvent(MouseEvent e, int modifiersEx) {
		// System.out.println("MousePressed " + e.getX() + "," + e.getY() + " "
		// + e.getWhen());

		// TODO WHY DID I NOT COMMENT THIS LINE!! MIKE SAYS
		if (LastRobotX != null) {
			_RobotTimer.stop();
			LastRobotX = null;
			LastRobotY = null;
			mouseMoved(e);
		}
		
		if(ExperimentalFeatures.MousePan.get()) {
    		// don't pan if we're not over the frame
    		_overFrame = FrameUtils.getCurrentItem() == null;
    		_isPanOp = false;
    		// update panning position values so position doesn't jump
    		panStartX = e.getX();
    		panStartY = e.getY();
    		MouseX = panStartX;
    		MouseY = panStartY;
		}

		// System.out.println(modifiersEx);
		if (_mouseDown == 0)
			_lastMouseClickDate = new Date();

		int buttonPressed = e.getButton();
		_mouseDown += buttonPressed;
		_lastClickedOn = FrameUtils.getCurrentItem();
		// load any frame if necessary
		Item on = _lastClickedOn;

		_lastClickedIn = FrameUtils.getCurrentItems(on);
		// if (_lastClickedIn != null){
		// System.out.println(_lastClickedIn.size());}

		/*
		 * This makes it so clicking repeatedly on the frameName doesnt add the
		 * frames to the backup stack. Only the first frame is added to the
		 * backup stack.
		 */
		if (on == null || buttonPressed != MouseEvent.BUTTON1
				|| !on.isFrameName()) {
			Navigation.ResetLastAddToBack();
		}

		SessionStats.MouseClicked(e.getButton());
		if (buttonPressed == MouseEvent.BUTTON1) {
			SessionStats.AddFrameEvent("Ld");
			_extrude = false;
		} else if (buttonPressed == MouseEvent.BUTTON2) {
			SessionStats.AddFrameEvent("Md");
			_extrude = false;
		} else if (buttonPressed == MouseEvent.BUTTON3) {
			SessionStats.AddFrameEvent("Rd");

			// Check if the user picked up a paint brush
			if (FreeItems.getInstance().size() == 1
					&& FreeItems.getItemAttachedToCursor().isAutoStamp()) {
				int delay = (int) (FreeItems.getItemAttachedToCursor()
						.getAutoStamp() * 1000);
				if (delay < 10) {
					_autoStamp = true;
				} else {
					_autoStampTimer.setDelay(delay);
					_autoStampTimer.start();
				}
			}
		}

		// Mike says...
		// For somereason the modifiers for e are different from modifiersEx
		// The SwingUtilities.convertMouseEvent method changes the modifiers
		_lastMouseClick = e;
		_lastMouseClickModifiers = modifiersEx;
		
		/*
		 * Only start the timer when in free space when the user double clicks
		 * to do super back TODO change this so that there are separate timers
		 * for super back and the other Long depress actions if that is what is
		 * wanted.
		 */
		if (_lastClickedOn == null && FreeItems.getInstance().size() == 0) {
			// System.out.println(e.getClickCount());
			if (e.getClickCount() >= 2) {
				_MouseTimer.start();
			}
		} else if (_lastClickedOn != null
				&& FreeItems.getInstance().size() == 0
				&& e.getButton() == MouseEvent.BUTTON3) {
			_ExtrudeMouseTimer.start();

		} else {
			_MouseTimer.start();
		}

		// pre-cache the frame if it is linked

		// If pre-caching is done, it must be done in the background

		// if (on != null && on.getLink() != null && on.isLinkValid()) {
		// FrameIO.Precache(on.getAbsoluteLink());
		// }

		// check for delete command
		if (isDelete(modifiersEx)) {
			_isDelete = true;
			// _lastRanged = null;
			_lastCropped = null;
			_wasDouble = false;
			// check for attributes command
		} else if (isGetAttributes(modifiersEx)) {
			_isAttribute = true;
			_wasDouble = false;
		} else if (isTwoClickNoOp(modifiersEx)) {
			_isAttribute = false;
			_wasDouble = false;
			_isDelete = false;
			_isNoOp = true;
		} else
			_isDelete = false;

		// This must happen before the previous code
		// This is when the user is anchoring something
		if (buttonPressed != MouseEvent.BUTTON1
				&& (_context == CONTEXT_FREESPACE || _context == CONTEXT_AT_ENCLOSURE)
				&& FreeItems.itemsAttachedToCursor()) {
			FrameGraphics.changeHighlightMode(_lastHighlightedItem,
					Item.HighlightMode.None);

			_lastHighlightedItem = FreeItems.getItemAttachedToCursor();
			for (Item i : FreeItems.getInstance()) {
				i.setHighlightColor(Item.DEPRESSED_HIGHLIGHT);
			}
			FrameGraphics.Repaint();
			// this is when the user is picking something up
		} else if (_lastHighlightedItem != null) {
			if (!(_lastHighlightedItem instanceof Line)) {
				_lastHighlightedItem
						.setHighlightColor(Item.DEPRESSED_HIGHLIGHT);
			} else {
				for (Item i : _lastHighlightedItem.getAllConnected()) {
					i.setHighlightColor(Item.DEPRESSED_HIGHLIGHT);
				}
			}
			FrameGraphics.Repaint();
		}

		// if the user is ranging text
		if (on != null && on instanceof Text && !_isDelete) {
			_lastRanged = (Text) on;
			// set start-drag point
			_lastRanged.setSelectionStart(DisplayIO.getMouseX(),
					FrameMouseActions.getY());
		}

		/*
		 * Want to repaint the text with deleteRange color as soon as the second
		 * button is pressed
		 */
		if (_lastRanged != null) {
			_lastRanged.invalidateAll();
			FrameGraphics.requestRefresh(true);
		}

		if (on != null && on instanceof Picture
				&& e.getButton() == MouseEvent.BUTTON3 && !_isDelete) {
			_lastCropped = (Picture) on;
			// set start crop point
			_lastCropped.setStartCrop(DisplayIO.getMouseX(), FrameMouseActions
					.getY());
			_lastCropped.setShowCrop(true);
		}
	}

	// This is where all the processing happens
	public void mouseReleased(MouseEvent e) {

		// System.out.println("Released " + e.getX() + "," + e.getY() + " " +
		// e.getWhen());
		FrameUtils.ResponseTimer.restart();
		_autoStampTimer.stop();
		_autoStamp = false;

		// Auto-hide popups when user clicks into expeditee world
		// If the user clicks into empty space and a popup-is showing, then
		// the user porbably wants to click away the popup - therefore ignore
		// the event
		boolean shouldConsume = PopupManager.getInstance()
				.shouldConsumeBackClick();
		PopupManager.getInstance().hideAutohidePopups();
		if (shouldConsume && e.getButton() == MouseEvent.BUTTON1) {
			return; // consume back click event
		}

		// _lastMovedDistance = new Point(e.getX() - _lastMouseClick.getX(), e
		// .getY()
		// - _lastMouseClick.getY());

		_mouseDown -= e.getButton();
		updateCursor();

		// System.out.println(e.getX() + ", " + e.getY());

		Text lastRanged = _lastRanged;
		_lastRanged = null;
		// Dont do ranging if the user moves really quickly...
		// They are probably trying to pick something up in this case
		if (lastRanged != null) {
			long depressTime = (new Date()).getTime()
					- _lastMouseClickDate.getTime();
			// double changeInDistance =
			// e.getPoint().distance(_currentMouseClick.getPoint());
			// double speed = changeInDistance * 1000 / changeInTime;

			// System.out.println(depressTime);

			if (depressTime < MINIMUM_RANGE_DEPRESS_TIME
					|| lastRanged.getSelectionSize() <= 0) {// Text.MINIMUM_RANGED_CHARS)
				// {
				lastRanged.clearSelection();
				lastRanged = null;
			}
		}

		_ExtrudeMouseTimer.stop();
		_MouseTimer.stop();
		
		setPulse(false);

		// if the last action was a delete, then ignore the next mouseup
		if (_wasDouble) {
			_wasDouble = false;
			return;
		}

		// This code must come after the _wasDouble code...
		// Otherwise get Stopping Agent method after doing the left+right format
		// shortcut
		if (Actions.isAgentRunning()) {
			Actions.stopAgent();
			return;
		}

		/*
		 * if (_isNoOp) { if (e.getButton() != MouseEvent.NOBUTTON) { _isNoOp =
		 * false; _wasDouble = true; // lastRanged.clearSelection();
		 * FrameGraphics.Repaint(); return; } }
		 */

		// get whatever the user was pointing at
		Item clickedOn = _lastClickedOn;
		Collection<Item> clickedIn = _lastClickedIn;

		MouseX = e.getX();
		MouseY = e.getY();

		Item releasedOn = FrameUtils.getCurrentItem();
		Collection<Item> releasedIn = FrameUtils.getCurrentItems(releasedOn);

		// Only a no op if user releases in free space!
		if (_isPanOp || (_isNoOp && (releasedOn == null && releasedIn == null))) {
			if (_isDelete) {
				_isDelete = false;
				_wasDouble = true;
			}

			_isNoOp = false;

			if (_lastHighlightedItem != null)
				FrameGraphics.changeHighlightMode(_lastHighlightedItem,
						Item.HighlightMode.None);

			if (FreeItems.itemsAttachedToCursor()) {
				move(FreeItems.getInstance());
			}

			if (FreeItems.hasCursor()) {
				move(FreeItems.getCursor(), true);
			}

			if(!_isPanOp) {
				MessageBay.displayMessage("Action cancelled, mouse moved more than "
							+ UserSettings.NoOpThreshold.get() + " pixels.");
			}
			FrameGraphics.Repaint();
			return;
		} else {
			_isNoOp = false;
		}

		// if this is a delete command
		if (_isDelete) {
			if (lastRanged != null) {

				Item i = FreeItems.getItemAttachedToCursor();
				if (i != null && i instanceof Text) {
					lastRanged.replaceSelectedText(((Text) i).getText());
					FreeItems.getInstance().clear();
				} else
					lastRanged.cutSelectedText();
				lastRanged.clearSelection();
				FrameGraphics.Repaint();

			} else {
				delete(clickedOn);
			}
			_wasDouble = true;
			_isDelete = false;
			return;
		}

		// if this is an attribute extraction command
		if (_isAttribute) {
			if (clickedOn == null) {
				Frame current = DisplayIO.getCurrentFrame();
				if (isControlDown()) {
					Actions.PerformActionCatchErrors(current, null, "HFormat");
				}
				if (!isControlDown() || isShiftDown()) {
					Actions.PerformActionCatchErrors(current, null, "Format");
				}
			} else {
				extractAttributes(clickedOn);
			}
			// if the user dragged and displayed some cropping with left and
			// right button is a no op for now
			// but later could make this the shrinkTo context
			if (_lastCropped != null) {
				_lastCropped.clearCropping();
				_lastCropped = null;
			}
			_wasDouble = true;
			_isAttribute = false;
			return;
		}

		// if the user is ranging-out text
		if (lastRanged != null && e.getButton() != MouseEvent.BUTTON1) {

			Text ranged;
			if (isShiftDown()) {
				// If shift is down, copy everything (size, color, etc.) except actions, links and data
				ranged = lastRanged.copy();
				ranged.setActions(null);
				ranged.setData((List<String>) null);
				ranged.setLink(null);
			} else {
				// If shift isn't down, don't copy any attributes, but base the new text item on the appropriate template
				ranged = DisplayIO.getCurrentFrame().getItemTemplate(lastRanged.copySelectedText().charAt(0));
			}

			// if the user is cutting text from the item
			if (e.getButton() == MouseEvent.BUTTON2) {
				// Check if the user is trying to range an item for which they
				// do not have permission to do so... or it is the frame name
				if (!lastRanged.hasPermission(UserAppliedPermission.full)
						|| lastRanged.isFrameName()) {
					MessageBay
							.displayMessage("Insufficient permission to cut text");
					lastRanged.clearSelection();
					FrameGraphics.Repaint();
					return;
				}
				// if the entire text is selected and its not a line end then
				// pickup the item
				boolean entireText = lastRanged.getSelectionSize() == lastRanged
						.getLength();
				if (entireText && !lastRanged.isLineEnd()) {
					lastRanged.clearSelection();
					ranged.delete();
					middleButton(clickedOn, clickedIn, e.isShiftDown());
					return;
				} else {
					ranged.setText(lastRanged.cutSelectedText());
					ranged.setWidth(lastRanged.getWidth());
					// If its the whole text then replace last ranged with a dot
					if (entireText) {
						Item dot = FrameKeyboardActions.replaceText(lastRanged);
						dot.setHighlightMode(HighlightMode.None);
					}
				}
				// if the user is copying text from the item
			} else if (e.getButton() == MouseEvent.BUTTON3) {
				// Check if the user is trying to range an item for which they
				// do not have permission to do so... or it is the frame name
				if (!lastRanged.hasPermission(UserAppliedPermission.copy)) {
					MessageBay
							.displayMessage("Insufficient permission to copy text");
					lastRanged.clearSelection();
					FrameGraphics.Repaint();
					return;
				}
				ranged.setText(lastRanged.copySelectedText());
			}

			ranged.setParent(null);
			ranged.setPosition(DisplayIO.getMouseX(), FrameMouseActions.getY());
			pickup(ranged);
			lastRanged.clearSelection();
			lastRanged.setHighlightMode(HighlightMode.None);
			refreshHighlights();
			FrameGraphics.refresh(false);
			return;
		}

		// if the user is cropping an image
		if (clickedOn != null && clickedOn == _lastCropped) {
			if (_lastCropped.isCropTooSmall()) {
				_lastCropped = null;
				// FrameGraphics
				// .WarningMessage("Crop cancelled because it was below the
				// minimum size");
			} else {
				Picture cropped = _lastCropped.copy();
				cropped.setParent(null);
				// move the cropped image to the cursor
				int width = cropped.getWidth();
				int height = cropped.getHeight();
				if(cropped.getSource().getX() + width < MouseX) {
					cropped.getSource().setX(MouseX - width);
				}
				if(cropped.getSource().getY() + height < MouseY) {
					cropped.getSource().setY(MouseY - height);
				}
				pickup(cropped);
				// MIKE put the code below up here
				_lastCropped.clearCropping();
				FrameGraphics.changeHighlightMode(_lastCropped,
						HighlightMode.None);
				_lastCropped = null;
				FrameGraphics.Repaint();
				return;
			}
		}

		assert (_lastCropped == null);
		// if the user has cropped an image, either the above happend or this is
		// a no-op MIKE says WHEN DO WE NEED THE CODE BELOW
		// if (_lastCropped != null && !_lastCropped.isCropTooSmall()) {
		// _lastCropped.clearCropping();
		// _lastCropped = null;
		// FrameGraphics.Repaint();
		// return;
		// }

		// if the user is left-clicking
		if (e.getButton() == MouseEvent.BUTTON1) {
			SessionStats.AddFrameEvent("Lu");
			leftButton(clickedOn, clickedIn, e.isShiftDown(), e.isControlDown());
			return;
		}

		if (e.getButton() == MouseEvent.BUTTON2) {
			SessionStats.AddFrameEvent("Mu");
			middleButton(clickedOn, clickedIn, e.isShiftDown());
			return;
		}

		if (e.getButton() == MouseEvent.BUTTON3) {
			SessionStats.AddFrameEvent("Ru");
			rightButton(clickedOn, clickedIn);
			return;
		}

		// error, we should have returned by now
		System.out.println("Error: mouseReleased should have returned by now. "
				+ e);
	}

	/**
	 * This method handles all left-click actions
	 */
	private void leftButton(Item clicked, Collection<Item> clickedIn,
			boolean isShiftDown, boolean isControlDown) {

		// if the user is pointing at something then either follow the link or
		// do TDFC
		if (clicked == null) {
			// Check if the user is nearby another item...
			int mouseX = DisplayIO.getMouseX();
			int mouseY = FrameMouseActions.getY();
			// System.out.println(mouseX + "," + mouseY);
			for (Item i : DisplayIO.getCurrentFrame().getItems()) {
				if (i instanceof Text) {
					if (i.isNear(mouseX, mouseY)) {
						clicked = i;
						break;
					}
				}
			}
		}

		if (clicked instanceof Text) {
			Text text = (Text) clicked;
			/* Dont follow link when just highlighting text with the left button */
			if (text.getText().length() == 0)
				clicked = null;
			else if (text.getSelectionSize() > 0) {
				return;
			}
		}
		
		// If the user clicked into a widgets free space...
		if (clicked == null && _lastClickedIn != null
				&& _lastClickedIn.size() >= 4) {

			// Check to see if the use clicked into a widgets empty space
			InteractiveWidget iw = null;

			for (Item i : _lastClickedIn) {

				if (i instanceof WidgetCorner) {
					iw = ((WidgetCorner) i).getWidgetSource();
					break;
				} else if (i instanceof WidgetEdge) {
					iw = ((WidgetEdge) i).getWidgetSource();
					break;
				}
			}

			if (iw != null) {

				// Handle dropping items on widgets
				if(iw.ItemsLeftClickDropped()) {
					return;
				}
				
				// Note: musten't directly use source for handling the link
				// because all link operations will by-pass the widgets special
				// handling with links...
				Item widgetLink = iw.getItems().get(0);
				assert (widgetLink != null);
				clicked = widgetLink;
			} else {
				for (Item i : _lastClickedIn) {
					/*
					 * Find the first linked item or the first unlinked Dot This
					 * code assumes that items are are ordered from top to
					 * bottom. TODO make sure the list will always be ordered
					 * correctly!!
					 */
					if (i.hasLink() || i instanceof Dot) {
						clicked = i;
						break;
					}
				}
			}

		}

		if (clicked != null) {
			// check item permissions
			boolean hasLinkOrAction = clicked.hasLink() || clicked.hasAction();

			if ((hasLinkOrAction && !clicked
					.hasPermission(UserAppliedPermission.followLinks))
					|| (!hasLinkOrAction && !clicked
							.hasPermission(UserAppliedPermission.createFrames))) {
				Item editTarget = clicked.getEditTarget();
				if (editTarget != clicked) {
					if (editTarget.hasPermission(UserAppliedPermission.followLinks)) {
						clicked = editTarget;
					} else {
						MessageBay
								.displayMessage("Insufficient permission to perform action on item");
						return;
					}
				}
			}

			Item clickedOn = clicked;

			// actions take priority
			if (_lastMouseClick != null && !_lastMouseClick.isControlDown()
					&& clickedOn.hasAction()) {
				clickedOn.performActions();
				clickedOn.setHighlightMode(HighlightMode.None);
				getInstance().refreshHighlights();
				return;
			} else if (clickedOn.getLink() != null) {
				/*
				 * Dont save the frame if we are moving to an old version of
				 * this frame because everytime we save with the old tag... the
				 * frame is backed up
				 */
				if (!clickedOn.isOldTag())
					FrameIO.SaveFrame(DisplayIO.getCurrentFrame());

				Navigation.setLastNavigationItem(clickedOn);
				load(clickedOn.getAbsoluteLink(), clickedOn.getLinkHistory());
				// DisplayIO.UpdateTitle();
				return;
				// no link is found, perform TDFC
			} else {
				/*
				 * if the user is clicking on the frame name then move to the
				 * next or previous frame regardless of whether or not the frame
				 * is protected
				 */
				if (clickedOn.isFrameName()) {
					if (isControlDown)
						Navigation.PreviousFrame(false);
					else
						Navigation.NextFrame(false);
					return;
				}

				// check for TDFC permission
				if (!clicked.hasPermission(UserAppliedPermission.createFrames)) {
					MessageBay
							.displayMessage("Insufficient permission to TDFC (Top Down Frame Creation) from that item");
					return;
				}

				if (clickedOn.isOldTag())
					return;

				try {
					tdfc(clickedOn);
				} catch (RuntimeException e) {
					e.printStackTrace();
					MessageBay.errorMessage("Top Down Frame Creation (TDFC) error: " + e.getMessage());
				}
				return;
			}

		} else {

			// if user is not pointing at something,this is a back
			if (isShiftDown || isControlDown)
				forward();
			else
				back();

		}
	}

	private static boolean doMerging(Item clicked) {
		if (clicked == null)
			return false;

		// // Brook: widgets do not merge
		// if (clicked instanceof WidgetCorner)
		// return false;
		//
		// // Brook: widgets do not merge
		// if (clicked instanceof WidgetEdge)
		// return false;

		// System.out.println(FreeItems.getInstance().size());
		if (isRubberBandingCorner()) {
			if (clicked.isLineEnd()
					|| clicked.getAllConnected().contains(
							FreeItems.getItemAttachedToCursor())) {
				return true;
			}
		}

		if (FreeItems.getInstance().size() > 2)
			return false;

		Item attachedToCursor = FreeItems.getItemAttachedToCursor();

		if (clicked instanceof Text
				&& !(attachedToCursor instanceof Text || attachedToCursor
						.isLineEnd())) {
			return false;
		}

		return true;
	}

	public static void middleButton() {
		Item currentItem = FrameUtils.getCurrentItem();
		getInstance().middleButton(currentItem,
				FrameUtils.getCurrentItems(currentItem), false);
		updateCursor();
	}

	public static void rightButton() {
		Item currentItem = FrameUtils.getCurrentItem();
		getInstance().rightButton(currentItem,
				FrameUtils.getCurrentItems(currentItem));
		updateCursor();
	}

	public static void leftButton() {
		Item currentItem = FrameUtils.getCurrentItem();
		getInstance().leftButton(currentItem,
				FrameUtils.getCurrentItems(currentItem), false, false);
		updateCursor();
	}

	/**
	 * This method handles all middle-click actions
	 */
	private void middleButton(Item clicked, Collection<Item> clickedIn,
			boolean isShiftDown) {
		
		// If the user clicked into a widgets free space...
		if (clicked == null && _lastClickedIn != null
				&& _lastClickedIn.size() >= 4) {

			// Check to see if the use clicked into a widgets empty space
			InteractiveWidget iw = null;

			for (Item i : _lastClickedIn) {

				if (i instanceof WidgetCorner) {
					iw = ((WidgetCorner) i).getWidgetSource();
					break;
				} else if (i instanceof WidgetEdge) {
					iw = ((WidgetEdge) i).getWidgetSource();
					break;
				}
			}

			if (iw != null) {

				// Handle dropping items on widgets
				if(iw.ItemsMiddleClickDropped()) {
					return;
				}
			}
		}
		// if the cursor has Items attached
		if (FreeItems.itemsAttachedToCursor()) {
			// if the user is pointing at something, merge the items (if
			// possible)
			if (doMerging(clicked)) {
				// check permissions
				if (!clicked.hasPermission(UserAppliedPermission.full)) {
					//Items on the message box have parent == null
					if (clicked.getParent() != null) {
						if (!clicked.isFrameName()) {
							Item editTarget = clicked.getEditTarget();
							if (editTarget != clicked
									&& editTarget
											.hasPermission(UserAppliedPermission.full)) {
								clicked = editTarget;
							} else {
								MessageBay
										.displayMessage("Insufficient permission");
								return;
							}
						}
						
					} else /*Its in the message area*/ {
						MessageBay.displayMessage("Insufficient permission");
						return;
					}
				}
				Item merger = FreeItems.getItemAttachedToCursor();
				assert (merger != null);
				Collection<Item> left = null;
				// when anchoring a line end onto a text line end, holding shift
				// prevents the line ends from being merged
				if (isShiftDown) {
					left = FreeItems.getInstance();
				} else {
					left = merge(FreeItems.getInstance(), clicked);
				}
				Collection<Item> toDelete = new LinkedList<Item>();
				toDelete.addAll(FreeItems.getInstance());
				toDelete.removeAll(left);
				anchor(left);
				FreeItems.getInstance().clear();
				DisplayIO.getCurrentFrame().removeAllItems(toDelete);
				updateCursor();
				// Make sure the dot goes away when anchoring a line end behind
				// a text line end
				if (isShiftDown) {
					refreshHighlights();
				}
				FrameGraphics.requestRefresh(true);
				return;
				// otherwise, anchor the items
			} else {
				if (clickedIn != null && FreeItems.getInstance().size() == 1) {
					Item item = FreeItems.getItemAttachedToCursor();
					if (item instanceof Text) {
						Text text = (Text) item;
						if (AttributeUtils.setAttribute(text, text, 2)) {
							clickedIn.removeAll(FrameUtils
									.getEnclosingLineEnds().iterator().next()
									.getAllConnected());
							for (Item i : clickedIn) {
								AttributeUtils.setAttribute(i, text);
							}
							FreeItems.getInstance().clear();
						}
					}
				}

				// if a line is being rubber-banded, check for auto
				// straightening
				anchor(FreeItems.getInstance());
				FreeItems.getInstance().clear();
				updateCursor();
				_offX = _offY = 0;
				return;
			}
			// otherwise if the user is pointing at something, pick it up unless shift is down
		} else if (clicked != null && !isShiftDown) {
			
			// check permissions
			if (!clicked.hasPermission(UserAppliedPermission.full)) {
				Item editTarget = clicked.getEditTarget();
				if (editTarget != clicked
						&& editTarget.hasPermission(UserAppliedPermission.full)) {
					clicked = editTarget;
				} else {
					MessageBay
							.displayMessage("Insufficient permission to pick up item");
					return;
				}
			}

			// BROOK: WIDGET RECTANGLES DONT ALLOW DISCONNECTION
			if (clicked instanceof Line && !(clicked instanceof WidgetEdge)) {
				// Check if within 20% of the end of the line
				Line l = (Line) clicked;
				Item toDisconnect = l.getEndPointToDisconnect(_lastMouseClick
						.getX(), _lastMouseClick.getY());

				if (toDisconnect == null) {
					pickup(clicked);
				} else {
					if (toDisconnect.getHighlightMode() == Item.HighlightMode.Normal) {
						DisplayIO.setCursorPosition(toDisconnect.getPosition(),
								false);
						pickup(toDisconnect);
					} else {
						List<Line> lines = toDisconnect.getLines();
						// This is to remove constraints from single lines
						// with constraints...
						// ie. partially deleted rectangles
						if (lines.size() == 1) {
							toDisconnect.removeAllConstraints();

							DisplayIO.setCursorPosition(toDisconnect
									.getPosition(), false);
							// This is to ensure the selected mode will be set
							// to Normal rather than disconnect when the line is
							// anchored
							toDisconnect
									.setHighlightMode(Item.HighlightMode.Normal);
							pickup(toDisconnect);
						} else {
							// If we are then detatch the line and pick up its
							// end point...
							Frame currentFrame = DisplayIO.getCurrentFrame();
							Item newPoint = null;

							// If the point we are disconnecting is text...
							// Then we want to leave the text behind
							// And disconnect a point
							if (toDisconnect instanceof Text) {
								newPoint = new Dot(toDisconnect.getX(),
										toDisconnect.getY(), -1);
								Item.DuplicateItem(toDisconnect, newPoint);
							} else {
								newPoint = toDisconnect.copy();
							}

							currentFrame.addItem(newPoint);
							// remove the current item from the connected
							// list for this item
							l.replaceLineEnd(toDisconnect, newPoint);
							// remove unneeded constrains
							newPoint.removeAllConstraints();

							// Set the new points mode to normal before picking
							// it up so it will be restored correctly when
							// anchored
							newPoint
									.setHighlightMode(Item.HighlightMode.Normal);
							toDisconnect
									.setHighlightMode(Item.HighlightMode.None);
							DisplayIO.setCursorPosition(toDisconnect
									.getPosition(), false);
							pickup(newPoint);
							ItemUtils.EnclosedCheck(toDisconnect
									.getParentOrCurrentFrame().getItems());
						}
					}
				}
			} else {
				if (clicked.isLineEnd()) {
					DisplayIO.setCursorPosition(clicked.getPosition(), false);
				}
				pickup(clicked);
			}
			// if we're inside a shape, pick it up unless shift is down
		} else if (clickedIn != null && !isShiftDown) {
			ArrayList<Item> toPickup = new ArrayList<Item>(clickedIn.size());
			for (Item ip : clickedIn)
				if (ip.hasPermission(UserAppliedPermission.full))
					toPickup.add(ip);
			pickup(toPickup);
			// otherwise the user is creating a line
		} else {
			Item on = FrameUtils.onItem(DisplayIO.getCurrentFrame(), Math
					.round(MouseX), Math.round(MouseY), true);
			// If we have permission to copy this item then pick it up
			if (on != null && on.isLineEnd()
					&& on.hasPermission(UserAppliedPermission.full)) {
				on.removeAllConstraints();
				pickup(on);
				return;
			}

			if (on instanceof WidgetEdge) {
				// Don't allow the user to break widget edges.
				// Note: had to return here because random dots would
				// appear otherwise... cannot understand code below
				// with create line.
				return;
			}

			// if its on a line then split the line and put a point on it and
			// pick that point up. Only if it is not a widget line
			if (on instanceof Line && on.hasPermission(UserAppliedPermission.full)) {
				Frame current = DisplayIO.getCurrentFrame();
				// create the two endpoints
				Line oldLine = (Line) on;
				Item newPoint = oldLine.getStartItem().copy();
				newPoint.setPosition(MouseX, MouseY);

				Item end = oldLine.getEndItem();
				// create the Line
				Line newLine = new Line(newPoint, end, current.getNextItemID());
				oldLine.replaceLineEnd(end, newPoint);
				newPoint.removeAllConstraints();
				pickup(newPoint);
				// Update the stats
				Collection<Item> created = new LinkedList<Item>();
				created.add(newPoint);
				created.add(newLine);
				SessionStats.CreatedItems(newLine.getAllConnected());
				return;
			}
			Line newLine = createLine();
			SessionStats.CreatedItems(newLine.getAllConnected());
			return;
		}
		SessionStats.MovedItems(FreeItems.getInstance());
	}

	private static Item getFirstFreeLineEnd() {
		for (Item i : FreeItems.getInstance())
			if (i.isLineEnd())
				return i;
		return null;
	}

	private static boolean isRubberBandingCorner() {
		return getShapeCorner(FreeItems.getInstance()) != null;
	}

	/**
	 * Gets the rectangle corner from the list of items that are part of a
	 * rectangle.
	 * 
	 * @param partialRectangle
	 *            a corner and its two connecting lines.
	 * @return the rectangle corner or null if the list of items is not part of
	 *         a rectangle.
	 */
	private static Item getShapeCorner(List<Item> partialRectangle) {
		if (partialRectangle.size() < 3)
			return null;
		Item lineEnd = null;
		// only one lineEnd will be present for rectangles
		// All other items must be lines
		for (Item i : partialRectangle) {
			if (i.isLineEnd()) {
				if (lineEnd == null) {
					lineEnd = i;
				} else {
					return null;
				}
			} else if (!(i instanceof Line)) {
				return null;
			}
		}
		// if this is at least the corner of two connected lines
		if (lineEnd != null && lineEnd.getAllConnected().size() >= 5)
			return lineEnd;

		return null;
	}

	/**
	 * This method handles all right-click action
	 */
	private void rightButton(Item clicked, Collection<Item> clickedIn) {
		
		// If the user clicked into a widgets free space...
		if (clicked == null && _lastClickedIn != null
				&& _lastClickedIn.size() >= 4) {

			// Check to see if the use clicked into a widgets empty space
			InteractiveWidget iw = null;

			for (Item i : _lastClickedIn) {

				if (i instanceof WidgetCorner) {
					iw = ((WidgetCorner) i).getWidgetSource();
					break;
				} else if (i instanceof WidgetEdge) {
					iw = ((WidgetEdge) i).getWidgetSource();
					break;
				}
			}

			if (iw != null) {

				// Handle dropping items on widgets
				if(iw.ItemsRightClickDropped()) {
					return;
				}
			}
		}
		
		// if the cursor has Items attached, then anchor a copy of them

		List<Item> copies = null;
		if (FreeItems.itemsAttachedToCursor()) {
			if (FreeItems.getInstance().size() == 1
					&& FreeItems.getItemAttachedToCursor().isAutoStamp()) {
				// Dont stamp if the user is painting... because we dont want to
				// save any of the items created!
				return;
				// if the user is clicking on something, merge the items
				// unless it is a point onto somethin other than a lineEnd or a
				// dot
			} else if (clicked != null
			// TODO Change the items merge methods so the logic is simplified
					&& (!(FreeItems.getItemAttachedToCursor() instanceof Dot)
							|| clicked instanceof Dot || clicked.isLineEnd())) {
				// check permissions
				if (!clicked.hasPermission(UserAppliedPermission.full)
						&& clicked.getParent().getNameItem() != clicked) {
					MessageBay
							.displayMessage("Insufficient permission to merge items");
					return;
				}
				if (clicked instanceof Text || clicked instanceof Dot
						|| clicked instanceof XRayable) {
					if (isRubberBandingCorner()) {
						// Move the cursor so that the copy is exactly the
						// same as the shape that was anchored
						DisplayIO.setCursorPosition(clicked.getPosition());
						Item d = getFirstFreeLineEnd();
						// get a copy of all enclosed items before merging
						// lineEnds
						Collection<Item> items = FrameUtils.getItemsEnclosedBy(
								DisplayIO.getCurrentFrame(), d
										.getEnclosedShape());
						// If its not an enclosed shape then pick up the
						// connected shape
						if (items == null || items.size() == 0) {
							items = d.getAllConnected();
						} else {
							// For some reason the item that was clicked ends up
							// in the enclosure and needs to be removed
							items.removeAll(clicked.getConnected());
							// the item that was the origin of the enclosed
							// shape used to create the enclosure does not get
							// returned from getItemsEnclosedBy to the enclosure
							// so it must be added
							items.addAll(d.getConnected());
						}

						Collection<Item> toCopy = new LinkedHashSet<Item>();

						for (Item ip : items) {
							if (ip.hasPermission(UserAppliedPermission.copy))
								toCopy.add(ip);
						}
						copies = copy(toCopy);
						// Now do the merging
						Collection<Item> remain = merge(
								FreeItems.getInstance(), clicked);
						// anchor the points
						anchor(remain);
						FreeItems.getInstance().clear();
						pickup(copies);
						// line onto something
					} else if (FreeItems.getInstance().size() == 2
					/* && clicked instanceof XRayable */) {
						copies = ItemUtils.UnreelLine(FreeItems.getInstance(),
								_controlDown);
						Collection<Item> leftOver = merge(FreeItems
								.getInstance(), clicked);
						anchor(leftOver);
						if (copies == null)
							copies = copy(FreeItems.getInstance());
						FreeItems.getInstance().clear();
						for (Item i : copies)
							i.setOffset(0, 0);
						// need to move to prevent cursor dislocation
						move(copies);
						pickup(copies);
						// point onto point
					} else if (FreeItems.getInstance().size() == 1) {
						copies = copy(FreeItems.getInstance());
						Collection<Item> remain = merge(copies, clicked);

						// ignore items that could not be merged.
						anchor(remain);
					} else {
						stampItemsOnCursor(true);
						copies = FreeItems.getInstance();
					}
				} else {
					copies = ItemUtils.UnreelLine(FreeItems.getInstance(),
							_controlDown);
					if (copies == null)
						copies = copy(FreeItems.getInstance());
					for (Item i : copies) {
						i.setOffset(0, 0);
					}
					anchor(FreeItems.getInstance());
					FreeItems.getInstance().clear();
					pickup(copies);
				}
				// otherwise, anchor the items
			} else {
				// check if this is anchoring a rectangle
				if (isRubberBandingCorner()) {
					Item d = getFirstFreeLineEnd();
					// anchor the points
					anchor(FreeItems.getInstance());
					FreeItems.getInstance().clear();
					updateCursor();
					// pick up a copy of all enclosed items
					Collection<Item> enclosedItems = FrameUtils
							.getItemsEnclosedBy(DisplayIO.getCurrentFrame(), d
									.getEnclosedShape());
					if (enclosedItems != null) {
						enclosedItems.removeAll(d.getAllConnected());
						Collection<Item> toCopy = getFullyEnclosedItems(enclosedItems);

						if (toCopy.size() > 0) {
							// Find the closest item to the mouse cursor
							double currentX = DisplayIO.getMouseX();
							double currentY = FrameMouseActions.getY();
							Item closest = null;
							double shortestDistance = Double.MAX_VALUE;
							for (Item next : toCopy) {
								if (next instanceof Line)
									continue;
								double distance = Point.distance(currentX,
										currentY, next.getX(), next.getY());
								if (distance < shortestDistance) {
									shortestDistance = distance;
									closest = next;
								}
							}
							// Move the cursor to closest item
							DisplayIO.setCursorPosition(closest.getPosition());
							// Pickup copy of the stuff inside the rectange
							copies = copy(toCopy);
							pickup(copies);
							// Remove the rectangle
							d.getParentOrCurrentFrame().removeAllItems(
									d.getAllConnected());
						} else {
							// Pick up a copy of the rectangle
							copies = copy(d.getAllConnected());
							pickup(copies);
						}
					}
				} else {
					if (rubberBanding()) {
						if (clicked != null) {
							Collection<Item> leftOver = merge(FreeItems
									.getInstance(), clicked);
							anchor(leftOver);
						}
						// This is executed when the user is putting down a line
						// endpoint and unreeling. ie. Normal unreeling
						copies = ItemUtils.UnreelLine(FreeItems.getInstance(),
								_controlDown);

						if (copies == null)
							copies = copy(FreeItems.getInstance());
						anchor(FreeItems.getInstance());
						for (Item i : copies)
							i.setOffset(0, 0);
						// need to move to prevent cursor dislocation
						move(copies);
						pickup(copies);
					} else if (_extrude) {
						List<Item> originals = new ArrayList<Item>();
						// remove any lines that dont have both endpoints
						// floating
						for (Item i : FreeItems.getInstance()) {
							if (i.isFloating())
								originals.add(i);
						}
						if (copies == null)
							copies = ItemUtils.CopyItems(originals, _extrude);
						for (Item i : copies)
							i.setOffset(0, 0);
						anchor(FreeItems.getInstance());
						// Move isnt working right for extruding!!
						// move(copies);
						pickup(copies);
					} else {
						stampItemsOnCursor(true);
						copies = FreeItems.getInstance();
					}
				}
			}
		} else {
			// if the user is pointing at something and shift isn't down, make a copy
			if (clicked != null && !isShiftDown()) {
				// check permissions
				if (clicked.isLineEnd()) {
					if (!clicked.hasPermission(UserAppliedPermission.full)) {
						MessageBay
								.displayMessage("Insufficient permission to unreel");
						return;
					}
				} else if (!clicked.hasPermission(UserAppliedPermission.copy)) {
					Item editTarget = clicked.getEditTarget();
					if (editTarget != clicked
							&& editTarget.hasPermission(UserAppliedPermission.copy)) {
						clicked = editTarget;
					} else {
						MessageBay
								.displayMessage("Insufficient permission to copy");
						return;
					}
				}

				copies = ItemUtils.UnreelLine(clicked, _controlDown);
				// Copies will NOT be null if the user right clicked on a point
				if (copies == null) {
					Collection<Item> originals = clicked.getConnected();
					copies = ItemUtils.CopyItems(originals, _extrude);
					// if this is the title of the frame, link it to the frame
					if (originals.size() == 1 && copies.size() == 1) {
						Item copy = copies.get(0);
						Item original = originals.iterator().next();
						if (original.getLink() == null
								&& original.isFrameTitle()) {
							// save the frame after copying
							// i.getParent().setChanged(true);
							copy.setLink(original.getParentOrCurrentFrame()
									.getName());
						}
					}

					FrameGraphics.changeHighlightMode(clicked,
							HighlightMode.None);

					if (!_extrude)
						clearParent(copies);
				}

				pickup(copies);
			} else {
				// if user is pointing in a closed shape and shift isn't down, make a copy of the items inside
				if (clickedIn != null && !isShiftDown()) {
					// Set the selection mode for the items that were clicked in
					Collection<Item> enclosed = getFullyEnclosedItems(clickedIn);
					if (enclosed.size() == 0) {
						MessageBay
								.displayMessage("Insufficient permission to copy items");
					} else {
						copies = copy(enclosed);
						clearParent(copies);
						pickup(copies);
						for (Item i : clickedIn) {
							i.setHighlightMode(HighlightMode.None);
						}
					}
					// otherwise, create a rectangle
				} else {
					Item on = FrameUtils.onItem(DisplayIO.getCurrentFrame(),
							MouseX, MouseY, true);
					// if its on a line then create a line from that line
					if (on instanceof Line && on.hasPermission(UserAppliedPermission.full)) {

						Line onLine = (Line) on;
						Line newLine = onLine.copy();
						Item end = newLine.getEndItem();
						Item start = newLine.getStartItem();
						end.setPosition(MouseX, MouseY);
						start.setPosition(MouseX, MouseY);
						onLine.autoArrowheadLength();
						// anchor the start
						anchor(start);
						// attach the line to the cursor
						pickup(end);

						List<Item> toMerge = new LinkedList<Item>();
						toMerge.add(newLine.getStartItem());
						toMerge.add(newLine);

						// Make sure the highlighting is shown when the end is
						// anchored
						end.setHighlightMode(Item.HighlightMode.Normal);
						merge(toMerge, on);
						// anchor(left);
						// FreeItems.getInstance().clear();
						FrameGraphics.Repaint();
						updateCursor();
						return;
					}

					copies = new ArrayList<Item>();
					Item[] d = new Item[RECTANGLE_CORNERS];
					// create dots
					Frame current = DisplayIO.getCurrentFrame();
					for (int i = 0; i < d.length; i++) {
						d[i] = current.createDot();
						copies.add(d[i]);
					}

					current.nextDot();

					// create lines
					copies.add(new Line(d[0], d[1], current.getNextItemID()));
					copies.add(new Line(d[1], d[2], current.getNextItemID()));
					copies.add(new Line(d[2], d[3], current.getNextItemID()));
					copies.add(new Line(d[3], d[0], current.getNextItemID()));

					new Constraint(d[0], d[1], current.getNextItemID(),
							Constraint.HORIZONTAL);
					new Constraint(d[2], d[3], current.getNextItemID(),
							Constraint.HORIZONTAL);
					new Constraint(d[1], d[2], current.getNextItemID(),
							Constraint.VERTICAL);
					new Constraint(d[3], d[0], current.getNextItemID(),
							Constraint.VERTICAL);

					anchor(new ArrayList<Item>(copies));
					pickup(d[3]);
					d[3].setHighlightMode(HighlightMode.Normal);

					SessionStats.CreatedItems(copies);
					copies.clear();
				}
			}
		}
		getInstance().refreshHighlights();
		SessionStats.CopiedItems(copies);
		updateCursor();
		FrameGraphics.Repaint();
	}

	/**
	 * 
	 */
	private static void stampItemsOnCursor(boolean save) {
		List<Item> copies = copy(FreeItems.getInstance());
		// MIKE: what does the below 2 lines do?
		for (Item i : copies) {
			i.setOffset(0, 0);
			i.setSave(save);
		}
		// The below code has a little problem withflicker when stamp
		// and dragging
		move(FreeItems.getInstance());
		for (Item i : copies) {
			i.setHighlightMode(HighlightMode.None);
		}
		anchor(copies);
	}

	/**
	 * @param enclosedItems
	 * @return
	 */
	private static Collection<Item> getFullyEnclosedItems(
			Collection<Item> enclosure) {
		// copy the enclosedItems because the list will be modified
		Collection<Item> enclosedItems = new LinkedHashSet<Item>(enclosure);
		Collection<Item> toCopy = new LinkedHashSet<Item>(enclosedItems.size());

		while (enclosedItems.size() > 0) {
			Item i = enclosedItems.iterator().next();
			if (i.hasPermission(UserAppliedPermission.copy)) {
				Collection<Item> items = i.getAllConnected();
				// Only copy if the entire shape is enclosed
				if (enclosedItems.containsAll(items)) {
					toCopy.addAll(items);
				}
				enclosedItems.removeAll(items);
			} else {
				enclosedItems.remove(i);
			}
		}
		return toCopy;
	}

	/**
	 * Marks the items as not belonging to any specific frame. When picking up
	 * items the parent will be automatically cleared for items on the current
	 * frame but not for overlay items. This method ensures that overlay items
	 * will also be cleared. This is useful when picking up copies of items from
	 * an overlay (with the right mouse button) to ensure that the copy will be
	 * anchored on the current frame rather than the overlay. When items are
	 * picked up with the middle button clearParent should NOT be called.
	 * 
	 * @param items
	 *            to have their parent cleared
	 */
	private static void clearParent(List<Item> items) {
		for (Item i : items) {
			// The next line is only necessary for circles...
			// Need to clean up/refactory some of this stuff
			i.getParentOrCurrentFrame().removeItem(i);
			i.setParent(null);
		}
	}

	private static boolean inWindow = false;
	/**
	 * event called when mouse exits window
	 * (can't use MouseListener callback since that callback doesn't
	 *  correctly receive all mouse exit events) 
	 */
	public static void mouseExitedWindow(MouseEvent e) {
		inWindow = false;
		// System.out.println("Left window");
		if(FreeItems.itemsAttachedToCursor()) {
			boolean cut = true;
			for(Item i : FreeItems.getInstance()) {
				for(Item j : i.getAllConnected()) {
					if(!FreeItems.getInstance().contains(j)) {
						cut = false;
						break;
					}
				}
			}
			if(cut) {
				ItemSelection.cut();
			}
		}
	}
	
	public void mouseEntered(MouseEvent e) {
		// check if we are entering the window from outside of the window, or if we were just over a widget
		if(!inWindow) {
			inWindow = true;
			// if there is expeditee data on the clipboard that has not yet been autoPasted, autoPaste it
    		Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
    		Transferable content = c.getContents(null);
    		try {
    			if(content.isDataFlavorSupported(ItemSelection.expDataFlavor)) {	// Expeditee data
    				ExpDataHandler expData = (ExpDataHandler)content.getTransferData(ItemSelection.expDataFlavor);
    				if(expData.autoPaste) {
        				List<Item> items = new ExpClipReader(FrameMouseActions.getX(), FrameMouseActions.getY()).read(expData.items);
       					// generate new IDs and pickup
       					FrameMouseActions.pickup(ItemUtils.CopyItems(items));
       					// update the clipboard contents so they won't be autoPasted again
       					expData.autoPaste = false;
       					String stringData = "";
       					Image imageData = null;
       					if(content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
       						stringData = (String) content.getTransferData(DataFlavor.stringFlavor);
       					}
       					if(content.isDataFlavorSupported(DataFlavor.imageFlavor)) {
       						imageData = (Image) content.getTransferData(DataFlavor.imageFlavor);
       					}
          				c.setContents(new ItemSelection(stringData, imageData, expData), null);
    				}
    			}
    		} catch(Exception ex) {
    			ex.printStackTrace(); 
    		}
		}
	}

	public void mouseExited(MouseEvent e) {
	}

	private boolean _overFrame;
	private int panStartX, panStartY;
	private boolean _isPanOp;
	public void mouseDragged(MouseEvent e) {
		_lastMouseDragged = e;
		// System.out.println("MouseDragged");

		// Stop the longDepress mouse timer if the user drags above a threshold
		if (_MouseTimer.isRunning()) {
			if (Math.abs(e.getX() - _lastMouseClick.getX())
					+ Math.abs(e.getY() - _lastMouseClick.getY()) > 10)
				_MouseTimer.stop();
		}

		if (_autoStamp) {
			stampItemsOnCursor(false);
		}

		/*
		 * Have the free items follow the cursor if the user clicks in freespace
		 * then moves.
		 */
		if (FreeItems.getInstance().size() > 0 && _lastClickedOn == null) {
			mouseMoved(e);
			return;
		}
		
		// panning the frame when dragging the mouse while shift-leftclicking
		if(ExperimentalFeatures.MousePan.get() && _overFrame && e.isShiftDown() &&
				(e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0 &&
				(_isPanOp || (Math.max(Math.abs(panStartX - e.getX()), Math.abs(panStartY - e.getY())) > 5))) {
			int dX = (int) (e.getX() - MouseX);
			int dY = (int) (e.getY() - MouseY);
			Misc.pan(DisplayIO.getCurrentFrame(), dX, dY);
			MouseX = e.getX();
			MouseY = e.getY();
			_isPanOp = true;
		}

		// check if user is dragging across a text item
		if (_lastRanged != null) {
			// System.out.println(MouseY - e.getY());

			MouseX = e.getX();
			MouseY = e.getY();

			int distance = _lastRanged.getY() - FrameMouseActions.getY();
			if (distance <= 0)
				distance = FrameMouseActions.getY() - _lastRanged.getY()
						- _lastRanged.getBoundsHeight();

			if (distance > UserSettings.NoOpThreshold.get()) {
				_lastRanged.clearSelectionEnd();
				_isNoOp = true;
			} else {
				// update the ranged section
				_lastRanged.setSelectionEnd(DisplayIO.getMouseX(),
						FrameMouseActions.getY());
				_isNoOp = false;
			}

			DisplayIO.setTextCursor(_lastRanged, Text.NONE, false, e
					.isShiftDown(), e.isControlDown(), false);
			FrameGraphics.Repaint();
			return;
		}

		// if the user is dragging across a picture
		if (_lastCropped != null) {
			// If shift is down then the distance moved is the same in the x and
			// y
			MouseX = e.getX();
			MouseY = e.getY();

			if (e.isControlDown()) {
				int deltaX = Math.abs(e.getX() - _lastMouseClick.getX());
				int deltaY = Math.abs(e.getY() - _lastMouseClick.getY());
				if (deltaX > deltaY) {
					MouseY = _lastMouseClick.getY() + deltaX
							* (e.getY() > _lastMouseClick.getY() ? 1 : -1);
				} else {
					MouseX = _lastMouseClick.getX() + deltaY
							* (e.getX() > _lastMouseClick.getX() ? 1 : -1);
				}
			}
			// update the ranged section
			_lastCropped.setEndCrop(DisplayIO.getMouseX(), FrameMouseActions
					.getY());

			FrameGraphics.Repaint();
			return;
		}

		/*
		 * This is the context of a user clicking in freespace an dragging onto
		 * the edge of a line
		 */
		if ((_mouseDown == MouseEvent.BUTTON2 || _mouseDown == MouseEvent.BUTTON3)
				&& _lastClickedOn == null && _lastClickedIn == null) {
			Item on = FrameUtils.onItem(DisplayIO.getCurrentFrame(), e.getX(),
					e.getY(), true);

			if (FreeItems.getInstance().size() == 0) {
				// if the user can spot-weld, show the virtual spot
				if (on instanceof Line) {
					Line line = (Line) on;
					line.showVirtualSpot(e.getX(), e.getY());
				}
				if (on != null && on.isLineEnd()) {
					_lastHighlightedItem = on;
					on.setHighlightMode(Item.HighlightMode.Normal);
				} else if (_lastHighlightedItem != null) {
					_lastHighlightedItem
							.setHighlightMode(Item.HighlightMode.None);
					_lastHighlightedItem = null;
				}
			}
		}

		// Use the below calculation for better speed. If it causes problems
		// switch back to the Euclidean distance calculation
		if (Math.abs(MouseX - e.getX()) > UserSettings.NoOpThreshold.get()
				|| Math.abs(MouseY - e.getY()) > UserSettings.NoOpThreshold.get())
			_isNoOp = true;

		FrameGraphics.Repaint();
	}

	private static MouseEvent _lastMouseMoved = null;

	private static Integer LastRobotX = null;

	private static Integer LastRobotY = null;

	// For some reason... sometimes the mouse move gets lost when moving the
	// mouse really quickly after clicking...
	// Use this timer to make sure it gets reset eventually if the Robot
	// generated event never arrives.
	private static Timer _RobotTimer = new Timer(200, new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
			_RobotTimer.stop();
			LastRobotX = null;
			LastRobotY = null;
			// System.out.println("RobotTimer");
		}
	});

	private static Timer _autoStampTimer = new Timer(200, new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
			stampItemsOnCursor(false);
		}
	});

	private static boolean _controlDown;

	private static boolean _shiftDown;
	
	private static OnNewFrameAction _onFrameAction = null;
	
	public static void setTDFCAction(OnNewFrameAction action) {
	    _onFrameAction = action;
	}

	public static boolean isControlDown() {
		return _controlDown;
	}

	public static boolean isShiftDown() {
		return _shiftDown;
	}

	public static void setLastRobotMove(float x, float y) {
		// Make sure the system is in the right state while waiting for the
		// Robots event to arrive.
		MouseX = x;
		MouseY = y;
		// System.out.println("MouseMoved: " + MouseX + "," + MouseY + " " +
		// System.currentTimeMillis());
		LastRobotX = Math.round(x);
		LastRobotY = Math.round(y);
		_RobotTimer.start();
	}

	public static boolean isWaitingForRobot() {
		return LastRobotX != null;
	}

	/**
	 * Updates the stored mouse position and highlights any items as necessary.
	 */
	public void mouseMoved(MouseEvent e) {
		mouseMoved(e, false);
	}

	private void mouseMoved(MouseEvent e, boolean shiftStateChanged) {
		// System.out.println("mouseMoved");
		// System.out.println(_context);
		if (_context == CONTEXT_FREESPACE)
			FrameKeyboardActions.resetEnclosedItems();
		// System.out.println(e.getX() + "," + e.getY() + " " + e.getWhen());
		if (LastRobotX != null) {
			// Wait until the last Robot mouse move event arrives before
			// processing other events
			if (/* FreeItems.getInstance().size() == 0 || */
			(LastRobotX == e.getX() && LastRobotY == e.getY())) {
				LastRobotX = null;
				LastRobotY = null;
				_RobotTimer.stop();
			} else {
				// System.out.println("Ignored: " +
				// FreeItems.getInstance().size());
				return;
			}
		}

		MouseX = e.getX();
		MouseY = e.getY();
		
		// System.out.println(MouseX + "," + MouseY);

		// Moving the mouse a certain distance removes the last edited text if
		// it is empty
		Text lastEdited = FrameUtils.getLastEdited();
		if (lastEdited != null && lastEdited.getText().length() == 0
				&& lastEdited.getPosition().distance(e.getPoint()) > 20) {
			FrameUtils.setLastEdited(null);
		}

		// If shift is down then the movement is constrained
		if (_controlDown && FreeItems.getInstance().size() > 0) {
			// Check if we are rubber banding a line
			if (shiftStateChanged && rubberBanding()) {
				// Get the line end that is being rubber banded
				Item thisEnd = FreeItems.getInstance().get(0).isLineEnd() ? FreeItems
						.getInstance().get(0)
						: FreeItems.getInstance().get(1);
				Line line = (Line) (FreeItems.getInstance().get(0).isLineEnd() ? FreeItems
						.getInstance().get(1)
						: FreeItems.getInstance().get(0));
				Item otherEnd = line.getOppositeEnd(thisEnd);
				int deltaX = Math.abs(e.getX() - otherEnd.getX());
				int deltaY = Math.abs(e.getY() - otherEnd.getY());
				// Check if its a vertical line
				if (deltaX < deltaY / 2) {
					// otherEnd.setX(thisEnd.getX());
					// MouseX = otherEnd.getX();
					if (shiftStateChanged) {
						new Constraint(thisEnd, otherEnd, thisEnd
								.getParentOrCurrentFrame().getNextItemID(),
								Constraint.VERTICAL);
					}
				}
				// Check if its horizontal
				else if (deltaY <= deltaX / 2) {
					// MouseY = otherEnd.getY();
					// otherEnd.setY(thisEnd.getY());
					if (shiftStateChanged) {
						new Constraint(thisEnd, otherEnd, thisEnd
								.getParentOrCurrentFrame().getNextItemID(),
								Constraint.HORIZONTAL);
					}
				} else {
					// Add DIAGONAL constraints
					// if (deltaX > deltaY) {
					// otherEnd.setY(thisEnd.getY() + deltaX
					// * (e.getY() < otherEnd.getY() ? 1 : -1));
					// } else {
					// otherEnd.setX(thisEnd.getX() + deltaY
					// * (e.getX() < otherEnd.getX() ? 1 : -1));
					// }
					if (shiftStateChanged) {
						int constraint = Constraint.DIAGONAL_NEG;
						// Check if the slope is positive
						if ((thisEnd.getY() - otherEnd.getY())
								/ (double) (thisEnd.getX() - otherEnd.getX()) > 0.0) {
							constraint = Constraint.DIAGONAL_POS;
						}

						new Constraint(thisEnd, otherEnd, thisEnd
								.getParentOrCurrentFrame().getNextItemID(),
								constraint);
					}
				}
			}// If its a lineend attached to two lines lengthen the shorter
			// so it is the same length as the longer line
			else if (FreeItems.getInstance().size() == 3) {
				// check if we are rubber banding the corner of a shape
				Item thisEnd = getShapeCorner(FreeItems.getInstance());
				if (thisEnd != null) {
					Line line1 = thisEnd.getLines().get(0);
					Line line2 = thisEnd.getLines().get(1);
					// Check if the two lines are constrained and hence it is a
					// rectangle
					Integer c1 = line1.getPossibleConstraint();
					Integer c2 = line2.getPossibleConstraint();

					if (c1 != null && c2 != null) {
						// This is the case of a constrained rectangle
						if ((c2 == Constraint.VERTICAL || c2 == Constraint.HORIZONTAL)
								&& (c1 == Constraint.VERTICAL || c1 == Constraint.HORIZONTAL)
								&& (c1 != c2)) {
							Line vLine = line2;
							Line hLine = line1;
							if (c1 == Constraint.VERTICAL) {
								vLine = line1;
								hLine = line2;
							}
							Item hOtherEnd = hLine.getOppositeEnd(thisEnd);
							Item vOtherEnd = vLine.getOppositeEnd(thisEnd);

							double vLength = Math
									.abs(vOtherEnd.getY() - MouseY);
							double hLength = Math
									.abs(hOtherEnd.getX() - MouseX);

							if (vLength > hLength) {
								MouseX = Math.round(hOtherEnd.getX() + vLength
										* (MouseX > hOtherEnd.getX() ? 1 : -1));
							} else /* if (hLength > vLength) */{
								MouseY = Math.round(vOtherEnd.getY() + hLength
										* (MouseY > vOtherEnd.getY() ? 1 : -1));
							}
						}
						// } else if (c2 != null) {
						//
						// } // Other wise it is a not constrained shape so
						// constrain
						// the two lines lengths to be equal
					} else {
						Item lineEnd1 = line1.getOppositeEnd(thisEnd);
						Item lineEnd2 = line2.getOppositeEnd(thisEnd);
						double l1 = Line.getLength(lineEnd1.getPosition(), e
								.getPoint());
						double l2 = Line.getLength(lineEnd2.getPosition(), e
								.getPoint());
						double l3 = Line.getLength(lineEnd1.getPosition(),
								lineEnd2.getPosition());
						// l1 needs to be the shorter end
						if (l1 > l2) {
							Item temp = lineEnd1;
							lineEnd1 = lineEnd2;
							lineEnd2 = temp;
							double tempL = l1;
							l1 = l2;
							l2 = tempL;
						}
						// Now use the cosine rule to calculate the angle
						// between l1 and l3
						double cosTheta = (l1 * l1 + l3 * l3 - l2 * l2)
								/ (2 * l1 * l3);
						// now calculate the new length for the lines using cos
						// rule
						double l_new = l3 / (2 * cosTheta);
						double ratio = l_new / l1;
						MouseX = Math.round((e.getX() - lineEnd1.getX())
								* ratio)
								+ lineEnd1.getX();
						MouseY = Math.round((e.getY() - lineEnd1.getY())
								* ratio)
								+ lineEnd1.getY();

					}
				}
			}
		} else if (shiftStateChanged && !_controlDown && rubberBanding()) {
			// Get the line end that is being rubber banded
			Item thisEnd = FreeItems.getInstance().get(0).isLineEnd() ? FreeItems
					.getInstance().get(0)
					: FreeItems.getInstance().get(1);
			thisEnd.removeAllConstraints();
		}

		if (_lastMouseMoved == null)
			_lastMouseMoved = e;

		_lastMouseMoved = e;

		refreshHighlights();

		if (FreeItems.hasCursor()) {
			move(FreeItems.getCursor(), true);
		}

		if (FreeItems.itemsAttachedToCursor()) {
			move(FreeItems.getInstance());
			// System.out.println(FreeItems.getInstance().size());
		}

		if (_forceArrowCursor)
			updateCursor();

		_forceArrowCursor = true;
	}

	public void refreshHighlights() {
		// ByMike: Get the item the mouse is hovering over
		Item click = FrameUtils.getCurrentItem();
		Item on = null;
		// System.out.println(click);
		if (click != null) {
			on = click;
			// set the context
			if (on instanceof Line)
				_context = CONTEXT_AT_LINE;
			else if (on instanceof Dot)
				_context = CONTEXT_AT_DOT;
			else if (on instanceof Text) {
				_context = CONTEXT_AT_TEXT;
			}
			if (FreeItems.getInstance().size() > 0)
				_alpha = 60;
			else
				_alpha = -1;
		} else {
			_context = CONTEXT_FREESPACE;
			_alpha = -1;
		}

		// if the user is pointing at an item, highlight it
		if (on != null && !FreeItems.getInstance().contains(on)) {
			// if the user can spot-weld, show the virtual spot
			if (FreeItems.getInstance().size() == 2 && on instanceof Line) {
				Line line = (Line) on;
				Item freeItem0 = FreeItems.getInstance().get(0);
				Item freeItem1 = FreeItems.getInstance().get(1);
				Item lineEnd = freeItem0.isLineEnd() ? freeItem0 : (freeItem1
						.isLineEnd() ? freeItem1 : null);
				if (lineEnd != null) {
					if (_mouseDown == 0)
						line.showVirtualSpot(lineEnd, DisplayIO.getMouseX(),
								FrameMouseActions.getY());
				} else
					// The user is pointing at another point or text item
					// etc
					FrameGraphics.changeHighlightMode(on,
							Item.HighlightMode.Normal);
			} else {
				// FrameGraphics.ChangeSelectionMode(on,
				// Item.SelectedMode.Connected);
				// TODO: The method below is for the most part redundant
				on = FrameGraphics.Highlight(on.getEditTarget());
			}
			// if the last item highlighted is still highlighted, clear it
			if (_lastHoldsHighlight) {
				_lastHoldsHighlight = false;
				for (Item i : DisplayIO.getCurrentFrame().getItems())
					if (i.isHighlighted() && i != on)
						FrameGraphics.changeHighlightMode(i,
								Item.HighlightMode.None);
			}

			// if the user is not pointing at an item, check for enclosure
			// highlighting
		} else if (on == null) {
			Collection<Item> enclosure = FrameUtils.getEnclosingLineEnds();
			if (enclosure != null && enclosure.size() > 0) {
				Item firstLineEnd = enclosure.iterator().next();
				HighlightMode hm;
				if(isShiftDown()) {
					hm = HighlightMode.Connected;
				} else {
					hm = HighlightMode.Enclosed;
				}
				if (firstLineEnd.getLines().size() > 1 &&
				// check that the enclosure is not part of a point being
						// dragged in space
						!ContainsOneOf(enclosure, FreeItems.getInstance())) {
					on = firstLineEnd.getLines().get(0);
					// System.out.println(on == null ? "Null" :
					// on.toString());
					FrameGraphics.changeHighlightMode(on, hm);
				} else if (firstLineEnd instanceof XRayable) {
					on = firstLineEnd;
					FrameGraphics.changeHighlightMode(firstLineEnd, hm);
				}
				_context = CONTEXT_AT_ENCLOSURE;
			} else if (_lastHighlightedItem != null) {
				// System.out.println("LastHighlightedItem");
				_lastHoldsHighlight = false;
			}
		}

		// disable cursor changes when the cursor has items attached
		if (FreeItems.itemsAttachedToCursor()
				&& DisplayIO.getCursor() != Item.TEXT_CURSOR)
			_forceArrowCursor = false;

		// setLastHighlightedItem(on);

		if (_lastHighlightedItem != null && _lastHighlightedItem != on
				&& !_lastHoldsHighlight) {
			// Turn off the highlighting only if
			// the last highlighted item is not connected to the currentItem
			// Otherwise we get flickering in transition from connected to
			// normal mode while moving the cursor along a line.
			if (on == null
					|| (!on.getAllConnected().contains(_lastHighlightedItem))) {
				FrameGraphics.changeHighlightMode(_lastHighlightedItem,
						Item.HighlightMode.None);
			}
		}

		_lastHighlightedItem = on;

	}

	private boolean ContainsOneOf(Collection<Item> enclosure,
			Collection<Item> freeItems) {
		if (freeItems == null)
			return false;
		for (Item i : freeItems) {
			if (enclosure.contains(i))
				return true;
		}
		return false;
	}

	/**
	 * Checks if lines are being rubber banded.
	 * 
	 * @return true if the user is rubberBanding one or more lines
	 */
	private static boolean rubberBanding() {
		if (FreeItems.getInstance().size() != 2) {
			return false;
		}

		// if rubber-banding, there will be 1 lineend and the rest will be lines
		boolean foundLineEnd = false;
		for (Item i : FreeItems.getInstance()) {
			if (i.isLineEnd()) {
				if (foundLineEnd) {
					return false;
				}
				foundLineEnd = true;
			} else if (!(i instanceof Line) || !i.isVisible()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Updates the current mouse cursor to whatever it should be. i.e. Hidden
	 * when rubber-banding lines, otherwise default (arrow)
	 */
	public static void updateCursor() {
		if (rubberBanding()) {
			DisplayIO.setCursor(Item.HIDDEN_CURSOR);
			return;
		}
		// This is to make sure the TEXT_CURSOR doesnt get inadvertantly turned
		// off!
		Item on = FrameUtils.getCurrentItem();
		if (on != null && on instanceof Text) {
			return;
		}
		DisplayIO.setCursor(Item.DEFAULT_CURSOR);
	}

	public static void setHighlightHold(boolean hold) {
		_lastHoldsHighlight = hold;
	}

	public static void resetOffset() {
		if (FreeItems.itemsAttachedToCursor()) {
			_offX = DisplayIO.getMouseX()
					- FreeItems.getInstance().get(0).getX()
					+ FreeItems.getInstance().get(0).getOffset().x;
			_offY = getY() - FreeItems.getInstance().get(0).getY()
					+ FreeItems.getInstance().get(0).getOffset().y;
		}
	}

	/**
	 * Moves the items to the current mouse position (plus the current offset)
	 * 
	 * @param toMove
	 */
	static void move(Collection<Item> toMove) {
		move(toMove, false);
	}

	static void move(Collection<Item> toMove, boolean cursor) {

		// Gets the origin of the first item to move
		int xPos = (DisplayIO.getMouseX() - (cursor ? 0 : _offX));

		Item firstDot = toMove.iterator().next();

		int deltax = firstDot.getX() - xPos;
		int deltay = firstDot.getY() - (getY() - (cursor ? 0 : _offY));

		for (Item move : toMove) {
			move.setPosition(move.getX() - deltax, move.getY() - deltay);

			if (!cursor && move instanceof Text) {
				((Text) move).setAlpha(_alpha);
			}
		}

		FrameGraphics.requestRefresh(true);
		// FrameGraphics.refresh(true);
	}

	private static void load(String toLoad, boolean addToHistory) {
		if (FrameIO.isValidFrameName(toLoad)) {
			DisplayIO.clearBackedUpFrames();
			FrameUtils.DisplayFrame(toLoad, addToHistory, true);
		} else {
			MessageBay.errorMessage(toLoad + " is not a valid frame name.");
		}
	}

	private static void back() {
		DisplayIO.Back();

		// repaint things if necessary
		if (FreeItems.itemsAttachedToCursor())
			move(FreeItems.getInstance());

		if (FreeItems.hasCursor())
			move(FreeItems.getCursor(), true);
	}

	private static void forward() {
		DisplayIO.Forward();

		// repaint things if necessary
		if (FreeItems.itemsAttachedToCursor())
			move(FreeItems.getInstance());

		if (FreeItems.hasCursor())
			move(FreeItems.getCursor(), true);
	}

	/**
	 * Returns true if the mouse moved during TDFC. This will happen if there is
	 * a start annotation item on the frame.
	 * 
	 * @param linker
	 * @return
	 */
	public static boolean tdfc(Item linker) throws RuntimeException {
		// if this is a non-usable item
		if (linker.getID() < 0)
			return false;

		// Check if its an image that can be resized to fit a box
		// around it
		String text = linker.getText();
		boolean isVector = text.equals("@v") || text.equals("@av");
		boolean isFrameImage = text.equals("@f");
		boolean isBitmap = false; // text.equals("@b");

		if (isVector || isFrameImage || isBitmap) {
			Collection<Item> enclosure = FrameUtils.getEnclosingLineEnds(linker
					.getPosition());
			if (enclosure != null) {
				for (Item i : enclosure) {
					if (i.isLineEnd() && i.isEnclosed()) {
						if (!isVector)
							DisplayIO.getCurrentFrame().removeAllItems(
									enclosure);
						Rectangle rect = i.getEnclosedRectangle();
						long width = Math.round(rect.getWidth());
						if (isVector) {
							NumberFormat nf = Vector.getNumberFormatter();
							linker.setText(linker.getText()
									+ ": "
									+ nf.format((width / FrameGraphics
											.getMaxFrameSize().getWidth())));
						} else {
							linker.setText(linker.getText() + ": " + width);
						}

						linker.setPosition(new Point(rect.x, rect.y));
						linker.setThickness(i.getThickness());
						linker.setBorderColor(i.getColor());
						break;
					}
				}
				if (!isVector)
					FrameMouseActions.deleteItems(enclosure, false);
			}
		}

		boolean mouseMoved;

		linker.getParent().setChanged(true);

		Frame next = FrameIO.CreateNewFrame(linker, _onFrameAction);

		linker.setLink("" + next.getNumber());

		for (Item i : next.getTextItems()) {
			// Set the link for @Parent annotation item if one
			if (ItemUtils.startsWithTag(i, ItemUtils.TAG_PARENT)
					&& i.getLink() == null) {
				Frame parent = linker.getParentOrCurrentFrame();
				i.setLink(parent.getName());
			} else if (ItemUtils.startsWithTag(i, ItemUtils.TAG_BACKUP, false)) {
				// Delink backup tag if it is on the frame
				i.setLink(null);
			}
		}

		FrameUtils.DisplayFrame(next, true, true);
		FrameUtils.setTdfcItem(linker);

		mouseMoved = next.moveMouseToDefaultLocation();
		// this needs to be done if the user doesnt move the mouse before doing
		// tdfc while the cursor is set to the text cursor
		DisplayIO.setCursor(Item.DEFAULT_CURSOR);
		// This needs to be done in case there was a @start on the frame which
		// triggers changed to be set to true when it should stay as false
		next.setChanged(false);
		return mouseMoved;
	}

	/**
	 * Creates a new Text item and fills it with particular attributes extracted
	 * from the given Item. Note: Users always have permission to extract
	 * attributes, so it is not checked.
	 * 
	 * @param toExtract
	 *            Item containing the Item to extract the attributes from.
	 */
	private static void extractAttributes(Item toExtract) {
		if (toExtract == null || toExtract == null)
			return;

		if (FreeItems.itemsAttachedToCursor())
			return;

		Item attribs;
		Item item = toExtract;
		// Extract the frames attributes when the user clicks on the frame name
		FrameGraphics.changeHighlightMode(item, HighlightMode.None);
		if (item.isFrameName())
			attribs = AttributeUtils.extractAttributes(item.getParent());
		else {
			attribs = AttributeUtils.extractAttributes(item);
		}

		if (attribs == null)
			MessageBay
					.displayMessage("All attributes of that item are default values.");
		else {
			// Give the attribute text item the color of the item for which
			// attributes are being extracted.
			// attribs.setColor(item.getColor());
			pickup(attribs);
		}
	}

	public static void delete(Item toDelete) {
		boolean bRecalculate = false;

		FrameUtils.setLastEdited(null);
		_offX = _offY = 0;

		Frame currentFrame = DisplayIO.getCurrentFrame();
		// check if the user is pointing at the frame's framename
		if (toDelete != null && toDelete == currentFrame.getNameItem()) {
			currentFrame.clear(false);
			FrameGraphics.Repaint();
			return;
		}

		// if the user is deleting items attached to the cursor
		if (FreeItems.itemsAttachedToCursor()) {
			// if this is an item-swap
			if (FreeItems.getInstance().size() == 1
					&& FreeItems.getInstance().get(0) instanceof Text
					&& toDelete != null && toDelete instanceof Text) {

				// check permissions
				if (!toDelete.hasPermission(UserAppliedPermission.full)) {
					MessageBay
							.displayMessage("Insufficient permission to swap Item text");
					return;
				}
				Text anchored = (Text) toDelete;
				Text free = (Text) FreeItems.getInstance().get(0);
				SessionStats.DeletedItem(free);
				// List<String> temp = anchored.getText();
				anchored.setText(free.getText());
				anchored.setFormula(free.getFormula());

				// free.setTextList(temp);
				FreeItems.getInstance().clear();

				anchored.getParent().setChanged(true);

				bRecalculate |= free.recalculateWhenChanged();
				bRecalculate |= anchored.recalculateWhenChanged();

				// update the offset since the text has changed
				_offX = DisplayIO.getMouseX() - anchored.getX()
						+ anchored.getOffset().x;
				_offY = getY() - anchored.getY() + anchored.getOffset().y;
			} else {
				// if shift is pressed delete the entire shape attached to the dot
				if(isShiftDown()) {
    				List<Item> tmp = new ArrayList<Item>(FreeItems.getInstance());
    				for(Item i : tmp) {
    					// remove entire rectangles instead of just the corner
    					if(i instanceof Dot) {
    						FreeItems.getInstance().addAll(i.getAllConnected());
    						for(Item j : i.getAllConnected()) {
    							if(j instanceof Dot) {
    								FreeItems.getInstance().addAll(j.getLines());
    							}
    						}
    					}
    				}
				}
				deleteItems(FreeItems.getInstance());
			}
			// reset the mouse cursor
			updateCursor();
			// the user is not pointing at an item
		} else if (toDelete == null) {
		
			// if the user is pointing inside a closed shape, delete it
			
			Collection<Item> items = null;
			// if shift is down, only delete the enclosing shape (ignore the items inside)
			if(isShiftDown()) {
				Collection<Item> tmp = FrameUtils.getEnclosingLineEnds();
				if(tmp != null) {
					items = new ArrayList<Item>();
					items.addAll(tmp);
    				for(Item i : tmp) {
    					if(i instanceof Dot) {
    						items.addAll(((Dot)i).getLines());
    					}
    				}
				}
			} else {
				items = FrameUtils.getCurrentItems(null);
			}
			
			if (items != null) {
				Collection<Item> toRemove = new LinkedHashSet<Item>(items
						.size());
				for (Item ip : items) {
					if (ip.hasPermission(UserAppliedPermission.full)) {
						// Only include lines if one of their enpoints are also
						// being removed
						if (ip instanceof Line) {
							Line l = (Line) ip;
							Item end = l.getEndItem();
							Item start = l.getStartItem();

							// If one end of a line is being delted, remove the
							// other end if all its connecting lines are being
							// delted
							if (items.contains(end)) {
								if (!items.contains(start)
										&& items.containsAll(start.getLines())) {
									toRemove.add(start);
								}
							} else if (items.contains(start)) {
								if (items.containsAll(end.getLines())) {
									toRemove.add(end);
								}
							} else {
								continue;
							}
						}
						toRemove.add(ip);
					}
				}

				deleteItems(toRemove);

				// reset the mouse cursor
				updateCursor();
				FrameGraphics.Repaint();

				// otherwise this is an undo command
			} else {
				if(isControlDown()) {
					DisplayIO.getCurrentFrame().redo();
				} else {
					DisplayIO.getCurrentFrame().undo();
				}
			}
			return;
			// this is a delete command
		} else {
			// check permissions
			if (!toDelete.hasPermission(UserAppliedPermission.full)) {
				Item editTarget = toDelete.getEditTarget();
				if (editTarget != toDelete
						&& editTarget.hasPermission(UserAppliedPermission.full)) {
					toDelete = editTarget;
				} else {
					MessageBay
							.displayMessage("Insufficient permission to delete item");
					return;
				}
			}

			Frame parent = toDelete.getParent();
			if (parent != null) {
				parent.setChanged(true);
			}
			Collection<Item> toUndo = null;
			if (toDelete.isLineEnd()) {
				// delete the entire connected shape if shift is down
				if(isShiftDown()) {
					List<Item> tmp = new ArrayList<Item>();
					tmp.add(toDelete);
					// remove entire shape instead of just the corner
					tmp.addAll(toDelete.getAllConnected());
					for(Item j : toDelete.getAllConnected()) {
						if(j instanceof Dot) {
							tmp.addAll(j.getLines());
						}
					}
					deleteItems(tmp);
					return;
				} else {
					toUndo = deleteLineEnd(toDelete);
				}
				// delete the entire connected shape if shift is down, unless we're hovering the end of the line
			} else if (toDelete instanceof WidgetEdge) { // must notify
				// widgets that they
				// are being deleted
				((WidgetEdge) toDelete).getWidgetSource().onDelete();
				toUndo = toDelete.getConnected();
			} else if (toDelete instanceof Line && isShiftDown() ||
					toDelete.getHighlightMode() == Item.HighlightMode.Disconnect) {
				Line line = (Line) toDelete;
				Item start = line.getStartItem();
				Item end = line.getEndItem();
				Collection<Item> delete = new LinkedList<Item>();
				delete.add(toDelete);
				if (end.getLines().size() == 1) {
					delete.add(end);
				} else {
					end.removeLine(line);
				}
				if (start.getLines().size() == 1) {
					delete.add(start);
				} else {
					start.removeLine(line);
				}
				toUndo = delete;
			} else {
				bRecalculate |= toDelete.recalculateWhenChanged();
				toUndo = toDelete.getConnected(); // copy(toDelete.getConnected());
			}
			SessionStats.DeletedItems(toUndo);
			if (parent != null) {
				parent.addToUndoDelete(toUndo);
				parent.removeAllItems(toUndo); // toDelete.getConnected()
			}
			// reset the mouse cursor
			updateCursor();
			if (parent != null)
				ItemUtils.EnclosedCheck(parent.getItems());
			if (toDelete.hasOverlay()) {
				FrameUtils.Parse(parent, false, false);
				FrameGraphics.requestRefresh(false);
			}

			DisplayIO.setCursor(Item.DEFAULT_CURSOR);

		}

		currentFrame.notifyObservers(bRecalculate);

		FrameGraphics.Repaint();
	}

	public static void deleteItems(Collection<Item> itemList) {
		deleteItems(itemList, true);
	}

	public static void deleteItems(Collection<Item> itemList, boolean addToUndo) {
		boolean bReparse = false;
		boolean bRecalculate = false;

		SessionStats.DeletedItems(itemList);
		List<Frame> modifiedFrames = new LinkedList<Frame>();
		// Get a list of all the modified frames
		for (Item i : itemList) {
			Frame parent = i.getParent();
			if (parent != null)
				modifiedFrames.add(parent);
			i.setHighlightMode(HighlightMode.None);
			bReparse |= i.hasOverlay();
			bRecalculate |= i.recalculateWhenChanged();
		}
		// If they are all free items then add the current frame
		if (modifiedFrames.size() == 0) {
			modifiedFrames.add(DisplayIO.getCurrentFrame());
		}

		Collection<Item> toUndo = new LinkedHashSet<Item>();
		// disconnect any connected items
		for (Item i : itemList) {

			// Only delete the current item if have not already deleted.
			// This is especially important for heavy duty widgets - so they
			// do not have to expire several times per delete.
			if (toUndo.contains(i))
				continue;

			// Make sure text items attached to cursor are reset back to the
			// transparency they should have.
			if (i instanceof Text) {
				((Text) i).setAlpha(-1);
			}

			if (i.getLines().size() > 0) {

				Collection<Item> toDelete = deleteLineEnd(i);
				if (addToUndo) {
					// add the copied items to the undo stack
					for (Item itemToUndo : toDelete) {

						if (!toUndo.contains(itemToUndo))
							toUndo.add(itemToUndo);

					}
				}
			} else if (!toUndo.contains(i)) {
				if (addToUndo)
					toUndo.add(i); // Why was is this a copy
			}
		}

		for (Frame f : modifiedFrames) {
			f.removeAllItems(itemList);
			ItemUtils.EnclosedCheck(f.getItems());
		}
		// TODO: How should undelete deal with undo when items are removed from
		// the current frame as well as the overlay frame
		Frame currentFrame = DisplayIO.getCurrentFrame();
		currentFrame.addToUndoDelete(itemList);
		itemList.clear();
		if (bReparse) {
			FrameUtils.Parse(currentFrame, false, false);
			/*
			 * TODO check if I need to recalculate even if reparse occurs, here
			 * and in anchor, pickup etc
			 */
		} else {
			currentFrame.notifyObservers(bRecalculate);
		}

	}

	private static Collection<Item> deleteLineEnd(Item lineEnd) {

		if (lineEnd instanceof WidgetCorner) { // Brook

			WidgetCorner wc = (WidgetCorner) lineEnd;
			Frame parent = wc.getWidgetSource().getParentFrame();

			// Remove from the parent frame
			if (parent != null) {
				parent.removeAllItems(wc.getWidgetSource().getItems());
			}

			wc.getWidgetSource().onDelete(); // Changes the widgets
			// corner/edges ID's...

			return wc.getWidgetSource().getItems();

		} else {

			// // create a backup copy of the dot and its lines
			// List<Item> copy = copy(lineEnd.getConnected());
			//			
			// // Remove lines from their anchored dots
			// // note: the line is kept so that it can be properly restored
			// for (Item ic : copy) {
			// if (ic instanceof Line) {
			// Line line = (Line) ic;
			// // Remove the line from the item that is not the copy of the
			// // line end being deletedF
			// if (!copy.contains(line.getStartItem()))
			// line.getStartItem().removeLine(line);
			// if (!copy.contains(line.getEndItem()))
			// line.getEndItem().removeLine(line);
			// }
			// }

			Collection<Item> copy = lineEnd.getConnected();

			// remove all lines being deleted
			for (Item ic : lineEnd.getConnected()) {
				if (ic instanceof Line
						&& ((Line) ic).getOppositeEnd(lineEnd) != null) {
					Line line = (Line) ic;

					// Invalidate the line to make sure we dont get any ghost
					// arrowheads.
					ic.invalidateAll();

					Item d = line.getOppositeEnd(lineEnd);
					d.removeLine(line);

					// if the dot was only part of one line, it can be
					// removed
					if (d.getLines().size() == 0) {
						if (d.getParent() != null)
							d.getParent().removeItem(d);
						if (!copy.contains(d))
							copy.add(d);
					}

					if (lineEnd.getParent() != null)
						lineEnd.getParent().removeItem(ic);
				}
			}
			return copy;
		}
	}

	private static void removeAllLinesExcept(Item from, Item but) {
		List<Line> lines = new LinkedList<Line>();
		lines.addAll(from.getLines());
		for (Line line : lines)
			if (line.getOppositeEnd(from) != but)
				from.removeLine(line);

		List<Constraint> consts = new LinkedList<Constraint>();
		consts.addAll(from.getConstraints());
		for (Constraint c : consts)
			if (c.getOppositeEnd(from) != but)
				from.removeConstraint(c);
	}

	public static Collection<Item> merge(List<Item> merger, Item mergee) {
		assert (mergee != null);
		if (mergee.isFrameName()) {
			return mergee.getParent().merge(merger);
		}

		// if(mergee instanceof XRayable)
		// return merger;

		// check for rectangle merging
		if (merger.size() == 3 && mergee.getLines().size() == 2) {
			Item corner = getShapeCorner(merger);
			// if this is a corner of a shape
			if (corner != null) {
				Collection<Item> allConnected = corner.getAllConnected();
				// Check if we are collapsing a rectangle
				if (allConnected.size() == 8 && allConnected.contains(mergee)) {
					DisplayIO.setCursorPosition(mergee.getPosition());
					DisplayIO.getCurrentFrame().removeAllItems(allConnected);

					// find the point opposite corner...
					Item opposite = null;
					List<Line> lines = corner.getLines();
					for (Line l : lines) {
						allConnected.remove(l.getOppositeEnd(corner));
					}
					allConnected.remove(corner);
					for (Item i : allConnected) {
						if (i.isLineEnd()) {
							opposite = i;
							break;
						}
					}
					assert (opposite != null);

					// check if the rectangle is small enough that it should be
					// collapsed to a single point
					int x1 = Math.abs(opposite.getX() - mergee.getX());
					int x2 = Math.abs(opposite.getY() - mergee.getY());
					int distance = (int) Math.sqrt(Math.pow(x1, 2)
							+ Math.pow(x2, 2));

					if (distance < RECTANGLE_TO_POINT_THRESHOLD) {
						mergee.removeAllConstraints();
						mergee.removeAllLines();
						mergee.setThickness(4 * mergee.getThickness());
						return mergee.getAllConnected();
					} else {
						removeAllLinesExcept(mergee, opposite);
						removeAllLinesExcept(opposite, mergee);

						return mergee.getAllConnected();
					}
				}
			}
		}

		List<Item> remain = new ArrayList<Item>();
		Item res = null;

		for (Item i : merger) {			
			if (!i.isVisible())
				continue;
			// check for link merging
			if (i instanceof Text
					&& FrameIO.isValidFrameName((((Text) i).getFirstLine()))
					&& FrameIO.canAccessFrame((((Text) i).getFirstLine()))) {
				// check that we can actually access the frame this link
				// corresponds to
				mergee.setLink(((Text) i).getFirstLine());
			} else {
				// check for attribute merging
				if (i instanceof Text && !i.isLineEnd()) {
					Text txt = (Text) i;

					// if this is not an attribute merge
					if (!AttributeUtils.setAttribute(mergee, txt)) {
						// set mouse position for text merges
						if (mergee instanceof Text) {
							((Text) mergee).insertText(txt.getText(), DisplayIO
									.getMouseX(), FrameMouseActions.getY());
							//Delete the item which had its text merged
							txt.delete();
							return remain;
						} else if (mergee instanceof WidgetCorner) {
							if (merger.size() == 1 && txt.getLink() != null) {
								// If the text item is linked then use that
								((WidgetCorner) mergee).setLink(txt
										.getAbsoluteLink(), txt);
							} else {
								remain.addAll(merger);
							}
							return remain;
						} else if (mergee instanceof WidgetEdge) {
							if (merger.size() == 1 && txt.getLink() != null) {
								// If the text item is linked then use that
								((WidgetEdge) mergee).setLink(txt
										.getAbsoluteLink(), txt);
							} else {
								remain.addAll(merger);
							}
							return remain;
						} else if (mergee instanceof Dot) {
							DisplayIO.setCursorPosition(mergee.getPosition());
							txt.setPosition(mergee.getPosition());
							txt.setThickness(mergee.getThickness());
							Frame parent = mergee.getParent();
							parent.removeItem(mergee);
							anchor(txt);
							// change the end points of the lines to the text
							// item
							while (mergee.getLines().size() > 0) {
								Line l = mergee.getLines().get(0);
								l.replaceLineEnd(mergee, txt);
							}
							break;
						}

						// TODO tidy this up...
						// Dot override doesnt use the x and y coords
						// Text does... but could be removed
						res = mergee.merge(i, DisplayIO.getMouseX(), getY());
						if (res != null) {
							remain.add(res);
						}
					}
				} else {
					if (mergee.isLineEnd()) {
						DisplayIO.setCursorPosition(mergee.getPosition());
					}
					// Moving the cursor ensures shapes are anchored correctly
					res = mergee.merge(i, DisplayIO.getMouseX(), getY());
					if (res != null)
						remain.addAll(res.getConnected());

				}
			}
		}
		updateCursor();

		mergee.getParent().setChanged(true);

		ItemUtils.EnclosedCheck(mergee.getParent().getItems());
		// Mike: Why does parse frame have to be called?!?
		FrameUtils.Parse(mergee.getParent());

		return remain;
	}

	/**
	 * Picks up an item on a frame.
	 * 
	 * @param toGrab
	 *            item to be picked up
	 * @param removeItem
	 *            true if the item should be removed from the frame
	 */
	public static void pickup(Item toGrab) {
		if (toGrab.isFrameName())
			return;

		if (!toGrab.hasPermission(UserAppliedPermission.full)) {
			if (toGrab.getEditTarget() != toGrab) {
				pickup(toGrab.getEditTarget());
			} else {
				MessageBay
						.displayMessage("Insufficient permission pickup the item");
			}
			return;
		}

		if (toGrab instanceof Circle)
			toGrab.setHighlightMode(HighlightMode.Connected);
		// Dont set the highlight mode if a vector is being picked up
		else if (toGrab.isVisible()) {
			toGrab.setHighlightMode(HighlightMode.Normal);
		}

		// Brook: If the widget corner is being picked up. Instead refer to
		// picking up the edge for fixed-sized widgets so it is not so confusing
		if (toGrab instanceof WidgetCorner) {
			WidgetCorner wc = (WidgetCorner) toGrab;
			if (wc.getWidgetSource().isFixedSize()) {
				for (Item i : toGrab.getConnected()) {
					if (i instanceof WidgetEdge) {
						toGrab = i;
						break;
					}
				}
			}
		}
		pickup(toGrab.getConnected());
	}

	public static void pickup(Collection<Item> toGrab) {
		if (toGrab == null || toGrab.size() == 0)
			return;

		boolean bReparse = false;
		boolean bRecalculate = false;

		Frame currentFrame = DisplayIO.getCurrentFrame();
		String currentFrameName = currentFrame.getName();
		Iterator<Item> iter = toGrab.iterator();
		while (iter.hasNext()) {
			Item i = iter.next();
			if (!i.hasPermission(UserAppliedPermission.full)) {
				iter.remove();
				continue;
			}
			if (i.equals(_lastHighlightedItem))
				_lastHighlightedItem = null;

			bRecalculate |= i.recalculateWhenChanged();
			// i.setSelectedMode(SelectedMode.None);
			// Check if it has a relative link if so make it absolute
			i.setAbsoluteLink();
			// parent may be null
			if (i.getParent() != null) {
				i.getParent().removeItem(i);
				if (currentFrameName.equals(i.getParent().getName()))
					i.setParent(null);
			}
			FreeItems.getInstance().add(i);
			i.setFloating(true);
			// If its a vector pick up a copy of the stuff on the vector frame
			if (i.hasVector()) {
				bReparse = true;

				Frame overlayFrame = FrameIO.LoadFrame(i.getAbsoluteLink());
				Collection<Item> copies = ItemUtils.CopyItems(overlayFrame
						.getNonAnnotationItems(false), i.getVector());
				for (Item copy : copies) {
					FreeItems.getInstance().add(copy);
					copy.setEditTarget(i);
					copy.setFloating(true);
					copy.setParent(null);
					// copy.setHighlightMode(HighlightMode.Connected);
				}
			}
		}
		currentFrame.change();

		_lastHighlightedItem = null;
		updateCursor();

		// if there are multiple items in the list, determine which to use for
		// offset calculations
		if (toGrab.size() > 1) {
			for (Item i : toGrab) {
				// MIKE: Movement goes haywire if these are removed because Line
				// class returns 0 for getX
				if (!(i instanceof Line) && !(i instanceof XRayable)) {
					_offX = DisplayIO.getMouseX() - i.getX() + i.getOffset().x;
					_offY = getY() - i.getY() + i.getOffset().y;

					// make the offset item the first item in the list (so
					// move method knows which item to use)
					FreeItems.getInstance().set(
							FreeItems.getInstance().indexOf(i),
							FreeItems.getInstance().get(0));
					FreeItems.getInstance().set(0, i);
					break;
				}
			}

			move(FreeItems.getInstance());
			ItemUtils.EnclosedCheck(toGrab);
			// otherwise, just use the first item
		} else if (toGrab.size() == 1) {
			Item soleItem = toGrab.iterator().next();
			_offX = DisplayIO.getMouseX() - soleItem.getX()
					+ soleItem.getOffset().x;
			_offY = getY() - soleItem.getY() + soleItem.getOffset().y;
			// Now call move so that if we are on a message in the message box
			// It doesnt appear up the top of the scree!!
			move(toGrab);
		} else {
			MessageBay
					.displayMessage("Insufficient permission to pickup the items");
		}
		if (bReparse)
			FrameUtils.Parse(currentFrame, false, false);
		else
			currentFrame.notifyObservers(bRecalculate);

		FrameGraphics.Repaint();
	}

	private static Line createLine() {
		Frame current = DisplayIO.getCurrentFrame();
		// create the two endpoints
		Item end = DisplayIO.getCurrentFrame().createDot();
		Item start = DisplayIO.getCurrentFrame().createDot();

		// create the Line
		Line line = new Line(start, end, current.getNextItemID());
		line.autoArrowheadLength();

		// anchor the start
		anchor(start);

		// attach the line to the cursor
		pickup(end);
		_lastHighlightedItem = null;

		// TODO figure out how to get the end to highlight
		// end.setSelectedMode(SelectedMode.Normal);
		// end.setSelectedMode(SelectedMode.None);

		return line;
	}

	/**
	 * Returns a list of copies of the list passed in
	 * 
	 * @param toCopy
	 *            The list of items to copy
	 * @return A List of copied Items
	 */
	private static List<Item> copy(Collection<Item> toCopy) {
		return ItemUtils.CopyItems(toCopy);
	}

	public static void anchor(Item toAnchor, boolean checkEnclosure) {
		// Only anchor items we have full permission over... i.e. don't anchor vector items
		if (!toAnchor.hasPermission(UserAppliedPermission.full))
			return;

		toAnchor.anchor();

		if (checkEnclosure) {
			ItemUtils.EnclosedCheck(toAnchor.getParentOrCurrentFrame()
					.getItems());
			FrameGraphics.Repaint();
		}
	}

	public static void anchor(Item toAnchor) {
		anchor(toAnchor, true);
	}

	public static void anchor(Collection<Item> toAnchor) {
		boolean bReparse = false;
		boolean bRecalculate = false;
		// Need to make sure we check enclosure for overlays etc
		Set<Frame> checkEnclosure = new HashSet<Frame>();

		// Create a clone of toAnchor since in the proccess of anchoring items
		// they can change the state of the toAnchor collection and thus create
		// concurrent modification exceptions.
		// This is especially needed for widgets being removed when anchored:
		// since they
		// currently are composed of 8 items this is vital. In the new revision
		// of
		// widgets being implemented as a single item this this can be
		// depreciated
		// however it may be useful for other applications.
		Collection<Item> toAnchorCopy = new ArrayList<Item>(toAnchor);

		for (Item i : toAnchorCopy) {
			if (toAnchor.contains(i)) { // since to anchor could change while
				// anchoring
				// if (!i.hasVector())
				anchor(i, false);
				checkEnclosure.add(i.getParentOrCurrentFrame());
				bReparse |= i.hasOverlay();
				bRecalculate |= i.recalculateWhenChanged();
			}
		}

		toAnchor.clear();
		// Check enclosure for all the frames of the items that were anchored
		for (Frame f : checkEnclosure) {
			ItemUtils.EnclosedCheck(f.getItems());
		}

		Frame currentFrame = DisplayIO.getCurrentFrame();
		if (bReparse)
			FrameUtils.Parse(currentFrame, false, false);
		else {
			currentFrame.notifyObservers(bRecalculate);
		}
		FrameGraphics.Repaint();
	}

	/*
	 * private boolean mouseMovedRecently() { Date now = new Date();
	 * 
	 * return now.getTime() - _lastMouseMovement.getTime() < 150; }
	 */

	public void mouseWheelMoved(MouseWheelEvent arg0) {
		Navigation.ResetLastAddToBack();

		int clicks = Math.abs(arg0.getWheelRotation());

		if (FreeItems.getInstance().size() == 2) {
			if ((FreeItems.getInstance().get(0).isLineEnd() && FreeItems
					.getInstance().get(1) instanceof Line)
					|| (FreeItems.getInstance().get(1).isLineEnd() && FreeItems
							.getInstance().get(0) instanceof Line)) {

				Line line;
				if (FreeItems.getInstance().get(0) instanceof Line)
					line = (Line) FreeItems.getInstance().get(0);
				else
					line = (Line) FreeItems.getInstance().get(1);

				// User must do multiple clicks to toggle the line
				if (clicks >= MOUSE_WHEEL_THRESHOLD) {
					if (arg0.isShiftDown())
						line.toggleArrowHeadRatio(arg0.getWheelRotation());
					else
						line.toggleArrowHeadLength(arg0.getWheelRotation());
					// line.getParent().change();
					FrameGraphics.Repaint();
				}
			}
		} else if (arg0.getWheelRotation() != 0 && arg0.isShiftDown()) {

			FunctionKey rotationType = FunctionKey.SizeUp;
			if (arg0.getWheelRotation() > 0) {
				rotationType = FunctionKey.SizeDown;
			}

			FrameKeyboardActions.functionKey(rotationType, 1, arg0
					.isShiftDown(), arg0.isControlDown());

		} else if (clicks >= MOUSE_WHEEL_THRESHOLD) {
			Item item = FrameUtils.getCurrentItem();

			// if the user is not pointing to any item
			if (item == null) {
				FrameKeyboardActions.NextTextItem(null,
						arg0.getWheelRotation() > 0);
				return;
			}

			if (item instanceof Line || item instanceof Circle) {
				// check permissions
				if (!item.hasPermission(UserAppliedPermission.full)) {
					MessageBay
							.displayMessage("Insufficient permission to edit the Line");
					return;
				}
				item.toggleDashed(arg0.getWheelRotation());
				item.getParent().change();
				FrameGraphics.Repaint();
				return;
			} else if (item instanceof Text) {
				FrameKeyboardActions.NextTextItem(item,
						arg0.getWheelRotation() > 0);
			}
		}
	}

	/**
	 * 
	 * @return the integer value for the last mouse button clicked.
	 */
	public static int getLastMouseButton() {
		if (_lastMouseClick == null)
			return MouseEvent.NOBUTTON;

		return _lastMouseClick.getButton();
	}

	public static boolean isDelete(int modifiersEx) {

		int onMask = MouseEvent.BUTTON3_DOWN_MASK
				| MouseEvent.BUTTON2_DOWN_MASK;
		return (modifiersEx & onMask) == onMask;
	}

	public static boolean isGetAttributes(int modifiersEx) {
		int onMask = MouseEvent.BUTTON3_DOWN_MASK
				| MouseEvent.BUTTON1_DOWN_MASK;
		return (modifiersEx & onMask) == onMask;
	}

	public static boolean isTwoClickNoOp(int modifiersEx) {
		int onMask = MouseEvent.BUTTON2_DOWN_MASK
				| MouseEvent.BUTTON1_DOWN_MASK;
		return (modifiersEx & onMask) == onMask;
	}

	public static boolean wasDeleteClicked() {
		if (_lastMouseClick == null)
			return false;
		return isDelete(_lastMouseClickModifiers);
	}

	public static void control(KeyEvent ke) {
		for (Item i : FreeItems.getInstance()) {
			i.invalidateCommonTrait(ItemAppearence.PreMoved);
		}

		for (Item i : FreeItems.getCursor()) {
			i.invalidateCommonTrait(ItemAppearence.PreMoved);
		}

		_controlDown = ke.isControlDown();

		if (_controlDown) {
			// TODO why are these two lines needed?!?!
			// _offX = 0;
			// _offY = 0;
		} else {
			resetOffset();
		}

		if (_mouseDown > 0 && _lastMouseDragged != null) {
			MouseEvent me = _lastMouseDragged;
			_lastMouseDragged = new MouseEvent(ke.getComponent(),
					MouseEvent.NOBUTTON, ke.getWhen(), ke.getModifiers(), me
							.getX(), me.getY(), 0, false);
			_instance.mouseDragged(_lastMouseDragged);
		} else if (_lastMouseMoved != null) {
			MouseEvent me = _lastMouseMoved;
			_lastMouseMoved = new MouseEvent(ke.getComponent(),
					MouseEvent.NOBUTTON, ke.getWhen(), ke.getModifiers(), me
							.getX(), me.getY(), 0, false);
			_instance.mouseMoved(_lastMouseMoved, true);
		}
		updateCursor();
		
		Help.updateStatus();

		for (Item i : FreeItems.getInstance()) {
			i.invalidateCommonTrait(ItemAppearence.PostMoved);
		}

		for (Item i : FreeItems.getCursor()) {
			i.invalidateCommonTrait(ItemAppearence.PostMoved);
		}
		// TODO: Check why the new constrained line is not repainted immediately
		FrameGraphics.requestRefresh(true);
		FrameGraphics.refresh(true);
	}

	public static int getX() {
		return Math.round(MouseX);
	}

	public static int getY() {
		return Math.round(MouseY);
	}

	public static Item getlastHighlightedItem() {
		return _lastHighlightedItem;
	}

	public static Point getPosition() {
		return new Point(getX(), getY());
	}

	public static Point getFreeItemsOffset() {
		return new Point(_offX, _offY);
	}

	public static void shift(KeyEvent e) {
		_shiftDown = e.isShiftDown();
		Help.updateStatus();
		getInstance().refreshHighlights();
	}
}
