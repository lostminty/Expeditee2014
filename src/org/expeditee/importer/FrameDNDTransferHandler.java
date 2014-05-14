package org.expeditee.importer;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.TransferHandler;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

/**
 * 
 * Expeditee's transfer handler (swing's drag and drop scheme) for importing
 * data into frames.
 * 
 * @author Brook Novak
 * 
 */
public class FrameDNDTransferHandler extends TransferHandler {

	private static final long serialVersionUID = 1L;

	private List<FileImporter> _customFileImporters = new LinkedList<FileImporter>();

	private List<FileImporter> _standardFileImporters = new LinkedList<FileImporter>();

	// GNOME and KDE desktops have a specialized way of DNDing files
	private DataFlavor _URIListDataflavorString;

	private DataFlavor _URIListDataflavorCharArray;

	private static FrameDNDTransferHandler _instance = new FrameDNDTransferHandler();

	public static FrameDNDTransferHandler getInstance() {
		return _instance;
	}

	private FrameDNDTransferHandler() {

		// Add standard file importers - order from most ideal to last resort
		// (if competing)

		// TODO: Image
		_standardFileImporters.add(new ImageImporter());
		_standardFileImporters.add(new pdfImporter());
		_standardFileImporters.add(new TextImporter());
		_standardFileImporters.add(new FilePathImporter()); // Filepath importer
		// as last resort

		try {
			_URIListDataflavorString = new DataFlavor(
					"text/uri-list;class=java.lang.String");
			_URIListDataflavorCharArray = new DataFlavor(
					"text/uri-list;class=\"[C\"");
		} catch (ClassNotFoundException e) { // This would never happen,
			// java.lang.String is always
			// present
			e.printStackTrace();
			_URIListDataflavorString = null;
		}
		assert (_URIListDataflavorString != null);
		assert (_URIListDataflavorCharArray != null);

	}

	/**
	 * Adds a custom importer. If an importer competes with another importer for
	 * handling the same file types, the importer that was added first will have
	 * first serve.
	 * 
	 * @param importer
	 *            The importer to add. Must not be null
	 * 
	 * @throws NullPointerException
	 *             if importer is null.
	 */
	public void addCustomFileImporter(FileImporter importer) {
		if (importer == null)
			throw new NullPointerException("importer");
		if (!_customFileImporters.contains(importer))
			_customFileImporters.add(importer);

	}

	/**
	 * Removes a custom importer.
	 * 
	 * @param importer
	 *            The importer to remove.
	 * 
	 * @throws NullPointerException
	 *             if importer is null.
	 */
	public void removeCustomFileImporter(FileImporter importer) {
		if (importer == null)
			throw new NullPointerException("importer");
		_customFileImporters.remove(importer);
	}

	@Override
	public boolean canImport(TransferSupport support) {

		if (!support.isDrop()) {
			return false;
		}

		// we only import Strings
		if (support.isDataFlavorSupported(DataFlavor.stringFlavor)
				|| support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
				|| support.isDataFlavorSupported(_URIListDataflavorString)
				|| support.isDataFlavorSupported(_URIListDataflavorCharArray)) {

			// check if the source actions (a bitwise-OR of supported actions)
			// contains the COPY action
			boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;
			if (copySupported) {
				support.setDropAction(COPY);
				return true;
			}
		}

		// Reject transfer
		return false;
	}

