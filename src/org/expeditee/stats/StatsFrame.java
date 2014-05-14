package org.expeditee.stats;

import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class StatsFrame extends SessionStats {

	private static Map<String, Method> _getMethods = null;

	public static Method getMethod(String name) {
		if(_getMethods == null){
			init();
		}
		
		return _getMethods.get(name.trim().toLowerCase());
	}

	private static void init() {
		_getMethods = new HashMap<String, Method>();
		for (Method m : StatsFrame.class.getMethods()) {
			if (m.getReturnType().equals(String.class)) {
				_getMethods.put(m.getName().substring(3).toLowerCase(), m);
			}
		}
	}

	public static String getTextCreated() {
		return _ItemStats[ItemType.Text.ordinal()][StatType.Created.ordinal()]
				+ "";
	}
	
	public static String getTextMoved() {
		return _ItemStats[ItemType.Text.ordinal()][StatType.Moved.ordinal()]
				+ "";
	}
	
	public static String getTextDeleted() {
		return _ItemStats[ItemType.Text.ordinal()][StatType.Deleted.ordinal()]
				+ "";
	}
	
	public static String getTextCopied() {
		return _ItemStats[ItemType.Text.ordinal()][StatType.Copied.ordinal()]
				+ "";
	}
	
	public static String getItemsCreated() {
		return _ItemStats[ItemType.Total.ordinal()][StatType.Created.ordinal()]
				+ "";
	}
	
	public static String getItemsMoved() {
		return _ItemStats[ItemType.Total.ordinal()][StatType.Moved.ordinal()]
				+ "";
	}
	
	public static String getItemsDeleted() {
		return _ItemStats[ItemType.Total.ordinal()][StatType.Deleted.ordinal()]
				+ "";
	}
	
	public static String getItemsCopied() {
		return _ItemStats[ItemType.Total.ordinal()][StatType.Copied.ordinal()]
				+ "";
	}
	
	public static String getPicturesCreated() {
		return _ItemStats[ItemType.Picture.ordinal()][StatType.Created.ordinal()]
				+ "";
	}
	
	public static String getPicturesMoved() {
		return _ItemStats[ItemType.Picture.ordinal()][StatType.Moved.ordinal()]
				+ "";
	}
	
	public static String getPicturesDeleted() {
		return _ItemStats[ItemType.Picture.ordinal()][StatType.Deleted.ordinal()]
				+ "";
	}
	
	public static String getPicturesCopied() {
		return _ItemStats[ItemType.Picture.ordinal()][StatType.Copied.ordinal()]
				+ "";
	}
	
	public static String getDotsCreated() {
		return _ItemStats[ItemType.Dot.ordinal()][StatType.Created.ordinal()]
				+ "";
	}
	
	public static String getDotsMoved() {
		return _ItemStats[ItemType.Dot.ordinal()][StatType.Moved.ordinal()]
				+ "";
	}
	
	public static String getDotsDeleted() {
		return _ItemStats[ItemType.Dot.ordinal()][StatType.Deleted.ordinal()]
				+ "";
	}
	
	public static String getDotsCopied() {
		return _ItemStats[ItemType.Dot.ordinal()][StatType.Copied.ordinal()]
				+ "";
	}
	
	public static String getLinesCreated() {
		return _ItemStats[ItemType.Line.ordinal()][StatType.Created.ordinal()]
				+ "";
	}
	
	public static String getLinesMoved() {
		return _ItemStats[ItemType.Line.ordinal()][StatType.Moved.ordinal()]
				+ "";
	}
	
	public static String getLinesDeleted() {
		return _ItemStats[ItemType.Line.ordinal()][StatType.Deleted.ordinal()]
				+ "";
	}
	
	public static String getLinesCopied() {
		return _ItemStats[ItemType.Line.ordinal()][StatType.Copied.ordinal()]
				+ "";
	}
	
	public static String getFrames() {
		return _CreatedFrames + "";
	}
	
	public static String getFramesCreated() {
		return getFrames();
	}
	
	public static String getEscapeCount() {
		return _EscapeCount + "";
	}
	
	public static String getBackspaceCount() {
		return _BackspaceCount + "";
	}
	
	public static String getLeftButtonCount() {
		return _MouseCounters[MouseEvent.BUTTON1] + "";
	}
	
	public static String getMiddleButtonCount() {
		return _MouseCounters[MouseEvent.BUTTON2] + "";
	}
	
	public static String getRightButtonCount() {
		return _MouseCounters[MouseEvent.BUTTON3] + "";
	}
}
