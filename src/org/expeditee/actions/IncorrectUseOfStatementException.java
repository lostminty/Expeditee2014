package org.expeditee.actions;

public class IncorrectUseOfStatementException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String _statement;

	
	
	public IncorrectUseOfStatementException(String message, String statement) {
		super(message);
		_statement = statement;
	}
	
	public String getStatement() {
		return _statement;
	}

}
