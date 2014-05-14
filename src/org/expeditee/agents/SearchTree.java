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

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.io.Conversion;
import org.expeditee.settings.folders.FolderSettings;

public class SearchTree extends SearchAgent {
	private Map<String, Collection<String>> _searchResults = new HashMap<String, Collection<String>>();

	public SearchTree(String searchText) {
		super(searchText);
	}

	@Override
	protected Frame process(Frame frame) {
		if (frame == null)
			frame = FrameIO.LoadFrame(_startName);

		searchTree(frame.getName(), _searchResults, new HashSet<String>());
		_results.save();

		String resultFrameName = _results.getName();
		if (_clicked != null)
			_clicked.setLink(resultFrameName);

		return _results.getFirstFrame();
	}

	@Override
	/**
	 * @param frame
	 * @return
	 */
	protected String getSearchDescription(Frame frame) {
		return frame.getName();
	}

	/**
	 * Returns a list of the frames searched and any matches on those frames.
	 * 
	 * @param frameName
	 *            the name of the top not in the tree of frames to search
	 * @param pattern
	 *            the pattern to search for
	 * @param results
	 *            a list of frames on which matches were found and the text that
	 *            matched the pattern
	 * @param visited
	 *            a list of the frames that were visited in the searchTree
	 */
	public void searchTree(String frameName,
			Map<String, Collection<String>> results, Collection<String> visited) {
		// Check if this node has already been visited
		if (visited.contains(frameName))
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
		overwriteMessage("Searching " + frameName);

		String frameset = Conversion.getFramesetName(frameName);

		Collection<String> frameResults = new LinkedList<String>();
		// Open the file and search the text items
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fullPath));
			String next;
			StringBuffer sb = new StringBuffer();
			String link = null;
			boolean ignore = false;

			while (reader.ready() && ((next = reader.readLine()) != null)) {

				if (next.length() == 0) {
					// Ignore annotations
					if (ignore) {
						ignore = false;
						link = null;
						continue;
					}

					// Ignore non text items
					if (sb.length() == 0) {
						link = null;
						continue;
					}

					String toSearch = sb.substring(0, sb.length() - 1);
					String resultSurrogate = getResultSurrogate(toSearch);
					if (resultSurrogate != null) {
						frameResults.add(resultSurrogate);
					}

					if (link != null) {
						searchTree(link, results, visited);
					}

					// Reinit the item variables
					link = null;
					sb = new StringBuffer();

				} else if (ignore) {
					continue;
				} else if (next.startsWith("T")) {
					String text = next.substring(2).trim();
					// Ignore the rest of annotation items...
					if (text.length() > 0
							&& text.charAt(0) == AttributeValuePair.ANNOTATION_CHAR) {
						ignore = true;
						continue;
					}
					sb.append(text).append('\n');
				} else if (next.startsWith("F")) {
					link = next.substring(2);
					// Convert number only links
					if (Character.isDigit(link.charAt(0)))
						link = frameset + link;
				}
			}
			// Only add the results if a match was found on the frame
			if (frameResults.size() > 0) {
				results.put(frameName, frameResults);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int size = frameResults.size();
		if (size > 0) {
			addResults(frameName, frameResults);

			// String repeats = size > 1 ? ("(" + size + ")") : "";
			// _results.addText(frameName + repeats, null, frameName, null,
			// false);
			// FrameGraphics.requestRefresh(true);
		}
	}

	protected String getResultSurrogate(String toSearch) {
		if (toSearch.toLowerCase().contains(_pattern))
			return toSearch;
		return null;
	}
}
