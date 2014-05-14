package org.expeditee.items;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;

/**
 * Implements a line that is drawn on the screen. A line is represented as two
 * Items that share a common Line ID.
 * 
 * @author jdm18
 * 
 */
public class Line extends Item {

	private static final int AUTO_ARROWHEAD_LENGTH      = -1;

	private static final int ARROW_HEAD_TO_LENGTH_RATIO = 8;

	private static final int MINIMUM_ARROW_HEAD_LENGTH  = 8;

	private static final int MAXIMUM_ARROW_HEAD_LENGTH  = 50;

	
	public static Polygon createArrowheadPolygon(double x0, double y0, float arrowLength0, double arrowRatio0, double arrowNibPerc0) 
	{
		
		Polygon arrowhead = new Polygon();
	    
		int ix0 = (int)Math.round(x0);
		int iy0 = (int)Math.round(y0);
		
		int flange_ix_project = (int) Math.round(x0 - arrowLength0);
		int ix_nib = (int)Math.round(x0 - (arrowLength0*arrowNibPerc0));
		
		double flange_y_width = (arrowLength0 * arrowRatio0);
		int flange_iy_left = (int) Math.round((y0 - flange_y_width));
		int flange_iy_right = (int) Math.round((y0 + flange_y_width));
				
		
		arrowhead.addPoint(ix0, iy0);
		arrowhead.addPoint(flange_ix_project, flange_iy_left);
		
		arrowhead.addPoint(ix_nib, iy0);
		
		arrowhead.addPoint(flange_ix_project, flange_iy_right);
		
		return arrowhead;
	}

	private Item _start;

	private Item _end;

	// old point locations, used for clearing
	private Point _startOffset = new Point(0, 0);

	private Point _endOffset = new Point(0, 0);

	// brush strokes used for painting this line and highlighting
	private Stroke _lineStroke = new BasicStroke(DEFAULT_THICKNESS, CAP, JOIN, 4.0F);

	Stroke getStroke() {
		return _lineStroke;
	}

	/**
	 * Constructs a new Line with the given start and end point and Item ID.
	 * 
	 * @param start
	 *            The starting Point of this Line.
	 * @param end
	 *            The end Point of this Line.
	 * @param id
	 *            The Item ID of this Line.
	 */
	public Line(Item start, Item end, int id) {
		super();
		setID(id);
		_start = start;
		_end = end;

		// update the two end points to add this line
		_start.addLine(this);
		_end.addLine(this);

		// initialise the line size
		float thick = (_start.getThickness() + _end.getThickness()) / 2.0f;

		if (thick < 0)
			setThickness(DEFAULT_THICKNESS);
		else {
			refreshStroke(thick);
		}

	}

	public void refreshStroke(float thick) {
		thick = Math.round(thick);

		int[] pattern = _start.getLinePattern();
		if (pattern == null)
			pattern = _end.getLinePattern();

		if (pattern != null) {
			float[] dash = new float[pattern.length];
			for (int i = 0; i < pattern.length; i++)
				dash[i] = (float) pattern[i];
			_lineStroke = new BasicStroke(Math.max(thick,
					MINIMUM_PAINT_THICKNESS), CAP, JOIN, 10f, dash, 0.0f);
		} else {
			_lineStroke = new BasicStroke(Math.max(thick,
					MINIMUM_PAINT_THICKNESS), CAP, JOIN);
		}

		updatePolygon();
	}

	/**
	 * Returns the Dot at the start of this Line.
	 * 
	 * @return The Dot that is the start of this line.
	 */
	public Item getStartItem() {
		return _start;
	}

	/**
	 * Returns the Dot that is the end of this Line.
	 * 
	 * @return The Dot that is the end of this line.
	 */
	public Item getEndItem() {
		return _end;
	}

	/**
	 * Returns the Dot at the opposite end to the Dot given. If the given Dot is
	 * not part of this line, null is returned.
	 * 
	 * @param fromThis
	 *            The Dot at the opposite end than this Dot is returned
	 * @return The Dot at the opposite end of this Line to the given Dot, or
	 *         Null if the given Dot is not on this Line.
	 */
	public Item getOppositeEnd(Item fromThis) {
		if (_start == fromThis)
			return _end;

		if (_end == fromThis)
			return _start;

		return null;
	}

