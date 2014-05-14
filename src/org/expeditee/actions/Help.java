package org.expeditee.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

public class Help {
	/**
	 * Return a list of actions. TODO show the params they take. TODO link the
	 * actions to their help pages.
	 * 
	 * @return
	 */
	public static Collection<Item> getActions(String start, String end) {
		return getItemList(Actions.getActions(), start, end);

	}

	public static Collection<Item> getActions(String start) {
		return getItemList(Actions.getActions(), start, null);

	}

	public static Collection<Item> getActions() {
		return getItemList(Actions.getActions(), null, null);

	}

	public static Collection<Item> getAgents(String start, String end) {
		return getItemList(Actions.getAgents(), start, end);

	}

	public static Collection<Item> getAgents(String start) {
		return getItemList(Actions.getAgents(), start, null);

	}

	public static Collection<Item> getAgents() {
		return getItemList(Actions.getAgents(), null, null);
	}

	private static Collection<Item> getItemList(List<String> stringList,
			String start, String end) {
		Collection<Item> actions = new LinkedList<Item>();

		Collections.sort(stringList, new Comparator<String>() {
			public int compare(String a, String b) {
				return String.CASE_INSENSITIVE_ORDER.compare(a, b);
			}
		});

		Frame current = DisplayIO.getCurrentFrame();
		float x = FrameMouseActions.MouseX;
		float y = FrameMouseActions.MouseY;

		// Do case sensitive comparison
		if (start != null)
			start = start.toLowerCase();
		if (end != null)
			end = end.toLowerCase();

		for (String s : stringList) {
			String lower = s.toLowerCase();
			if (start != null) {
				if (end != null) {
					if (lower.compareToIgnoreCase(start) < 0)
						continue;
					if (lower.compareToIgnoreCase(end) > 0)
						break;
				} else if (!lower.matches(start) && !lower.startsWith(start)) {
					continue;
				}
			}

			Text t = current.createNewText(s);
			t.setWidth(1000);
			t.setPosition(x, y);
			y += t.getBoundsHeight();
			actions.add(t);
		}

		return actions;
	}
}
