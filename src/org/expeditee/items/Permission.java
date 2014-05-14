package org.expeditee.items;

public enum Permission {
	none, followLinks, copy, createFrames, full;

	/**
	 * Converts the given Expeditee permission code into a int corresponding to
	 * the constants defined in Item.
	 * 
	 * @param permissionCode
	 *            The Expeditee permission code to convert
	 * @return The resulting int corresponding to one of the constants defined
	 *         in Item
	 */
	public static Permission convertString(String permissionCode) {
		return getPermission(permissionCode, Permission.full);
	}

	public static Permission getPermission(String permissionCode,
			Permission defaultPermission) {
		if (permissionCode == null)
			return defaultPermission;

		permissionCode = permissionCode.trim().toLowerCase();
		if (permissionCode.length() == 0)
			return defaultPermission;

		// if it is a single char just match the first character
		try {
			return values()[Integer.parseInt(permissionCode)];
			// Otherwise match the whole string
		} catch (Exception ex) {
			try {
				return valueOf(permissionCode);
			} catch (Exception e) {
			}
		}

		// default permission
		return defaultPermission;
	}

	public int getCode() {
		return ordinal();
	}

	public String toString() {
		return this.name();
	}

	public static Permission min(Permission p1, Permission p2) {
		return p1.ordinal() < p2.ordinal() ? p1 : p2;
	}

	public static Permission max(Permission p1, Permission p2) {
		return p1.ordinal() > p2.ordinal() ? p1 : p2;
	}
}
