package org.expeditee.agents.wordprocessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.items.Text;

import com.swabunga.spell.engine.Configuration;
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.event.SpellCheckEvent;
import com.swabunga.spell.event.SpellCheckListener;
import com.swabunga.spell.event.SpellChecker;
import com.swabunga.spell.event.StringWordTokenizer;

public class JSpellChecker {

	private class JSpellCheckListener extends Thread implements
			SpellCheckListener {
		private JSpellCheckListener(StringWordTokenizer tokens) {
			log(this.hashCode() + ": JSpellCheckListener.init");
			this.tokens = tokens;
		}

		public synchronized void spellingError(SpellCheckEvent event) {
			log(this.hashCode() + ": JSpellCheckListener.spellingError");
			JSpellChecker.this.event = event;
			synchronized (JSpellChecker.this) {
				JSpellChecker.this.notifyAll();
			}
			try {
				this.wait();
			} catch (InterruptedException e) {
			}
		}

		public boolean done() {
			return done;
		}

		public void run() {
			log(this.hashCode() + ": JSpellCheckListener.run");
			done = false;
			checker.checkSpelling(this.tokens);
			done = true;
			synchronized (JSpellChecker.this) {
				JSpellChecker.this.notifyAll();
			}
		}

		private boolean done;

		private StringWordTokenizer tokens;
	}

	public static JSpellChecker _theInstance = null;

	public static JSpellChecker getInstance() throws FileNotFoundException,
			IOException {
		if (_theInstance == null) {
			create();
		}
		return _theInstance;
	}

	public static void create(Frame frame) throws FileNotFoundException,
			IOException {
		if (_theInstance == null)
			_theInstance = new JSpellChecker();
		_theInstance.init();

		if (frame != null) {
			for (Text t : frame.getBodyTextItems(false)) {
				for (String s : t.getText().split(" ")) {
					_theInstance.checker.addToDictionary(s);
				}
			}
		}
	}

	public static void create() throws FileNotFoundException, IOException {
		create(null);
	}

	public static String dictDirectory = FrameIO.PARENT_FOLDER + "dict/";

	public JSpellChecker() {
		initialized = false;
	}

	public void init() throws FileNotFoundException, IOException {
		log(this.hashCode() + ": JSpellChecker.init");
		//MessageBay.displayMessage("Loading spell checker...");

		checker = new SpellChecker();
		SpellDictionaryHashMap dictionaryHashMap = new SpellDictionaryHashMap();

		for (File dict : new File(dictDirectory).listFiles()) {
			if (dict.isFile()) {
				dictionaryHashMap.addDictionary(dict);
			}
		}

		checker.addDictionary(dictionaryHashMap);

		initialized = true;

		//MessageBay.displayMessage("Spell checker Loaded", Color.green.darker());
	}

	public void setText(String text) {
		log(this.hashCode() + ": JSpellChecker.setText(" + text + ")");
		if (!initialized)
			throw new java.lang.IllegalStateException(
					"Spellchecker is loading.");
		checker.removeSpellCheckListener(listener);
		tokens = new StringWordTokenizer(text);
		listener = new JSpellCheckListener(tokens);
		checker.addSpellCheckListener(listener);
		checker.reset();
		event = null;
	}

	public void setOptions(int options) {
		// System.out.println("in " + getClass().getName() + ".setOptions(" +
		// Integer.toBinaryString(options) + ")");
		Configuration config = checker.getConfiguration();
		for (int i = 1; i <= 0x20000; i = i << 1) {
			String key = (String) configmap.get(new Integer(i));
			if (key != null)
				config.setBoolean(key, (options & i) > 0);
		}
	}

	public synchronized boolean check() {
		if (!listener.isAlive()) {
			listener.start();
			try {
				wait();
			} catch (InterruptedException e) {
			}
			return listener.done();
		}
		synchronized (listener) {
			listener.notifyAll();
		}
		try {
			wait();
		} catch (InterruptedException e) {
		}
		return listener.done();
	}

