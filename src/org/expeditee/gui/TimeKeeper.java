package org.expeditee.gui;

import java.text.DecimalFormat;

/**
 * Provides methods an easy way of measuring time. Currently uses
 * System.nanoTime to get the most precise time measurement available, results
 * are then converted to milliseconds.
 * 
 * @author jdm18
 */
public class TimeKeeper {

	/**
	 * The Unit of measure for the returned time values
	 */
	public static DecimalFormat Formatter = new DecimalFormat("0.00");

	private long _start = 0;

	private static String MILLIS = "ms";

	private static String SECONDS = "sec";

	/**
	 * Constructs a new TimeKeeper object and initiates the starting time
	 */
	public TimeKeeper() {
		restart();
	}

	/**
	 * Returns the time Sandbox has been running, in the time unit set in UNIT.
	 * 
	 * @return The amount of time Sandbox has been running.
	 */
	public static long getCurrentTime() {
		long nanoTime = System.nanoTime();

		nanoTime /= 1000000;

		return nanoTime;
	}

	/**
	 * Resets the starting time of this TimeKeeper to the value returned by
	 * getCurrentTime()
	 */
	public void restart() {
		_start = getCurrentTime();
	}

	/**
	 * Returns the difference between the current time (getCurrentTime()) and
	 * this TimeKeeper's starting time.
	 * 
	 * @return The time that has elapsed since this TimeKeeper's start time.
	 */
	public long getElapsedMillis() {
		return getCurrentTime() - _start;
	}

	/**
	 * The same result as getElapsed() but with the unit of measure (UNIT)
	 * appended.
	 * 
	 * @return The time that has elapsed since this TimeKeeper's start time,
	 *         with the unit of measure displayed.
	 */
	public String getElapsedStringMillis() {
		return getElapsedMillis() + MILLIS;
	}

	public String getElapsedStringMillis(int pad) {
		long time = getElapsedMillis();
		String padding = "";
		for (int i = 0; i < pad; i++)
			if (time == 0)
				padding += " ";
			else
				time /= 10;

		return padding + getElapsedMillis() + MILLIS;
	}

	// returns the string in mm:ss format, with seconds shown to 3 decimal
	// places
	public String getElapsedStringFull() {
		int mins = (int) Math.floor(getElapsedSeconds() / 60f);
		float secs = getElapsedSeconds() - (mins * 60);

		return mins + ":" + Formatter.format(secs);
	}

	public String getElapsedStringSeconds() {
		return Formatter.format(getElapsedSeconds()) + SECONDS;
	}

	public float getElapsedSeconds() {
		return getElapsedMillis() / 1000f;
	}
}
