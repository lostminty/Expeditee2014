package org.apollo.widgets;

import java.awt.Color;
import java.awt.Font;

public final class TrackWidgetCommons {
	private TrackWidgetCommons() {
	}
	
	/** For parsing metadata */
	public static final String META_NAME_TAG = "name=";
	public static final String META_INITIATIONTIME_TAG = "it=";
	public static final String META_RUNNINGMSTIME_TAG = "rt=";
	public static final String META_LAST_WIDTH_TAG = "lw=";
	public static final String META_OMIT_LAYOUT_TAG = "dontlayout";
	
	public static final int CACHE_DEPTH = 1;
	
	public static final Color FREESPACE_TRACKNAME_TEXT_COLOR = Color.WHITE;
	public static final Font FREESPACE_TRACKNAME_FONT = new Font("Arial", Font.BOLD, 20);
	
	public static final int POPUP_LIFETIME = 1500;
	
	public static final Color STANDARD_TRACK_EDGE_COLOR = Color.BLACK;
	public static final Color MUTED_TRACK_EDGE_COLOR = Color.GRAY;
	public static final Color SOLO_TRACK_EDGE_COLOR = Color.RED;
	public static final Color MUTED_SOLO_TRACK_EDGE_COLOR = new Color(255, 168, 168);
	
	public static final float STOPPED_TRACK_EDGE_THICKNESS = 1.0f;
	public static final float PLAYING_TRACK_EDGE_THICKNESS = 4.0f;
	

	public static Color getBorderColor(boolean isSolo, boolean isMuted) {
		
		Color newC = null;
		
		if (isSolo && isMuted) {
			newC = TrackWidgetCommons.MUTED_SOLO_TRACK_EDGE_COLOR;
		} else if (isSolo) {
			newC = TrackWidgetCommons.SOLO_TRACK_EDGE_COLOR;
		} else if (isMuted) {
			newC = TrackWidgetCommons.MUTED_TRACK_EDGE_COLOR;
		} else {
			newC = TrackWidgetCommons.STANDARD_TRACK_EDGE_COLOR;
		}
		
		return newC;
	
	}
}
