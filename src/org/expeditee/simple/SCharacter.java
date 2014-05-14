package org.expeditee.simple;

public class SCharacter extends SPrimitive<Character> {
	public static String prefix = SVariable.prefix + "c" + SVariable.separator;

	public SCharacter() {
		super();
	}

	public SCharacter(String name, Character value) {
		super(name, value);
	}

	public SCharacter(Character value) throws Exception {
		super(value);
	}

	@Override
	public void parse(String s) {
		if (s.equals(""))
			value_ = '\0';
		else
			value_ = s.charAt(0);
	}

	@Override
	public Boolean booleanValue() {
		return value_ == 'T' || value_ == 't';
	}

	@Override
	public Long integerValue() {
		return (long) value_.charValue();
	}

	@Override
	public Double doubleValue() {
		return (double) value_.charValue();
	}

	@Override
	public String stringValue() {
		return value_.toString();
	}

	@Override
	public Character characterValue() {
		return value_;
	}

	@Override
	public void setValue(SPrimitive v) throws IncorrectTypeException {
		value_ = v.characterValue();
	}
}