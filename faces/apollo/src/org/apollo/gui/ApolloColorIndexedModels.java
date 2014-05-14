package org.apollo.gui;

import java.awt.Color;
import java.awt.image.IndexColorModel;

public class ApolloColorIndexedModels {
	
	public static final Color KEY_COLOR = new Color(123,58,123,0);
	private static final int KEY_INDEX = 0; 
	
	/** Color index model for the track graphs */
	public static final IndexColorModel graphIndexColorModel;

	
	public static final Color WAVEFORM_COLOR = new Color(0x1C5BA3);
	public static final Color WAVEFORM_SELECTION_COLOR = Color.YELLOW;
	
	private static final int WAVEFORM_INDEX = 1;
	private static final int WAVEFORM_SELECTION_INDEX = 2;
	
	
	public static final IndexColorModel linkedTrackIndexColorModel;
	
	public static final Color TRACK1_COLOR = new Color(0x1C5BA3);
	public static final Color TRACK2_COLOR = new Color(0x3280B7);
	
	public static final int TRACK1_INDEX = 1;
	public static final int TRACK2_INDEX = 2;

	static { // Prepare the global color index model for the graphs

		// For waveform graphs:
		byte[] reds = new byte[256];
		byte[] greens = new byte[256];
		byte[] blues = new byte[256];
		
		reds[KEY_INDEX] = (byte)KEY_COLOR.getRed();
		greens[KEY_INDEX] = (byte)KEY_COLOR.getGreen();
		blues[KEY_INDEX] = (byte)KEY_COLOR.getBlue();
		
		reds[WAVEFORM_INDEX] = (byte)WAVEFORM_COLOR.getRed();
		greens[WAVEFORM_INDEX] = (byte)WAVEFORM_COLOR.getGreen();
		blues[WAVEFORM_INDEX] = (byte)WAVEFORM_COLOR.getBlue();
		
		reds[WAVEFORM_SELECTION_INDEX] = (byte)WAVEFORM_SELECTION_COLOR.getRed();
		greens[WAVEFORM_SELECTION_INDEX] = (byte)WAVEFORM_SELECTION_COLOR.getGreen();
		blues[WAVEFORM_SELECTION_INDEX] = (byte)WAVEFORM_SELECTION_COLOR.getBlue();
		
		graphIndexColorModel = new IndexColorModel(8, 256, reds, greens, blues, KEY_INDEX);
		
		
		// For track heirarchy graphs:
		reds = new byte[256];
		greens = new byte[256];
		blues = new byte[256];
		
		reds[KEY_INDEX] = (byte)KEY_COLOR.getRed();
		greens[KEY_INDEX] = (byte)KEY_COLOR.getGreen();
		blues[KEY_INDEX] = (byte)KEY_COLOR.getBlue();
		
		reds[TRACK1_INDEX] = (byte)TRACK1_COLOR.getRed();
		greens[TRACK1_INDEX] = (byte)TRACK1_COLOR.getGreen();
		blues[TRACK1_INDEX] = (byte)TRACK1_COLOR.getBlue();
		
		reds[TRACK2_INDEX] = (byte)TRACK2_COLOR.getRed();
		greens[TRACK2_INDEX] = (byte)TRACK2_COLOR.getGreen();
		blues[TRACK2_INDEX] = (byte)TRACK2_COLOR.getBlue();
		
		linkedTrackIndexColorModel = new IndexColorModel(8, 256, reds, greens, blues, KEY_INDEX);
		
	}
	
	
	
	
}
