package org.expeditee.setting;

import java.util.LinkedList;
import java.util.List;

import org.expeditee.items.Text;

public abstract class ListSetting<T> extends VariableSetting {

	protected List<T> _default;
	protected List<T> _value;
	
	public ListSetting(String tooltip, List<T> value) {
		super(tooltip);
		_default = new LinkedList<T>(value);
		_value = value;
	}
	
	public ListSetting(String tooltip) {
		this(tooltip, new LinkedList<T>());
	}
	
	public List<T> get() {
		return _value;
	}
	
	public void set(List<T> value) {
		_value = value;
	}
	
	public abstract boolean setSetting(Text text);
	
	public void setDefault(List<T> value) {
		_default = new LinkedList<T>(value);
	}
	
	public void reset() {
		_value = new LinkedList<T>(_default);
	}

}
