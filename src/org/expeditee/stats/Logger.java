package org.expeditee.stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.expeditee.gui.TimeKeeper;

/**
 * This class is used to create all log files. The log file is opened, appended,
 * and closed for each new entry to ensure robustness against program crashes.
 * Files are created with filename: UserData.Date.Time<br>
 * Where Date is in the format DDMMMYYYY and Time is in the format HHMM. <br>
 * <br>
 * Log files are stored in the following format: <br>
 * <code>
 * TIME: (Time since program start) <br>
 * X,Y (For user events only) <br> 
 * Origin (System/User) <br>
 * Description <br>
 * </code> <br>
 * Exceptions can also be logged, in which case the result from
 * Exception.printStackTrace() is output to the file.
 * 
 * @author jdm18
 * 
 */
public class Logger {

	public static final String SYSTEM = "S";

	public static final String USER = "U";

	public static final String LOAD = "Load";

	public static final String SAVE = "Save";

	public static final String PICKUP = "Pickup";

	public static final String ANCHOR = "Anchor";

	public static final String DELETE = "Delete";

	public static final String TDFC = "Top Down Frame Creation";

	public static final String NEW_FRAMESET = "New Frameset";

	public static final String PICKUP_COPY = "Pickup Copies";

	public static final String ANCHOR_COPY = "Anchor Copies";

	public static final String CONTROL_CHAR = "Control Character";

	public static final String CREATE_TEXT = "Creating Text";

	public static final String INSERTING_TEXT = "Inserting Text";

	public static final String BACK = "Back";

	public static final String FOLLOW_LINK = "Following Link";

	public static final String MERGING = "Merging";

	// These should be the same as used in KMSReader
	private static String _filename = null;

	// how many spaces the output time should be padded to
	private static int _padTime = 8;

	private static TimeKeeper _timer = new TimeKeeper();

	/**
	 * Sets the path on disk that the log files will be created in
	 * 
	 * @param location
	 *            The path on disk to create the log files in.
	 */
	public static void Init() {
		if (!org.expeditee.settings.UserSettings.Logging.get())
			return;

		File test = new File(org.expeditee.gui.FrameIO.LOGS_DIR);
		if (!test.exists())
			test.mkdir();

		_filename = "UserData-" + Formatter.getDateTime() + ".txt";
	}

	/**
	 * Appends the given event to the log file.
	 * 
	 * @param origin
	 *            The origin of this event, typically SYSTEM
	 * @param type
	 *            The Type of this event (LOAD, SAVE, CLICK, etc)
	 * @param comments
	 *            Any further comments that should be recorded about the event
	 */
	public static void Log(String origin, String type, String comments) {
		String toWrite = "TIME:\t" + _timer.getElapsedStringMillis(_padTime)
				+ " \t\t\t\t" + " - " + origin + " - " + type + " - "
				+ comments;
		WriteToFile(toWrite);
	}

	/**
	 * Appends the given event to the log file.
	 * 
	 * @param x
	 *            The mouse X coordinate when the event occured
	 * @param y
	 *            The mouse Y coordinate when the event occured
	 * @param origin
	 *            The origin of the event, typically USER
	 * @param type
	 *            The type of this event (LOAD, SAVE, CLICK, etc).
	 * @param comments
	 *            Any further comments that should be recorded about the event
	 */
	public static void Log(int x, int y, String origin, String type,
			String comments) {
		String toWrite = "TIME:\t" + _timer.getElapsedStringMillis(_padTime)
				+ " \t X:" + x + " Y:" + y + " - " + origin + " - " + type
				+ " - " + comments;
		WriteToFile(toWrite);
	}

	/**
	 * Appends a record of the Exception to the log file. The text output is
	 * equal to that output by e.printStackTrace()
	 * 
	 * @param e
	 *            The Exception to log.
	 */
	public static void Log(Exception e) {
		String toWrite = "TIME:" + _timer.getElapsedStringMillis(_padTime)
				+ "\tException Thrown: " + e.getClass().getSimpleName();

		WriteToFile(toWrite);

		StackTraceElement[] st = e.getStackTrace();

		for (StackTraceElement element : st)
			WriteToFile(element.toString());

	}

	private static void WriteToFile(String toWrite) {
		if (!org.expeditee.settings.UserSettings.Logging.get())
			return;

		// if Init has not been run yet
		if (_filename == null)
			Init();

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(
					org.expeditee.gui.FrameIO.LOGS_DIR + _filename, true));
			writer.write(toWrite);
			writer.newLine();

			writer.flush();
			writer.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

}
