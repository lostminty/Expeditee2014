package org.apollo.util;

public final class StringEx {
	
	private StringEx() {}
	
	/**
	 * This is a string equals method that handles null pointers
	 * 
	 * Notes: Javas thread-pool system doesn't mean that a string cannot only
	 * be created once in java ... one can explicity use the new operater for 
	 * a string to be duplicated... thus using the "==" operater is to risky.
	 * Especially since many methods such as substring uses the new operator!
	 * 
	 * @param str1
	 * 
	 * @param str2
	 * 
	 * @return
	 */
	public static boolean equals(String str1, String str2) {
		
		if (str1 == str2) return true; // they both null or same ref?
		
		else if (str1 == null) return false; // one is null
		
		else if (str2 == null) return false; // one is null
		
		return str1.equals(str2); // Evaluate..
		
	}

}
