package org.expeditee.agents;

import java.io.IOException;
import java.lang.reflect.Constructor;

import org.expeditee.actions.Actions;
import org.expeditee.gui.Frame;
import org.expeditee.gui.MessageBay;
import org.expeditee.io.FrameWriter;
import org.expeditee.io.TreeWriter;
import org.expeditee.items.Item;
import org.expeditee.stats.Logger;

public class WriteTree extends DefaultAgent {

	private String _format = "txt";

	protected TreeWriter _treeWriter;

	protected FrameWriter _frameWriter;

	private boolean _clipboard = false;

	// write tree and write frame are almost identical, so WriteFrame.java
	// sets this to false
	private boolean _followLinks = true;

	private String _outFile = null;

	private static final String IO_PACKAGE = Actions.ROOT_PACKAGE + "io.";

	public WriteTree() {
	}

	public WriteTree(String params) {
		String format = params.trim();

		if (format.equalsIgnoreCase(CLIPBOARD)) {
			_clipboard = true;
			return;
		}

		_format = format.trim();

		int ind = params.indexOf(" ");
		if (ind > 0) {
			String lastParam = params.substring(ind + 1);
			_format = params.substring(0, ind).toLowerCase();

			if (lastParam.equalsIgnoreCase(CLIPBOARD))
				_clipboard = true;
			else
				_outFile = lastParam;
		}
	}

	public WriteTree(String format, String outFile) {
		_format = format;
		_outFile = outFile;
	}

	public void setFollowLinks(boolean val) {
		_followLinks = val;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean initialise(Frame start, Item launcher) {
		if (_outFile == null)
			_outFile = start.getExportFileTagValue();

		try {
			Class c = null;

			try {
				c = Class
						.forName(IO_PACKAGE + _format.toUpperCase() + "Writer");
			} catch (NoClassDefFoundError ex) {
				c = Class.forName(IO_PACKAGE + _format + "Writer");
			}

			Constructor con = c.getConstructor();
			Object o = con.newInstance();

			// if o is not a tree or frame writer, then it is not a valid
			// format

			if (_followLinks) {
				// check that o is a valid treewriter
				if (!(o instanceof TreeWriter)) {
					MessageBay.warningMessage(_format.toUpperCase()
							+ " format cannot be used to write trees.");
					return false;
				}

				_treeWriter = (TreeWriter) o;

				if (_clipboard)
					_treeWriter.setOutputLocation(CLIPBOARD);
				else if (_outFile != null)
					_treeWriter.setOutputLocation(_outFile);
			} else {
				if (!(o instanceof FrameWriter) && !(o instanceof TreeWriter)) {
					MessageBay.warningMessage(_format.toUpperCase()
							+ " format cannot be used to write frames.");
					return false;
				}

				_frameWriter = (FrameWriter) o;

				if (_clipboard)
					_frameWriter.setOutputLocation(CLIPBOARD);
				else if (_outFile != null)
					_frameWriter.setOutputLocation(_outFile);
			}

		} catch (ClassNotFoundException e) {
			MessageBay
					.warningMessage("The agent does not exist or has incorrect parametres.");
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return super.initialise(start, launcher);
	}

	@Override
	protected Frame process(Frame frame) {
		String msg = "Failed.";
		try {

			if (_followLinks) {
				msg = _treeWriter.writeTree(frame);
				_frameCount = _treeWriter.getFrameCount();
			} else {
				msg = _frameWriter.writeFrame(frame);
				_frameCount = 1;
			}
		} catch (IOException e) {
			//System.out.println("Caught");
			Logger.Log(e);
			MessageBay.errorMessage("IOException in WriteTree: "
					+ e.getMessage());
			e.printStackTrace();
			super.stop();
			return null;
		}

		if (_stop)
			message("WriteTree halted by user.");
		else
			message(msg);

		return null;
	}

	@Override
	public void stop() {
		super.stop();

		if (_treeWriter != null)
			_treeWriter.stop();

		if (_frameWriter != null)
			_frameWriter.stop();
	}

	@Override
	public boolean isRunning() {
		if (_treeWriter != null)
			return _treeWriter.isRunning();

		if (_frameWriter != null)
			return _frameWriter.isRunning();

		return false;
	}

}
