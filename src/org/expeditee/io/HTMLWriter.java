package org.expeditee.io;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import org.expeditee.actions.Misc;
import org.expeditee.gui.FrameIO;
import org.expeditee.items.FramePicture;
import org.expeditee.items.Item;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;

public class HTMLWriter extends AbstractHTMLWriter {

	@Override
	protected void writeTitle(Text toWrite, List<Item> items)
			throws IOException {
		int indent = getIndent();
		if (indent == 0)
			return;

		String heading = toWrite.getText();
		String tag = "h" + indent;
		// indenting for tag
		indent();
		_writer.write("<" + tag + ">" + heading + "</" + tag + ">"
				+ ItemWriter.NEW_LINE);
	}

	@Override
	protected void writeText(Text text) throws IOException {
		indent();
		_writer.write("<p>");
		_writer.write(text.getText().replace('\n', ' '));
		_writer.write("</p>" + ItemWriter.NEW_LINE);
	}

	@Override
	protected void writePicture(Picture pic) throws IOException {
		// TODO handle different colored @f's of the same frame
		// TODO handle cropped images
		Image image = null;

		if (pic instanceof FramePicture || pic.isCropped()) {
			image = pic.getImage();
			// Crop the image
			BufferedImage bufferedImage = new BufferedImage(pic
					.getUnscaledWidth(), pic.getUnscaledHeight(),
					BufferedImage.TYPE_INT_ARGB);
			bufferedImage.getGraphics().drawImage(image, 0, 0,
					pic.getUnscaledWidth(), pic.getUnscaledHeight(),
					pic.getStart().x, pic.getStart().y, pic.getEnd().x,
					pic.getEnd().y, null);
			image = bufferedImage;
		} else {
			image = pic.getImage();
		}

		String filesFolder = getFilesFolder();
		String fileName;
		// If its a bufferedImage then just write it out to the files directory
		// This means it is probably a FrameImage
		if (image instanceof BufferedImage) {
			String link = pic.getAbsoluteLink();
			// Account for the possiblitly of an unlinked buffered image
			fileName = link == null ? ("Image" + pic.getID()) : link;
			fileName = Misc.SaveImage((BufferedImage) image, "PNG",
					FrameIO.EXPORTS_DIR + filesFolder, fileName);
		} else {// It is a normal Image stored somewhere
			fileName = pic.getName();

			String oldImageName = FrameIO.IMAGES_PATH + fileName;
			String newImageName = FrameIO.EXPORTS_DIR + filesFolder + fileName;
			try {
				FrameIO.copyFile(oldImageName, newImageName);
			} catch (Exception e) {
				filesFolder = "";
			}
			if (filesFolder.equals("")) {
				filesFolder = "../" + FrameIO.IMAGES_FOLDER;
			}
		}
		indent();
		StringBuffer imageTag = new StringBuffer("<img src=\"");
		imageTag.append(filesFolder).append(fileName);
		imageTag.append("\" height=").append(pic.getHeight());
		imageTag.append(" width=").append(pic.getWidth());
		imageTag.append(" border=1>");
		_writer.write(imageTag.toString());
		_writer.write(ItemWriter.NEW_LINE);
	}
}