	/**
	 * Replaces either the start or end point of this Line with the given
	 * LineEnd. This is used when merging LineEnds. It will also add and remove
	 * the line from the LineEnds as well as adjust constraints.
	 * 
	 * @param replace
	 *            Either the start or end Dot of this Line indicating which is
	 *            to be replaced.
	 * @param with
	 *            The Dot to replace the start or end Dot with.
	 */
	public void replaceLineEnd(Item replace, Item with) {
		Item otherEnd = null;
		if (_start == replace) {
			setStartItem(with);
			otherEnd = _end;
		} else if (_end == replace) {
			setEndItem(with);
			otherEnd = _start;
		}
		// if no end was replaced
		if (otherEnd == null)
			return;
		// Copy the constraints list so the endPoints list can be modified
		List<Constraint> constraints = new ArrayList<Constraint>(replace
				.getConstraints());
		// Now change all the constraints for this line only
		for (int i = 0; i < constraints.size(); i++) {
			Constraint c = constraints.get(i);
			if (c.contains(otherEnd)) {
				c.replaceEnd(replace, with);
			}
		}
	}

	public void setStartItem(Item start) {
		if (start != null) {
			_start.removeLine(this);

			/*
			 * if (!(start instanceof Dot)) { _startOffset.x = _start.getX() -
			 * start.getX(); _startOffset.y = _start.getY() - start.getY(); }
			 * else
			 */{
				_startOffset.x = 0;
				_startOffset.y = 0;
			}

			_start = start;
			_start.addLine(this);
			updatePolygon();
		}
	}

	public void setEndItem(Item end) {
		if (end != null) {
			_end.removeLine(this);

			/*
			 * if (!(end instanceof Dot)) { _endOffset.x = _end.getX() -
			 * end.getX(); _endOffset.y = _end.getY() - end.getY(); } else
			 */{
				_endOffset.x = 0;
				_endOffset.y = 0;
			}

			_end = end;
			_end.addLine(this);
			updatePolygon();
		}
	}

	/**
	 * Returns the Y value of the start Dot of this Line. To determine the end
	 * Dot Y value use getEndDot().getY()
	 * 
	 * @return The Y value of the starting Dot of this Line.
	 * @see #getStartDot()
	 * @see #getEndDot()
	 */
	public int getY() {
		return _start.getY();
	}

	/**
	 * Returns the X value of the start Dot of this Line. To determine the end
	 * Dot X value use getEndDot().getX()
	 * 
	 * @return The X value of the starting Dot of this Line.
	 * @see #getStartDot()
	 * @see #getEndDot()
	 */
	public int getX() {
		// return 0;
		return _start.getX();
	}

	@Override
	public void setX(float x) {
	}

	@Override
	public void setY(float y) {
	}

	@Override
	public void setPosition(float x, float y) {
	}

	@Override
	public String getLink() {
		return null;
	}

	@Override
	public void setLinePattern(int[] pattern) {
		super.setLinePattern(pattern);

		float thick = Math.round(getThickness());

		if (thick < 0)
			thick = DEFAULT_THICKNESS;

		if (pattern != null) {
			float[] dash = new float[pattern.length];
			for (int i = 0; i < pattern.length; i++)
				dash[i] = (float) pattern[i];
			_lineStroke = new BasicStroke(Math.max(thick, MINIMUM_THICKNESS),
					CAP, JOIN, 10f, dash, 0.0f);
		} else
			_lineStroke = new BasicStroke(Math.max(thick, MINIMUM_THICKNESS),
					CAP, JOIN);

		if (_start.getLinePattern() != pattern)
			_start.setLinePattern(pattern);

		if (_end.getLinePattern() != pattern)
			_end.setLinePattern(pattern);

		invalidateAll();
	}

	@Override
	public void paint(Graphics2D g) {
		if (!isVisible())
			return;

		// Dont paint lines with thickness 0 if they are the border on a filled
		// shape
		if (dontPaint())
			return;

		g.setColor(getPaintColor());

		// if (this._mode == Item.SelectedMode.Disconnect)
		// System.out.println("Disconnect mode!");
		g.setStroke(_lineStroke);
		// Get a path of points
		int[][][] paths = getPaths();
		for (int i = 0; i < paths.length; i++) {
			int[][] path = paths[i];
			int last = path[0].length - 1;
			if (path[0][0] == path[0][last] && path[1][0] == path[1][last]) {
				g.drawPolygon(path[0], path[1], last);
			} else {
				g.drawPolyline(path[0], path[1], last + 1);
			}
		}
		// paint the arrowhead (if necessary)
		paintArrows(g);

		if (showLineHighlight() && isHighlighted()) {
			g.setColor(getHighlightColor());
			g.setStroke(HIGHLIGHT_STROKE);
			((Graphics2D) g).draw(this.getArea());
		}
	}

	protected boolean dontPaint() {
		// enable invisible shapes (for web browser divs) only when XRayMode is off
		return getThickness() <= 0 && _start.isEnclosed()
				&& (_start.getFillColor() != null || !FrameGraphics.isXRayMode());
	}

