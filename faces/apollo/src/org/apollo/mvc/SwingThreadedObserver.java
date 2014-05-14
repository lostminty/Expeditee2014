package org.apollo.mvc;

public interface SwingThreadedObserver {
	
	/**
	 * Whenever the observed SwingEventQueue dequeu's an event on the swing thread.
	 * 
	 * @param event A type of SubjectChangedEvent
	 * @param source The subject which the event was fired from
	 */
	void modelChangedOnSwingThread(Subject source, SubjectChangedEvent event);
}
