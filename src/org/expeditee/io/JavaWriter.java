package org.expeditee.io;

import java.awt.Desktop;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.expeditee.gui.Browser;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameUtils;
import org.expeditee.io.flowlayout.AreaPolygon;
import org.expeditee.io.flowlayout.DimensionExtent;
import org.expeditee.io.flowlayout.XGroupItem;
import org.expeditee.io.flowlayout.XItem;
import org.expeditee.io.flowlayout.XOrderedLine;
import org.expeditee.io.flowlayout.XRawItem;
import org.expeditee.io.flowlayout.YOverlappingItemsShadow;
import org.expeditee.io.flowlayout.YOverlappingItemsSpan;
import org.expeditee.io.flowlayout.YOverlappingItemsTopEdge;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Text;

public class JavaWriter extends DefaultTreeWriter {

	
	// may be needed for sectioning commands
	private Text _title = null;

	
	
	@Override
	protected void initialise(Frame start, Writer writer) throws IOException {
		_format = ".java";
		super.initialise(start, writer);
	}

	protected String getFileName(Frame start) {
		String start_title = start.getTitle();
		
		String start_filename = start_title.replaceAll("\\.", "/");
		
		return getValidFilename(start_filename);
	}
	
	protected void writeTitle(Text title, List<Item> items) throws IOException {
		_title = title;

	}
	
	
	
	
	@Override
	protected List<Item> getSortedItems(Frame frame) 
	{
		List<Item> y_ordered_items = frame.getItems();
		
		XGroupItem toplevel_xgroup = new XGroupItem(frame,y_ordered_items);
		
		// ... following on from Steps 1 and 2 in the Constructor in XGroupItem ...
		
		// Step 3: Reposition any 'out-of-flow' XGroupItems
		toplevel_xgroup.repositionOutOfFlowGroups(toplevel_xgroup);
		
		// Step 4: Now add in the remaining (nested) XGroupItems
		List<XGroupItem> grouped_item_list = toplevel_xgroup.getGroupedItemList();
		toplevel_xgroup.mapInXGroupItemsRecursive(grouped_item_list);
	
		// Finally, retrieve linear list of all Items, (ordered, Y by X, allowing for overlap, nested-boxing, and arrow flow)
		
		List<Item> overlapping_y_ordered_items = toplevel_xgroup.getYXOverlappingItemList();	
		
		return overlapping_y_ordered_items;
		
		
		/*
		List<Text> raw_text_item_list = new ArrayList<Text>();
		List<XGroupItem> grouped_item_list = new ArrayList<XGroupItem>();
		List<Item> remaining_item_list = new ArrayList<Item>();
		
		XGroupItem.separateYOverlappingItems(frame, y_ordered_items, raw_text_item_list, grouped_item_list, remaining_item_list);
		*/
		
		
	}
	
	@Override
	protected String finalise() throws IOException {
		try {
			_writer.flush();
			_writer.close();
		} catch (IOException ioe) {
		} finally {
			_writer.close();
		}
		
		return " exported to " + _output;
	}
	


	@Override
	protected void writeText(Text text) throws IOException {
		for (String s : text.getTextList()) {
			_writer.write(s);
			_writer.write(ItemWriter.NEW_LINE);
		}
	}

	@Override
	protected void writeAnnotationText(Text text) throws IOException {

	}
}

