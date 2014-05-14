package org.expeditee.io.flowlayout;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameUtils;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Text;


public class XGroupItem extends XItem 
{
	enum FlowType { in_flow, out_of_flow_original, out_of_flow_faked_position };

	class MultiArrowHeadComparable implements Comparator<Item>{
		 
	    @Override
	    public int compare(Item o1, Item o2) 
	    {
	    	int o1y = o1.getY();
	    	int o2y = o2.getY();
	    	
	    	int order = 0; // default to assume they are identical
	    	
	    	if (o1y<o2y) {
	    		order = -1; // done, o1 is higher up the frame than o2 => our definition of 'before'
	    	}
	    	else if (o2y<o1y) {
	    		order = +1; // also done, o1 is lower down the frame than o2 => our definition of 'after'
	    		
	    	}
	    	else {
	    		// have identical 'y' values, so look to 'x' to tie-break
	    		
	    		int o1x = o1.getX();
		    	int o2x = o2.getX();
		    	
		    	if (o1x<o2x) {
		    		order = -1; // done, o1 is to the left of o2 => our definition of 'before'
		    	}
		    	else if (o2x<o1x) {
		    		order = +1; // also done, o1 is to the right of o2  => our definition of 'after'
		    	}
	    	}
	    		
	    	return order;
	    }
	}
	
	Frame frame;
	
	protected int y_span_height;
	protected YOverlappingItemsSpan[] yitems_span_array;
	
	List<Text> raw_text_item_list;
	List<XGroupItem> grouped_item_list;
	List<Item> remaining_item_list;
	
	FlowType out_of_flow;
	
	protected XGroupItem()
	{
		frame = null;
		
		y_span_height = 0;
		yitems_span_array = null;
		
		raw_text_item_list = null;
		grouped_item_list = null;
		remaining_item_list = null;
		
		out_of_flow = FlowType.in_flow;
	}
	
	public XGroupItem(Frame frame, List<Item> y_ordered_items, Polygon enclosing_polygon) 
	{
		this.frame = frame;
		this.out_of_flow = FlowType.in_flow;;
		
		if (enclosing_polygon == null) {
			// e.g. when the top-level case, with no enclosing polygon at this stage
			// => generate one based on the bounding box of the y_ordered_items
			enclosing_polygon = DimensionExtent.boundingBoxPolygon(y_ordered_items);
		}
		
		Rectangle enclosing_bounding_rect = enclosing_polygon.getBounds(); 
		initSpanArray(enclosing_bounding_rect);
		
		// Step 1: Separate out the raw-text-items, the grouped-items and 'remaining' (e.g. points and lines)
		
		raw_text_item_list = new ArrayList<Text>();
		grouped_item_list = new ArrayList<XGroupItem>();
		remaining_item_list = new ArrayList<Item>();
		
		separateYOverlappingItems(frame, y_ordered_items, enclosing_polygon, raw_text_item_list, grouped_item_list, remaining_item_list);
		
	
		// Step 2: Add in the raw-text items
		for (Text text_item : raw_text_item_list) {
			
			XRawItem x_raw_item = new XRawItem(text_item);
			mapInItem(x_raw_item);
		}
		
	}
	
	public XGroupItem(Frame frame, List<Item> y_ordered_items) 
	{
		this(frame,y_ordered_items,null);
	}
	
