package org.expeditee.items;

public class StringUtils {

	public static String convertNewLineChars(String string) {
		return string.replaceAll("\r\n|\r", "\n");
	}
}
