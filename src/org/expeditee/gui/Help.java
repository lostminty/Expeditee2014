package org.expeditee.gui;

import java.util.Collection;

import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;
import org.expeditee.settings.experimental.ExperimentalFeatures;


public class Help {
	
	// contexts
	private static enum Context {
		background,
		item,
		polygon,
		line,
		dot,
		text,
		image,
		widget
	};
	
	// mouse buttons
	private static final int left = 1, middle = 2, right = 4;
	private static final String[] buttons = { "", "L", "M", "LM", "R", "LR", "MR" };
	
	// modifiers
	private static final int control = 1, shift = 2;
	private static final String[] modifiers = { "", "^", "+", "^+" };
	
	// other
	private static final int panning = 1, action = 2, link = 4, cursor = 8;
	
	// separator between command and description
	private static final String separatorString = ": ";
	
	private static final String commandPadding = "      ";
	private static final String padding = "                    ";
	private static final String emptyPadding = commandPadding + padding;
	
	private static final String command(int mod, int button) {
		String cmd = modifiers[mod] + buttons[button] + separatorString;
		return commandPadding.substring(cmd.length()) + cmd;
	}
	
	private static final String left(Context context, int mod, int other) {
		switch(context) {
		case widget: return "Left click widget";
		case background:
    		if((mod & (control | shift)) != 0) return "Go forward";
    		return "Go back";
    	
		default:
			if((other & action) != 0 && (mod & control) == 0) return "Run action";
			if((other & link) != 0) return "Follow link";
			return "Create link";
		}
	}
	
	private static final String middle(Context context, int mod, int other) {
		switch(context) {
		case widget: return "Middle click widget";
		case background:
    		if((other & cursor) != 0) return FreeItems.hasMultipleVisibleItems() ? "Put down items" : "Put down item";
    		return "Draw line/arrow";
		case line:
			if(FreeItems.isDrawingPolyLine()) return "Spot-weld polyline";
			if((mod & shift) != 0) return "Extend shape";
		case dot:
			if((mod & shift) != 0) return "Pickup unconstrained";
		case text:
			if((other & cursor) != 0 && FreeItems.getInstance().size() == 1 && FreeItems.textOnlyAttachedToCursor()) return "Merge text";
		default:
			if((other & cursor) != 0) return FreeItems.hasMultipleVisibleItems() ? "Put down items" : "Put down item";
			if((mod & shift) != 0) return "Draw line/arrow";
			if(context == Context.polygon) return "Pickup enclo'd items";
			return "Pickup item";
		}
	}
	
	private static final String right(Context context, int mod, int other) {
		switch(context) {
		case widget: return "Right click widget";
		case background:
			if((other & cursor) != 0) {
				if(FreeItems.isDrawingPolyLine()) return "Continue polyline";
				return "Stamp copy";
			}
			return "Draw rectangle";
		case line:
			if(FreeItems.isDrawingPolyLine()) return "Spot-weld & continue";
			if((mod & shift) != 0) return "Add line";
			break;
		case dot:
			if((mod & shift) == 0)return "Extend line";
			break;
		}
		if((other & cursor) != 0) return "Stamp copy";
		if((mod & shift) != 0) return "Draw rectangle";
		if(context == Context.polygon) return "Copy enclosed items";
		return "Copy item";
	}
	
	private static final String left_right(Context context, int mod, int other) {
		switch(context) {
		case widget: return null;
		default:
			if((other & cursor) == 0) return "Extract attributes";
			return null;
		case polygon:
		case background:
			if((mod & control) != 0) return "Horizontal format";
			return "Vertical format";
		}
	}
	
    private static final String middle_right(Context context, int mod, int other) {
		switch(context) {
		case widget: return null;
		case text:
			if((other & cursor) != 0 && FreeItems.getInstance().size() == 1 && FreeItems.textOnlyAttachedToCursor()) return "Swap text";
			break;
		case background:
			if((other & cursor) == 0) {
				if((mod & control) != 0) return "Redo";
				return "Undo";
			}
			break;
		}
		return "Delete";
	}
	
