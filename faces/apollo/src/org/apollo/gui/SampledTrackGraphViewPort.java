package org.apollo.gui;

import java.awt.Color;
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.SampledTrackModel;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.AudioMath;
import org.expeditee.gui.Browser;

public class SampledTrackGraphViewPort extends SampledTrackGraphView
	implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

	private static final long serialVersionUID = 1L;
	
	private static final int ZOOM_SELECTION_PIXEL_RANGE_THRESHOLD = 1;
	private static final int COARSE_PAN_PIXEL_LENGTH = 14;
	private static final int SCROLL_ZOOM_PIXEL_LENGTH = 10; // for scroll-wheel
	
	private static final int ZOOM_ADJUSTMENT_MODE_NONE = 0;
	private static final int ZOOM_ADJUSTMENT_MODE_REZOOM = 1;
	private static final int ZOOM_ADJUSTMENT_MODE_PAN = 2;
	private static final int ZOOM_ADJUSTMENT_MODE_MODIFYZOOM = 3;
	
	public static final Color ZOOM_BACKING_COLOR_NORMAL = new Color(251, 255, 168);
	private static final Color ZOOM_BACKING_COLOR_HIGHLIGHT = new Color(255, 255, 224);
	private static final Color ZOOM_BACKING_BORDER_COLOR = new Color(215, 219, 144);
	public static final Stroke ZOOM_BACKING_BORDER_STROKE = Strokes.SOLID_1;
	
	public static final Font TIME_HELPER_FONT = new Font("Arial", Font.PLAIN, 12);
	public static final Color TIME_HELPER_COLOR = new Color(132, 175, 201);
	public static final Color TIME_HELPER_BORDERCOLOR = new Color(111, 146, 168);
	
	// displays the current zoom or the new zoom-selection
	// (actual zoom is not set until mouse released
	private ZoomBackSection zoomBackSection;

	private int zoomAdjustmentMode = ZOOM_ADJUSTMENT_MODE_NONE;
	private int adjustmentStartX = 0;
	private int adjustmentEndX = 0;
	private int panOffset = 0;

	// The model data
	private int zoomStartFrame = -1;
	private int zoomEndFrame = -1;
	
	private List<ZoomChangeListener> zoomChangeListeners = new LinkedList<ZoomChangeListener>();

	public SampledTrackGraphViewPort() {
		super();
		
		setBackColor(new Color(250, 250, 250), new Color(230, 230, 230));
		setAlwaysFullView(true);

		// Create back sections
		zoomBackSection = new ZoomBackSection();
		addBackingSection(zoomBackSection);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		this.addComponentListener(new ComponentListener() {

			public void componentHidden(ComponentEvent e) {
			}

			public void componentMoved(ComponentEvent e) {
			}

			public void componentResized(ComponentEvent e) {
				if (getSampledTrackModel() == null) return;
				
				// Initialization
				if (zoomStartFrame < 0 || zoomEndFrame < 0) {
					
					fullyZoomOut();
					
				} else {
					
					// Update backing
					zoomBackSection.updateBounds(false);
				}
			}

			public void componentShown(ComponentEvent e) {
			}
		
		});
	}

	public void addZoomChangeListener(ZoomChangeListener zcl) {
		if (!zoomChangeListeners.contains(zcl))
			zoomChangeListeners.add(zcl);
	}

	@Override
	public void setObservedSubject(Subject parent) {
		super.setObservedSubject(parent);
		
		if (parent != null && parent instanceof SampledTrackModel) {
			fullyZoomOut();
		}
	}
	
	public void fullyZoomOut() 
	{
		if (getSampledTrackModel() == null) return;
		setZoom(0, getSampledTrackModel().getFrameCount());
	}

	/**
	 * Sets the current zoom. In frames.
	 * Invalidates
	 * 
	 * @param startFrame
	 * 
	 * @param endFrame
	 * 
	 */
	public void setZoom(int startFrame, int endFrame) {
		if (getSampledTrackModel() == null) return;
	
		if (startFrame < 0) 
			startFrame = 0;
		if (startFrame > getSampledTrackModel().getFrameCount()) 
			startFrame = getSampledTrackModel().getFrameCount();
		
		if (endFrame < 0) 
			endFrame = 0;
		if (endFrame > getSampledTrackModel().getFrameCount()) 
			endFrame = getSampledTrackModel().getFrameCount();
		
		// Anything different?
		if (startFrame == zoomStartFrame && endFrame == zoomEndFrame)
			return;
		
		zoomStartFrame = startFrame;
		zoomEndFrame = endFrame;
		
		// Update backing
		zoomBackSection.updateBounds(false);
		
		invalidateAll();
		
		// Fire model changed event
		Event e = new Event(this, 0, null);
		for (ZoomChangeListener zcl : zoomChangeListeners)
			zcl.zoomChanged(e);
		
	}
	
	public int getZoomStartFrame() {
		return this.zoomStartFrame;
	}
	
	public int getZoomEndFrame() {
		return this.zoomEndFrame;
	}
	
	public void keyPressed(KeyEvent e) {
		if (getSampledTrackModel() == null) return;
		
		// Fine pan the zoom
		if (e.getKeyCode() == KeyEvent.VK_LEFT && Math.min(zoomStartFrame, zoomEndFrame) > 0) {

			float ftmp = (float)COARSE_PAN_PIXEL_LENGTH / (float)getWidth();
			int pan = (int)(Math.abs(zoomStartFrame - zoomEndFrame) *  ftmp);
			if (pan == 0) pan = 1;
			
			setZoom(zoomStartFrame - pan, zoomEndFrame - pan);
			
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT && 
				Math.max(zoomStartFrame, zoomEndFrame) < getSampledTrackModel().getFrameCount()) {
			
			float ftmp = (float)COARSE_PAN_PIXEL_LENGTH / (float)getWidth();
			int pan = (int)(Math.abs(zoomStartFrame - zoomEndFrame) *  ftmp);
			if (pan == 0) pan = 1;
			
			setZoom(zoomStartFrame + pan, zoomEndFrame + pan);
		}
		
		// Fine zoom the view
		else if (e.getKeyCode() == KeyEvent.VK_UP) {
			zoom(-1);
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			zoom(1);
		}
	
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() >= 2) { // zoom out fully on double click
			fullyZoomOut();
		}
	}
	
	/**
	 * Zooms in/out of viewport...
	 * 
	 * @param times
	 * 		How many zooms to do. Negative for zoom in. Positive for zoom out
	 */
	private void zoom(int times) {
		
		if (getSampledTrackModel() == null || times == 0) return;
		
		float ftmp = (float)SCROLL_ZOOM_PIXEL_LENGTH / (float)getWidth();
		int zoom = (int)(getTimeScaleLength() *  ftmp * times);
		
		if (Math.abs(zoom) > Math.abs(zoomStartFrame - zoomEndFrame)) {
			
			if (times < 0) {
				zoom = Math.abs(zoomStartFrame - zoomEndFrame) / 2;
				zoom *= -1;
			} else {
				zoom = Math.abs(zoomStartFrame - zoomEndFrame) * 2;
			}
		}
		
		if (zoom == 0) zoom = 1;

		int start = 0, end = 0;
		
		if (zoomStartFrame < zoomEndFrame) {
			
			start = zoomStartFrame - (zoom / 2);
			end = zoomEndFrame + (zoom / 2);
			
		} else {
			
			start = zoomStartFrame + (zoom / 2);
			end = zoomEndFrame - (zoom / 2);
			
		}

		setZoom(start, end);
		
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		zoom(e.getWheelRotation());
	}

	public void mouseEntered(MouseEvent e) {
	}


	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		if (getSampledTrackModel() == null) return;
		
		// set selection mode
		if (e.getButton() == MouseEvent.BUTTON1)  { // re-zoom from scratch
			setZoomAdjustmentMode(ZOOM_ADJUSTMENT_MODE_REZOOM);
			adjustmentStartX = adjustmentEndX = e.getX();
			
		} else if(e.getButton() == MouseEvent.BUTTON2) { 
			
			int adjustStart = XatFrame(zoomStartFrame);
			int adjustEnd = XatFrame(zoomEndFrame);
			
			// Only start panning if mouse over zoom area
			if (e.getX() >= Math.min(adjustStart, adjustEnd)
					&& e.getX() <= Math.max(adjustStart, adjustEnd)) {
				
				adjustmentStartX = adjustStart;
				adjustmentEndX = adjustEnd;
				
				setZoomAdjustmentMode(ZOOM_ADJUSTMENT_MODE_PAN);
				panOffset = e.getX() -  Math.min(adjustmentStartX, adjustmentEndX);
				assert(panOffset >= 0);
			}
			
		} else if(e.getButton() == MouseEvent.BUTTON3) { // add/subtract to current zoom
			
			setZoomAdjustmentMode(ZOOM_ADJUSTMENT_MODE_MODIFYZOOM);
			
			adjustmentStartX = XatFrame(zoomStartFrame);
			adjustmentEndX = XatFrame(zoomEndFrame);
			
			
			if (e.isControlDown()) {
				subtractCurrentZoomSelection(e.getX());
			} else {
				addCurrentZoomSelection(e.getX());
			}
			
		} else {
			setZoomAdjustmentMode(ZOOM_ADJUSTMENT_MODE_NONE);
		}
		
		if (zoomAdjustmentMode != ZOOM_ADJUSTMENT_MODE_NONE) {
			zoomBackSection.updateBounds(true);
			invalidateAll();
		}

	}

	public void mouseReleased(MouseEvent e) {
		if (getSampledTrackModel() == null) return;

		// Set the actual zoom only on mouse release events
		if (zoomAdjustmentMode != ZOOM_ADJUSTMENT_MODE_NONE) {
			
			setZoomAdjustmentMode(ZOOM_ADJUSTMENT_MODE_NONE);
			
			if (Math.abs(adjustmentStartX - adjustmentEndX) >= ZOOM_SELECTION_PIXEL_RANGE_THRESHOLD)
				setZoom(frameAtX(adjustmentStartX), frameAtX(adjustmentEndX));
			else invalidateAll();

		}
		
	}


	public void mouseDragged(MouseEvent e) {
		if (getSampledTrackModel() == null) return;
		
		switch (zoomAdjustmentMode) {
		case ZOOM_ADJUSTMENT_MODE_REZOOM:
			adjustmentEndX = e.getX();
			break;
			
		case ZOOM_ADJUSTMENT_MODE_PAN:

			assert(panOffset >= 0);
			
			int len = Math.abs(adjustmentEndX - adjustmentStartX);
			
			if (adjustmentStartX < adjustmentEndX) {
				adjustmentStartX = e.getX() - panOffset;
				adjustmentEndX = adjustmentStartX + len;
					
				// Clamp
				if (adjustmentStartX < 0) {
					adjustmentStartX = 0;
					adjustmentEndX = len;
				}
				
				else if (adjustmentEndX > getWidth()) {
					adjustmentEndX = getWidth();
					adjustmentStartX = adjustmentEndX - len;
				}
				
			} else {
				adjustmentEndX = e.getX() - panOffset;
				adjustmentStartX = adjustmentEndX + len;
					
				// Clamp
				if (adjustmentEndX < 0) {
					adjustmentEndX = 0;
					adjustmentStartX = len;
				}
				
				else if (adjustmentStartX > getWidth()) {
					adjustmentStartX = getWidth();
					adjustmentEndX = adjustmentStartX - len;
				}
			}
			
			
			break;
			
		case ZOOM_ADJUSTMENT_MODE_MODIFYZOOM:
			if (e.isControlDown()) {
				subtractCurrentZoomSelection(e.getX());
			} else {
				addCurrentZoomSelection(e.getX());
			}
			break;
		}
		
		if (zoomAdjustmentMode != ZOOM_ADJUSTMENT_MODE_NONE) {
			zoomBackSection.updateBounds(true);
			invalidateAll();
		}

	}
	

	public void mouseMoved(MouseEvent e) {
		
		// If missed a mouse release event must reset the adjustment state...
		if (e.getButton() == MouseEvent.NOBUTTON 
				&& zoomAdjustmentMode != ZOOM_ADJUSTMENT_MODE_NONE) {
			setZoomAdjustmentMode(ZOOM_ADJUSTMENT_MODE_NONE);
			invalidateAll();
		}
		
	}
	
	private void addCurrentZoomSelection(int x) {
		
		if (adjustmentStartX < adjustmentEndX) {
			
			if (x < adjustmentStartX) // expand to left
				adjustmentStartX = x;
			
			else if (x > adjustmentEndX) // expand to right
				adjustmentEndX = x;
			
		} else {
			
			if (x < adjustmentEndX) // expand to left
				adjustmentEndX = x;
			
			else if (x > adjustmentStartX) // expand to right
				adjustmentStartX = x;
			
		}
	}
	
	private void subtractCurrentZoomSelection(int x) {
		
		if (adjustmentStartX < adjustmentEndX) {
			
			if (x > adjustmentStartX) // shrink to left
				adjustmentStartX = x;
			
			else if (x < adjustmentEndX) // shrink to right
				adjustmentEndX = x;
			
		} else {
			
			if (x > adjustmentEndX) // shrink to right
				adjustmentEndX = x;
			
			else if (x < adjustmentStartX) // shrink to left
				adjustmentStartX = x;
		}
		
	}


	@Override
	public void modelChanged(Subject source, SubjectChangedEvent event) {
		super.modelChanged(source, event);
		
		if (getSampledTrackModel() == null) return;


		switch(event.getID()) {

			case ApolloSubjectChangedEvent.AUDIO_INSERTED:
			case ApolloSubjectChangedEvent.AUDIO_REMOVED:
				zoomBackSection.updateBounds(false);
				break;
				
		}
	}
	
	
	/**
	 * Use this for setting the ajudstment mode.
	 * Updates GUI. But does not Invalidate.
	 * 
	 * @param mode
	 */
	private void setZoomAdjustmentMode(int mode) {
		
		zoomAdjustmentMode = mode;
		
		if (mode == ZOOM_ADJUSTMENT_MODE_NONE) {
			zoomBackSection.updateBounds(false);
		} else if (mode == ZOOM_ADJUSTMENT_MODE_REZOOM) {
			
		} else if (mode == ZOOM_ADJUSTMENT_MODE_PAN) {
			
		} else if (mode == ZOOM_ADJUSTMENT_MODE_MODIFYZOOM) {
			
		} else {
			assert(false);
		}

	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		zoomBackSection.postPaint(g);
	}
	
	
	private class ZoomBackSection extends BackSection
	{
		
		private String startTimeHelpLabel = null;
		private String endTimeHelpLabel = null;
		private int labelStartX = 0;
		private int labelEndX = 0;
		private int labelStartY = 0;
		private int labelEndY = 0;
		private int labelWidth = 0;
		private int labelHeight = 0;
		
		public ZoomBackSection() {
			super();
			visible = true;
		}
		
		@Override
		void paint(Graphics g) {
			
			color =  (zoomAdjustmentMode == ZOOM_ADJUSTMENT_MODE_NONE) ?
					ZOOM_BACKING_COLOR_NORMAL : ZOOM_BACKING_COLOR_HIGHLIGHT;
			super.paint(g);
			
			g.setColor(ZOOM_BACKING_BORDER_COLOR);
			((Graphics2D)g).setStroke(ZOOM_BACKING_BORDER_STROKE);
			g.drawLine(left, 0, left, getHeight());
			g.drawLine(left + width, 0, left + width, getHeight());
			
		}
		
		void postPaint(Graphics g) {
			// Paint little helper labels when ajusting view port
			if (startTimeHelpLabel != null && endTimeHelpLabel != null &&
					zoomAdjustmentMode != ZOOM_ADJUSTMENT_MODE_NONE) {
				
				g.setColor(TIME_HELPER_COLOR);
				g.fillRect(labelStartX - 2, labelStartY - labelHeight + 2, labelWidth + 3, labelHeight);
				g.fillRect(labelEndX - 2, labelEndY  - labelHeight + 2, labelWidth + 3, labelHeight);
				
				g.setColor(TIME_HELPER_BORDERCOLOR);
				((Graphics2D)g).setStroke(ZOOM_BACKING_BORDER_STROKE);
				g.drawRect(labelStartX - 2, labelStartY - labelHeight + 2, labelWidth + 3, labelHeight);
				g.drawRect(labelEndX - 2, labelEndY  - labelHeight + 2, labelWidth + 3, labelHeight);

				g.setFont(TIME_HELPER_FONT);
				g.setColor(Color.BLACK);
				g.drawString(startTimeHelpLabel, labelStartX, labelStartY);
				g.drawString(endTimeHelpLabel, labelEndX, labelEndY);
				
			}
		}
		
		void updateBounds(boolean toAdjust) {
			if (toAdjust) {
				left = Math.min(adjustmentStartX, adjustmentEndX);
				
				width = Math.abs(adjustmentStartX - adjustmentEndX);
				
				updateHelperLabels();
				
			} else {
				left = XatFrame(Math.min(zoomStartFrame, zoomEndFrame));
				
				width = Math.abs(XatFrame(zoomStartFrame) - XatFrame(zoomEndFrame));
			}
			
			if (width < 3) width = 3;
		}
		
		private void updateHelperLabels() {
			if (getSampledTrackModel() == null) return;
			
			Graphics g = null;
			try {
			
				// create time stings
				long startms = AudioMath.framesToMilliseconds(
						frameAtX(Math.min(adjustmentStartX, adjustmentEndX)), 
						getSampledTrackModel().getFormat());
				
				long endms = AudioMath.framesToMilliseconds(
						frameAtX(Math.max(adjustmentStartX, adjustmentEndX)), 
						getSampledTrackModel().getFormat());

				startTimeHelpLabel = createHelperLabel(startms);
				endTimeHelpLabel = createHelperLabel(endms);
				
				assert(startTimeHelpLabel != null);
				assert(endTimeHelpLabel != null);
				
				// Create a temp graphics for centered label positioning
				if (Browser._theBrowser != null && Browser._theBrowser.g != null) {
					g = Browser._theBrowser.g.create();
				} else {
					g = new BufferedImage(1,1,BufferedImage.TYPE_BYTE_INDEXED).getGraphics();
				}
		
				// Position labels
				FontMetrics fm   = g.getFontMetrics(TIME_HELPER_FONT);
				Rectangle2D rect = fm.getStringBounds(startTimeHelpLabel, g);
				
				labelWidth = (int)rect.getWidth();
				labelHeight = (int)rect.getHeight();
				
				labelStartX = Math.min(adjustmentStartX, adjustmentEndX) - labelWidth - 5;
				labelEndX = Math.max(adjustmentStartX, adjustmentEndX) + 5;
				
				labelStartY = labelEndY = (getHeight() >> 1) + (labelHeight >> 1);

			} finally {
				if (g != null)
					g.dispose();
			}
			
		}
		
		/**
		 * @param time
		 * @return Never null
		 */
		private String createHelperLabel(long time) {
			assert(getSampledTrackModel() != null);
			
			long totalMS = AudioMath.framesToMilliseconds(getSampledTrackModel().getFrameCount(),
					getSampledTrackModel().getFormat());
			
			
			Long mins = (totalMS < 60000) ? null : time / 60000;
			
			Long secs = (totalMS < 1000) ? null : (time / 1000) % 60;

			Long ms = time % 1000;
			
			return TimeAxis.createTimeLabel(mins, secs, ms);
		}
	}
}
