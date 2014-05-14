package org.expeditee.io.flowlayout;

import java.awt.Point;
import java.awt.Polygon;

public class AreaPolygon extends Polygon 
{
	
	private static final long serialVersionUID = 1863373131107377026L;
	
	double area;
	
	public AreaPolygon(Polygon polygon)
	{
		super(polygon.xpoints,polygon.ypoints,polygon.npoints);
		area = calcArea();
	}
	
	protected double calcArea() 
	{
		int n = npoints;
		int[] x_pts = xpoints;
		int[] y_pts = ypoints;
		
		double area = 0;
		
		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			area += x_pts[i] * y_pts[j];
			area -= x_pts[j] * y_pts[i];
		}
		
		area /= 2.0;
		
		return Math.abs(area);
	}

	public double getArea()
	{
		return area;
	}
	
	public boolean completelyContains(Polygon p)
	{
		boolean inside = true;
		
		int p_n = p.npoints;
		int[] p_x = p.xpoints;
		int[] p_y = p.ypoints;
		
		for (int i=0; i<p_n; i++) {
			if (!contains(p_x[i],p_y[i])) {
				inside = false;
				break;
			}
		}
		
		return inside;
	}
	
	public boolean isPerimieterPoint(Point pt)
	{	
		int n = npoints;
		int[] poly_x_pts = xpoints;
		int[] poly_y_pts = ypoints;
		
		boolean isVertex = false;
		
		
		for (int i = 0; i < n; i++) {
			if ((poly_x_pts[i]==pt.x) && (poly_y_pts[i]==pt.y)) {
				isVertex = true;
				break;
			}
		}
		
		return isVertex;
	}
	
}
