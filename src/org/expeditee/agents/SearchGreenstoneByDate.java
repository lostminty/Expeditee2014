package org.expeditee.agents;

import java.util.Collections;
import java.util.Map;
import java.util.Vector;

import org.expeditee.greenstone.Result;
import org.expeditee.greenstone.ResultDocument;
import org.expeditee.gui.FrameCreator;

public class SearchGreenstoneByDate extends SearchGreenstone {

	public SearchGreenstoneByDate(int resultsCount, String searchText) {
		super(resultsCount, searchText);
	}
	
	public SearchGreenstoneByDate(String searchText) {
		super(searchText);
	}
	
	public SearchGreenstoneByDate() {
		super();
	}

	protected void createResults() {
		if(_dateMap.isEmpty()){
			initialiseDateMap(_currentResultSet);
		}
		viewByDate(_dateMap, _results);
	}

	public void initialiseDateMap(Vector<Result> results) {
		for (Result result : results) {
			String docID = result.getDocID();

			ResultDocument rd = _gsdl.getDocument(docID);

			if (rd.metadataExists("Date")) {
				addToDateMap(docID, rd);
			}
		}
	}

	public void addToDateMap(String docID, ResultDocument rd) {
		if (_dateMap.containsKey(rd.getDate())) {
			Vector<String> dateVector = _dateMap.get(rd.getDate());
			if (!dateVector.contains(docID)) {
				dateVector.addElement(docID);
			}
			_dateMap.put(rd.getDate(), dateVector);
		} else {
			Vector<String> dateVector = new Vector<String>();
			dateVector.addElement(docID);
			_dateMap.put(rd.getDate(), dateVector);
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
	private void viewByDate(Map<String, Vector<String>> dateMap,
			FrameCreator results) {
		Vector<String> theDates = new Vector<String>(dateMap.keySet());
		Collections.sort(theDates);

		for (String date : theDates) {
			Vector<String> ids = dateMap.get(date);

			// Text dateText = results.addText(date, null, null, null, false);
			// dateText.setFontStyle("bold");

			for (String docID : ids) {
				ResultDocument rd = _gsdl.getDocument(docID);
				addText(rd, results, date + "- " + rd.getTitle());
			}
		}
	}
}
