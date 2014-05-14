package org.expeditee.io.flowlayout;

import java.awt.Point;
import java.awt.Rectangle;

import org.expeditee.items.Item;

public abstract class XItem 
{
	protected Rectangle bounding_rect;
		
	protected XItem()
	{
		bounding_rect = null;
	}
	
	protected XItem(Rectangle imprint_bounding_rect)
	{
		this.bounding_rect = imprint_bounding_rect;
	}
	
	public int getX()
	{
		return bounding_rect.x;
	}
	
	public int getBoundingXLeft()
	{
		// name alias for getX()
		return bounding_rect.x;
	}
	
	public int getY()
	{
		return bounding_rect.y;
	}
	
	public int getBoundingYTop()
	{
		// name alias for getY()
		return bounding_rect.y;
	}
	
	public int getBoundingYBot()
	{
		return bounding_rect.y + bounding_rect.height -1;
	}
	
	public int getBoundingWidth()
	{
		return bounding_rect.width;
	}
	
	public int getBoundingHeight()
	{
		return bounding_rect.height;
	}
	
	public Rectangle getBoundingRect()
	{
		return bounding_rect;
	}
	
	public Rectangle setBoundingRect(Rectangle rect)
	{
		return bounding_rect = rect;
	}
	
	public boolean containsItem(Item item)
	{
		int x = item.getX();
		int y = item.getY();
		
		Point pt = new Point(x,y);
		
		return bounding_rect.contains(pt);
	}
}
