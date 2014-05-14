package org.apollo.util;

import java.awt.Point;
import java.util.List;
/**
 * Represents a single mutable search result.
 * 
 * @author Brook Novak
 */
public class TextItemSearchResult {
	/** The textual content of the text item. */
	public String text;
	
	/** The link of the item. Null if no link: note: this could be a number ... in expeditees format without F tag. */
	public String explink;
	
	/** Any data that is with the text item. */
	public List<String> data;
	
	/** The pixel position in the frame at which the text item resides. */
	public Point position;
	
	public boolean containsData(String str) {
		assert(str != null);
		if (data != null) 
			return data.contains(str);
		return false;
	}
	
	public boolean containsDataTrimmedIgnoreCase(String str) {
		assert(str != null);
		if (data != null) {
			for (String d : data) {
				if (d != null && d.trim().equalsIgnoreCase(str)) {
					return true;
				}
			}
		}
			
		return false;
	}
}
