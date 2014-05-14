package org.apollo.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import org.apollo.ApolloSystem;
import org.expeditee.gui.Browser;

/**
 * A model time scale along the X axis that can be rendered.
 * 
 * @author Brook Novak
 *
 */
public class TimeAxis {
	
	private static final long serialVersionUID = 1L;
	
	public static final Font TIME_BAR_FONT = new Font("Arial", Font.PLAIN, 14);
	private static final Stroke TICK_STROKE = Strokes.SOLID_1;
	
	private static final Color MAJOR_TICK_COLOR = Color.BLACK;
	private static final Color MINOR_TICK_COLOR = new Color(0.4f, 0.4f, 0.4f);
	
	private static final int DESIRED_MAJOR_TICK_PIXEL_SPACING = 100;
	private static final int MIN_TICK_COUNT = 3; // per between majors

	private String[] majorTickLabels = null;
	private int[] majorTickXPositions = null;
	private int[] majorTickLabelXPositions = null;
	private int[] minorTickXPositions = null;
	private String alternateLabel = null;
	
	private int width = 0;
	private long totalTrackMSLength = 0;

	public TimeAxis() {
	}

	public void setAxis(
			long startTimeMS, 
			long timeLengthMS, 
			long timeLengthSamples,
			long totalTrackMSLength, 
			int width) {

		assert(width > 0);
		assert(startTimeMS >= 0);
		assert((startTimeMS + timeLengthMS) <= totalTrackMSLength);
		
		this.width = width;
		this.totalTrackMSLength = totalTrackMSLength;

		double majorTickPixelSpacing = DESIRED_MAJOR_TICK_PIXEL_SPACING;
		double majorTickCount = width / majorTickPixelSpacing;
		double timeAggregation = timeLengthMS / majorTickCount;
		
		if (timeAggregation <= 0) { // in between ms 
			majorTickLabels = null;
			majorTickXPositions = null;
			majorTickLabelXPositions = null;
			minorTickXPositions = null;
			alternateLabel = (timeLengthSamples >= 1) ? 
					timeLengthSamples + " samples"
					: " ";
			return;
		}

		// Re-adjust majorTickCount and aggregationSize to nice values
		// update majorTickPixelSpacing.
		
		// so users can follow the axis more easily
		boolean agregationAdjusted = true;
		
		if (timeAggregation > ((5 * 60000) + 30000)) {
			agregationAdjusted = false;
		} else if (timeAggregation > ((4 * 60000) + 30000)) { // 4:30 - 5:30
			timeAggregation = 5 * 60000;
		} else if (timeAggregation > ((3 * 60000) + 30000)) { // 3:30 - 4:30
			timeAggregation = 4 * 60000;
		} else if (timeAggregation > ((2 * 60000) + 30000)) { // 2:30 - 3:30
			timeAggregation = 3 * 60000;
		} else if (timeAggregation > (60000 + 30000)) { // 1:30 - 2:30
			timeAggregation = 2 * 60000;
		} else if (timeAggregation > 40000) { // 0:40 - 1:30
			timeAggregation = 60000;
		} else if (timeAggregation > 25000) { // 0:25 - 0:40
			timeAggregation = 30000;
		} else if (timeAggregation > 15000) { // 0:15 - 0:25
			timeAggregation = 20000;
		} else if (timeAggregation > 9000) { // 0:9 - 0:15
			timeAggregation = 10000;
		} else if (timeAggregation > 6500) { // 0:6.5 - 0:9
			timeAggregation = 8000;
		} else if (timeAggregation > 4500) { // 4.5s - 6.5s
			timeAggregation = 5000;
		} else if (timeAggregation > 3500) { // 3.5s - 4.5s
			timeAggregation = 4000;
		} else if (timeAggregation > 2500) { // 2.5s - 3.5s
			timeAggregation = 3000;
		} else if (timeAggregation > 1500) { // 1.5s - 2.5s
			timeAggregation = 2000;
		} else if (timeAggregation > 850) { // 850ms - 1.5s
			timeAggregation = 1000;
		} else if (timeAggregation > 600) { // 600ms - 850ms
			timeAggregation = 750;
		} else if (timeAggregation > 380) { // 380ms - 600ms
			timeAggregation = 500;
		} else if (timeAggregation > 120) { // 120ms - 380ms
			timeAggregation = 250;
		} else if (timeAggregation > 50) { // 50ms - 120ms
			timeAggregation = 100;
		} else {
			agregationAdjusted = false;
		}

		if (agregationAdjusted) {
			majorTickCount = timeLengthMS / timeAggregation;
			majorTickPixelSpacing = width / majorTickCount;
		}

		// The axis starts at zero always... calc the offset for the first tick
		
		double currentTickTime = startTimeMS - (startTimeMS % timeAggregation);
		if (currentTickTime < startTimeMS) {
			currentTickTime += timeAggregation;
		}
		
		
		double offsetPortion = ((double)(startTimeMS % timeAggregation) / (double)timeAggregation);
		if (offsetPortion > 0) offsetPortion = 1.0 - offsetPortion; 
		double majorTickXStart = offsetPortion * majorTickPixelSpacing;
		double currentTickX = majorTickXStart;
		
		// Create major x ticks
		LinkedList<Integer> majorTickPositions = new LinkedList<Integer>();
		LinkedList<String> majorTickLabels = new LinkedList<String>();
		LinkedList<Integer> majorTickLabelPositions = new LinkedList<Integer>();
	
		Graphics g = null;
		try {
		
			
			// Create a temp graphics for centered label positioning
			if (Browser._theBrowser != null && Browser._theBrowser.g != null) {
				g = Browser._theBrowser.g.create();
			} else {
				g = new BufferedImage(1,1,BufferedImage.TYPE_BYTE_INDEXED).getGraphics();
			}
			
			do {
	
				majorTickPositions.add(new Integer((int)currentTickX));

				String label = createSpecificTimeLabel((long)currentTickTime, timeAggregation);
				
				majorTickLabels.add(label);

				// Center label
				FontMetrics fm   = g.getFontMetrics(TIME_BAR_FONT);
				Rectangle2D rect = fm.getStringBounds(label, g);
				int textWidth  = (int)(rect.getWidth());
				
				majorTickLabelPositions.add(new Integer((int)currentTickX - (textWidth >> 1)));

				currentTickX += majorTickPixelSpacing;
				currentTickTime += timeAggregation;
	
			} while (currentTickX < width);

			
		} finally {
			if (g != null)
				g.dispose();
		}
		
		this.majorTickLabels = majorTickLabels.toArray(new String[0]);
		
		majorTickXPositions = new int[majorTickPositions.size()];
		for (int i = 0; i < majorTickPositions.size(); i++)
			majorTickXPositions[i] = majorTickPositions.get(i);
		
		majorTickLabelXPositions = new int[majorTickLabelPositions.size()];
		for (int i = 0; i < majorTickLabelPositions.size(); i++)
			majorTickLabelXPositions[i] = majorTickLabelPositions.get(i);
		
		
		// Build minor ticks
		double minorTickPixelSpacing = majorTickPixelSpacing / (MIN_TICK_COUNT + 1);
		
		if (minorTickPixelSpacing > 5) { // have some safety condition - avoid clutter
		
			// Find minor tick start X
			double tx = majorTickXStart;
			while (tx > 0) tx -= minorTickPixelSpacing;
			tx += minorTickPixelSpacing;
			if (tx == majorTickXStart) tx += minorTickPixelSpacing;
			
			LinkedList<Integer> minorPositions = new LinkedList<Integer>();
			
			// Draw the minor ticks
			while (tx < width) {
	
				minorPositions.add(new Integer((int)tx));
				
				tx += minorTickPixelSpacing;
	
				// Skip major positions
				if ((((int)(tx - majorTickXStart)) % (int)majorTickPixelSpacing) <= 4) // could be a pixel out 
					tx += minorTickPixelSpacing;
				
			}
			
			minorTickXPositions = new int[minorPositions.size()];
			for (int i = 0; i < minorTickXPositions.length; i++) 
				minorTickXPositions[i] = minorPositions.get(i).intValue();
		}
	}
	
	
	public String createSpecificTimeLabel(long atTime, double timeAggregation) {

		Long min = null;
		Long sec = null;
		Long ms = null;
		
		if (timeAggregation < 1000) { // omit MS if aggregation size >= 1sec
			
			ms = new Long((atTime) % 1000);
			
		}
		
		if (timeAggregation < 60000 &&
				totalTrackMSLength >= 1000) { // omit sec if aggregation size >= 1min or track length < 1 sec
			
			sec = new Long(((atTime) / 1000) % 60);
			
		}
		
		if (totalTrackMSLength >= 60000) { // omit min if track length < 1 min
			
			min = new Long((atTime) / 60000);
			
		}

		String label = createTimeLabel(min, sec, ms);
		if (label == null) {
			label = "00:00:00";
		}
		
		return label;
		
	}
	
