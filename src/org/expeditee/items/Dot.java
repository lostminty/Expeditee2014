package org.expeditee.items;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameKeyboardActions;

/**
 * Represents a point on the screen. All Point objects are stored as x,y pairs
 * with an Item ID. Note: Lines and all combinates of lines (rectangles, etc)
 * are represented only as Points that share line IDs.
 * 
 * @author jdm18
 * 
 */
public class Dot extends Item {

	// Standard Item variables

	// private static final int _MINIMUM_DOT_SIZE = 6;

	private static final int MINIMUM_DOT_SIZE = 2;

	public Dot(int id) {
		super();
		setID(id);
	}

	/**
	 * Constructs a new Point with the given x,y coordinates and Item ID.
	 * 
	 * @param x
	 *            The x coordinate of this point
	 * @param y
	 *            The y coordinate of this point
	 * @param id
	 *            The Item ID of this Point
	 */
	public Dot(int x, int y, int id) {
		super();
		setID(id);
		setPosition(x, y);
	}

	@Override
	public void setColor(Color c) {
		super.setColor(c);

		// update the colour of any lines
		for (Line line : getLines())
			line.setColor(c);
	}

	@Override
	public void setAnchorLeft(Float anchor) {
		if (!isLineEnd()) {
			super.setAnchorLeft(anchor);
			return;
		}
		
		invalidateFill();
		invalidateCommonTrait(ItemAppearence.PreMoved);
		
		this._anchorLeft = anchor;
		this._anchorRight = null;
		
		int oldX = getX();
		if (anchor != null) {
			float deltaX = anchor - oldX;
			anchorConnected(AnchorEdgeType.Left, deltaX);
		}
		
		invalidateCommonTrait(ItemAppearence.PostMoved);
		invalidateFill();
	}

	@Override
	public void setAnchorRight(Float anchor) {
		if (!isLineEnd()) {
			super.setAnchorRight(anchor);
			return;
		}
		invalidateFill();
		invalidateCommonTrait(ItemAppearence.PreMoved);
		
		this._anchorRight = anchor;
		this._anchorLeft = null;
		
		int oldX = getX();
		if (anchor != null) {
			float deltaX = FrameGraphics.getMaxFrameSize().width - anchor
					- getBoundsWidth() - oldX;

			anchorConnected(AnchorEdgeType.Right, deltaX);
		}
	
		invalidateCommonTrait(ItemAppearence.PostMoved);
		invalidateFill();
	}

	@Override
	public void setAnchorTop(Float anchor) {
		if (!isLineEnd()) {
			super.setAnchorTop(anchor);
			return;
		}
		invalidateFill();
		invalidateCommonTrait(ItemAppearence.PreMoved);
		
		this._anchorTop = anchor;
		this._anchorBottom = null;
		
		int oldY = getY();
		if (anchor != null) {
			float deltaY = anchor - oldY;
			anchorConnected(AnchorEdgeType.Top, deltaY);
		}

		invalidateCommonTrait(ItemAppearence.PostMoved);
		invalidateFill();
	}

	@Override
	public void setAnchorBottom(Float anchor) {
		if (!isLineEnd()) {
			super.setAnchorBottom(anchor);
			return;
		}
		invalidateFill();
		invalidateCommonTrait(ItemAppearence.PreMoved);
		
		this._anchorBottom = anchor;
		this._anchorTop = null;
		
		int oldY = getY();
		if (anchor != null) {
			float deltaY = FrameGraphics.getMaxFrameSize().height - anchor
					- getBoundsHeight() - oldY;
			anchorConnected(AnchorEdgeType.Bottom, deltaY);
		}

		invalidateCommonTrait(ItemAppearence.PostMoved);
		invalidateFill();
	}



	@Override
	public void paint(Graphics2D g) {
		if (isHighlighted() /* && !FreeItems.getInstance().contains(this) */) {
			Color highlightColor = getHighlightColor();
			g.setColor(highlightColor);
			g.setStroke(DOT_STROKE);
			// g.setStroke()
			// Draw the highlighting rectangle surrounding the dot
			// this is drawn even if its part of a rectangle

			if (isVectorItem())
				updatePolygon();

			Rectangle rect = getPolygon().getBounds();
			if (_mode == HighlightMode.Enclosed ||
			// Make sure single dots are highlighted filled
					this.getConnected().size() <= 1)
				g.fillRect(rect.x, rect.y, rect.width, rect.height);
			else if (_mode == HighlightMode.Connected){
				g.setStroke(HIGHLIGHT_STROKE);
				g.drawRect(rect.x, rect.y, rect.width, rect.height);
			}else if (_mode == HighlightMode.Normal) {
				g.fillOval(rect.x, rect.y, rect.width, rect.height);
			}
			// System.out.println(_mode.toString());
		}

		// dots on lines are hidden
		if (getLines().size() > 0)
			return;

		g.setColor(getPaintColor());

		int thick = (int) getThickness();
		if (thick < MINIMUM_DOT_SIZE)
			thick = MINIMUM_DOT_SIZE;

		int width = thick / 2;

		switch (_type) {
		case circle:
			if (_filled)
				g.fillOval(getX() - width, getY() - width, thick, thick);
			else {
				g.drawOval(getX() - width, getY() - width, thick, thick);
			}
			break;
		case diamond:
			int[] x = new int[4];
			int[] y = new int[4];

			x[0] = x[2] = getX();
			x[1] = getX() - width;
			x[3] = getX() + width;

			y[1] = y[3] = getY();
			y[0] = getY() - width;
			y[2] = getY() + width;

			if (_filled)
				g.fillPolygon(x, y, 4);
			else {
				g.drawPolygon(x, y, 4);
			}
			break;
		case triangle:
			x = new int[3];
			y = new int[3];

			x[0] = getX();
			x[1] = getX() - width;
			x[2] = getX() + width;

			y[0] = getY() - width;
			y[1] = y[2] = getY() + width;

			if (_filled)
				g.fillPolygon(x, y, 3);
			else {
				g.drawPolygon(x, y, 3);
			}
			break;
		case roundSquare:
			int arc = thick / 2;
			if (_filled)
				g.fillRoundRect(getX() - width, getY() - width, thick, thick,
						arc, arc);
			else {
				g.drawRoundRect(getX() - width, getY() - width, thick, thick,
						arc, arc);
			}
			break;
		default:
			if (_filled)
				g.fillRect(getX() - width, getY() - width, thick, thick);
			else {
				g.drawRect(getX() - width, getY() - width, thick, thick);
			}

		}

	}

