package org.expeditee.simple;

import org.expeditee.math.ExpediteeJEP;

public class Primitives extends Variables<SPrimitive<?>> {
	private static final String[] prefixes = new String[] { SInteger.prefix,
			SBoolean.prefix, SString.prefix, SReal.prefix, SCharacter.prefix };

	public static boolean isPrimitive(String varName) {
		for (String s : prefixes) {
			if (varName.startsWith(s))
				return true;
		}
		return false;
	}

	public static final int PREFIX_LENGTH = SInteger.prefix.length();

	public Primitives() {
		super();
	}

	protected String getType() {
		return "primitive";
	}

	public void setValue(String variableName, SPrimitive newValue)
			throws IncorrectTypeException {
		try {
			// if it is an existing variable change the value
			SPrimitive v = getVariable(variableName);
			v.setValue(newValue);
		} catch (Exception e) {
			try{
				add(variableName, newValue);
			}catch(IncorrectTypeException ite){
				throw ite;
			}catch(Exception ex){
				throw new IncorrectTypeException(ex.getClass().getSimpleName());
				//DO NOTHING... THIS SHOULD NOT HAPPEN
				//But the ANT builder is complaining
				//ex.printStackTrace();
			}
		}
	}

	public void setValue(String variableName, String newValue) throws Exception {
		setValue(variableName, new SString(newValue));
	}

	/**
	 * Adds a variable to the list of primitives.
	 * 
	 * @param name
	 *            the variable name
	 * @param value
	 *            a primitive to take the new variables value from
	 * @throws Exception
	 *             if the variable name is invalid
	 */
	public void add(String name, SPrimitive value)
			throws IncorrectTypeException {
		// figure out the type and add it...
		SPrimitive newVar;
		if (name.startsWith(SInteger.prefix))
			newVar = new SInteger();
		else if (name.startsWith(SBoolean.prefix))
			newVar = new SBoolean();
		else if (name.startsWith(SReal.prefix))
			newVar = new SReal();
		else if (name.startsWith(SString.prefix))
			newVar = new SString();
		else if (name.startsWith(SCharacter.prefix))
			newVar = new SCharacter();
		else
			throw new IncorrectTypeException(name, "primitive");
		newVar.setName(name);
		newVar.setValue(value);
		list_.add(newVar);
	}

	public String getStringValue(String name) throws Exception {
		return getVariable(name).stringValue();
	}

	public boolean getBooleanValue(String name) throws Exception {
		return getVariable(name).booleanValue();
	}

	public long getIntegerValue(String name) throws Exception {
		return getVariable(name).integerValue();
	}

	public double getDoubleValue(String name) throws Exception {
		return getVariable(name).doubleValue();
	}

	public char getCharacterValue(String name) throws Exception {
		return getVariable(name).characterValue();
	}

	/*
	 * public String setStringValue(String name, String value) throws Exception {
	 * return getVariable(name).stringValue(); }
	 * 
	 * public boolean setBooleanValue(String name, boolean value) throws
	 * Exception { return getVariable(name).booleanValue(); }
	 * 
	 * public long setIntegerValue(String name, long value) throws Exception {
	 * return getVariable(name).integerValue(); }
	 * 
	 * public double setDoubleValue(String name, double value) throws Exception {
	 * return getVariable(name).doubleValue(); }
	 * 
	 * public double setCharacterValue(String name, char value) throws Exception {
	 * return getVariable(name).characterValue(); }
	 */

	public static boolean isNumeric(String varName) {
		return varName.startsWith(SInteger.prefix)
				|| varName.startsWith(SReal.prefix);
	}

	public boolean equalValues(String var1, String var2) throws Exception {
		return compareValues(var1, var2) == 0;
	}

	/**
	 * Checks if the string representation of two variables is identical
	 * ignoring case.
	 * 
	 * @param var1
	 * @param var2
	 * @return
	 * @throws Exception
	 */
	public boolean equalValuesNoCase(String var1, String var2) throws Exception {
		return getStringValue(var1).equalsIgnoreCase(getStringValue(var2));
	}

