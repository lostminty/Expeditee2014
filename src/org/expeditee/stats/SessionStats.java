package org.expeditee.stats;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.TimeKeeper;
import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;

public class SessionStats extends Stats {

	public enum ItemType {
		Text, Dot, Line, Picture, Total;
	}

	public enum StatType {
		Created, Copied, Moved, Deleted;
	}

	private static final int STAT_TYPES = StatType.values().length;

	private static final int ITEM_TYPES = ItemType.values().length;

	protected static int[][] _ItemStats = new int[ITEM_TYPES][STAT_TYPES];

	private static List<String> _FrameEvents = new ArrayList<String>();

	// statistics counters
	private static final int MOUSE_BUTTONS = 4;

	private static StringBuffer _FramesEdited = new StringBuffer();

	protected static int[] _MouseCounters = new int[MOUSE_BUTTONS];

	private static int _AccessedFrames = 0;

	private static int _SavedFrames = 0;

	// the number of frames created via TDFC
	protected static int _CreatedFrames = 0;

	// character counts
	private static int[] _CharCounts = new int[255];

	private static long _LastEvent = new Date().getTime();

	private static final long DARK_TIME_THRESHOLD = 60000;

	protected static int _EscapeCount = 0;

	private static boolean _StatsEnabled = true;

	private static final int[] EVENT_INTERVALS = new int[] { 1, 2, 3, 4, 5, 10,
			20, 60, 300, 600, Integer.MAX_VALUE };

	private static int[] _EventTimes = new int[EVENT_INTERVALS.length];

	protected static int _BackspaceCount;

	public static String getCurrentStats() {

		StringBuffer stats = getLength();

		long elapsedTime = (new Date()).getTime() - _StartTime.getTime();
		//Add darkTime to the nearest minute
		stats.append("DarkTime: ").append(
				(_DarkTime.getTime() + MILLISECONDS_PER_MINUTE / 2) / MILLISECONDS_PER_MINUTE).append(
				"-->" + _DarkTime.getTime() * 100 / elapsedTime + "%\n");

		stats.append(getResponseStats()).append("\n");
		stats.append(getFrameStats());

		stats.append(getCharacterStats());

		stats.append("Version: ").append(DisplayIO.TITLE);

		return stats.toString();
	}

	private static String getTimeElapsed() {
		Date currentTime = new Date();
		long elapsedTime = currentTime.getTime() - _StartTime.getTime();
		String time = ""
				+ Math.round((float) elapsedTime / MILLISECONDS_PER_MINUTE);
		return time;
	}

	private static String getResponseStats() {
		String stats = "ResponseTime: "
				+ TimeKeeper.Formatter.format(FrameUtils.getLastResponseTime())
				+ "sec, ";

		if (_AccessedFrames > 0)
			stats += TimeKeeper.Formatter.format(FrameUtils
					.getResponseTimeTotal()
					/ _AccessedFrames)
					+ "avg";
		else
			stats += "--avg";

		return stats;
	}

	private static String getFrameStats() {
		return getFrameStats(true);
	}

	private static String getFrameStats(boolean newline) {
		StringBuffer stats = new StringBuffer();
		appendStat(stats, "FramesAccessed", _AccessedFrames, newline, false,
				DEFAULT_VALUE_WIDTH, DEFAULT_RATE_WIDTH);
		appendStat(stats, "FramesEdited", _SavedFrames, newline, false,
				DEFAULT_VALUE_WIDTH, DEFAULT_RATE_WIDTH);
		return stats.toString();
	}

