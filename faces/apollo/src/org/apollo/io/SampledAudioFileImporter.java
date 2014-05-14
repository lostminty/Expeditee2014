package org.apollo.io;

import java.awt.Point;
import java.io.File;
import java.io.IOException;

import org.apollo.widgets.SampledTrack;
import org.expeditee.gui.DisplayIO;
import org.expeditee.importer.FileImporter;
import org.expeditee.items.Item;

/**
 * Imports sampled audio files as track widgets into the current frame.
 * 
 * @author Brook Novak
 *
 */
public class SampledAudioFileImporter implements FileImporter {

	public Item importFile(File f, Point location) throws IOException {

		if (location == null || !AudioIO.canImportFile(f) ||
				DisplayIO.getCurrentFrame() == null) return null;
		
		SampledTrack trackWidget = SampledTrack.createFromFile(
				f,
				DisplayIO.getCurrentFrame(), 
				location.x, 
				location.y);
		
		// Add the sampled track widget to the current frame
		DisplayIO.getCurrentFrame().addAllItems(trackWidget.getItems());
		
		return trackWidget.getSource(); // Don't allow for other importers to deal with this file
		
	}

}
