package org.expeditee.io.flowlayout;

public class YOverlappingItemsTopEdge extends YOverlappingItemsSpan 
{

	protected XOrderedLine x_items;
	
	public YOverlappingItemsTopEdge(XOrderedLine x_items) 
	{
		this.x_items = x_items;
	}
	
	public XOrderedLine getXOrderedLine() 
	{
		return x_items;
	}
	
}
