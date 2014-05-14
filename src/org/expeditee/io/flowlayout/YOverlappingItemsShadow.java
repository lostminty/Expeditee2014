package org.expeditee.io.flowlayout;

public class YOverlappingItemsShadow extends YOverlappingItemsSpan 
{

	protected YOverlappingItemsTopEdge top_edge;
	
	public YOverlappingItemsShadow(YOverlappingItemsTopEdge top_edge) 
	{
		this.top_edge = top_edge;
	}
	
	public YOverlappingItemsTopEdge getTopEdge() 
	{
		return top_edge;
	}
	
}
