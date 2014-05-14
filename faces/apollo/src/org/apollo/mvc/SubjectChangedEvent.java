package org.apollo.mvc;

public class SubjectChangedEvent {
	
	private int id;
	private Object state;
	
	public SubjectChangedEvent(int id) {
		this(id, null);
	}
	
	public SubjectChangedEvent(int id, Object state) {
		this.id = id;
		this.state = state; 
	}

	public int getID() {
		return id;
	}
	
	public Object getState() {
		return state;
	}
	
	public boolean isEmpty() {
		return (this == EMPTY);
	}
	
	/** An event with no data. */
	public static final SubjectChangedEvent EMPTY = new SubjectChangedEvent(-1);


}
