package org.expeditee.items;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FrameUtils;
import org.expeditee.stats.Logger;

/**
 * This class represents an Image loaded from a file which is shown on the
 * screen. Loading of the Image from disk occurs in the constructor, and takes
 * approximately one second per mb of the Image file size. <br>
 * <br>
 * Currently Supported (Tested) Image formats:<br>
 * BMP<br>
 * JPG<br>
 * GIF<br>
 * GIF (Animated)<br>
 * <br>
 * Currently only the default size of the Image is supported, but future
 * versions may support scaling.
 * 
 * @author jdm18
 * 
 */
public class Picture extends XRayable {

	private static final int MINIMUM_WIDTH = 10;

	public static final int WIDTH = 0;

	public static final int RATIO = 1;

	protected Image _image = null;

	private int _scaleType = RATIO;

	private float _scale = 1.0f;

	// Start of the crop relative to START
	private Point _cropStart = null;

	// Start of the crop relative to END
	private Point _cropEnd = null;

	private Point _start = new Point(0, 0);

	private Point _end = new Point(0, 0);
	
	private double _rotate = 0;
	
	private boolean _flipX = false;
	private boolean _flipY = false;

	private boolean _showCropping = false;

	private String _path = "";

	private String _size = "";

	private String _fileName = null;

	// used to repaint animated GIF images, among other things.
	protected ImageObserver _imageObserver = null;

	protected Picture(Text source, ImageObserver observer, Image image) {
		super(source);
		_imageObserver = observer;
		_image = image;

		refresh();

		if (_image != null)
			parseSize();
	}

	/**
	 * Creates a new Picture from the given path. The ImageObserver is optional
	 * and can be set to NULL. <br>
	 * Note: It is assumed that the file described in path has already been
	 * checked to exist.
	 * 
	 * @param source
	 *            The Text Item that was used to create this Picture
	 * @param fileName
	 *            the name of the file as it should be displayed in the source
	 *            text
	 * @param path
	 *            The Path of the Image to load from disk.
	 * @param observer
	 *            The ImageObserver to assign when painting the Image on the
	 *            screen.
	 */
	public Picture(Text source, String fileName, String path, String size,
			ImageObserver observer) {
		super(source);
		_imageObserver = observer;
		_fileName = fileName;
		_path = path;
		_size = size;

		refresh();
		parseSize();
	}

	protected String getImageSize() {
		return _size;
	}

	protected void parseSize() {
		String size = getImageSize();

		if (_end.x != 0 || _end.y != 0)
			return;

		// set the default values for start and end
		_start.setLocation(0, 0);
		if (_image == null)
			_end.setLocation(0, 0);
		else
			_end.setLocation(_image.getWidth(null), _image.getHeight(null));
		size = size.trim();
		String sizeLower = size.toLowerCase();
		String[] values = size.split("\\s+");
		// Now get the cropping values if there are any
		try {
			if (values.length > 2) {
				int startX = Integer.parseInt(values[1]);
				int startY = Integer.parseInt(values[2]);
				_start = new Point(startX, startY);
				if (values.length > 4) {
					int endX = Integer.parseInt(values[3]);
					int endY = Integer.parseInt(values[4]);
					_end = new Point(endX, endY);
				}
				scaleCrop();
			}
		} catch (Exception e) {
		}
		
		if(sizeLower.contains("flipx")) {
			_flipX = true;
		}
		
		if(sizeLower.contains("flipy")) {
			_flipY = true;
		}
		
		int index = sizeLower.indexOf("rotation=");
		if(index != -1) {
			int tmp = sizeLower.indexOf(" ", index);
			String rotation;
			if(tmp == -1) {
				rotation = sizeLower.substring(index + "rotation=".length());
			} else {
				rotation = sizeLower.substring(index + "rotation=".length(), index + tmp);
			}
			_rotate = Double.parseDouble(rotation);
		}

		try {
			if (size.length() == 0) {
				size = "" + _image.getWidth(null);
				_source.setText(getTagText() + size);
				return;
			}
			size = values[0];
			// parse width or ratio from text
			if (size.contains(".")) {
				// this is a ratio
				_scale = Float.parseFloat(size);
				_scaleType = RATIO;
			} else if (size.length() > 0) {
				// this is an absolute width
				int width = Integer.parseInt(size);
				_scaleType = WIDTH;
				setWidth(width);
			}
		} catch (Exception e) {
			_scale = 1F;
		}
	}

