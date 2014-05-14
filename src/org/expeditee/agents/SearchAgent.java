package org.expeditee.agents;

import java.util.Collection;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameCreator;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.io.Conversion;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

public abstract class SearchAgent extends DefaultAgent {

	private static final String DEFAULT_RESULTS_FRAMESET = "SearchResults";

	public static final int SURROGATE_LENGTH = 50;

	protected FrameCreator _results;

	protected String _pattern;

	protected String _replacementString;

	protected String _startName;

	public SearchAgent(String searchText) {
		if (searchText != null)
			_pattern = searchText.toLowerCase();
	}

	@Override
	public boolean initialise(Frame frame, Item item) {
		String pattern = item.getText();
		String resultFrameset = null;

		// TODO use a results frame specified on the profile frame
		if (item.getLink() == null) {
			resultFrameset = DEFAULT_RESULTS_FRAMESET;
		} else {
			resultFrameset = Conversion.getFramesetName(item.getAbsoluteLink(),
					false);
		}
		return initialise(frame, item, getSearchDescription(frame), resultFrameset,
				null, pattern);
	}

	/**
	 * @param frame
	 * @return
	 */
	protected String getSearchDescription(Frame frame) {
		return frame.getFramesetName();
	}

	/**
	 * 
	 * @param frame
	 * @param item
	 * @param startName
	 * @param resultsFrameset
	 * @param replacementString
	 * @param pattern
	 *            is ignored if the pattern has already been set earlier.
	 * @return
	 */
	public boolean initialise(Frame frame, Item item, String startName,
			String resultsFrameset, String replacementString, String pattern) {
		// TODO: Put the init params in the constructor!! Dont want to be
		// setting _pattern in two places!

		if (_pattern == null)
			_pattern = pattern.toLowerCase();
		_replacementString = replacementString;
		_startName = startName;

		// Create a frame to put the results on with the search query
		// and type as the title
		String title = this.getClass().getSimpleName() + " [" + startName
				+ "]"+getResultsTitleSuffix();
		_results = new FrameCreator(resultsFrameset, FrameIO.FRAME_PATH, title,
				false, true);
		// Set the frame to be displayed after running the agent
		_end = _results.getFirstFrame();

		return super.initialise(frame, item);
	}

	protected String getResultsTitleSuffix() {
		return " [" + _pattern + "]";
	}

	public String getResultsFrameName() {
		return _results.getName();
	}

	public static boolean searchItem(Text itemToSearch, String pattern,
			String replacementString) {
		String searchStr = itemToSearch.getText().toLowerCase();
		boolean bFound = searchStr.contains(pattern.toLowerCase());
		// If it is a find and replace... then replace with the replacement
		// string
		if (bFound && replacementString != null) {
			itemToSearch.setText(searchStr.replaceAll(pattern,
					replacementString));
		}
		return bFound;
	}

	public static boolean searchFrame(FrameCreator results, String frameName,
			String pattern, String replacementString) {
		int oldMode = FrameGraphics.getMode();
		FrameGraphics.setMode(FrameGraphics.MODE_XRAY, false);
		Frame frameToSearch = FrameIO.LoadFrame(frameName);
		FrameGraphics.setMode(oldMode, false);
		if (frameToSearch == null)
			return false;
		for (Text itemToSearch : frameToSearch.getTextItems()) {
			// Search for the item and add it to the results page if
			// it is found
			if (searchItem(itemToSearch, pattern, replacementString)) {
				// Add a linked item to the results frame
				results.addText(frameName, null, frameName, null, false);
			}
		}
		FrameGraphics.requestRefresh(true);
		FrameIO.SaveFrame(frameToSearch, false);
		return true;
	}
	
	protected int addResults(String frameName, Collection<String> found) {
		return addResults(frameName, frameName, found);
	}
	
	/**
	 * @param frameNumber
	 * @param frameName
	 * @param found
	 * @return
	 */
	protected int addResults(String frameNumber, String frameName, Collection<String> found) {
		int size = found == null ? 0 : found.size();
		// If the frame exists
		if (found != null)
			_frameCount++;
		if (size > 0) {
			// String repeats = size > 1? ("("+ size+ ")") : "";
			for (String s : found) {
				StringBuffer surrogate = new StringBuffer();
				surrogate.append("[").append(frameNumber).append("] ");
				if (s.length() > SearchAgent.SURROGATE_LENGTH)
					surrogate.append(
							s.substring(0, SearchAgent.SURROGATE_LENGTH - 3))
							.append("...");
				else {
					surrogate.append(s);
				}

				_results.addText(surrogate.toString(), null,
						frameName, null, false);
				FrameGraphics.requestRefresh(true);
			}
		}
		return size;
	}
}
