package org.apollo.mvc;

/**
 * An Observer as any object that would like to be notified
 * when another object changes. To do this the object that will change
 * must implement the Subject interface and then the Observer object
 * should be registered with the Subject by calling addObserver()
 * 
 * @author Brook NOvak
 */
public interface Observer {
	
	/**
	 * The method called when a Subject that this observer has
	 * registered with changes and the Subject has specified a SubjectChangedEvent.
	 * 
	 * @param event A type of SubjectChangedEvent
	 * @param source The subject which the event was fired from
	 */
	void modelChanged(Subject source, SubjectChangedEvent event);
	
	/**
	 * Informs this observer what it is observing.
	 * The subject will call this automatically.
	 * The observer must know what it's observing so that
	 * it can update based on the subject's data.
	 * 
	 * @param parent the subject we are observing
	 */
	void setObservedSubject(Subject parent);

	/**
	 * Provides functionality to retrieve the subject that this class
	 * will be observing.
	 * 
	 * @return the subject we are observing
	 */
	Subject getObservedSubject();
	
}