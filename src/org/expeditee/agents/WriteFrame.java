package org.expeditee.agents;

public class WriteFrame extends WriteTree {

	public WriteFrame() {
		super();
		super.setFollowLinks(false);
	}

	public WriteFrame(String format) {
		super(format);
		super.setFollowLinks(false);
	}
}
