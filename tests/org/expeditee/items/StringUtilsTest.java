package org.expeditee.items;

import junit.framework.TestCase;

public class StringUtilsTest extends TestCase{

	public void setUp() throws Exception {
	}

	public final void testConvertNewLineChars() {
		String windows = "Line 1\r\nLine 2\r\nLine 3\r\nLine 4";
		String linux = "Line 1\nLine 2\nLine 3\nLine 4";
		String mac = "Line 1\rLine 2\rLine 3\rLine 4";
		String mix = "Line 1\rLine 2\nLine 3\r\nLine 4";
		String result = linux;
		assertEquals(result, StringUtils.convertNewLineChars(windows));
		assertEquals(result, StringUtils.convertNewLineChars(linux));
		assertEquals(result, StringUtils.convertNewLineChars(mac));
		assertEquals(result, StringUtils.convertNewLineChars(mix));
	}

}
