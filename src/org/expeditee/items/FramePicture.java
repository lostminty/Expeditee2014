package org.expeditee.items;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.ImageObserver;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.Frame;

public abstract class FramePicture extends Picture {
	protected FramePicture(Text source, ImageObserver observer, Image image) {
		super(source, observer, image);
	}

	@Override
	public void setColor(Color c) {
		super.setColor(c);
		refresh();
	}
	
	@Override
	public void setLink(String frameName) {
		if (_source == null)
			return;
		else {
			_source.setLink(frameName);
			//remove the picture if the link is being removed
			if (_source.getLink() == null) {
				Frame parent = getParent();
				if (parent != null) {
					parent.removeItem(this);
					_source.setHidden(false);
					_source.removeEnclosure(this);
				}
			} else {
				refresh();
			}
		}
		updatePolygon();
	}
	
	@Override
	public String getName() {
		return _source.getAbsoluteLink();
	}
	
	@Override
	protected String getImageSize() {
		return new AttributeValuePair(_source.getText()).getValue();
	}
}