	private static String getCharacterStats() {
		int[] counts = _CharCounts;

		int chars = 0;

		for (int i = 'A'; i <= 'Z'; i++)
			chars += counts[i];

		for (int i = 'a'; i <= 'z'; i++)
			chars += counts[i];

		for (int i = '0'; i <= '9'; i++)
			chars += counts[i];

		chars -= counts[KeyEvent.VK_BACK_SPACE];
		chars -= counts[KeyEvent.VK_DELETE];

		int EOS = counts['.'] + counts[','] + counts['!'] + counts['?'];

		int punct = counts[' '] + counts[';'] + counts[':'];
		chars += counts['('] + counts[')'] + counts['\''] + counts['"']
				+ counts['+'] + counts['='] + counts['-'];

		StringBuffer stats = new StringBuffer();
		appendStat(stats, "Chars", chars + punct + EOS);
		appendStat(stats, "Words", punct + EOS);
		appendStat(stats, "Sentences", EOS);
		appendStat(stats, "TextItems",
				_ItemStats[ItemType.Text.ordinal()][StatType.Created.ordinal()]);
		appendStat(stats, "Frames", _CreatedFrames);
		appendStat(stats, "Escape", _EscapeCount);
		appendStat(stats, "Backspace", _BackspaceCount);
		appendStat(stats, "Left", _MouseCounters[MouseEvent.BUTTON1]);
		appendStat(stats, "Middle", _MouseCounters[MouseEvent.BUTTON2]);
		appendStat(stats, "Right", _MouseCounters[MouseEvent.BUTTON3]);

		return stats.toString();
	}

	public static void resetStats() {
		_StartTime = new Date();
		_AccessedFrames = 0;
		_SavedFrames = 0;
		_CreatedFrames = 0;
		_CharCounts = new int[255];
		_EscapeCount = 0;
		_BackspaceCount = 0;
		_DarkTime.setTime(0);
		_MouseCounters = new int[MOUSE_BUTTONS];

		for (int i = 0; i < ITEM_TYPES; i++) {
			for (int j = 0; j < STAT_TYPES; j++) {
				_ItemStats[i][j] = 0;
			}
		}
	}

	/**
	 * Called signal that a frame has been accessed.
	 * 
	 */
	public static void AccessedFrame() {
		if (_StatsEnabled) {
			_AccessedFrames++;
		}
	}

	public static void SavedFrame(String frameName) {
		FrameEdited(frameName);
		_SavedFrames++;
	}

	public static void Escape() {
		_EscapeCount++;
	}

	public static void CreatedFrame() {
		_CreatedFrames++;
	}

	/**
	 * Increments the count for a character that is typed.
	 * 
	 * @param ch
	 *            ascii value for the typed character
	 */
	public static void TypedChar(int ch) {
		if (ch == KeyEvent.VK_BACK_SPACE || ch == KeyEvent.VK_DELETE)
			_BackspaceCount++;
		UserEvent();
		_CharCounts[ch]++;
	}

	private static void UserEvent() {
		long thisEvent = new Date().getTime();
		long elapsedTime = thisEvent - _LastEvent;
		addEventTime(elapsedTime);
		if (elapsedTime > DARK_TIME_THRESHOLD) {
			_DarkTime.setTime(_DarkTime.getTime() + elapsedTime);
		}
		_LastEvent = thisEvent;
	}

	public static void AddFrameEvent(String description) {
		Date elapsedTime = getFrameTotalTime();

		_FrameEvents.add(Formatter.getTimeWithMillis(elapsedTime) + " "
				+ DisplayIO.getMouseX() + " " + FrameMouseActions.getY() + " "
				+ description);
	}

	public static String getFrameEventList(Frame currentFrame) {
		StringBuilder eventList = new StringBuilder();
		// First put on the session and darkTime
		Time darkTime = currentFrame == null ? getFrameDarkTime()
				: currentFrame.getDarkTime();
		Time activeTime = currentFrame == null ? getFrameActiveTime()
				: currentFrame.getActiveTime();
		eventList.append(ACTIVE_TIME_ATTRIBUTE).append(
				Formatter.getTimePeriod(activeTime)).append('\n');
		eventList.append(DARK_TIME_ATTRIBUTE).append(
				Formatter.getTimePeriod(darkTime)).append('\n');
		for (String s : _FrameEvents)
			eventList.append(s).append('\n');
		if (eventList.length() > 0)
			eventList.deleteCharAt(eventList.length() - 1);
		return eventList.toString();
	}

	public static String getFrameEventList() {
		return getFrameEventList(null);
	}

	public static void MouseClicked(int button) {
		UserEvent();
		_MouseCounters[button]++;
	}

