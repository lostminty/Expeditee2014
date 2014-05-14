/**
 * 
 */
package org.expeditee.items;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.LinkedList;

/**
 * @author root
 * 
 */
public class Circle extends XRayable {

	private Item _center;

	private Line _line;

	/**
	 * Construct a circle
	 * 
	 * @param _source
	 */
	public Circle(Text source) {
		super(source);
		// Collection<Item> connected = source.getAllConnected();
		// assert (connected.size() == 4);
		_line = source.getLines().get(0);
		_center = _line.getOppositeEnd(_source);
		_center.addEnclosure(this);
		_line.setHidden(true);
		updatePolygon();
	}

	public Collection<Item> getItemsToSave() {
		Collection<Item> toSave = super.getItemsToSave();
		toSave.add(_center);
		return toSave;
	}

	@Override
	public Collection<Item> getConnected() {
		Collection<Item> conn = super.getConnected();
		conn.add(_center);
		conn.add(_line);
		return conn;
	}

	@Override
	public void addAllConnected(Collection<Item> connected) {
		super.addAllConnected(connected);
		if (!connected.contains(_center)) {
			connected.add(_center);
			connected.add(_line);
		}
	}

	@Override
	public boolean isFloating() {
		return _center.isFloating() || super.isFloating();
	}

	@Override
	public boolean isEnclosed() {
		return true;
	}

	@Override
	public Polygon getEnclosedShape() {
		// assert(_poly != null);
		// Ensure that vector items will gradient fill are painted OK!!
		if (_poly == null) {
			updatePolygon();
		}
		return _poly;
	}

	@Override
	public double getEnclosedArea() {
		double radius = getRadius();
		return Math.PI * radius * radius;
	}

