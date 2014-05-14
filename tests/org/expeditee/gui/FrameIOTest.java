package org.expeditee.gui;

import junit.framework.TestCase;

public class FrameIOTest extends TestCase {

	public void testIsValidFrameName() {
		assertTrue(FrameIO.isValidFrameName("a1"));
		assertTrue(FrameIO.isValidFrameName("a1c1"));
		assertFalse(FrameIO.isValidFrameName("1a1"));
		assertFalse(FrameIO.isValidFrameName("$a1"));
		assertFalse(FrameIO.isValidFrameName("aa"));
		assertFalse(FrameIO.isValidFrameName("a1.1"));
		assertFalse(FrameIO.isValidFrameName(null));
		assertFalse(FrameIO.isValidFrameName("1"));
		assertFalse(FrameIO.isValidFrameName("a"));
		assertFalse(FrameIO.isValidFrameName(""));
	}

	public void testIsValidFramesetName() {
		assertTrue(FrameIO.isValidFramesetName("a"));
		assertTrue(FrameIO.isValidFramesetName("a1c"));
		assertFalse(FrameIO.isValidFramesetName("1a"));
		assertFalse(FrameIO.isValidFramesetName("$a"));
		assertFalse(FrameIO.isValidFramesetName("a$"));
		assertFalse(FrameIO.isValidFramesetName("a.a"));
		assertFalse(FrameIO.isValidFramesetName(null));
		assertFalse(FrameIO.isValidFramesetName("1"));
		assertFalse(FrameIO.isValidFrameName(""));
		assertFalse(FrameIO
				.isValidFramesetName("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz012345678901A"));

	}

	public void testConvertToFramesetName() {
		assertEquals(FrameIO.ConvertToValidFramesetName("a"), "a");
		assertEquals(FrameIO.ConvertToValidFramesetName("a b"), ("aB"));
		assertEquals(FrameIO.ConvertToValidFramesetName("1a"), ("A1a"));
		assertEquals(FrameIO.ConvertToValidFramesetName("$a"), ("A"));
		assertEquals(FrameIO.ConvertToValidFramesetName("a$"), ("a"));
		assertEquals(FrameIO.ConvertToValidFramesetName("a.a"), ("aA"));
		assertEquals(FrameIO.ConvertToValidFramesetName("1"), ("A1A"));
		assertEquals(
				FrameIO
						.ConvertToValidFramesetName("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz01234567890123456789"),
				("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz01234567890A"));

	}
	/*
	 * @Test(expected=RuntimeException.class) public void
	 * ConvertToFramesetNameException() {
	 * FrameIO.ConvertToValidFramesetName(null); }
	 * 
	 * @Test(expected=RuntimeException.class) public void
	 * ConvertToFramesetNameException2() {
	 * FrameIO.ConvertToValidFramesetName(""); }
	 */
}
