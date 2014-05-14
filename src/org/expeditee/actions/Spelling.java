package org.expeditee.actions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.expeditee.agents.wordprocessing.JSpellChecker;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.settings.folders.FolderSettings;

public class Spelling {
	public static Collection<String> spellCheckFrame(String frameName,
			String path) {
		String fullPath = null;
		JSpellChecker spellChecker = null;
		try {
			spellChecker = JSpellChecker.getInstance();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return null;
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		if (path == null) {
			for (String possiblePath : FolderSettings.FrameDirs.get()) {
				fullPath = FrameIO
						.getFrameFullPathName(possiblePath, frameName);
				if (fullPath != null)
					break;
			}
		} else {
			fullPath = FrameIO.getFrameFullPathName(path, frameName);
		}
		// If the frame was not located return null
		if (fullPath == null)
			return null;
		Collection<String> results = new LinkedList<String>();
		// Open the file and search the text items
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fullPath));
			String next;
			while (reader.ready() && ((next = reader.readLine()) != null)) {
				if (next.startsWith("T")) {
					String toSearch = next.substring(2);
					spellChecker.setText(toSearch);
					String misspelled = spellChecker.getMisspelledWord();
					if (misspelled != null && misspelled.length() > 0) {
						results.add(toSearch);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return results;
	}

	public static String checkSpelling(String word) {
		try {
			return JSpellChecker.getInstance().getSuggestions(word);
		} catch (FileNotFoundException e) {
			MessageBay.errorMessage("Could not find dictionary: "
					+ e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String spellCheck(String word) {
		return checkSpelling(word);
	}
}
