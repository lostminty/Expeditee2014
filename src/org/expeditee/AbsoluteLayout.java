package org.expeditee;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

public class AbsoluteLayout implements LayoutManager {

	public void addLayoutComponent(String name, Component comp) {}
	public void removeLayoutComponent(Component comp) {}
	
	
	
	public Dimension minimumLayoutSize(Container parent) {
		return parent.getMinimumSize();
	}

	public Dimension preferredLayoutSize(Container parent) {
		return parent.getPreferredSize();
	}

	public void layoutContainer(Container parent) {}

}
