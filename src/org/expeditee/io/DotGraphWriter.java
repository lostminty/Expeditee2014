package org.expeditee.io;

import java.io.IOException;
import java.io.Writer;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameUtils;
import org.expeditee.items.Item;

/**
 * Writes a file that can be opened in an app such as Gephi or GraphViz in order to visualize/graph the links between frames in a frameset.
 * Only maps the frameset that it is called from, although links from within the frameset to other framesets will be shown/included
 * 
 * @author ngw8
 * 
 */
public class DotGraphWriter extends DefaultFrameWriter {

	private String framesetName;
	
	private Frame currentFrame;

	@Override
	protected void initialise(Frame start, Writer writer) throws IOException {


		_format = ".dot";

		framesetName = start.getFramesetName();

		currentFrame = FrameUtils.getFrame(framesetName + "0");

		super.initialise(currentFrame, writer);

		_writer.write("digraph " + currentFrame.getFramesetName() + "{");
		_writer.newLine();

	}
	
	@Override
	public String writeFrame(Frame toWrite) throws IOException {

		initialise(toWrite, null);

		while (currentFrame != null) {
			super.outputFrame(currentFrame);

			currentFrame = FrameIO.LoadNext(currentFrame);
		}
		
		return this.finalise();
	}

	@Override
	protected void writeItem(Item toWrite) throws IOException {
		if (toWrite.hasLink()) {
			_writer.write(toWrite.getParentOrCurrentFrame().getName() + "->" + toWrite.getAbsoluteLink() + ";");

			_writer.newLine();
		}
	}

	@Override
	protected String finalise() throws IOException {
		_writer.write("}");
		_running = false;
		return super.finalise();
	}

}
