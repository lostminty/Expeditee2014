package org.expeditee.importer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.FrameKeyboardActions;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FrameUtils;
import org.expeditee.items.Item;
import org.expeditee.items.Text;
import org.expeditee.items.XRayable;

public class ImageImporter implements FileImporter {

	private static Collection<String> validImageTypes = null;

	public ImageImporter() {
		super();
		if (validImageTypes == null) {
			validImageTypes = new HashSet<String>();
			validImageTypes.add("png");
			validImageTypes.add("bmp");
			validImageTypes.add("jpg");
			validImageTypes.add("jpeg");
		}
	}

	public Item importFile(File f, Point location) throws IOException {

		if (location == null || f == null) {
			return null;
		}
		String fullPath = f.getAbsolutePath();
		int separator = fullPath.lastIndexOf('.');
		if (separator < 0)
			return null;
		String suffix = fullPath.substring(separator + 1).toLowerCase();

		if (!validImageTypes.contains(suffix)) {
			return null;
		}

		Color borderColor = null;
		float thickness = 0;
		String size = "";
		Collection<Item> enclosure = FrameUtils.getEnclosingLineEnds(location);
		if (enclosure != null) {
			for (Item i : enclosure) {
				if (i.isLineEnd() && i.isEnclosed()) {
					DisplayIO.getCurrentFrame().removeAllItems(enclosure);
					Rectangle rect = i.getEnclosedRectangle();
					size = " " + Math.round(rect.getWidth());
					location = new Point(rect.x, rect.y);
					thickness = i.getThickness();
					borderColor = i.getColor();
					break;
				}
			}
			FrameMouseActions.deleteItems(enclosure, false);
		}

		Text source = FrameDNDTransferHandler.importString("@i: " + fullPath
				+ size, location);
		source.setThickness(thickness);
		source.setBorderColor(borderColor);

		FrameKeyboardActions.Refresh();
		Collection<? extends XRayable> pictures = source.getEnclosures();
		if (pictures.size() == 0)
			return source;

		return source.getEnclosures().iterator().next();
	}
}