	protected XGroupItem(XGroupItem imprint, Rectangle copy_to_bounding_rect)
	{
		super();
		
		// Implement a shallow copy => share references to frame, and item list, and y-span
		// Only the bounding box is changed => set to the 'copy_to_bounding_rect' passed in
		
		this.frame = imprint.frame;
		
		y_span_height = imprint.y_span_height;
		yitems_span_array = imprint.yitems_span_array;
		
		this.raw_text_item_list  = imprint.raw_text_item_list;
		this.grouped_item_list   = imprint.grouped_item_list;
		
//		int offX = imprint.getBoundingRect().x - copy_to_bounding_rect.x;
//		int offY = imprint.getBoundingRect().y - copy_to_bounding_rect.y;
//		this.grouped_item_list   = new ArrayList<XGroupItem>();
//		for(XGroupItem newChild : imprint.grouped_item_list) {
//			Rectangle newRect = newChild.getBoundingRect();
//			newRect.x -= offX;
//			newRect.y -= offY;
//			this.grouped_item_list.add(new XGroupItem(newChild, newRect));
//		}
		this.remaining_item_list = imprint.remaining_item_list;
		
		this.out_of_flow = imprint.out_of_flow; // Or perhaps set it to FlowType.out_of_flow_fake_position straight away?
		
		this.bounding_rect = new Rectangle(copy_to_bounding_rect); // deep copy to be on the safe side
	}
	
	
	protected void initSpanArray(Rectangle bounding_rect)
	{
		this.bounding_rect = bounding_rect;
		
		int y_top = getBoundingYTop();
		int y_bot = getBoundingYBot();
		
		if (y_top<=y_bot) {
			// => have non-trivial span
		
			y_span_height = (y_bot - y_top) + 2;  // Polygon in Java excludes right and bottom sides so need extra "+1" in calc
		
			yitems_span_array = new YOverlappingItemsSpan[y_span_height]; 
		}
		else {
			y_span_height = 0;
			yitems_span_array = null;
		}
	}
	
	public YOverlappingItemsSpan getSpanItemAt(int index) {
	    return yitems_span_array[index];
	}
	
	public void setOutOfFlow(FlowType flow_type)
	{
		out_of_flow = flow_type;
	}
	
	public FlowType getFlowItem() 
	{
		return out_of_flow;
	}
	
	public boolean isOriginalOutOfFlowItem() 
	{
		return out_of_flow == FlowType.out_of_flow_original;
	}
	
	public boolean isFakedOutOfFlowItem() 
	{
		return out_of_flow == FlowType.out_of_flow_faked_position;
	}

	
	public List<Text> getRawTextItemList()
	{
		return raw_text_item_list;
	}
	
	public List<XGroupItem> getGroupedItemList()
	{
		return grouped_item_list;
	}
	
	public List<Item> getRemainingItemList()
	{
		return remaining_item_list;
	}
	
	public YOverlappingItemsSpan getYOverlappingItemsSpan(int y) 
	{
		int y_index = y - getBoundingYTop();
		
		if ((y_index<0) || (y_index>=yitems_span_array.length)) {
		    int y_top = getBoundingYTop();
		    int y_bot = y_top + yitems_span_array.length -1;
		    System.err.println("Error in getYOverlappingItemsSpan(): index out of bounds for value " + y);
		    System.err.println("  => Operation mapped into local array: y-top=" + y_top + ", y-bot=" + y_bot);
		    return null;
		}
		return yitems_span_array[y_index];
	}
	
	public void setYOverlappingItemsSpan(int y,YOverlappingItemsSpan yitems_span) 
	{
		int y_index = y - getBoundingYTop();

		if ((y_index<0) || (y_index>=yitems_span_array.length)) {
		    int y_top = getBoundingYTop();
		    int y_bot = y_top + yitems_span_array.length -1;
		    System.err.println("Error in setYOverlappingItemsSpan(): index out of bounds for value " + y);
		    System.err.println("  => Operation mapped into local array: y-top=" + y_top + ", y-bot=" + y_bot);
		    return;
		}
		yitems_span_array[y_index] = yitems_span;
	}
	
	
	protected List<Item> followLinesToArrowHeads(Collection<Item> visited, Item anchor_item, List<Line>used_in_lines)
	{
		List<Item> arrow_head_endpoints = new ArrayList<Item>();
		
		for (Line line: used_in_lines) {
			
			Item start_item = line.getStartItem();
			
			if (start_item == anchor_item) {
				// the line we're considering is heading in the right direction
				
				Item end_item = line.getEndItem();

				if (!visited.contains(end_item)) {
					// Needs processing
					visited.add(end_item);

					List<Line> follow_lines = end_item.getLines();
					
					if (follow_lines.size()==1) {
						// reached an end-point
						if (end_item.hasVisibleArrow()) {
							arrow_head_endpoints.add(end_item);
						}
					}
					else if (follow_lines.size()>1) {
						
						List<Item> followed_arrow_heads = followLinesToArrowHeads(visited,end_item,follow_lines);
						arrow_head_endpoints.addAll(followed_arrow_heads);
					}
				}
			}
			
		}
		return arrow_head_endpoints;
	}
	