	public static String createTimeLabel(Long min, Long sec, Long ms) {
		
		if (min == null && sec == null && ms != null) {
			return ms + "ms";
		}
		
		if (min == null && sec != null && ms == null) {
			return sec + "secs";
		}
		
		if (min == null && sec != null && ms != null) {
			String strms = ms.toString();
			String strsec = sec.toString();
			
			while (strms.length() < 3) strms = "0" + strms;
			
			return strsec + "." + strms + "secs";
		}
		
		if (min != null && sec == null && ms == null) {
			return min + "mins";
		}
		
		
		if (min != null && ms != null) {
			
			if (sec == null) sec = 0L;
			
			String strms = ms.toString();
			String strsec = sec.toString();
			String strmin = min.toString();
			
			while (strms.length() < 3) strms = "0" + strms;
			while (strsec.length() < 2) strsec = "0" + strsec;
			
			return strmin + ":" + strsec + "." + strms;
		}
		
		if (min != null && sec != null && ms == null) {

			String strsec = sec.toString();
			String strmin = min.toString();
			
			while (strsec.length() < 2) strsec = "0" + strsec;
			
			return strmin + ":" + strsec;
			
		}
		
		return null; // all is null

	}
	
	public void paint(Graphics g, int x, int y, int height, Color backgroundColor) {
		
		int majorTickHeight = height / 2;
		int minorTickHeight = height / 4;

		// Draw backing
		if (ApolloSystem.useQualityGraphics) {
			
			GradientPaint gp = new GradientPaint(
					x + (width / 2), y + (int)(height * 0.5), backgroundColor,
					x + (width / 2), y, SampledTrackGraphView.DEFAULT_BACKGROUND_HIGHTLIGHTS_COLOR);
			((Graphics2D)g).setPaint(gp);
			
		} else {
			g.setColor(backgroundColor);
		}
		
		g.fillRect(x, y, width, height);
		
		g.setFont(TIME_BAR_FONT);
		
		if (majorTickXPositions != null) {

			((Graphics2D)g).setStroke(TICK_STROKE);
			
			g.setColor(MAJOR_TICK_COLOR);
			// Draw Major ticks and labels
			for (int i = 0; i < majorTickLabels.length; i++) {
				
				// Label
				g.drawString(
						majorTickLabels[i], 
						x + majorTickLabelXPositions[i], 
						y + height - 2);
				
				// Tick
				g.drawLine(
						x + majorTickXPositions[i], 
						y, 
						x + majorTickXPositions[i], 
						y + majorTickHeight);
				
			}
			
			g.setColor(MINOR_TICK_COLOR);
			
			// Draw minor ticks
			if (minorTickXPositions != null) {
				for (int tx : minorTickXPositions) {
					g.drawLine(
							x + tx, 
							y, 
							x + tx, 
							y + minorTickHeight);
				}
			}
		
		} else if (alternateLabel != null) {
			
			g.setColor(Color.BLACK);
			
			FontMetrics fm   = g.getFontMetrics(TIME_BAR_FONT);
			Rectangle2D rect = fm.getStringBounds(alternateLabel, g);
			int textWidth  = (int)(rect.getWidth());
			
			g.drawString(
					alternateLabel, 
					x + ((width >> 1) - (textWidth >> 1)), 
					y + (height - 2));

		}
		
	}

	public int getWidth() {
		return width;
	}

}
