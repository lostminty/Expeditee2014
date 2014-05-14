package org.expeditee.taskmanagement;


/**
 * A saveable entity. These can be added to the SaveEntityManager.
 * 
 * @author Brook Novak
 * 
 * @see EntitySaveManager
 * 
 */
public interface SaveableEntity {

	/**
	 * @return 
	 * 		True if the entity needs saving. False if does not need to save
	 */
	public boolean doesNeedSaving();

	/**
	 * Perform all save logic here. This will be invoked on a dedicated thread.
	 */
	public void performSave();
	
	/**
	 * @return 
	 * 		A human readable string using sentance captalization rules.
	 * 		should be short, and should decribe the entity being saved. 
	 * 		<br>For example: "Video: "goodtimes.avi""
	 * 		<br>or "Audio track".
	 */
	public String getSaveName();
	
}