	protected XGroupItem innermostXGroup(Item arrow_head, List<XGroupItem> grouped_item_list)
	{
		XGroupItem innermost_item = null;
		
		for (XGroupItem xgroup_item: grouped_item_list) {
			if (xgroup_item.containsItem(arrow_head)) {
				
				innermost_item = xgroup_item;
				
				// Now see if it is in any of the nested ones?
				
				List<XGroupItem> nested_group_item_list = xgroup_item.grouped_item_list;
				
				XGroupItem potentially_better_item = innermostXGroup(arrow_head,nested_group_item_list);
						
				if (potentially_better_item != null) {
					innermost_item = potentially_better_item;
				}
				break;
			}
			
		}
		
		return innermost_item;
	}
	
	protected void removeXGroupItem(XGroupItem remove_item, List<XGroupItem> grouped_item_list)
	{
		
		for (XGroupItem xgroup_item: grouped_item_list) {
			
			if (xgroup_item == remove_item) {
				grouped_item_list.remove(xgroup_item);
				return;
			}
			else {
				List<XGroupItem> nested_group_item_list = xgroup_item.grouped_item_list;
				removeXGroupItem(remove_item,nested_group_item_list);
				
			}
		}
	}

	
	
	protected void repositionOutOfFlowGroupsFollowLine(XGroupItem toplevel_xgroup, Item remaining_item, Collection<XGroupItem> out_of_flow)
	{
		// See if this item is the start of a line
		// Follow it if it is
		// For each end of the line (potentially a multi-split poly-line) that has an arrow head:
		//   See if the arrow-head falls inside an XGroup area
		//   => Ignore endings that are in the same XGroupItem as the line started in
		
		List<Line> used_in_lines = remaining_item.getLines();
		if (used_in_lines.size()==1) {
			// at the start (or end) of a line
			
			Item start_item = used_in_lines.get(0).getStartItem();

			if (remaining_item == start_item) {

				// found the start of a line
				
				Collection<Item> visited = new HashSet<Item>();
				visited.add(remaining_item);

				List<Item> arrow_head_endpoints = followLinesToArrowHeads(visited,remaining_item,used_in_lines);

				//System.out.println("**** For Xgroup " + this + " with dot starting at " + remaining_item + " has " + arrow_head_endpoints.size() + " arrow head endpoints");

				Collections.sort(arrow_head_endpoints, new MultiArrowHeadComparable());
								
				for (Item arrow_head: arrow_head_endpoints) {
					// find the inner-most group it falls within
					
					List<XGroupItem> toplevel_grouped_item_list = toplevel_xgroup.getGroupedItemList();
					
					XGroupItem xgroup_item = innermostXGroup(arrow_head,toplevel_grouped_item_list); 
					
					if (xgroup_item != null) {
						
						// Ignore if the found 'xgroup_item' is 'this' 
						// (i.e. the found arrow head is at the same XGroupItem level we are currently processing)
						
						if (xgroup_item != this) {
							
							out_of_flow.add(xgroup_item);
							// Can't delete here, as it causes a concurrent exception => add to 'out_of_flow' hashmap and perform removal later
							
							//System.out.println("**** innermost XGroupItem = " + xgroup_item);

							// Artificially rework its (x,y) org and dimension to make it appear where the start of the arrow is

							Rectangle start_rect = start_item.getArea().getBounds();
							
							
							XGroupItem xgroup_item_shallow_copy = new XGroupItem(xgroup_item,start_rect);
							
							// Perhaps the following two lines should be moved to inside the constructor??
							
							xgroup_item.setOutOfFlow(FlowType.out_of_flow_original);
							xgroup_item_shallow_copy.setOutOfFlow(FlowType.out_of_flow_faked_position);
							
							
							//xgroup_item.setBoundingRect(start_rect);
							//xgroup_item.setOutOfFlow();
							
							// Finally add it in 
							//mapInItem(xgroup_item);
							mapInItem(xgroup_item_shallow_copy);
						}
					}
				}
			}

		}
	}
	
	/** 	
	  *	Look for any 'out-of-flow' XGroup boxes, signalled by the user drawing an arrow line to it
	  *	 => force an artificial change to such boxes so its dimensions become point becomes the starting point
	  *	    of the arrow line.  This will make it sort to the desired spot in the YOverlapping span
	  */
	
