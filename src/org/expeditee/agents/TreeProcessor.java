package org.expeditee.agents;

import java.util.List;
import java.util.Stack;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.items.Item;

/**
 * This class provides some generic methods that process a tree of Frames.
 * Subclasses that only use text items can simply implement
 * processTextItem(Text, int)
 */
public abstract class TreeProcessor extends DefaultAgent {

	// the list of frames currently being processed
	private Stack<FrameCounter> _frames = new Stack<FrameCounter>();
	
	public TreeProcessor(String delay) {
		super(delay);
	}

	public TreeProcessor() {
		super();
	}
	
	/**
	 * Processes the Frame by calling processItem() for each unlinked item
	 * encountered. Linked items call processStartLinkItem(), then load the
	 * linked frame and process it recursively, upon returning
	 * processEndLinkItem() is called.
	 */
	protected Frame process(Frame toProcess) {
		if (_stop)
			return null;

		if (toProcess == null)
			return null;

		_frames.push(new FrameCounter(toProcess.getName(), -1));

		// process the entire tree of frames in depth-first order
		while (_frames.size() > 0) {
			if (_stop)
				return null;

			FrameCounter cur = _frames.pop();

			Frame next = FrameIO.LoadFrame(cur.frame);
			if (next == null) {
				message("There is no " + cur.frame + " Frame to display");
				return null;
			}

			if (cur.index < 0) {
				if (next != null)
					overwriteMessage("Processing: " + next.getName());
				processFrame(next);
			}

			// the items on the frame currently being processed.
			List<Item> items = next.getItems();

			// resume from the next item in the list
			for (int i = cur.index + 1; i < items.size(); i++) {
				Item item = items.get(i);

				// ignore annotation and framenames
				if (!item.isAnnotation() && item.getID() >= 0) {
					if (item.getLink() != null) {
						cur.index = i;
						_frames.push(cur);

						Frame linked = FrameIO.LoadFrame(item.getAbsoluteLink());

						// if the linked frame was found, then display it next
						if (linked != null) {
							FrameCounter fc = new FrameCounter(linked
									.getName(), -1);
							if (!_frames.contains(fc)) {
								// remember what frame we are on before
								// processing it
								_frames.push(fc);

								// processFrame(linked);

								// process the loaded frame immediately
								// (depth-first)
								break;
							}
						}
					}
				}
			}
		}
		
		return null;
	}

	protected abstract void processFrame(Frame toProcess);

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

}
