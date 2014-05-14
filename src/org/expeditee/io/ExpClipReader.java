package org.expeditee.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.items.Circle;
import org.expeditee.items.Constraint;
import org.expeditee.items.Dot;
import org.expeditee.items.FrameBitmap;
import org.expeditee.items.FrameImage;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Line;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;

/**
 * Takes a String containing data produced by ExpClipWriter, 
 * and generates a list of Items
 * 
 * Most of this code is copy+pasted and modified from ExpReader, since it's too different to just subclass
 *
 */
public class ExpClipReader extends ExpReader {

	private HashMap<Integer, Item> _linePoints;
	private ArrayList<Item> _items;
	private int dX, dY;
	
	public ExpClipReader(int dX, int dY) {
		super("");
		this.dX = dX;
		this.dY = dY;
	}
	
	/**
	 * Makes images, circles and widgets display without having to toggle xray mode
	 * 
	 */
	public static void updateItems(List<Item> items) {
		if(FrameGraphics.isXRayMode()) {
			return;
		}
		// make an array to iterate over instead of the list so we don't get stuck when we remove items from the list
		Item[] tmpitems = items.toArray(new Item[0]);
		for(Item item : tmpitems) {
			if(!(item instanceof Text)) {
				continue;
			}
			if (ItemUtils.startsWithTag(item,
					ItemUtils.TAG_IMAGE, true)) {
				if (!item.hasEnclosures()) {
					items.add(ItemUtils.CreatePicture((Text) item, DisplayIO.getCurrentFrame()));
				}
				// check for frame images
			} else if (ItemUtils.startsWithTag(item,
					ItemUtils.TAG_FRAME_IMAGE)
					&& item.getLink() != null
					&& !item.getAbsoluteLink()
							.equalsIgnoreCase(
									DisplayIO.getCurrentFrame().getName())) {
				if (item.hasEnclosures()) {
					// item.setHidden(true);
					// image =
					// item.getEnclosures().iterator().next();
					// image.refresh();
				} else {
					items.add(new FrameImage((Text) item, DisplayIO.getCurrentFrame(), null));
				}
			} else if (ItemUtils.startsWithTag(item,
					ItemUtils.TAG_BITMAP_IMAGE)
					&& item.getLink() != null
					&& !item.getAbsoluteLink()
							.equalsIgnoreCase(
									DisplayIO.getCurrentFrame().getName())) {
				if (item.hasEnclosures()) {
					// image =
					// item.getEnclosures().iterator().next();
					// image.refresh();
					// item.setHidden(true);
				} else {
					// If a new bitmap is created for a
					// frame which already has a bitmap dont
					// recreate the bitmap
					items.add(new FrameBitmap((Text) item, DisplayIO.getCurrentFrame(), null));
				}
			} else if (ItemUtils.startsWithTag(item, "@c")) {
				// Can only have a @c
				if (!item.hasEnclosures()
						&& item.getLines().size() == 1) {
					items.add(new Circle((Text) item));
				}
				// Check for interactive widgets
			} else if (ItemUtils.startsWithTag(item,
					ItemUtils.TAG_IWIDGET)) {
				items.remove(item);
				item.setParent(DisplayIO.getCurrentFrame());
				try {
					items.addAll(InteractiveWidget.createWidget((Text) item).getItems());
				} catch (Exception e) {
					System.err.println("Failed to create widget");
					e.printStackTrace();
				}
			} else {
				// System.out.println("None of the above");
			}
		}
	}
	
	public ArrayList<Item> read(String data) {
		
		_linePoints = new HashMap<Integer, Item>();
		_items = new ArrayList<Item>();
		
		String[] lines = data.split("\n");
		int index = 0;
		
		try {
			// Read all the items
			Item currentItem = null;
			while(index < lines.length && !(lines[index].equals("Z"))) {
				// if this is the start of a new item add a new item
				if (isValidLine(lines[index])) {
					if (getTag(lines[index]) == 'S') {
						if(currentItem != null) {
							currentItem.setPosition(currentItem.getX() + dX, currentItem.getY() + dY);
						}
						String value = getValue(lines[index]);
						int id = Integer.parseInt(value.substring(2));
	
						switch (value.charAt(0)) {
						case 'P': // check if its a point
							currentItem = new Dot(id);
							break;
						default:
							currentItem = new Text(id);
							break;
						}
						_linePoints.put(currentItem.getID(), currentItem);
						_items.add(currentItem);
					} else if (currentItem != null) {
						processBodyLine(currentItem, lines[index]);
					}
				}
				++index;
			}
			if(currentItem != null) {
				currentItem.setPosition(currentItem.getX() + dX, currentItem.getY() + dY);
			}
	
			// Read the lines
			while(index < lines.length && !(lines[++index].equals("Z"))) {
				if (isValidLine(lines[index])) {
					java.awt.Point idtype = separateValues(lines[index].substring(2));
					// The next line must be the endpoints
					if (index >= lines.length)
						throw new Exception("Unexpected end of file");
					++index;
					java.awt.Point startend = separateValues(lines[index].substring(2));
					int start = startend.x;
					int end = startend.y;
	
					if (_linePoints.get(start) != null
							&& _linePoints.get(end) != null) {
						_items.add(new Line(_linePoints.get(start),
								_linePoints.get(end), idtype.x));
					} else {
						System.out
								.println("Error reading line with unknown end points");
					}
				}
			}
	
			// Read the constraints
			while(index < lines.length && !(lines[++index].equals("Z"))) {
				if (isValidLine(lines[index])) {
					java.awt.Point idtype = separateValues(lines[index].substring(2));
					// The next line must be the endpoints
					if (index >= lines.length)
						throw new Exception("Unexpected end of file");
					++index;
					java.awt.Point startend = separateValues(lines[index].substring(2));
	
					Item a = _linePoints.get(startend.x);
					Item b = _linePoints.get(startend.y);
					
					// System.out.println("adding  constraint" + a + " " + b + " " + idtype.x + " " + idtype.y);
	
					new Constraint(a, b, idtype.x, idtype.y);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error reading expeditee data line: " + lines[index] + " "
					+ e.getMessage());
		}
		
		updateItems(_items);
		return _items;
	}

	@Override
	public Frame readFrame(BufferedReader frameContents) throws IOException {
		// Does nothing, just stops ExpReader's readFrame from running
		return null;
	}
}