	@Override
	public Collection<Item> getEnclosingDots() {
		Collection<Item> enclosed = new LinkedList<Item>();
		enclosed.add(this);
		enclosed.add(_center);
		return enclosed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.expeditee.items.Item#copy()
	 */
	@Override
	public Item copy() {
		Collection<Item> toCopy = new LinkedList<Item>();
		toCopy.add(_source);
		toCopy.add(_line);
		toCopy.add(_center);

		Collection<Item> newItems = ItemUtils.CopyItems(toCopy);
		assert (newItems.size() == 3);
		// find the Source item from the three items
		Text newSource = null;
		for (Item i : newItems) {
			if (i instanceof Text) {
				newSource = (Text) i;
				if (ItemUtils.startsWithTag(i, "@c"))
					break;
			}
		}
		assert (newSource != null);
		Circle newCircle = new Circle(newSource);
		Item.DuplicateItem(this, newCircle);
		newCircle._line.setVisible(_line.isVisible());
		newCircle._source.setVisible(_source.isVisible());
		newCircle.updatePolygon();
		return newCircle;
	}

	/**
	 * Gets the radius of this circle.
	 * 
	 * @return the radius of the cicle
	 */
	public double getRadius() {
		return _line.getLength();
	}

	@Override
	public boolean contains(int x, int y) {
		double radius = getRadius();

		double distance = Math.sqrt(Math.pow(Math.abs(_center.getX() - x), 2)
				+ Math.pow(Math.abs(_center.getY() - y), 2));

		return Math.abs(distance - radius) < getGravity() * 2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.expeditee.items.Item#paint(java.awt.Graphics2D)
	 */
	@Override
	public void paint(Graphics2D g) {
		int radius = (int) Math.round(getRadius());
		int diameter = radius * 2;
		Color fillColor = getFillColor();
		if (fillColor != null) {
			setFillPaint(g);
			g.fillOval(_center.getX() - radius, _center.getY() - radius,
					diameter, diameter);
		}
		if (getThickness() > 0 || fillColor == null) {
			Color lineColor = getPaintColor();
			g.setColor(lineColor);
			g.setStroke(_line.getStroke());
			g.drawOval(_center.getX() - radius, _center.getY() - radius,
					diameter, diameter);
		}
		// Arc version, same result but allows for portions of the circle to be
		// drawn
		// g.drawArc(end.getX() - (distance / 2), end.getY() - (distance / 2),
		// distance, distance, 0, 360);

		if (isHighlighted()) {
			// Flag the background color of the circle so that the item will be
			// drawn with alternate color if the background is the same as
			// the highlight}
			_center.paint(g);
			Color highlightColor = getHighlightColor();
			g.setColor(highlightColor);
			g.setStroke(HIGHLIGHT_STROKE);
			g.drawOval(_center.getX() - radius, _center.getY() - radius,
					diameter, diameter);
		}
	}

	@Override
	public void setHighlightMode(HighlightMode mode, Color color) {
		_center.setHighlightMode(mode, color);
		super.setHighlightMode(mode, color);
	}

	@Override
	public int setHighlightColor(Color c) {
		_center.setHighlightColor(c);
		return super.setHighlightColor(c);
	}

	@Override
	public void setFillColor(Color c) {
		super.setFillColor(c);
		_center.setColor(c);
		invalidateCommonTrait(ItemAppearence.FillColor);
	}

	@Override
	public void setGradientColor(Color c) {
		super.setGradientColor(c);
		invalidateCommonTrait(ItemAppearence.GradientColor);
	}

	// @Override
	// public void setPosition(float x, float y) {
	// // float deltaX = x - _source._x;
	// // float deltaY = y - _source._y;
	// // _center.setPosition(_center._x + deltaX, _center._y + deltaY);
	// _source.setPosition(x, y);
	//
	// updatePolygon();
	// }

	// TODO use an algorithm to get more precicely for contains and intersects

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.expeditee.items.Item#setAnnotation(boolean)
	 */
	@Override
	public void setAnnotation(boolean val) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.expeditee.items.Item#updatePolygon()
	 */
	@Override
	public void updatePolygon() {
		double radius = getRadius();
		// Approximation of circle for mouse interaction
		int points = 20;

		double radians = 0.0;
		int xPoints[] = new int[points];
		int yPoints[] = new int[xPoints.length];

		for (int i = 0; i < xPoints.length; i++) {
			xPoints[i] = (int) Math.round(radius * Math.cos(radians));
			yPoints[i] = (int) Math.round(radius * Math.sin(radians));
			radians += (2.0 * Math.PI) / xPoints.length;
		}

		_poly = new Polygon(xPoints, yPoints, xPoints.length);
		_poly.translate(_center.getX(), _center.getY());
		return;
	}

	@Override
	public float getSize() {
		return (float) getRadius();
	}

	/**
	 * Resizes the circle from the center.
	 */
	@Override
	public void setSize(float size) {
		double ratio = size / getRadius();
		
		// Extend the line along the same plane as the underlying line
		_source.translate(_center.getPosition(), ratio);

		updatePolygon();
	}

	@Override
	public void setLinePattern(int[] pattern) {
		_line.setLinePattern(pattern);
	}

	@Override
	public int[] getLinePattern() {
		return _line.getLinePattern();
	}

	@Override
	public void translate(Point2D origin, double ratio) {
		updatePolygon();
		// _center.translate(origin, ratio);
		// super.translate(origin, ratio);
	}

	@Override
	public Rectangle[] getDrawingArea() {

		float thickness = getThickness();
		double radius = getRadius();

		int size = (int) ((2 * radius) + 3.0 + thickness);

		return new Rectangle[] { new Rectangle((int) (_center.getX() - radius
				- 0.5 - (thickness / 2.0)), (int) (_center.getY() - radius
				- 0.5 - (thickness / 2.0)), size, size) };
	}

	// @Override
	// public void setPosition(float x, float y){
	// float deltaX = x - _source._x;
	// float deltaY = y = _source._y;
	// _center.setPosition(deltaX, deltaY);
	// super.setPosition(x,y);
	// }
	public Item getCenter() {
		return _center;
	}

	@Override
	public void setPermission(PermissionPair permissionPair) {
		super.setPermission(permissionPair);
		_center.setPermission(permissionPair);
		_line.setPermission(permissionPair);
	}
	
	@Override
	public void scale(Float scale, int originX, int originY) {
		getCenter().scale(scale, originX, originY);
		super.scale(scale, originX, originY);
	}
	
	@Override
	public void setThickness(float thick, boolean setConnected) {
		super.setThickness(thick, setConnected);
		_line.refreshStroke(thick);
	}
	
	@Override
	public boolean isConnectedToAnnotation() {
		return false;
	}
}
