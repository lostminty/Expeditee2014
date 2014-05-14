package org.expeditee.io;

import java.io.IOException;
import java.io.Writer;

import org.expeditee.gui.Frame;

public interface FrameWriter {

	public void setOutputLocation(String filename);

	public String writeFrame(Frame toWrite) throws IOException;

	/**
	 * Methods for writing out frames to file as well as with other writers such
	 * as StringWriter.
	 * 
	 * @param toWrite
	 * @param writer
	 * @return
	 * @throws IOException
	 */
	public String writeFrame(Frame toWrite, Writer writer) throws IOException;

	public void stop();

	public boolean isRunning();

	public String getFileContents();
}
