package org.expeditee.stats;

import java.sql.Time;
import java.util.HashSet;
import java.util.Set;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;

public class TreeStats extends CometStats {
	protected int _treeFrames = 0;

	protected int _treeSessions = 0;

	protected long _treeActive = 0;

	protected long _treeDark = 0;

	public TreeStats(Frame topFrame) {
		this(topFrame, new HashSet<String>());
	}

	public TreeStats(Frame topFrame, Set<String> visited) {
		super(topFrame);
		//TreeStats doesnt include the current session.
		_active = topFrame.getActiveTime().getTime();
		_dark = topFrame.getDarkTime().getTime();
		_sessions -= 1;

		visited.add(_name.toLowerCase());
		MessageBay.overwriteMessage("Computed: " + _name);

		// Initialise variables with the data for this frames comet
		_treeActive = _active;
		_treeDark = _dark;
		_treeSessions = _sessions;
		_treeFrames = 1;
		// Now get all add all the trees for linked items
		for (Item i : topFrame.getItems()) {
			String link = i.getAbsoluteLink();
			if (link == null)
				continue;
			if (i.isAnnotation())
				continue;
			// Stop infinate loops by not visiting nodes we have already visited
			if (visited.contains(link.toLowerCase())) {
				continue;
			}
			Frame childFrame = FrameIO.LoadFrame(i.getAbsoluteLink());
			if (childFrame == null)
				continue;

			TreeStats childItemStats = new TreeStats(childFrame, visited);
			_treeActive += childItemStats._treeActive;
			_treeDark += childItemStats._treeDark;
			_treeSessions += childItemStats._treeSessions;
			_treeFrames += childItemStats._treeFrames;
		}
	}

	@Override
	public String toString() {
		Time total = new Time(_treeActive + _treeDark);
		Time active = new Time(_treeActive);
		Time dark = new Time(_treeDark);

		StringBuffer sb = new StringBuffer();
		sb.append(SessionStats.getDate());
		sb.append("TreeStats: ").append(_name).append('\n');
		sb.append("Frames:   ").append(_treeFrames).append('\n');
		sb.append("Versions: ").append(_treeSessions).append('\n');
		sb.append(String.format(
				"           %cVersionAve%c FrameAve %c Total%n",
				COLUMN_SEPARATOR_CHAR, COLUMN_SEPARATOR_CHAR,
				COLUMN_SEPARATOR_CHAR));

		StringBuffer rowSeparator = new StringBuffer();
		rowSeparator.append(getBufferedString(10, ROW_SEPARATOR_CHAR));
		for (int i = 0; i < 3; i++) {
			rowSeparator.append(ROW_COLUMN_SEPARATOR).append(
					getBufferedString(8, ROW_SEPARATOR_CHAR));
		}
		sb.append(rowSeparator).append('\n');

		Time averageActive = new Time((long) (0.5 + active.getTime()
				/ (1.0 * _treeFrames)));
		Time averageDark = new Time((long) (0.5 + dark.getTime()
				/ (1.0 * _treeFrames)));
		Time averageTotal = new Time((long) (0.5 + total.getTime()
				/ (1.0 * _treeFrames)));
		Time averageActiveEdit = new Time((long) (0.5 + active.getTime()
				/ (1.0 * _treeSessions)));
		Time averageDarkEdit = new Time((long) (0.5 + dark.getTime()
				/ (1.0 * _treeSessions)));
		Time averageTotalEdit = new Time((long) (0.5 + total.getTime()
				/ (1.0 * _treeSessions)));

		sb.append("ActiveTime").append(COLUMN_SEPARATOR).append(
				Formatter.getTimePeriod(averageActiveEdit)).append(
				COLUMN_SEPARATOR).append(Formatter.getTimePeriod(averageActive))
				.append(COLUMN_SEPARATOR).append(Formatter.getTimePeriod(active))
				.append('\n');
		sb.append("  DarkTime").append(COLUMN_SEPARATOR).append(
				Formatter.getTimePeriod(averageDarkEdit))
				.append(COLUMN_SEPARATOR).append(
						Formatter.getTimePeriod(averageDark)).append(
						COLUMN_SEPARATOR).append(Formatter.getTimePeriod(dark))
				.append('\n');
		sb.append(" TotalTime").append(COLUMN_SEPARATOR).append(
				Formatter.getTimePeriod(averageTotalEdit)).append(
				COLUMN_SEPARATOR).append(Formatter.getTimePeriod(averageTotal))
				.append(COLUMN_SEPARATOR).append(Formatter.getTimePeriod(total))
				.append('\n');
		sb.append(rowSeparator.toString().replace(ROW_COLUMN_SEPARATOR_CHAR,
				BOTTOM_COLUMN_SEPARATOR_CHAR));
		
		return sb.toString();
	}
}