	public void repositionOutOfFlowGroupsRecursive(XGroupItem toplevel_xgroup, Collection<XGroupItem> out_of_flow)
	{
		// Map in all the items in the given list:
		for (Item remaining_item: remaining_item_list) {

			repositionOutOfFlowGroupsFollowLine(toplevel_xgroup,remaining_item,out_of_flow); 
		}	

		// Now recursively work through each item's nested x-groups 
		for (XGroupItem xgroup_item: grouped_item_list) {

			if (!out_of_flow.contains(xgroup_item)) {
				xgroup_item.repositionOutOfFlowGroupsRecursive(toplevel_xgroup,out_of_flow);
			}
		}	
	}
	
	
	public void repositionOutOfFlowGroups(XGroupItem toplevel_xgroup)
	{
		Collection<XGroupItem> out_of_flow = new HashSet<XGroupItem>();

		repositionOutOfFlowGroupsRecursive(toplevel_xgroup,out_of_flow); 
		
		List<XGroupItem >toplevel_grouped_item_list = toplevel_xgroup.getGroupedItemList();

		// ****
		
		// Want to remove the original "out-of-position" blocks that were found, as (for each arrow
		// point to such an original) shallow-copies now exist with 'faked' positions that correspond
		// to where the arrows start.
		
		
		// No longer remove them from the nested grouped structure, rather, rely on these items being 
		// tagged "isOutOfFLow()" to be skipped by subsequent traversal calls (e.g., to generate
		// the normal "in-flow" nested blocks)
		
		// Structuring things this way, means that it is now possible to have multiple arrows
		// pointing to the same block of code, and both "out-of-flow" arrows will be honoured,
		// replicating the code at their respective start points 
		
		/*
		for (XGroupItem xgroup_item: out_of_flow) {
			removeXGroupItem(xgroup_item,toplevel_grouped_item_list);
		}
		
	*/
		
	}
	
