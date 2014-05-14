package org.expeditee.agents;

import java.awt.Color;
import java.awt.geom.Point2D;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.items.Text;

public class SwitchyardTree extends DefaultAgent {

	@Override
	protected Frame process(Frame frame) {
		//MessageBay.displayMessage("Running switchyard tree...");

		for (Text textItem : frame.getBodyTextItems(false)) {
			// Delete all non-annotations with more that one letter
			if (textItem.getText().length() > 1){
				frame.removeItem(textItem);
				if(_stop)
					return null;
			} else {
				// goto child frame of any linked 1 letter items
				String link = textItem.getAbsoluteLink();
				if (link != null) {
					Frame childFrame = FrameIO.LoadFrame(link);
					if (childFrame != null) {
						Point2D.Float lastItemEnd = textItem.getParagraphEndPosition();
						for (Text childItem : childFrame.getBodyTextItems(false)) {
							// look for red items (remember get color may be null
							if (Color.RED.equals(childItem.getPaintColor())) {
								// make a copy and add to parent frame
								Text itemCopy = childItem.copy();
								// add to the right of parent item
								lastItemEnd.setLocation(lastItemEnd.x + 20, lastItemEnd.y);
								itemCopy.setPosition(lastItemEnd.x, lastItemEnd.y);
								lastItemEnd = itemCopy
										.getParagraphEndPosition();
								itemCopy.setID(frame.getNextItemID());
								frame.addItem(itemCopy);
							}
							_itemCount++;
							if(_stop)
								return null;
						}
					}
				}
			}
		}
		_frameCount++;
		return null;
	}
}