	protected int[][][] getPaths() {
		List<List<Point>> pointPaths = new LinkedList<List<Point>>();
		Collection<Line> visited = new HashSet<Line>();
		LinkedList<Line> toExplore = new LinkedList<Line>();
		toExplore.add(this);
		while (toExplore.size() > 0) {
			Line nextLine = toExplore.remove(0);
			// Start at the item we have already explored... unless both have
			// been explored
			if (!visited.contains(nextLine)) {
				pointPaths.add(nextLine.getPath(visited, toExplore));
			}
		}
		// Put the paths into int arrays
		int[][][] paths = new int[pointPaths.size()][][];
		Iterator<List<Point>> iter = pointPaths.iterator();

		for (int i = 0; i < paths.length; i++) {
			List<Point> pointPath = iter.next();
			int[][] path = new int[2][pointPath.size()];
			paths[i] = path;
			// Add all the x and y's to the array
			for (int j = 0; j < path[0].length; j++) {
				path[0][j] = pointPath.get(j).x;
				path[1][j] = pointPath.get(j).y;
			}
		}
		return paths;
	}

	protected List<Point> getPath(Collection<Line> visited,
			LinkedList<Line> toExplore) {
		LinkedList<Point> points = new LinkedList<Point>();
		// put the start item points into our list
		Item start = getStartItem();
		Item end = getEndItem();
		visited.add(this);

		start.appendPath(visited, points, true, toExplore);
		end.appendPath(visited, points, false, toExplore);
		return points;
	}

	public void paintArrows(Graphics2D g) {
		if (dontPaint())
			return;

		g.setColor(getPaintColor());
		g.setStroke(new BasicStroke(getPaintThickness(), CAP,
				BasicStroke.JOIN_MITER));
		paintArrow(g, getStartArrow());
		paintArrow(g, getEndArrow());

		if (_virtualSpot != null) {
			_virtualSpot.paint(g);
			invalidateVirtualSpot();
			_virtualSpot = null;
		}
	}

	private float getPaintThickness() {
		float thickness = getThickness();
		if (thickness > 0) {
			return thickness;
		}
		return MINIMUM_PAINT_THICKNESS;
	}

	private void invalidateVirtualSpot() {
		assert (_virtualSpot != null);

		invalidate(_virtualSpot.getDrawingArea());

	}
	
	/**
	 * Gets the arrow head 
	 * @param withArrow
	 * @param startOffset
	 * @param endOffset
	 * @return
	 */
	private Shape getArrow(Item withArrow, Point startOffset, Point endOffset) {
		boolean disconnectMode = withArrow._mode == Item.HighlightMode.Disconnect;
		// only draw an arrowhead if necessary
		if (!(this._mode == Item.HighlightMode.Disconnect && disconnectMode)
				&& (!withArrow.hasVisibleArrow() || withArrow.getLines().size() > 1))
			return null;

		int x0, x1, y0, y1;

		x1 = withArrow.getX() + startOffset.x;
		y1 = withArrow.getY() + startOffset.y;
		x0 = getOppositeEnd(withArrow).getX() + endOffset.x;
		y0 = getOppositeEnd(withArrow).getY() + endOffset.y;

		float arrowLength   = withArrow.getArrowheadLength();
		double arrowRatio   = withArrow.getArrowheadRatio();
		double arrowNibPerc = withArrow.getArrowheadNibPerc();
		
		// set the size of the disconnect indicator arrowhead
		if (this._mode == Item.HighlightMode.Disconnect) {
			arrowLength  = 15;
			arrowRatio   = 0.3;
			arrowNibPerc = 0.5;
		}

		// if the arrowhead is 'auto', then one and only one end must be
		// floating
		if (arrowLength != AUTO_ARROWHEAD_LENGTH && (arrowRatio < 0 || arrowNibPerc < 0)) {
			return null;
		}

		int deltaX = x1 - x0;
		int deltaY = y1 - y0;

		int length = (int) Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

		// The length of the line must at least be as long as the arrow or we
		// wont show the arrow
		if (length <= MINIMUM_ARROW_HEAD_LENGTH)
			return null;
		if (arrowLength == AUTO_ARROWHEAD_LENGTH) {
			arrowLength = getAutoArrowheadLength(length);
			withArrow.setArrowhead(null);
			if (arrowRatio < 0) {
				arrowRatio = DEFAULT_ARROWHEAD_RATIO;
			}
			if (arrowNibPerc < 0) {
				arrowNibPerc = DEFAULT_ARROWHEAD_NIB_PERC;
			}
		}

		// only calculate the arrowhead polygon if necessary
		Polygon arrowhead = withArrow.getArrowhead();
		if (arrowhead == null || disconnectMode) {
			arrowhead = createArrowheadPolygon(x0,y0,arrowLength,arrowRatio,arrowNibPerc); 
			
			if (!disconnectMode)
				withArrow.setArrowhead(arrowhead);
		}
		double rad = calcAngle((float) x0, (float) y0, (float) x1, (float) y1);
		arrowhead.translate((x0 + length) - arrowhead.xpoints[0], y0
				- arrowhead.ypoints[0]);
		AffineTransform tx = AffineTransform.getRotateInstance(rad, x0, y0);
		
        int[] rx = new int[arrowhead.npoints];
        int[] ry = new int[arrowhead.npoints];
        
        for(int i = 0; i < arrowhead.npoints; i++){
            Point2D p = new Point2D.Double(arrowhead.xpoints[i], arrowhead.ypoints[i]);
            tx.transform(p,p);
            rx[i] = (int) p.getX();
            ry[i] = (int) p.getY();
        }
        
        return new Polygon(rx, ry, arrowhead.npoints);
	}
	
