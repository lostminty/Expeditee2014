package org.expeditee.agents;

import java.io.IOException;

import org.expeditee.gui.Frame;
import org.expeditee.gui.MessageBay;
import org.expeditee.io.PdfFramesetWriter;


public class PdfFrameset extends DefaultAgent {
	private PdfFramesetWriter _pdfWriter;
	
	private int _firstFrame = 1;

	private int _maxFrame = Integer.MAX_VALUE;
	
	private boolean _showFrameNames = false;

	public PdfFrameset(int firstFrame, int maxFrame, boolean showFrameNames) {
		super();
		_firstFrame = firstFrame;
		_maxFrame = maxFrame;
		_showFrameNames = showFrameNames;
	}
	
	public PdfFrameset(int firstFrame, int maxFrame) {
		this(firstFrame, maxFrame, false);
	}
	
	public PdfFrameset( boolean showFrameNames) {
		this(1,Integer.MAX_VALUE, showFrameNames);
	}

	public PdfFrameset() {
		this(false);
	}

	@Override
	protected Frame process(Frame frame) {
		_pdfWriter = new PdfFramesetWriter(_firstFrame, _maxFrame, _showFrameNames);
		
		try {
			_pdfWriter.writeFrame(frame);
		} catch (IOException e) {
			MessageBay.errorMessage("PdfFrameset error: " + e.getMessage());
			//e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public void stop(){
		_pdfWriter.stop();
	}
}
