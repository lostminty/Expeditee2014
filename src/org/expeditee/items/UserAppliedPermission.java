package org.expeditee.items;


public enum UserAppliedPermission {
	none, followLinks, copy, createFrames, full;
	
	
	public static UserAppliedPermission getPermission(String permissionCode,
			UserAppliedPermission defaultPermission) 
	{
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
			} 
			catch (Exception e) {
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

	public static UserAppliedPermission min(UserAppliedPermission p1, UserAppliedPermission p2) {
		return p1.ordinal() < p2.ordinal() ? p1 : p2;
	}

	public static UserAppliedPermission max(UserAppliedPermission p1, UserAppliedPermission p2) {
		return p1.ordinal() > p2.ordinal() ? p1 : p2;
	}
}
