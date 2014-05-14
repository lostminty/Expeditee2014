package org.expeditee.simple;

import junit.framework.TestCase;

public class SIntegerTest extends TestCase {

	private SInteger intPos, int0, int1, intNeg;
	private SPrimitive prim, sReal;
		
	protected void setUp() throws Exception {
		super.setUp();
		intPos = new SInteger(25);
		int0 = new SInteger(0);
		int1 = new SInteger(1);
		intNeg = new SInteger(-1);
		prim = intNeg;
		sReal = new SReal(13.0);
	}

	public final void testIntegerValue() {
		assertEquals(new Long(25L), intPos.integerValue());
		assertEquals(new Long(0L), int0.integerValue());
		assertEquals(new Long(-1L), intNeg.integerValue());
	}

	public final void testBooleanValue() {
		assertEquals(new Boolean(true), intPos.booleanValue());
		assertEquals(new Boolean(false), int0.booleanValue());
		assertEquals(new Boolean(true), int1.booleanValue());
		assertEquals(new Boolean(false), intNeg.booleanValue());
	}

	public final void testDoubleValue() {
		assertEquals(new Double(25), intPos.doubleValue());
		assertEquals(new Double(0), int0.doubleValue());
		assertEquals(new Double(-1), intNeg.doubleValue());
	}

	public final void testCharacterValue() {
		assertEquals(new Character((char)25), intPos.characterValue());
		assertEquals(new Character((char)0), int0.characterValue());
		assertEquals(new Character((char)-1), intNeg.characterValue());
	}

	public final void testSetValueSPrimitive() {
		prim.setValue(intPos);
		assertEquals(intPos.getValue(),prim.getValue());
		prim.setValue(sReal);
		assertEquals(13L ,prim.getValue());
	}

	public final void testParse() {
		try{
		intPos.parse("");
		assertEquals(new Long(0), intPos.value_);
		intPos.parse("0xfF");
		assertEquals(new Long(255), intPos.value_);
		intPos.parse("25.7");
		assertEquals(new Long(26), intPos.value_);
		}catch(Exception e){
			fail();
		}
		
		try{
		intPos.parse(null);
		}catch(Exception e){
			return;
		}
		fail();
	}
}
