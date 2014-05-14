package org.apollo.mvc;

import java.util.LinkedList;
import java.util.Queue;

import javax.swing.SwingUtilities;

/**
 * If your observer needs to reacts to events on the swing thread (i.e. interacting with swing
 * objects somewhere in the event) and the observed subject raised events on other threads,
 * this class thunnels all events into the swing thread and ensures that they are raised in
 * the swing thread (thread safety!).
 * 
 * @author Brook Novak
 *
 */
public class SwingEventQueue {
	
	private SwingThreadedObserver target;

	// Using a FIFO queue implementation
	private Queue<QueudEvent> eventQueue = new LinkedList<QueudEvent>();
	
	private EventProccessor eventProccessor = new EventProccessor();
	
	/**
	 * COnstructor.
	 * @param target The SwingThreadedObserver instance to thunnel events to on the swing thread.
	 */
	public SwingEventQueue(SwingThreadedObserver target) {
		if (target == null) throw new NullPointerException("target");
		this.target = target;
	}

	/**
	 * Adds an event to the wing queue. The event will be re-fired on the swing thread
	 * and raised on the target.
	 * 
	 * @param event A type of SubjectChangedEvent
	 * @param source The subject which the event was fired from
	 */
	public synchronized void queueEvent(Subject source, SubjectChangedEvent event) {
		synchronized (eventQueue) {
			eventQueue.add(new QueudEvent(source, event));
		}
		SwingUtilities.invokeLater(eventProccessor);
	}

	class EventProccessor implements Runnable {

		public void run() { // invoked on swing thread

			synchronized (eventQueue) {
				while (!eventQueue.isEmpty()) {
					QueudEvent qe = eventQueue.remove();
					target.modelChangedOnSwingThread(qe.source, qe.event);
				}
			}
		}

	}
	
	class QueudEvent {
		public QueudEvent(Subject source, SubjectChangedEvent event) {
			this.event = event;
			this.source = source;
		}
		SubjectChangedEvent event;
		Subject source;
	}

}
