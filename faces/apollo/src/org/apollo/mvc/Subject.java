package org.apollo.mvc;

/**
 * A Subject is any object that wants to notify other objects of
 * changes in its state. 
 *@author Brook Novak
 */
public interface Subject {

	/**
	 * Adds an observer to the set of Observers for this subject.
	 * @param ob the Observer to be added.
	 */
	void addObserver(Observer ob);

	/**
	 * Removes an Observer from the set of Observers for this subject.
	 * @param ob the Observer to be removed.
	 */
	void removeObserver(Observer ob);

}
