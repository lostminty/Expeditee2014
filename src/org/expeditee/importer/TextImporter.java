package org.expeditee.importer;

import java.awt.Color;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.expeditee.gui.FrameCreator;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

public class TextImporter implements FileImporter {

	public TextImporter() {
		super();
	}

	public Item importFile(final File f, Point location) throws IOException {
		if (location == null || f == null) {
			return null;
		}
		final String fullPath = f.getAbsolutePath();

		final Text source = FrameDNDTransferHandler.importString(f.getPath(),
				location);

		// Create a frameCreator to write the text
		final FrameCreator frames = new FrameCreator(f.getName());

		new Thread() {
			public void run() {
				try {
					// Open a file stream to the file
					BufferedReader br = new BufferedReader(new FileReader(
							fullPath));

					MessageBay.displayMessage("Importing " + f.getName() + "...");

					// Read in the text
					String nextLine;
					while ((nextLine = br.readLine()) != null) {
						frames.addText(nextLine, null, null, null, false);
					}

					frames.save();
					source.setLink(frames.getName());
					MessageBay.displayMessage(f.getName() + " import complete", Color.GREEN);
					FrameGraphics.requestRefresh(true);
				} catch (Exception e) {
					e.printStackTrace();
					MessageBay.errorMessage(e.getMessage());
				}
			}
		}.start();
		FrameGraphics.refresh(true);
		return source;
	}
}
