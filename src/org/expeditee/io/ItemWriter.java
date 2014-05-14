package org.expeditee.io;

import java.io.IOException;
import java.util.List;

import org.expeditee.items.Circle;
import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetEdge;

public abstract class ItemWriter {

	public static final String NEW_LINE = System.getProperty("line.separator");

	protected void writeTitle(Text toWrite, List<Item> items)
			throws IOException {
	}

	protected void writeAnnotationTitle(Item toWrite) throws IOException {
	}

	/**
	 * Called for each item on each frame to write out the contents of the
	 * items.
	 * 
	 * @param toWrite
	 *            the item to be written out.
	 * @throws IOException
	 */
	protected void writeItem(Item toWrite) throws IOException {
		if (toWrite.isAnnotation()) {
			writeAnnotationItem(toWrite);
			return;
		}

		if (toWrite instanceof Text)
			writeText((Text) toWrite);

		if (toWrite instanceof Picture)
			writePicture((Picture) toWrite);

		if (toWrite instanceof Line) {
			if (toWrite instanceof WidgetEdge) {
				writeWidget(((WidgetEdge) toWrite).getWidgetSource());
			}
			writeLine((Line) toWrite);
		}

		if (toWrite instanceof Dot)
			writeDot((Item) toWrite);

		if (toWrite instanceof Circle)
			writeCircle((Circle) toWrite);
	}

	protected void writeAnnotationItem(Item toWrite) throws IOException {
		if (toWrite instanceof Text)
			writeAnnotationText((Text) toWrite);

		if (toWrite instanceof Picture)
			writeAnnotationPicture((Picture) toWrite);

		if (toWrite instanceof Line) {
			writeAnnotationLine((Line) toWrite);
		}

		if (toWrite instanceof Dot)
			writeAnnotationDot((Item) toWrite);
	}

	protected void writeAnnotationText(Text toWrite) throws IOException {
	}

	protected void writeAnnotationPicture(Picture toWrite) throws IOException {
	}

	protected void writeAnnotationLine(Line toWrite) throws IOException {
	}

	protected void writeAnnotationDot(Item toWrite) throws IOException {
	}

	protected void writeText(Text toWrite) throws IOException {
	}

	protected void writePicture(Picture toWrite) throws IOException {
	}

	protected void writeLine(Line toWrite) throws IOException {
	}

	protected void writeCircle(Circle toWrite) throws IOException {
	}

	protected void writeWidget(InteractiveWidget toWrite) throws IOException {
	}

	protected void writeDot(Item toWrite) throws IOException {
	}
}