	/**
	 * Focusing only on enclosures that fit within the given polygon *and* have this item
	 * in it, the method finds the largest of these enclosures and 
	 * returns all the items it contains
	 * 
	 * @return
	 */
	public Collection<Item> getItemsInNestedEnclosure(Item given_item, AreaPolygon outer_polygon) 
	{
		Collection<Item> sameEnclosure = null;
		Collection<Item> seen = new HashSet<Item>();
		
		double enclosureArea = 0.0;
	
		for (Item i : frame.getItems()) {
			
			 // Go through all the enclosures ... 
			 
			if (!seen.contains(i) && i.isEnclosed()) {
				
				Collection<Item> i_enclosing_dots_coll = i.getEnclosingDots();
				List<Item> i_enclosing_dots = new ArrayList<Item>(i_enclosing_dots_coll); // Change to List of items
				
				seen.addAll(i_enclosing_dots);
				
				Polygon i_polygon = new Polygon();
				for (int di=0; di<i_enclosing_dots.size(); di++) {
					Item d = i_enclosing_dots.get(di);
					
					i_polygon.addPoint(d.getX(), d.getY());
				}
				
				// ... looking for one that is completely contained in the 'outer_polygon' and ... 
				if (outer_polygon.completelyContains(i_polygon)) {

					Collection<Item> enclosed = i.getEnclosedItems();
					
					// ... includes this item 
					if (enclosed.contains(given_item)) {

						// Remember it if it is larger than anything we've encountered before 
						if ((i.getEnclosedArea() > enclosureArea)) {
							sameEnclosure = enclosed;
						}
					}
				}
				
			}
		}

		if (sameEnclosure == null)
			return new LinkedList<Item>();

		return sameEnclosure;
	}
	
	
	public void separateYOverlappingItems(Frame frame, List<Item> item_list, Polygon enclosing_polygon,
			 List<Text> raw_text_item_list, List<XGroupItem> grouped_item_list, List<Item> remaining_item_list)
	{
		// raw_text_items_list => all the non-enclosed text items
		// grouped_items_list  => list of all enclosures containing text items 
		// remaining_item_list => whatever is left: mostly dots that form part of lines/polylines/polygons

		AreaPolygon area_enclosing_polygon = new AreaPolygon(enclosing_polygon);
		
		List<AreaPolygon> area_enclosed_polygon_list = new ArrayList<AreaPolygon>();

		while (item_list.size()>0) {
			Item item = item_list.remove(0); // shift

			if (item instanceof Text) {
				Text text = (Text)item;

				Collection<Item> items_in_nested_enclosure_coll = getItemsInNestedEnclosure(text,area_enclosing_polygon);
				List<Item>  items_in_nested_enclosure = new ArrayList<Item>(items_in_nested_enclosure_coll); // Change to handling it as a list
				
				if (items_in_nested_enclosure.size()==0) {
					
					raw_text_item_list.add(text);
				}
				else {
				
					// Something other than just this text-item is around
					
					while (items_in_nested_enclosure.size()>0) {
						
						Item enclosure_item = items_in_nested_enclosure.remove(0); // shift
								
						Polygon enclosed_polygon = enclosure_item.getEnclosedShape();
						
						if (enclosed_polygon!=null) {
							// Got a group 
							// => Remove any items-in-nested-enclosure from:
							//
							//     'item_list' (so we don't waste our time discovering and processing again these related items)
							//   and 
							//      'raw_text_item_list' and 'remaining_item_lst' (as we now know they are part of the nested enclosed group)
							
							AreaPolygon area_enclosed_polygon = new AreaPolygon(enclosed_polygon);
							area_enclosed_polygon_list.add(area_enclosed_polygon);

							item_list.remove(enclosure_item);
							item_list.removeAll(items_in_nested_enclosure); 
							
							raw_text_item_list.remove(enclosure_item);
							raw_text_item_list.removeAll(items_in_nested_enclosure); 
							
							remaining_item_list.remove(enclosure_item);
							remaining_item_list.removeAll(items_in_nested_enclosure); 
							
							// Now remove any of the just used enclosed-items if they formed part of the perimeter
							List<Item> items_on_perimeter = new ArrayList<Item>();
							
							Iterator<Item> item_iterator = items_in_nested_enclosure.iterator();
							while (item_iterator.hasNext()) {
								Item item_to_check = item_iterator.next();
								Point pt_to_check = new Point(item_to_check.getX(),item_to_check.getY());
								
								if (area_enclosed_polygon.isPerimieterPoint(pt_to_check)) {
									items_on_perimeter.add(item_to_check);
								}
							}
						
							items_in_nested_enclosure.removeAll(items_on_perimeter);
						}
						
						else {
							// Text or single point (with no enclosure)
						
							// This item doesn't feature at this level
							// => will be subsequently capture by the group polygon below
							//    and processed by the recursive call
							
							item_list.remove(enclosure_item);
						}
					}
					
				}

			} // end of Text test
			else {
				// non-text item => add to remaining_item_list
				remaining_item_list.add(item);
			}

		}

		// Sort areas, smallest to largest
		Collections.sort(area_enclosed_polygon_list, new Comparator<AreaPolygon>() {

			public int compare(AreaPolygon ap1, AreaPolygon ap2) {
				Double ap1_area = ap1.getArea();
				Double ap2_area = ap2.getArea();

				return ap2_area.compareTo(ap1_area);
			}
		});

		// Remove any enclosed polygon areas that are completely contained in a larger one 

		for (int ri=0; ri<area_enclosed_polygon_list.size(); ri++) {
			// ri = remove index pos

			AreaPolygon rpoly = area_enclosed_polygon_list.get(ri);

			for (int ci=ri+1; ci<area_enclosed_polygon_list.size(); ci++) {
				// ci = check index pos
				AreaPolygon cpoly = area_enclosed_polygon_list.get(ci);
				if (rpoly.completelyContains(cpoly)) {
					area_enclosed_polygon_list.remove(ci);
					ri--; // to offset to the outside loop increment
					break;
				}
			}
		}
		

		// By this point, guaranteed that the remaining polygons are the largest ones
		// that capture groups of items
		//
		// There may be sub-groupings within them, which is the reason for the recursive call below
		

		for (AreaPolygon area_polygon: area_enclosed_polygon_list) {

			Collection<Item> enclosed_items = FrameUtils.getItemsEnclosedBy(frame, area_polygon);
			List<Item> enclosed_item_list = new ArrayList<Item>(enclosed_items); 
			
			int i=0;
			while (i<enclosed_item_list.size()) {
				Item enclosed_item = enclosed_item_list.get(i);
				
				// Filter out enclosed-items points that are part of the polygon's perimeter
				if (area_polygon.isPerimieterPoint(enclosed_item.getPosition())) {
					enclosed_item_list.remove(i);
				}
				else {
					i++;
				}
			}
			
			// Recursively work on the identified sub-group
			
			XGroupItem xgroup_item = new XGroupItem(frame, enclosed_item_list, area_polygon);

			grouped_item_list.add(xgroup_item);
		}
	}

	
	
