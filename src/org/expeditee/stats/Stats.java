package org.expeditee.stats;

import java.sql.Time;
import java.util.Date;


public abstract class Stats {
	protected static Time _DarkTime = new Time(0);
	protected static Date _StartTime = new Date();
	
	/**The time the current frame was accessed (displayed)*/
	protected static Date _FrameAccessTime = new Date();
	
	/**The session dark time when the current frame was accessed (displayed)*/
	protected static Date _FrameAccessDarkTime = new Date();
	
	protected static final long MILLISECONDS_PER_MINUTE = 60000;
	
	public static final char ROW_SEPARATOR_CHAR = '\u2500';
	
	public static final char COLUMN_SEPARATOR_CHAR = '\u2502';
	
	public static final char ROW_COLUMN_SEPARATOR_CHAR = '\u253C';

	public static final char BOTTOM_COLUMN_SEPARATOR_CHAR = '\u2534';

	public static final char TOP_COLUMN_SEPARATOR_CHAR = '\u252C';
	
	public static final String COLUMN_SEPARATOR = " " + COLUMN_SEPARATOR_CHAR
			+ ' ';

	public static final String ROW_COLUMN_SEPARATOR = "" + ROW_SEPARATOR_CHAR + ROW_COLUMN_SEPARATOR_CHAR + ROW_SEPARATOR_CHAR;

	public static final String DARK_TIME_ATTRIBUTE = "DarkTime:";

	public static final String ACTIVE_TIME_ATTRIBUTE = "ActiveTime:";

	protected static final int DEFAULT_RATE_WIDTH = 4;

	protected static final int DEFAULT_VALUE_WIDTH = 3;
	
	/**
	 * Appends a single stats to a string buffer containing a collection of
	 * stats.
	 * 
	 * @param stats
	 *            The string buffer to append the stat onto
	 * @param name
	 *            The name of the stat
	 * @param value
	 *            The new value for the stat
	 */
	protected static void appendStat(StringBuffer stats, String name, int value,
			boolean newline, boolean ignoreZero, int minValueWidth,
			int minRateWidth) {
		// prevent divide by zero errors
		if (ignoreZero && value <= 0)
			return;

		String perHour = getRate(value);
		while (perHour.length() < minRateWidth)
			perHour = " " + perHour;
		String valueString = "" + value;
		while (valueString.length() < minValueWidth)
			valueString = " " + valueString;
		stats.append(valueString).append(" @ ").append(perHour.toString())
				.append("/hour ").append(name);
		if (newline)
			stats.append("\n");
		else
			stats.append(" ");

	}

	protected static StringBuffer getCompactStat(String name, int value,
			int minValueWidth, int minTotalWidth) {
		StringBuffer stats = new StringBuffer();
		// prevent divide by zero errors
		if (value > 0) {
			String perHour = getRate(value);
			String valueString = "" + value;
			while (valueString.length() < minValueWidth)
				valueString = " " + valueString;
			stats.append(valueString).append("@").append(perHour.toString())
					.append("/h");
		}
		while (stats.length() < minTotalWidth) {
			stats.append(' ');
		}
		return stats.append(COLUMN_SEPARATOR);
	}

	protected static String getRate(int value) {
		return "" + Math.round(value * 60 / getMinutesUsed());
	}

	protected static void appendStat(StringBuffer stats, String name, int value) {
		appendStat(stats, name, value, true, false, DEFAULT_VALUE_WIDTH,
				DEFAULT_RATE_WIDTH);
	}
	
	protected static double getMinutesUsed() {
		long elapsedTime = new Date().getTime() - _StartTime.getTime()
				- _DarkTime.getTime();

		return (double) elapsedTime / MILLISECONDS_PER_MINUTE;
	}
	
	public static StringBuffer getDate() {
		StringBuffer stats = new StringBuffer("@Date: ");
		stats.append(Formatter.getDateTime()).append("\n");
		return stats;
	}
	
	public static StringBuffer getBufferedString(String string, int width) {
		return getBufferedString(string, width, ' ');
	}
	
	public static StringBuffer getBufferedString(int width, char bufferChar) {
		return getBufferedString("", width, bufferChar);
	}
	
	public static StringBuffer getBufferedString(int width) {
		return getBufferedString("", width);
	}

	public static StringBuffer getBufferedString(String start, int width,
			char bufferChar) {
		StringBuffer sb = new StringBuffer(start);
		while (sb.length() < width) {
			sb.append(bufferChar);
		}
		return sb;
	}
	
	public static Time getFrameDarkTime() {
		return new Time(_DarkTime.getTime() - _FrameAccessDarkTime.getTime());
	}

	public static Time getFrameActiveTime() {
		return new Time(getFrameTotalTime().getTime()
				- getFrameDarkTime().getTime());
	}

	public static Time getFrameTotalTime() {
		return new Time((new Date()).getTime() - _FrameAccessTime.getTime());
	}
}
