package org.apollo.audio.structure;

import java.util.Stack;

/**
 * Raised when a track graph contains a loop. Provide loop trace information.
 * 
 * @author Brook Novak
 *
 */
public class TrackGraphLoopException extends Exception {


	private static final long serialVersionUID = 1L;
	
	private Stack<OverdubbedFrame> loopTrace;
	
	public TrackGraphLoopException(Stack<OverdubbedFrame> loopTrace) {
		if (loopTrace == null) {
			loopTrace = new Stack<OverdubbedFrame>();
		} else this.loopTrace = loopTrace;
	}

	/**
	 * The full loop trace ... from where the start of
	 * the loop checker began
	 * 
	 * @return
	 * 		The loop trace. Never null.
	 */
	public Stack<OverdubbedFrame> getFullLoopTrace() {
		return loopTrace;
	}

	@Override
	public String getLocalizedMessage() {
		StringBuilder sb = new StringBuilder("Contains loop ");
		
		String causeFrameName = loopTrace.lastElement().getFrameName();
		boolean mentionedLoopStart = false;
		
		for (OverdubbedFrame odf : loopTrace) {
			sb.append("\n\t\tat ");
			sb.append(odf);
			
			if (!mentionedLoopStart && odf.getFrameName().equals(causeFrameName)) {
				sb.append("\t --- Loop starts here ---");
				mentionedLoopStart = true;
			}
		}
		
		return sb.toString();
	}

	
	
	

}
