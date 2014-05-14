package org.expeditee.taskmanagement;

import java.util.EventListener;


/**
 * 
 * An EventListener for save events.
 * 
 * @author Brook Novak
 * 
 */
public interface SaveStateChangedEventListener extends EventListener {

	/**
	 * Raised whenever a saveable entity has started or is about to start saving
	 * 
	 * @param event The event information.
	 */
	public void saveStarted(SaveStateChangedEvent event);

	/**
	 * Raised whenever a saveable entity has finished saving.
	 * 
	 * @param event The event information.
	 */
	public void saveCompleted(SaveStateChangedEvent event);
	
}
