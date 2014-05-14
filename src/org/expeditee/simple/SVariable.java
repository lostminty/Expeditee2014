package org.expeditee.simple;

/**
 * Class for a generic variable.
 * 
 * @author mrww1
 * 
 */
public abstract class SVariable<T extends Object> {
	public static final String prefix = "$";

	protected static final String separator = ".";

	protected String name_;

	protected T value_;

	public SVariable(String name, T value) {
		name_ = name;
		value_ = value;
	}

	public SVariable() {
	}

	public String getName() {
		return name_;
	}

	public T getValue() {
		return value_;
	}

	protected void setName(String newName) {
		name_ = newName;
	}

	public String stringValue() {
		return value_.toString();
	}

	public abstract void setValue(SVariable<T> v) throws Exception;
}
