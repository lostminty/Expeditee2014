package org.expeditee.setting;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

/**
 * Generic setting class for storing simple settings
 * 
 * @author jts21
 *
 * @param <T> type of the setting
 */
public abstract class GenericSetting<T> extends VariableSetting {

	protected final Class<T> _type;
	protected T _default;
	protected T _value;
	
	/**
	 * Instantiates a new setting
	 * @param type Reference to the type of the setting, since Java's generics implementation apparently got
	 * 	dropped on it's head at some point, and we can't know what type the generic object is at runtime without
	 * 	manually passing a reference to the class
	 * @param tooltip Tooltip text to display (once tooltips are implemented)
	 * @param value Default value
	 */
	public GenericSetting(Class<T> type, String tooltip, T value) {
		super(tooltip);
		_type = type;
		_default = value;
		_value = value;
	}
	
	/**
	 * Instantiates a new setting with no default value
	 */
	public GenericSetting(Class<T> type, String tooltip) {
		this(type, tooltip, (T) saneInitialValue(type));
	}
	
	/**
	 * @return true if this setting is a String, Number or Boolean
	 */
	public boolean isPrimitive() {
		return _type.equals(String.class) || _type.equals(Integer.class) || _type.equals(Float.class) || _type.equals(Double.class) || _type.equals(Boolean.class);
	}
	
	/**
	 * Provides a sane initial value for settings with no default value
	 */
	private static Object saneInitialValue(Class<?> type) {
		if(type.equals(String.class)) {
			return "";
		} else if(type.equals(Integer.class) || type.equals(Float.class) || type.equals(Double.class)) {
			return 0;
		} else if(type.equals(Boolean.class)) {
			return false;
		}
		return null;
	}
	
	/**
	 * @return the type of this setting
	 */
	public Class<T> getType() {
		return _type;
	}
	
	/**
	 * @return the value of this setting
	 */
	public T get() {
		return _value;
	}
	
	/**
	 * Sets the value directly
	 */
	public void set(T value) {
		_value = value;
	}
	
	public boolean setSetting(Text text) {
		AttributeValuePair avp = new AttributeValuePair(text.getText(), false);
		if(avp.hasAttribute() && avp.getValue().trim().length() != 0) {
    		if(_type.equals(String.class)) {
    			String value = avp.getValue();
    			if(value.trim().length() == 0) {
    				return false;
    			}
    			set((T) value);
    		} else if(_type.equals(Integer.class)) {
    			set((T) avp.getIntegerValue());
    		} else if(_type.equals(Float.class)) {
    			set((T) (Float) avp.getDoubleValue().floatValue());
    		} else if(_type.equals(Double.class)) {
    			set((T) avp.getDoubleValue());
    		} else if(_type.equals(Boolean.class)) {
    			set((T) (Boolean) avp.getBooleanValue());
    		} else if(_type.equals(Text.class)) {
    			set((T) text);
    		} else {
    			System.err.println("Invalid type: " + _type.getName());
    			return false;
    		}
    		return true;
		}
		return false;
	}
	
	public void setDefault(T value) {
		_default = value;
	}
	
	/**
	 * Reset the value back to the default, if a default value was specified
	 */
	public void reset() {
		_value = _default;
	}

}
