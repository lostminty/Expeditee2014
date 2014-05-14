package org.expeditee.gui;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.expeditee.io.ExpClipReader;
import org.expeditee.io.ItemSelection;
import org.expeditee.io.ItemSelection.ExpDataHandler;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;

/**
 * The gateway for mouse input; conditionally forwards mouse messages to swing
 * components / expeditee frames for the Browser.
 * 
 * Various mouse listeners can attatch themselves here to listen to mouse events
 * forwared to expeditee .. this excludes widget-exclusive mouse messages.
 * 
 * @author Brook Novak
 * 
 */
public class MouseEventRouter extends JComponent {

	private static final long serialVersionUID = 1L;

	private JMenuBar _menuBar;

	private Container _contentPane;

	private List<MouseListener> _mouseListeners = new LinkedList<MouseListener>();

	private List<MouseMotionListener> _mouseMotionListeners = new LinkedList<MouseMotionListener>();

	private List<MouseWheelListener> _mouseWheelListeners = new LinkedList<MouseWheelListener>();

	private static MouseEvent _currentMouseEvent = null;

	/**
	 * Routes events on given frame layers... the menu bar and content pane
	 * 
	 * @param menuBar
	 *            Must not be null.
	 * 
	 * @param contentPane
	 *            Must not be null.
	 */
	public MouseEventRouter(JMenuBar menuBar, Container contentPane) {

		if (contentPane == null)
			throw new NullPointerException("contentPane");

		// Listen for all AWT events (ensures popups are included)
		Toolkit.getDefaultToolkit().addAWTEventListener(
				new EventCatcher(),
				AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK
						| AWTEvent.MOUSE_WHEEL_EVENT_MASK);

		this._menuBar = menuBar;
		this._contentPane = contentPane;
	}

	/**
	 * Listens only to events to frames... i.e. to exeditee, not to widgets.
	 * 
	 * @param listener
	 *            The listener to add.
	 */
	public void addExpediteeMouseListener(MouseListener listener) {
		if (listener == null)
			throw new NullPointerException("listener");
		_mouseListeners.add(listener);
	}

	public void removeExpediteeMouseListener(MouseListener listener) {
		if (listener == null)
			throw new NullPointerException("listener");
		_mouseListeners.remove(listener);
	}

	public void addExpediteeMouseMotionListener(MouseMotionListener listener) {
		if (listener == null)
			throw new NullPointerException("listener");
		_mouseMotionListeners.add(listener);
	}

	public void removeExpediteeMouseMotionListener(MouseMotionListener listener) {
		if (listener == null)
			throw new NullPointerException("listener");
		_mouseMotionListeners.remove(listener);
	}

	public void addExpediteeMouseWheelListener(MouseWheelListener listener) {
		if (listener == null)
			throw new NullPointerException("listener");
		_mouseWheelListeners.add(listener);
	}

	public void removeExpediteeMouseWheelListener(MouseWheelListener listener) {
		if (listener == null)
			throw new NullPointerException("listener");
		_mouseWheelListeners.remove(listener);
	}

	/**
	 * Conceal event catching from outside
	 * 
	 * @author Brook Novak
	 */
	private class EventCatcher implements AWTEventListener {

		/**
		 * All events for every component in frame are fired to here
		 */
		public void eventDispatched(AWTEvent event) {
			if (event instanceof MouseEvent) {
				routeMouseEvent((MouseEvent) event);
			}
		}

	}

