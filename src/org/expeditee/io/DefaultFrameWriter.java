package org.expeditee.io;

import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.expeditee.agents.WriteTree;
import org.expeditee.gui.Browser;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Text;

public abstract class DefaultFrameWriter extends ItemWriter implements
		FrameWriter {

	protected String _filename = null;

	protected String _output = null;

	protected ProxyWriter _writer = null;

	protected String _format = "";

	protected boolean _running = true;

	protected boolean _stop = false;

	// keep track of methods that are put on the same line
	protected static LinkedHashMap<Character, Method> _ItemTags = null;

	protected static LinkedHashMap<Character, Method> _FrameTags = null;

	public DefaultFrameWriter() {
		
		if (_ItemTags != null && _FrameTags != null)
			return;

		_ItemTags = new LinkedHashMap<Character, Method>();
		_FrameTags = new LinkedHashMap<Character, Method>();

		try {
			_FrameTags.put('V', Frame.class.getMethod("getVersion"));
			_FrameTags.put('p', Frame.class.getMethod("getPermission"));
			_FrameTags.put('U', Frame.class.getMethod("getOwner"));
			_FrameTags.put('D', Frame.class.getMethod("getDateCreated"));
			_FrameTags.put('M', Frame.class.getMethod("getLastModifyUser"));
			_FrameTags.put('d', Frame.class.getMethod("getLastModifyDate"));
			_FrameTags.put('F', Frame.class.getMethod("getFrozenDate"));

			_FrameTags.put('O', Frame.class.getMethod("getForegroundColor"));
			_FrameTags.put('B', Frame.class.getMethod("getBackgroundColor"));
			
			_ItemTags.put('S', Item.class.getMethod("getTypeAndID"));
			_ItemTags.put('s', Item.class.getMethod("getDateCreated"));
			_ItemTags.put('d', Item.class.getMethod("getColor"));
			_ItemTags.put('G', Item.class.getMethod("getBackgroundColor"));
			_ItemTags.put('K', Item.class.getMethod("getBorderColor"));

			_ItemTags.put('P', Item.class.getMethod("getPosition"));
			_ItemTags.put('T', Text.class.getMethod("getText"));
			_ItemTags.put('F', Item.class.getMethod("getLink"));
			_ItemTags.put('X', Item.class.getMethod("getAction"));
			_ItemTags.put('x', Item.class.getMethod("getActionMark"));
			_ItemTags.put('U', Item.class.getMethod("getActionCursorEnter"));
			_ItemTags.put('V', Item.class.getMethod("getActionCursorLeave"));
			_ItemTags.put('W', Item.class.getMethod("getActionEnterFrame"));
			_ItemTags.put('Y', Item.class.getMethod("getActionLeaveFrame"));
			_ItemTags.put('D', Item.class.getMethod("getData"));
			_ItemTags.put('u', Item.class.getMethod("getHighlight"));
			_ItemTags.put('e', Item.class.getMethod("getFillColor"));
			_ItemTags.put('E', Item.class.getMethod("getGradientColor"));
			_ItemTags.put('Q', Item.class.getMethod("getGradientAngle"));
			
			_ItemTags.put('R', Item.class.getMethod("getAnchorLeft"));
			_ItemTags.put('H', Item.class.getMethod("getAnchorRight"));
			_ItemTags.put('N', Item.class.getMethod("getAnchorTop"));
			_ItemTags.put('I', Item.class.getMethod("getAnchorBottom"));

			_ItemTags.put('i', Item.class.getMethod("getFillPattern"));
			_ItemTags.put('o', Item.class.getMethod("getOwner"));
			_ItemTags.put('n', Item.class.getMethod("getLinkMark"));
			_ItemTags.put('q', Item.class.getMethod("getLinkFrameset"));
			_ItemTags.put('y', Item.class.getMethod("getLinkTemplate"));
			_ItemTags.put('g', Item.class.getMethod("getLinePattern"));

			_ItemTags.put('j', Item.class.getMethod("getArrow"));
			
			_ItemTags.put('v', Item.class.getMethod("getDotType"));
			_ItemTags.put('z', Item.class.getMethod("getFilled"));

			_ItemTags.put('f', Text.class.getMethod("getFont"));
			_ItemTags.put('t', Text.class.getMethod("getSpacing"));

			// TODO set a boolean flag to indicate that the text is a formula
			// Store the formula in the text property NOT the answer
			_ItemTags.put('J', Item.class.getMethod("getFormula"));

			_ItemTags.put('a', Text.class.getMethod("getWordSpacing"));
			_ItemTags.put('b', Text.class.getMethod("getLetterSpacing"));
			_ItemTags.put('m', Text.class.getMethod("getInitialSpacing"));
			_ItemTags.put('w', Text.class.getMethod("getWidthToSave"));
			_ItemTags.put('k', Text.class.getMethod("getJustification"));
			_ItemTags.put('r', Text.class.getMethod("getAutoWrapToSave"));

			_ItemTags.put('h', Item.class.getMethod("getThickness"));
			_ItemTags.put('l', Item.class.getMethod("getLineIDs"));
			_ItemTags.put('c', Item.class.getMethod("getConstraintIDs"));
			
			_ItemTags.put('A', Item.class.getMethod("getTooltip"));
			_ItemTags.put('B', Item.class.getMethod("getLinkHistory"));
			
			_ItemTags.put('p', Item.class.getMethod("getPermission"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setOutputLocation(String filename) {
		_filename = filename;
	}

	public String writeFrame(Frame toWrite) throws IOException {
		return writeFrame(toWrite, null);
	}

	public String writeFrame(Frame toWrite, Writer writer) throws IOException {
		try {
			initialise(toWrite, writer);

			outputFrame(toWrite);

			_running = false;
			return finaliseFrame();
		} catch (IOException ioe) {
			_running = false;
			throw ioe;
		}

	}

	/**
	 * Called before writing out the body items of each frame.
	 * 
	 * @param starting
	 *            the name of the frame currently being written out.
	 * @throws IOException
	 */
	protected void writeStartFrame(Frame starting) throws IOException {
		if (starting.getTitleItem() != null) {
			if (starting.getTitleItem().isAnnotation())
				this.writeAnnotationTitle(starting.getTitleItem());
			else
				this.writeTitle(starting.getTitleItem(), starting.getItems());
		}
	}

	/**
	 * Called after writing out the body items of each frame.
	 * 
	 * @param ending
	 *            the name of the frame currently being written out.
	 * @throws IOException
	 */
	protected void writeEndFrame(Frame ending) throws IOException {
	}

	protected final void initialise(Frame start) throws IOException {
		initialise(start, null);
	}

	protected void initialise(Frame start, Writer writer) throws IOException {
		if (_filename == null)
			_filename = FrameIO.EXPORTS_DIR + getFileName(start) + _format;

		if (writer != null) {
			_writer = new ProxyWriter(writer);
			_output = writer.toString();
		} else if (_filename.equalsIgnoreCase(WriteTree.CLIPBOARD)) {
			_writer = new ProxyWriter(true);
			_output = WriteTree.CLIPBOARD;
		} else {
			if (_filename.contains(File.separator)) {
				String extTest = _filename.substring(_filename
						.lastIndexOf(File.separator) + 1);
				if (!extTest.contains("."))
					_filename += _format;
			} else if (!_filename.contains("."))
				_filename += _format;

			if (!_filename.contains(File.separator))
				_filename = FrameIO.EXPORTS_DIR + _filename;

			File test = new File(_filename);

			File parent = test.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}

			// Open an Output Stream Writer to set encoding
			OutputStream fout = new FileOutputStream(_filename);
			OutputStream bout = new BufferedOutputStream(fout);
			Writer out = new OutputStreamWriter(bout, "UTF-8");

			_writer = new ProxyWriter(out);
			_output = _filename;
		}
	}

	protected String getFileName(Frame start) {
		return getValidFilename(start.getTitle());
	}

	public static String getValidFilename(String filename) {
		return filename.replaceAll("[ \\.]", "_");
	}

	protected String finalise() throws IOException {
		try {
			_writer.flush();
			_writer.close();
		} catch (IOException ioe) {
		} finally {
			_writer.close();
		}
		try {
			if (Browser._theBrowser.isMinimumVersion6()) {
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().open(new File(_output));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return " exported to " + _output;
	}

	protected String finaliseFrame() throws IOException {
		return "Frame" + finalise();
	}

	protected void outputFrame(Frame toWrite) throws IOException {
		writeStartFrame(toWrite);

		Collection<Item> done = new HashSet<Item>();

		for (Item i : getItemsToWrite(toWrite)) {
			if (_stop) {
				return;
			}

			if (i instanceof Line) {
				if (done.contains(i)) {
					continue;
				}
				done.addAll(i.getAllConnected());
			}
			writeItem(i);
		}
		writeEndFrame(toWrite);
	}

	protected Collection<Item> getItemsToWrite(Frame toWrite) {
		return toWrite.getItemsToSave();
	}

	public boolean isRunning() {
		return _running;
	}

	public void stop() {
		_stop = true;
	}

	public String getFileContents() {
		return "Not Supported";
	}

}
