package org.expeditee.stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.items.Text;
import org.expeditee.settings.UserSettings;

/**TODO make this threadsafe*/

/**
 * This class is used to create all stats log files. Files are created with
 * filename: Date-Time.stat<br>
 * Where Date is in the format DDMMMYYYY and Time is in the format HHMM.
 * 
 * @author mrww1
 * 
 */
public class StatsLogger {

	private static String _filename = null;

	private static final String FRAMES_EDITED_FILENAME = "FramesEdited.log";

	/**
	 * Sets the path on disk that the statlog files will be created in.
	 * 
	 * @param location
	 *            The path on disk to create the log files in.
	 */
	private static void Init() {
		if (!org.expeditee.settings.UserSettings.LogStats.get())
			return;

		File test = new File(org.expeditee.gui.FrameIO.STATISTICS_DIR);
		if (!test.exists())
			test.mkdir();

		_filename = Formatter.getDateTime() + ".stat";
	}

	/**
	 * Writes the current stats to a file.
	 * 
	 */
	public static void WriteStatsFile() {
		if (!org.expeditee.settings.UserSettings.LogStats.get())
			return;

		Init();

		String statsFrameset = UserSettings.StatisticsFrameset.get();
		if (statsFrameset != null) {
			try {
				Frame statsFrame = null;
				if (FrameIO.LoadFrame(statsFrameset + "0") == null) {
					statsFrame = FrameIO.CreateNewFrameset(statsFrameset);
					statsFrame.setTitle(Formatter.getDateTime());
				} else {
					statsFrame = FrameIO.CreateFrame(statsFrameset, Formatter
							.getDateTime(), null);
				}
				
				//Now check the attribute value pairs
				for(Text t: statsFrame.getBodyTextItems(false)){
					Method m = StatsFrame.getMethod(t.getText());
					if(m != null){
						String value = m.invoke(null, new Object[]{}).toString();
						t.setText(t.getText() + ": " + value);
					}
				}
				FrameIO.ForceSaveFrame(statsFrame);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(
					org.expeditee.gui.FrameIO.STATISTICS_DIR + _filename, true));

			writer.write(SessionStats.getCurrentStats());
			writer.newLine();
			writer.newLine();
			writer.write(SessionStats.getItemStats());
			writer.newLine();
			writer.newLine();
			writer.write(SessionStats.getEventStats());
			writer.newLine();

			writer.flush();
			writer.close();

			writer = new BufferedWriter(new FileWriter(
					org.expeditee.gui.FrameIO.STATISTICS_DIR
							+ FRAMES_EDITED_FILENAME, true));
			writer.write(SessionStats.getFramesEdited());
			writer.newLine();

			writer.flush();
			writer.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
