package org.apollo.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.swing.SwingUtilities;

import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.SampledTrackModel;
import org.apollo.io.IconRepository;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.TrackNameCreator;
import org.apollo.widgets.SampledTrack;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FreeItems;
import org.expeditee.items.Item;
import org.expeditee.items.widgets.WidgetCorner;

/**
 * 
 * EditableSampledTrackGraphView's provide selection, removing, copying and inserting audio.
 * 
 * @author Brook Novak
 *
 */
public class EditableSampledTrackGraphView 
	extends SampledTrackGraphView
	implements MouseListener, MouseMotionListener, KeyListener {
	
	private static final long serialVersionUID = 1L;

	private int selectionMode = SELECTION_MODE_SELECT_ONLY;
	private int selectionStartX = 0;
	
	private SelectionBackSection selectionBackSection;

	private boolean selectAllOnDoubleClick = false;

	public static final Color SELECTION_BACKING_COLOR_SELECT_ONLY = Color.BLACK;
	private static final Color SELECTION_BACKING_COLOR_SELECT_AND_COPY = Color.GREEN;

	private static final int SELECTION_MODE_SELECT_ONLY = 1;
	private static final int SELECTION_MODE_SELECT_AND_COPY = 2;

	private static final int COPY_SELECTION_PIXEL_RANGE_THRESHOLD = 8;
	private static final int COARSE_PAN_PIXEL_LENGTH = 10;
	public static final int MIN_FRAME_SELECTION_SIZE = 100;
	
	public static final int LOCK_ICON_CORNER_OFFSET = 20;
	
	public EditableSampledTrackGraphView() {

		// Create selection back section
		selectionBackSection = new SelectionBackSection();
		addBackingSection(selectionBackSection);
		
		reAddListeners();

		setSelectionMode(SELECTION_MODE_SELECT_ONLY);

	}
	
	public void reAddListeners() {
		removeMouseListener(this);
		removeMouseMotionListener(this);
		removeKeyListener(this);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
	}
	
	/**
	 * Use this for setting selectionMode. Note is does not invlaidate.
	 * 
	 * @param mode
	 * 		The new mode to set.
	 */
	private void setSelectionMode(int mode) {
		selectionMode = mode;
		if (selectionMode == SELECTION_MODE_SELECT_ONLY) {
			
		} else if (selectionMode == SELECTION_MODE_SELECT_AND_COPY) {
			
		} else {
			assert(false);
		}
	} 


	public void keyPressed(KeyEvent e) {
	}
	
	public void selectAllInView() {
		if (getSampledTrackModel() == null) return;
		
		getSampledTrackModel().setSelection(
				getTimeScaleStart(), 
				getTimeScaleLength());
	}

	public void keyReleased(KeyEvent e) {
		if (getSampledTrackModel() == null) return;
		if (e.isConsumed()) return;
		
		switch (e.getKeyCode()) {
		case KeyEvent.VK_DELETE:
			
			if (getSampledTrackModel().getSelectionLength() > 1 
					&& !isPlaying()
					&& (getSampledTrackModel().getFrameCount() - getSampledTrackModel().getSelectionLength())  > MIN_FRAME_SELECTION_SIZE) {

				getSampledTrackModel().removeSelectedBytes();
				
			}
			
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_RIGHT:

			// Select to end of track?
			if (e.isShiftDown()) {
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					
					int oldStart = getSampledTrackModel().getSelectionStart();
					int oldLength = getSampledTrackModel().getSelectionLength();
					if (oldLength < 1) oldLength = 1;
					
					getSampledTrackModel().setSelection(0, oldStart + oldLength);
					
				} else { // Right
		
					getSampledTrackModel().setSelection(
							getSampledTrackModel().getSelectionStart(), 
							getSampledTrackModel().getFrameCount() - getSampledTrackModel().getSelectionStart());
					
				}
			} else {
				float ftmp = (float)COARSE_PAN_PIXEL_LENGTH / (float)getWidth();
				int pan = (int)(getTimeScaleLength() *  ftmp);
				if (pan == 0) pan = 1;
				
				if (e.getKeyCode() == KeyEvent.VK_RIGHT &&
						(getSampledTrackModel().getSelectionStart() +
						getSampledTrackModel().getSelectionLength()) < getSampledTrackModel().getFrameCount()) { // RIGHT
					
					getSampledTrackModel().setSelection(getSampledTrackModel().getSelectionStart() + pan, 
							getSampledTrackModel().getSelectionLength());
					
				} else if(e.getKeyCode() == KeyEvent.VK_LEFT &&
						getSampledTrackModel().getSelectionStart() > 0) { // LEFT
					getSampledTrackModel().setSelection(getSampledTrackModel().getSelectionStart() - pan, 
							getSampledTrackModel().getSelectionLength());
				}
			}
			break;
		}
	
		
	}


	public void keyTyped(KeyEvent e) {
	}


	public void mouseClicked(MouseEvent e) {
		if (e.isConsumed()) return;
		
		if (selectAllOnDoubleClick && getSampledTrackModel() != null && e.getClickCount() >= 2) 
		{
			selectAllInView();
			
			if (e.getButton() == MouseEvent.BUTTON3) { // copy
				copySelection(e);
			}
			
		} else if (!isPlaying() &&
				((e.getButton() == MouseEvent.BUTTON2 // Insert
				|| e.getButton() == MouseEvent.BUTTON3) && !FreeItems.getInstance().isEmpty())) {
			
			SampledTrack floatingTrack = null;
			
			// Look for a floating track for injection of audio bytes
			for (Item i : FreeItems.getInstance()) {
				if (i instanceof WidgetCorner) {
					if (((WidgetCorner)i).getWidgetSource() instanceof SampledTrack) {
						floatingTrack = (SampledTrack)((WidgetCorner)i).getWidgetSource();
						break;
					}
				}
			}
			
			// Perform an injection
			if (floatingTrack != null) {
				try {
					floatingTrack.injectAudio(this, e.getX(), e.getButton() == MouseEvent.BUTTON2);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			
			FrameGraphics.refresh(true);
			
		}
	}


	public void mouseEntered(MouseEvent e) {
	}


	public void mouseExited(MouseEvent e) {
	}


	public void mousePressed(MouseEvent e) {
		if (getSampledTrackModel() == null) return;
		if (e.isConsumed()) return;
		
		selectionStartX = e.getX();
		
		// set selection mode
		if (e.getButton() == MouseEvent.BUTTON3) 
			setSelectionMode(SELECTION_MODE_SELECT_AND_COPY);
		else setSelectionMode(SELECTION_MODE_SELECT_ONLY);
		
		// Set selection start, and length as a single frame
		getSampledTrackModel().setSelection(frameAtX(e.getX()), 1);
	}


	public void mouseReleased(MouseEvent e) {
		if (getSampledTrackModel() == null) return;
		if (e.isConsumed()) return;

		// If the mouse was released and the current selection mode is for copying frames...
		if (selectionMode == SELECTION_MODE_SELECT_AND_COPY) {
			
			// Get selection range in pixels to check if selection range is outside threshold...
			int selectionStartXPixel = XatFrame(getSampledTrackModel().getSelectionStart());
			int selectionEndXPixel = (getSampledTrackModel().getSelectionLength() > 1) ?
					XatFrame(getSampledTrackModel().getSelectionStart() + getSampledTrackModel().getSelectionLength()) 
					: -1;
			
			// Only copy if valid range selected
			int selLength = (selectionEndXPixel > selectionStartXPixel) ? 
					selectionEndXPixel - selectionStartXPixel : -1;
			
			// Only copy if outside threshold - in pixels of course
			if (selLength > COPY_SELECTION_PIXEL_RANGE_THRESHOLD) {
				
				copySelection(e);
				
			}
			
			// Reset selection mode since copy HAVE FINISHED
			setSelectionMode(SELECTION_MODE_SELECT_ONLY);
			
			// Update the new selection color
			invalidateSelection();
		}
	}

	/**
	 * Copies the selection into free space as a {@link SampledTrack}
	 * 
	 * @param e
	 */
	private void copySelection(MouseEvent e) {
		
		if (FreeItems.getInstance().isEmpty()) {
			// Get copy of selected bytes
			byte[] copiedAudioRegion = getSampledTrackModel().getSelectedFramesCopy();
			assert(copiedAudioRegion != null);
			
			Frame targetFrame = DisplayIO.getCurrentFrame();
			if (targetFrame != null) {
				
				SampledTrack twidget = SampledTrack.createFromMemory(
						copiedAudioRegion, 
						getSampledTrackModel().getFormat(),
						targetFrame, 
						0, 
						0,
						TrackNameCreator.getDefaultName(),
						getMix());
				
					/*	getSampledTrackModel().getName() != null ?
								TrackNameCreator.getNameCopy(getSampledTrackModel().getName() + "_section")
								: null);*/
				
				assert(Browser._theBrowser != null);
				
				// A workround for getting the widget into fre space centered on cursor
				Point p = e.getLocationOnScreen();
				SwingUtilities.convertPointFromScreen(p, Browser._theBrowser.getContentPane());
				twidget.setPosition(p.x - (twidget.getWidth() / 2), p.y - (twidget.getHeight() / 2));

				for (Item i : twidget.getItems()) {
					i.setOffset(new Point(
							(i.getX() - DisplayIO.getMouseX()) + (twidget.getWidth() / 2), 
							(i.getY() - FrameMouseActions.getY()) + (twidget.getHeight() / 2)
						));
				}
				
				// Put the new widget into free space
				FrameMouseActions.pickup(twidget.getItems());

			}
		
		}
	}


	public void mouseDragged(MouseEvent e) {
		if (getSampledTrackModel() == null) return;
		if (e.isConsumed()) return;
		if (selectionStartX < 0) return;
		
		// Set selection range
		int frameAtCursor = frameAtX(e.getX());
		int frameAtStartPoint = frameAtX(selectionStartX);
		
		int start = Math.min(frameAtCursor, frameAtStartPoint);
		int length = Math.max(frameAtCursor, frameAtStartPoint) - start;
		if (length > 0) {
			getSampledTrackModel().setSelection(start, length);
		}
	}


	public void mouseMoved(MouseEvent e) {
		
		// Ensure selection is not out of synch - could have missed some mouse events.
		if (e.getButton() == MouseEvent.NOBUTTON
				&& selectionMode != SELECTION_MODE_SELECT_ONLY) {
			setSelectionMode(SELECTION_MODE_SELECT_ONLY);
			invalidateSelection();
		}
			
	}

	
	@Override
	public void modelChanged(Subject source, SubjectChangedEvent event) {
		super.modelChanged(source, event);
		
		if (getSampledTrackModel() == null) return;
		
		switch(event.getID()) {

		// Whenever the selection changes, update the selection backing & invalidate
			case ApolloSubjectChangedEvent.SELECTION_CHANGED:
				
				selectionBackSection.left = XatFrame(getSampledTrackModel().getSelectionStart());
				
				selectionBackSection.width = (getSampledTrackModel().getSelectionLength() <= 1) ?
					-1 :
					XatFrame(getSampledTrackModel().getSelectionStart() + getSampledTrackModel().getSelectionLength())
					- selectionBackSection.left;

				if (selectionBackSection.visible) {

					invalidateSelection();
				}
				
				break;
				
		}

	
	}

	/**
	 * If the selection region drawn differs to the current selection region,
	 * then the dirty regions will be invalidated
	 *
	 */
	public void invalidateSelection() { 

		invalidateAll(); // TODO: FINE GRAINED VERSION - MORE EFFICIENT
	}

	
	@Override
	public void paint(Graphics g) {
		super.paint(g);

		// Draw selection start bar
		if (getSampledTrackModel().getSelectionStart() >= getTimeScaleStart()) {

			((Graphics2D)g).setStroke(GRAPH_BAR_STROKE);
			
			// Note that if the start line is near the edges of the panel it can be concealed - thus
			// set a thick line on the edges
			int x = selectionBackSection.left;
			if (x == 0) {
				x = 1;
			} else  if (x == getWidth()){
				x = getWidth() - 1;
			}
			
			g.setColor(Color.RED);
			g.drawLine(
					x, 
					0, 
					x,
					getHeight());
			
		}
		
		paintLock(g);

		
	}
	
	private void paintLock(Graphics g) {
		if (isPlaying()) {
			IconRepository.getIcon("lock.png").paintIcon(null,
					g, 
					getWidth() - LOCK_ICON_CORNER_OFFSET, 
					LOCK_ICON_CORNER_OFFSET - 16);
		}
	}
	
	private void invalidateLockIcon() {
		Point exp = getExpediteePoint();
		if (exp != null) {
			FrameGraphics.invalidateArea(new Rectangle(
					exp.x + getWidth() - LOCK_ICON_CORNER_OFFSET, 
					exp.y + LOCK_ICON_CORNER_OFFSET  - 16, 
					16, 16));
		}
		
	}
	
	@Override
	protected void onPlaybackStarted() {
		super.onPlaybackStarted();
		invalidateLockIcon();
	}

	@Override
	protected void onPlaybackStopped() {
		super.onPlaybackStopped();
		invalidateLockIcon();
	}

	@Override
	protected void fireTimelineChanged() {
		super.fireTimelineChanged();
		// Keep selection consistent when timeline changes
		selectionBackSection.updateBounds();
	}
	
	/**
	 * Injects bytes into the observed {@link SampledTrackModel} - if one is set and is not playing.
	 * Also resets selection - to select all on the new bytes
	 * 
	 * @see SampledTrackModel#insertBytes(byte[], javax.sound.sampled.AudioFormat, int)
	 */
	public void insertAudio(byte[] bytesToAdd, AudioFormat format, int framePosition) 
		throws IOException {
		if (getSampledTrackModel() != null && !isPlaying()) {
			
			// Perform insert
			getSampledTrackModel().insertBytes(bytesToAdd, format, framePosition);
			
			// Set new selection
			getSampledTrackModel().setSelection(framePosition, bytesToAdd.length / format.getFrameSize());
		}
		
		
	}
	
	public void setSelectAllOnDoubleClick(boolean selectAllOnDoubleClick) {
		this.selectAllOnDoubleClick = selectAllOnDoubleClick;
	}

	private class SelectionBackSection extends BackSection
	{

		public SelectionBackSection() {
			super();
			visible = true;
		}
		
		@Override
		void paint(Graphics g) {
			
			color =  (selectionMode == SELECTION_MODE_SELECT_ONLY) ?
					SELECTION_BACKING_COLOR_SELECT_ONLY : SELECTION_BACKING_COLOR_SELECT_AND_COPY;
			super.paint(g);
		}
		
		@Override
		void updateBounds() {
			
			assert(getSampledTrackModel() != null);
		
			left = XatFrame(getSampledTrackModel().getSelectionStart());
			
			width = (getSampledTrackModel().getSelectionLength() <= 1) ?
				-1 :
				XatFrame(getSampledTrackModel().getSelectionStart() + getSampledTrackModel().getSelectionLength())
				- left;
		}
	}

}
