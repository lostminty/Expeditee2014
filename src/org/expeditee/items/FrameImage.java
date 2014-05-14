package org.expeditee.items;

import java.awt.Image;
import java.awt.image.ImageObserver;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;

public class FrameImage extends FramePicture {
	/**
	 * Creates a new Picture from the given path. The ImageObserver is optional
	 * and can be set to NULL. <br>
	 * Note: It is assumed that the file described in path has already been
	 * checked to exist.
	 * 
	 * @param source
	 *            The Text Item that was used to create this Picture
	 * @param path
	 *            The Path of the Image to load from disk.
	 * @param observer
	 *            The ImageObserver to assign when painting the Image on the
	 *            screen.
	 */
	public FrameImage(Text source, ImageObserver observer, Image image) {
		super(source, observer, image);
	}

	@Override
	protected Picture createPicture() {
		return new FrameImage(_source.copy(), _imageObserver, _image);
	}

	@Override
	public boolean refresh() {
		// Need to parse the first time the frame is being displayed
		// parseSize();
		assert (_source.getLink() != null);
		Frame frame = FrameIO.LoadFrame(_source.getAbsoluteLink(), null, true);
		if (frame == null)
			return false;

		frame.setBuffer(null);
		FrameGraphics.UpdateBuffer(frame, false, false);
		_image = frame.getBuffer();

		// TODO tidy this up, need to call parse size only when the frame has
		// been created to begin with
		parseSize();
		updatePolygon();
		return true;
	}

	@Override
	protected String getTagText() {
		return "@f: ";
	}
}
