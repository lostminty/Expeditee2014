package org.expeditee.setting;

import java.util.Arrays;

import org.expeditee.items.Text;

public abstract class ArraySetting<T> extends VariableSetting {

	protected T[] _default;
	protected T[] _value;
	
	public ArraySetting(String tooltip, T[] value) {
		super(tooltip);
		_default = Arrays.copyOf(value, value.length);
		_value = value;
	}
	
	public T[] get() {
		return _value;
	}
	
	public void set(T[] value) {
		_value = value;
	}
	
	public abstract boolean setSetting(Text text);
	
	public void setDefault(T[] value) {
		_default = Arrays.copyOf(value, value.length);
	}
	
	public void reset() {
		_value = Arrays.copyOf(_default, _default.length);
	}

}
