package org.expeditee.gui;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.MemoryImageSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.swing.JOptionPane;

import org.expeditee.items.Item;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.settings.UserSettings;
import org.expeditee.stats.SessionStats;
import org.expeditee.taskmanagement.EntitySaveManager;

/**
 * This Interface is used by the Frame to control all display input and output.
 * 
 * @author jdm18
 * 
 */
public class DisplayIO {

	private static final int SMALL_CURSOR_SIZE = 16;

	private static final int MEDIUM_CURSOR_SIZE = 32;

	private static final int LARGE_CURSOR_SIZE = 64;

	/**
	 * The color to be used to highlight the linked parent item, when the user
	 * navigates backwards.
	 */
	public static final Color BACK_HIGHLIGHT_COLOR = Color.MAGENTA;

	private static Browser _Browser;

	// The current Frame being displayed on the screen.
	private static Frame _CurrentFrames[] = new Frame[2];

	// Maintains the list of frames visited thus-far for back-tracking
	@SuppressWarnings("unchecked")
	private static Stack<String>[] _VisitedFrames = new Stack[2];

	@SuppressWarnings("unchecked")
	private static Stack<String>[] _BackedUpFrames = new Stack[2];

	// used to change the mouse cursor position on the screen
	private static Robot _Robot;

	private static boolean _TwinFrames = false;

	/** Notified whenever the frame changes */
	private static HashSet<DisplayIOObserver> _displayIOObservers = new HashSet<DisplayIOObserver>();

	/**
	 * The title to display in the Title bar.
	 */
	public static String TITLE = "ExpDev";

	private DisplayIO() {
	}

	public static void Init(Browser browser) {
		_Browser = browser;
		try {
			_Robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}

		Point mouse = _Browser.getMousePosition();
		if (mouse != null) {
			FrameMouseActions.MouseX = mouse.x;
			FrameMouseActions.MouseY = mouse.y;
		}

		_VisitedFrames[0] = new Stack<String>();
		_VisitedFrames[1] = new Stack<String>();
		_BackedUpFrames[0] = new Stack<String>();
		_BackedUpFrames[1] = new Stack<String>();
	}

	/**
	 * Notifies observers that the frame has changed.
	 */
	private static void fireFrameChanged() {
		for (DisplayIOObserver observer : _displayIOObservers) {
			observer.frameChanged();
		}
	}

	/**
	 * Adds a DisplayIOObserver to the DisplayIO. DisplayIOObserver's are
	 * notified when frame changes.
	 * 
	 * @see #removeDisplayIOObserver(DisplayIOObserver)
	 * 
	 * @param observer
	 *            The observer to add
	 * 
	 * @throws NullPointerException
	 *             If observer is null.
	 */
	public static void addDisplayIOObserver(DisplayIOObserver observer) {
		if (observer == null)
			throw new NullPointerException("observer");
		_displayIOObservers.add(observer);
	}

	/**
	 * Removes a DisplayIOObserver from the DisplayIO.
	 * 
	 * @see #addDisplayIOObserver(DisplayIOObserver)
	 * 
	 * @param observer
	 *            The observer to add
	 * 
	 * @throws NullPointerException
	 *             If observer is null.
	 */
	public static void removeDisplayIOObserver(DisplayIOObserver observer) {
		if (observer == null)
			throw new NullPointerException("observer");
		_displayIOObservers.remove(observer);
	}

	public static void setTextCursor(Text text, int cursorMovement) {
		setTextCursor(text, cursorMovement, false, false, false, false);
	}

