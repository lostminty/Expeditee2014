package org.expeditee.simple;

import junit.framework.TestCase;

public class SRealTest extends TestCase {

	private SReal pos, r0, r1, neg;

	private SPrimitive prim, sInt;

	protected void setUp() throws Exception {
		super.setUp();
		pos = new SReal(24.5);
		r0 = new SReal(0);
		r1 = new SReal(0.9);
		neg = new SReal(-1.3);
		prim = neg;
		sInt = new SInteger(13);
	}

	public final void testIntegerValue() {
		assertEquals(new Long(25L), pos.integerValue());
		assertEquals(new Long(0L), r0.integerValue());
		assertEquals(new Long(-1L), neg.integerValue());
	}

	public final void testBooleanValue() {
		assertEquals(new Boolean(true), pos.booleanValue());
		assertEquals(new Boolean(false), r0.booleanValue());
		assertEquals(new Boolean(true), r1.booleanValue());
		assertEquals(new Boolean(false), neg.booleanValue());
	}

	public final void testDoubleValue() {
		assertEquals(new Double(24.5), pos.doubleValue());
		assertEquals(new Double(0.0), r0.doubleValue());
		assertEquals(new Double(-1.3), neg.doubleValue());
	}

	public final void testCharacterValue() {
		assertEquals(new Character((char) 25), pos.characterValue());
		assertEquals(new Character((char) 0), r0.characterValue());
		assertEquals(new Character((char) -1), neg.characterValue());
	}

	public final void testSetValueSPrimitive() {
		prim.setValue(pos);
		assertEquals(pos.getValue(), prim.getValue());
		prim.setValue(sInt);
		assertEquals(13.0, prim.getValue());
	}

	public final void testParse() throws Exception{
		pos.parse("");
		assertEquals(new Double(0), pos.value_);
		pos.parse("0xfF");
		assertEquals(new Double(255), pos.value_);
		pos.parse("25.7");
		assertEquals(new Double(25.7), pos.value_);

		try {
			pos.parse(null);
		} catch (Exception e) {
			return;
		}
		fail();
	}

}
