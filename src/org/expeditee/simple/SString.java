package org.expeditee.simple;

public class SString extends SPrimitive<String> {
	public static String prefix = SVariable.prefix + "s" + SVariable.separator;

	Double doubleValue_ = null;

	public SString() {
		super();
	}

	public SString(String name, String value) {
		super(name, value);
	}

	public SString(String value) /* throws Exception */{
		super(value);
	}

	@Override
	public void parse(String s) {
		value_ = s;
		doubleValue_ = null;
	}

	@Override
	public Boolean booleanValue() {
		return Boolean.parseBoolean(value_);
	}

	@Override
	public Long integerValue() {
		if (value_.equals(""))
			return 0L;
		try {
			return Long.decode(value_);
		} catch (NumberFormatException ne) {
		}
		return Math.round(Double.parseDouble(value_));
	}

	@Override
	public Double doubleValue() {
		if (doubleValue_ != null)
			return doubleValue_;

		if (value_.equals(""))
			doubleValue_ = 0.0;
		else {
			try {
				doubleValue_ = Double.parseDouble(value_);
			} catch (NumberFormatException ne) {
				try{
				doubleValue_ =(double) Long.decode(value_);
				}catch(Exception e){
					doubleValue_ = Double.NaN;
				}
			}
		}
		return doubleValue_;
	}

	@Override
	public String stringValue() {
		return value_;
	}

	@Override
	public Character characterValue() {
		if (value_.length() > 0)
			return value_.charAt(0);

		return '\0';
	}

	@Override
	public void setValue(SPrimitive v) {
		value_ = v.stringValue();
		doubleValue_ = null;
	}

	public int length() {
		return value_.length();
	}
}