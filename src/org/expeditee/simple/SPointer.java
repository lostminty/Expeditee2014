package org.expeditee.simple;

public class SPointer<T> extends SVariable<T> {

	public static final String framePrefix = SVariable.prefix + "fp"
			+ SVariable.separator;

	public static final String itemPrefix = SVariable.prefix + "ip"
			+ SVariable.separator;

	public static final String filePrefix = SVariable.prefix + "f"
			+ SVariable.separator;

	public static final String associationPrefix = SVariable.prefix + "ap"
			+ SVariable.separator;

	public SPointer(String name, T value) {
		super(name, value);
	}

	public void setValue(T newValue) {
		value_ = newValue;
	}

	@Override
	public String stringValue() {
		return value_.toString();
	}

	@Override
	public void setValue(SVariable<T> v) throws Exception {
		if (v instanceof SPointer) {
			setValue(v.getValue());
			return;
		}
		throw new Exception("Can not set pointer variable with primitive");
	}
}
