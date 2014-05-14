package org.expeditee.io;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.items.Item;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;

public class oldHTMLWriter extends DefaultTreeWriter {

	private int _indent = 0;

	private static final String INDENT = "   ";

	@Override
	protected void initialise(Frame start, Writer writer) throws IOException {
		_format = ".html";
		super.initialise(start, writer);
	}

	@Override
	protected void writeStartLink(Item linker) throws IOException {
		// only output text items
		if (!(linker instanceof Text)) {
			_indent++;
			return;
		}

		Text text = (Text) linker;
		List<String> toWrite = text.getTextList();

		String first = toWrite.get(0);
		int ind = first.indexOf(":");

		if (ind > 0) {
			String tag = first.substring(0, ind);
			String remain = first.substring(ind + 1);

			if (remain.length() > 0) {
				// indenting for comments
				indent();
				_writer.write("<!-- " + remain);

				for (int i = 1; i < toWrite.size(); i++)
					_writer.write(ItemWriter.NEW_LINE + toWrite.get(i));

				_writer.write(" -->" + ItemWriter.NEW_LINE);
			}

			// indenting for tag
			indent();
			_writer.write("<" + tag + ">" + ItemWriter.NEW_LINE);
		}

		_indent++;
	}

	@Override
	protected void writeEndLink(Item linker) throws IOException {
		_indent--;

		// only output text items
		if (!(linker instanceof Text))
			return;

		Text text = (Text) linker;
		List<String> toWrite = text.getTextList();

		String first = toWrite.get(0);
		int ind = first.indexOf(":");

		if (ind > 0) {
			String tag = first.substring(0, ind);

			// indenting for tag
			indent();
			_writer.write("</" + tag + ">" + ItemWriter.NEW_LINE);
		}

	}

	@Override
	protected void writeText(Text text) throws IOException {
		for (String s : text.getTextList()) {

			indent();
			// check if the first word is proceeded by a :
			int firstColonIndex = s.indexOf(":");
			int firstSpaceIndex = s.indexOf(" ");
			if (firstColonIndex > 0 && firstColonIndex < firstSpaceIndex) {
				_writer.write("<" + s.substring(0, firstColonIndex) + ">");
				if (firstColonIndex < s.length())
					_writer.write(s.substring(firstColonIndex + 1));
				_writer.write("</" + s.substring(0, firstColonIndex) + ">");
			} else
				_writer.write(s);
			_writer.write(ItemWriter.NEW_LINE);
		}
	}

	private void indent() throws IOException {
		for (int i = 0; i < _indent; i++)
			_writer.write(INDENT);
	}

	@Override
	protected void writePicture(Picture pic) throws IOException {
		Text source = pic.getSource();

		String line = source.getFirstLine();
		line = line.substring(line.indexOf(":") + 1).trim();

		indent();

		_writer.write("<img src = '../images/" + line + "'>");
		_writer.write(ItemWriter.NEW_LINE);
	}
}
