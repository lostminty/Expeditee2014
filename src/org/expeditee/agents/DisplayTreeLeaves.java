package org.expeditee.agents;

import org.expeditee.gui.Frame;
import org.expeditee.items.Item;

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
public class DisplayTreeLeaves extends DisplayTree {

	public DisplayTreeLeaves(String delay) {
		super(delay);
	}

	public DisplayTreeLeaves() {
		super();
	}

	@Override
	protected void processFrame(Frame toProcess) {
		// call display trees process only if the frame doesnt have any linked
		// items
		for (Item item : toProcess.getItems()) {
			if (item.getLink() != null && !item.isAnnotation())
				return;
		}

		super.processFrame(toProcess);
	}
}
