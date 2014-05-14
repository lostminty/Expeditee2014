package org.expeditee.io;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Text;
import org.expeditee.settings.UserSettings;

public abstract class AbstractHTMLWriter extends DefaultTreeWriter {

	private static final String INDENT = "   ";

	private static final String FILE_FOLDER_SUFFIX = "_files";

	@Override
	protected void initialise(Frame start, Writer writer) throws IOException {
		_format = ".html";
		super.initialise(start, writer);

		// Clear the filesFolder
		String filesFolderName = FrameIO.EXPORTS_DIR + getFilesFolder();
		if (filesFolderName.length() > 0) {
			File filesFolder = new File(filesFolderName);
			for (File f : filesFolder.listFiles()) {
				f.delete();
			}
			filesFolder.delete();
			_filesFolderName = null;
		}

		_writer.write("<html>" + ItemWriter.NEW_LINE);
		_writer.write("<head>" + ItemWriter.NEW_LINE);
		_writer
				.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
		_writer.write("<title>" + start.getTitle());
		_writer.write("</title>" + ItemWriter.NEW_LINE);

		// Write in the style for the headers and body text
		_writer.write("<style>" + ItemWriter.NEW_LINE);
		_writer.write("<!--" + ItemWriter.NEW_LINE);

		// Go through all the styles
		List<Text> style = UserSettings.Style.get();

		writeStyleInfo(_writer, "P", style.get(0));
		for (int i = 1; i < style.size(); i++) {
			writeStyleInfo(_writer, "H" + i, style.get(i));
		}
		_writer.write("-->" + ItemWriter.NEW_LINE);
		_writer.write("</style>" + ItemWriter.NEW_LINE);

		_writer.write("</head>" + ItemWriter.NEW_LINE);
		_writer.write("<body>" + ItemWriter.NEW_LINE);
	}

	private void writeStyleInfo(ProxyWriter writer, String styleName, Text style)
			throws IOException {
		if (style == null)
			return;

		Font font = style.getPaintFont();
		if (font == null)
			return;

		writer.write(styleName + " { font-family: "
				+ Conversion.getCssFontFamily(font.getFamily()));

		// writer.write("; font-size: " + Math.round(style.getSize()) + "px");

		if (font.isBold()) {
			writer.write("; font-weight: bold");
		} else {
			writer.write("; font-weight: normal");
		}

		if (font.isItalic()) {
			writer.write("; font-style: italic");
		} else {
			writer.write("; font-style: normal");
		}
		Color c = style.getBackgroundColor();
		if (c != null) {
			writer.write("; background-color: " + Conversion.getCssColor(c));
		}
		c = style.getColor();
		if (c != null) {
			writer.write("; color: " + Conversion.getCssColor(c));
		}

		c = style.getBorderColor();
		if (c != null) {
			writer.write("; outline-color: " + Conversion.getCssColor(c));
			writer.write("; outline-style: solid");
		}

		writer.write("}" + ItemWriter.NEW_LINE);

	}

	private String _filesFolderName = null;

	protected String getFilesFolder() {
		if (_filesFolderName == null) {
			_filesFolderName = _filename.substring(0, _filename
					.lastIndexOf('.'))
					+ FILE_FOLDER_SUFFIX;
			File dir = new File(_filesFolderName);
			_filesFolderName = dir.getName();
			if (!dir.exists()) {
				try {
					dir.mkdirs();
				} catch (Exception e) {
					MessageBay.errorMessage("Could not create folder: "
							+ dir.getName());
					_filesFolderName = null;
					return "";
				}
			}
		}

		return _filesFolderName + File.separator;
	}

	@Override
	protected String finaliseTree() throws IOException {
		_writer.write("</body>" + ItemWriter.NEW_LINE);
		return super.finaliseTree();
	}

	protected void indent() throws IOException {
		for (int i = 0; i < getIndent(); i++)
			_writer.write(INDENT);
	}
}
