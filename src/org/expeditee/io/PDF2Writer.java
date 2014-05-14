package org.expeditee.io;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.items.Circle;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.settings.UserSettings;

import com.lowagie.text.Anchor;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

/**
 * @author jts21
 *
 */
public class PDF2Writer extends DefaultTreeWriter {
	
	private class OrderedHashSet<T> implements Set<T> {
		private List<T> _orderedList;
		private HashSet<T> _hashSet;
		public OrderedHashSet() {
			super();
			_orderedList = new LinkedList<T>();
			_hashSet = new HashSet<T>();
		}
		
		@Override
		public Iterator<T> iterator() {
			return _orderedList.iterator();
		}
		
		@Override
		public void clear() {
			_hashSet.clear();
			_orderedList.clear();
		}
		
		@Override
		public boolean add(T o) {
			return _hashSet.add(o) && _orderedList.add(o);
		}
		
		@Override
        public boolean remove(Object o) {
	        return _hashSet.remove(o) && _orderedList.remove(o);
        }

		@Override
        public int size() {
	        return _orderedList.size();
        }

		@Override
        public boolean isEmpty() {
	        return _orderedList.isEmpty();
        }

		@Override
        public boolean contains(Object o) {
	        return _hashSet.contains(o);
        }

		@Override
        public Object[] toArray() {
	        return _orderedList.toArray();
        }

		@SuppressWarnings("hiding")
        @Override
        public <T> T[] toArray(T[] a) {
	        return _orderedList.toArray(a);
        }

		@Override
        public boolean containsAll(Collection<?> c) {
	        return _hashSet.containsAll(c);
        }

		@Override
        public boolean addAll(Collection<? extends T> c) {
	        return _hashSet.addAll(c) && _orderedList.addAll(c);
        }

		@Override
        public boolean retainAll(Collection<?> c) {
	        return _hashSet.retainAll(c) && _orderedList.retainAll(c);
        }

		@Override
        public boolean removeAll(Collection<?> c) {
	        return _hashSet.removeAll(c) && _orderedList.removeAll(c);
        }
	}
	
	private Document _pdfDocument;
	private Dimension _pageSize;
	private PdfWriter _pdfWriter;
	private float _height;
	private boolean _first = true;
	private OrderedHashSet<Frame> _frames = null;

	public PDF2Writer() {
		_pageSize = FrameGraphics.getMaxSize();
		_pdfDocument = new Document(new Rectangle(_pageSize.width, _pageSize.height));
	}

