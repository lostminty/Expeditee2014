package org.expeditee.io.flowlayout;

import org.expeditee.items.Item;
import org.expeditee.items.Text;

public class XRawItem extends XItem 
{
	protected Item item;
	
	public XRawItem(Item item) 
	{
		this.item = item;
		
		if (item instanceof Text) {
			// Expecting it to be a text item
			// For it's bounding rectangle, go with the tighter 'PixelBounds' version
			
			Text text_item = (Text)item;
			bounding_rect = text_item.getPixelBoundsUnion();
		}
		else {
			// Just in case it isn't a text item
			bounding_rect = item.getArea().getBounds();
		}
		
	}

	public Item getItem()
	{
		return item;
	}
	
}
