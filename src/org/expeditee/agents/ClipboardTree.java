package org.expeditee.agents;

/**
 * Writes the Tree of Frames, starting from the given Frame, to the Clipboard.
 * Currently only Text output is supported, all other items are ignored.
 * 
 * @author jdm18
 * 
 */
public class ClipboardTree extends WriteTree {	
	public ClipboardTree() {
		super(CLIPBOARD);
	}

	public ClipboardTree(String format) {
		super(format + " " + CLIPBOARD);
	}

}
