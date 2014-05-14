package org.expeditee.items;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.LinkedList;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;

import org.expeditee.actions.Javascript2;
import org.expeditee.gui.DisplayIO;
import org.expeditee.items.widgets.InteractiveWidget;

public class JSItem extends XRayable implements JSThreadable {
	
	private static final Object global = Javascript2.eval("new Object()");
	
	// a method to run that will set up and return the root JComponent for this Widget
	private final String init;
	// a method to run to populate a List<String> with our state
	private final String save;
	// a method to run that will load state from a String[]
	private final String load;
	// a method to run that will paint our item using a Graphics2D object
	private final String paint;
	
	// we have our own script context since it needs to have some global variables which are specific to each widget
	private final ScriptEngine scriptEngine;
	private final Invocable invocable;
	
	private int _width, _height;
	
	private static Text getSauce(int width, int height) {
		Text source = new Text(DisplayIO.getCurrentFrame().getNextItemID(), "@js: " + width + " " + height);
		source.setParent(DisplayIO.getCurrentFrame());
		return source;
	}

	private JSItem(Text source, String init, String save, String load, String paint) throws Exception {
		super(source);
		this.init = init;
		this.save = save;
		this.load = load;
		this.paint = paint;
		this.scriptEngine = Javascript2.scriptEngineManager.getEngineByMimeType("application/javascript");
		this.invocable = (Invocable) this.scriptEngine;
		this.scriptEngine.put("global", global);
		this.scriptEngine.put("invocable", this.invocable);
		this.scriptEngine.put("item", this);
		this.scriptEngine.put("source", this._source);
		this.parseSource();
    	System.out.println(this.init);
    	this.scriptEngine.eval("var init = " + this.init + "\ninit()");
    	this.scriptEngine.eval("save = " + this.save);
    	this.scriptEngine.eval("paint = " + this.paint);
    	this.updateSource();
	}
	
	public JSItem(Text source) throws Exception {
		this(source, source.getData().get(0).replaceAll("\\\\n", "\n"),
				source.getData().get(1).replaceAll("\\\\n", "\n"),
				source.getData().get(2).replaceAll("\\\\n", "\n"),
				source.getData().get(3).replaceAll("\\\\n", "\n"));
	}
	
	public JSItem(int width, int height, String init, String save, String load, String paint) throws Exception {
		this(getSauce(width, height), init, save, load, paint);
	}
	
	public JSItem(String init, String save, String load, String paint) throws Exception {
		this(100, 100, init, save, load, paint);
	}

	@Override
	public Item copy() {
		try {
	        return new JSItem(_source.copy());
        } catch (Exception e) {
	        e.printStackTrace();
        }
		return null;
	}

	@Override
	public void paint(Graphics2D g) {
		try {
	        this.invocable.invokeFunction("paint", (Object)g);
        } catch (Exception e) {
	        e.printStackTrace();
        }
	}

	@Override
	public void setAnnotation(boolean val) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updatePolygon() {
		_poly = new Polygon();
		_poly.addPoint(getX(), getY());
		_poly.addPoint(getX() + _width, getY());
		_poly.addPoint(getX() + _width, getY() + _height);
		_poly.addPoint(getX(), getY() + _height);
	}
	
	private void parseSource() {
		String text = _source.getText();
		text = text.replaceFirst("@js:", "");
		text = text.replaceAll("\n", "");
		text = text.trim();
		int index = text.indexOf(':');
		String[] values;
		if(index != -1) {
			values = text.substring(0, index).split("\\s+");
		} else {
			values = text.split("\\s+");
		}
		if(values.length >= 2) {
			_width = Integer.parseInt(values[0]);
			_height = Integer.parseInt(values[1]);
		}
		
		if(index == -1) {
			return;
		}
		String[] args = InteractiveWidget.parseArgs(text.substring(index));
		try {
			this.scriptEngine.eval("load = " + this.load);
	        this.invocable.invokeFunction("load", (Object) args);
        } catch (Exception e) {
	        e.printStackTrace();
        }
	}
	
	private String[] saveArgs() {
		try {
			List<String> args = new LinkedList<String>();
			this.invocable.invokeFunction("save", (Object)args);
			return args.toArray(new String[0]);
        } catch (Exception e) {
        	e.printStackTrace();
        }
		return null;
	}
	
	private void updateSource() {
		List<String> data = new LinkedList<String>();
		data.add(this.init.replaceAll("[\n\r]", "\\\\n"));
		data.add(this.save.replaceAll("[\n\r]", "\\\\n"));
		data.add(this.load.replaceAll("[\n\r]", "\\\\n"));
		data.add(this.paint.replaceAll("[\n\r]", "\\\\n"));
		_source.setData(data);
		
		StringBuffer newText = new StringBuffer("@js: ");
		newText.append(_width).append(" ").append(_height);
		
		String stateArgs = InteractiveWidget.formatArgs(saveArgs());
		if (stateArgs != null) {
			newText.append(':');
			newText.append(stateArgs);
		}
		
		_source.setText(newText.toString());
	}
	
	@Override
	public Integer getWidth() {
		return this._width;
	}
	
	@Override
	public int getHeight() {
		return this._height;
	}
	
	private List<JSThread> threads = new LinkedList<JSThread>();
	
	public JSThread addThread(String code) {
		JSThread t = new JSThread(scriptEngine, code);
		this.threads.add(t);
		return t;
	}
	
	@Override
	public void onParentStateChanged(ItemParentStateChangedEvent e) {
		switch (e.getEventType()) {
    		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
    		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY:
    		case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
    			for(JSThread t : this.threads) {
    				t.kill();
    			}
    			break;
    
    		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
    		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
    		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
    		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
    			for(JSThread t : this.threads) {
   					t.resume();
    			}
    			break;

		}
	}

}
