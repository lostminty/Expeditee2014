package org.expeditee.io;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Text;

public class TEXWriter extends DefaultTreeWriter {

	// may be needed for sectioning commands
	private Text _title = null;

	@Override
	protected void initialise(Frame start, Writer writer) throws IOException {
		_format = ".tex";
		super.initialise(start, writer);
	}

	protected void writeTitle(Text title, List<Item> items) throws IOException {
		_title = title;

		Text text = ItemUtils.FindTag(items, "@TexSection");

		if (text != null) {
			String first = text.getFirstLine();
			int ind = first.indexOf(":");

			if (ind > 0) {
				String command = first.substring(ind + 1).trim().toLowerCase();

				_writer.write("\\" + command + "{");

				List<String> titleLines = _title.getTextList();
				for (int i = 0; i < titleLines.size() - 1; i++) {
					_writer.write(titleLines.get(i));
					_writer.write(ItemWriter.NEW_LINE);
				}

				_writer.write(titleLines.get(titleLines.size() - 1));

				_writer.write("}" + ItemWriter.NEW_LINE);
			}
		}
	}

	@Override
	protected void writeStartLink(Item linker) throws IOException {
		// only output text items
		if (!(linker instanceof Text))
			return;

		Text text = (Text) linker;
		List<String> toWrite = text.getTextList();

		String first = toWrite.get(0);
		TexEnvironment te = new TexEnvironment(first);

		if (te.isValid()) {
			if (te.hasComment())
				_writer.write("%" + te.getComment() + ItemWriter.NEW_LINE);
			_writer.write("\\begin{" + te.getName() + "}" + ItemWriter.NEW_LINE);
		}
	}
	
	@Override
	protected void writeEndLink(Item linker) throws IOException {
		// only output text items
		if (!(linker instanceof Text))
			return;

		Text text = (Text) linker;
		List<String> toWrite = text.getTextList();

		String first = toWrite.get(0);
		TexEnvironment te = new TexEnvironment(first);

		if (te.isValid()) {
			_writer.write("\\end{" + te.getName() + "}" + ItemWriter.NEW_LINE);
		}

	}

	@Override
	protected void writeText(Text text) throws IOException {
		for (String s : text.getTextList()) {
			_writer.write(s);
			_writer.write(ItemWriter.NEW_LINE);
		}
	}

	@Override
	protected void writeAnnotationText(Text text) throws IOException {

	}
}

