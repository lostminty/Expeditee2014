package org.expeditee.simple;

public class Pointers extends Variables<SPointer<?>> {

	private static final String[] prefixes = new String[] {
			SPointer.itemPrefix, SPointer.framePrefix, SPointer.filePrefix,
			SPointer.associationPrefix };

	public static boolean isPointer(String varName) {
		for (String s : prefixes) {
			if (varName.startsWith(s))
				return true;
		}
		return false;
	}

	public Pointers() {
		super();

	}

	protected String getType() {
		return "pointer";
	}

	// TODO how do I put these two together without the warning
	public void add(String name, SPointer<?> value) {
		addT(name, value);
	}

	public <T> void addT(String name, SPointer<T> value) {
		list_.add(new SPointer<T>(name, value.getValue()));
	}

	/**
	 * Assigns a new value to a pointer variable in the list. If the variable
	 * does not already exist the variable is created.
	 * 
	 * @param <T>
	 * @param name
	 *            the name of the variable which will have its value changed.
	 * @param value
	 *            the new value of the variable.
	 * @throws Exception
	 *             if an error occurs in changing the variables value.
	 */
	public <T> void setObject(String name, T value) throws Exception {
		SPointer v = null;
		try {
			// if it is an existing variable change the value
			v = getVariable(name);
		} catch (VariableNotFoundException e) {
			// If the first variable doesnt exist then add it
			list_.add(new SPointer<T>(name, value));
			return;
		}
		// This will throw an exception if types dont match
		v.setValue(value);
	}

	/**
	 * Deletes a variable if it exists.
	 * @param variableName name of the variable to delete
	 */
	public void delete(String variableName) {
		try {
			list_.remove(getVariable(variableName));
		} catch (VariableNotFoundException e) {

		}
	}
}
