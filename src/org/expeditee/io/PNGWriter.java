package org.expeditee.io;

import java.io.IOException;

import org.expeditee.actions.Misc;
import org.expeditee.agents.DefaultAgent;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;

public class PNGWriter extends AbstractHTMLWriter {
	@Override
	protected void writeStartFrame(Frame starting) throws IOException {
		if (_filename.equalsIgnoreCase(DefaultAgent.CLIPBOARD))
			return;

		indent();
		String filesFolder = getFilesFolder();
		String imageDirectory = FrameIO.EXPORTS_DIR + filesFolder;
		String fileName = Misc.ImageFrame(starting, "PNG", imageDirectory);
		if (fileName != null) {
			String imageRelativeFullPath = filesFolder + fileName;
			_writer.write("<p><img src = '" + imageRelativeFullPath
					+ "' border=1></p>");
			_writer.write(ItemWriter.NEW_LINE);
		} else {
			MessageBay.errorMessage("Could not create image for "
					+ starting.getName());
		}
	}

	@Override
	protected void writeItem(Item item) {
	}
}

// Get the inheritance right
// Make sure the outline is output correctly and to the correct file
// Output all the images into a correctly named folder xxx_files
// Add the image links to the HTML file
// Without annotations- option to do it with
