package org.expeditee.io;

import junit.framework.TestCase;

public class DefaultFrameWriterTest extends TestCase {

	public final void testGetValidFilename() {
		assertEquals("", DefaultFrameWriter.getValidFilename(""));
		assertEquals("_b_c", DefaultFrameWriter.getValidFilename(" b c"));
		assertEquals("Test_txt", DefaultFrameWriter.getValidFilename("Test.txt"));
	}

}
