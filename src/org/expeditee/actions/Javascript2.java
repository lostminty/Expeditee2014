package org.expeditee.actions;

import java.awt.Color;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.MessageBay;
import org.expeditee.io.flowlayout.XGroupItem;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Text;

/**
 * Javascript parser.
 * Works differently to the other Javascript class in that it
 * parses a frame as a whole rather than parsing individual text items as separate statements
 * 
 * @author jts21
 */
public class Javascript2 {
	
	public static final String ERROR_FRAMESET = "JavascriptErrors";
	
	public static final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
	public static final ScriptEngine scriptEngine = scriptEngineManager.getEngineByMimeType("application/javascript");
	static {
		scriptEngine.put("invocable", (Invocable) scriptEngine);
	}
	
	public static void printJSFrame(Item item) {
		if(item.getChild() == null) {
			// if the user clicked on the action without an item on their cursor
    		if(item.hasAction()) {
    			boolean isThis = false;
    			for(String s : item.getAction()) {
    				if(s.equalsIgnoreCase("runJSFrame")) {
    					isThis = true;
    					break;
    				}
    			}
    			if(isThis) {
    				System.out.println(new Javascript2(item.getParentOrCurrentFrame(), true));
    				return;
    			}
    		}
    		MessageBay.warningMessage("Requires either an item with a link to a frame, or no item (will run the current frame)");
		} else {
			System.out.println(new Javascript2(item.getChild(), true));
		}
	}
	
	public static void runJSFrame(Item item) throws Exception {
		if(item.getChild() == null) {
			// if the user clicked on the action without an item on their cursor
    		if(item.hasAction()) {
    			boolean isThis = false;
    			for(String s : item.getAction()) {
    				if(s.equalsIgnoreCase("runJSFrame")) {
    					isThis = true;
    					break;
    				}
    			}
    			if(isThis) {
    				Javascript2.runFrame(item.getParentOrCurrentFrame(), true);
    				return;
    			}
    		}
    		MessageBay.warningMessage("Requires either an item with a link to a frame, or no item (will run the current frame)");
		} else {
			Javascript2.runFrame(item.getChild(), true);
		}
	}
	
	public static Object eval(String code) {
		try {
	        return scriptEngine.eval(code);
        } catch (ScriptException e) {
	        e.printStackTrace();
        }
		return null;
	}
	
	private static synchronized void runFrame(Frame frame, boolean followLinks) throws Exception {
		Javascript2 js = new Javascript2(frame, followLinks);
		try {
    		try {
                scriptEngine.eval(js.toString());
            } catch (ScriptException e) {
        		js.handleError(e.getMessage(), e.getLineNumber());
            } catch (RuntimeException e) {
            	// there doesn't seem to be a way to safely get the lineNumber on which the error occurred
            	// so as a workaround we just parse the exception
            	if(e.getCause() == null) {
            		throw e;
            	}
            	String detail = e.getCause().getStackTrace()[1].toString();
            	int lastColon = detail.lastIndexOf(':');
            	int lastBracket = detail.lastIndexOf(')');
            	int lineNumber;
            	if(lastColon == -1 || lastBracket == -1) {
            		lineNumber = -1;
            	} else {
            		lineNumber = Integer.parseInt(detail.substring(lastColon + 1, lastBracket));
            	}
            	js.handleError(e.getMessage(), lineNumber);
            }
		} catch(Exception e) {
			js.handleError(null, -1);
			System.out.println(js.toString());
			e.printStackTrace();
		}
	}
	
	private static final class CodeLine {
		public Text item;
		public int line;
		public String source;
		
		public CodeLine(Text item, int line, String source) {
			this.item = item;
			this.line = line;
			this.source = source;
		}
		
		public String toString() {
			return line + ": " + source;
		}
	}
	
	private List<Frame> seen = new LinkedList<Frame>();
	private List<CodeLine> lines = new LinkedList<CodeLine>();
	private StringBuffer sb = new StringBuffer();
	private Javascript2(Frame frame, boolean followLinks) {
		this.parseFrame(frame, followLinks);
	}
	
