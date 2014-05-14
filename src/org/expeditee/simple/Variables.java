package org.expeditee.simple;

import java.util.ArrayList;

import org.expeditee.gui.MessageBay;

public abstract class Variables<T extends SVariable<?>> {

	protected ArrayList<T> list_;

	public Variables() {
		list_ = new ArrayList<T>();
	}

	public int size() {
		return list_.size();
	}

	public void clear() {
		list_.clear();
	}

	public void display() {
		for (T v : list_) {
			MessageBay.displayMessage(v.getName() + ": " + v.stringValue());
		}
	}

	public T getVariable(String name) throws VariableNotFoundException {
		for (T v : list_) {
			if (v.getName().equalsIgnoreCase(name)) {
				return v;
			}
		}
		throw new VariableNotFoundException(name + " is not an existing "
				+ getType());
	}

	/**
	 * Sets the value of variable1 to value of variable2. If variable1 doesnt
	 * exist then it is created.
	 * 
	 * @param variableToSet-
	 *            the variable whos value will be modified.
	 * @param variableWithNewValue-
	 *            the variable to get the new value from.
	 */
	public void set(String variableToSet, String variableWithNewValue)
			throws Exception {
		// If the variables are the same then do nothing
		if (variableToSet.equalsIgnoreCase(variableWithNewValue))
			return;

		T toGetValueFrom = getVariable(variableWithNewValue);
		T toSet = null;
		try {
			toSet = getVariable(variableToSet);
		} catch (Exception e) {
			// if the variable to be set doesnt exist and exception is
			// thrown So it will be added
			add(variableToSet, toGetValueFrom);
			return;
		}
		//TODO:figure out how to fix the ANT build error
		((SVariable)toSet).setValue(toGetValueFrom);
	}

	public abstract void add(String name, T value) throws Exception;

	protected abstract String getType();
}
