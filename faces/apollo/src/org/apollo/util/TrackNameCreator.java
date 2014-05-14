package org.apollo.util;

import java.util.HashMap;

/**
 * Creates default names for tracks.
 * 
 * @author Brook Novak
 *
 */
public class TrackNameCreator {
	
	private static HashMap<String, Integer> nameCounters = new HashMap<String, Integer>();

	private TrackNameCreator() {}
	
	
	private static final String DEFAULT_BASENAME = "untitled";
	
	// TODO: LOADING AND SAVING OF COUNTER MAP
	
	/**
	 * 
	 * @param nameBase
	 * 		If null, the defulat name
	 * @return
	 */
	public static String getDefaultName() {
		return getNameCopy(null);
	}
	
	/**
	 * Copies a name - increments counter value at the end of the name.
	 * 
	 * @param name
	 * 		If null then a default name copy is supplied. The name can already
	 *      include the counter - this will be extracted if so.
	 * 
	 * @return
	 * 		A copy of the given name. Never null.
	 */
	public static String getNameCopy(String name) {
		
		if (name == null) name = DEFAULT_BASENAME;
		
		int i;
		for (i = name.length() - 1; i > 0; i--) {
			if (!Character.isDigit(name.charAt(i))) {
				break;
			}		
		}
		
		// Extract case sensitive basename
		if (i <= 0) {
			name = DEFAULT_BASENAME;
		} else {
			name = name.substring(0, i + 1);
		}

		Integer count = nameCounters.get(name.toLowerCase());
		
		if (count == null) {
			count = new Integer(1);
		} else {
			count = new Integer(count.intValue() + 1);
		}
		
		// Add or replace current count for this namebase
		nameCounters.put(name.toLowerCase(), count);
		
		return name + count.toString();

	}
}