	public Polygon getStartArrow() {
		return (Polygon) getArrow(_start, _startOffset, _endOffset);
	}
	
	public Polygon getEndArrow() {
		return (Polygon) getArrow(_end, _endOffset, _startOffset);
	}

	/**
	 * Based on code from DeSL (Arrow2D) http://sourceforge.net/projects/desl/
	 */
	private void paintArrow(Graphics2D g, Shape arrow) {
		
		if(arrow == null) {
			return;
		}
		
		// TODO
		// **** Need to find a more principled (and one-off) way to switch 
		// this on for 'Graphics' variables in Expeditee in general
		
		// Switching on Aliasing on 'g' at this point a stop-gap for now
		// ****
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.draw(arrow);
		g.fill(arrow);
	
	}

	/**
	 * 
	 * @param length
	 *            The length of the line
	 * @return
	 */
	private float getAutoArrowheadLength(float length) {
		float arrowLength;
		arrowLength = length / ARROW_HEAD_TO_LENGTH_RATIO;
		arrowLength = Math.max(MINIMUM_ARROW_HEAD_LENGTH, arrowLength);
		arrowLength = Math.min(MAXIMUM_ARROW_HEAD_LENGTH, arrowLength);
		return arrowLength;
	}

	/**
	 * Calcuates the angle of rotation (in radians) of the arrowhead Based on
	 * code from DeSL (Arrow2D) http://sourceforge.net/projects/desl/
	 */
	private double calcAngle(float x1, float y1, float x2, float y2) {
		double rad = 0.0d;
		float dx = x2 - x1;
		float dy = y2 - y1;

		if (dx == 0.0) {
			if (dy == 0.0) {
				rad = 0.0;
			} else if (dy > 0.0) {
				rad = Math.PI / 2.0;
			} else {
				rad = Math.PI * 3.0 / 2.0;
			}
		} else if (dy == 0.0) {
			if (dx > 0.0) {
				rad = 0.0;
			} else {
				rad = Math.PI;
			}
		} else {
			if (dx < 0.0) {
				rad = Math.atan(dy / dx) + Math.PI;
			} else if (dy < 0.0) {
				rad = Math.atan(dy / dx) + (2 * Math.PI);
			} else {
				rad = Math.atan(dy / dx);
			}
		}

		return rad;
	}

	@Override
	public Line copy() {
		Item s = _start.copy();
		Item e = _end.copy();

		Line temp = new Line(s, e, getID());

		Item.DuplicateItem(this, temp);

		temp.setLinePattern(getLinePattern());
		temp.setThickness(getThickness());

		temp.setArrow(getArrowheadLength(), getArrowheadRatio(), getArrowheadNibPerc());

		return temp;
	}

