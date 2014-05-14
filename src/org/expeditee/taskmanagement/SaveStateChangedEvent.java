package org.expeditee.taskmanagement;

import java.util.EventObject;


/**
 * Occurs when a saveable entities (save)state changes.
 * 
 * @author Brook Novak
 */
public class SaveStateChangedEvent extends EventObject {
    
	private static final long serialVersionUID = 1L;
	
	private SaveableEntity entity;
	
	public SaveStateChangedEvent(Object source, SaveableEntity entity) {
        super(source);
        this.entity = entity;
    }

	public SaveableEntity getEntity() {
		return entity;
	}

}

