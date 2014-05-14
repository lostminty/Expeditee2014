package org.expeditee.items;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;

//TODO tidy up the mess I caused with refresh during the period where I was going into XRay mode before saving

public class FrameBitmap extends FramePicture {
	public FrameBitmap(Text source, ImageObserver observer, Image image){
		super(source, observer, image);
	}

	@Override
	public boolean refresh() {
		assert (_source.getLink() != null);
		Frame frame = FrameIO.LoadFrame(_source.getAbsoluteLink());
		// if the frame cant be found just use the current image
		if (frame == null) {
			return false;
		}

		List<Text> textList = frame.getBodyTextItems(false);
		if (textList.size() == 0)
			return false;
		List<String> imageLines = textList.get(0).getTextList();
		int width = 0;
		int height = imageLines.size();
		// Determine the image width by finding the widest line of text
		for (String s : imageLines) {
			if (s.length() > width)
				width = s.length();
		}
		
		if(width == 0)
			return false;
		
		BufferedImage bi = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		// now set the bits on the image
		final int transparent = (new Color(0F, 0F, 0F, 0F)).getRGB();
		final int main = _source.getPaintColor().getRGB();
		final Color c = _source.getPaintColor();
		int currentColor = main;
		int row = 0;
		for (String s : imageLines) {
			for (int i = 0; i < width; i++) {
				currentColor = transparent;
				if (i < s.length()) {
					char currentPixel = s.charAt(i);
					// Space is transparent as is 0
					if (Character.isDigit(currentPixel)) {
						int alpha = Math.round((currentPixel - '0') * 25.5F);
						currentColor = new Color(c.getRed(), c.getGreen(), c
								.getBlue(), alpha).getRGB();
					}else if (currentPixel != ' ') {
						currentColor = main;
					}
				}
				bi.setRGB(i, row, currentColor);
			}
			row++;
		}
		_image = bi;

		return true;
	}

	@Override
	protected Picture createPicture() {
		return new FrameBitmap((Text) _source.copy(),
				_imageObserver, _image);
	}

	@Override
	protected String getTagText() {
		return "@b: ";
	}
}
