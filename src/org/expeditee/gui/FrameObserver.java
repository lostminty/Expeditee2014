package org.expeditee.gui;

public interface FrameObserver {
	public abstract void update();

	public abstract void removeSubject(Frame frame);
	
	public abstract boolean isVisible();
}
