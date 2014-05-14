package org.expeditee.agents;

import java.util.Collection;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameKeyboardActions;
import org.expeditee.gui.Vector;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Text;
import org.expeditee.items.UserAppliedPermission;

public class ScaleFrameset extends DefaultAgent {
	private int _firstFrame = 1;

	private float _scale = 1F;

	private int _maxFrame = Integer.MAX_VALUE;

	public ScaleFrameset(float scale, int firstFrame, int maxFrame) {
		super();
		_firstFrame = firstFrame;
		_maxFrame = maxFrame;
		_scale = scale;
	}

	public ScaleFrameset(float scale) {
		this(scale, 1, Integer.MAX_VALUE);
	}

	@Override
	protected Frame process(Frame frame) {
		String framesetName = frame.getFramesetName();

		_maxFrame = Math.min(FrameIO.getLastNumber(framesetName), _maxFrame);

		// Scale each of the frames
		for (int i = _firstFrame; i <= _maxFrame; i++) {
			scaleFrame(FrameIO.LoadFrame(framesetName + i), _scale);
		}
		// TODO fix this so it actually works properly!!
		// TODO figure out why it doesnt repaint correctly sometimes

		// TODO make this thread safe!
		FrameKeyboardActions.Refresh();

		return null;
	}
	
	public static void scaleFrame(Frame frame, float scaleFactor) {
		
		if (frame == null) {
			return;
		}
		
		for (Vector v : frame.getVectors()) {
			v.Source.scale(scaleFactor, 0, 0);
		}
		
		Collection<Item> items = frame.getVisibleItems();
		
		for(Item item : items) {
			if(item instanceof Text && item.getSize() <= Text.MINIMUM_FONT_SIZE && scaleFactor < 1) {
				return;
			}
		}
		
		for (Item item : items) {
			// This line is only needed for circles!!
			// Need to really fix up the way this works!!
			if (item.hasEnclosures())
				continue;
			if (!item.hasPermission(UserAppliedPermission.full))
				continue;
			item.invalidateAll();
			if (!(item instanceof Line)) {
				item.scale(scaleFactor, 0, 0);
			}
		}

		for (Item item : items) {
			if (!item.hasPermission(UserAppliedPermission.full))
				continue;
			// if (!(item instanceof Line))
			item.updatePolygon();

			if (item instanceof Line) {
				((Line) item).refreshStroke(item.getThickness());
			}

			item.invalidateAll();
		}
	}
}