	@Override
	public boolean importData(TransferSupport support) {

		if (!canImport(support) || DisplayIO.getCurrentFrame() == null)
			return false;

		// Get the drop location of where to lot the import
		DropLocation location = support.getDropLocation();

		// Covert it into expeditee space
		Point expediteeDropPoint = location.getDropPoint();

		try {

			// The list of data flavors are ordered by most rich to least ..
			// keep trying until first
			// data flavor recognized.
			for (DataFlavor df : support.getTransferable()
					.getTransferDataFlavors()) {

				System.out.println(df);

				if (df == DataFlavor.stringFlavor) { // import as text item

					String str = (String) support.getTransferable()
							.getTransferData(DataFlavor.stringFlavor);

					if (str != null && str.length() > 0) {
						importString(str, expediteeDropPoint);
						return true;
					}

					// Usually Windows and MAC enviroments
				} else if (df == DataFlavor.javaFileListFlavor
						|| df.getSubType().equals("x-java-file-list")) { // Windows
					// has
					// other
					// random
					// types...

					List<? extends File> files = (List<? extends File>) support
							.getTransferable().getTransferData(
									DataFlavor.javaFileListFlavor);

					importFileList(files, expediteeDropPoint);

					return true;

					// Usually GNOME and KDE enviroments
				} else if (df.equals(_URIListDataflavorString)) {

					String data = (String) support.getTransferable()
							.getTransferData(_URIListDataflavorString);

					List<File> files = textURIListToFileList(data);

					importFileList(files, expediteeDropPoint);

					return true;

				} else if (df.equals(_URIListDataflavorCharArray)) {

					char[] data = (char[]) support.getTransferable()
							.getTransferData(_URIListDataflavorCharArray);

					String uriString = new String(data);

					List<File> files = textURIListToFileList(uriString);

					importFileList(files, expediteeDropPoint);

					return true;
				}
			}

		} catch (UnsupportedFlavorException e) {
			MessageBay
					.displayMessage("Drag and drop for that type of data is not supported");
		} catch (IOException e) {
			e.printStackTrace();
			MessageBay.displayMessage("Failed to import data in Expeditee");
		}

		return false;
	}

	/**
	 * Imports a string into expeditee's current frame
	 * 
	 * @param text
	 *            The text content.
	 * 
	 * @param expediteeDropPoint
	 *            The location in the current ecpeditee frame of where to drop
	 *            the text item.
	 */
	public static Text importString(String text, Point expediteeDropPoint) {

		assert (DisplayIO.getCurrentFrame() != null);
		assert (text != null && text.length() > 0);

		Text importedTextItem = new Text(DisplayIO.getCurrentFrame()
				.getNextItemID(), text);
		importedTextItem.setPosition(expediteeDropPoint);

		DisplayIO.getCurrentFrame().addItem(importedTextItem);
		FrameGraphics.requestRefresh(true);

		return importedTextItem;
	}

	public void importFileList(List<? extends File> files,
			Point expediteeDropPoint) throws IOException {

		Point currentPoint = expediteeDropPoint.getLocation();

		for (File fileToImport : files) { // import files one by one

			Item lastItem = importFile(fileToImport, currentPoint);

			if (lastItem == null) {
				currentPoint.y += 30;
			} else {
				currentPoint.y += lastItem.getBoundsHeight();
			}
			// of the item that was created

			// TODO: Better placement strategy
			// if (currentPoint.y > (Browser._theBrowser.getHeight() - 20))
			// currentPoint.y = Browser._theBrowser.getHeight() - 20;
		}
	}

	/**
	 * Imports a file into expeditee.
	 * 
	 * @param f
	 *            The file to import.
	 * 
	 * @param expediteeDropPoint
	 * 
	 * @throws IOException
	 */
	public Item importFile(File f, Point expediteeDropPoint) throws IOException {
		assert (f != null);

		// Check for custom importers first. They get preference to standard
		// importing routines...
		Item lastCreatedItem;
		if (null == (lastCreatedItem = performFileImport(_customFileImporters,
				f, expediteeDropPoint))) {

			// Standard file importing
			lastCreatedItem = performFileImport(_standardFileImporters, f,
					expediteeDropPoint);

		}
		return lastCreatedItem;
	}

	private Item performFileImport(List<FileImporter> importers, File f,
			Point expediteeDropPoint) throws IOException {

		for (FileImporter fi : importers) {
			Item lastCreated = fi.importFile(f, expediteeDropPoint);
			if (lastCreated != null)
				return lastCreated;
		}

		return null;
	}

	/**
	 * Code adopted from SUN - java BUG ID 4899516 workaround for KDE/GNOME
	 * Desktops
	 * 
	 * @param uriListString
	 *            Formatted according to RFC 2483
	 * 
	 * @return The list of FILES in the uriListString. Never null.
	 */
	private List<File> textURIListToFileList(String uriListString) {

		List<File> fileList = new LinkedList<File>();

		for (StringTokenizer st = new StringTokenizer(uriListString, "\r\n"); st
				.hasMoreTokens();) {

			String s = st.nextToken();

			if (s.startsWith("#")) {
				// the line is a comment (as per the RFC 2483)
				continue;
			}

			try {

				URI uri = new URI(s);
				File file = new File(uri);
				fileList.add(file);

			} catch (URISyntaxException e) {
				// malformed URI
			} catch (IllegalArgumentException e) {
				// the URI is not a valid 'file:' URI
			}
		}

		return fileList;
	}
}
