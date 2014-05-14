package org.expeditee.io.flowlayout;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.List;

import org.expeditee.items.Item;

public class DimensionExtent 
{
	public int min;
	public int max;
	
	public DimensionExtent(int min, int max)
	{
		this.min = min;
		this.max = max;
	}
	
	public static DimensionExtent calcMinMaxXExtent(List<Item>items)
	{

		int min_x = Integer.MAX_VALUE;
		int max_x = Integer.MIN_VALUE;
		
		for (Item item : items) {
			Area area = item.getArea();
			Rectangle rect = area.getBounds();
			int xl = rect.x;
			int xr = xl + rect.width-1;
			
			min_x = Math.min(min_x, xl);
			max_x = Math.max(max_x, xr);
			
		}
		
		DimensionExtent extent = new DimensionExtent(min_x,max_x);
		
		return extent;
	}
	
	public static DimensionExtent calcMinMaxYExtent(List<Item>items)
	{

		int min_y = Integer.MAX_VALUE;
		int max_y = Integer.MIN_VALUE;
		
		for (Item item : items) {
			Area area = item.getArea();
			Rectangle rect = area.getBounds();
			int yt = rect.y;
			int yb = yt + rect.height-1;
			
			min_y = Math.min(min_y, yt);
			max_y = Math.max(max_y, yb);
			
		}
		
		DimensionExtent extent = new DimensionExtent(min_y,max_y);
		
		return extent;
	}

	
	public static DimensionExtent calcMinMaxXExtent(Polygon polygon)
	{

		int min_x = Integer.MAX_VALUE;
		int max_x = Integer.MIN_VALUE;
		
		int npoints = polygon.npoints;

		for (int i=0; i<npoints; i++) {

			int x = polygon.xpoints[i];

			min_x = Math.min(min_x, x);
			max_x = Math.max(max_x, x);
		}
		
		DimensionExtent extent = new DimensionExtent(min_x,max_x);
		
		return extent;
	}
	
	public static DimensionExtent calcMinMaxYExtent(Polygon polygon)
	{

		int min_y = Integer.MAX_VALUE;
		int max_y = Integer.MIN_VALUE;
		
		int npoints = polygon.npoints;
		
		for (int i=0; i<npoints; i++) {

			int y = polygon.ypoints[i];
				
			min_y = Math.min(min_y, y);
			max_y = Math.max(max_y, y);
		}
		
		DimensionExtent extent = new DimensionExtent(min_y,max_y);
		
		return extent;
	}
	
	public static Polygon boundingBoxPolygon(List<Item> items)
	{
		DimensionExtent xextent = DimensionExtent.calcMinMaxXExtent(items);
		DimensionExtent yextent = DimensionExtent.calcMinMaxYExtent(items);
	
		int xl=xextent.min;
		int xr=xextent.max;
		int yt=yextent.min;
		int yb=yextent.max;
		
		int[] xpoints = new int[]{xl,xr,xr,xl};
		int[] ypoints = new int[]{yt,yt,yb,yb};
		
		
		Polygon polygon = new Polygon(xpoints,ypoints,4);
		
		return polygon;
	}
}
