package org.apollo.util;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Performs text item searches in expeditee files.
 * 
 * @author Brook Novak
 *
 */
public class ExpediteeFileTextSearch {
	
	private ExpediteeFileTextSearch() {}

	/**
	 * Peforms a perfix search.
	 * 
	 * @param framePath
	 * 		The path of the frame file to search. Must not be null.
	 * 
	 * @param textPrefixes
	 * 		The prefixes of the text to match against. Must not be null or empty.
	 * 
	 * @return
	 * 		A list of results. Never null.
	 * 
	 * @throws FileNotFoundException
	 * 		If the frame path does not exist.
	 * 
	 * @throws IOException
	 * 		If a read operation failed.
	 * 
	 * @throws NullPointerException
	 * 		If framePath or textPrefixes is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If textPrefixes is empty.
	 */
	public static List<TextItemSearchResult> prefixSearch(String framePath, String[] textPrefixes)
		throws FileNotFoundException, IOException {
		
		if (framePath == null)
			throw new NullPointerException("framePath");
		else if (textPrefixes == null)
			throw new NullPointerException("textPrefix");
		else if (textPrefixes.length == 0)
			throw new IllegalArgumentException("textPrefix");
		
		for (String prefix : textPrefixes)
			if (prefix == null) throw new NullPointerException("textPrefix");
		
		List<TextItemSearchResult> results = new LinkedList<TextItemSearchResult>();

		BufferedReader reader = null;
		
		try 
		{
			reader = new BufferedReader(new FileReader(framePath));
			
			String line = null;
			
			String strippedTextualLine = null; // not null if match
			String framelinkLine = null;
			LinkedList<String> currentDataLines = new LinkedList<String>();
			String positionLine = null;

			while (((line = reader.readLine()) != null)) {
				
				if (line.length() <= 2) continue;
				
				if (line.startsWith("S")) {
					
					if (strippedTextualLine != null) {
						results.add(createResult(strippedTextualLine, framelinkLine, currentDataLines, positionLine));
					}
					
					// Reset - next item to parse
					currentDataLines.clear();
					strippedTextualLine = null;
					framelinkLine = null;
					positionLine = null;
				
				} else if (line.startsWith("T ")) { // leave space - important
					
					strippedTextualLine = line.substring(2);
					boolean matches = false; 
					for (String prefix : textPrefixes) { // check for match
						if (strippedTextualLine.startsWith(prefix)) {
							matches = true;
							break;
						}
					}
					
					if (!matches) strippedTextualLine = null;
							
				} else if (line.startsWith("D ")) { // leave space - important
					
					currentDataLines.add(line);
				
				} else if (line.startsWith("F ")) { // leave space - important
					
					framelinkLine = line;
					
				} else if (line.startsWith("P ")) {
					positionLine = line;
				}
				
			}
			
			// Add last item
			if (strippedTextualLine != null) {
				results.add(createResult(strippedTextualLine, framelinkLine, currentDataLines, positionLine));
			}
			
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		
		return results;
	}
	
	
	private static TextItemSearchResult createResult(
			String strippedTextualLine,
			String framelinkLine,
			LinkedList<String> dataLines,
			String positionLine) {
		
		if (strippedTextualLine == null || dataLines == null || positionLine == null) {
			System.err.println("FAILED TO READ ITEM IN EXPEDITEE FILE");
			return null;
		}
		
		
		TextItemSearchResult result = new TextItemSearchResult();
		
		result.text = strippedTextualLine;
		result.explink = (framelinkLine != null) ? framelinkLine.substring(2) : null; // not stripped
		result.data = new LinkedList<String>();
		for (String dataLine : dataLines) {
			result.data.add(dataLine.substring(2)); // data lines not stripped
		}
		
		// Parse position information
		if (positionLine.length() <= 2) return null;
		String tmp = positionLine.substring(2);
		tmp = tmp.trim();
		int yStrIndex = tmp.indexOf(' ');
		if (yStrIndex == -1) return null;
		
		int x;
		int y;
		
		try {
			x = Integer.parseInt(tmp.substring(0, yStrIndex).trim());
			y = Integer.parseInt(tmp.substring(yStrIndex).trim());
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return null;
		}
		
		result.position = new Point(x, y);
		
		return result;
		
	}
	
}