	public static void setEnabled(boolean value) {
		_StatsEnabled = true;
	}

	private static void FrameEdited(String name) {
		_FramesEdited.append(Formatter.getLongDateTime()).append("[").append(
				name).append("]\n");
	}

	public static String getFramesEdited() {
		return _FramesEdited.toString();
	}

	public static StringBuffer getShortStats() {
		StringBuffer sb = new StringBuffer();
		sb.append("FramesA:").append(_AccessedFrames);
		sb.append(", FramesE:").append(_SavedFrames);
		sb.append(", ").append(getResponseStats());
		return sb;
	}

	private static void ItemStats(Collection<Item> items, StatType stat) {
		if (items == null)
			return;
		for (Item i : items) {
			if (i instanceof Text)
				_ItemStats[ItemType.Text.ordinal()][stat.ordinal()]++;
			else if (i instanceof Dot)
				_ItemStats[ItemType.Dot.ordinal()][stat.ordinal()]++;
			else if (i instanceof Picture)
				_ItemStats[ItemType.Picture.ordinal()][stat.ordinal()]++;
			else if (i instanceof Line)
				_ItemStats[ItemType.Line.ordinal()][stat.ordinal()]++;
		}
	}

	public static void CreatedItems(Collection<Item> items) {
		ItemStats(items, StatType.Created);
	}

	public static void MovedItems(Collection<Item> items) {
		ItemStats(items, StatType.Moved);
	}

	public static void CopiedItems(Collection<Item> items) {
		ItemStats(items, StatType.Copied);
	}

	public static void DeletedItems(Collection<Item> items) {
		ItemStats(items, StatType.Deleted);
	}

	public static void CreatedText() {
		_ItemStats[ItemType.Text.ordinal()][StatType.Created.ordinal()]++;
	}

	public static String getItemStats() {

		StringBuffer sb = getLength();

		int max = 0;
		final int TOTAL_INDEX = ITEM_TYPES - 1;
		for (int i = 0; i < STAT_TYPES; i++) {
			_ItemStats[TOTAL_INDEX][i] = 0;
			for (int j = 0; j < TOTAL_INDEX; j++) {
				_ItemStats[TOTAL_INDEX][i] += _ItemStats[j][i];
			}
		}
		for (int i = 0; i < STAT_TYPES; i++) {
			max = Math.max(_ItemStats[TOTAL_INDEX][i], max);
		}
		int maxWidthValue = ("" + max).length();
		int maxWidthRate = (getRate(max)).length();

		int maxNameWidth = 0;
		int maxColumnWidth = 0;
		ItemType[] itemTypes = ItemType.values();
		StatType[] statTypes = StatType.values();
		// Get the width of the longest itemType
		for (int i = 0; i < ITEM_TYPES; i++) {
			maxNameWidth = Math.max(maxNameWidth, itemTypes[i].toString()
					.length());
		}

		for (int i = 0; i < STAT_TYPES; i++) {
			maxNameWidth = Math.max(maxColumnWidth, statTypes[i].toString()
					.length());
		}
		maxColumnWidth = Math.max(maxWidthRate + maxWidthValue + 3,
				maxNameWidth);

		sb.append(getBufferedString("", maxNameWidth)).append(COLUMN_SEPARATOR);

		StringBuffer lineSeparator = getBufferedString("", maxNameWidth,
				ROW_SEPARATOR_CHAR);
		lineSeparator.append(ROW_COLUMN_SEPARATOR);

		for (int i = 0; i < STAT_TYPES; i++) {
			sb.append(
					getBufferedString(statTypes[i].toString(), maxColumnWidth))
					.append(COLUMN_SEPARATOR);
			lineSeparator.append(getBufferedString("", maxColumnWidth,
					ROW_SEPARATOR_CHAR).append(ROW_COLUMN_SEPARATOR));
		}
		// Remove the last column separator
		lineSeparator.delete(lineSeparator.length()
				- ROW_COLUMN_SEPARATOR.length(), lineSeparator.length() - 1);
		sb.delete(sb.length() - COLUMN_SEPARATOR.length(), sb.length() - 1);
		sb.append('\n');
		sb.append(lineSeparator).append('\n');

		for (int j = 0; j < ITEM_TYPES; j++) {
			sb.append(getBufferedString(itemTypes[j].toString(), maxNameWidth))
					.append(COLUMN_SEPARATOR);
			for (int i = 0; i < STAT_TYPES; i++) {
				int total = 0;
				int nonZeroItems = 0;
				String statName = StatType.values()[i].toString();

				int value = _ItemStats[j][i];
				if (value > 0)
					nonZeroItems++;
				total += value;
				sb.append(getCompactStat(ItemType.values()[j].toString()
						+ statName, value, maxWidthValue, maxColumnWidth));
			}
			sb.delete(sb.length() - COLUMN_SEPARATOR.length(), sb.length() - 1);
			sb.append('\n');
		}
		sb.append(lineSeparator.toString().replace(ROW_COLUMN_SEPARATOR_CHAR,
				BOTTOM_COLUMN_SEPARATOR_CHAR));
		return sb.toString();
	}

