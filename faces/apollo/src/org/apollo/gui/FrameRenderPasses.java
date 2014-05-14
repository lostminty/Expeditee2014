package org.apollo.gui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;

import org.apollo.AudioFrameKeyboardActions;
import org.apollo.AudioFrameMouseActions;
import org.apollo.audio.util.Timeline;
import org.apollo.items.FramePlaybackLauncher;
import org.apollo.items.RecordOverdubLauncher;
import org.apollo.util.Mutable;
import org.apollo.widgets.LinkedTrack;
import org.apollo.widgets.SampledTrack;
import org.expeditee.gui.Browser;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FreeItems;
import org.expeditee.gui.FrameGraphics.FrameRenderPass;
import org.expeditee.items.Item;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;

/**
 * All final effects rendered here - to give extra feedback to the user and simply to make
 * it more apealing.
 * @author Brook Novak
 *
 */
public class FrameRenderPasses implements FrameRenderPass {
	
	private static final Color SHADED_EXPANDED_BACKING_COLOR = new Color(80, 80, 80, 140);
	private static final Color Y_HELPER_LINES_COLOR = Color.DARK_GRAY;
	private static final Stroke Y_HELPER_LINES_STROKE = OverdubbedFrameTimeAxis.TRACK_TIMELINE_STROKE;
	
	private static FrameRenderPasses instance = new FrameRenderPasses();
	
	private LinkedList<Integer> timeHelperLinesToPaint = null;
	private Mutable.Integer yAxisTimelineHelperToPaint = null;
	
	private boolean shouldDrawFullscreenHelpers = false;
	
	private FrameRenderPasses() {
		FrameGraphics.addFrameRenderPass(this);
	}
	
	public static FrameRenderPasses getInstance() {
		return instance;
	}

	public void paintFinalPass(Graphics g) {
		if (Browser._theBrowser == null) return;
		int frameHeight = Browser._theBrowser.getContentPane().getHeight();
		if (ExpandedTrackManager.getInstance().isAnyExpandedTrackVisible()
				&& !FreeItems.getInstance().isEmpty()) {

			// Conceivable could have multiple
			LinkedList<SampledTrack> toPaint = null;
			
			for (Item i : FreeItems.getInstance()) {
				if (i instanceof WidgetCorner) {
					InteractiveWidget iw = ((WidgetCorner)i).getWidgetSource();
					
					if (iw != null && iw instanceof SampledTrack) {
						
						if (toPaint == null) {
							toPaint = new LinkedList<SampledTrack>();
						} else if (toPaint.contains(iw)) {
							continue;
						}
						
						toPaint.add((SampledTrack)iw);
					}
				}
			}
			
			if (toPaint != null) {
				for (SampledTrack trackWidget : toPaint) {
					if (g.getClip() == null
							|| g.getClip().intersects(trackWidget.getComponant().getBounds()))
					trackWidget.paintInFreeSpace(g, true);
					
				}
			}
			
		} 
		
		OverdubbedFrameTimeAxis.getInstance().paint(g);
		
		TimeAxis timeAxis = OverdubbedFrameTimeAxis.getInstance().getTimeAxis();	
		Timeline lastTimeline = OverdubbedFrameTimeAxis.getInstance().getLastTimeline();
		
		FramePlaybackBarRenderer.getInstance().paint((Graphics2D)g);

		if (timeHelperLinesToPaint != null && timeAxis != null && lastTimeline != null) {

			Rectangle r = OverdubbedFrameTimeAxis.getInstance().getCurrentTimelineArea();
			
			// Draw helper bar and text
			for (Integer x : timeHelperLinesToPaint) {
				
				((Graphics2D)g).setStroke(OverdubbedFrameTimeAxis.TRACK_TIMELINE_STROKE);
				g.setColor(Color.RED);
				
				if (shouldDrawFullscreenHelpers) 
					g.drawLine(x, 0, x, frameHeight);
				else 
					g.drawLine(x, r.y, x, r.y + r.height);
									
				long ms = lastTimeline.getMSTimeAtX(x) - lastTimeline.getFirstInitiationTime();

				String label = timeAxis.createSpecificTimeLabel(Math.abs(ms), 500.0);
				if (ms < 0) label = "-" + label;
				
				FontMetrics fm   = g.getFontMetrics(SampledTrackGraphViewPort.TIME_HELPER_FONT);
				Rectangle2D rect = fm.getStringBounds(label, g);
				int labelWidth  = (int)(rect.getWidth());
				int labelHeight  = (int)(rect.getHeight());
				
				int y = r.y + (r.height / 2) - ((labelHeight + 4) / 2);
				

				g.setColor(SampledTrackGraphViewPort.TIME_HELPER_COLOR);
				g.fillRect(x + 2,  y, labelWidth + 3, labelHeight + 4);
	
				g.setColor(SampledTrackGraphViewPort.TIME_HELPER_BORDERCOLOR);
				((Graphics2D)g).setStroke(SampledTrackGraphViewPort.ZOOM_BACKING_BORDER_STROKE);
				g.fillRect(x + 2,  y, labelWidth + 3, labelHeight + 4);

				g.setFont(SampledTrackGraphViewPort.TIME_HELPER_FONT);
				g.setColor(Color.BLACK);
				g.drawString(label, x + 4, y + labelHeight);
				
				
			}
		
		} 
		
//		 Draw Y helper
		if (yAxisTimelineHelperToPaint != null) {

			((Graphics2D)g).setStroke(Y_HELPER_LINES_STROKE);
			g.setColor(Y_HELPER_LINES_COLOR);

			g.drawLine(
					0, 
					yAxisTimelineHelperToPaint.value, 
					Browser._theBrowser.getContentPane().getWidth(), 
					yAxisTimelineHelperToPaint.value);
			
		}
		
	
	}

