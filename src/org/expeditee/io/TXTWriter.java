package org.expeditee.io;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Text;

public class TXTWriter extends DefaultTreeWriter {

	private boolean _join = false;

	private int _indent = 0;

	@Override
	protected void initialise(Frame start, Writer writer) throws IOException {
		_format = ".txt";
		super.initialise(start, writer);
	}

	@Override
	protected void writeStartFrame(Frame toParse) throws IOException {
		if (ItemUtils.ContainsTag(toParse.getItems(), "@join"))
			_join = !_join;

		if (ItemUtils.ContainsTag(toParse.getItems(), "@indent"))
			_indent++;

		super.writeStartFrame(toParse);
	}

	@Override
	protected void writeTitle(Text title, List<Item> items) throws IOException {
		int indent = 0;

		if (_indent > 0)
			indent = _indent - 1;

		for (String s : title.getTextList()) {

			for (int i = 0; i < indent; i++)
				_writer.write("\t");

			_writer.write(s);
			_writer.write(ItemWriter.NEW_LINE);
		}

		if (!_join)
			_writer.write(ItemWriter.NEW_LINE);
	}

	@Override
	protected void writeEndFrame(Frame toParse) throws IOException {
		if (ItemUtils.ContainsTag(toParse.getItems(), "@indent"))
			if (_indent > 0)
				_indent--;

		if (ItemUtils.ContainsTag(toParse.getItems(), "@join"))
			_join = !_join;

		_writer.write(ItemWriter.NEW_LINE);
	}

	@Override
	protected void resumeFrame(Frame resuming) {
		_join = ItemUtils.ContainsTag(resuming.getItems(), "@join");
	}

	@Override
	protected void writeText(Text text) throws IOException {
		String s = text.getText();

		for (int i = 0; i < _indent; i++)
			_writer.write("\t");

		_writer.write(s);
		_writer.write(ItemWriter.NEW_LINE);

		if (!_join)
			_writer.write(ItemWriter.NEW_LINE);
	}

	@Override
	protected void writeAnnotationText(Text toWrite) throws IOException {
		if (toWrite.startsWith("@BlankLine"))
			_writer.write(ItemWriter.NEW_LINE);
	}
}
