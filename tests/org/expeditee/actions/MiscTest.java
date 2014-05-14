package org.expeditee.actions;

import junit.framework.TestCase;

public class MiscTest extends TestCase {
	public void testGetClassName() {
		assertEquals("org.expeditee.agents.Agent", Misc.getClassName("org.expeditee.agents.Agent.Method"));
	}
	
	public void testGetMethodName() {
		assertEquals("Method", Misc.getMethodName("org.expeditee.agents.Agent.Method"));
	}
}
