package org.expeditee.agents;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.expeditee.greenstone.Greenstone3Connection;
import org.expeditee.greenstone.Query;
import org.expeditee.greenstone.QueryOutcome;
import org.expeditee.greenstone.Result;
import org.expeditee.greenstone.ResultDocument;
import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameCreator;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Text;

public class SearchGreenstone extends SearchAgent {

	private static String _fullCaseSearchQuery = null;

	private static boolean _doCasefolding = true;

	private static boolean _doStemming = false;

	protected static Greenstone3Connection _gsdl = null;

	public static Greenstone3Connection getConnection() {
		return _gsdl;
	}

	private static String _maxResults = "10";

	private static boolean _showAbstract = false;

	private static boolean _showKeywords = false;

	private static boolean _showAuthors = false;

	private static boolean _showDate = false;

	private String _thisMaxResults = "10";

	private int _indexChoice = 1;

	private static boolean _getAllMetadata = true;

	private static int _locationChoice = 1;

	private static String[] _indexKeys = { "TX", "TI", "JO", "BO", "CR", "KE" };

	protected static Vector<Result> _currentResultSet = null;

	private boolean _useLastSearchResults = false;

	/**
	 * dateMap is a hash table. The keys are year values. the data associated
	 * with each key is a Vector of document IDs therefore, for the current
	 * result set you can get the set of years in which the results were
	 * published, and for each year you can get the set of documents published
	 * in that year
	 * 
	 * If you want to introduce additional mappings (eg document written by
	 * authors) you should introduce additional structures here (HashMap used in
	 * the same way as dateMap will probably suffice
	 * 
	 */
	protected static Map<String, Vector<String>> _dateMap = new HashMap<String, Vector<String>>();

	protected static Map<Integer, Vector<String>> _pageCountMap = new HashMap<Integer, Vector<String>>();

	protected static Map<String, String> _titleMap = new HashMap<String, String>();

	public SearchGreenstone(int resultsCount, String searchText) {
		super(searchText);
		_thisMaxResults = resultsCount + "";
		_fullCaseSearchQuery = searchText;
	}

	public SearchGreenstone(String searchText) {
		super(searchText);
		_thisMaxResults = _maxResults;
		_fullCaseSearchQuery = searchText;
	}

	public SearchGreenstone(int resultsCount) {
		this(null);
		_thisMaxResults = resultsCount + "";
	}

	public SearchGreenstone() {
		super(null);
		_useLastSearchResults = true;
	}

	public static void init(Frame settings) {
		if (settings == null)
			return;

		_maxResults = "10";
		_showAbstract = false;
		_showKeywords = false;
		_showAuthors = false;
		_showDate = false;
		_locationChoice = 1;

		// Set the settings
		for (Text item : settings.getBodyTextItems(false)) {

			AttributeValuePair avp = new AttributeValuePair(item.getText());
			if (avp.isAnnotation())
				continue;

			String attribute = avp.getAttributeOrValue().toLowerCase();

			if (attribute.equals("campus"))
				_locationChoice = 0;
			else if (attribute.equals("autoconnect"))
				connect();
			else if (attribute.equals("maxresults")) {
				try {
					_maxResults = avp.getValue();
				} catch (Exception e) {
				}
			} else if (attribute.equals("dostemming"))
				_doStemming = true;
			else if (attribute.startsWith("showabstract"))
				_showAbstract = true;
			else if (attribute.startsWith("showauthor"))
				_showAuthors = true;
			else if (attribute.startsWith("showkeyword"))
				_showKeywords = true;
			else if (attribute.startsWith("showdate"))
				_showDate = true;
		}
	}

	public static void connect() {
		if (_gsdl == null)
			_gsdl = new Greenstone3Connection(_locationChoice);
	}

	protected String getResultsTitle() {
		return this.getClass().getSimpleName() + "[" + getCursorText() + "]";
	}

	@Override
	protected Frame process(Frame frame) {
		String resultsTitle = getResultsTitle();
		_results.setTitle(resultsTitle);

		if (!_useLastSearchResults) {
			connect();
			doQuery(_pattern);
		} else if (_currentResultSet != null) {
			Text newText = DisplayIO.getCurrentFrame().createNewText(
					getCursorText());
			_clicked = newText;
			FrameMouseActions.pickup(newText);
		}

		if (_currentResultSet == null || _currentResultSet.size() == 0) {
			MessageBay.errorMessage(getNoResultsMessage());
			return null;
		}

		createResults();

		_results.save();

		String resultFrameName = _results.getName();
		if (_clicked != null) {
			_clicked.setLink(resultFrameName);
			_clicked.setText(resultsTitle);
		}

		return _results.getFirstFrame();
	}

	protected String getNoResultsMessage() {
		return "Could not find Greenstone query text";
	}

	/**
	 * @return
	 */
	protected String getCursorText() {
		return _fullCaseSearchQuery;
	}

	protected void createResults() {
		viewByScore(_currentResultSet, _results);
	}

	/**
	 * TODO make this more efficient so the maps are loaded on demand...
	 * 
	 * @param queryText
	 */
	protected void doQuery(String queryText) {
		_pageCountMap.clear();
		_dateMap.clear();
		_titleMap.clear();

		Query query = createQuery(queryText);
		QueryOutcome queryOutcome = _gsdl.issueQueryToServer(query);
		if (queryOutcome != null)
			_currentResultSet = getResultSetMetadata(queryOutcome);
	}

