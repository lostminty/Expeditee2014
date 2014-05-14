package org.apollo.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * A FileFilter that filters via regular expressions.
 * @author Bruce Eckel
 */
public class RegExpFileFilter implements FilenameFilter {
	private Pattern pattern;

	public RegExpFileFilter(String regex) {
		pattern = Pattern.compile(regex);
	}

	public boolean accept(File dir, String name) {
		// Strip path information, search for regex:
		return pattern.matcher(new File(name).getName()).matches();
	}
}