	/**
	 * 
	 * @param var1
	 * @param var2
	 * @return
	 * @throws Exception
	 */
	public int compareValues(String var1, String var2) throws Exception {
		// If one of them is a real and one is an integer then do a numeric
		// comparison
		if ((var1.startsWith(SReal.prefix) || var2.startsWith(SReal.prefix))) {
			double v1 = getDoubleValue(var1);
			double v2 = getDoubleValue(var2);
			return v1 > v2 ? 1 : (v1 < v2 ? -1 : 0);
		} else if ((var1.startsWith(SInteger.prefix) || var2
				.startsWith(SInteger.prefix))) {
			long v1 = getIntegerValue(var1);
			long v2 = getIntegerValue(var2);
			return v1 > v2 ? 1 : (v1 < v2 ? -1 : 0);
		} else if ((var1.startsWith(SBoolean.prefix) || var2
				.startsWith(SBoolean.prefix))) {
			boolean v1 = getBooleanValue(var1);
			boolean v2 = getBooleanValue(var2);
			return v1 == v2 ? 0 : (v1 == true ? 1 : -1);
		}
		return getStringValue(var1).compareTo(getStringValue(var2));
	}

	/**
	 * Increments a variable.
	 * 
	 * @param var
	 *            the name of the variable to increment
	 * @throws Exception
	 */
	public void add(String var) throws Exception {
		setValue(var, new SReal(getVariable(var).doubleValue() + 1));
	}

	/**
	 * Decrements a variable.
	 * 
	 * @param var
	 *            the name of the variable to decrement
	 * @throws Exception
	 */
	public void subtract(String var) throws Exception {
		setValue(var, new SReal(getVariable(var).doubleValue() - 1));
	}

	/**
	 * Adds two variables to gether returning the value in the first.
	 * 
	 * @param toSet
	 * @param amount
	 * @throws Exception
	 */
	public void add(String toSet, String amount) throws Exception {
		setValue(toSet, new SReal(getVariable(toSet).doubleValue()
				+ getVariable(amount).doubleValue()));
	}

	public void subtract(String toSet, String amount) throws Exception {
		setValue(toSet, new SReal(getVariable(toSet).doubleValue()
				- getVariable(amount).doubleValue()));
	}

	public void multiply(String toSet, String amount) throws Exception {
		setValue(toSet, new SReal(getVariable(toSet).doubleValue()
				- getVariable(amount).doubleValue()));
	}

	public void divide(String toSet, String amount) throws Exception {
		setValue(toSet, new SReal(getVariable(toSet).doubleValue()
				- getVariable(amount).doubleValue()));
	}

	public void add(String var1, String var2, String varAnswer)
			throws Exception {
		setValue(varAnswer, new SReal(getVariable(var1).doubleValue()
				+ getVariable(var2).doubleValue()));
	}

	public void subtract(String var1, String var2, String varAnswer)
			throws Exception {
		setValue(varAnswer, new SReal(getVariable(var1).doubleValue()
				- getVariable(var2).doubleValue()));
	}

	public void divide(String var1, String var2, String varAnswer)
			throws Exception {
		setValue(varAnswer, new SReal(getVariable(var1).doubleValue()
				/ getVariable(var2).doubleValue()));
	}

	public void multiply(String var1, String var2, String varAnswer)
			throws Exception {
		setValue(varAnswer, new SReal(getVariable(var1).doubleValue()
				* getVariable(var2).doubleValue()));
	}

	public void modulo(String var1, String var2, String varAnswer)
			throws Exception {
		setValue(varAnswer, new SReal(getVariable(var1).integerValue()
				% getVariable(var2).integerValue()));
	}

	public void power(String var1, String var2, String varAnswer)
			throws Exception {
		setValue(varAnswer, new SReal(Math.pow(getVariable(var1).doubleValue(),
				getVariable(var2).doubleValue())));
	}

	public void log(String variable, String answer) throws Exception {
		setValue(answer, new SReal(Math
				.log(getVariable(variable).doubleValue())));
	}

	public void log10(String variable, String answer) throws Exception {
		setValue(answer, new SReal(Math.log10(getVariable(variable)
				.doubleValue())));
	}

	public void sqrt(String variable, String answer) throws Exception {
		setValue(answer, new SReal(Math.sqrt(getVariable(variable)
				.doubleValue())));
	}

	public void exp(String variable, String answer) throws Exception {
		setValue(answer, new SReal(Math
				.exp(getVariable(variable).doubleValue())));
	}

	public void not(String variable, String answer) throws Exception {
		setValue(answer, new SBoolean(!getVariable(variable).booleanValue()));
	}

	public void addToParser(ExpediteeJEP myParser) {
		for (SPrimitive var : list_) {
			try {
				Double value = var.doubleValue();
				myParser.addVariable(var.getName().substring(SReal.prefix.length()), value);
			} catch (Exception e) {
			}
		}
	}
}
