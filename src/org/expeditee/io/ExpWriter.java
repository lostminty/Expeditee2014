package org.expeditee.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.agents.DefaultAgent;
import org.expeditee.gui.Frame;
import org.expeditee.items.Constraint;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.widgets.WidgetEdge;
import org.expeditee.stats.SessionStats;

/**
 * Writes a Frame out to a Expeditee format file.
 * 
 * @author jdm18
 * 
 */
public class ExpWriter extends DefaultFrameWriter {

	private ProxyWriter _writer = null;

	protected StringBuilder _stringWriter = null;

	private static final char TERMINATOR = 'Z';

	public ExpWriter() {
		super();
	}

	@Override
	public void initialise(Frame start, Writer writer) throws IOException {
		String name = start.getFramesetName().toLowerCase();

		if (_filename == null)
			_filename = start.getPath() + name + File.separator
					+ start.getNumber() + ExpReader.EXTENTION;

		_stringWriter = new StringBuilder();

		if (writer != null) {
			_writer = new ProxyWriter(writer);
			_output = writer.toString();
		} else if (_filename.equalsIgnoreCase(DefaultAgent.CLIPBOARD)) {
			_writer = new ProxyWriter(true);
			_filename = DefaultAgent.CLIPBOARD;
		} else {
			// Open an Output Stream Writer to set encoding
			OutputStream fout = new FileOutputStream(_filename);
			OutputStream bout = new BufferedOutputStream(fout);
			Writer out = new OutputStreamWriter(bout, "UTF-8");

			_writer = new ProxyWriter(out);
		}

		try {
			_FrameTags.remove('A');
			_ItemTags.put('S', Item.class.getMethod("getTypeAndID",
					new Class[] {}));
		} catch (Exception e) {

		}
	}

	/**
	 * Writes the given Frame (and all items it contains) to a Expeditee file.
	 * Note: File path and location must be set before calling this or it will
	 * do nothing.
	 * 
	 * @param frame
	 *            The Frame to write out to the file.
	 * @throws IOException
	 *             Any exceptions occured by the BufferedWriter.
	 */
	public void outputFrame(Frame frame) throws IOException {
		if (_writer == null)
			return;

		writeHeader(frame);

		// write each item in the frame
		for (Item i : frame.getItemsToSave()) {
			assert (!(i instanceof Line));
			writeItem(i);
		}

		// write any lines or constraints
		writeTerminator();
		writeLineData();
		writeTerminator();
		writeConstraintData();
		writeTerminator();
		writeLine(SessionStats.getFrameEventList(frame));

		return;
	}

	private void writeHeader(Frame toWrite) throws IOException {
		Iterator<Character> it = _FrameTags.keySet().iterator();
		Object[] param = {};

		while (it.hasNext()) {
			Character tag = it.next();
			try {
				Object o = _FrameTags.get(tag).invoke(toWrite, param);
				o = Conversion.ConvertToExpeditee(_FrameTags.get(tag), o);
				if (o != null) {
					writeLine(tag.toString(), o.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		writeTerminator();
	}

	private void writeLine(String tag, String line) throws IOException {
		writeLine(tag + " " + line);
	}

	protected void writeTerminator() throws IOException {
		writeLine(TERMINATOR + "\n");
	}

	// writes the given line out to the file
	protected void writeLine(String line) throws IOException {
		// do not write empty lines
		if (line == null)
			return;

		String toWrite = line + "\n";

		_writer.write(toWrite);
		_stringWriter.append(toWrite);
	}

	// writes the given Item out to the file
	// This method is not used to write out LINE items
	public void writeItem(Item item) throws IOException {
		if (_writer == null)
			return;

		writeItemAlways(item);
	}
	
	protected void writeItemAlways(Item item) throws IOException {
		if (item.isLineEnd())
			writeLineEnd(item);
		else if (!(item instanceof Line))
			writeClass(item);
		// lines are saved at the end of the file
		// So dont worry about them in here
//		else
//			System.out.println("Unknown Item: " + item.getID() + " (" + item.getClass().getName() + ")");

		writeLine("");
	}

	private List<Item> _lineEnds = new LinkedList<Item>();

	// Writes out a LineEnd to the file
	protected void writeLineEnd(Item point) throws IOException {
		_lineEnds.add(point);
		writeClass(point);
	}

	// writes out all lines to the file
	protected void writeLineData() throws IOException {
		List<Line> seen = new LinkedList<Line>();

		// loop through all points stored
		for (int i = 0; i < _lineEnds.size(); i++) {
			List<Line> lines = _lineEnds.get(i).getLines();

			// if this point is part of one or more lines
			if (lines != null && lines.size() > 0) {
				for (Line line : lines) {

					// Brook: widget edges are not saved
					if (line instanceof WidgetEdge) {
						seen.add(line);
						continue;
					}

					// only output new lines that have not yet been output
					if (!seen.contains(line)) {
						writeLine("L", line.getID() + " " + line.getLineType());
						writeLine("s", line.getLineEnds());
						writeLine("");

						// add this line to the list of lines that have been
						// seen
						seen.add(line);
					}
				}
			}
		}
	}

	// writes out any constraints to the file
	protected void writeConstraintData() throws IOException {
		// outputs any constraints the points have

		// loop through all the points
		while (_lineEnds.size() > 0) {
			Item i = _lineEnds.get(0);

			// if there are any constraints to write
			if (i.getConstraints() != null) {
				List<Constraint> constraints = i.getConstraints();

				// do not write constraints that have already been
				// written
				for (Constraint c : constraints) {
					if (_lineEnds.contains(c.getStart())
							&& _lineEnds.contains(c.getEnd())) {
						writeLine("C", c.getID() + " " + c.getType());
						writeLine("s", c.getLineEnds());
						writeLine("");
					}

				}
			}
			// remove the point from the list
			_lineEnds.remove(0);
		}
	}

	@Override
	protected String finaliseFrame() throws IOException {
		_writer.flush();
		_writer.close();
		_writer = null;

		return "Frame successfully written to " + _filename;
	}

	private void writeClass(Object toWrite) throws IOException {
		Iterator<Character> it = _ItemTags.keySet().iterator();
		Object[] param = {};

		while (it.hasNext()) {
			Character tag = it.next();
			Method toRun = _ItemTags.get(tag);
			Class<?> declarer = toRun.getDeclaringClass();
			if (declarer.isAssignableFrom(toWrite.getClass())) {
				try {
					Object o = toRun.invoke(toWrite, param);
					o = Conversion.ConvertToExpeditee(toRun, o);
					if (o != null) {
						if (o instanceof List) {
							for (Object line : (List) o)
								writeLine(tag.toString(), line.toString());
						} else
							writeLine(tag.toString(), o.toString());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Gets a string representation of the frame file contents.
	 */
	public String getFileContents() {
		return _stringWriter.toString();
	}
}