	public static void setTextCursor(Text text, int cursorMovement,
			boolean newSize, boolean isShiftDown, boolean isCtrlDown,
			boolean allowClearSelection) {

		int size = Math.round(text.getSize());
		if (allowClearSelection && !isShiftDown && text.hasSelection())
			text.clearSelection();

		Point2D.Float newMouse = text.moveCursor(cursorMovement, DisplayIO
				.getFloatMouseX(), FrameMouseActions.MouseY, isShiftDown,
				isCtrlDown);

		if (!newSize && cursorType == Item.TEXT_CURSOR) {
			if (cursorMovement != 0)
				DisplayIO.setCursorPosition(newMouse, false);
			return;
		}

		cursorType = Item.TEXT_CURSOR;

		// Do some stuff to adjust the cursor size based on the font size
		final int MEDIUM_CURSOR_CUTOFF = 31;
		final int LARGE_CURSOR_CUTOFF = 62;

		int cursorSize = LARGE_CURSOR_SIZE;
		int hotspotPos = 0;
		int start = 0;

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension best_cursor_dim = toolkit.getBestCursorSize(cursorSize,cursorSize);
		int best_cursor_height = best_cursor_dim.height;
		
		if (best_cursor_height < cursorSize) {
			// not able to provide the size of cursor Expeditee wants to
			// => lock on to 'best_cursor_height' and use this to generate dependent values
			cursorSize = best_cursor_height; // OS + Java version dependent: most likely MEDIUM_CURSOR_SIZE
			if (size < best_cursor_height) {
				start = cursorSize - size - 1;
				hotspotPos = cursorSize - (size + 1) / 4;
			}
			else {
				start = size - best_cursor_height;
				hotspotPos = cursorSize -1;
			}
			
		}
		else if (size < MEDIUM_CURSOR_CUTOFF) {
			cursorSize = MEDIUM_CURSOR_SIZE;
			start = cursorSize - size - 1;
			hotspotPos = cursorSize - (size + 1) / 4;
		} else if (size < LARGE_CURSOR_CUTOFF) {
			hotspotPos = cursorSize - (size - 5) / 4;
			start = cursorSize - size - 2;
		} else {
			int FIXED_CURSOR_MIN = 77;
			if (size >= FIXED_CURSOR_MIN) {
				hotspotPos = cursorSize - 2;
			} else {
				hotspotPos = size - (FIXED_CURSOR_MIN - cursorSize);
			}
		}
		
		int[] pixels = new int[cursorSize * cursorSize];

		for (int i = start; i < cursorSize; i++) {
			pixels[i * cursorSize] = pixels[i * cursorSize + 1] = 0xFF000000;
		}

		MemoryImageSource memory_image = new MemoryImageSource(cursorSize, cursorSize, pixels, 0, cursorSize);
		Image image = toolkit.createImage(memory_image);
		
		Cursor textCursor = toolkit.createCustomCursor(image, new Point(0, hotspotPos), "textcursor");
		_Browser.setCursor(textCursor);
		
		if (cursorMovement != Text.NONE) {
			DisplayIO.setCursorPosition(newMouse, false);
		}
	}

	/**
	 * Sets the type of cursor the display should be using
	 * 
	 * @param type
	 *            The type of cursor to display, using constants defined in the
	 *            Cursor class.
	 */
	public static void setCursor(int type) {
		// avoid flicker when not changing
		if (type == cursorType || type == Item.UNCHANGED_CURSOR)
			return;

		cursorType = type;
		
		if(_Browser != null){
			refreshCursor();
		}
	}
	
	public static void refreshCursor() {
		if (cursorType == Item.HIDDEN_CURSOR
				|| (FreeItems.hasCursor() && cursorType == Item.DEFAULT_CURSOR)) {
			int[] pixels = new int[SMALL_CURSOR_SIZE * SMALL_CURSOR_SIZE];
			Image image = Toolkit.getDefaultToolkit().createImage(
					new MemoryImageSource(SMALL_CURSOR_SIZE, SMALL_CURSOR_SIZE,
							pixels, 0, SMALL_CURSOR_SIZE));
			Cursor transparentCursor = Toolkit.getDefaultToolkit()
					.createCustomCursor(image, new Point(0, 0),
							"invisiblecursor");
				_Browser.setCursor(transparentCursor);
		} else 
			_Browser.setCursor(new Cursor(cursorType));
	}

	private static int cursorType = Item.DEFAULT_CURSOR;

	public static int getCursor() {
		return cursorType;
	}

	/**
	 * Moves the mouse cursor to the given x,y coordinates on the screen
	 * 
	 * @param x
	 *            The x coordinate
	 * @param y
	 *            The y coordinate
	 */
	public static void setCursorPosition(float x, float y) {
		setCursorPosition(x, y, true);
	}

