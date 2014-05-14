package org.apollo.mvc;

import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

/**
 * A Skeletal implementation of a Subject in the model-view-controller package.
 * 
 * @author Brook Novak
 */
public abstract class AbstractSubject implements Subject {

	/** Collection of registered observers. */
	protected Set<Observer> observers = new HashSet <Observer>();

	/**
	 * Adds an observer to the set of registered observers.
	 * @param ob The observer to add.
	 */
	public final void addObserver(final Observer ob) {
        synchronized (observers) {
        	observers.add(ob);
        }
		ob.setObservedSubject(this);
	}

	/**
	 * Removes an observer from the set of registered observers.
	 * 
	 * @param ob The observer to remove
	 */
	public final void removeObserver(final Observer ob) {
        synchronized (observers) {
        	observers.remove(ob);
        }
		ob.setObservedSubject(null);
	}

	/**
	 * Notifies all observers that this object has changed with the given
	 * ModelChangeEvent.
	 * 
	 * @param event
	 *            A ModelChangeEvent which specifies how this object has
	 *            changed.
	 */
	protected final void fireSubjectChanged(final SubjectChangedEvent event) {
        
        synchronized (observers) {
    		for (Observer ob : observers) {
    			ob.modelChanged(this, event);
    		}
        }
	}
	
	/**
	 * Notifies all observers that this object has changed with the given
	 * ModelChangeEvent. On the swing thread.
	 * 
	 * @param event
	 *            A ModelChangeEvent which specifies how this object has
	 *            changed.
	 */
	protected final void fireSubjectChangedLaterOnSwingThread(final SubjectChangedEvent event) {
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				fireSubjectChanged(event);
			}
		});
		
	}
	
}