	/**
	 * Updates the points of the polygon surrounding this Dot
	 */
	public void updatePolygon() {
		int thick = Math.round(getThickness());
		// Sets a minimum size for the dot
		thick = Math.max(thick, getGravity() * 2);

		int x = getX() - thick / 2;
		int y = getY() - thick / 2;

		_poly = new Polygon();
		_poly.addPoint(x - getGravity(), y - getGravity());
		_poly.addPoint(x + thick + getGravity(), y - getGravity());
		_poly.addPoint(x + thick + getGravity(), y + thick + getGravity());
		_poly.addPoint(x - getGravity(), y + thick + getGravity());
	}

	@Override
	public Item copy() {
		Dot copy = new Dot(getX(), getY(), getID());

		Item.DuplicateItem(this, copy);

		return copy;
	}

	@Override
	public int setHighlightColor() {
		super.setHighlightColor();

		return Item.DEFAULT_CURSOR;
	}

	@Override
	public void setAnnotation(boolean val) {
		DisplayIO.setCursorPosition(this.getPosition());
		FrameKeyboardActions.replaceDot(this, '@');
	}

	@Override
	public Item merge(Item merger, int mouseX, int mouseY) {
		// if the item being merged with is another Dot
		if (merger instanceof Dot) {
			if (merger.hasEnclosures() || hasEnclosures())
				return merger;

			Item dot = (Item) merger;
			merger.setPosition(this.getPosition());
			// prevent concurrency issues if removing lines
			List<Line> lines = new ArrayList<Line>();
			lines.addAll(dot.getLines());

			for (Line line : lines) {
				// remove lines that are in common
				if (getLines().contains(line)) {
					dot.removeLine(line);
					removeLine(line);
				} else {
					// check for duplicated lines as a result of merging
					Item check = (Item) line.getOppositeEnd(dot);
					boolean dup = false;

					for (Line l : getLines()) {
						Item opposite = l.getOppositeEnd(this);

						if (check == opposite) {
							line.getStartItem().removeLine(line);
							line.getEndItem().removeLine(line);
							dup = true;
							break;
						}
					}

					if (!dup)
						line.replaceLineEnd(dot, this);
				}
			}

			setThickness(dot.getThickness());
			setColor(dot.getColor());
			setFillColor(dot.getFillColor());

			return null;
		}

		if (merger instanceof Text) {
			merger.setPosition(this.getPosition());
			List<Line> lines = new LinkedList<Line>();
			lines.addAll(getLines());
			for (Line line : lines)
				line.replaceLineEnd(this, merger);
			this.delete();
			return merger;
		}

		// if the item being merged with is a Line
		if (merger instanceof Line) {
			Line line = (Line) merger;
			// if this dot is part of the line then keep it, otherwise it
			// can be removed
			if (line.getOppositeEnd(this) != null && getLines().contains(line))
				return merger;
			else
				return null;
		}

		return merger;
	}

	@Override
	public String getTypeAndID() {
		return "P " + getID();
	}

	@Override
	public void delete() {
		super.delete();

		for (Line l : this.getLines())
			l.delete();
	}

	@Override
	public void anchor() {
		Frame current = getParentOrCurrentFrame();
		// This is to make lines anchored across frames be on one frame
		for (Line l : getLines()) {
			Frame parent = l.getOppositeEnd(this).getParent();
			if (parent != null && parent != current) {
				this.setParent(parent);
				if (DisplayIO.getCurrentSide() == 0)
					this.setX(this.getX() - DisplayIO.getMiddle());
				else
					this.setX(this.getX() + DisplayIO.getMiddle());
			}
			break;
		}

		super.anchor();

		// TODO is the code below needed... what for?
		for (Line line : getLines()) {
			if (line.getID() < 0 && !current.getItems().contains(line)) {
				line.setID(current.getNextItemID());
				line.setHighlightColor();
				// Mike: Why was this line here?
				// anchor(line);
			}
		}
	}

	@Override
	public void addLine(Line line) {
		super.addLine(line);
		line.setColor(getColor());
		line.setThickness(getThickness());
		line.setLinePattern(getLinePattern());
	}

	@Override
	public void lineColorChanged(Color c) {
		if (getColor() != c) {
			setColor(c);
		}
	}

	@Override
	public boolean dontSave() {
		if (getThickness() <= 1 && (getLines().size() == 0)
				&& getConstraints().size() == 0) {
			return true;
		}
		return super.dontSave();
	}
}
