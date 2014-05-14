package org.expeditee.agents;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.expeditee.agents.wordprocessing.JSpellChecker;

public class SpellcheckTree extends SearchTree {

	private JSpellChecker _spellChecker = null;

	public SpellcheckTree() {
		super(null);

		try {
			_spellChecker = JSpellChecker.getInstance();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TODO better handle errors
	}

	@Override
	protected String getResultSurrogate(String toSearch) {
		if (_spellChecker == null)
			return null;
		_spellChecker.setText(toSearch);
		_spellChecker.check();

		String badSpelling = _spellChecker.getMisspelledWord();
		String result = badSpelling;
//		while (badSpelling != null) {
//			_spellChecker.ignoreWord(true);
//			_spellChecker.check();
//			badSpelling = _spellChecker.getMisspelledWord();
//			if (badSpelling != null)
//				result += "," + badSpelling;
//		}

		if (badSpelling == null || badSpelling.length() <= 1)
			return null;

		return "[" + result + "] " + toSearch;
	}
	
	@Override
	protected String getResultsTitleSuffix() {
		return "";
	}
}