	public void setStartCrop(int x, int y) {
		invalidateCroppedArea();
		_cropStart = new Point(x - getX(), y - getY());
		invalidateCroppedArea();
	}

	public void setEndCrop(int x, int y) {
		invalidateCroppedArea();
		_cropEnd = new Point(x - getX(), y - getY());
		invalidateCroppedArea();
	}

	private void invalidateCroppedArea() {
		if (_cropStart != null && _cropEnd != null) {
			Point topLeft = getTopLeftCrop();
			Point bottomRight = getBottomRightCrop();
			int startX = getX() + topLeft.x - _highlightThickness;
			int startY = getY() + topLeft.y - _highlightThickness;
			int border = 2 * _highlightThickness;
			invalidate(new Rectangle(startX, startY, bottomRight.x - topLeft.x
					+ 2 * border, bottomRight.y - topLeft.y + 2 * border));
			invalidateAll();
		} else {
			invalidateAll();
		}
	}

	public Point getTopLeftCrop() {
		return new Point(Math.min(_cropStart.x, _cropEnd.x), Math.min(
				_cropStart.y, _cropEnd.y));
	}

	public Point getBottomRightCrop() {
		return new Point(Math.max(_cropStart.x, _cropEnd.x), Math.max(
				_cropStart.y, _cropEnd.y));
	}

	public void setShowCrop(boolean value) {
		// invalidateCroppedArea();
		_showCropping = value;
		invalidateCroppedArea();
	}

	public boolean isCropTooSmall() {
		if (_cropStart == null || _cropEnd == null)
			return true;

		int cropWidth = Math.abs(_cropEnd.x - _cropStart.x);
		int cropHeight = Math.abs(_cropEnd.y - _cropStart.y);

		return cropWidth < MINIMUM_WIDTH || cropHeight < MINIMUM_WIDTH;
	}

	public void clearCropping() {
		invalidateCroppedArea();
		_cropStart = null;
		_cropEnd = null;
		setShowCrop(false);
	}

	public void updatePolygon() {
		if (_image == null) {
			refresh();
			parseSize();
		}
		
		Point[] ori = new Point[4];
		Point2D[] rot = new Point2D[4];
		Point centre = new Point();

		if (_cropStart == null || _cropEnd == null) {
			int width = getWidth();
			int height = getHeight();
			
			centre.x = _source.getX() + width / 2;
			centre.y = _source.getY() + height / 2;

			int xdiff = -MARGIN_RIGHT; // -getLeftMargin();

			// extra pixel around the image so the highlighting is visible
//			_poly.addPoint(_source.getX() + 1 + xdiff, _source.getY() - 1);
//			_poly.addPoint(_source.getX() + width, _source.getY() - 1);
//			_poly.addPoint(_source.getX() + width, _source.getY() + height);
//			_poly.addPoint(_source.getX() + 1 + xdiff, _source.getY() + height);
			
			ori[0] = new Point(_source.getX() + 1 + xdiff, _source.getY() - 1);
			ori[1] = new Point(_source.getX() + width, _source.getY() - 1);
			ori[2] = new Point(_source.getX() + width, _source.getY() + height);
			ori[3] = new Point(_source.getX() + 1 + xdiff, _source.getY() + height);
			
		} else {
			Point topLeft = getTopLeftCrop();
			Point bottomRight = getBottomRightCrop();
			
			centre.x = _source.getX() + (bottomRight.x - topLeft.x) / 2;
			centre.y = _source.getY() + (bottomRight.y - topLeft.y) / 2;
			
			Rectangle clip = new Rectangle(topLeft.x + _source.getX(),
					topLeft.y + _source.getY(), bottomRight.x - topLeft.x,
					bottomRight.y - topLeft.y).getBounds();
//			_poly.addPoint((int) clip.getMinX() - 1, (int) clip.getMinY() - 1);
//			_poly.addPoint((int) clip.getMinX() - 1, (int) clip.getMaxY());
//			_poly.addPoint((int) clip.getMaxX(), (int) clip.getMaxY());
//			_poly.addPoint((int) clip.getMaxX(), (int) clip.getMinY() - 1);
			
			ori[0] = new Point((int) clip.getMinX() - 1, (int) clip.getMinY() - 1);
			ori[1] = new Point((int) clip.getMinX() - 1, (int) clip.getMaxY());
			ori[2] = new Point((int) clip.getMaxX(), (int) clip.getMaxY());
			ori[3] = new Point((int) clip.getMaxX(), (int) clip.getMinY() - 1);

		}
		
		AffineTransform.getRotateInstance(Math.PI * _rotate / 180, centre.x, centre.y).transform(ori, 0, rot, 0, 4);
		
		_poly = new Polygon();
		for(Point2D p : rot) {
			_poly.addPoint((int)p.getX(), (int)p.getY());
		}
	}

