package org.expeditee.io;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.items.Circle;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.settings.UserSettings;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

/**
 * 
 * @author root
 * 
 */
public class PdfFramesetWriter extends DefaultFramesetWriter {

	private Document _pdfDocument;

	private PdfWriter _pdfWriter;

	private boolean _showFrameNames;

	public PdfFramesetWriter(long firstFrame, long maxFrame,
			boolean showFrameNames) {
		super(firstFrame, maxFrame);
		Dimension d = FrameGraphics.getMaxSize();
		_pdfDocument = new Document(new Rectangle(d.width, d.height));
		_showFrameNames = showFrameNames;
	}

	public PdfFramesetWriter() {
		this(1, Long.MAX_VALUE, false);
	}

	@Override
	protected String getFileName(Frame start) {
		String fileName = start.getFramesetName();
		if (_maxFrame < Long.MAX_VALUE) {
			fileName += "_" + _firstFrame + "-" + _maxFrame;
		}
		return fileName;
	}

	@Override
	protected void initialise(Frame start, Writer writer) throws IOException {
		_format = ".pdf";
		super.initialise(start, writer);

		try {
			_pdfWriter = PdfWriter.getInstance(_pdfDocument,
					new FileOutputStream(_filename));
			_pdfDocument.open();
			_pdfDocument.addCreationDate();
			_pdfDocument.addAuthor(UserSettings.UserName.get());
			_pdfDocument.addCreator("Expeditee");
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
	}

	@Override
	protected void writeTitle(Text toWrite, List<Item> items)
			throws IOException {
		_pdfDocument.addTitle(toWrite.getText());
		writeText(toWrite);
	}

	protected void writeStartFrame(Frame starting) throws IOException {
		// TODO color in the frame background color

		super.writeStartFrame(starting);
	}

	protected void writeEndFrame(Frame ending) throws IOException {
		if (_showFrameNames)
			writeText((Text) ending.getNameItem());
		super.writeEndFrame(ending);
		// Move to the next page
		_pdfDocument.newPage();
	}

	@Override
	protected String finalise() throws IOException {
		_pdfDocument.close();
		return super.finalise();
	}

	@Override
	protected void writeText(Text text) throws IOException {
		PdfContentByte cb = _pdfWriter.getDirectContent();
		// we tell the ContentByte we're ready to draw text
		cb.beginText();
		Font font = FontFactory.getFont(
				Conversion.getPdfFont(text.getFamily()), text.getSize(), text
						.getPaintFont().getStyle(), text.getPaintColor());
		cb.setFontAndSize(font.getBaseFont(), text.getSize());
		// cb.setColorStroke(text.getPaintColor());
		cb.setColorFill(text.getPaintColor());

		// we draw some text on a certain position
		cb.setTextMatrix(text.getX(), _pdfWriter.getPageSize().getHeight()
				- text.getY());
		List<String> textLines = text.getTextList();
		// cb.showText(textLines.get(0));
		for (int i = 0; i < textLines.size(); i++) {
			cb.showText(textLines.get(i));
			cb.moveText(0, -text.getLineHeight());
		}
		// we tell the contentByte, we've finished drawing text
		cb.endText();
	}

	@Override
	protected void writePicture(Picture pic) throws IOException {
		Image image = pic.getCroppedImage();
		if (image == null)
			return;
		try {
			PdfContentByte cb = _pdfWriter.getDirectContent();
			com.lowagie.text.Image iTextImage = com.lowagie.text.Image
					.getInstance(image, null);
			iTextImage.setAbsolutePosition(pic.getX(), _pdfWriter.getPageSize()
					.getHeight()
					- pic.getY() - pic.getHeight());
			cb.addImage(iTextImage);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void writeLine(Line lineEnd) throws IOException {
		PdfContentByte cb = _pdfWriter.getDirectContent();
//		Color fill = lineEnd.getFillColor();
//		if (fill != null) {
//			cb.setColorFill(fill);
//		}
		Graphics2D g = cb.createGraphicsShapes(
				FrameGraphics.getMaxSize().width,
				FrameGraphics.getMaxSize().height);
		lineEnd.paint(g);
//		if (fill != null) {
//			g.setPaint(fill);
//		}
//		lineEnd.paintFill(g);
	}

	@Override
	protected Collection<Item> getItemsToWrite(Frame toWrite) {
		return toWrite.getVisibleItems();
	}
	
	@Override
	protected void writeCircle(Circle toWrite) throws IOException {
		PdfContentByte cb = _pdfWriter.getDirectContent();
		Graphics2D g = cb.createGraphicsShapes(
				FrameGraphics.getMaxSize().width,
				FrameGraphics.getMaxSize().height);
		toWrite.paint(g);
	}
	
	
	@Override
	protected void writeWidget(InteractiveWidget toWrite) throws IOException {
		PdfContentByte cb = _pdfWriter.getDirectContent();
		Graphics2D g = cb.createGraphicsShapes(
				FrameGraphics.getMaxSize().width,
				FrameGraphics.getMaxSize().height);
		toWrite.paint(g);
	}
}
