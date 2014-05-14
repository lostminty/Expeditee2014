package org.expeditee.io.flowlayout;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class XOrderedLine 
{		
		ArrayList<XItem> overlapping;
		
		public XOrderedLine()
		{
			overlapping = new ArrayList<XItem>();
			
		}
		
		public XOrderedLine(XItem item)
		{
			this();
			overlapping.add(item);	
		}
		
		public boolean isEmpty() 
		{
	
			return overlapping.size()==0;
		}
		
		public List<XItem> getXItemList() 
		{
			return overlapping;
		}
		
		
	
		
		public void orderedMergeItem(int new_xl, XItem new_item)
		{
			// More generate case for insert, when we *don't* want to
			// insert based on the new_item's 'xl' position
			
			boolean added_item = false;
			
			for (int i=0; i<overlapping.size(); i++) {
				XItem existing_item = overlapping.get(i);
				
				
				int existing_xl = existing_item.getX();
				if (new_xl<existing_xl) {
					overlapping.add(i,new_item);
					added_item = true;
					break;
				}
				
			}
			
			if (!added_item) {
				
				// add to the end of the list
				overlapping.add(new_item);
			}
		}
		
		public void orderedMergeItem(XItem new_item)
		{
			// Simple case => want to insert based on the new_item's xl position
			Rectangle rect = new_item.getBoundingRect();
			int xl = rect.x;
			orderedMergeItem(xl,new_item);
		}
}