	public Item getEndPointToDisconnect(int x, int y) {
		if (!hasPermission(UserAppliedPermission.full))
			return null;

		Item start = getStartItem();
		Item end = getEndItem();
		int deltaX = start.getX() - end.getX();
		int deltaY = start.getY() - end.getY();
		int lineLength = (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

		int startX = start.getX() - x;
		int endX = end.getX() - x;
		int startY = start.getY() - y;
		int endY = end.getY() - y;

		int distStart = (int) Math.sqrt(startX * startX + startY * startY);
		int distEnd = (int) Math.sqrt(endX * endX + endY * endY);

		final double DISCONNECT_THRESHHOLD = Math.min(0.2 * lineLength, 25.0);
		final double NORMAL_THRESHHOLD = Math.min(0.1 * lineLength, 13.0);

		if (distStart < NORMAL_THRESHHOLD) {
			start._mode = Item.HighlightMode.Normal;
			return start;
		} else if (distEnd < NORMAL_THRESHHOLD) {
			end._mode = Item.HighlightMode.Normal;
			return end;
		} else if (distStart < DISCONNECT_THRESHHOLD) {
			if (start.getLines().size() > 1
					|| start.getConstraints().size() > 0)
				start._mode = Item.HighlightMode.Disconnect;
			else
				start._mode = Item.HighlightMode.Normal;
			return start;
		} else if (distEnd < DISCONNECT_THRESHHOLD) {
			if (end.getLines().size() > 1 || end.getConstraints().size() > 0)
				end._mode = Item.HighlightMode.Disconnect;
			else
				end._mode = Item.HighlightMode.Normal;
			return end;
		}

		return null;
	}

	@Override
	public int setHighlightColor() {
		super.setHighlightColor();

		return Item.UNCHANGED_CURSOR;
	}

	/**
	 * Sets the Color of this Line and the two Dots at the ends.
	 * 
	 * @param c
	 *            The Color to paint this Line in.
	 */
	@Override
	public void setColor(Color c) {
		super.setColor(c);

		_start.lineColorChanged(c);
		_end.lineColorChanged(c);
	}

	@Override
	public Color getFillColor() {
		return _start.getFillColor();
	}

	@Override
	public void setThickness(float thick, boolean setConnectedThickness) {

		float oldThickness = this.getThickness();
		if (thick == oldThickness)
			return;
		boolean bigger = thick > oldThickness;

		if (!bigger)
			invalidateCommonTrait(ItemAppearence.Thickness);

		if (thick < 0)
			thick = DEFAULT_THICKNESS;

		if (setConnectedThickness) {
			if (_start.getThickness() != thick)
				_start.setThickness(thick);

			if (_end.getThickness() != thick)
				_end.setThickness(thick);
		}

		refreshStroke(thick);

		if (bigger)
			invalidateCommonTrait(ItemAppearence.Thickness);
	}

	@Override
	public final float getThickness() {
		/*
		 * Don't change this method of getting the thickness otherwise setting
		 * thickness for connected lines breaks. The method for setting
		 * thickness of connected lines is inefficient and should be changed at
		 * some stage!
		 */
		return (_start.getThickness() + _end.getThickness()) / 2;
	}

	@Override
	public void setAnnotation(boolean val) {
		if (_end.isAnnotation() != val)
			_end.setAnnotation(val);
		else if (_start instanceof Text && _start.isAnnotation() != val)
			_start.setAnnotation(val);
	}

	@Override
	public boolean isAnnotation() {
		return _start.isAnnotation() || _end.isAnnotation();
	}

	@Override
	public boolean isConnectedToAnnotation() {
		return _start.isConnectedToAnnotation()
				|| _end.isConnectedToAnnotation();
	}

	/**
	 * Fixes the length of the arrowhead on the end of the line.
	 */
	public void fixArrowheadLength() {
		if (_end.getArrowheadLength() == AUTO_ARROWHEAD_LENGTH) {
			fixArrowheadLength(_end);
		}
	}

	public void autoArrowheadLength() {
		_end.setArrowheadLength(AUTO_ARROWHEAD_LENGTH);
	}

	public void fixArrowheadLength(Item arrow) {
		// if the arrow isnt there turn it on!
		int x0, x1, y0, y1;

		x1 = arrow.getX();
		y1 = arrow.getY();
		x0 = getOppositeEnd(arrow).getX();
		y0 = getOppositeEnd(arrow).getY();

		int deltaX = x1 - x0;
		int deltaY = y1 - y0;

		int length = (int) Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
		length /= ARROW_HEAD_TO_LENGTH_RATIO;
		arrow.setArrowheadLength(Math.min(MAXIMUM_ARROW_HEAD_LENGTH, Math.max(
				MINIMUM_ARROW_HEAD_LENGTH, length)));
	}

	/**
	 * This method toggles the arrow on the floating end point of a line. If the
	 * line does not have one floating end and one fixed end this method does
	 * nothing.
	 * 
	 */
	public void toggleArrow() {
		Item arrow;
		if (_start.isFloating() && !_end.isFloating())
			arrow = _start;
		else if (_end.isFloating() && !_start.isFloating())
			arrow = _end;
		else
			return;

		invalidateAll();
		// if there already an arrow, turn it off
		if (arrow.getArrowheadLength() != 0 && arrow.getArrowheadRatio() > 0) {
			arrow.setArrowheadLength(0);
			return;
		}

		fixArrowheadLength(arrow);
		invalidateAll();
	}

	private static double[] ArrowHeadRatios = new double[] { 0.25, 0.5, 1.0 };

	/**
	 * This will toggle the arrow head between the various types of UML arrow
	 * heads.
	 * 
	 * @param amount
	 */
	public void toggleArrowHeadRatio(int amount) {
		Item arrow = null;
		if (_start.isFloating() && !_end.isFloating())
			arrow = _start;
		else if (_end.isFloating() && !_start.isFloating())
			arrow = _end;
		else
			return;

		// find the index of the current line pattern
		double currentRatio = arrow.getArrowheadRatio();

		// Find the current pattern and move to the next pattern in the wheel
		for (int i = 0; i < ArrowHeadRatios.length; i++) {
			if (currentRatio - ArrowHeadRatios[i] < 0.001) {
				i += ArrowHeadRatios.length + amount;
				i %= ArrowHeadRatios.length;
				arrow.setArrowheadRatio(ArrowHeadRatios[i]);
				return;
			}
		}
	}

	private static int[] ArrowHeadLength = new int[] { /* AUTO_ARROWHEAD_LENGTH, */
	0, 20, 40, MAXIMUM_ARROW_HEAD_LENGTH };

	/**
	 * Changes the length of the arrow head. In the future this may toggle the
	 * arrow head between the various types of UML arrow heads.
	 * 
	 * @param amount
	 */
	public void toggleArrowHeadLength(int amount) {
		Item arrow = null;
		if (_start.isFloating() && !_end.isFloating())
			arrow = _start;
		else if (_end.isFloating() && !_start.isFloating())
			arrow = _end;
		else
			return;

		// find the index of the current line pattern
		float currentLength = arrow.getArrowheadLength();

		// Find the current pattern and move to the next pattern in the wheel
		for (int i = 0; i < ArrowHeadLength.length; i++) {
			if (currentLength <= ArrowHeadLength[i]) {
				i += ArrowHeadLength.length + amount;
				i %= ArrowHeadLength.length;
				arrow.setArrowheadLength(ArrowHeadLength[i]);
				return;
			}
		}
	}

	private Item _virtualSpot = null;

	public void showVirtualSpot(Item orig, int mouseX, int mouseY) {
		if (orig.getLines().size() != 1)
			return;

		// lines that are in 'connected' mode, also cannot be attached
		if (orig.getLines().get(0).getOppositeEnd(orig).isFloating())
			return;

		Item spot = new Dot(-1, orig.getX(), orig.getY());
		Item.DuplicateItem(orig, spot);
		spot.setThickness(Math.max(this.getThickness(), 5));
		if (this.getColor() != Color.RED)
			spot.setColor(Color.RED);
		else
			spot.setColor(Color.BLUE);

		// unhighlight all the dots
		for (Item conn : getAllConnected()) {
			conn.setHighlightMode(Item.HighlightMode.None);
		}

		// calculate nearest point on line from spot
		double slope1;
		double slope2;
		double x, y;

		slope1 = (_start.getY() - _end.getY() * 1.0)
				/ (_start.getX() - _end.getX());
		slope2 = -1 / slope1;

		// if the line is horizontal
		if (slope1 == 0) {
			x = spot.getX();
			y = _start.getY();
			// if the line is vertical
		} else if (slope2 == 0) {
			x = _start.getX();
			y = spot.getY();
			// otherwise, the line is sloped
		} else {
			x = (-1 * slope2 * spot.getX() + spot.getY() - _start.getY() + slope1
					* _start.getX())
					/ (slope1 - slope2);
			y = slope1 * (x - _start.getX()) + _start.getY();
		}

		// position spot on the line
		spot.setPosition((int) x, (int) y);
		_virtualSpot = spot;
		invalidateVirtualSpot();
	}

	public void showVirtualSpot(int mouseX, int mouseY) {
		Item spot = new Dot(mouseX, mouseY, -1);
		spot.setThickness(Math.max(this.getThickness(), 5));
		if (DEFAULT_HIGHLIGHT.equals(this.getColor()))
			spot.setColor(ALTERNATE_HIGHLIGHT);
		else
			spot.setColor(DEFAULT_HIGHLIGHT);

		// calculate nearest point on line from spot
		double slope1;
		double slope2;
		double x, y;

		slope1 = (_start.getY() - _end.getY() * 1.0)
				/ (_start.getX() - _end.getX());
		slope2 = -1 / slope1;

		// if the line is horizontal
		if (slope1 == 0) {
			x = spot.getX();
			y = _start.getY();
			// if the line is vertical
		} else if (slope2 == 0) {
			x = _start.getX();
			y = spot.getY();
			// otherwise, the line is sloped
		} else {
			x = (-1 * slope2 * spot.getX() + spot.getY() - _start.getY() + slope1
					* _start.getX())
					/ (slope1 - slope2);
			y = slope1 * (x - _start.getX()) + _start.getY();
		}

		// position spot on the line
		spot.setPosition((int) x, (int) y);
		_virtualSpot = spot;
		invalidateVirtualSpot();
	}

	public Item forceMerge(Item spot, int mouseX, int mouseY) {

		// calculate nearest point on line from spot
		double slope1;
		double slope2;
		double x, y;

		slope1 = (_start.getY() - _end.getY() * 1.0)
				/ (_start.getX() - _end.getX());
		slope2 = -1 / slope1;

		// if the line is horizontal
		if (slope1 == 0) {
			x = spot.getX();
			y = _start.getY();
			// if the line is vertical
		} else if (slope2 == 0) {
			x = _start.getX();
			y = spot.getY();
			// otherwise, the line is sloped
		} else {
			x = (-1 * slope2 * spot.getX() + spot.getY() - _start.getY() + slope1
					* _start.getX())
					/ (slope1 - slope2);
			y = slope1 * (x - _start.getX()) + _start.getY();
		}

		// position spot on the line
		spot.setPosition((int) x, (int) y);

		// Keep constraints
		// If its a constrained line dont merge
		for (Constraint c : _end.getConstraints()) {
			if (c.getOppositeEnd(_end).equals(_start)) {
				// c.replaceEnd(_start, spot);
				new Constraint(spot, _start, getParentOrCurrentFrame()
						.getNextItemID(), c.getType());
				new Constraint(spot, _end, getParentOrCurrentFrame()
						.getNextItemID(), c.getType());
				return null;
			}
		}

		Line temp = copy();

		Frame currentFrame = DisplayIO.getCurrentFrame();
		temp.setID(currentFrame.getNextItemID());
		temp.setEndItem(_end);
		temp.setStartItem(spot);
		currentFrame.addItem(temp);

		setEndItem(spot);

		// if(_arrowEnd == spot)
		// setArrow(-1,-1);

		return spot;
	}

	@Override
	public Item merge(Item merger, int mouseX, int mouseY) {

		if (!(merger.isLineEnd()))
			return merger;

		Item spot = merger;

		// dots with multiple lines cannot be attached, nor can dots with no
		// lines
		if (spot.getLines().size() != 1)
			return merger;

		// lines that are in 'connected' mode, also cannot be attached
		if (spot.isFloating()
				&& spot.getLines().get(0).getOppositeEnd(spot).isFloating())
			return merger;

		return forceMerge(merger, mouseX, mouseY);
	}

	@Override
	public Collection<Item> getConnected() {
		return getAllConnected();
	}

	@Override
	public void addAllConnected(Collection<Item> connected) {
		if (!connected.contains(this))
			connected.add(this);

		if (!connected.contains(_end))
			_end.addAllConnected(connected);

		if (!connected.contains(_start))
			_start.addAllConnected(connected);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Line))
			return false;

