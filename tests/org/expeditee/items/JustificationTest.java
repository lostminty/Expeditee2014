package org.expeditee.items;

import junit.framework.TestCase;

public class JustificationTest extends TestCase {

	public final void testGetJustification() {
		assertEquals(Justification.full, Justification.convertString("   FULL   "));
		assertEquals(Justification.left, Justification.convertString("l"));
		assertEquals(Justification.right, Justification.convertString("R"));
		assertEquals(Justification.center, Justification.convertString("CeNtEr"));
		assertEquals(Justification.left, Justification.convertString("CeNtre"));
	}

	public final void testGetCode() {
		assertEquals('C', Justification.center.getCode());
	}

	public final void testToString() {
		assertEquals("center", Justification.center.toString());
	}
}