package org.expeditee.io;

import java.io.IOException;

import org.expeditee.gui.Frame;

public interface TreeWriter extends FrameWriter {

	public String writeTree(Frame toWrite) throws IOException;
	
	public int getFrameCount();
}
