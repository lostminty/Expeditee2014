package org.expeditee.stats;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.io.Conversion;
import org.expeditee.settings.folders.FolderSettings;

public class DocumentStatsFast extends Stats {
	protected int _treeFrames = 0;
	
	protected int _characters = 0;

	protected int _words = 0;

	protected int _textItems = 0;

	protected int _sentences = 0;

	protected String _name = null;

	protected String _title = null;

	public static int wordCount(String paragraph) {
		return paragraph.trim().split("\\s+").length + 1;
	}

	public DocumentStatsFast(String topFrame, String title) {
		this(topFrame, new HashSet<String>());
		_title = title;
	}

	public DocumentStatsFast(String topFrame, Set<String> visited) {
		_name = topFrame;
		String lowerName = _name.toLowerCase();

		if (visited.contains(lowerName)) {
			return;
		}

		visited.add(_name.toLowerCase());
		MessageBay.overwriteMessage("Computed: " + _name);

		// Initialise variables with the data for this frames comet
		_words = 0;
		_characters = 0;
		_textItems = 0;
		_sentences = 0;
		_treeFrames = 1;

		String fullPath = null;
		for (String possiblePath : FolderSettings.FrameDirs.get()) {
			fullPath = FrameIO.getFrameFullPathName(possiblePath, _name);
			if (fullPath != null)
				break;
		}

		// If the frame was not located return null
		if (fullPath == null)
			return;

		String frameset = Conversion.getFramesetName(_name);

		// Open the file and search the text items
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fullPath));
			String next;
			StringBuffer sb = new StringBuffer();
			String link = null;
			boolean ignore = false;
			while (reader.ready() && ((next = reader.readLine()) != null)) {
				if (next.length() == 0) {
					// Ignore annotations
					if (ignore) {
						ignore = false;
						link = null;
						continue;
					}

					// Ignore non text items
					if (sb.length() == 0) {
						link = null;
						continue;
					}

					if (link == null) {
						// remove the last newLine... not absolutely needed
						String text = sb.substring(0, sb.length() - 1);
						_textItems++;
						_characters += text.length();
						_words += text.split("\\s+").length;
						_sentences += text.split("\\.+").length;
					} else {
						DocumentStatsFast childItemStats = new DocumentStatsFast(
								link, visited);
						_characters += childItemStats._characters;
						_words += childItemStats._words;
						_textItems += childItemStats._textItems;
						_sentences += childItemStats._sentences;
						_treeFrames += childItemStats._treeFrames;
					}
					// Reinit the item variables
					link = null;
					sb = new StringBuffer();
				} else if (ignore) {
					continue;
				} else if (next.startsWith("T")) {
					String text = next.substring(2).trim();
					// Ignore the rest of annotation items...
					if (text.length() > 0
							&& text.charAt(0) == AttributeValuePair.ANNOTATION_CHAR) {
						ignore = true;
						continue;
					}
					sb.append(text).append('\n');
				} else if (next.startsWith("F")) {
					link = next.substring(2);
					// Convert number only links
					if (Character.isDigit(link.charAt(0)))
						link = frameset + link;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(SessionStats.getDate());
		sb.append("DocStats:  ").append(_name).append('\n');
		sb.append("Title:  ").append(_title).append('\n');
		sb.append("Frames:    ").append(_treeFrames).append('\n');
		sb.append("TextItems: ").append(_textItems).append('\n');
		sb.append("Sentences: ").append(_sentences).append('\n');
		sb.append("Words:     ").append(_words).append('\n');
		sb.append("Chars:     ").append(_characters);
		return sb.toString();
	}
}
