package org.expeditee.stats;

import java.util.Date;

public class AgentStats {

	private static int _ItemsExecuted = 0;

	private static int _FramesExecuted = 0;

	private static String SECONDS = "sec";

	private static Date _StartTime = new Date();

	private static String getTimeElapsedString() {
		return "Time: " + getSecondsElapsed() + SECONDS;
	}

	public static long getMilliSecondsElapsed() {
		return new Date().getTime() - _StartTime.getTime();
	}

	public static String getStats() {
		StringBuffer stats = new StringBuffer();
		stats.append("Run ").append(getTimeElapsedString()).append(
				", Processed ");
		appendStat(stats, "frames", _FramesExecuted, ", ");
		appendStat(stats, "items", _ItemsExecuted, "");
		return stats.toString();
	}

	private static double getSecondsElapsed() {
		return (new Date().getTime() - _StartTime.getTime()) / 1000.0;
	}

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
	private static void appendStat(StringBuffer stats, String name, int value,
			String separator) {
		// prevent divide by zero errors
		if (value < 0)
			value = 0;

		int perSec = (int) (value / Math.max(0.001, getSecondsElapsed()));

		stats.append(value).append(" ").append(name).append(" @ ").append(
				perSec).append("/" + SECONDS);
		stats.append(separator);
	}

	public static void reset() {
		_StartTime = new Date();
		_FramesExecuted = 0;
		_ItemsExecuted = 0;
	}

	/**
	 * Called signal that a frame has been accessed.
	 * 
	 */
	public static void FrameExecuted() {
		_FramesExecuted++;
	}

	public static void ItemExecuted() {
		_ItemsExecuted++;
	}
}
