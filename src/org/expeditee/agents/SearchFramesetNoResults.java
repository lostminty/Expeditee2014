package org.expeditee.agents;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;

public class SearchFramesetNoResults extends SearchAgent {
	private long _firstFrame = 1;
	private long _maxFrame = Integer.MAX_VALUE;
	private Map<String, Collection<String>> _results = new HashMap<String, Collection<String>>();
	
	public SearchFramesetNoResults(long firstFrame, long maxFrame, String searchText) {
		this(searchText);
		_firstFrame = firstFrame;
		_maxFrame = maxFrame;
	}
	
	public SearchFramesetNoResults(String searchText) {
		super(searchText);
	}

	@Override
	protected Frame process(Frame frame) {
		String path = frame.getPath();
		int count = FrameIO.getLastNumber(frame.getFramesetName());
		for (long i = _firstFrame;i <= _maxFrame && i <= count; i++) {
			if (_stop) {
				break;
			}
			String frameName = _startName + i;
			Collection<String> found = FrameIO.searchFrame(frameName, _pattern, path);
			int size = found == null? 0 :found.size();
			if(found!= null)
				_frameCount++;
			if(size > 0){
				_results.put(frameName, found);
			}
		}
		return null;
	}
	
	public Map<String, Collection<String>> getResult(){
		return _results;
	}
	
	@Override
	public boolean hasResultFrame() {
		return false;
	}
	
	@Override
	public boolean hasResultString() {
		return true;
	}
	
	@Override
	public String toString(){
		StringBuffer resultString = new StringBuffer();
		for(String frame: _results.keySet()){
			resultString.append(frame);
			for(String found: _results.get(frame)){
				resultString.append('[').append(found).append(']');
			}
			resultString.append('\n');
		}
		resultString.deleteCharAt(resultString.length()-1);
		return resultString.toString();
	}
}
