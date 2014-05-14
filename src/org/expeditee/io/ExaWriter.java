package org.expeditee.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.gui.AttributeUtils;
import org.expeditee.gui.AttributeUtils.Attribute;
import org.expeditee.gui.Frame;
import org.expeditee.items.Constraint;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.widgets.WidgetEdge;

/**
 * Experimental format which is designed to be more readable
 * NON FUNCTIONAL DUE TO CHANGES TO AttributeUtils
 * 
 * @author jts21
 *
 */
public class ExaWriter implements FrameWriter {

	private StringBuilder sb = new StringBuilder();
	private Writer _writer;
	private String _output;
	private String _frameName;
	private List<Item> _lineEnds = new LinkedList<Item>();
	private boolean _running = false;

	public void setOutputLocation(String filename) {
		_output = filename;
	}
	
	public String getFileContents() {
		return sb.toString();
	}
	
	public String writeFrame(Frame frame) throws IOException {
		if(_output == null) {
			_frameName = frame.getPath() + frame.getFramesetName().toLowerCase() + File.separator
					+ frame.getNumber() + ExaReader.EXTENTION;
		}
		return writeFrame(frame, null);
	}
	
	public String writeFrame(Frame frame, Writer writer) throws IOException {
		
		_running = true;
		boolean setWriter = true;
		
		if(writer != null) {
			_writer = writer;
			setWriter = false;
		} else if(_output != null) {
			try {
				_writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(_output)), "UTF-8");
			} catch (Exception e) {
				System.err.println("Couldn't open " + _output + " for writing");
				_writer = null;
			}
		} else if(_frameName != null) {
			try {
				_writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(_frameName)), "UTF-8");
			} catch (Exception e) {
				System.err.println("Couldn't open " + _frameName + " for writing");
				_writer = null;
			}
		}
		
		AttributeUtils.ensureReady();
		
		writeHeader(frame);
		propertyPrefix = "\t";
		writeItems(frame);
		writeLines();
		writeConstraints();
		
		_writer.flush();
		if(setWriter) {
			_writer.close();
		}
		_running = false;
		
		return "Frame " + frame.getName() + " exported" + (writer != null ? writer.toString() : (_output != null ? _output : "")); 
	}

	private void writeHeader(Frame frame) throws IOException {
		// TODO: NON FUNCTIONAL DUE TO CHANGES TO AttributeUtils
//		for (String prop : AttributeUtils._FrameAttrib.keys) {
//			Attribute a = AttributeUtils._FrameAttrib.get(prop);
//			if(a == null || a.saveGetter == null) {
//				continue;
//			}
//			if(a.saveGetter.getDeclaringClass().isAssignableFrom(frame.getClass())) {
//				try {
//					Object o = a.saveGetter.invoke(frame);
//					o = Conversion.ConvertToExpeditee(a.saveGetter, o);
//					if (o != null) {
//						writeProperty(a.displayName, o);
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}
	}
	
	private void writeItems(Frame frame) throws IOException {
		writeLine();
		for (Item item : frame.getItemsToSave()) {
			if (item.isLineEnd()) {
				_lineEnds.add(item);
			}
			if(item instanceof Line) {
				continue;
			}
			writeLine("Item " + item.getClass().getName() + " " + item.getID());
			for (String prop : AttributeUtils._Attrib.keys) {
				// TODO: NON FUNCTIONAL DUE TO CHANGES TO AttributeUtils
//				Attribute a = AttributeUtils._Attrib.get(prop);
//				if(a == null || a.saveGetter == null) {
//    				continue;
//    			}
//				Class<?> declarer = a.saveGetter.getDeclaringClass();
//				if (declarer.isAssignableFrom(item.getClass())) {
//					try {
//						Object o = a.saveGetter.invoke(item);
//						o = Conversion.ConvertToExpeditee(a.saveGetter, o);
//						if (o != null) {
//							if (o instanceof List) {
//								for (Object line : (List<?>) o) {
//									writeProperty(a.displayName, line);
//								}
//							} else {
//								writeProperty(a.displayName, o);
//							}
//						}
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
			}
		}
	}
	
	private void writeLines() throws IOException {
		writeLine();
		List<Line> seen = new LinkedList<Line>();

		// loop through all points stored
		for(Item item : _lineEnds) {
			List<Line> lines = item.getLines();

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
						writeLine("Line " + line.getID());
						writeProperty("Start", line.getStartItem().getID());
						writeProperty("End", line.getEndItem().getID());

						// add this line to the list of lines that have been seen
						seen.add(line);
					}
				}
			}
		}
	}
	
	private void writeConstraints() throws IOException {
		
		writeLine();
		while(!_lineEnds.isEmpty()) {
			Item item = _lineEnds.get(0);

			// if there are any constraints to write
			List<Constraint> constraints = item.getConstraints();
			if (constraints != null) {

				// do not write constraints that have already been written
				for (Constraint c : constraints) {
					if (_lineEnds.contains(c.getStart()) && _lineEnds.contains(c.getEnd())) {
						writeLine("Constraint " + c.getType() + " " + c.getID());
						writeProperty("Start", c.getStart().getID());
						writeProperty("End", c.getEnd().getID());
					}
				}
			}
			
			_lineEnds.remove(0);
		}
	}
	
	private String propertyPrefix = "";
	
	private void writeProperty(String prop, Object value) throws IOException {
		writeLine(propertyPrefix + prop + " " + value.toString());
	}

	private void writeLine() throws IOException {
		write("\n");
	}
	
	private void writeLine(String line) throws IOException {
		write(line + "\n");
	}

	private void write(String data) throws IOException {
		// System.out.print(data);
		if(_writer != null) {
			_writer.write(data);
		}
		sb.append(data);
	}
	
	public boolean isRunning() {
		return _running;
	}

	public void stop() {
		// surely we'll stop at some point in the future
	}
}
