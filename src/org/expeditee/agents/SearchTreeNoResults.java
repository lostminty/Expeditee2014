package org.expeditee.agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.io.Conversion;
import org.expeditee.settings.folders.FolderSettings;

public class SearchTreeNoResults extends SearchAgent {
	private Map<String, Collection<String>> _results = new HashMap<String, Collection<String>>();

	public SearchTreeNoResults(String searchText) {
		super(searchText);
	}
	
	@Override
	protected Frame process(Frame frame) {
		searchTree(frame.getName(), _pattern, _results, new HashSet<String>());
		return null;
	}
	
	public Map<String, Collection<String>> getResult(){
		return _results;
	}
	
	@Override
	public boolean hasResultFrame() {
		return false;
	}
	
	/**
	 * Returns a list of the frames searched and any matches on those frames.
	 * @param frameName the name of the top not in the tree of frames to search
	 * @param pattern the pattern to search for
	 * @param results a list of frames on which matches were found and the text that matched the pattern
	 * @param visited a list of the frames that were visited in the searchTree
	 */
	public void searchTree(String frameName, String pattern, Map<String, Collection<String>> results, Collection<String> visited) {
		//Check if this node has already been visited
		if(visited.contains(frameName))
			return;
		
		visited.add(frameName);
		
		String fullPath = null;
		for (String possiblePath : FolderSettings.FrameDirs.get()) {
			fullPath = FrameIO.getFrameFullPathName(possiblePath, frameName);
			if (fullPath != null)
				break;
		}

		// If the frame was not located return null
		if (fullPath == null)
			return;
		
		_frameCount++;
		
		String frameset = Conversion.getFramesetName(frameName);
		
		Collection<String> frameResults = new LinkedList<String>();
		// Open the file and search the text items
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fullPath));
			String next;
			while (reader.ready() && ((next = reader.readLine()) != null)) {
				if (next.startsWith("T")) {
					String toSearch = next.substring(2);
					if (toSearch.toLowerCase().contains(pattern))
						frameResults.add(toSearch);
				}else if (next.startsWith("F")) {
					String link = next.substring(2);
					if(!FrameIO.isValidFrameName(link))
						link = frameset + link;
					searchTree(link, pattern, results, visited);
				}
			}
			//Only add the results if a match was found on the frame
			if(frameResults.size() > 0)
				results.put(frameName, frameResults);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
