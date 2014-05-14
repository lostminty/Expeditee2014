package org.expeditee.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameUtils;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

/**
 * A simple formatting agent that aligns all non-annotation Items on the given
 * Frame by the X and Y axis of the first Item on the Frame. This agent
 * separates the items into columns, the actual alignment is done by
 * FrameUtils.Align()
 * 
 * @author jdm18
 * 
 */
public class Format extends DefaultAgent {
	// whether the Items could be formatted successfully
	private boolean _success = true;

	// adjustmen from the default format spacing
	private int _adjust = 0;

	public Format() {
		super();
	}

	public Format(String param) {
		try {
			if (param.startsWith("+"))
				param = param.substring(1);
			_adjust = Integer.parseInt(param);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Frame process(Frame start) {
		// TODO What will happen if user runs the SIMPLE form of this...
		// Does format box need to be disabled?!?!
		// Check the position of the cursor and only format stuff inside the
		// same box as the cursor
		Collection<Text> itemsToFormat = getItemsToFormat(start);
		
		List<Item> changedItems = new ArrayList<Item>();

		ArrayList<Item> columnHeads = new ArrayList<Item>();

		ArrayList<ArrayList<Text>> columns = new ArrayList<ArrayList<Text>>();

		for (Text t : itemsToFormat) {
			int col = findColumn(columnHeads, t);
			// if this is the head of a new column
			if (col < 0) {
				columnHeads.add(t);
				columns.add(new ArrayList<Text>());
				// otherwise the column for this item has already been
				// found set the column to be the one we just added...
				col = columns.size() - 1;
			} else
				columns.get(col).add(t);
		}

		// check for empty columns
		int[] clear = new int[columnHeads.size()];
		for (int i = 0; i < columns.size(); i++)
			clear[i] = columns.get(i).size();

		// remove empty columns
		for (int i = (clear.length - 1); i >= 0; i--)
			if (clear[i] == 0) {
				columns.remove(i);
				columnHeads.remove(i);
			}

		// if there are no columns to align, we are done
		if (columns.size() == 0)
			return null;

		// sort the column heads by X position
		Collections.sort(columnHeads, new Comparator<Item>() {
			public int compare(Item o1, Item o2) {
				return o1.getX() - o2.getX();
			}

		});

		// sort lists by their X axis
		Collections.sort(columns, new Comparator<ArrayList<Text>>() {
			public int compare(ArrayList<Text> o1, ArrayList<Text> o2) {
				if (o2.size() == 0)
					return -10;

				if (o1.size() == 0)
					return 10;

				Item i1 = o1.get(0);
				Item i2 = o2.get(0);

				return (i1.getX() - i2.getX());
			}

		});

		int res = FrameUtils.Align(columns.get(0), true, _adjust);
		_success = _success && (res >= 0);

		for (int i = 0; i < columns.size() - 1; i++) {
			List<Text> list = columns.get(i);

			int maxX = 0;
			int maxY = 0;
			for (Item it : list) {
				maxX = Math.max(maxX, it.getX() + it.getBoundsWidth());
				maxY = Math.max(maxY, it.getY() + it.getBoundsHeight());
			}

			int xCheck = maxX;
			for (Item it : columns.get(i + 1))
				xCheck = Math.max(xCheck, maxX
						+ /* item.getX() + */it.getBoundsWidth());

			if (xCheck < FrameGraphics.getMaxSize().width) {
				Item columnHead = columnHeads.get(i + 1);
				if (columnHead.getX() < maxX && columnHead.getY() < maxY) {
					if(columnHead.getX() != maxX && !changedItems.contains(columnHead)) {
						Item copy = columnHead.copy();
						copy.setID(columnHead.getID());
						changedItems.add(copy);
					}
					columnHead.setX(maxX);
				}

				for (Item it : columns.get(i + 1)) {
					if (it.getX() < maxX && it.getY() < maxY) {
    					if(it.getX() != maxX && !changedItems.contains(it)) {
    						Item copy = it.copy();
    						copy.setID(it.getID());
    						changedItems.add(copy);
    					}
						it.setX(maxX);
					}
				}
			}
			
			res = FrameUtils.Align(columns.get(i + 1), true, _adjust, changedItems);
			_success = _success && (res >= 0);
		}

		// align all the separate columns
		/*
		 * for(ArrayList<Item> l : columns){ int res = FrameUtils.Align(l,
		 * true); _success = _success && (res >= 0); }
		 */
		start.setChanged(true);
		start.addToUndoMove(changedItems);
		FrameGraphics.requestRefresh(true);
		return null;
	}

	/**
	 * Finds which column contains the given Item, and returns the index to the
	 * column in the start & end lists. Returns -1 if no column is found that
	 * contains the given Item.
	 * 
	 * @param toFind
	 *            The Item to determine the correct column for
	 * @return The index of the column in the lists, or -1 if no column is found
	 */
	private int findColumn(ArrayList<Item> columnHeads, Item toFind) {
		for (Item top : columnHeads)
			if (FrameUtils.inSameColumn(top, toFind))
				return columnHeads.indexOf(top);

		return -1;
	}

	@Override
	protected void finalise(Frame start) {
		if (_success)
			overwriteMessage("Formatting complete.");
	}

	@Override
	protected void message(String message) {
	}

	@Override
	protected void overwriteMessage(String message) {
	}

	/**
	 * Gets all the items that need to be formatted. If the user clicks in
	 * freeSpace these are all items not enclosed by a rectangle. If the user is
	 * formatting the items in a rectangle this is all the items in the
	 * rectangle.
	 * 
	 * @param start
	 */
	protected Collection<Text> getItemsToFormat(Frame start) {
		Collection<Text> itemsToFormat = FrameUtils.getCurrentTextItems();

		// If the cursor is not inside a box...
		if (itemsToFormat.size() < 1) {
			// Add all the items that are in free space
			itemsToFormat = start.getBodyTextItems(true);
			// Remove all the enclosed items
			Collection<Item> seen = new HashSet<Item>();
			for (Item i : start.getVisibleItems()) {
				if (!seen.contains(i) && i.isEnclosed()) {
					seen.addAll(i.getEnclosingDots());
					itemsToFormat.removeAll(i.getEnclosedItems());
				}
			}
		}
		return itemsToFormat;
	}

}