	private static final String drag_left(Context context, int mod, int other) {
		switch(context) {
		case widget: return null;
		case background:
			if((mod & shift) != 0 && (other & cursor) == 0 && (other & panning) != 0) return "Drag to pan";
			break;
		case text:
			if((other & cursor) == 0) return "Select text region";
			break;
		}
		return null;
	}
	
	private static final String drag_middle(Context context, int mod, int other) {
		switch(context) {
		case widget: return null;
		case text:
			if((other & cursor) == 0) return "Cut text region";
		}
		return null;
	}
	
	private static final String drag_right(Context context, int mod, int other) {
		switch(context) {
		case widget: return null;
		case image:
			return "Crop image";
		case line:
			return "Extrude shape";
		case text:
			if((other & cursor) == 0) return "Copy text region";
		}
		return null;
	}
	
	
	public static void updateStatus() {
		Item current = FrameUtils.getCurrentItem();
		Collection<Item> currents;
		Context context;
		InteractiveWidget iw = null;
		if(current != null) {
			if(current instanceof Line) {
				context = Context.line;
			} else if(current instanceof Text) {
				context = Context.text;
			} else if(current instanceof Picture) {
				context = Context.image;
			} else if(current instanceof Dot ) {
				context = Context.dot;
			}else {
				context = Context.item;
			}
		} else {
			currents = FrameUtils.getCurrentItems();
			if(currents != null) {
				if(currents.size() >= 4) {
    				for(Item i : currents) {
    					if (i instanceof WidgetCorner) {
        					iw = ((WidgetCorner) i).getWidgetSource();
        					break;
        				} else if (i instanceof WidgetEdge) {
        					iw = ((WidgetEdge) i).getWidgetSource();
        					break;
        				}
    				}
				}
				if(iw != null) {
					context = Context.widget;
				} else {
					context = Context.polygon;
				}
			} else {
				context = Context.background;
			}
		}
		int mod = (FrameMouseActions.isControlDown() ? control : 0) | (FrameMouseActions.isShiftDown() ? shift : 0);
		int other = (ExperimentalFeatures.MousePan.get() ? panning : 0) |
				(current != null && current.hasAction() ? action : 0)|
				(current != null && current.hasLink() ? link : 0) |
				(FreeItems.itemsAttachedToCursor() ? cursor : 0);
		String status = "";
		
		
		String l =  left(context, mod, other);
		String m =  middle(context, mod, other);
		String r =  right(context, mod, other);
		String lr = left_right(context, mod, other);
		String mr = middle_right(context, mod, other);
		String dl = drag_left(context, mod, other);
		String dm = drag_middle(context, mod, other);
		String dr = drag_right(context, mod, other);
		
		if(l != null || m != null || r != null || lr != null || mr != null) status += " Click:";
		status += (l != null ? command(mod, left) + l + padding.substring(l.length()) : emptyPadding);
		status += (m != null ? command(mod, middle) + m + padding.substring(m.length()) : emptyPadding);
		status += (r != null ? command(mod, right) + r + padding.substring(r.length()) : emptyPadding);
		status += (lr != null ? command(mod, left | right) + lr + padding.substring(lr.length()) : emptyPadding);
		status += (mr != null ? command(mod, middle | right) + mr + padding.substring(mr.length()) : emptyPadding);
		if(iw != null) {
			status += "\n Widget:  " + iw.getClass().getSimpleName();
		} else {
			if(dl != null || dm != null || dr != null) status += "\n Drag: ";
    		status += (dl != null ? command(mod, left) + dl + padding.substring(dl.length()) : emptyPadding);
    		status += (dm != null ? command(mod, middle) + dm + padding.substring(dm.length()) : emptyPadding);
    		status += (dr != null ? command(mod, right) + dr + padding.substring(dr.length()) : emptyPadding);
		}
		
		
		MessageBay.setStatus(status);
	}

}
