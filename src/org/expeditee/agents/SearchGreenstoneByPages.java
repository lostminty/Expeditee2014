package org.expeditee.agents;

import java.util.Collections;
import java.util.Map;
import java.util.Vector;

import org.expeditee.greenstone.Result;
import org.expeditee.greenstone.ResultDocument;
import org.expeditee.gui.FrameCreator;

public class SearchGreenstoneByPages extends SearchGreenstone {

	public SearchGreenstoneByPages(int resultsCount, String searchText) {
		super(resultsCount, searchText);
	}
	
	public SearchGreenstoneByPages(String searchText) {
		super(searchText);
	}
	
	public SearchGreenstoneByPages() {
		super();
	}

	protected void createResults() {
		if (_pageCountMap.isEmpty()) {
			initialisePageCountMap(_currentResultSet);
		}
		viewByPageCount(_pageCountMap, _results);
	}

	public void initialisePageCountMap(Vector<Result> results) {
		for (Result result : results) {
			String docID = result.getDocID();

			ResultDocument rd = _gsdl.getDocument(docID);

			if (rd.metadataExists("Pages")) {
				addToPageCountMap(docID, rd);
			}
		}
	}

	public void addToPageCountMap(String docID, ResultDocument rd) {
		int pages = 1;
		String[] pageRange = rd.getPages().trim().split("-");

		if (pageRange.length == 2) {
			try {
				pages = Integer.parseInt(pageRange[1])
						- Integer.parseInt(pageRange[0]) + 1;
			} catch (Exception e) {
				return;
			}
		}

		if (_pageCountMap.containsKey(pages)) {
			Vector<String> dateVector = _pageCountMap.get(pages);
			if (!dateVector.contains(docID)) {
				dateVector.addElement(docID);
			}
		} else {
			Vector<String> dateVector = new Vector<String>();
			dateVector.addElement(docID);
			_pageCountMap.put(pages, dateVector);
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
	private void viewByPageCount(Map<Integer, Vector<String>> pageCountMap,
			FrameCreator results) {
		Vector<Integer> thePageCounts = new Vector<Integer>(pageCountMap
				.keySet());
		Collections.sort(thePageCounts);

		for (Integer pageCount : thePageCounts) {
			Vector<String> ids = pageCountMap.get(pageCount);

			// Text dateText = results.addText(date, null, null, null, false);
			// dateText.setFontStyle("bold");

			for (String docID : ids) {
				ResultDocument rd = _gsdl.getDocument(docID);
				addText(rd, results, pageCount + "- " + rd.getTitle());
			}
		}
	}
}
