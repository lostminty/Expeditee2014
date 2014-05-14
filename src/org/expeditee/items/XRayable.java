package org.expeditee.items;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.gui.FrameGraphics;

public abstract class XRayable extends Item {
	
	
	protected Text _source = null;
	
	public XRayable(Text source){
		super();
		_source = source;
		source.setHidden(true);
		source.addEnclosure(this);
	}
	
	public Collection<Item> getItemsToSave() {
		Collection<Item> toSave = new LinkedList<Item>();
		toSave.add(_source);
		return toSave;
	}
	
	@Override
	public Collection<Item> getConnected() {
		Collection<Item> conn = super.getConnected();
		conn.add(_source);
		return conn;
	}
	
	@Override
	public void addAllConnected(Collection<Item> connected) {
		super.addAllConnected(connected);
		if (!connected.contains(_source)) {
			connected.add(_source);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.expeditee.items.Item#merge(org.expeditee.items.Item, int, int)
	 */
	@Override
	public Item merge(Item merger, int mouseX, int mouseY) {
		return merger;
	}
	
	/**
	 * Returns the Text Item that was used to create this Picture object. This
	 * is required when saving the Frame.
	 * 
	 * @return The Text Item used to create this Picture.
	 */
	public Text getSource() {
		return _source;
	}

	@Override
	public int getID() {
		return _source.getID();
	}

	@Override
	public void setID(int newID) {
		_source.setID(newID);
	}
	
	@Override
	public int getY() {
		return _source.getY();
	}

	@Override
	public int getX() {
		return _source.getX();
	}
	
	@Override
	public Color getColor() {
		return _source.getColor();
	}

	@Override
	public void setColor(Color c) {
		_source.setColor(c);
		invalidateCommonTrait(ItemAppearence.ForegroundColorChanged);
	}
	
	@Override
	public void setBackgroundColor(Color c) {
		_source.setBackgroundColor(c);
	}
	
	@Override
	public void setFillColor(Color c) {
		_source.setFillColor(c);
	}
	
	@Override
	public void setBorderColor(Color c) {
		_source.setBorderColor(c);
		invalidateCommonTrait(ItemAppearence.BorderColorChanged);
	}
	
	@Override
	public void setGradientColor(Color c) {
		_source.setGradientColor(c);
	}
	
	@Override
	public void setTooltips(List<String> tooltips) {
		_source.setTooltips(tooltips);
	}
	
	@Override
	public void setTooltip(String tooltip) {
		_source.setTooltip(tooltip);
	}
	
	@Override
	public List<String> getTooltip() {
		return _source.getTooltip();
	}
	
	@Override
	public float getThickness() {
		return _source.getThickness();
	}
	
	@Override
	public Float getAnchorLeft() {
	        return _source.getAnchorLeft();
	}
	
	@Override
	public Float getAnchorRight() {
		return _source.getAnchorRight();
	}

	@Override
	public Float getAnchorTop() {
		return _source.getAnchorTop();
	}

	@Override
	public Float getAnchorBottom() {
		return _source.getAnchorBottom();
	}
	
	
	@Override
	public Color getBorderColor() {
		return _source.getBorderColor();
	}
	
	@Override
	public void setThickness(float thick, boolean setConnected) {
		this.invalidateCommonTrait(ItemAppearence.Thickness);
		_source.setThickness(thick, setConnected);
		this.invalidateCommonTrait(ItemAppearence.Thickness);
	}
	
	@Override
	public Color getBackgroundColor() {
		return _source.getBackgroundColor();
	}
	
	@Override
	public Color getFillColor() {
		return _source.getFillColor();
	}
	
	@Override
	public Color getGradientColor() {
		return _source.getGradientColor();
	}

	@Override
	public String getFillPattern() {
		return _source.getFillPattern();
	}
	
	@Override
	public boolean hasLink() {
		return getLink() != null;
	}
	
	@Override
	public String getLink() {
		if (_source == null)
			return null;
		else
			return _source.getLink();
	}

	@Override
	public void setLink(String frameName) {
		if (_source == null) {
			return;
		}
		if(frameName == null)
			invalidateAll();
		_source.setLink(frameName);
		updatePolygon();
		//TODO: only invalidate the link bit
		invalidateAll();
	}

	@Override
	public void setActions(List<String> action) {
		if (_source == null){
			return;
		}
		if(action == null || action.size() == 0)
			invalidateAll();
		_source.setActions(action);
		updatePolygon();
		invalidateAll();
	}

	@Override
	public List<String> getAction() {
		if (_source == null)
			return null;
		else
			return _source.getAction();
	}
	
	@Override
	public void translate(Point2D origin, double ratio){
		//_source.translate(origin, ratio);
		updatePolygon();
	}
	
	@Override
	public void setPosition(float x, float y) {
		//_source.setPosition(x, y);
		
	}
	
	@Override
	public boolean isAnchored() {
		return _source.isAnchored();
	}

	@Override
	public boolean isAnchoredX() {
		return _source.isAnchoredX();
	}

	@Override
	public boolean isAnchoredY() {
		return _source.isAnchoredY();
	}

	@Override
	public void setAnchorTop(Float anchor) {
		_source.setAnchorTop(anchor);
		if (anchor != null)
			_source.setY(anchor);
	}

	@Override
	public void setAnchorBottom(Float anchor) {
		_source.setAnchorBottom(anchor);
		if (anchor != null)
			_source.setY(FrameGraphics.getMaxFrameSize().height - getHeight() - anchor);
	}

	
	@Override
	public void setAnchorLeft(Float anchor) {
		_source.setAnchorLeft(anchor);
	}

	@Override
	public void setAnchorRight(Float anchor) {
		_source.setAnchorRight(anchor);
		_source.setX(FrameGraphics.getMaxFrameSize().width - anchor - this.getWidth());
	}
	
	public boolean refresh(){
		return true;
	}
	
	@Override
	public void setXY(float x, float y){
		_source.setXY(x, y);
	}
	
	@Override
	public boolean hasPermission(UserAppliedPermission p) {
		return _source.hasPermission(p);
	}
	
	@Override
	public void setPermission(PermissionPair permissionPair) {
		_source.setPermission(permissionPair);
	}
}
