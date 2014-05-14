package org.expeditee.agents;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameUtils;
import org.expeditee.items.Text;

public class Sort extends DefaultAgent {

	/**
	 * Sorts all the text Items on the given Frame alphabetically. The Items are
	 * then rearranged to reflect this ordering.
	 */
	public Frame process(Frame start) {
		// Check the position of the cursor and only format stuff inside the
		// same box as the cursor
		Collection<Text> itemsToSort = FrameUtils.getCurrentTextItems();
		if (itemsToSort.size() < 1) {
			itemsToSort = start.getBodyTextItems(false);
		}

		ArrayList<Text> textItems = new ArrayList<Text>();
		textItems.addAll(itemsToSort);

		// copy current positions of items
		ArrayList<Point> positions = new ArrayList<Point>(textItems.size());

		for (int i = 0; i < textItems.size(); i++)
			positions.add(i, textItems.get(i).getPosition());

		// Sort text items by their strings
		Collections.sort(textItems, new Comparator<Text>() {
			public int compare(Text a, Text b) {
				return String.CASE_INSENSITIVE_ORDER.compare(a.getText(), b
						.getText());
			}
		});

		// update positions based on new order
		for (int i = 0; i < positions.size(); i++)
			textItems.get(i)
					.setPosition(positions.get(i).x, positions.get(i).y);

		// items will need to be resorted after this
		start.setResort(true);
		FrameGraphics.Repaint();

		return null;
	}

	@Override
	protected void finalise(Frame start) {
		message("Sorting Complete.");
	}
}