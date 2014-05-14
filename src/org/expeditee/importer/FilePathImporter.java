package org.expeditee.importer;

import java.awt.Point;
import java.io.File;
import java.io.IOException;

import org.expeditee.items.Item;


/**
 * The most basic of file importers: imports a file/directory as a Text item.
 * 
 * @author Brook Novak
 *
 */
public class FilePathImporter implements FileImporter {

	public Item importFile(File f, Point location) throws IOException {
		if (location != null && f != null) {
			return FrameDNDTransferHandler.importString(f.getAbsolutePath(), location);			
		}

		return null;
		
	}

	
}
