package org.expeditee.agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.expeditee.greenstone.ResultDocument;
import org.expeditee.gui.FrameCreator;
import org.expeditee.stats.Formatter;

public class DisplayGreenstoneSession extends SearchGreenstone {

	public DisplayGreenstoneSession(int resultsCount) {
		super(resultsCount);
	}

	public DisplayGreenstoneSession() {
		super();
	}

	protected void createResults() {
		if (!_gsdl.getSessionResults().isEmpty()) {
			viewSessionResults(_gsdl.getSessionResults(), _results);
		}
	}
	
	protected String getResultsTitle() {
		return "SessionResults at " + Formatter.getDateTime();
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
	private void viewSessionResults(Map<String, ResultDocument> sessionResults,
			FrameCreator results) {

		List<ResultDocument> resultDocArrayList = new ArrayList<ResultDocument>(
				sessionResults.values());
		Collections.sort(resultDocArrayList,
				new DescendingSessionScoreComparator());

		int docRank = 1;
		
		for(ResultDocument rd: resultDocArrayList){
			addText(rd, results, docRank++ + ". " + rd.getTitle());
		}

	}
	
	protected String getCursorText() {
		return getResultsTitle();
	}

	static class DescendingSessionScoreComparator implements Comparator<ResultDocument> {

		public int compare(ResultDocument d1, ResultDocument d2) {

			double sessionScore1 = d1.getSessionScore();
			double sessionScore2 = d2.getSessionScore();

			if (sessionScore1 > sessionScore2) {
				return -1;
			} else if (sessionScore1 < sessionScore2) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
	protected String getNoResultsMessage() {
		return "The session results set is empty";
	}
}
