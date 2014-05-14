package org.expeditee.agents;

import java.util.Collection;
import java.util.HashSet;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.items.Text;

public class SearchTreeAndReplace extends SearchAgent {
	public SearchTreeAndReplace(String searchText) {
		super(searchText);
	}
	
	@Override
	protected Frame process(Frame frame) {
		try {
			searchTree(new HashSet<String>(), _startName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		_results.save();

		String resultFrameName = _results.getName();
		if (_clicked != null)
			_clicked.setLink(resultFrameName);

		return _results.getFirstFrame();
	}

	public boolean searchTree(Collection<String> visitedFrames, String frameName)
			throws Exception {
		if (_stop) {
			return false;
		}
		// Avoid infinate loops
		if (visitedFrames.contains(frameName))
			return false;
		visitedFrames.add(frameName);

		Frame frameToSearch = FrameIO.LoadFrame(frameName);
		if (frameToSearch == null)
			return false;

		overwriteMessage("Searching " + frameName);

		for (Text itemToSearch : frameToSearch.getTextItems()) {
			// Search for the item and add it to the results page if
			// it is found
			if (searchItem(itemToSearch, _pattern,
					_replacementString)) {
				// Add a linked item to the results frame
				_results.addText(frameName, null, frameName, null, false);
			}
			if (!itemToSearch.isAnnotation()) {
				String link = itemToSearch.getAbsoluteLink();
				if (link != null) {
					searchTree(visitedFrames, link);
				}
			}
		}
		FrameGraphics.requestRefresh(true);
		FrameIO.SaveFrame(frameToSearch, false);
		return true;
	}
}
