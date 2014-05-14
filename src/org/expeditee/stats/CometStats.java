package org.expeditee.stats;

import java.sql.Time;

import org.expeditee.gui.Frame;

public class CometStats extends Stats{
	protected int _sessions = 0;

	protected String _name = null;
	protected String _title = null;

	protected long _active = 0;

	protected long _dark = 0;

	public CometStats(Frame topFrame) {
		_name = topFrame.getName();
		_title = topFrame.getTitle();

		_active = getFrameActiveTime().getTime()
				+ topFrame.getActiveTime().getTime();
		_dark = getFrameDarkTime().getTime()
				+ topFrame.getDarkTime().getTime();
		_sessions = topFrame.getVersion() + 1;
	}

	@Override
	public String toString() {
		Time total = new Time(_active + _dark);
		Time active = new Time(_active);
		Time dark = new Time(_dark);

		StringBuffer sb = new StringBuffer();
		sb.append(SessionStats.getDate());
		sb.append("CometStats: ").append(_name).append('\n');
		sb.append("Versions: ").append(_sessions).append('\n');
		sb.append(String.format("           %c Current  %c Average  %c Total", SessionStats.COLUMN_SEPARATOR_CHAR,SessionStats.COLUMN_SEPARATOR_CHAR,SessionStats.COLUMN_SEPARATOR_CHAR)).append('\n');
		
		
		StringBuffer rowSeparator = new StringBuffer();
		rowSeparator.append(getBufferedString(10, ROW_SEPARATOR_CHAR));
		for(int i=0;i<3;i++){
			rowSeparator.append(ROW_COLUMN_SEPARATOR).append(getBufferedString(8, ROW_SEPARATOR_CHAR));
		}
		sb.append(rowSeparator).append('\n');
		
		
		Time averageActive = new Time((long) (0.5 + active.getTime()
				/ (1.0 * _sessions)));
		Time averageDark = new Time((long) (0.5 + dark.getTime()
				/ (1.0 * _sessions)));
		Time averageTotal = new Time((long) (0.5 + total.getTime()
				/ (1.0 * _sessions)));
		Time currentActive = SessionStats.getFrameActiveTime();
		Time currentDark = SessionStats.getFrameDarkTime();
		Time currentTotal = SessionStats.getFrameTotalTime();

		sb.append("ActiveTime").append(COLUMN_SEPARATOR).append(Formatter.getTimePeriod(currentActive))
				.append(COLUMN_SEPARATOR).append(
						Formatter.getTimePeriod(averageActive)).append(
						COLUMN_SEPARATOR).append(Formatter.getTimePeriod(active))
				.append('\n');
		sb.append("  DarkTime").append(COLUMN_SEPARATOR).append(Formatter.getTimePeriod(currentDark))
				.append(COLUMN_SEPARATOR).append(
						Formatter.getTimePeriod(averageDark)).append(
						COLUMN_SEPARATOR).append(Formatter.getTimePeriod(dark))
				.append('\n');
		sb.append(" TotalTime").append(COLUMN_SEPARATOR).append(Formatter.getTimePeriod(currentTotal))
				.append(COLUMN_SEPARATOR).append(
						Formatter.getTimePeriod(averageTotal)).append(
						COLUMN_SEPARATOR).append(Formatter.getTimePeriod(total)).append('\n');
		sb.append(rowSeparator.toString().replace(ROW_COLUMN_SEPARATOR_CHAR, BOTTOM_COLUMN_SEPARATOR_CHAR));
		return sb.toString();
	}
}
