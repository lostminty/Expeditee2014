package org.expeditee.taskmanagement;

import java.util.HashMap;
import java.util.PriorityQueue;



/**
 * Manages asynchronous loading. It is essentially a daemon that performs all expensive proccessing
 * for a frame.
 * 
 * The Objective / Purpose: 
 * To load data on Expeditee frames which can take long periods of time in a way such that
 * navigating through frames with lots of data is smooth / fast. 
 * 
 * This helps reduce vital usability concerns with frame load times - a conern of high importance
 * in KMS publications...
 * 
 * @author Brook Novak
 *
 */
public final class EntityLoadManager {
	
	/**
	 * Singleton design pattern
	 */
	private static EntityLoadManager _instance = new EntityLoadManager();
	public static EntityLoadManager getInstance() {
		return _instance;
	}
	
	/**
	 * Start daemon right away
	 */
	private EntityLoaderDaemon loadDaemon;
	private EntityLoadManager() {
		loadDaemon = new EntityLoaderDaemon();
		loadDaemon.start();
	}
	
	private PriorityQueue<QueuedEntity> loadQueue = new PriorityQueue<QueuedEntity>();
	
	private HashMap<LoadableEntity, QueuedEntity> queueMap = new HashMap<LoadableEntity, QueuedEntity>();
	
	private QueuedEntity busyEntity = null;
	
	private Object entityLocker = new Object(); // union of queueMap, queueMap and busyEntity resources

	/**
	 * Queue's an entity for loading. 
	 * This operation is threadsafe.
	 * 
	 * @param entity 
	 * 		The entity to load.
	 * 
	 * @param minDelay 
	 * 		The minimum time to wait before loading in milliseconds.
	 * 
	 * @return 
	 * 		True if the entity was added. False if the entity is already queued prior to this call.
	 * 		Note that if entity is currently loading, it will still be added to the queue, this
	 * 		allows for requeueing a busy entity.
	 * 			
	 */
	public boolean queue(LoadableEntity entity, int minDelay) {
		synchronized(entityLocker) {
			
			if (queueMap.containsKey(entity))
				return false;
			
			QueuedEntity qe = new QueuedEntity(entity, (int)minDelay, 0);
			loadQueue.add(qe);
			queueMap.put(entity, qe);
		}
		
		// Ensure daemon knows of new item
		loadDaemon.reschedule();
		
		return true;
	}
	
	/**
	 * Increasing a priority on an entity will invalidate the wait-time
	 * until it is loaded and push it up the load-queue.
	 * 
	 * Proceeding calls to this on the same LoadableEntity will cumatively
	 * increase its priority so that it will continually be pushed up in the
	 * priority queue.
	 * 
	 * This operation is threadsafe.
	 * 
	 * @param entity The entity to increase its priority
	 */
	public void increasePriority(LoadableEntity entity) {
		
		synchronized(entityLocker) {
			
			QueuedEntity qe = queueMap.remove(entity);
			loadQueue.remove(qe);
			
			if (qe != null) {
				qe = qe.higherPriority();
				loadQueue.add(qe);
				queueMap.put(entity, qe);
			}
			
		}
		
		// Ensure daemon knows of new item
		loadDaemon.reschedule();
		
	}
	
	/**
	 * Cancels all loadable entities on the queue.
	 * If an entity is currently being loaded that
	 *
	 * Note that it is up to the entities descretion to stop loading.
	 * As far as the LoadTimeManager is concerned all entities are cancelled
	 * and the queue is emptied.
	 * 
	 * This operation is threadsafe.
	 */
	public void cancelAll() {
		
		synchronized(entityLocker) {
			
			// Notify all entities that they are cancelled
			for (QueuedEntity qe : loadQueue) {
				qe.entity.cancelLoadRequested();
			}
			
			// Clear queue / map
			loadQueue.clear();
			queueMap.clear();
			
			// Cancel current entity if applicable
			if (busyEntity != null) {
				busyEntity.entity.cancelLoadRequested();
				busyEntity = null;
			}
		}
	}
	
	/**
	 * Cancels a specific loadable entity from loading. Removes it from the queue
	 * 
	 * @param entity
	 */
	public void cancel(LoadableEntity entity) {
		
		synchronized(entityLocker) {
			QueuedEntity qe = queueMap.remove(entity);
			if (qe != null) {
				loadQueue.remove(qe);
			} else if (busyEntity != null && busyEntity.entity == entity) {
				busyEntity.entity.cancelLoadRequested();
				busyEntity = null;
			}
		}
		
	}
	
	private class EntityLoaderDaemon extends Thread {
		
		private Object idleLocker = new Object(); // Daemon sleeps on this
		
		public EntityLoaderDaemon() {
			super("EntityLoaderDaemon");
			setDaemon(true);
		}
		
		/**
		 * Invoke to wake up the daemon and recheck the queue
		 */
		public void reschedule() {
			synchronized(idleLocker) {
				idleLocker.notify();
			}
		}
		
		@Override
		public void run() {
	
			while (true) { // continually load whatever needs loading (daemon)
				
				// See if there is an item in the load queu
				long waitTime = -1;
				QueuedEntity qe = null;
				synchronized(entityLocker) {
					
					assert (busyEntity == null);
					busyEntity = loadQueue.peek();
					
					if (busyEntity != null) {
						// See if it is time to load the entity
						waitTime = busyEntity.loadTime - System.currentTimeMillis();
						if (busyEntity.priority > 0 || waitTime <= 0) {
							loadQueue.remove(); // remove the entity
							queueMap.remove(busyEntity.entity);
							qe = busyEntity;
						} else {
							busyEntity = null; // not ready to load yet
						}
					}
					
				}
				
				// If there is nothing to load
				if (qe == null) {
					try {
						synchronized(idleLocker) { // obtain idleLocker's monitor
							if (waitTime > 0) { // if there is a entity to load in a few momenets, wait until delay elapsed
								idleLocker.wait(waitTime);
							} else { // If there is nothing to load, then wait until something is added
								idleLocker.wait();
							}
						}
					} catch (InterruptedException e) { // Something added/re-prioritized.
						/* Consume */
					}
					
				} else { // load a queued entity
					
					try {
						qe.entity.performLoad();
					} catch (Exception e) { // safety
						e.printStackTrace();
					}
					
					synchronized(entityLocker) { // Not busy anymore
						busyEntity = null;
					}
					
				}

			} // continue running daemon
		}
	}
	
	/**
	 * Used for prioritized queue
	 * 
	 * @author Brook
	 */
	private class QueuedEntity implements Comparable {

		QueuedEntity(LoadableEntity entity, int minDelay, int priority) {
			this.entity = entity;
			this.priority = priority;
			loadTime = System.currentTimeMillis() + minDelay;
		}
		
		private QueuedEntity(LoadableEntity entity, long loadTime, int priority) {
			this.entity = entity;
			this.priority = priority;
			this.loadTime = loadTime;
		}
		
		public QueuedEntity higherPriority() {
			return new QueuedEntity(entity, loadTime, priority + 1);
		}

		private LoadableEntity entity;
		private long loadTime;
		private int priority;
		
		public int compareTo(Object obj) {
			QueuedEntity other = (QueuedEntity)obj;
			if (priority == other.priority) {
				return new Long(loadTime).compareTo(other.loadTime);
			} else if (priority < 0) {
				return -1;
			}
			return 1;
			
		}

		@Override
		public int hashCode() {
			return entity.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return entity.equals(obj);
		}
	}


}