	public static void setCursorPosition(float x, float y, boolean forceArrow) {
		// Adjust the position to move the mouse to to account for being in
		// TwinFramesMode
		if (_TwinFrames) {
			if (getCurrentSide() == 1) {
				int middle = getMiddle();
				x += middle;
			}
		}

		float deltax = x - FrameMouseActions.MouseX;
		float deltay = y - FrameMouseActions.MouseY;

		// When the Robot moves the cursor... a short time later a mouseMoved
		// event is generated...
		// We want to ignore this event by remembering the location the robot
		// was shifted to.
		FrameMouseActions.setLastRobotMove(x, y);

		if (FreeItems.itemsAttachedToCursor()) {
			List<Item> toMove = FreeItems.getInstance();
			for (Item move : toMove) {
				move.setPosition(move.getX() + deltax, move.getY() + deltay);
			}
		}

		// cheat
		FrameMouseActions.setForceArrow(forceArrow);
		int mouseX = (int) _Browser.getContentPane().getLocationOnScreen()
				.getX()
				+ Math.round(x);
		int mouseY = (int) _Browser.getContentPane().getLocationOnScreen()
				.getY()
				+ Math.round(y);
		_Robot.mouseMove(mouseX, mouseY);
		// System.out.println("MouseMoved: " + x + "," + y);
	}

	public static void resetCursorOffset() {
		FrameMouseActions.resetOffset();
	}

	/**
	 * Sets the current cursor position in the current frame
	 * 
	 * @param pos
	 */
	public static void setCursorPosition(Point2D.Float pos) {
		setCursorPosition(pos.x, pos.y);
	}

	public static void setCursorPosition(Point pos) {
		setCursorPosition(pos.x, pos.y);
	}

	public static void setCursorPosition(Point pos, boolean forceArrow) {
		setCursorPosition(pos.x, pos.y, forceArrow);
	}

	public static void setCursorPosition(Point2D.Float pos, boolean forceArrow) {
		setCursorPosition(pos.x, pos.y, forceArrow);
	}

	/**
	 * Returns the top item (last added) of the Back-Stack (which is popped off)
	 * 
	 * @return The name of the last Frame added to the back-stack
	 */
	public static String getLastFrame() {
		int side = getCurrentSide();

		if (_VisitedFrames[side].size() > 0)
			return _VisitedFrames[side].pop();
		else
			return null;
	}

	/**
	 * Adds the given Frame to the back-stack
	 * 
	 * @param frame
	 *            The Frame to add
	 */
	public static void addToBack(Frame toAdd) {
		int side = getCurrentSide();

		// // do not allow duplicate frames
		// if (_VisitedFrames[side].size() > 0)
		// if (_VisitedFrames[side].peek().equals(toAdd.getName())) {
		// return;
		// }

		Item ip = FrameUtils.getCurrentItem();
		if (ip == null)
			_VisitedFrames[side].push(toAdd.getName());
		else
			_VisitedFrames[side].push(toAdd.getName());
		// System.out.println("Added: " + _VisitedFrames[side].size());
	}

	public static String removeFromBack() {
		int side = getCurrentSide();

		// there must be a frame to go back to
		if (_VisitedFrames[side].size() > 0) {
			return _VisitedFrames[side].pop();
		}
		return null;
	}

	/**
	 * Returns a 'peek' at the end element on the back-stack of the current
	 * side. If the back-stack is empty, null is returned.
	 * 
	 * @return The name of the most recent Frame added to the back-stack, or
	 *         null if the back-stack is empty.
	 */
	public static String peekFromBackUpStack() {
		int side = getCurrentSide();

		// check that the stack is not empty
		if (_VisitedFrames[side].size() > 0)
			return _VisitedFrames[side].peek();

		// if the stack is empty, return null
		return null;
	}

