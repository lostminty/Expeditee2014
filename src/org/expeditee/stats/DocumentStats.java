package org.expeditee.stats;

import java.util.HashSet;
import java.util.Set;

import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Text;

public class DocumentStats extends CometStats {
	protected int _treeFrames = 0;

	protected int _characters = 0;
	
	protected int _words = 0;

	protected int _textItems = 0;

	protected int _sentences = 0;

	public static int wordCount(String paragraph) {
		return paragraph.trim().split("\\s+").length + 1;
	}

	
	public DocumentStats(Frame topFrame) {
		this(topFrame, new HashSet<String>());
	}

	public DocumentStats(Frame topFrame, Set<String> visited) {
		super(topFrame);
		visited.add(_name.toLowerCase());
		MessageBay.overwriteMessage("Computed: " + _name);

		// Initialise variables with the data for this frames comet
		_characters = 0;
		_words = 0;
		_textItems = 0;
		_sentences = 0;
		_treeFrames = 1;		
		
		// Now get all add all the trees for linked items
		for (Text i : topFrame.getBodyTextItems(false)) {
			_textItems++;
			String text = i.getText().trim();
			_words += text.split("\\s+").length;
			_sentences += text.split("\\.+").length;
			_characters += text.length();
			
			String link = i.getAbsoluteLink();
			if (link == null)
				continue;
			// Stop infinite loops by not visiting nodes we have already visited
			if (visited.contains(link.toLowerCase())) {
				continue;
			}
			Frame childFrame = FrameIO.LoadFrame(i.getAbsoluteLink());
			if (childFrame == null)
				continue;

			DocumentStats childItemStats = new DocumentStats(childFrame,
					visited);
			_words += childItemStats._words;
			_characters += childItemStats._characters;
			_textItems += childItemStats._textItems;
			_sentences += childItemStats._sentences;
			_treeFrames += childItemStats._treeFrames;
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(SessionStats.getDate());
		sb.append("DocStats: ").append(_name).append('\n');
		sb.append("Title:    ").append(_title).append('\n');
		sb.append("Frames:    ").append(_treeFrames).append('\n');
		sb.append("TextItems: ").append(_textItems).append('\n');
		sb.append("Sentences: ").append(_sentences).append('\n');
		sb.append("Words:     ").append(_words).append('\n');
		sb.append("Chars:     ").append(_characters);
		return sb.toString();
	}
}
