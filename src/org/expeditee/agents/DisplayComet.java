package org.expeditee.agents;

import java.util.Collection;
import java.util.HashSet;
import java.util.Stack;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;

public class DisplayComet extends DefaultAgent {
	private Stack<Frame> _frameList = new Stack<Frame>();

	public DisplayComet(String delay) {
		super(delay);
	}

	public DisplayComet() {
		super();
	}

	@Override
	protected Frame process(Frame frame) {
		Collection<String> seen = new HashSet<String>();

		DisplayIO.addToBack(frame);

		// Goto the end of the comet
		Item old = null;
		Frame oldFrame = frame;
		while (oldFrame != null) {
			if (_stop)
				return null;
			seen.add(oldFrame.getName().toLowerCase());
			_frameList.push(oldFrame);
			old = ItemUtils.FindExactTag(oldFrame.getItems(), ItemUtils
					.GetTag(ItemUtils.TAG_BACKUP));
			oldFrame = null;
			if (old != null) {
				String link = old.getAbsoluteLink();

				if (link != null) {
					String linkLowerCase = link.toLowerCase();
					if (!seen.contains(linkLowerCase)) {
						oldFrame = FrameIO.LoadFrame(linkLowerCase);
						MessageBay.overwriteMessage("Loading frames: " + link);
					}
				}
			}
		}
		// Back out one frame at a time
		while (!_frameList.empty()) {
			if (_stop)
				return null;
			DisplayIO.setCurrentFrame(_frameList.pop(), true);
			_frameCount++;
			pause(_delay);
		}

		return _start;
	}

}