	public static void setCurrentFrame(Frame frame, boolean incrementStats) {
		if (frame == null)
			return;

		if (_TwinFrames) {
			if (_CurrentFrames[0] == null) {
				_CurrentFrames[0] = frame;
				fireFrameChanged();
				return;
			}
			if (_CurrentFrames[1] == null) {
				_CurrentFrames[1] = frame;
				fireFrameChanged();
				return;
			}
		}

		// if this is already the current frame
		if (frame == getCurrentFrame()) {
			FrameGraphics.Repaint();
			MessageBay.displayMessage(frame.getName()
					+ " is already the current frame.");
			return;
		} else if (incrementStats) {
			SessionStats.AccessedFrame();
		}

		// Invalidate free items
		if (!FreeItems.getInstance().isEmpty() && getCurrentFrame() != null) {

			// Empty free items temporarily so that the old frames buffer is
			// repainted
			// without the free items.
			ArrayList<? extends Item> tmp = (ArrayList<? extends Item>) FreeItems
					.getInstance().clone();
			FreeItems.getInstance().clear(); // NOTE: This will invalidate
			// all the cleared free items
			FrameGraphics.refresh(true);
			FreeItems.getInstance().addAll(tmp);

		}

		// Changing frames is a Save point for saveable entities:
		EntitySaveManager.getInstance().saveAll();

		if (_TwinFrames) {
			// if the same frame is being shown in both sides, load a fresh
			// copy from disk
			if (_CurrentFrames[getOppositeSide()] == frame
					|| _CurrentFrames[getOppositeSide()].hasOverlay(frame)) {
				FrameIO.SuspendCache();
				frame = FrameIO.LoadFrame(frame.getName());
				FrameIO.ResumeCache();
			}

			// If the frames are the same then the items for the
			// frame that is just about to hide will still be in view
			// so only notify items that they are hidden if the
			// frames differ.
			if (_CurrentFrames[getCurrentSide()] != null
					&& _CurrentFrames[0] != _CurrentFrames[1]) {
				for (Item i : _CurrentFrames[getCurrentSide()].getItems()) {
					i.onParentStateChanged(new ItemParentStateChangedEvent(
							_CurrentFrames[getCurrentSide()],
							ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN));
				}
			}
			_CurrentFrames[getCurrentSide()] = frame;

			// BROOK : TODO... overlays and loadable widgets
			for (Item i : _CurrentFrames[getCurrentSide()].getItems()) {
				i.onParentStateChanged(new ItemParentStateChangedEvent(
						_CurrentFrames[getCurrentSide()],
						ItemParentStateChangedEvent.EVENT_TYPE_SHOWN));
			}
		} else {

			// Notifying items on the frame being hidden that they
			// are about to be hidden.
			// ie. Widgets use this method to remove themselves from the JPanel
			List<Frame> currentOnlyOverlays = new LinkedList<Frame>();
			List<Frame> nextOnlyOverlays = new LinkedList<Frame>();
			List<Frame> sharedOverlays = new LinkedList<Frame>();

			// Get all overlayed frames seen by the next frame
			for (Overlay o : frame.getOverlays()) {
				if (!nextOnlyOverlays.contains(o))
					nextOnlyOverlays.add(o.Frame);
			}

			// Get all overlayed frames seen by the current frame
			if (_CurrentFrames[getCurrentSide()] != null) {
				for (Overlay o : _CurrentFrames[getCurrentSide()].getOverlays()) {
					if (!currentOnlyOverlays.contains(o))
						currentOnlyOverlays.add(o.Frame);
				}
			}

			// Extract shared overlays between the current and next frame
			for (Frame of : currentOnlyOverlays) {
				if (nextOnlyOverlays.contains(of)) {
					sharedOverlays.add(of);
				}
			}

			// The first set, currentOnlyOverlays, must be notified that they
			// are hidden
			Collection<Item> items = new LinkedList<Item>();

			// Notify items that will not be in view any more
			if (_CurrentFrames[getCurrentSide()] != null) {
				List<Frame> seen = new LinkedList<Frame>();
				seen.addAll(sharedOverlays); // Signify that seen all shared
				// overlays
				seen.remove(_CurrentFrames[getCurrentSide()]); // must ensure
				// excluded

				// Get all items seen from the current frame - including all
				// possible non-shared overlays
				items = _CurrentFrames[getCurrentSide()].getAllItems();
				for (Frame f : seen)
					items.removeAll(f.getAllItems());

				// Notify items that they are hidden
				for (Item i : items) {
					i.onParentStateChanged(new ItemParentStateChangedEvent(
							_CurrentFrames[getCurrentSide()],
							ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN));
				}
			}

			// Set the new frame
			_CurrentFrames[getCurrentSide()] = frame;
			frame.refreshSize();
			// Notify items on the frame being displayed that they are in view
			// ie. widgets use this method to add themselves to the content pane
			items.clear();

			// Notify overlay items that they are shown
			for (Item i : frame.getOverlayItems()) {
				Overlay owner = frame.getOverlayOwner(i);
				// if (owner == null) i.onParentFameShown(false, 0);
				// else ...
				assert (owner != null);
				i
						.onParentStateChanged(new ItemParentStateChangedEvent(
								frame,
								ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY,
								owner.permission));
			}

			for (Item i : frame.getItems()) {
				i.onParentStateChanged(new ItemParentStateChangedEvent(frame,
						ItemParentStateChangedEvent.EVENT_TYPE_SHOWN));
			}
		}
		frame.reset();
		FrameMouseActions.getInstance().refreshHighlights();
		FrameGraphics.refresh(false);
		fireFrameChanged();
	}

