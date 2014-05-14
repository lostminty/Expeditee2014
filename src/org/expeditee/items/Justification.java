package org.expeditee.items;

public enum Justification {
	center, full, left, right;

	/**
	 * Converts the given Expeditee justification code into a int corresponding
	 * to the constants defined in Item.
	 * 
	 * @param justCode
	 *            The Expeditee justification code to convert
	 * @return The resulting int corresponding to one of the constants defined
	 *         in Item
	 */
	public static Justification convertString(String justCode) {
		assert (justCode != null);
		justCode = justCode.trim().toLowerCase();

		// if it is a single char just match the first character
		if (justCode.length() == 1) {
			char code = justCode.charAt(0);
			Justification[] values = values();
			for (int i = 0; i < values.length; i++) {
				Justification j = values[i];
				if (Character.toLowerCase(j.name().charAt(0)) == code)
					return j;
			}
			// Otherwise match the whole string
		} else {
			try {
				return valueOf(justCode);
			} catch (Exception e) {
			}
		}

		// default justification
		return left;
	}

	public char getCode() {
		return Character.toUpperCase(this.toString().charAt(0));
	}

	public String toString() {
		return this.name();
	}
}
