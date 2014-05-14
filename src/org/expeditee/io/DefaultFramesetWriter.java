package org.expeditee.io;

import java.io.IOException;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;

public class DefaultFramesetWriter extends DefaultFrameWriter {
	protected long  _firstFrame = 1;
	protected long _maxFrame = Long.MAX_VALUE;
	
	protected DefaultFramesetWriter(long firstFrame, long maxFrame){
		_firstFrame = firstFrame;
		_maxFrame = maxFrame;
	}
	
	@Override
	protected void outputFrame(Frame toWrite) throws IOException {
		String framesetName = toWrite.getFramesetName();
		
		_maxFrame = Math.min(_maxFrame, FrameIO.getLastNumber(framesetName));
		
		for (long i = _firstFrame; i <= _maxFrame; i++) {
			if (_stop) {
				break;
			}
			String frameName = framesetName + i;
			Frame nextFrame = FrameIO.LoadFrame(frameName);
			if (nextFrame != null) {
				MessageBay.overwriteMessage("Processing " + frameName);
				super.outputFrame(nextFrame);
			}
		}
	}
	
	@Override
	protected String finaliseFrame() throws IOException {
		return "Frameset" + finalise();
	}
}
