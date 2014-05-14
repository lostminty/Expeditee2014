package org.expeditee.agents;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Text;

/**
 * Displays the Tree of Frames starting from the given frame. The delay between
 * frames can be adjusted, either per-frame with the
 * 
 * @DisplayFramePause tag, or mid-stream with the
 * @DisplayTreePause tag, both tags should be followed with the desired delay in
 *                   ms. The default delay between Frames is 50ms.
 * @author jdm18
 * 
 */
public class DisplayTree extends TreeProcessor {

	public static final int GARBAGE_COLLECTION_THRESHOLD = 100000;

	private Runtime _runtime = Runtime.getRuntime();
	
	public DisplayTree(String delay) {
		super(delay);
	}

	public DisplayTree() {
		super();
	}

	@Override
	public boolean initialise(Frame start, Item launcher) {

		// push current frame on to back-stack
		DisplayIO.addToBack(start);

		return super.initialise(start, launcher);
	}

	@Override
	protected void finalise(Frame start) {
		// return the user to the Frame they started on
		if (!_stop) {
			DisplayIO.Back();
		}
		super.finalise(start);
	}

	@Override
	protected void processFrame(Frame toProcess) {
		long freeMemory = _runtime.freeMemory();
		if (freeMemory < GARBAGE_COLLECTION_THRESHOLD) {
			_runtime.gc();
			MessageBay.displayMessage("Force Garbage Collection!");
		}

		// FrameUtils.ResponseTimer.restart();

		// ignore loops
		if (toProcess != DisplayIO.getCurrentFrame())
			DisplayIO.setCurrentFrame(toProcess, false);
		// parse the frame for any pause settings
		delay(toProcess);

		_frameCount++;
	}

	/**
	 * Parses through the given Frame to find any
	 * 
	 * @DisplayTreePause or
	 * @DisplayFramePause tags, and updates the delay or pause as necessary
	 * @param toSearch
	 */
	private void delay(Frame toSearch) {
		// check for change in globaly delay time
		Item delay = ItemUtils.FindTag(toSearch.getItems(),
				"@DisplayTreePause:");
		
		if (delay != null) {
			try {
				// attempt to read in the delay value
				_delay = Long.parseLong(ItemUtils.StripTag(((Text) delay)
						.getFirstLine(), "@DisplayTreePause"));
				message("DisplayTree delay changed to: " + _delay + "ms");
			} catch (NumberFormatException nfe) {
				message("Incorrect paramter for DisplayTreePause");
			}

		}

		// check for change in delay for this frame only
		delay = ItemUtils.FindTag(toSearch.getItems(), "@DisplayFramePause:");
		if (delay != null) {
			try {
				// attempt to read in the delay value
				long pause = Long.parseLong(ItemUtils.StripTag(((Text) delay)
						.getFirstLine(), "@DisplayFramePause"));
				pause(pause);
			} catch (NumberFormatException nfe) {
				message("Incorrect paramter for DisplayFramePause");
			}
		} else
			pause(_delay);
	}
}
