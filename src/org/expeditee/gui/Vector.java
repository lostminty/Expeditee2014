package org.expeditee.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.text.NumberFormat;

import org.expeditee.items.Item;
import org.expeditee.items.UserAppliedPermission;

public class Vector extends Overlay {

	public Point Origin;

	public float Scale;

	public Color Foreground;

	public Color Background;
	
	public Item Source;
	
	public Dimension Size;

	public Vector(Frame overlay, UserAppliedPermission permission,
			Float scale, Item source) {
		super(overlay, permission);
		Origin = source.getPosition();
		Scale = scale;
		Foreground = source.getColor();
		Background = source.getBackgroundColor();
		Source = source;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != Vector.class)
			return false;
		Vector v = (Vector) o;

		return v.Frame == Frame && Origin.equals(v.Origin)
				&& Scale == v.Scale && Foreground == v.Foreground
				&& Background == v.Background;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	/**
	 * Converts the given x position to be relative to the overlay frame.
	 * 
	 * @param x
	 * @return
	 */
	public float getX(int x) {
		return (x - Origin.x) / Scale;
	}

	public float getY(int y) {
		return (y - Origin.y) / Scale;
	}

	public void setSize(int maxX, int maxY) {
		Size = new Dimension(maxX, maxY);
	}

	public static NumberFormat getNumberFormatter() {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(4);
		return nf;
	}
}
