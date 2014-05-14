package org.expeditee.io;

import java.io.IOException;
import java.util.List;
import java.util.Stack;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

public abstract class DefaultTreeWriter extends DefaultFrameWriter implements
		TreeWriter {

	private int _indent = 0;

	// the list of frames currently being processed
	private Stack<FrameCounter> _frames = new Stack<FrameCounter>();

	private int _frameCount = 0;

	public int getFrameCount() {
		return _frameCount;
	}

	public String writeTree(Frame toWrite) throws IOException {
		try {
			initialise(toWrite);
			outputTree(toWrite);

		} catch (IOException ioe) {
			_running = false;
			throw ioe;
		} catch (Exception e) {
			e.printStackTrace();
		}
		_running = false;
		return finaliseTree();
	}

	/**
	 * This method is used to output any tags before following the Item's link
	 * when using tree writers.
	 * 
	 * @param linker
	 *            The linked Item that is about to be followed.
	 * @throws IOException
	 */
	protected void writeStartLink(Item linker) throws IOException {
		_indent++;
	}

	/**
	 * This method is called after the Frame the Item links to has been
	 * processed. This allows end tags to be written if the format requires it.
	 * 
	 * @param linker
	 *            The Item whose link was just followed.
	 * @throws IOException
	 */
	protected void writeEndLink(Item linker) throws IOException {
		_indent--;
	}

	protected void resumeFrame(Frame resuming) {
	}

	protected List<Item> getSortedItems(Frame frame) 
	{
		List<Item> items = frame.getItems();
		return items;
	}
	
	protected void outputTree(Frame toWrite) throws IOException {
		if (toWrite == null)
			return;

		_frames.push(new FrameCounter(toWrite.getName(), -1));

		// process the entire tree of frames in depth-first order
		while (_frames.size() > 0) {
			FrameCounter cur = _frames.pop();

			if (_stop)
				return;

			Frame next = FrameIO.LoadFrame(cur.frame);

			if (next == null) {
				return;
			}

			Text title = next.getTitleItem();

			// the items on the frame currently being processed.
			List<Item> items = getSortedItems(next);

			// write any information relating to the end of the link
			if (cur.index > 0) {
				this.writeEndLink(items.get(cur.index));
				this.resumeFrame(next);
			} else {
				MessageBay.overwriteMessage("Writing: " + next.getName());
				_frameCount++;
				writeStartFrame(next);
			}

			boolean complete = true;

			// resume from the next item in the list
			for (int i = cur.index + 1; i < items.size(); i++) {
				if (_stop)
					return;

				Item item = items.get(i);

				// ignore annotation and framenames
				if (item.getID() >= 0) {
					// Only follow the links of non annotation text items
					boolean followLink = item instanceof Text
							&& item.getLink() != null
							&& (!item.isAnnotation() /*|| item.getText()
									.toLowerCase().equals("@next")*/);

					if (followLink) {
						cur.index = i;
						_frames.push(cur);

						// write any information relating to the start of the
						// link
						this.writeStartLink(item);

						Frame linked = FrameIO
								.LoadFrame(item.getAbsoluteLink());

						// if the linked frame was found, then display it next
						if (linked != null) {
							FrameCounter fc = new FrameCounter(
									linked.getName(), -1);
							if (!_frames.contains(fc)) {
								// remember what frame we are on before
								// processing it
								_frames.push(fc);

								complete = false;
								// process the loaded frame immediately
								// (depth-first)
								break;
							}
						}
						// Don't write out the title here because it is written
						// out earlier
					} else if (item != title)
						this.writeItem(item);
				}
			}

			if (complete)
				writeEndFrame(next);
		}
	}

	protected String finaliseTree() throws IOException {
		return "Tree" + finalise();
	}

	/**
	 * Inner class used to keep track of what frames have been seen, as well as
	 * what Item in the Frame the processing was up to. Only Frame names are
	 * stored to keep memory usage down.
	 */
	private class FrameCounter {
		public int index;

		public String frame;

		public FrameCounter(String f, int i) {
			frame = f.toLowerCase();
			index = i;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof FrameCounter)
				return (((FrameCounter) o).frame.equals(frame));// && fc.index
			// == index);

			return false;
		}
	}

	protected int getIndent() {
		return _indent;
	}
}
