package org.apollo.items;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.MouseEventRouter;
import org.expeditee.gui.Popup;
import org.expeditee.gui.PopupManager;
import org.expeditee.items.Text;
import org.expeditee.items.Item.HighlightMode;

/**
 * 
 * Its a terrible hack - but it will do.
 * 
 * @author Brook Novak
 *
 */
public class EmulatedTextItem {

	private static final long serialVersionUID = 1L;
	
	private static Robot robot;

	private List<TextChangeListener> textChangeListeners = new LinkedList<TextChangeListener>();
	
	private RestrictedTextItem emulatedSource;
	private LabelEditPopup popup = null;
	private JComponent parentComponant = null;
	private Point masterOffset;
	
	private static final Font DEFAULT_FONT = new Font("Serif-Plain", Font.PLAIN, 12);
	
	static {
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Constructor.
	 * 
	 * @param parent
	 * 		Must not be null: the parent that lives directly on the content pane.
	 * 
	 * @param offset
	 * 		The offset to add to the label position - from the parents top left corner. 
	 * 		Must not be null.
	 */
	public EmulatedTextItem(JComponent parent, Point offset) {
		assert(parent != null);
		assert(offset != null);
		
		parentComponant = parent;
		masterOffset = offset;
		emulatedSource = new RestrictedTextItem();
		emulatedSource.setPosition(parent.getX() + offset.x, parent.getY() + offset.y);
		emulatedSource.setFont(DEFAULT_FONT);

	}
	
	
	public void setFontStyle(int style) {
		emulatedSource.setFont(emulatedSource.getFont().deriveFont(style));
	}
	
	public void setFontSize(float size) {
		emulatedSource.setSize(size);
	}
	
	
	/**
	 * Determines whether or no a mouse event is on the emulated text item.
	 * @param swingME
	 * 		Can be null.
	 * 
	 * @return
	 * 		True if the event is over the emulated item.
	 */
	private boolean isOnEmulatedText(MouseEvent swingME) {
		if (swingME == null) return false;
		Point p = convertToExpediteeSpace(swingME.getLocationOnScreen());
		return isOnEmulatedText(p.x, p.y);
	}
	
	private boolean isOnEmulatedText(int expX, int expY) {
		return emulatedSource.contains(expX, expY);
	}
	
	private Point convertToExpediteeSpace(Point screenPos) {
		assert(screenPos != null);
		
		Point expPoint = new Point(0, 0);
		SwingUtilities.convertPointToScreen(expPoint, Browser._theBrowser.getContentPane());
		
		return new Point(screenPos.x - expPoint.x, screenPos.y - expPoint.y);
	}
	
	/**
	 * Applies highlights - shows popup
	 * 
	 * @param e
	 * @param parent
	 */
	public boolean onMouseMoved(MouseEvent e, Component parent) {

		if (isOnEmulatedText(e)) {
			
			if (emulatedSource.getHighlightMode() != HighlightMode.Normal) {
				emulatedSource.setHighlightMode(HighlightMode.Normal);
				repaint();
			}
			
			if (parent != popup && 
					(emulatedSource.getBoundsWidth() + masterOffset.x) > (parent.getWidth() - 10)) { // have some give to transition over widget edge
				if (popup == null) {
					popup = new LabelEditPopup();
				}
				
				int x = (emulatedSource.getPolygon() == null) ? parent.getX() + masterOffset.x : emulatedSource.getPolygon().getBounds().x;
				int y = (emulatedSource.getPolygon() == null) ? parent.getY() + masterOffset.y : emulatedSource.getPolygon().getBounds().y;
				x -= 2;
				y -= 2;

				if (!PopupManager.getInstance().isShowing(popup)) {
					PopupManager.getInstance().showPopup(
							popup, 
							new Point(x, y), 
							popup);  
				}
				
			}
			
			return true;
			
		} else {
			
			lostFocus();

		}
		
		return false;
		
	}
	
	/**
	 * 
	 * @param e
	 * 
	 * @param parent
	 */
	public boolean onKeyPressed(KeyEvent e, Component parent) {

		MouseEvent currentME = MouseEventRouter.getCurrentMouseEvent();
		
		if (currentME == null) return false;
		
		Point p = convertToExpediteeSpace(currentME.getLocationOnScreen());

		// If so and the event really comes from its parent ...
		if (currentME.getComponent() == parent && 
				(isOnEmulatedText(p.x, p.y))) {
			
			int yOffset = (parent == popup) ? -6 : -6;
			
			Browser._theBrowser.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

			// If a alphanumeric text entry...
			if (!e.isActionKey() && 
					(Character.isLetterOrDigit(e.getKeyChar()) || 
							(e.getKeyChar() == KeyEvent.VK_BACK_SPACE && !emulatedSource.isEmpty()) ||
							(e.getKeyChar() == KeyEvent.VK_DELETE && !emulatedSource.isEmpty()) ||
							e.getKeyChar() == KeyEvent.VK_SPACE ||
							e.getKeyChar() == KeyEvent.VK_UNDERSCORE) ||
							e.getKeyChar() == KeyEvent.VK_AMPERSAND ||
							e.getKeyChar() == KeyEvent.VK_OPEN_BRACKET ||
							e.getKeyChar() == KeyEvent.VK_CLOSE_BRACKET ||
							e.getKeyChar() == KeyEvent.VK_MINUS) {


				Rectangle[] oldArea = emulatedSource.getDrawingArea();
				int oldWidth = emulatedSource.getBoundsWidth();
				
				// Insert the text according to the current mouse position
				Point2D.Float newMouse = emulatedSource.insertChar(
						e.getKeyChar(), 
						p.x, 
						p.y);
				
				List<String> lines = emulatedSource.getTextList();
				if (lines != null && lines.size() > 1) {
					emulatedSource.setText(lines.get(0));
				}
				
				// Notify observers
				fireTextChanged();

				// Move "cursured mouse"
				if (newMouse != null) {
					Point bloc = Browser._theBrowser.getContentPane().getLocationOnScreen();
					robot.mouseMove(bloc.x + (int)newMouse.x, bloc.y + (int)newMouse.y + yOffset);
				}
				
				// Invalidate emulated item if not in popup (popup will naturally handle invalidation)
				if (parent != popup) {

					if (oldWidth > emulatedSource.getBoundsWidth()) {
						for (Rectangle r : oldArea) 
							FrameGraphics.invalidateArea(r);
					}
					
					repaint();

				}
				
			} else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
				
				Point2D.Float newMouse = emulatedSource.moveCursor(
						(e.getKeyCode() == KeyEvent.VK_LEFT) ? Text.LEFT : Text.RIGHT, 
						(float)p.getX(), (float)p.getY(), false, false);
				
				if (newMouse != null) {
					Point bloc = Browser._theBrowser.getContentPane().getLocationOnScreen();
					robot.mouseMove(bloc.x + (int)newMouse.x, bloc.y + (int)newMouse.y + yOffset);
				}

			}
			
			if (emulatedSource.hasSelection()) {
				emulatedSource.clearSelection();
				repaint();
			}
			
			return true;
			
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param e
	 * 
	 * @param parent
	 */
	public boolean onKeyReleased(KeyEvent e, Component parent) {
		return isOnEmulatedText(MouseEventRouter.getCurrentMouseEvent());
		
	}
	
	
	public boolean onMouseReleased(MouseEvent e) {
		
		Point p = convertToExpediteeSpace(e.getLocationOnScreen());

		// If so and the event really comes from its parent ...
		if (isOnEmulatedText(p.x, p.y)) {
			
			
			String toMoveIntoFreespace = null;
			
			if (e.getButton() == MouseEvent.BUTTON1) {
				// LINK
			} else if (e.getButton() == MouseEvent.BUTTON2 && 
					emulatedSource.hasSelection() && 
					!emulatedSource.isEmpty() &&
					emulatedSource.selectionMouseButton == MouseEvent.BUTTON2) {
	
				invalidate();
				
				toMoveIntoFreespace = emulatedSource.cutSelectedText();
				
			} else if (e.getButton() == MouseEvent.BUTTON3 && 
					emulatedSource.hasSelection() && 
					!emulatedSource.isEmpty() &&
					emulatedSource.selectionMouseButton == MouseEvent.BUTTON3) {
				toMoveIntoFreespace = emulatedSource.copySelectedText();
				
			} else if (e.getButton() == MouseEvent.BUTTON3 && 
					!emulatedSource.hasSelection() && 
					!emulatedSource.isEmpty()) {
				toMoveIntoFreespace = emulatedSource.getText();
			}
			
			if (toMoveIntoFreespace != null) {
				
				Frame target = DisplayIO.getCurrentFrame();
				if (target != null) {
					Text selectionCopy = new Text(target.getNextItemID(), toMoveIntoFreespace);
					selectionCopy.setPosition(p.x, p.y);
					selectionCopy.setSize(emulatedSource.getSize());
					FrameMouseActions.pickup(selectionCopy);
					lostFocus();
				}
				
				
			}
			
			emulatedSource.clearSelection();
			repaint();
			
			return true;
		}
		
		return false;
	}
	
	public boolean onMousePressed(MouseEvent e) {

		Point p = convertToExpediteeSpace(e.getLocationOnScreen());

		// If so and the event really comes from its parent ...
		if (isOnEmulatedText(p.x, p.y)) {
			
			emulatedSource.clearSelection();
			emulatedSource.setSelectionStart(p.x, p.y, e.getButton());

			repaint();
			
			return true;
		}
		
		return false;
	}
	
	public boolean onMouseClicked(MouseEvent e) {
		return isOnEmulatedText(e);
	}
	public boolean onMouseDragged(MouseEvent e) {

		Point p = convertToExpediteeSpace(e.getLocationOnScreen());

		// If so and the event really comes from its parent ...
		if (isOnEmulatedText(p.x, p.y)) {

			Browser._theBrowser.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
			
			emulatedSource.setSelectionEnd(p.x, p.y);
			repaint();
			
			return true;
		}
		
		return false;
	}
	
	public void addTextChangeListener(TextChangeListener listener) {
		assert(listener != null);
		if (!textChangeListeners.contains(listener))
			textChangeListeners.add(listener);
	}
	
	public void lostFocus() {
		Browser._theBrowser.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
		boolean dirty = false;
		if (emulatedSource.hasSelection()) {
			emulatedSource.clearSelection();
			dirty = true;
		}
		
		if (emulatedSource.getHighlightMode() != HighlightMode.None) {
			emulatedSource.setHighlightMode(HighlightMode.None);
			dirty = true;
		}

		if (popup != null &&
				PopupManager.getInstance().isShowing(popup)) {
			PopupManager.getInstance().hidePopup(popup);
		}
		
		if (dirty) repaint();
		
	}
	
	/**
	 * Sets the text and fires a text changed event if the text has changed.
	 * 
	 * @param text
	 * 		Null is allowed
	 */
	public void setText(String text) {
		if (text == null) text = "";
		
		if (emulatedSource.getText().equals(text)) return;
		
		invalidate();
		
		this.emulatedSource.setText(text);
		
		invalidate();
		
		fireTextChanged();
	}
	
	public String getText() {
		return emulatedSource.getText();
	}
	
	private void fireTextChanged() {
		
		for (TextChangeListener listener : textChangeListeners)
			listener.onTextChanged(this, this.emulatedSource.getText());
		
	}

	public void setBackgroundColor(Color c) {
		emulatedSource.setBackgroundColor(c);
	}
	
	private void repaint() {

		if (popup != null && PopupManager.getInstance().isShowing(popup)) {
			popup.invalidateAppearance();
			FrameGraphics.refresh(true);
		} else {
			invalidate();
			FrameGraphics.refresh(true);
		}
	}
	
	private void invalidate() {
		for (Rectangle r : emulatedSource.getDrawingArea()) 
			FrameGraphics.invalidateArea(r);
	}

	public void paint(Graphics g) {
		
		if ((popup != null && PopupManager.getInstance().isShowing(popup)) ||
				!parentComponant.isShowing())
			return;
		
		Point pos;
		try {
		pos = this.convertToExpediteeSpace(parentComponant.getLocationOnScreen());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		paint(	g, 
				pos.x + masterOffset.x, 
				pos.y + masterOffset.y, 
				parentComponant.getWidth() - masterOffset.x);
	}
	
	private void paint(Graphics g, int parentX, int parentY, int maxWidth) {

		if (Browser._theBrowser == null) return;
		
		/*int xoffset = (emulatedSource.getLink() == null) ? 6 : 18;
		int yoffset = emulatedSource.getBoundsHeight() - 3;
		parentX += xoffset;
		parentY += yoffset;*/
		
		// Set the emulated position according to the parent position - the parent position
		// can be from the popup or the actual parent componant
		if (emulatedSource.getX() != parentX || emulatedSource.getY() != parentY)
			emulatedSource.setPosition(parentX, parentY);

		Shape saveClip = g.getClip();
		
		if (maxWidth > 0) {
			
			Area clip = FrameGraphics.getCurrentClip();

			Area tmpClip = (clip != null) ? clip : 
				new Area(new Rectangle(0, 0, 
						Browser._theBrowser.getContentPane().getWidth(), 
						Browser._theBrowser.getContentPane().getHeight()));
			
		/*	tmpClip.intersect(new Area(new Rectangle(0, 0,
					parentX + maxWidth - xoffset, Browser._theBrowser.getContentPane().getHeight()
			)));*/
			
			tmpClip.intersect(new Area(new Rectangle(0, 0,
					parentX + maxWidth, Browser._theBrowser.getContentPane().getHeight()
			)));
	
			// No intersection
			if (!tmpClip.isEmpty()) 
				g.setClip(tmpClip);
		
		}
		
		emulatedSource.paint((Graphics2D)g);
		
		// Restore
		if (maxWidth > 0) {
			g.setClip(saveClip);
		}
		
		// Restore emulated position for consuming mouse movements
		try {
		Point pos = this.convertToExpediteeSpace(parentComponant.getLocationOnScreen());
		emulatedSource.setPosition(
				pos.x + masterOffset.x, 
				pos.y + masterOffset.y);
		} catch (IllegalComponentStateException ex) { // Cannot fix this .. this whole class needs revising
			ex.printStackTrace();
		}
	}
	
	public interface TextChangeListener
	{
		public void onTextChanged(Object source, String newLabel);
	}
	
	private class LabelEditPopup extends Popup implements KeyListener, MouseListener, MouseMotionListener {
		
		private static final long serialVersionUID = 1L;
		
		LabelEditPopup() {
			super.setAudoHide(true);
			super.setConsumeBackClick(false);
			super.setBorderThickness(0);
			
			updateSize();
			
			addKeyListener(this);
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		
		public void updateSize() {
			setSize(emulatedSource.getBoundsWidth() + 4, emulatedSource.getBoundsHeight() + 4);
		}
	

		public void keyPressed(KeyEvent e) {
			onKeyPressed(e, LabelEditPopup.this);
			updateSize();
		}

		public void keyReleased(KeyEvent e) {
		}
		

		public void keyTyped(KeyEvent e) {
		}
		
		
		public void mouseClicked(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			onMousePressed(e);
		}

		public void mouseReleased(MouseEvent e) {
			onMouseReleased(e);
			updateSize();
		}
		

		public void mouseDragged(MouseEvent e) {
			onMouseDragged(e);
		}

		public void mouseMoved(MouseEvent e) {

		}

		@Override
		public void onHide() {
			super.onHide();
		}
	
		@Override
		public void onShow() {
			super.onShow();
		}
		
		

		@Override
		public void paint(Graphics g) {
			
			EmulatedTextItem.this.paint(g, 7, 16, -1);

		}
	
	}
	
	private class RestrictedTextItem extends Text {
		
		private Color selectionColor = null;
		private int selectionMouseButton = -1;
		
		RestrictedTextItem() {
			super(1);
		}
		
		private void updateSelection(int mouseButton) {
			selectionColor = super.getSelectionColor(mouseButton);
			selectionMouseButton = mouseButton;
		}

		public void setSelectionStart(float mouseX, float mouseY, int mouseButton) {
			super.setSelectionStart(mouseX, mouseY);
			updateSelection(mouseButton);
		}
		
		

		@Override
		protected Color getSelectionColor(int mouseButton) {
			
			if (selectionColor != null)
				return selectionColor;
			
			else return super.getSelectionColor(mouseButton);

		}
		
		
	}
}
