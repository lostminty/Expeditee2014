package org.expeditee.stats;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Formatter {

	public static final String DATE_FORMAT = "ddMMMyyyy";

	public static final String TIME_FORMAT = "[HH:mm]";

	private static final String LONG_TIME_FORMAT = "[HH:mm.ss]";
	
	private static final String TIME_WITH_MILLISECONDS = "mm:ss:SSS";

	private static final String LONG_DATE_TIME_FORMAT = DATE_FORMAT
			+ LONG_TIME_FORMAT;

	public static final String DATE_TIME_FORMAT = DATE_FORMAT + TIME_FORMAT;

	private static String EasyDateFormat(String format, Date date) {
		return (new SimpleDateFormat(format)).format(date);
	}
	
	private static String EasyDateFormat(String format) {
		return EasyDateFormat(format, new Date());
	}

	public static String getTimePeriod(Time time) {
		/*
		 * Truncate the millis
		 */
		long total = time.getTime() / 1000;
		long seconds = total % 60;
		// Truncate the secs
		total /= 60;
		long minutes = total % 60;
		// Truncate the minutes
		long hours = total / 60;

		return String.format("%1$02d:%2$02d:%3$02d", hours, minutes, seconds);
	}

	public static String getLongDateTime() {
		return EasyDateFormat(LONG_DATE_TIME_FORMAT);
	}

	public static String getDateTime(Date dateTime) {
		return EasyDateFormat(DATE_TIME_FORMAT, dateTime);
	}
	
	public static String getDateTime() {
		return EasyDateFormat(DATE_TIME_FORMAT);
	}

	public static String getTimeWithMillis(Date elapsedTime) {
		return EasyDateFormat(TIME_WITH_MILLISECONDS);
	}

	public static String getDate() {
		return EasyDateFormat(DATE_FORMAT);
	}

}
