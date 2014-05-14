package org.expeditee.simple;

public abstract class SPrimitive<T> extends SVariable<T> {

	public SPrimitive(String name, T value) {
		super(name, value);
	}

	public SPrimitive(T value) {
		super(null, value);
	}

	public SPrimitive() {
		super();
	}

	public Long integerValue() throws IncorrectTypeException {
		throw new IncorrectTypeException("integer",this.getClass().getName());
	}

	public Boolean booleanValue() throws IncorrectTypeException {
		throw new IncorrectTypeException("boolean", this.getClass().getName());
	}

	public Double doubleValue() throws IncorrectTypeException {
		throw new IncorrectTypeException("real", this.getClass().getName());
	}

	public Character characterValue() throws IncorrectTypeException {
		throw new IncorrectTypeException("character", this.getClass().getName());
	}

	public abstract void setValue(SPrimitive v) throws IncorrectTypeException;

	@Override
	public void setValue(SVariable<T> v) throws IncorrectTypeException {
		if (v instanceof SPrimitive) {
			setValue((SPrimitive<?>) v);
			return;
		}
		throw new IncorrectTypeException("primitive", "pointer");
	}

	/**
	 * Sets the value of the primitive using a string.
	 * 
	 * @param s
	 * @throws Exception
	 */
	public abstract void parse(String s) throws Exception;
	
	@Override
	public String toString() {
		return stringValue();
	}
}
