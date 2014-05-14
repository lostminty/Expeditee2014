package org.expeditee.gui;

import org.expeditee.simple.SString;

public class AttributeValuePair {
	public static final char ANNOTATION_CHAR = '@';

	public static final char SEPARATOR_CHAR = ':';

	public static final char VALUE_SEPARATOR_CHAR = ' ';
	
	public static final char VALUE_DELIM_CHAR = '\n';

	public static final String SEPARATOR_STRING = "" + SEPARATOR_CHAR
			+ VALUE_SEPARATOR_CHAR;

	private String _notes;

	private SString _attribute;

	private SString _value;

	private String _string;

	private boolean _bAnnotation;

	public AttributeValuePair(String text) {
		this(text, true);
	}

	public AttributeValuePair(String text, boolean lastSeparator) {
		_bAnnotation = false;
		_attribute = null;
		_value = null;
		_string = null;

		if (text == null)
			return;

		int lineSeparator = text.indexOf(Character.LINE_SEPARATOR);
		if (lineSeparator >= 0) {
			_notes = text.substring(lineSeparator + 1);
			text = text.substring(0, lineSeparator);
		}

		text = text.trim();

		if (text.length() == 0)
			return;

		int ind = lastSeparator ? text.lastIndexOf(SEPARATOR_CHAR) : text
				.indexOf(SEPARATOR_CHAR);

		// If its an annotation there must be no space between the @ and colon
		// and the first character after the annotation must be a letter
		if (text.charAt(0) == ANNOTATION_CHAR) {
			_bAnnotation = true;
			if (text.length() == 1
					|| !Character.isLetterOrDigit(text.charAt(1)))
				return;
			// All chars in the attribute must be letters or digits
			for (int i = 2; i < ind; i++) {
				if (!Character.isLetterOrDigit(text.charAt(i)))
					return;
			}
			text = text.substring(1);
			ind--;
			if (ind < 1) {
				setAttribute(text);
				return;
			}
		} else if (ind < 1) {
			setValue(text.trim());
			return;
		}
		setAttribute(text.substring(0, ind).trim());
		setValue(text.substring(ind + 1).trim());
	}

	public AttributeValuePair(String attribute, String value) {
		setAttribute(attribute.trim());
		setValue(value.trim());
	}

	public boolean isAnnotation() {
		return _bAnnotation;
	}

	public String getAttribute() {
		if (_attribute == null)
			return null;
		return _attribute.stringValue();
	}

	public String getValue() {
		if (_value == null)
			return "";
		return _value.stringValue();
	}

	public Double getDoubleValue() throws NumberFormatException {
		assert (_value != null);
		// Can be null if the text for the AVP was empty
		if (_value == null) {
			return Double.NaN;
		}

		return _value.doubleValue();
	}

	public Double getDoubleAttribute() throws NumberFormatException {
		assert (_attribute != null);
		return _attribute.doubleValue();
	}

	public void setAttribute(String newAttribute) {
		if (_attribute == null)
			_attribute = new SString(newAttribute.trim());
		else
			_attribute.parse(newAttribute);
		_string = null;
	}

	public void setValue(String newValue) {
		newValue = newValue.trim();
		if (_value == null)
			_value = new SString(newValue);
		else
			_value.parse(newValue);
		_string = null;
	}

	@Override
	public String toString() {
		if (_string == null) {
			StringBuffer sb = new StringBuffer();
			if (_bAnnotation)
				sb.append(ANNOTATION_CHAR);
			if (_attribute != null) {
				sb.append(_attribute);
				if (_value != null) {
					sb.append(SEPARATOR_STRING);
				}
			}
			if (_value != null) {
				sb.append(_value);
			}
			if (_notes != null) {
				sb.append(Character.LINE_SEPARATOR).append(_notes);
			}

			_string = sb.toString();
		}
		return _string;
	}

	public boolean isValid() {
		return _value != null || _attribute != null;
	}

	public boolean hasAttribute() {
		return _attribute != null;
	}

	public void appendValue(String value) {
		if (_value == null)
			_value = new SString(value.trim());
		else
			_value.parse(_value.stringValue() + VALUE_DELIM_CHAR
					+ value.trim());
	}

	public Integer getIntegerValue() throws NumberFormatException {
		assert (_value != null);

		return _value.integerValue().intValue();
	}

	public boolean getBooleanValue() {
		assert (_value != null && _value.length() > 0);

		return _value.booleanValue();
	}

	public boolean hasPair() {
		return _attribute != null && _value != null;
	}

	public String getAttributeOrValue() {
		if (_attribute == null)
			return getValue();
		return getAttribute();
	}

	public boolean hasAttributeOrValue() {
		return _attribute != null || _value != null;
	}

}