	public static String getEventStats() {
		StringBuffer stats = getLength();
		int max = getMax(_EventTimes);
		int maxWidthEvents = ("" + max).length();
		int maxWidthRate = (getRate(max)).length();
		for (int i = 0; i < _EventTimes.length - 1; i++) {
			String upperBound = getFormattedTime(EVENT_INTERVALS[i]);
			appendStat(stats, "<" + upperBound, _EventTimes[i], true, false,
					maxWidthEvents, maxWidthRate);
		}
		int lastIndex = EVENT_INTERVALS.length - 1;
		appendStat(stats, "+"
				+ getFormattedTime(EVENT_INTERVALS[lastIndex - 1]),
				_EventTimes[lastIndex], false, false, maxWidthEvents,
				maxWidthRate);
		return stats.toString();
	}

	/**
	 * Gets the highest number in the list of numbers.
	 * 
	 * @param nums
	 *            list of numbers
	 * @return highest number in the list
	 */
	private static int getMax(int[] nums) {
		int max = 0;
		for (int i = 0; i < nums.length; i++)
			if (nums[i] > max)
				max = nums[i];
		return max;
	}

	private static String getFormattedTime(int secs) {
		String time;
		if (secs < 60) {
			time = secs + "s";
		} else if (secs < 60 * 60) {
			time = (secs / 60) + "m";
		} else {
			time = (secs / 60 / 60) + "h";
		}
		if (time.length() == 2)
			time = " " + time;
		return time;
	}

	/**
	 * @return
	 */
	public static StringBuffer getLength() {
		StringBuffer stats = getDate();
		stats.append("Start: ").append(Formatter.getDateTime(_StartTime))
				.append("\n");
		stats.append("SessionTime: ").append(getTimeElapsed()).append("\n");
		return stats;
	}

	private static void addEventTime(long millis) {

		for (int i = 0; i < _EventTimes.length - 1; i++) {
			if (millis < EVENT_INTERVALS[i] * 1000) {
				_EventTimes[i]++;
				return;
			}
		}
		_EventTimes[_EventTimes.length - 1]++;
	}

	public static void DeletedItem(Item toDelete) {
		List<Item> items = new ArrayList<Item>();
		items.add(toDelete);
		DeletedItems(items);
	}

	/**
	 * Reset the stats ready to keep track of the stats for this current session
	 * of editting a newly displayed frame. This method is called everytime the
	 * user navigates to a new frame.
	 * 
	 * The frame access time is used when the frame is saved to see how long the
	 * user was editting the frame. The frameAccessDark time keeps track of the
	 * total session dark time when this frame was accessed. If the frame is
	 * editted and saved, its the difference in the current session dark time
	 * and the session dark time when the frame was accessed, is added to the
	 * frames dark time.
	 * 
	 */
	public static void NewFrameSession() {
		_FrameEvents.clear();
		_FrameAccessTime = new Date();
		_FrameAccessDarkTime = (Time) _DarkTime.clone();
	}
}