	@Override
	public double getEnclosedArea() {
		return getWidth() * getHeight();
	}

	@Override
	public void setWidth(Integer width) {
		_scale = width * 1F / (_end.x - _start.x);
	}

	public Point getStart() {
		return _start;
	}
	
	public Point getEnd() {
		return _end;
	}
	
	/**
	 * Gets the width with which the picture is displayed on the screen.
	 */
	@Override
	public Integer getWidth() {
		return Math.round(getUnscaledWidth() * _scale);
	}

	/**
	 * Gets the height with which the picture is displayed on the screen.
	 */
	@Override
	public int getHeight() {
		return Math.round(getUnscaledHeight() * _scale);
	}

	/**
	 * Dont paint links in audience mode for images.
	 */
	@Override
	protected void paintLink(Graphics2D g) {
		if (FrameGraphics.isAudienceMode())
			return;
		super.paintLink(g);
	}
	
	public void paintImageTiling(Graphics2D g) {
		if (_image == null) {
			return;
		}
		
		int iw = _image.getWidth(null);
		int ih = _image.getHeight(null);
		if(iw <= 0 || ih <= 0) {
			return;
		}
		
		int dX1 = _source.getX();
		int dY1 = _source.getY();
		int dX2 = _source.getX() + getWidth();
		int dY2 = _source.getY() + getHeight();
		
		BufferedImage tmp = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = tmp.createGraphics();
		int offX = (tmp.getWidth() - getWidth()) / 2;
		int offY = (tmp.getHeight() - getHeight()) / 2;
		
		// g2d.rotate(rotate, tmp.getWidth() / 2, tmp.getHeight() / 2);
		
		int cropStartX = _start.x;
		int cropEndX = _end.x;
    	if(cropEndX > iw) {
    		cropEndX = iw;
    	}
		for(int x = dX1; x < dX2; ) {
			// end - start = (cropEnd - cropStart) * scale
			// => cropEnd = cropStart + (end - start) / scale
			int w = (int) ((cropEndX - cropStartX) * _scale);
			int endX = x + w;
			if(endX > dX2) {
				endX = dX2;
				cropEndX = cropStartX + (int) ((dX2 - x) / _scale);
			}
			
			int cropStartY = _start.y;
			int cropEndY = _end.y;
        	if(cropEndY > ih) {
        		cropEndY = ih;
        	}
			for(int y = dY1; y < dY2; ) {
				int h = (int) ((cropEndY - cropStartY) * _scale);
				int endY = y + h;
				if(endY > dY2) {
					endY = dY2;
					cropEndY = cropStartY + (int) ((dY2 - y) / _scale);
				}
				
				int sx = _flipX ? cropEndX : cropStartX;
				int ex = _flipX ? cropStartX : cropEndX;
				int sy = _flipY ? cropEndY : cropStartY;
				int ey = _flipY ? cropStartY : cropEndY;
				g2d.drawImage(_image, x - dX1 + offX, y - dY1 + offY, endX - dX1 + offX, endY - dY1 + offY, sx, sy, ex, ey, null);
				
				cropStartY = 0;
				cropEndY = ih;
				
				y = endY;
			}
			
			cropStartX = 0;
			cropEndX = iw;
			
			x = endX;
		}
		
		AffineTransform at = new AffineTransform();
		at.translate(dX1, dY1);
		at.rotate(Math.PI * _rotate / 180, tmp.getWidth() / 2, tmp.getHeight() / 2);
		g.drawImage(tmp, at, _imageObserver);
		// g.drawImage(tmp, dX1, dY1, dX2, dY2, 0, 0, tmp.getWidth(), tmp.getHeight(), _imageObserver);
	}