		return ((Item) o).getID() == getID();
	}

	@Override
	public void setActions(List<String> actions) {
	}

	@Override
	public List<String> getAction() {
		return null;
	}

	public String getLineEnds() {
		return _start.getID() + " " + _end.getID();
	}

	public int getLineType() {
		return 1;
	}

	@Override
	public boolean isFloating() {
		return _start.isFloating() && _end.isFloating();
	}

	@Override
	public void updatePolygon() {
		_poly = new Polygon();

		Rectangle one;
		Rectangle two;

		/**
		 * TODO: Fake dots seems inefficient, Fix it.
		 */

		// line goes from left->right, one->two
		if (_start.getX() <= _end.getX()) {
			/*
			 * one = _start.getPolygon().getBounds(); two =
			 * _end.getPolygon().getBounds();
			 */

			Item s = new Dot(_start.getX(), _start.getY(), -1);
			Item e = new Dot(_end.getX(), _end.getY(), -1);

			one = s.getPolygon().getBounds();
			two = e.getPolygon().getBounds();
		} else {
			Item s = new Dot(_start.getX(), _start.getY(), -1);
			Item e = new Dot(_end.getX(), _end.getY(), -1);

			one = e.getPolygon().getBounds();
			two = s.getPolygon().getBounds();
			/*
			 * one = _end.getPolygon().getBounds(); two =
			 * _start.getPolygon().getBounds();
			 */
		}

		// if one is above two
		if (one.getY() < two.getY()) {
			_poly.addPoint((int) one.getMaxX(), (int) one.getMinY());
			_poly.addPoint((int) two.getMaxX(), (int) two.getMinY());
			_poly.addPoint((int) two.getMinX(), (int) two.getMaxY());
			_poly.addPoint((int) one.getMinX(), (int) one.getMaxY());
			// if one is below two
		} else {
			_poly.addPoint((int) one.getMinX(), (int) one.getMinY());
			_poly.addPoint((int) two.getMinX(), (int) two.getMinY());
			_poly.addPoint((int) two.getMaxX(), (int) two.getMaxY());
			_poly.addPoint((int) one.getMaxX(), (int) one.getMaxY());
		}

	}

	@Override
	public void delete() {
		super.delete();

		_start.removeLine(this);
		_end.removeLine(this);
		
		//_start.invalidateAll();
		//_end.invalidateAll();

		if (_start.getLines().size() == 0)
			_start.delete();
		if (_end.getLines().size() == 0)
			_end.delete();
	}

	@Override
	public void anchor() {
		Frame current = getParentOrCurrentFrame();
		// Check if either end is off the current frame
		Frame parentEnd = getEndItem().getParent();
		if (parentEnd != null && parentEnd != current) {
			this.setParent(parentEnd);
		} else {
			Frame parentStart = getStartItem().getParent();
			if (parentStart != null && parentStart != current)
				this.setParent(parentStart);
		}

		super.anchor();
		fixArrowheadLength();
	}

	/**
	 * Removes constraints from the end points of this line.
	 */
	@Override
	public void removeAllConstraints() {
		// Find all constraints that include both the start and end.
		for (Constraint c : _start.getConstraints()) {
			if (c.contains(_end)) {
				_start.removeConstraint(c);
				_end.removeConstraint(c);
			}
		}
	}

	public double getLength() {
		return getLength(_start.getPosition(), _end.getPosition());
	}

	public Integer getPossibleConstraint() {
		if (_start.getY() == _end.getY() && _start.getX() != _end.getX())
			return Constraint.HORIZONTAL;
		else if (_start.getX() == _end.getX() && _start.getY() != _end.getY())
			return Constraint.VERTICAL;
		return null;
	}

	public static double getLength(Point p1, Point p2) {
		return Point.distance(p1.x, p1.y, p2.x, p2.y);
	}

	@Override
	public Rectangle[] getDrawingArea() { // TODO: CACHE - LIKE UPDATE POLYGON

		float currentThickness = this.getThickness() + 4;

		// Establish bounds
		int x = Math.min(_start.getX(), _end.getX());
		int y = Math.min(_start.getY(), _end.getY());
		int w = Math.max(_start.getX(), _end.getX()) - x + 1;
		int h = Math.max(_start.getY(), _end.getY()) - y + 1;

		int thickness = (int) Math.ceil(currentThickness);
		if (thickness < 4)
			thickness = 4;
		int halfThickness = (int) Math.ceil(currentThickness / 2);

		// Consider line thickness
		w += thickness;
		h += thickness;
		x -= halfThickness;
		y -= halfThickness;

		Rectangle bounds = new Rectangle(x, y, w, h);

		// TODO: Cap bounds

		if (_end.hasVisibleArrow()) {

			double arrowLength = _end.getArrowheadLength();

			if (arrowLength == AUTO_ARROWHEAD_LENGTH) {
				arrowLength = Math
						.ceil(getAutoArrowheadLength((float) getLength()));
			} else {
				arrowLength = Math.ceil(arrowLength);
			}

			arrowLength += ((thickness * 2) + 1);
			int nArrowLength = (int) (arrowLength + 1);

			/* NAIVE version - but probably more efficient in the long run...
			 overall as opposed to advanced/expensive calculations for getting exact
			 bounding box */
			Rectangle arrowBounds = new Rectangle(_end.getX() - nArrowLength,
					_end.getY() - nArrowLength, 2 * nArrowLength,
					2 * nArrowLength);

			if (currentThickness > 0.0f) {

				arrowBounds = new Rectangle(arrowBounds.x - halfThickness,
						arrowBounds.y - halfThickness, arrowBounds.width
								+ thickness, arrowBounds.height + thickness);
			}

			return new Rectangle[] { bounds, arrowBounds };
		} else {
			return new Rectangle[] { bounds };
		}

	}

	@Override
	public boolean hasPermission(UserAppliedPermission p) {
		return _start.hasPermission(p);
	}

	@Override
	public void setPermission(PermissionPair permissionPair) {
		_start.setPermission(permissionPair);
		_end.setPermission(permissionPair);
	}

	@Override
	public boolean dontSave() {
		return true;
	}

	@Override
	public void scale(Float scale, int originX, int originY) {
	}
}
