package org.expeditee.agents;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;

public class SearchFramesetAndReplace extends SearchAgent {
	private long _firstFrame = 1;
	private long _maxFrame = Integer.MAX_VALUE;
	
	public SearchFramesetAndReplace(long firstFrame, long maxFrame, String searchText) {
		this(searchText);
		_firstFrame = firstFrame;
		_maxFrame = maxFrame;
	}
	
	public SearchFramesetAndReplace(String searchText) {
		super(searchText);
	}

	@Override
	protected Frame process(Frame frame) {
		int count = FrameIO.getLastNumber(_startName);
		for (long i = _firstFrame;i <= _maxFrame && i <= count; i++) {
			if (_stop) {
				break;
			}
			String frameName = _startName + i;
			overwriteMessage("Searching " + frameName);
			if(searchFrame(_results, frameName, _pattern,
					_replacementString))
				_frameCount++;
		}
		_results.save();

		String resultFrameName = _results.getName();
		if (_clicked != null)
			_clicked.setLink(resultFrameName);

		return _results.getFirstFrame();
	}
}
