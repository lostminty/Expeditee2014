package org.expeditee.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.agents.DefaultAgent;
import org.expeditee.gui.Frame;
import org.expeditee.items.Constraint;
import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.stats.SessionStats;

/**
 * Writes a Frame out to a KMS format file.
 * 
 * @author jdm18
 * 
 */
public class KMSWriter extends DefaultFrameWriter {

	private ProxyWriter _writer = null;

	private StringBuilder _stringWriter = null;

	private static final char TERMINATOR = 'Z';

	private static final char DELIMITER = '+';

	public KMSWriter() {
		super();
	}

	public void initialise(Frame start, Writer writer) throws IOException {
		String name = start.getFramesetName().toLowerCase();

		if (_filename == null)
			_filename = start.getPath() + name + File.separator + name + "."
					+ start.getNumber();

		_stringWriter = new StringBuilder();
		if (_filename.toLowerCase().equals(DefaultAgent.CLIPBOARD)) {
			_writer = new ProxyWriter(true);
			_filename = DefaultAgent.CLIPBOARD;
		} else{
//			 Open an Output Stream Writer to set encoding
			OutputStream fout = new FileOutputStream(_filename);
			OutputStream bout = new BufferedOutputStream(fout);
			Writer out = new OutputStreamWriter(bout, "UTF-8");
			_writer = new ProxyWriter(out);
		}
		
		try {
			_FrameTags.put('A', Frame.class.getMethod("getName", new Class[] {}));
			_ItemTags.put('S', Item.class.getMethod("getID", new Class[] {}));
		} catch (Exception e) {

		}
		// _writer = new BufferedWriter(new FileWriter(filepath.toLowerCase() +
		// filename.toLowerCase()));
	}

	/**
	 * Writes the given Frame (and all items it contains) to a KMS file. Note:
	 * File path and location must be set before calling this or it will do
	 * nothing.
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

		// write out each Item in the Frame.
		for (Item i : frame.getItemsToSave()) {
			writeItem(i);
		}

		// write any lines or constraints
		writeLineData();
		writeConstraintData();

		return;
	}

	private void writeHeader(Frame toWrite) throws IOException {
		Iterator<Character> it = _FrameTags.keySet().iterator();
		Object[] param = {};

		// Write the version tag
		writeLine("v", "22");

		while (it.hasNext()) {
			Character tag = it.next();
			try {
				Object o = _FrameTags.get(tag).invoke(toWrite, param);
				o = Conversion.ConvertToExpeditee(_FrameTags.get(tag), o);
				if (o != null) {
					writeLine(tag.toString(), o.toString());
				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void writeLine(String tag, String line) throws IOException {
		writeLine(DELIMITER + tag + DELIMITER + " " + line);
	}

	private void writeTerminator() throws IOException {
		writeLine("" + DELIMITER + TERMINATOR + DELIMITER);
	}

	// writes the given line out to the file
	private void writeLine(String line) throws IOException {
		// do not write empty lines
		if (line == null)
			return;

		String toWrite = line + "\n";

		_writer.write(toWrite);
		_stringWriter.append(toWrite);
	}

	// writes the given Item out to the file
	public void writeItem(Item item) throws IOException {
		if (_writer == null)
			return;

		writeLine("");
		if (item.getLines().size() > 0)
			writePoint(item);
		else 
			writeClass(item);
	}

	private List<Item> _points = new LinkedList<Item>();

	// Writes out a Dot to the file
	protected void writePoint(Item point) throws IOException {
		_points.add(point);
		writeClass(point);
	}

	// writes out all lines to the file
	private void writeLineData() throws IOException {
		List<Line> seen = new LinkedList<Line>();

		// loop through all points stored
		for (int i = 0; i < _points.size(); i++) {
			List<Line> lines = _points.get(i).getLines();

			// if this point is part of one or more lines
			if (lines != null && lines.size() > 0) {
				for (Line line : lines) {

					// only output new lines that have not yet been output
					if (!seen.contains(line)) {
						writeLine("L", line.getID() + " " + line.getLineType());
						writeLine("s", line.getLineEnds());

						// add this line to the list of lines that have been
						// seen
						seen.add(line);
					}

				}
			}
		}
	}

	// writes out any constraints to the file
	private void writeConstraintData() throws IOException {
		// outputs any constraints the points have

		// loop through all the points
		while (_points.size() > 0) {
			Item i = _points.get(0);

			if (i instanceof Dot) {
				Item p = (Item) i;

				// if there are any constraints to write
				if (p.getConstraints() != null) {
					List<Constraint> constraints = p.getConstraints();

					// do not write constraints that have already been
					// written
					for (Constraint c : constraints) {
						if (_points.contains(c.getStart())
								&& _points.contains(c.getEnd())) {
							writeLine("C", c.getID() + " " + c.getType());
							writeLine("s", c.getLineEnds());
						}

					}
				}
			}
			// remove the point from the list
			_points.remove(0);
		}
	}

	@Override
	protected String finaliseFrame() throws IOException {
		writeTerminator();

		writeLine(SessionStats.getFrameEventList());

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
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
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
