package org.expeditee.simple;

public class SReal extends SPrimitive<Double> {
	public static String prefix = SVariable.prefix + "r" + SVariable.separator;

	public SReal() {
		super();
	}

	public SReal(String name, Double value) {
		super(name, value);
	}

	public SReal(double value) throws Exception {
		super(value);
	}

	public SReal(float value) throws Exception {
		super((double) value);
	}

	@Override
	public void parse(String s) throws Exception {
		if (s.equals(""))
			value_ = 0.0;
		else {
			try {
				value_ = Double.parseDouble(s);
			} catch (NumberFormatException ne) {
				value_ = (double) Long.decode(s);
			}
		}
	}

	@Override
	public Boolean booleanValue() {
		return value_ > 0;
	}

	@Override
	public Long integerValue() {
		return Math.round(value_);
	}

	@Override
	public Double doubleValue() {
		return value_;
	}

	@Override
	public Character characterValue() {
		return new Character((char) Math.round(value_));
	}

	@Override
	public void setValue(SPrimitive v) throws IncorrectTypeException {
		value_ = v.doubleValue();
	}
}
