package org.expeditee.gui;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Text;

public class FreeItems extends ArrayList<Item> {

	private static final long serialVersionUID = 1L;

	private static FreeItems _instance = new FreeItems();
	
	private static FreeItems _cursor = new FreeItems();

	private FreeItems() {
	}

	public static FreeItems getCursor() {
		return _cursor;
	}
	
	public static FreeItems getInstance() {
		return _instance;
	}

	@Override
	public void clear() {
		for (Item i : this) {
			i.invalidateAll();
			i.invalidateFill();
		}
		super.clear();
	}

	@Override
	public Item remove(int index) {
		Item i = get(index);
		remove(i);
		return i;
	}

	@Override
	public boolean remove(Object o) {
		if (o instanceof Item) {
			((Item) o).invalidateAll();
			((Item) o).invalidateFill();
		}
		return super.remove(o);
	}

	/**
	 * Return true if there is at least one item attached to the end of the
	 * cursor.
	 * 
	 * @return true if at least one item is attached to the cursor.
	 */
	public static boolean itemsAttachedToCursor() {
		return getInstance().size() > 0;
	}

	/**
	 * Checks if only text items are attached to the cursor.
	 * 
	 * @return true if at least one item is attached to the cursor and all items
	 *         attached are text items.
	 */
	public static boolean textOnlyAttachedToCursor() {
		if (!itemsAttachedToCursor())
			return false;
		for (Item i : getInstance()) {
			if (!(i instanceof Text)) {
				return false;
			}
		}
		return true;
	}

	public static Item getItemAttachedToCursor() {
		if (itemsAttachedToCursor())
			return getInstance().get(0);
		return null;
	}

	public static Text getTextAttachedToCursor() {
		if (textOnlyAttachedToCursor())
			return (Text) getInstance().get(0);
		return null;
	}

	/**
	 * Creates a list of the free text items.
	 * @return the list of free text items
	 */
	public static Collection<Text> getTextItems() {
		Collection<Text> textItems = new LinkedList<Text>();
		for (Item i : getInstance()) {
			if (i instanceof Text) {
				textItems.add((Text) i);
			}
		}

		return textItems;
	}

	public static Map<String, Collection<String>> getGroupedText() {
		Map<String, Collection<String>> groupedText = new HashMap<String, Collection<String>>();
		// Go throught the lineEnds
		Collection<Item> addedItems = new HashSet<Item>();
		for (Item i : getInstance()) {
			if (!(i instanceof Text) || !i.isLineEnd()) {
				continue;
			}
			// Check for text inside the box
			Collection<String> textList = new LinkedList<String>();
			for (Text enclosed : getInstance().getTextWithin(i)) {
				textList.add(enclosed.getText());
				addedItems.add(enclosed);
			}
			if (textList.size() > 0) {
				groupedText.put(i.getText(), textList);
			}
		}
		// Now add the items that were not contained in any of the boxes
		Collection<String> outsideList = new LinkedList<String>();
		for (Item i : getInstance()) {
			if (i instanceof Text && !i.isLineEnd() && !addedItems.contains(i)) {
				outsideList.add(i.getText());
			}
		}
		groupedText.put("", outsideList);

		return groupedText;
	}

	/**
	 * Gets a list of non-line-end text items within a specified rectangle in
	 * the free items list.
	 * 
	 * @param lineEnd
	 * @return
	 */
	private Collection<Text> getTextWithin(Item lineEnd) {
		Polygon poly = lineEnd.getEnclosedShape();
		Collection<Text> results = new LinkedHashSet<Text>();
		for (Item i : this) {
			if (i.intersects(poly) && i instanceof Text && !i.isLineEnd()) {
				results.add((Text) i);
			}
		}
		return results;
	}

	public static boolean hasCursor() {
		return getCursor().size() > 0;
	}

	public static void setCursor(Collection<Item> cursor) {
		_cursor.clear();
		_cursor.addAll(cursor);
	}
	
	public static boolean hasMultipleVisibleItems() {
		List<Item> toCount = new LinkedList<Item>(getInstance());
		int c = 0;
		while(!toCount.isEmpty()) {
			Item i = toCount.remove(0);
			if(i.isVisible()) {
				toCount.removeAll(i.getAllConnected()); // treat polygons as a single item
				if(c++ > 0) return true;
			}
		}
		return false;
	}
	
	public static boolean isDrawingPolyLine() {
		List<Item> tmp = getInstance();
		return tmp.size() == 2 && tmp.get(0) instanceof Dot && tmp.get(1) instanceof Line;
	}
}
