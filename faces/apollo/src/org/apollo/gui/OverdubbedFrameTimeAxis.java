package org.apollo.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.SwingUtilities;

import org.apollo.audio.util.Timeline;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.MessageBay;

/**
 * The global timeline on the current overdubbed frame
 * 
 * @author Brook Novak
 * 
 */
public class OverdubbedFrameTimeAxis implements Observer { //, DisplayIOObserver {

	private TimeAxis timeAxis = null;
	private Rectangle lastInvalidated;
	private Timeline lastTimeline = null;

	private static OverdubbedFrameTimeAxis instance = new OverdubbedFrameTimeAxis();

	private static final int GLOBAL_TIMELINE_HEIGHT = 30;
	private static final Color GLOBAL_TIMELINE_BACKCOLOR = new Color(220, 220, 220);
	private static final Color GLOBAL_EMPTY_TIMELINE_BACKCOLOR = new Color(240, 240, 240);
	
	public static final Stroke TRACK_TIMELINE_STROKE = new BasicStroke(
			1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0.0f, new float[] { 5.0f, 5.0f }, 0.0f);
	
	private static final Stroke TIMELINE_BORDER_STROKE = new BasicStroke(1.0f);
			
	/**
	 * Singleton constructor,
	 */
	private OverdubbedFrameTimeAxis() {
		
		FrameLayoutDaemon.getInstance().addObserver(this);
		
		//DisplayIO.addDisplayIOObserver(this);
		
		// Listen for resize events
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				// Keep expanded view centered to the browser window
				if (Browser._theBrowser != null) {
					Browser._theBrowser.addComponentListener(new ComponentListener() {

						public void componentHidden(ComponentEvent e) {
						}

						public void componentMoved(ComponentEvent e) {
						}

						public void componentResized(ComponentEvent e) {
							updateAxis();
						}

						public void componentShown(ComponentEvent e) {
						}
						
					});
				}
				
			}
		});
	}


	/**
	 * 
	 * @return
	 * 		The singleton instance.
	 */
	public static OverdubbedFrameTimeAxis getInstance() {
		return instance;
	}
	
	public void setObservedSubject(Subject parent) {
	}

	public Subject getObservedSubject() {
		return FrameLayoutDaemon.getInstance();
	}

	public void modelChanged(Subject source, SubjectChangedEvent event) {
		updateAxis();
	}
	
	/*public void frameChanged() {
		updateAxis();
	}*/
	
	private void updateAxis() {
		
		if (Browser._theBrowser == null)
			return;

		lastTimeline = FrameLayoutDaemon.getInstance().getLastComputedTimeline();
		if (lastTimeline == null || 
			FrameLayoutDaemon.getInstance().getTimelineOwner() != DisplayIO.getCurrentFrame()) {
			
			// The frame layout daemon only lays out frames once - unless the user rearranges tracks
			// on a frame then the daemon we re-calc the timeline... thus a timeline must be infferred
			
			lastTimeline = FrameLayoutDaemon.inferTimeline(DisplayIO.getCurrentFrame());
			
			if (lastTimeline == null) { // no widgets
				if (timeAxis != null && lastInvalidated != null) {
					FrameGraphics.invalidateArea(lastInvalidated); // old position
				}
				
				timeAxis = null;
				return;
			}
			
		}
		
		timeAxis = new TimeAxis();

		timeAxis.setAxis(
				0, 
				lastTimeline.getRunningTime(), 
				-1,
				lastTimeline.getRunningTime(), 
				lastTimeline.getPixelWidth());

		
		Rectangle newBounds = getCurrentTimelineArea();
		if (lastInvalidated != null && !lastInvalidated.equals(newBounds)) {
			FrameGraphics.invalidateArea(lastInvalidated); // old position
		}
		
		FrameGraphics.invalidateArea(newBounds);
		
		lastInvalidated = newBounds;
		
		FrameGraphics.refresh(true);
		
	}

	public Rectangle getCurrentTimelineArea() {
		assert(Browser._theBrowser != null);
		int y = Browser._theBrowser.getContentPane().getHeight() - GLOBAL_TIMELINE_HEIGHT;
		if (FrameGraphics.getMode() != FrameGraphics.MODE_AUDIENCE)
			y -= MessageBay.MESSAGE_BUFFER_HEIGHT;
		
		if (y < 0) y = 0;

		return new Rectangle(0, y, Browser._theBrowser.getContentPane().getWidth(), GLOBAL_TIMELINE_HEIGHT);
	}
	

	public void paint(Graphics g) {
		if (Browser._theBrowser == null) return;

		if (timeAxis != null && lastTimeline != null) {
		
			Rectangle r = getCurrentTimelineArea();

			g.setColor(GLOBAL_EMPTY_TIMELINE_BACKCOLOR);
			g.fillRect(0, r.y, Browser._theBrowser.getContentPane().getWidth(), r.height);
			
			timeAxis.paint(g, 
					lastTimeline.getInitiationXPixel(),
					r.y,
					r.height, 
					GLOBAL_TIMELINE_BACKCOLOR);
			
			
			g.setColor(Color.DARK_GRAY);
			
			((Graphics2D)g).setStroke(TIMELINE_BORDER_STROKE);
			g.drawLine(0, r.y, Browser._theBrowser.getContentPane().getWidth(), r.y);
	
		}
		
		
	}


	public Timeline getLastTimeline() {
		return lastTimeline;
	}


	public TimeAxis getTimeAxis() {
		return timeAxis;
	}
	
	
	
}