	private void parseFrame(Frame frame, boolean followLinks) {
		if(frame == null) {
			return;
		}
		
		// make sure we don't get into an infinite loop
		// TODO: find a smarter way to do this that allows reusing frames but still stops infinite loops?
		seen.add(frame);
		
		// get all items on the frame
		List<Item> y_ordered_items = (List<Item>)frame.getItems();
		// remove the title item
		y_ordered_items.remove(frame.getTitleItem());
		
		XGroupItem toplevel_xgroup = new XGroupItem(frame,y_ordered_items);
		// ... following on from Steps 1 and 2 in the Constructor in XGroupItem ...
		
		// Step 3: Reposition any 'out-of-flow' XGroupItems
		toplevel_xgroup.repositionOutOfFlowGroups(toplevel_xgroup);
		
		// Step 4: Now add in the remaining (nested) XGroupItems
		List<XGroupItem> grouped_item_list = toplevel_xgroup.getGroupedItemList();
		toplevel_xgroup.mapInXGroupItemsRecursive(grouped_item_list);
	
		// Finally, retrieve linear list of all Items, (ordered, Y by X, allowing for overlap, nested-boxing, and arrow flow)
		List<Item> overlapping_y_ordered_items = toplevel_xgroup.getYXOverlappingItemList(true);	
		
		// Loop through the items looking for code and links to new frames
		for(Item i : overlapping_y_ordered_items) {
			if(followLinks && i.hasLink()) {
				Frame child = i.getChild();
				if(child != null && !seen.contains(child)) {
					this.parseFrame(child, true);
				}
			}
			if(i instanceof Text && !i.isAnnotation()) {
				String text = ((Text)i).getText();
				int lineNumber = 0;
				for(String line : text.trim().split("[\\n\\r]+")) {
					sb.append(line).append("\n");
					lines.add(new CodeLine((Text)i, lineNumber++, line));
				}
			}
		}
	}
	
	private void handleError(String message, int lineNumber) throws Exception {
		// negative line number bad
		if(lineNumber < 0) {
			MessageBay.errorMessage("Failed to determine the line on which the error occurred");
			return;
		}
		// if for some reason the error is after the end of the code, assume it should be the last line
		if(lineNumber > this.lines.size()) {
			lineNumber = this.lines.size();
		}
		CodeLine cl = this.lines.get(lineNumber - 1);
		Frame errorSourceFrame = cl.item.getParent();
		if(errorSourceFrame == null) {
			MessageBay.errorMessage("Failed to find frame on which the error occurred");
			return;
		}
		Frame errorFrame;
		String title = "Error parsing \"" + errorSourceFrame.getTitle() + "\" (" + errorSourceFrame.getName() + ")";
		if(FrameIO.canAccessFrameset(ERROR_FRAMESET)) {
			errorFrame = FrameIO.CreateFrame(ERROR_FRAMESET, title, null);
		} else {
			errorFrame = FrameIO.CreateFrameset(ERROR_FRAMESET, FrameIO.FRAME_PATH);
			errorFrame.setTitle(title);
		}
		Collection<Item> toAdd = errorSourceFrame.getAllItems();
		toAdd.remove(errorSourceFrame.getTitleItem());
		toAdd.remove(cl.item);
		errorFrame.addAllItems(ItemUtils.CopyItems(toAdd));
		String errorItemText = cl.item.getText().trim();
		String[] errorItemLines = errorItemText.split("[\\n\\r]+");
		int errorLinePos = 0;
		int x = cl.item.getX();
		int y = cl.item.getY();
		if(cl.line != 0) {
    		for(int i = 0; i < cl.line; i++) {
    			errorLinePos += errorItemLines[i].length();
    		}
    		Text beforeErrorItem = errorFrame.addText(x, y,
    				errorItemText.substring(0, errorLinePos), null);
    		y = beforeErrorItem.getY() + beforeErrorItem.getBoundsHeight();
		}
		Text errorItem;
		errorItem = errorFrame.addText(x, y, errorItemLines[cl.line], null);
		errorItem.setBackgroundColor(Color.RED);
		for(String line : message.split("[\\n\\r]+")) {
			errorItem.setTooltip("text: " + line);
		}
		errorItem.setTooltip("font: " + Text.MONOSPACED_FONT);
		errorItem.setTooltip("width: " + 80 * 12);
		errorLinePos += errorItemLines[cl.line].length();
		if(++errorLinePos < errorItemText.length()) {
			errorFrame.addText(cl.item.getX(), errorItem.getY() + errorItem.getBoundsHeight(),
					errorItemText.substring(errorLinePos + 1), null);
		}
		errorFrame.change();
		FrameIO.SaveFrame(errorFrame);
		MessageBay.displayMessage("Script failed at line " + lineNumber +  " - `" + cl.source + "`",
				errorFrame.getName(), MessageBay.ERROR_COLOR, true, null);
		FrameUtils.DisplayFrame(errorFrame, true, true);
	}
	
	public String toString() {
		return this.sb.toString();
	}

}
