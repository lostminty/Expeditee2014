package org.expeditee.gui;

/**
 * An {@link DisplayIOObserver} can observe the {@link DisplayIO} for detecting when
 * frame changes occur.
 * 
 * @author Brook Novak
 *
 */
public interface DisplayIOObserver {
	
	/**
	 * Raised whenever a frame change occurs.
	 */
	public void frameChanged();
}