	public static void UpdateTitle() {
		StringBuffer title = new StringBuffer(TITLE);

		if (FrameGraphics.isAudienceMode())
			title.append(" - Audience Mode");
		else if (FrameGraphics.isXRayMode())
			title.append(" - X-Ray Mode");
		else
			title.append(" [").append(SessionStats.getShortStats()).append(']');

		_Browser.setTitle(DisplayIO.title + " ~~~ " + title.toString());
	}
	
	private static String title = "";
	public static void setTitle(String str) {
	    title = str;
	}

	public static int getCurrentSide() {
		if (_Browser == null)
			return 0;

		if (_TwinFrames
				&& FrameMouseActions.MouseX >= (_Browser.getWidth() / 2F)
				&& _CurrentFrames[1] != null)
			return 1;

		if (_CurrentFrames[0] == null && _CurrentFrames[1] != null)
			return 1;

		return 0;
	}

	private static int getOppositeSide() {
		if (getCurrentSide() == 0)
			return 1;

		return 0;
	}

	public static int FrameOnSide(Frame toFind) {
		if (_CurrentFrames[0] == toFind)
			return 0;

		if (_CurrentFrames[1] == toFind)
			return 1;

		return -1;
	}

	/**
	 * Returns the Frame currently being displayed on the screen.
	 * 
	 * @return The Frame currently displayed.
	 */
	public static Frame getCurrentFrame() {
		return _CurrentFrames[getCurrentSide()];
	}

	public static Frame getOppositeFrame() {
		return _CurrentFrames[getOppositeSide()];
	}

	public static Frame[] getFrames() {
		return _CurrentFrames;
	}

	public static int getMiddle() {
		return _Browser.getWidth() / 2;
	}

	public static int getHeight() {
		return _Browser.getHeight();
	}

	/**
	 * Returns the current mouse X coordinate. This coordinate is relative to
	 * the left edge of the frame the mouse is in. It takes into account the
	 * user being in twin frames mode.
	 * 
	 * @return The X coordinate of the mouse.
	 */
	public static float getFloatMouseX() {
		if (_TwinFrames
				&& FrameMouseActions.MouseY < FrameGraphics.getMaxSize().height)
			return FrameMouseActions.MouseX % (_Browser.getWidth() / 2);

		return FrameMouseActions.MouseX;
	}


	/**
	 * Returns the current mouse X coordinate. This coordinate is relative to
	 * the left edge of the frame the mouse is in. It takes into account the
	 * user being in twin frames mode.
	 * 
	 * @return The X coordinate of the mouse.
	 */
	public static int getMouseX() {
		return Math.round(getFloatMouseX());
	}
	
	
	/**
	 * Returns the current mouse Y coordinate. This coordinate is relative to
	 * the top edge of the frame the mouse is in. 
	 * 
	 * @return The Y coordinate of the mouse.
	 */
	public static float getFloatMouseY() {
	
		return FrameMouseActions.MouseY;
	}
	
