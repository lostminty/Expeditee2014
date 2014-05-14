package org.expeditee.items;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.script.Invocable;
import javax.swing.JPanel;

public class JSPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private final Invocable invocable;
	private final String functionName;
	private boolean autoPaint;
	
	public JSPanel(Invocable invocable, String functionName, boolean autoPaint) {
		super();
		this.invocable = invocable;
		this.functionName = functionName;
		this.autoPaint = autoPaint;
		this.setIgnoreRepaint(true);
	}
	
	public JSPanel(Invocable invocable, String functionName) {
		this(invocable, functionName, true);
	}
	
	private boolean paintNext = false;
	@Override
	public void paintComponent(Graphics g) {
		if(!autoPaint && !paintNext)
			return;
		paintNext = false;
		try {
			invocable.invokeFunction(functionName, (Graphics2D) g);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Calls paintComponent()
	 * The only way to cause a repaint if autoPaint is false
	 */
	public void doPaint() {
		paintNext = true;
		this.repaint();
	}
	
	public void setAutoPaint(boolean autoPaint) {
		this.autoPaint = autoPaint;
	}
	
	public boolean getAutoPaint() {
		return autoPaint;
	}
	
	/**
	 * Make some padding around the drawable area so we don't fight with the item outline
	 */
	@Override
	public int getX() {
		return super.getX() + 5;
	}
	
	@Override
	public int getY() {
		return super.getY() + 5;
	}
	
	@Override
	public int getWidth() {
		return super.getWidth() - 10;
	}
	
	@Override
	public int getHeight() {
		return super.getHeight() - 10;
	}
}
