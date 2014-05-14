package org.expeditee.actions;


import junit.framework.TestCase;

public class ActionsTest extends TestCase {

	public final void testRemainingParams() {
		assertEquals("", Actions.RemainingParams(" \"test1 test2\""));
		assertEquals("test2", Actions.RemainingParams(" \"test1 test2"));
		
		assertEquals("3", Actions.RemainingParams(" \" t1 t2 \" 3"));
		
		assertEquals("test2", Actions.RemainingParams("test1 test2"));
		assertEquals("\"test2\"", Actions.RemainingParams("\"t1 t2\" \"test2\""));
	}

	public final void testParseValue() {
		assertEquals("test1 test2", Actions.ParseValue(" \"test1 test2\""));
		assertEquals("\"test1", Actions.ParseValue(" \"test1 test2"));
		
		assertEquals(" t1 t2 ", Actions.ParseValue(" \" t1 t2 \" "));
		
		assertEquals("test1", Actions.ParseValue("test1 test2"));
		assertEquals("test1", Actions.ParseValue(  "\"test1\"   \"test2\"  "));
	}

}
