package org.expeditee.io;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PipedWriter;
import java.io.Writer;

public class ProxyWriter extends BufferedWriter {

	private StringBuffer _contents = null;

	public ProxyWriter(Writer out) {
		super(out);
	}

	public ProxyWriter(Writer out, int sz) {
		super(out, sz);
	}

	public ProxyWriter(boolean useClipboard) {
		// create a writer that does nothing
		super(new PipedWriter());
		_contents = new StringBuffer("");
	}

	@Override
	public void write(String s) throws IOException {
		if (_contents != null) {
			_contents.append(s);
		} else
			super.write(s);
	}

	@Override
	public void flush() throws IOException {
		if (_contents != null) {
			StringSelection selection = new StringSelection(_contents
					.toString());
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
					selection, null);
		} else
			super.flush();
	}

	@Override
	public void close() throws IOException {
		if (_contents == null)
			super.close();
	}
}