	protected void castShadowIntoEmptySpace(YOverlappingItemsTopEdge y_item_span_top_edge, int yt, int yb)
	{
		// Assumes that all entries cast into are currently null
		// => Use the more general castShadow() below, if this is not guaranteed to be the case
		
		YOverlappingItemsShadow y_span_shadow = new YOverlappingItemsShadow(y_item_span_top_edge);
		
		for (int y=yt; y<=yb; y++) {
			setYOverlappingItemsSpan(y,y_span_shadow);
		}
	}
	
	protected void castShadow(YOverlappingItemsTopEdge y_item_span_top_edge, int yt, int yb)
	{
		// Cast shadows only in places where there are currently no shadow entries
		
		YOverlappingItemsShadow y_span_shadow = new YOverlappingItemsShadow(y_item_span_top_edge);
		
		int y=yt;
		
		while (y<=yb) {
			YOverlappingItemsSpan y_item_span = getYOverlappingItemsSpan(y);
			
			if (y_item_span==null) {
				setYOverlappingItemsSpan(y,y_span_shadow);
				y++;
			}
			else if (y_item_span instanceof YOverlappingItemsTopEdge) {
				// Our shadow has run into another top-edged zone 
				//   => need to extend the shadow to include this zone as well
				//      (making this encountered top-edge obsolete
				
				// Merge the newly encountered top-line in with the current one
				// and change all its shadow references to our (dominant) one
				
				XOrderedLine dominant_x_line = y_item_span_top_edge.getXOrderedLine();
				
				YOverlappingItemsTopEdge obsolete_top_edge = (YOverlappingItemsTopEdge)y_item_span;
				XOrderedLine obsolete_x_line = obsolete_top_edge.getXOrderedLine();
				
				for (XItem xitem: obsolete_x_line.getXItemList()) {
					
					dominant_x_line.orderedMergeItem(xitem);
				}
				
				while (y_item_span != null) {
					
					setYOverlappingItemsSpan(y,y_span_shadow);
					
					y++;
					
					if (y<=yb) {
						y_item_span = getYOverlappingItemsSpan(y);
					}
					else {
						y_item_span = null;
					}
				}
			}
			else {
				y++;
			}
		}
	}
	
