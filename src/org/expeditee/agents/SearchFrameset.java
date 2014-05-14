package org.expeditee.agents;

import java.util.Collection;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;

public class SearchFrameset extends SearchAgent {
	private long _firstFrame = 1;

	private long _maxFrame = Integer.MAX_VALUE;

	public SearchFrameset(long firstFrame, long maxFrame, String searchText) {
		this(searchText);
		_firstFrame = firstFrame;
		_maxFrame = maxFrame;
	}

	public SearchFrameset(String searchText) {
		super(searchText);
	}

	@Override
	protected Frame process(Frame frame) {
		if (frame == null) {
			frame = FrameIO.LoadFrame(_startName + '0');
		}
		String path = frame.getPath();

		int count = FrameIO.getLastNumber(_startName);
		for (long i = _firstFrame; i <= _maxFrame && i <= count; i++) {
			if (_stop) {
				break;
			}
			String frameName = _startName + i;
			overwriteMessage("Searching " + frameName);
			Collection<String> found = FrameIO.searchFrame(frameName, _pattern,
					path);
			addResults(i + "", frameName, found);
		}
		_results.save();

		String resultFrameName = _results.getName();
		if (_clicked != null)
			_clicked.setLink(resultFrameName);

		return _results.getFirstFrame();
	}
}
