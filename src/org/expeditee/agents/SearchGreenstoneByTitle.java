package org.expeditee.agents;

import java.util.Collections;
import java.util.Map;
import java.util.Vector;

import org.expeditee.greenstone.Result;
import org.expeditee.greenstone.ResultDocument;
import org.expeditee.gui.FrameCreator;

public class SearchGreenstoneByTitle extends SearchGreenstone {

	public SearchGreenstoneByTitle(int resultsCount, String searchText) {
		super(resultsCount, searchText);
	}
	
	public SearchGreenstoneByTitle(String searchText) {
		super(searchText);
	}
	
	public SearchGreenstoneByTitle() {
		super();
	}

	protected void createResults() {
		if (_titleMap.isEmpty()) {
			initialiseTitleMap(_currentResultSet);
		}
		viewByTitle(_titleMap, _results);
	}

	public void initialiseTitleMap(Vector<Result> results) {
		for (Result result : results) {
			String docID = result.getDocID();

			ResultDocument rd = _gsdl.getDocument(docID);

			if (rd.metadataExists("Title")) {
				_titleMap.put(rd.getTitle(), docID);
			}
		}
	}

	/**
	 * This method provides an alternative view of the result set It uses the
	 * dateMap which organises results according to the year in which they were
	 * published
	 * 
	 * You will need to write a similar method for any additional view that you
	 * implement
	 * 
	 * You may wish to modify this method to provide a better layout of the
	 * output
	 */
	private void viewByTitle(Map<String, String> titleMap,
			FrameCreator results) {
		Vector<String> theTitles = new Vector<String>(titleMap.keySet());
		Collections.sort(theTitles);

		for (String title : theTitles) {
			String id = titleMap.get(title);

			ResultDocument rd = _gsdl.getDocument(id);

			addText(rd, results, rd.getTitle());
		}
	}
}