	@Override
	public void paint(Graphics2D g) {
		if (_image == null)
			return;

		paintLink(g);

		// if we are showing the cropping, then show the original as transparent
		if (_showCropping && !isCropTooSmall()) {
			// show the full image as transparent
			float alpha = .5f;
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					alpha));
			
			paintImageTiling(g);

			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					1.0f));
			// show the cropped area normally
			Point topLeft = getTopLeftCrop();
			Point bottomRight = getBottomRightCrop();
			Shape clip = new Rectangle(_source.getX() + topLeft.x, _source.getY() + topLeft.y,
					bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
			g.setColor(getPaintHighlightColor());
			g.draw(clip);
			g.setClip(clip);

			paintImageTiling(g);
			
			g.draw(clip);
			// if the image is cropped, but we are not showing the cropping
			// otherwise, paint normally
		} else {
			paintImageTiling(g);
		}

		if (hasVisibleBorder()) {
			g.setColor(getPaintBorderColor());
			Stroke borderStroke = new BasicStroke(getThickness(), CAP, JOIN);
			g.setStroke(borderStroke);
			g.drawPolygon(getPolygon());
		}

		if (isHighlighted()) {
			Stroke borderStroke = new BasicStroke(1, CAP, JOIN);
			g.setStroke(borderStroke);
			g.setColor(getHighlightColor());
			g.drawPolygon(getPolygon());
		}
		
		//System.out.print("p_");	
	}

	@Override
	public Color getHighlightColor() {
		if (_highlightColor.equals(getBorderColor()))
			return ALTERNATE_HIGHLIGHT;
		return _highlightColor;
	}

	@Override
	public int setHighlightColor() {
		super.setHighlightColor();

		return Item.DEFAULT_CURSOR;
	}

	protected Picture createPicture() {
		return ItemUtils.CreatePicture((Text) _source.copy(), _imageObserver);
	}

	@Override
	public Picture copy() {
		Picture p = createPicture();
		p._image = _image;
		p._mode = _mode;
		// Doing Duplicate item duplicates link mark which we dont want to do
		// when in audience mode because the linkMark will be copied incorrectly
		// Get all properties from the source

		if (!isCropTooSmall() && _cropStart != null && _cropEnd != null) {
			assert (_cropEnd != null);
			// make the start be the top left
			// make the end be the bottom right
			Point topLeft = getTopLeftCrop();
			Point bottomRight = getBottomRightCrop();
			int startX = Math.round(topLeft.x / _scale) + _start.x;
			int startY = Math.round(topLeft.y / _scale) + _start.y;
			int endX = Math.round(bottomRight.x / _scale + _start.x);
			int endY = Math.round(bottomRight.y / _scale + _start.y);
			int width = _image.getWidth(null);
			int height = _image.getHeight(null);
			// adjust our start and end if the user has dragged outside of the
			// shape
			if (endX > width) {
				endX = width;
			}
			if (endY > height) {
				endY = height;
			}
			if (startX < 0) {
				startX = 0;
			}
			if (startY < 0) {
				startY = 0;
			}
			p._start = new Point(startX, startY);
			p._end = new Point(endX, endY);
			p._source.setPosition(topLeft.x + _source.getX(), topLeft.y
					+ _source.getY());
		} else {
			p._start = new Point(_start);
			p._end = new Point(_end);
		}
		p._scale = _scale;
		p._scaleType = _scaleType;
		p._path = _path;
		p._fileName = _fileName;

		p.updateSource();
		p.updatePolygon();

		return p;
	}

	public float getScale() {
		return _scale;
	}

	public void setScale(float scale) {
		_scale = scale;
	}
	
	public void scaleCrop() {
		// scale crop values to within image bounds
		int iw = _image.getWidth(null);
		int ih = _image.getHeight(null);
		if(iw > 0 || ih > 0) {
			while(_start.x >= iw) {
    			_start.x -= iw;
    			_end.x -= iw;
    		}
    		while(_start.y >= ih) {
    			_start.y -= ih;
    			_end.y -= ih;
    		}
    		while(_start.x < 0) {
    			_start.x += iw;
    			_end.x += iw;
    		}
    		while(_start.y < 0) {
    			_start.y += ih;
    			_end.y += ih;
    		}
		}
	}
	
	public void setCrop(int startX, int startY, int endX, int endY) {
		_start = new Point(startX, startY);
		_end = new Point(endX, endY);
		updateSource();
	}

	@Override
	public float getSize() {
		return _source.getSize();
	}

	@Override
	public void setSize(float size) {
		float diff = size - _source.getSize();
		float oldScale = _scale;

		float multiplier = (1000F + diff * 40F) / 1000F;
		_scale = _scale * multiplier;

		// picture must still be at least XX pixels wide
		if (getWidth() < MINIMUM_WIDTH) {
			_scale = oldScale;
		} else {
			_source.translate(new Point2D.Float(FrameMouseActions.MouseX,
					FrameMouseActions.MouseY), multiplier);
		}
		updateSource();
		updatePolygon();
		// Make sure items that are resized display the border
		invalidateAll();
	}

	@Override
	public void setAnnotation(boolean val) {
	}

	/**
	 * Returns the Image that this Picture object is painting on the screen.
	 * This is used by Frame to repaint animated GIFs.
	 * 
	 * @return The Image that this Picture object represents.
	 */
	public Image getImage() {
		return _image;
	}

	public Image getCroppedImage() {
		if (_image == null)
			return null;
		if (!isCropped()) {
			return _image;
		}
		
		return Toolkit.getDefaultToolkit().createImage(
				new FilteredImageSource(_image.getSource(),
						new CropImageFilter(_start.x, _start.y,
								getUnscaledWidth(), getUnscaledHeight())));
	}

	public int getUnscaledWidth() {
		return _end.x - _start.x;
	}

	public int getUnscaledHeight() {
		return _end.y - _start.y;
	}

	/**
	 * @return true if this is a cropped image.
	 */
	public boolean isCropped() {
		return (_end.x != 0 && _end.x != _image.getWidth(null)) || (_end.y != 0 && _end.y != _image.getHeight(null)) || _start.y != 0 || _start.x != 0;
	}

	@Override
	public boolean refresh() {
		// ImageIcon is faster, but cannot handle some formats
		// (notably.bmp) hence, we try this first, then if it fails we try
		// ImageIO
		try {
			_image = new ImageIcon(_path).getImage();
		} catch (Exception e) {
		}

		// if ImageIcon failed to read the image
		if (_image == null || _image.getWidth(null) <= 0) {
			try {
				_image = ImageIO.read(new File(_path));
			} catch (IOException e) {
				// e.printStackTrace();
				Logger.Log(e);
				_image = null;
				return false;
			}
		}
		return true;
	}

	@Override
	protected int getLinkYOffset() {
		return getBoundsHeight() / 2;
	}

	@Override
	public void setLinkMark(boolean state) {
		// TODO use the more efficient invalidiate method
		// The commented code below is not quite working
		// if(!state)
		// invalidateCommonTrait(ItemAppearence.LinkChanged);
		_source.setLinkMark(state);
		// if(state)
		// invalidateCommonTrait(ItemAppearence.LinkChanged);
		invalidateAll();
	}

	@Override
	public void setActionMark(boolean state) {
		// if (!state)
		// invalidateCommonTrait(ItemAppearence.LinkChanged);
		_source.setActionMark(state);
		// if (state)
		// invalidateCommonTrait(ItemAppearence.LinkChanged);
		invalidateAll();
	}

	@Override
	public boolean getLinkMark() {
		return !FrameGraphics.isAudienceMode() && _source.getLinkMark();
	}

	@Override
	public boolean getActionMark() {
		return _source.getActionMark();
	}

	@Override
	public String getName() {
		return _fileName;
	}
	
	public String getPath() {
		return _path;
	}
	
	/**
	 * Copies the image to the default images folder and updates the reference to it in Expeditee
	 * Used for correcting image references for FrameShare
	 */
	public void moveToImagesFolder() {
		File f = new File(getPath());
		// if the file is not in the default images folder, copy it there
		if(! f.getParentFile().equals(new File(FrameIO.IMAGES_PATH))) {
			try {
				File f2 = new File(FrameIO.IMAGES_PATH + f.getName());
                FrameUtils.copyFile(f, f2, false);
                f = f2;
            } catch (IOException e) {
                e.printStackTrace();
                f = null;
            }
		}
		_path = f.getPath();
		_fileName = f.getName();
		updateSource();
	}

	protected String getTagText() {
		return "@i: " + _fileName + " ";
	}

	/**
	 * Updates the source text for this item to match the current size of the
	 * image.
	 * 
	 */
	private void updateSource() {
		StringBuffer newText = new StringBuffer(getTagText());

		switch (_scaleType) {
		case (RATIO):
			DecimalFormat format = new DecimalFormat("0.00");
			newText.append(format.format(_scale));
			break;
		case (WIDTH):
			newText.append(getWidth());
			break;
		}
		
		scaleCrop();

		// If the image is cropped add the position for the start and finish of
		// the crop to the soure text
		if (_start.x > 0 || _start.y > 0 || _end.x != _image.getWidth(null)
				|| _end.y != _image.getHeight(null)) {
			newText.append(" ").append(_start.x).append(" ").append(_start.y);
			newText.append(" ").append(_end.x).append(" ").append(_end.y);
		}
		
		if(_flipX) {
			newText.append(" flipX");
		}
		if(_flipY) {
			newText.append(" flipY");
		}
		if(Double.compare(_rotate, 0) != 0) {
			newText.append(" rotation=" + _rotate);
		}

		_source.setText(newText.toString());
	}

	@Override
	public void translate(Point2D origin, double ratio) {
		_scale *= ratio;
		updateSource();
		super.translate(origin, ratio);
	}

	@Override
	public Rectangle[] getDrawingArea() {

		Rectangle[] da = super.getDrawingArea();

		if (getLink() != null || hasAction()) {
			Rectangle[] da2 = new Rectangle[da.length + 1];
			System.arraycopy(da, 0, da2, 0, da.length);
			da2[da.length] = getLinkPoly().getBounds();
			da2[da.length].translate(getX() - LEFT_MARGIN, getY()
					+ getLinkYOffset());
			da2[da.length].width += 2;
			da2[da.length].height += 2;
			da = da2;
		}

		return da;

	}

	@Override
	public void scale(Float scale, int originX, int originY) {
		setScale(getScale() * scale);
		super.scale(scale, originX, originY);
	}
	
	public void setFlipX(boolean flip) {
		_flipX = flip;
	}
	
	public void setFlipY(boolean flip) {
		_flipY = flip;
	}
	
	public boolean getFlipX() {
		return _flipX;
	}
	
	public boolean getFlipY() {
		return _flipY;
	}
	
	public void setRotate(double rotate) {
		_rotate = rotate;
		updateSource();
		updatePolygon();
	}
	
	public double getRotate() {
		return _rotate;
	}

}
