package org.expeditee.simple;

public class SInteger extends SPrimitive<Long> {
	public static String prefix = SVariable.prefix + "i" + SVariable.separator;

	public SInteger() {
		super();
	}

	public SInteger(String name, Long value) {
		super(name, value);
	}

	public SInteger(String name, Integer value) {
		super(name, value.longValue());
	}

	public SInteger(long value) throws Exception {
		super(value);
	}

	public SInteger(int value) throws Exception {
		super((long) value);
	}

	@Override
	public void parse(String s) throws Exception {
		if (s.equals(""))
			value_ = 0L;
		else {
			try {
				value_ = Long.decode(s);
			} catch (NumberFormatException ne) {
				value_ = Math.round(Double.parseDouble(s));
			}
		}
	}

	@Override
	public Boolean booleanValue() {
		return value_ > 0;
	}

	@Override
	public Long integerValue() {
		return value_;
	}

	@Override
	public Double doubleValue() {
		return value_.doubleValue();
	}

	@Override
	public Character characterValue() {
		return new Character((char) value_.intValue());
	}

	@Override
	public void setValue(SPrimitive v) throws IncorrectTypeException {
		value_ = v.integerValue();
	}
}
