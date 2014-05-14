package org.expeditee.io;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.XRayable;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;

/**
 * Subclass of ExpWriter that takes a list of items and writes it to a String
 * 	i.e. the changes are: - doesn't write frame header/data
 * 	                      - doesn't write to a file
 * 
 */
public class ExpClipWriter extends ExpWriter {
	
	private int dX, dY;

	public ExpClipWriter(int dX, int dY) {
		super();
		this.dX = dX;
		this.dY = dY;
		_stringWriter = new StringBuilder();
	}
	
	// writes the line to the stringbuilder
	@Override
	protected void writeLine(String line) throws IOException {
		// do not write empty lines
		if (line == null)
			return;
		
		_stringWriter.append(line + "\n");
	}
	
	public void output(List<Item> items) throws IOException {
		// switch to savable items
		LinkedList<InteractiveWidget> widgets = new LinkedList<InteractiveWidget>();
		// make an array to iterate over instead of the list so we don't get stuck when we remove items from the list
		Item[] tmpitems = items.toArray(new Item[0]);
		for(Item i : tmpitems) {
			if (i instanceof XRayable) {
				items.remove(i);
				// Show the items
				for (Item item : ((XRayable) i).getConnected()) {
					item.setVisible(true);
					item.removeEnclosure(i);
				}
			} else if (i instanceof WidgetCorner) {
				InteractiveWidget iw = ((WidgetCorner)i).getWidgetSource();
				if(!widgets.contains(iw)) {
					widgets.add(iw);
				}
				items.remove(i);
			} else if (i instanceof WidgetEdge) {
				InteractiveWidget iw = ((WidgetEdge)i).getWidgetSource();
				if(!widgets.contains(iw)) {
					widgets.add(iw);
				}
				items.remove(i);
			} else if (i.hasFormula()) {
				i.setText(i.getFormula());
			} else if (i.hasOverlay()) {
				i.setVisible(true);
				// int x = i.getBoundsHeight();
			}
		}
		for (InteractiveWidget iw : widgets) {
			items.add(iw.getSource());
		}
		widgets.clear();
		// write each item in the frame
		for (Item i : items) {
			assert (!(i instanceof Line));
			i.setPosition(i.getX() - dX, i.getY() - dY);
			writeItemAlways(i);
		}

		// write any lines or constraints
		writeTerminator();
		writeLineData();
		writeTerminator();
		writeConstraintData();
		writeTerminator();
	}
	
	@Override
	public void outputFrame(Frame frame) throws IOException {
		// Does nothing, just stops ExpWriter's outputFrame from running
	}
}
