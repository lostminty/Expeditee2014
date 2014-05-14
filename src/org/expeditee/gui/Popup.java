package org.expeditee.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.expeditee.items.ItemUtils;

/**
 * 
 * A Custom swing popup in Expeditee.
 * 
 * Expeditee popus can be re-used. Popups are always heavyweight (i.e. uses invalidation).
 * 
 * @see {@link PopupManager}
 * 
 * @author Brook Novak
 * 
 */
public abstract class Popup extends JPanel {
	
	//Mike says: Can we get the border for the IW to which this popup corresponds?
	// Brook says: Would be nice - but popups are actually independant from widgets
	//   =>Now: It is up to the user of the popup to set the border thickness
	private static final BasicStroke DEFAULT_STROKE = new BasicStroke(2.0f);
	
	private BasicStroke _borderStroke = DEFAULT_STROKE;
	private Color _borderColor = Color.BLACK;
	
	private boolean _isReadyToPaint = false;
	private boolean _consumeBackClick = false;
	private boolean _autoHide = true;

	/**
	 * Creates a new popup.
	 * Autohide is set to true.
	 * 
	 */
	public Popup() {
		super();
		setVisible(false);
	}
	
	/**
	 * Creates a new popup.
	 * 
	 * @param layout the LayoutManager to use
	 */
	public Popup(LayoutManager layout) {
		super(layout);
		setVisible(false);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		// Draw border - if not transparent
		if (_borderStroke != null && _borderColor != null) {
			g.setColor(_borderColor);
			((Graphics2D)g).setStroke(_borderStroke);
			g.drawRect(0, 0, getWidth(), getHeight());
		}
	}
	
	private void ignoreAWTPainting(Component c) {
		
		if (c instanceof JComponent) {
			((JComponent)c).setDoubleBuffered(false);
		}
		
		c.setIgnoreRepaint(true);
		
		if (c instanceof Container) {
			for (Component child : ((Container) c).getComponents()) {

				if (child instanceof Container) {
					ignoreAWTPainting(child);
				} else {
					if (child instanceof JComponent) {
						((JComponent)child).setDoubleBuffered(false);
					}
					
					child.setIgnoreRepaint(true);
				}
			}
		}
		
	}
	
	/**
	 * Ensures that AWT painting turned off
	 */
	void prepareToPaint() {
		if (!_isReadyToPaint) {
			_isReadyToPaint = true;
			ignoreAWTPainting(this);
		}
	}
	
	/**
	 * Invoked when the popup becomes hidden, or when the popup is animating to show but cancelled.
	 */
	public void onHide() {} 
	
	/**
	 * Invoked when the popup shows. Note this might not eventuate for animated popups.
	 */
	public void onShow() {}
	
	/**
	 * Invoked when popups is going to show.
	 * This always is invoked first.
	 */
	public void onShowing() {}

	public boolean shouldConsumeBackClick() {
		return _consumeBackClick;
	}

	/**
	 * @param consumeBackClick
	 * 		Set to True for whenever the user clicks empty space
	 * 		to go back a frame that if this popup is visible should
	 * 		consume the back-click event.
	 */
	protected void setConsumeBackClick(boolean consumeBackClick) {
		_consumeBackClick = consumeBackClick;
	}
	
	/**
	 * @param autoHideOn
	 * 		Set to True if this popup should auto hide. (The default).
	 * 		Set to false if this popup should be manually hidden.
	 */
	protected void setAudoHide(boolean autoHideOn) {
		_autoHide = autoHideOn;
	}
	
	/**
	 * @return
	 * 		True if this popup auto hides.
	 */
	public boolean doesAutoHide() {
		return _autoHide;
	}
	
	/**
	 * Invalidates self.
	 * 
	 * @param thickness
	 * 		The new thickness to set. Null for no border.
	 */
	public void setBorderThickness(float thickness) {
		assert(thickness >= 0);
		
		if (_borderStroke != null && _borderStroke.getLineWidth() == thickness)
			return;
		
		boolean posInvalidate = true;
		
		if (thickness < _borderStroke.getLineWidth()) {
			invalidateAppearance();
			posInvalidate = false;
		}
		
		if (thickness == 0) _borderStroke = null;
		else _borderStroke = new BasicStroke(thickness);
		
		if (posInvalidate) invalidateAppearance();

	}
	
	/**
	 * @return
	 * 		The border thickness of this popup. Zero or more.
	 */
	public float getBorderThickness() {
		if (_borderStroke == null) return 0.0f;
		return _borderStroke.getLineWidth();

	}
	
	/**
	 * Sets the border color around the popup.
	 * Invalidates self.
	 * 
	 * @param c
	 * 		The new color. Null for transparent.
	 */
	public void setBorderColor(Color c) {
		
		if (c == null && _borderColor != null) 
			invalidateAppearance();
		
		if (c != _borderColor) {
			_borderColor = c;
			invalidateAppearance();
		}
	}
	
	/**
	 * 
	 * @return
	 * 		The border color for the popup. NUll if transparent
	 */
	public Color getBorderColor() {
		return _borderColor;
	}
	
	/**
	 * Invalidates the whole popup so that it must be fully repainted.
	 */
	public void invalidateAppearance() {
		
		if (_borderColor != null && _borderStroke != null && _borderStroke.getLineWidth() > 0) { // border
			FrameGraphics.invalidateArea(ItemUtils.expandRectangle(getBounds(), 
					(int)Math.ceil(getBorderThickness()) + 1));
		} else { // no border
			FrameGraphics.invalidateArea(getBounds());
		}

	}
	

	
}