	private Query createQuery(String queryText) {

		Query query = new Query();
		// set the query options
		query.setQueryText(queryText);
		query.setIndex(_indexKeys[_indexChoice]);
		query.setMaxDocsToReturn(_thisMaxResults);

		if (_doStemming) {
			query.setStemming("1");
		} else {
			query.setStemming("0");
		}

		if (_doCasefolding) {
			query.setCasefolding("1");
		} else {
			query.setCasefolding("0");
		}

		return query;
	}

	public Vector<Result> getResultSetMetadata(QueryOutcome queryOutcome) {

		Vector<Result> results = queryOutcome.getResults();
		for (Result result : results) {
			getResultMetadata(result);
		}
		return results;
	}

	private void getResultMetadata(Result result) {
		String docID = result.getDocID();

		_gsdl.getDocumentMetadataFromServer(docID, "Title");
		_gsdl.getDocumentMetadataFromServer(docID, "Date");

		if (_getAllMetadata) {
			_gsdl.getDocumentMetadataFromServer(docID, "Date");
			_gsdl.getDocumentMetadataFromServer(docID, "Booktitle");
			_gsdl.getDocumentMetadataFromServer(docID, "Journal");
			_gsdl.getDocumentMetadataFromServer(docID, "Creator");
			_gsdl.getDocumentMetadataFromServer(docID, "Keywords");
			_gsdl.getDocumentMetadataFromServer(docID, "Publisher");
			_gsdl.getDocumentMetadataFromServer(docID, "Abstract");
			_gsdl.getDocumentMetadataFromServer(docID, "Pages");
			_gsdl.getDocumentMetadataFromServer(docID, "Number");
			_gsdl.getDocumentMetadataFromServer(docID, "Volume");
		}

	}

	/*
	 * given the Vector of result items (ordered by descending relevance to the
	 * query) this method iterates through them and constructs an HTML document
	 * and has it rendered in the result window
	 * 
	 * This is the default presentation for results
	 * 
	 * You can modify this method as you wish to change the HTML for the default
	 * presentation
	 * 
	 * Some useful method calls are illustrated here - if you have the ID of a
	 * document you can get all data stored for it with ResultDocument rd =
	 * gsdl.getDocument(docID); - a ResultDocument object has a set of methods
	 * for getting metadata values for that document most metadata is a single
	 * value, but authors and keywords can have multiple values and these are
	 * stored in a Vector
	 * 
	 * The IF condition for 'Title' shows how to construct a link that can be
	 * clicked on and then dealt with by the handleLinkClick() method
	 * 
	 */
	public void viewByScore(Vector<Result> results, FrameCreator resultsCreator) {

		for (Result result : results) {
			String docID = result.getDocID();
			ResultDocument rd = _gsdl.getDocument(docID);
			int docRank = result.getRank();

			addText(rd, resultsCreator, (docRank + 1) + ". " + rd.getTitle());
		}
	}

	protected String getDetails(ResultDocument rd) {
		StringBuffer resultText = new StringBuffer("");

		if (rd.metadataExists("Title")) {
			resultText.append("title: " + rd.getTitle()).append('\n');
		}
		if (rd.metadataExists("Date")) {
			resultText.append("date: " + rd.getDate()).append('\n');
		}
		if (rd.metadataExists("Booktitle")) {
			resultText.append("booktitle: " + rd.getBooktitle()).append('\n');
		}
		if (rd.metadataExists("Pages")) {
			resultText.append("pages: " + rd.getPages()).append('\n');
		}
		if (rd.metadataExists("Journal")) {
			resultText.append("journal: " + rd.getJournal()).append('\n');
		}
		if (rd.metadataExists("Volume")) {
			resultText.append("volume: " + rd.getVolume()).append('\n');
		}
		if (rd.metadataExists("Number")) {
			resultText.append("number: " + rd.getNumber()).append('\n');
		}
		if (rd.metadataExists("Editor")) {
			resultText.append("editor: " + rd.getEditor()).append('\n');
		}
		if (rd.metadataExists("Publisher")) {
			resultText.append("publisher: " + rd.getPublisher()).append('\n');
		}
		if (rd.metadataExists("Abstract")) {
			resultText.append("abstract: " + rd.getAbstract()).append('\n');
		}
		for (String author : rd.getAuthors()) {
			resultText.append("author: " + author).append('\n');
		}

		for (String keyword : rd.getKeywords()) {
			resultText.append("keyword: " + keyword).append('\n');
		}

		resultText.deleteCharAt(resultText.length() - 1);

		return resultText.toString();
	}

	@Override
	public Frame getResultFrame() {
		if (_useLastSearchResults && _currentResultSet == null)
			return null;

		return super.getResultFrame();
	}

	protected void addText(ResultDocument rd, FrameCreator results, String text) {
		// Put the details on a separate frame
		FrameCreator details = new FrameCreator(rd.getTitle());
		details.addText(getDetails(rd), null, null, null, true);

		if (_showDate && rd.metadataExists("Date"))
			text += ", " + rd.getDate();

		if (_showAbstract && rd.metadataExists("Abstract"))
			text += "\n  " + rd.getAbstract();

		if (_showAuthors && rd.metadataExists("Creator"))
			text += "\nAuthors" + rd.getAuthors().toString();

		if (_showKeywords && rd.getKeywords().size() > 0)
			text += "\nKeywords" + rd.getKeywords().toString();

		results.addText(text, null, details.getName(), "getTextFromChildFrame",
				false);

		FrameGraphics.requestRefresh(true);
	}

	public static void clearSession() {
		getConnection().getSessionResults().clear();
		_currentResultSet = null;
	}

}
