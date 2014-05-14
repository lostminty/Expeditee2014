package org.expeditee.items;

/**
 * This class represents a constraint between two Dots on the screen. A vertical
 * constraint means the Dot's Y values will be equal, likewise a horizontal
 * constraint means the Dot's X values will be equal.
 * 
 * @author jdm18
 * 
 */
public class Constraint {
	public static final int VERTICAL = 2;

	public static final int HORIZONTAL = 3;
	
	public static final int DIAGONAL_POS = 4;
	
	public static final int DIAGONAL_NEG = 5;

	// the list of points that are constrained
	private Item _start = null;

	private Item _end = null;

	// id and type info
	private int _id;

	private int _type;

	/**
	 * Constructs a constraint with the two given end points, id, and type. Type
	 * should be one of the constants defined in this class.
	 * 
	 * @param a
	 *            One of the points in this constraint.
	 * @param b
	 *            The other point in this constraint.
	 * @param id
	 *            The ID of this constraint item.
	 * @param type
	 *            The type of this constraint (horizontal, vertical).
	 */
	public Constraint(Item a, Item b, int id, int type) {
		_start = a;
		_end = b;

		a.addConstraint(this);
		b.addConstraint(this);

		_id = id;
		_type = type;
	}

	/**
	 * @return The Item ID of this Constraint.
	 */
	public int getID() {
		return _id;
	}

	/**
	 * 
	 * @return The type of this Constraint. The Type corresponds to the
	 *         constants defined in this class.
	 */
	public int getType() {
		return _type;
	}

	public Item getStart() {
		return _start;
	}

	public Item getEnd() {
		return _end;
	}

	public boolean contains(Item i) {
		if (_start == i || _end == i)
			return true;

		return false;
	}

	/**
	 * If there are only two points in this constraint, this method returns the
	 * Point not passed to it. If there are more than two points then the first
	 * encountered is returned.
	 * 
	 * @return A Point in the constraint that is not the one given (or null if
	 *         none is found).
	 */
	public Item getOppositeEnd(Item from) {
		if (from == _start)
			return _end;

		if (from == _end)
			return _start;

		return null;
	}

	public void replaceEnd(Item toReplace, Item with) {
		assert(_start != _end);
		if (_start == toReplace) {
			_start = with;
		} else if (_end == toReplace) {
			_end = with;
		}
		toReplace.removeConstraint(this);
		with.addConstraint(this);
	}

	public String getLineEnds() {
		String ends = "";

		if (_start != null)
			ends += _start.getID();

		if (_end != null)
			ends += " " + _end.getID();

		return ends;
	}

	public boolean isDiagonal() {
		return _type == Constraint.DIAGONAL_NEG || _type == Constraint.DIAGONAL_POS;
	}
	
	public Float getGradient() {
		switch(_type){
		case Constraint.DIAGONAL_NEG:
			return -1.0F;
		case Constraint.DIAGONAL_POS:
			return 1.0F;
		case Constraint.HORIZONTAL:
			return 0.0F;
		case Constraint.VERTICAL:
			return Float.POSITIVE_INFINITY;
		}
		assert(false);
		return null;
	}

}
