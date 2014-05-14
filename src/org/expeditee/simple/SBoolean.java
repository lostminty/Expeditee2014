package org.expeditee.simple;

public class SBoolean extends SPrimitive<Boolean> {
	public static String prefix = SVariable.prefix + "b" + SVariable.separator;

	public SBoolean() {
		super();
	}

	public SBoolean(String name, Boolean value) {
		super(name, value);
	}

	public SBoolean(Boolean value) throws IncorrectTypeException {
		super(value);
	}

	@Override
	public void parse(String s) {
		String lowerCase = s.toLowerCase();
		value_ = lowerCase.equals("true") || lowerCase.equals("t")
				|| lowerCase.equals("yes") || lowerCase.equals("on");
	}

	@Override
	public Boolean booleanValue() {
		return value_;
	}

	@Override
	public Long integerValue() {
		return value_ ? 1L : 0L;
	}

	@Override
	public Double doubleValue() {
		return value_ ? 1.0 : 0.0;
	}

	@Override
	public Character characterValue() {
		return value_ ? 'T' : 'F';
	}

	@Override
	public void setValue(SPrimitive v) throws IncorrectTypeException {
		value_ = v.booleanValue();
	}
}
