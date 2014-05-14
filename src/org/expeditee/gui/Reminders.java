package org.expeditee.gui;

import java.awt.Color;
import java.util.Date;

import org.expeditee.actions.Misc;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.charts.TimeSeries;

public class Reminders {

	public synchronized static void init(final Frame reminderFrame) {
		if(reminderFrame == null)
			return;
		
		for (Text text : reminderFrame.getBodyTextItems(false)) {
			try {
				final Text reminderItem = text;
				final AttributeValuePair avp = new AttributeValuePair(text
						.getText(), true);
				final String dateString = avp.getAttributeOrValue();
				final String reminderString;
				if (avp.hasPair()) {
					reminderString = avp.getValue() + " at "
							+ avp.getAttribute();
				} else {
					reminderString = dateString;
				}

				Date date = TimeSeries.parseDate(dateString);
				long millisToWait = date.getTime() - new Date().getTime();
				final long adjustedToWait;

				// Adjust the time to wait if their is no time left
				if (millisToWait < 0) {
					adjustedToWait = 0;
				} else {
					adjustedToWait = millisToWait;
				}
				// Now create the reminder
				new Thread() {
					public void run() {
						try {
							Thread.sleep(adjustedToWait);
							synchronized (_alertsRunning) {
								_alertsRunning++;
							}

							do {
								Misc.beep();
								MessageBay.displayMessage(
										"Reminder: " + reminderString, null,
										Color.red, false, "StopReminder");
								Thread.sleep(5000);
								// newMessage.setColor(new Color(100, 70, 70));
								// Thread.sleep(100);
								// newMessage.setColor(Color.red);
							} while (!_bStop);
							synchronized (_alertsRunning) {
								assert (_alertsRunning > 0);
								_alertsRunning--;
								reminderItem.setText("@" + reminderItem.getText());
								FrameIO.ForceSaveFrame(reminderItem.getParent());
								if (_alertsRunning == 0) {
									_bStop = false;
								}
							}
						} catch (Exception e) {
							//e.printStackTrace();
						}
					}
				}.start();
			} catch (Exception e) {
				MessageBay.errorMessage("Invalid reminder format: "
						+ text.getText());
			}
		}
	}

	private static Boolean _bStop = false;

	private static Integer _alertsRunning = 0;

	public synchronized static void stop() {
		if (_alertsRunning > 0)
			_bStop = true;
	}
}