	/**
	 * Returns the current mouse Y coordinate. This coordinate is relative to
	 * the top edge of the frame the mouse is in. 
	 * 
	 * @return The Y coordinate of the mouse.
	 */
	public static int getMouseY() {
		return Math.round(getFloatMouseY());
	}
	
	public static boolean Back() {
		int side = getCurrentSide();

		// there must be a frame to go back to
		if (_VisitedFrames[side].size() < 1) {
			MessageBay.displayMessageOnce("You are already on the home frame");
			return false;
		}

		if (!FrameUtils.LeavingFrame(getCurrentFrame())) {
			MessageBay.displayMessage("Error navigating back");
			return false;
		}

		String oldFrame = getCurrentFrame().getName().toLowerCase();

		// do not get a cached version (in case it is in the other window)
		if (isTwinFramesOn())
			FrameIO.SuspendCache();
		Frame frame = FrameIO.LoadFrame(removeFromBack());
		// If the top frame on the backup stack is the current frame go back
		// again... or if it has been deleted
		// Recursively backup the stack
		if (frame == null || frame.equals(getCurrentFrame())) {
			Back();
			return false;
		}

		if (isTwinFramesOn()) {
			FrameIO.ResumeCache();
		}
		_BackedUpFrames[side].push(oldFrame);
		FrameUtils.DisplayFrame(frame, false, true);
		FrameMouseActions.setHighlightHold(true);

		for (Item i : frame.getItems()) {
			if (i.getLink() != null
					&& i.getAbsoluteLink().toLowerCase().equals(oldFrame)) {
				if (i.getHighlightMode() != Item.HighlightMode.Normal) {
					i.setHighlightMode(Item.HighlightMode.Normal,
							BACK_HIGHLIGHT_COLOR);
				}
				// check if its an @f item and if so update the buffer
				if (i instanceof Picture) {
					Picture p = (Picture) i;
					p.refresh();
				}
			}
		}
		FrameGraphics.requestRefresh(true);
		return true;
	}

	public static boolean Forward() {
		int side = getCurrentSide();

		// there must be a frame to go back to
		if (_BackedUpFrames[side].size() == 0) {
			return false;
		}

		if (!FrameUtils.LeavingFrame(getCurrentFrame())) {
			MessageBay.displayMessage("Error navigating forward");
			return false;
		}

		String oldFrame = getCurrentFrame().getName().toLowerCase();

		// do not get a cached version (in case it is in the other window)
		if (isTwinFramesOn())
			FrameIO.SuspendCache();
		Frame frame = FrameIO.LoadFrame(_BackedUpFrames[side].pop());
		// If the top frame on the backup stack is the current frame go back
		// again... or if it has been deleted
		// Recursively backup the stack
		if (frame == null || frame.equals(getCurrentFrame())) {
			Forward();
			return false;
		}

		if (isTwinFramesOn()) {
			FrameIO.ResumeCache();
		}
		_VisitedFrames[side].push(oldFrame);
		FrameUtils.DisplayFrame(frame, false, true);
		FrameGraphics.requestRefresh(true);
		return true;
	}

	/**
	 * Toggles the display of frames between TwinFrames mode and Single frame
	 * mode.
	 */
	public static void ToggleTwinFrames() {
		// determine which side is the active side
		int opposite = getOppositeSide();
		int current = getCurrentSide();
		_TwinFrames = !_TwinFrames;

		// if TwinFrames is being turned on
		if (_TwinFrames) {
			// if this is the first time TwinFrames has been toggled on,
			// load the user's first frame
			if (_VisitedFrames[opposite].size() == 0) {
				FrameIO.SuspendCache();
				setCurrentFrame(FrameIO.LoadFrame(UserSettings.HomeFrame.get()), true);
				FrameIO.ResumeCache();
			} else {
				// otherwise, restore the frame from the side's back-stack
				setCurrentFrame(FrameIO.LoadFrame(_VisitedFrames[opposite]
						.pop()), true);
			}

			// else, TwinFrames is being turned off
		} else {
			// add the frame to the back-stack
			Frame hiding = _CurrentFrames[opposite];
			FrameUtils.LeavingFrame(hiding);
			_VisitedFrames[opposite].add(hiding.getName());
			_CurrentFrames[opposite] = null;
			_CurrentFrames[current].refreshSize();
		}
		if (_CurrentFrames[current] != null)
			_CurrentFrames[current].refreshSize();
		if (_CurrentFrames[opposite] != null)
			_CurrentFrames[opposite].refreshSize();

		FrameGraphics.Clear();
		FrameGraphics.requestRefresh(false);
		FrameGraphics.Repaint();
	}

