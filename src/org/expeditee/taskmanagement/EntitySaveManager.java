package org.expeditee.taskmanagement;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.SwingUtilities;


/**
 * Manages time/resource-consuming saving procedures. For enties in an expeditee Frame that
 * requires heavy-duty saving, implement the entity as a SaveableEntity.
 * 
 * The convention used:
 * 
 * Register a SaveableEntity only if there is a possibility that it might need to be saved
 * at the next save point.
 * 
 * There are two save points:
 * <ul>
 * 		<li>Everytime the current frame is changed.
 * 		<li>When the application exits.
 * </ul>
 * 
 * @author Brook Novak
 */
public class EntitySaveManager {
	
	/**
	 * Singleton design pattern
	 */
	private static EntitySaveManager _instance = new EntitySaveManager();
	public static EntitySaveManager getInstance() {
		return _instance;
	}
	
	private Set<SaveableEntity> entitiesToSave = new HashSet<SaveableEntity>();
	private SaveThread saveThread = null;
	private Set<SaveStateChangedEventListener> listeners = new HashSet<SaveStateChangedEventListener>();
	
	/**
	 * Adds a SaveStateChangedEventListener.
	 * 
	 * @see notes in removeSaveStateChangedEventListener
	 * 
	 * @param listener
	 * 		The listener to add
	 * 
	 * @throws NullPointerException
	 * 		If listener is null
	 */
	public void addSaveStateChangedEventListener(SaveStateChangedEventListener listener) {
		if (listener == null) throw new NullPointerException("listener");
		synchronized(listeners) {
			listeners.add(listener);
		}
	}
	
	/**
	 * Removes a SaveStateChangedEventListener.
	 * 
	 * This is important if your SaveStateChangedEventListener is temporarily used - 
	 * as this is a singleton and will thus keep the SaveStateChangedEventListener
	 * in memory until it is explicity removed (due to java garbage collection).
	 * 
	 * @param listener
	 * 		The listener to add
	 * 
	 * @throws NullPointerException
	 * 		If listener is null
	 */
	public void removeSaveStateChangedEventListener(SaveStateChangedEventListener listener) {
		if (listener == null) throw new NullPointerException("listener");
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}

	/**
	 * Adds an entity for saving at the next save point.
	 * 
	 * @param entity
	 * 		The entity to save
	 * 
	 * @return
	 * 		True if added. False if already registered.
	 * 
	 * @throws NullPointerException
	 * 		If entity is null
	 */
	public boolean register(SaveableEntity entity) {
		if (entity == null) throw new NullPointerException("entity");
		return entitiesToSave.add(entity);
	}
	
	/**
	 * Removes an entity from saving at the next save point.
	 * Note that if in the process of saving or is about to save, then it will not be removed.
	 * In such cases it is up to the entity to ignore the save command.
	 * 
	 * @param entity
	 * 		The entity to cancel save
	 * 
	 * @return
	 * 		True if added. False if already registered.
	 * 
	 * @throws NullPointerException
	 * 		If entity is null
	 */
	public boolean unregister(SaveableEntity entity) {
		if (entity == null) throw new NullPointerException("entity");
		return entitiesToSave.remove(entity);
	}	
	
	/**
	 * Saves all current registered entities and clears registrations.
	 * 
	 * Returns immediatly.
	 */
	public synchronized void saveAll() {
		if (entitiesToSave.isEmpty()) return;
		
		// Get list of entities that needs sacing
		LinkedList<SaveableEntity> toSave = new LinkedList<SaveableEntity>();
		for (SaveableEntity entity : entitiesToSave) {
			if (entity.doesNeedSaving()) toSave.add(entity);
		}
		entitiesToSave.clear();
		
		if (toSave.isEmpty()) return;
			
		// If already saving
		if (saveThread != null && saveThread.isAlive()) {
			// Add the extras
			saveThread.addMoreToSave(toSave);
			
			// If did not finish while adding extras then return...
			if (!saveThread.isFinished) {
				return;
			}
		}
		
		// Start saving
		saveThread = new SaveThread(toSave);
		saveThread.start();
	}
	
	/**
	 * If something is saving, this will block the thread until all entities that
	 * have been requested to be saved has finished their saving proccesses.
	 * 
	 * @throws InterruptedException 
	 * 		If any thread has interrupted the current thread. 
	 * 		Note: The interrupted status of the current thread is cleared when this exception is thrown
	 */
	public void waitUntilAllSavingFinished() throws InterruptedException {
		if (saveThread != null && saveThread.isAlive()) {
			saveThread.join();
		}
	}

	/**
	 * Non-daemon thread for asynchronously saving heavy duty save procs.
	 * 
	 * @author Brook Novak
	 */
	private class SaveThread extends Thread {
		
		private LinkedList<SaveableEntity> toSave = new LinkedList<SaveableEntity>();
		private boolean isFinished = false;
		
		SaveThread(Collection<SaveableEntity> toSave) {
			super("SaveThread");
			
			this.toSave.addAll(toSave);
		}
		
		/**
		 * Adds more entities to save.
		 * 
		 * Note: due to my limited knowledge of java threading I have assumed that
		 * it is possible for the thread to end after the call without having saved
		 * the given extras. Thus check the isFinished - where if true then it indicates
		 * that the extras may not have saved.
		 * 
		 * @param extra Extra entities to save.
		 */
		public void addMoreToSave(Collection<SaveableEntity> extra) {
			assert(extra != null);
			
			synchronized(toSave) {
				for (SaveableEntity entity : extra) {
					if (!toSave.contains(entity)) {
						toSave.add(entity);
					}
				}
			}
		}

		@Override
		public void run() {
			
			// Keep running until saved all entities.
			while (true) {
				SaveableEntity entity = null;
				synchronized(toSave) {
					
					if (toSave.isEmpty()) {
						isFinished = true; // safety check
						return;
					}
					
					entity = toSave.remove();
				}
				
				assert(entity != null);
				
				if (entity.doesNeedSaving()) {
					
					// notify observers that save has started - on swing thread
					SwingUtilities.invokeLater(new SafeFireEvent(entity, true));
	
					// Complete the save
					try {
						entity.performSave();
					} catch (Exception e) { // safety
						e.printStackTrace();
					}
	
					// notify observers that save has finished - on swing thread
					SwingUtilities.invokeLater(new SafeFireEvent(entity, false));
				}
			}
		}
		
		/**
		 * Notifies all listners of save start/completion event when run.
		 * 
		 * @author Brook Novak
		 *
		 */
		private class SafeFireEvent implements Runnable {
			
			private SaveableEntity entity;
			private boolean started;
			
			SafeFireEvent(SaveableEntity entity, boolean started) {
				this.entity = entity;
				this.started = started;
			}
			
			public void run() {
				synchronized(listeners) {
					for (SaveStateChangedEventListener listener : listeners) {
						if (started) listener.saveStarted(new SaveStateChangedEvent(this, entity));
						else listener.saveCompleted(new SaveStateChangedEvent(this, entity));
					}
				}
			}
			
		}
		

	}

}