	/**
	 * Forwards the mouse event to its appropriate destination. For events that
	 * belong to Expiditee space the events are consumed and manually routed to
	 * the mouse actions handler.
	 * 
	 * @param e
	 *            The mouse event for any component in the browser frame
	 */
	private void routeMouseEvent(MouseEvent e) {
		_currentMouseEvent = e;

		// First convert the point to expeditee space
		Point containerPoint = SwingUtilities.convertPoint(e.getComponent(), e
				.getPoint(), _contentPane);
		
		// TODO: Find a reliable way of detecting when the mouse moved onto a window that isn't a child of ours
		if(e.getID() == MouseEvent.MOUSE_EXITED) {
			// System.out.println(e.getComponent());
			if(containerPoint.x <= 0 || containerPoint.x >= _contentPane.getWidth() ||
					containerPoint.y <= 0 || containerPoint.y >= _contentPane.getHeight()) {
				FrameMouseActions.mouseExitedWindow(e);
			}
		}

		if (containerPoint.y < 0) { // not in the content pane

			if (_menuBar != null
					&& containerPoint.y + _menuBar.getHeight() >= 0) {
				// The mouse event is over the menu bar.
				// Could handle specially.
			} else {
				// The mouse event is over non-system window
				// decorations, such as the ones provided by
				// the Java look and feel.
				// Could handle specially.
			}

		} else {

			// Check to see if the mouse is over an expeditee item or
			// whether an expeditee item is currently picked up
			boolean forwardToExpiditee = false;
			boolean isOverPopup = PopupManager.getInstance().isPointOverPopup(
					containerPoint);
			if (isOverPopup) {
				// Popups have highest preference
				// forwardToExpiditee = false...

				// If there ate items in free space - keep them moving with the
				// cursor over the
				// popups.
				if (!FreeItems.getInstance().isEmpty()) {
					FrameMouseActions.move(FreeItems.getInstance());
				}

				// Note: all frame.content pane events belong to expeditee
			} else if (e.getSource() == _contentPane
					|| e.getSource() == Browser._theBrowser
					|| !FreeItems.getInstance().isEmpty()) {
				forwardToExpiditee = true;
			} else if (DisplayIO.getCurrentFrame() != null) {
				/* is mouse over a specific expeditee item? */
				// NOTE: Do not use FrameUtils.getCurrentItem() - thats relevent
				// for old mouse position only
				/*
				 * for (Item i : DisplayIO.getCurrentFrame().getItems()) { if
				 * (i.getPolygon().contains(containerPoint)) {
				 * forwardToExpiditee = true; break; } }
				 */// ABOVE: Does not consider overlays
				// NOTE: Below is an expensive operation and could be re-used
				// when passing mouse events!!!
				forwardToExpiditee = (FrameUtils.onItem(DisplayIO
						.getCurrentFrame(), containerPoint.x, containerPoint.y,
						true) != null);
			}

			// Create artificial mouse event and forward it to expeditee
			MouseEvent expediteeEvent = SwingUtilities.convertMouseEvent(e
					.getComponent(), e, _contentPane);

			// NOTE: Convert util masks-out the needed extensions
			MouseEvent withExtensions = null;

			if (forwardToExpiditee) {

				// Ensure that underlying widgets don't get the event
				e.consume();

				switch (e.getID()) {
				case MouseEvent.MOUSE_MOVED:

					for (MouseMotionListener listener : _mouseMotionListeners) {
						listener.mouseMoved(expediteeEvent);
					}

					// Ensure that expiditee has focus only if no popups exist
					if (Browser._theBrowser != null
							&& Browser._theBrowser.getContentPane() != null) {
						if (!Browser._theBrowser.getContentPane()
								.isFocusOwner()
								&& !isPopupVisible()) {
							Browser._theBrowser.getContentPane().requestFocus();
						}
					}

					break;

				case MouseEvent.MOUSE_CLICKED:

					withExtensions = duplicateMouseEvent(expediteeEvent, e
							.getModifiers()
							| e.getModifiersEx());

					for (MouseListener listener : _mouseListeners) {
						listener.mouseClicked(withExtensions);
					}

					break;

				case MouseEvent.MOUSE_PRESSED:

					withExtensions = duplicateMouseEvent(expediteeEvent, e
							.getModifiers()
							| e.getModifiersEx());

					for (MouseListener listener : _mouseListeners) {
						listener.mousePressed(withExtensions);
					}

					break;

				case MouseEvent.MOUSE_RELEASED:
					withExtensions = duplicateMouseEvent(expediteeEvent, e
							.getModifiers()
							| e.getModifiersEx());

					for (MouseListener listener : _mouseListeners) {
						listener.mouseReleased(withExtensions);
					}

					break;
				case MouseEvent.MOUSE_WHEEL:
					MouseWheelEvent mwe = (MouseWheelEvent) expediteeEvent;
					for (MouseWheelListener listener : _mouseWheelListeners) {
						listener.mouseWheelMoved(mwe);
					}
					break;
				case MouseEvent.MOUSE_ENTERED:
					withExtensions = duplicateMouseEvent(expediteeEvent, e
							.getModifiers()
							| e.getModifiersEx());
					for (MouseListener listener : _mouseListeners) {
						listener.mouseEntered(withExtensions);
					}
					break;
				case MouseEvent.MOUSE_EXITED:
					for (MouseListener listener : _mouseListeners) {
						listener.mouseExited(withExtensions);
					}
					break;
				case MouseEvent.MOUSE_DRAGGED:
					for (MouseMotionListener listener : _mouseMotionListeners) {
						listener.mouseDragged(expediteeEvent);
					}
					break;
				}

			} else {

				// Keep expeditees mouse X/Y updated
				FrameMouseActions.MouseX = expediteeEvent.getX();
				FrameMouseActions.MouseY = expediteeEvent.getY();

				// If forwarding to swing ensure that widgets are not
				// highlighted
				// to give visual feedback yo users such that swing has focus.
				Item i = FrameMouseActions.getlastHighlightedItem();
				if (i != null
						&& i.getHighlightMode() != Item.HighlightMode.None) {
					FrameGraphics.changeHighlightMode(i,
							Item.HighlightMode.None);
				}

				// Also bring expideditee behaviour to swing: Auto-focus on
				// component
				// whenever the mouse moves over it.
				if (e.getID() == MouseEvent.MOUSE_MOVED) {
					if (e.getSource() instanceof Component) {
						Component target = (Component) e.getSource();
						if (!target.isFocusOwner()) {
							target.requestFocus();
						}
					}
					// Auto-hide popups when user click on something other other
					// than
					// a popup - and is not a on a popup invoker
				} else if (!isOverPopup
						&& e.getID() == MouseEvent.MOUSE_PRESSED
						&& !PopupManager.getInstance().isInvoker(
								e.getComponent())) {
					PopupManager.getInstance().hideAutohidePopups();
				}
			}
		}
		
		Help.updateStatus();
		
	}

	public static boolean isPopupVisible() {
		return isPopupVisible(Browser._theBrowser.getLayeredPane());
	}

	private static boolean isPopupVisible(Container parent) {

		for (Component c : parent.getComponents()) {

			if (c instanceof JPopupMenu && ((JPopupMenu) c).isVisible()) {
				return true;

			} else if (c instanceof Container
					&& c != Browser._theBrowser.getContentPane()) {
				if (isPopupVisible((Container) c))
					return true;
			}
		}

		return false;
	}

	private MouseEvent duplicateMouseEvent(MouseEvent e, int modifiers) {
		return new MouseEvent(e.getComponent(), e.getID(), e.getWhen(),
				modifiers, e.getX(), e.getY(),
				/** The below methods are not compatible with Java 1.5 */
				/*
				 * e.getXOnScreen(), e.getYOnScreen(),
				 */
				e.getClickCount(), e.isPopupTrigger(), e.getButton());
	}

	public static MouseEvent getCurrentMouseEvent() {

		return _currentMouseEvent;

	}
}