	private String getSuggestionList(List suggestions) {
		String s = "";
		for (Iterator i = suggestions.iterator(); i.hasNext();) {
			com.swabunga.spell.engine.Word element = (com.swabunga.spell.engine.Word) i
					.next();
			s += element.getWord() + ",";

		}

		if (s.length() == 0) {
			return "No suggestions";
		}
		// for (int i = 0; i < suggestions.size(); i ++) {
		// s +=
		// ((com.swabunga.spell.engine.Word)suggestions.elementAt(i)).getWord()
		// + "|";
		// }
		return s.substring(0, s.length() - 1);
	}

	public String getSuggestions() {
		return getSuggestionList(event.getSuggestions());
	}

	public String getSuggestions(String word) {
		return getSuggestionList(checker.getSuggestions(word, 0));
	}

	public String getMisspelledWord() {
		if (event != null)
			return event.getInvalidWord();
		return null;
	}

	public int getCursorPosition() {
		if (event != null)
			return event.getWordContextPosition();
		return 0;
	}

	public void changeWord(String word, boolean all) {
		if (event != null) {
			if (word == null)
				word = "";
			event.replaceWord(word, all);
		}
	}

	public void ignoreWord(boolean all) {
		if (event != null) {
			event.ignoreWord(all);
		}
	}

	// private String getText(){
	// if (tokens != null)
	// return tokens.getFinalText();
	// return null;
	// }

	protected static void log(String s) {
		System.out.println(s);
	}

	private boolean initialized;

	protected SpellCheckEvent event;

	protected SpellChecker checker;

	private StringWordTokenizer tokens;

	private JSpellCheckListener listener;

	// private static final int CASE_SENSITIVE = 1;
	private static final int IGNORE_ALL_CAPS_WORD = 2;

	private static final int IGNORE_CAPPED_WORD = 4;

	private static final int IGNORE_MIXED_CASE = 8;

	private static final int IGNORE_MIXED_DIGITS = 16;

	// private static final int IGNORE_NON_ALPHA_WORD = 32;
	private static final int REPORT_DOUBLED_WORD = 64;

	// private static final int REPORT_MIXED_CASE = 128;
	// private static final int REPORT_MIXED_DIGITS = 256;
	// private static final int REPORT_SPELLING = 512;
	// private static final int REPORT_UNCAPPED = 1024;
	// private static final int SPLIT_CONTRACTED_WORDS = 2048;
	// private static final int SPLIT_HYPHENATED_WORDS = 4096;
	// private static final int SPLIT_WORDS = 8192;
	// private static final int STRIP_POSSESSIVES = 16384;
	// private static final int SUGGEST_SPLIT_WORDS = 32768;
	private static final int IGNORE_DOMAIN_NAMES = 0x10000;

	// private static final int ALLOW_ACCENTED_CAPS = 0x20000;

	private static Hashtable<Integer, String> configmap;

	static {
		configmap = new Hashtable<Integer, String>();
		configmap.put(new Integer(IGNORE_MIXED_DIGITS),
				Configuration.SPELL_IGNOREDIGITWORDS);
		configmap.put(new Integer(IGNORE_DOMAIN_NAMES),
				Configuration.SPELL_IGNOREINTERNETADDRESSES);
		configmap.put(new Integer(IGNORE_MIXED_CASE),
				Configuration.SPELL_IGNOREMIXEDCASE);
		configmap.put(new Integer(REPORT_DOUBLED_WORD),
				Configuration.SPELL_IGNOREMULTIPLEWORDS);
		configmap.put(new Integer(IGNORE_CAPPED_WORD),
				Configuration.SPELL_IGNORESENTENCECAPITALIZATION);
		configmap.put(new Integer(IGNORE_ALL_CAPS_WORD),
				Configuration.SPELL_IGNOREUPPERCASE);
	}
}
