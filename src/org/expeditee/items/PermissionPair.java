package org.expeditee.items;

import org.expeditee.items.UserAppliedPermission;
import org.expeditee.settings.UserSettings;

public class PermissionPair {
	
	private UserAppliedPermission ownerPermission;
	private UserAppliedPermission notOwnerPermission;
	


	
	public PermissionPair(String permissionCode, UserAppliedPermission defaultPermission) 
	{
		ownerPermission = defaultPermission;
		notOwnerPermission = defaultPermission;
		
		if (permissionCode != null) {
	
			permissionCode = permissionCode.trim().toLowerCase();
			if (permissionCode.length() != 0) {
			
				if (permissionCode.length()==1) {
					// replicate it to cover ifOwner/ifNotOwner
					permissionCode += permissionCode;
				}
				
				
				ownerPermission = UserAppliedPermission.getPermission(permissionCode.substring(0,1), defaultPermission);
				notOwnerPermission = UserAppliedPermission.getPermission(permissionCode.substring(1,2), defaultPermission);	
				
			}
		}
	}

	public PermissionPair(UserAppliedPermission ownerPermission, UserAppliedPermission notOwnerPermission) 
	{
		this.ownerPermission = ownerPermission;;
		this.notOwnerPermission = notOwnerPermission;
	}
	
	public PermissionPair(UserAppliedPermission ownerPermissionForBoth) 
	{
		this(ownerPermissionForBoth,ownerPermissionForBoth);
	}
	
	
	public PermissionPair(PermissionPair pp) 
	{
		ownerPermission = pp.ownerPermission;;
		notOwnerPermission = pp.notOwnerPermission;
	}
	
	
	public  UserAppliedPermission getPermission(String username) 
	{
		
		if (UserSettings.UserName.get().equals(username)) {
			return ownerPermission;
		}
		else {
			return notOwnerPermission;
		}
	}
	
	/**
	 * Converts the given Expeditee permission code into a PermissionPair corresponding to
	 * the constants defined in Item.
	 * 
	 * @param permissionCode
	 *            The Expeditee permission code to convert
	 * @return The resulting PermissionPair corresponding to a pair of constants as defined
	 *         in Item
	 */
	public static PermissionPair convertString(String permissionCode) 
	{
		PermissionPair pp = new PermissionPair(permissionCode,UserAppliedPermission.full);
	
		return pp;
	}
	
	public String getCode() {
		return Integer.toString(ownerPermission.getCode()) + Integer.toString(notOwnerPermission.getCode());
	}

	
	
	public String toString()
	{
		return ownerPermission.toString() + ":" + notOwnerPermission.toString();
	}
}