	@Override
	protected void initialise(Frame start, Writer writer) throws IOException {
		_format = ".pdf";
		super.initialise(start, writer);
		try {
			_pdfWriter = PdfWriter.getInstance(_pdfDocument,
					new FileOutputStream(_filename));
			_pdfDocument.open();
			_pdfDocument.addCreationDate();
			_pdfDocument.addAuthor(UserSettings.UserName.get());
			_pdfDocument.addCreator("Expeditee");
			_pdfDocument.addTitle(start.getTitle());
			_height = _pdfWriter.getPageSize().getHeight();
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
	}
	
	@Override
	protected String finalise() throws IOException {
		_pdfDocument.close();
		return super.finalise();
	}
	
	/**
	 * Used to make sure that only links with valid targets are used
	 * @param start
	 */
	private boolean findValidLinksRecursive(Frame current, Set<Frame> known) {
		if(current == null || known.contains(current))
			return false;
		known.add(current);
		
		Frame child;
		for(Item i : current.getItems()) {
			if(i.isAnnotation())
				continue;
			if((child = i.getChild()) != null)
				findValidLinksRecursive(child, known);
		}
		
		return true;
	}
	
	@Override
	protected void outputTree(Frame toWrite) throws IOException {
		if (toWrite == null)
			return;
		_frames = new OrderedHashSet<Frame>();
		
		findValidLinksRecursive(toWrite, _frames);
		
		for(Frame f : _frames) {
			if(f == null)
				continue;
			writeStartFrame(f);
    		Text title = f.getTitleItem();
    		for(Item i : f.getItems()) {
    			if(i != title) {
    				this.writeItem(i);
    			}
    		}
    		// draw title last since it needs to be handled separately from other items,
    		// and also drawn on top of shapes
    		if (title != null) {
    			if (title.isAnnotation())
    				this.writeAnnotationTitle(title);
    			else
    				this.writeTitle(title, f.getItems());
    		}
		}
	}
	
	@Override
	protected void writeStartFrame(Frame starting) throws IOException {
		// start a new page
		if(!_first) {
			_pdfDocument.newPage();
		} else {
			_first = false;
		}
		// set bg color
		PdfContentByte cb = _pdfWriter.getDirectContent();
		cb.setColorFill(starting.getPaintBackgroundColor());
		cb.rectangle(0, 0, _pageSize.width, _pageSize.height);
		cb.fill();
		// super.writeStartFrame(starting);
	}
	
	private void drawPolygon(PdfContentByte cb, Polygon poly, Color fill, Color line, float lineThickness) {
		if(poly != null) {
			cb.moveTo(poly.xpoints[0], _height - poly.ypoints[0]);
			for(int i = 1; i < poly.npoints; i++) {
				cb.lineTo(poly.xpoints[i], _height - poly.ypoints[i]);
			}
			cb.closePath();
			if(fill != null) {
				cb.setColorFill(fill);
				if(lineThickness > 0) {
    				cb.setLineWidth(lineThickness);
    				cb.setColorStroke(line);
    				cb.fillStroke();
    			} else {
    				cb.fill();
    			}
			} else {
				cb.setLineWidth(lineThickness);
				cb.setColorStroke(line);
				cb.stroke();
			}
		}
	}
	
	private void drawMark(PdfContentByte cb, Item i, float x, float y) {
		if(i == null) return;
		boolean hasLink = i.getLink() != null;
		
		if (hasLink) {
			
			Color lineColor = Color.BLACK, fillColor = null;
			Polygon poly = i.getLinkPoly();
			poly = new Polygon(poly.xpoints, poly.ypoints, poly.npoints);
			poly.translate((int) (x - Item.LEFT_MARGIN), (int) (_height - y - i.getBoundsHeight() / 2));
			
			drawPolygon(cb, poly, fillColor, lineColor, 0.5f);
		}
	}
	
	private void writeAnchor(PdfContentByte cb, Item i, Anchor a, float x, float y) {
		if(i.getParent() != null && i.getParent().getTitleItem()!=null && i.getParent().getTitleItem().equals(i)) {
			a.setName(i.getParent().getName());
		}
		if(_frames != null && i.getChild() != null && _frames.contains(i.getChild())) {
			a.setReference("#" + i.getAbsoluteLink());
			drawMark(cb, i, x, y);
		}
		
		ColumnText.showTextAligned(cb, PdfContentByte.ALIGN_LEFT, a, x, y, 0);
	}
	
	@Override
	protected void writeTitle(Text title, List<Item> items) throws IOException {
		writeText(title);
	}
	
	@Override
	protected void writeText(Text text) throws IOException {
		PdfContentByte cb = _pdfWriter.getDirectContent();
		Font font = FontFactory.getFont(
				Conversion.getPdfFont(text.getFamily()), text.getSize(), text
						.getPaintFont().getStyle(), text.getPaintColor());
		
		// we draw some text on a certain position
		float x = text.getX(), y = _pdfWriter.getPageSize().getHeight() - text.getY();
		List<String> textLines = text.getTextList();
		for(int i = 0; i < textLines.size(); i++){
			Chunk chunk = new Chunk(textLines.get(i), font);
			Anchor anchor = new Anchor(chunk);
			writeAnchor(cb, text, anchor, x, y);
			y -= text.getLineHeight();
		}
	}

	@Override
	protected void writePicture(Picture pic) throws IOException {
		Image image = pic.getCroppedImage();
		try {
			PdfContentByte cb = _pdfWriter.getDirectContent();
			com.lowagie.text.Image iTextImage = com.lowagie.text.Image.getInstance(image, null);
			float angle = (float) (pic.getRotate() * Math.PI / 180);
			double sin = Math.sin(angle), cos = Math.cos(angle);
			int w = pic.getWidth(), h = pic.getHeight();
			iTextImage.scalePercent(pic.getScale() * 100);
			iTextImage.setRotation(-angle);
			Chunk chunk = new Chunk(iTextImage, 0, 0);
			Anchor anchor = new Anchor(chunk);
			writeAnchor(cb, pic, anchor,
					(float) (pic.getX() + (w - Math.abs(w * cos + h * sin)) / 2),
					(float) (_height - pic.getY() - (h + Math.abs(w * sin + h * cos)) / 2));
			// cb.addImage(iTextImage);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void writeCircle(Circle circle) {
		PdfContentByte cb = _pdfWriter.getDirectContent();
		cb.circle(circle.getCenter().getX(), _pdfWriter.getPageSize().getHeight() - circle.getCenter().getY(), (float) circle.getRadius());
		cb.setColorFill(circle.getFillColor());
		if(circle.getThickness() > 0) {
			cb.setLineWidth(circle.getThickness());
			cb.setColorStroke(circle.getPaintColor());
			cb.fillStroke();
		} else {
			cb.fill();
		}
	}
	
	private List<Line> seenLines = new LinkedList<Line>();
	@Override
	protected void writeLine(Line line) throws IOException {
		if(seenLines.contains(line)) {
			return;
		}
		PdfContentByte cb = _pdfWriter.getDirectContent();
		if(line.getStartItem().isEnclosed()) {
			seenLines.add(line);
			Line currentLine = line;
			Item currentItem = line.getStartItem();
			cb.moveTo(currentItem.getX(), _height - currentItem.getY());
			for(;;) {
				currentItem = currentLine.getOppositeEnd(currentItem);
				cb.lineTo(currentItem.getX(), _height - currentItem.getY());
				for(Line l : currentItem.getLines()) {
					if(l.equals(currentLine)) {
						continue;
					}
					currentLine = l;
					break;
				}
				if(currentLine == null || currentLine.equals(line)) {
					break;
				}
				seenLines.add(currentLine);
			}
			cb.closePath();
			Color fill = currentItem.getFillColor();
			if(fill != null) {
    			cb.setColorFill(fill);
    			if(currentItem.getThickness() > 0) {
    				cb.setLineWidth(currentItem.getThickness());
    				cb.setColorStroke(currentLine.getPaintColor());
    				cb.fillStroke();
    			} else {
    				cb.fill();
    			}
			} else if(currentItem.getThickness() > 0) {
				cb.setLineWidth(currentItem.getThickness());
				cb.setColorStroke(currentLine.getPaintColor());
				cb.stroke();
			}
		} else {
			for(Item l : line.getStartItem().getAllConnected()) {
				if(l instanceof Line && !seenLines.contains(l)) {
					seenLines.add((Line) l);
					Item start = ((Line) l).getStartItem();
					Item end = ((Line) l).getEndItem();
					cb.moveTo(start.getX(), _height - start.getY());
					cb.lineTo(end.getX(), _height - end.getY());
					cb.closePath();
					if(l.getThickness() >= 0) {
						cb.setLineWidth(l.getThickness());
						cb.setColorStroke(l.getPaintColor());
						cb.stroke();
					}
					drawPolygon(cb, ((Line) l).getStartArrow(), l.getPaintColor(), l.getPaintColor(), l.getThickness());
					drawPolygon(cb, ((Line) l).getEndArrow(), l.getPaintColor(), l.getPaintColor(), l.getThickness());
				}
			}
		}
	}
	
	@Override
	protected Collection<Item> getItemsToWrite(Frame toWrite) {
		return toWrite.getVisibleItems();
	}
}