	public void paintPreLayeredPanePass(Graphics g) {
		if (Browser._theBrowser == null) return;
		
		if (ExpandedTrackManager.getInstance().isAnyExpandedTrackVisible()) {
			
			g.setColor(SHADED_EXPANDED_BACKING_COLOR);
			g.fillRect(0, 0, 
					Browser._theBrowser.getContentPane().getWidth(), 
					Browser._theBrowser.getContentPane().getHeight());
		}
		
	}

	public Area paintStarted(Area currentClip) {
		if (Browser._theBrowser == null) return currentClip;

		LinkedList<InteractiveWidget> seen = null;
		
		//boolean idDirty = false;
		//if (helperLinesToPaint != null) {
		//	idDirty = true;
			
		//}
		
		invalidateHelperLines(currentClip);
		
		timeHelperLinesToPaint = null;
		yAxisTimelineHelperToPaint = null;
		shouldDrawFullscreenHelpers = AudioFrameKeyboardActions.isControlDown() || 
			(AudioFrameKeyboardActions.isShiftDown() && !FreeItems.getInstance().isEmpty());

		// Get free tracks and paint line helpers
		if (!FreeItems.getInstance().isEmpty() || FrameMouseActions.getlastHighlightedItem() != null) {

			LinkedList<Item> possibles = new LinkedList<Item>(FreeItems.getInstance());
			if (FreeItems.getInstance().isEmpty() &&
					FrameMouseActions.getlastHighlightedItem() != null)
				possibles.add(FrameMouseActions.getlastHighlightedItem());
			
			for (Item i : possibles) {
				
				InteractiveWidget iw = null;
				
				if (i instanceof WidgetCorner) {
					iw = ((WidgetCorner)i).getWidgetSource();
				} else if (i instanceof WidgetEdge) {
					iw = ((WidgetEdge)i).getWidgetSource();
				}
				
				if (iw != null) {
					
					if (iw != null && (iw instanceof SampledTrack ||
							iw instanceof LinkedTrack)) {
						
						if (seen == null) {
							seen = new LinkedList<InteractiveWidget>();
						} else if (seen.contains(iw)) {
							continue;
						}
						
						if (timeHelperLinesToPaint == null)
							timeHelperLinesToPaint = new LinkedList<Integer>();
		
						timeHelperLinesToPaint.add(iw.getX());
					}
				} else if (i instanceof FramePlaybackLauncher || i instanceof RecordOverdubLauncher) {
					if (timeHelperLinesToPaint == null)
						timeHelperLinesToPaint = new LinkedList<Integer>();
					timeHelperLinesToPaint.add(i.getX());
				}
			}

		} 
		
		if ((AudioFrameMouseActions.isYAxisRestictionOn() || AudioFrameMouseActions.isSnapOn()) 
			&& !FreeItems.getInstance().isEmpty()) {
			
			int smallestY = FreeItems.getInstance().get(0).getY();
			for (Item i : FreeItems.getInstance()) {
				if (i.getY() < smallestY) smallestY = i.getY();
			}

			if (smallestY > 0) {
				yAxisTimelineHelperToPaint = Mutable.createMutableInteger(smallestY);
			}
		}
	
		// Invalidate on the fly
		invalidateHelperLines(currentClip);
		
		return currentClip;
		
	}
	
	private void invalidateHelperLines(Area currentClip) {
		if (currentClip != null) {
			
			if (timeHelperLinesToPaint != null && !timeHelperLinesToPaint.isEmpty()) {
				currentClip.add(new Area(OverdubbedFrameTimeAxis.getInstance().getCurrentTimelineArea()));
				if (shouldDrawFullscreenHelpers) {
					int fsHeight = Browser._theBrowser.getContentPane().getHeight();
					for (Integer n : timeHelperLinesToPaint) {
						currentClip.add(new Area(new Rectangle(n - 1, 0, 3, fsHeight)));
					}
				}
			}
			
			if (yAxisTimelineHelperToPaint != null && shouldDrawFullscreenHelpers) {

				currentClip.add(new Area(new Rectangle(
						0, 
						yAxisTimelineHelperToPaint.value - 1, 
						Browser._theBrowser.getContentPane().getWidth(), 
						3)));
			
			}
		}
	}
	
	
	

}
