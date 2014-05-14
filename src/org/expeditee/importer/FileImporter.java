package org.expeditee.importer;

import java.awt.Point;
import java.io.File;
import java.io.IOException;

import org.expeditee.items.Item;

/**
 * Used for importing files (and folders) into Expeditee. A FileImporter
 * may or maynot choose to import a file.
 * 
 * @author Brook Novak
 *
 */
public interface FileImporter {
	
	/**
	 * Invoked when a file (or directory) is to be imported.
	 * 
	 * The importer can choose whether or not to stop other importers to import
	 * the files by returning true.
	 * 
	 * @param f
	 * 		The file to import. Not null.
	 * 
	 * @param location
	 * 		The location in expeditee space where the import was requested. 
	 * 		Null if not applicable.
	 * 
	 * @return 
	 * 		True to stop the import handling proccess for this file. False to allow other
	 * 		potential import handlers to also import the file.
	 * 		Should always return false if cannot handle the file.
	 * 
	 * @throws IOException
	 * 		If the import procedure failed.
	 */
	Item importFile(File f, Point location) throws IOException;
}
