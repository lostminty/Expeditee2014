package org.expeditee.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

/**
 * A simple formatting agent that aligns the Y values.
 * 
 * @author Mike
 * 
 */
public class HFormat extends Format {

	public HFormat() {
		super();
	}

	@Override
	public Frame process(Frame start) {
		Collection<Text> itemsToFormat = getItemsToFormat(start);
		
		if(itemsToFormat.size() == 0)
			return null;
		
		List<Item> changedItems = new ArrayList<Item>();
		
		float anchorY = 0F;
		float yThreshold = 0F;
		
		for(Text t: itemsToFormat){
			if(t.getY() > yThreshold + anchorY){
				anchorY = t.getY();
				yThreshold = t.getSize();
			}else{
				if(t.getY() != anchorY && !changedItems.contains(t)) {
					Item copy = t.copy();
					copy.setID(t.getID());
					changedItems.add(copy);
				}
				t.setY(anchorY);
			}
		}
		
		System.out.println(changedItems);
		start.addToUndoMove(changedItems);
		return null;
	}
}