	public static boolean isTwinFramesOn() {
		return _TwinFrames;
	}

	public static void Reload(int side) {
		if (side < 0)
			return;

		FrameIO.SuspendCache();
		_CurrentFrames[side] = FrameIO
				.LoadFrame(_CurrentFrames[side].getName());
		FrameIO.ResumeCache();
	}

	public static boolean DisplayConfirmDialog(String message, String title,
			int type, int options, int res) {
		return JOptionPane.showConfirmDialog(_Browser, message, title, options,
				type) == res;
	}

	public static final int RESULT_OK = JOptionPane.OK_OPTION;

	public static final int OPTIONS_OK_CANCEL = JOptionPane.OK_CANCEL_OPTION;

	public static final int TYPE_WARNING = JOptionPane.WARNING_MESSAGE;

	public static void pressMouse(int buttons) {
		_Robot.mousePress(buttons);
	}

	public static void releaseMouse(int buttons) {
		_Robot.mouseRelease(buttons);
	}

	public static void clickMouse(int buttons) throws InterruptedException {
		_Robot.mousePress(buttons);
		Thread.sleep(100);
		_Robot.mouseRelease(buttons);
	}

	public static void typeKey(int key) throws InterruptedException {
		_Robot.keyPress(key);
		// _Robot.waitForIdle();
		_Robot.keyRelease(key);
		// _Robot.waitForIdle();
		Thread.sleep(200);
	}

	public static void typeText(String s) throws InterruptedException {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isUpperCase(c))
				_Robot.keyPress(KeyEvent.VK_SHIFT);
			typeKey(getKeyCode(c));
			if (Character.isUpperCase(c))
				_Robot.keyRelease(KeyEvent.VK_SHIFT);
		}
	}

	protected static int getKeyCode(char c) {
		switch (c) {
		case '\n':
			return KeyEvent.VK_ENTER;
		case ' ':
			return KeyEvent.VK_SPACE;
		}

		if (Character.isSpaceChar(c))
			return KeyEvent.VK_SPACE;
		else if (Character.isDigit(c)) {
			return (int) (KeyEvent.VK_0 + c - '0');
		} else if (Character.isUpperCase(c)) {
			return c;
		}

		return (int) (KeyEvent.VK_A + c - 'a');
	}

	/**
	 * Moves the cursor the end of this item.
	 * 
	 * @param i
	 */
	public static void MoveCursorToEndOfItem(Item i) {
		setTextCursor((Text) i, Text.END, true, false, false, false);
	}

	public static void translateCursor(int deltaX, int deltaY) {
		setCursorPosition(FrameMouseActions.MouseX + deltaX,
				FrameMouseActions.MouseY + deltaY, false);
	}

	public static void clearBackedUpFrames() {
		_BackedUpFrames[getCurrentSide()].clear();
	}

	/**
	 * @param secondsDelay
	 * @param s
	 * @throws InterruptedException
	 */
	public static void typeStringDirect(double secondsDelay, String s)
			throws InterruptedException {
		for (int i = 0; i < s.length(); i++) {
			FrameKeyboardActions.processChar(s.charAt(i), false);
			Thread.sleep((int) (secondsDelay * 1000));
		}
	}

	public static List<String> getUnmodifiableVisitedList() {
		return Collections.unmodifiableList(_VisitedFrames[getCurrentSide()]);
	}
}