	public void mapInItem(XItem xitem) 
	{
		
		int yt = xitem.getBoundingYTop();
		int yb = xitem.getBoundingYBot();
				
		// Merge into 'items_span' checking for overlaps (based on xitem's y-span) 
				
		boolean merged_item = false;
		for (int y=yt; y<=yb; y++) {

			YOverlappingItemsSpan item_span = getYOverlappingItemsSpan(y);

			if (item_span != null) {

				if (item_span instanceof YOverlappingItemsTopEdge) {

					// Hit a top edge of an existing item

					// Need to:
					//   1. *Required*      Insert new item into current x-ordered line 
					//   2. *Conditionally* Move entire x-ordered line up to higher 'y' top edge 
					//   3. *Required*      Cast shadow for new item 

					// Note for Step 2:
					// i)  No need to do the move if y == yt 
					//      (the case when the new top edge is exactly the same height as the existing one)
					// ii) If moving top-edge (the case when y > yt) then no need to recalculate the top edge for existing shadows, as
					//       this 'connection' is stored as a reference


					// Step 1: insert into existing top-edge

					YOverlappingItemsTopEdge y_item_span_top_edge = (YOverlappingItemsTopEdge)item_span;
					XOrderedLine xitem_span = y_item_span_top_edge.getXOrderedLine();

					xitem_span.orderedMergeItem(xitem.getBoundingXLeft(),xitem);

					// Step 2: if our new top-edge is higher than the existing one, then need to move existing top-edge
					if (y>yt) {

						// Move to new position
						setYOverlappingItemsSpan(yt, y_item_span_top_edge);

						// Old position needs to become a shadow reference
						YOverlappingItemsShadow y_span_shadow = new YOverlappingItemsShadow(y_item_span_top_edge);
						setYOverlappingItemsSpan(y,y_span_shadow);
					}


					// Step 3: Cast shadow
					castShadow(y_item_span_top_edge, yt+1, yb);

				}
				else {
					// Top edge to our new item has hit a shadow entry (straight off)
					// => Look up what the shadow references, and then add in to that 

					// Effectively after the shadow reference lookup this is the same
					// as the above, with the need to worry about Step 2 (as no move is needed)

					YOverlappingItemsShadow y_item_span_shadow = (YOverlappingItemsShadow)item_span;
					YOverlappingItemsTopEdge y_item_span_top_edge = y_item_span_shadow.getTopEdge();

					XOrderedLine xitem_span = y_item_span_top_edge.getXOrderedLine();

					// merge with this item list, preserving x ordering

					xitem_span.orderedMergeItem(xitem.getBoundingXLeft(),xitem);

					// Now Cast shadow
					castShadow(y_item_span_top_edge, yt+1, yb);
				}

				merged_item = true;
				break;

			}

		}

		if (!merged_item) {
			// xitem didn't intersect with any existing y-spans 
			// => simple case for add (i.e. all entries affected by map are currently null)

			// Start up a new x-ordered-line (consisting of only 'xitem'), add in top edge and cast shadow
			
			XOrderedLine xitem_line = new XOrderedLine(xitem);
			
			YOverlappingItemsTopEdge y_item_span_top_edge = new YOverlappingItemsTopEdge(xitem_line);
			setYOverlappingItemsSpan(yt,y_item_span_top_edge);

			castShadowIntoEmptySpace(y_item_span_top_edge, yt+1, yb);

		}


	}
	
	public void mapInXGroupItemsRecursive(List<XGroupItem> xgroup_item_list)
	{
		
		// Map in all the items in the given list:
		for (XGroupItem xgroup_item: xgroup_item_list) {
			if (!xgroup_item.isOriginalOutOfFlowItem()) {
				mapInItem(xgroup_item); // Map in this x-group-item
			}
		}	

		// Now recursively work through each item's nested x-groups 
		for (XGroupItem xgroup_item: xgroup_item_list) {

			if (!xgroup_item.isOriginalOutOfFlowItem()) {
				List<XGroupItem> nested_xgroup_item_list = xgroup_item.getGroupedItemList();

				if (nested_xgroup_item_list.size() >0) {
					xgroup_item.mapInXGroupItemsRecursive(nested_xgroup_item_list);
				}
			}
		}	
	}
	
	public ArrayList<Item> getYXOverlappingItemList(boolean separateGroups)
	{
		ArrayList<Item> overlapping_y_ordered_items = new ArrayList<Item>();
		
		for (int y=0; y<y_span_height; y++) {
			
			YOverlappingItemsSpan item_span = yitems_span_array[y];
			
			if (item_span != null) {

				if (item_span instanceof YOverlappingItemsTopEdge) {
					
					YOverlappingItemsTopEdge item_span_top_edge = (YOverlappingItemsTopEdge)item_span;
					XOrderedLine xitem_line = item_span_top_edge.getXOrderedLine();

					for (XItem xspan : xitem_line.getXItemList()) {
						if (xspan instanceof XRawItem) {
							XRawItem xitem_span = (XRawItem)xspan;
							Item item = xitem_span.getItem();
							
							overlapping_y_ordered_items.add(item);
						}
						else {
							// Must be an XGroupItem => recursive call on xspan item
							
							XGroupItem nested_group_item = (XGroupItem)xspan;
							ArrayList<Item> nested_overlapping_items = nested_group_item.getYXOverlappingItemList(separateGroups);
							if(separateGroups) {
								overlapping_y_ordered_items.add(new Text("{"));
							}
							overlapping_y_ordered_items.addAll(nested_overlapping_items);
							if(separateGroups) {
								overlapping_y_ordered_items.add(new Text("}"));
							}
						}
					}
				}
			}
		}
		
		return overlapping_y_ordered_items;
	}
	
	public ArrayList<Item> getYXOverlappingItemList() {
		return getYXOverlappingItemList(false);
	}
}

