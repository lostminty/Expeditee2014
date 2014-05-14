package org.expeditee.io;

import java.awt.Image;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.items.Item;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.settings.UserSettings;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Writes out a tree in a linear format.
 * 
 * @author root
 * 
 */
public class PDFWriter extends DefaultTreeWriter {

	private Document _pdfDocument;

	private Font _bodyFont;

	public PDFWriter() {
		assert(UserSettings.Style.get() != null);
		if (UserSettings.Style.get().size() > 0) {
			Text text = UserSettings.Style.get().get(0);
			_bodyFont = FontFactory.getFont(Conversion.getPdfFont(text
					.getFamily()), text.getSize(), text.getPaintFont()
					.getStyle(), text.getColor());
		}
		_pdfDocument = new Document();
	}

	@Override
	protected void initialise(Frame start, Writer writer) throws IOException {
		_format = ".pdf";
		super.initialise(start, writer);
		try {
			PdfWriter
					.getInstance(_pdfDocument, new FileOutputStream(_filename));
			_pdfDocument.open();
			_pdfDocument.addCreationDate();
			_pdfDocument.addAuthor(UserSettings.UserName.get());
			_pdfDocument.addCreator("Expeditee");
			_pdfDocument.addTitle(start.getTitle());
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
	}

	@Override
	protected void writeTitle(Text toWrite, List<Item> items)
			throws IOException {
		int indent = getIndent();
		if (indent == 0)
			return;

		if (indent < UserSettings.Style.get().size()) {
			String text = toWrite.getText();
			toWrite = UserSettings.Style.get().get(indent).getTemplateForm();
			toWrite.setText(text);
		}

		writeText(toWrite, false);
	}

	@Override
	protected String finaliseTree() throws IOException {
		_pdfDocument.close();
		return super.finaliseTree();
	}

	@Override
	protected void writeText(Text text) throws IOException {
		writeText(text, true);
	}

	protected void writeText(Text text, boolean bodyText) throws IOException {
		try {
			Font font = null;
			if (bodyText) {
				font = _bodyFont;
			}

			if (font == null) {
				font = FontFactory.getFont(Conversion.getPdfFont(text
						.getFamily()), text.getSize(), text.getPaintFont()
						.getStyle(), text.getColor());
			}
			
			_pdfDocument.add(new Paragraph(text.getText(), font));
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void writePicture(Picture pic) throws IOException {
		Image image = pic.getCroppedImage();
		try {
			_pdfDocument.add(com.lowagie.text.Image.getInstance(image, null));
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
