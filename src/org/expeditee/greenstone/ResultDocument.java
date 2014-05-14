package org.expeditee.greenstone;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class ResultDocument {
	private Vector<String> authors;

	private Vector<String> keywords;

	private String title;

	private String date;

	private String booktitle;

	private String pages;

	private String journal;

	private String volume;

	private String number;

	private String docabstract;

	private String editor;

	private String publisher;

	private int frequencyReturned;

	private Vector<QueryContext> queryContexts;

	private Set<String> retrievedMetadata;

	private double sessionScore;

	public ResultDocument() {
		authors = new Vector<String>();
		keywords = new Vector<String>();
		title = null;
		date = null;
		booktitle = null;
		pages = null;
		journal = null;
		volume = null;
		number = null;
		docabstract = null;
		editor = null;
		publisher = null;
		frequencyReturned = 1;
		sessionScore = 0;
		queryContexts = new Vector<QueryContext>();
		retrievedMetadata = new HashSet<String>();
	}

	public String toString() {
		String dump = "Authors\t\t" + authors.toString() + "\n";
		dump = dump + "Keywords\t" + keywords.toString() + "\n";
		dump = dump + "Title\t\t" + title + "\n";
		dump = dump + "Date\t\t" + date + "\n";
		dump = dump + "Booktitle\t" + booktitle + "\n";
		dump = dump + "Pages\t\t" + pages + "\n";
		dump = dump + "Journal\t\t" + journal + "\n";
		dump = dump + "Volume\t\t" + volume + "\n";
		dump = dump + "Number\t\t" + number + "\n";
		dump = dump + "Editor\t\t" + editor + "\n";
		dump = dump + "Publisher\t" + publisher + "\n";
		dump = dump + "Frequency\t" + frequencyReturned + "\n";
		dump = dump + "Abstract\t" + docabstract;

		for (QueryContext queryContext : queryContexts) {
			dump = dump + queryContext.toString();
		}
		return dump;
	}

	public String toDelimitedString(String delimiter) {
		String dump = "";
		dump = dump + date + delimiter;
		if (authors.size() > 0) {
			dump = dump + authors.elementAt(0) + delimiter;
		} else {
			dump = dump + null + delimiter;
		}
		if (keywords.size() > 0) {
			dump = dump + keywords.elementAt(0) + delimiter;
		} else {
			dump = dump + null + delimiter;
		}
		dump = dump + title + delimiter;
		dump = dump + booktitle + delimiter;
		dump = dump + journal + delimiter;
		dump = dump + volume + delimiter;
		dump = dump + number + delimiter;
		dump = dump + editor + delimiter;
		dump = dump + publisher + delimiter;
		dump = dump + frequencyReturned;
		return dump;
	}

	public void addAuthor(String author) {
		this.authors.addElement(author);
		this.retrievedMetadata.add("Creator");
	}

	public void addKeyword(String keyword) {
		this.keywords.addElement(keyword);
		this.retrievedMetadata.add("Keywords");
	}

	public void setTitle(String title) {
		this.title = removeHTML(title);
		this.retrievedMetadata.add("Title");
	}

	public void setDate(String date) {
		this.date = new String(date);
		this.retrievedMetadata.add("Date");
	}

	public void setBooktitle(String booktitle) {
		this.booktitle = new String(booktitle);
		this.retrievedMetadata.add("Booktitle");
	}

	public void setPages(String pages) {
		this.pages = new String(pages);
		this.retrievedMetadata.add("Pages");
	}

	public void setJournal(String journal) {
		this.journal = new String(journal);
		this.retrievedMetadata.add("Journal");
	}

	public void setVolume(String volume) {
		this.volume = new String(volume);
		this.retrievedMetadata.add("Volume");
	}

	public void setNumber(String number) {
		this.number = new String(number);
		this.retrievedMetadata.add("Number");
	}

	public void setAbstract(String docabstract) {
		this.docabstract = new String(docabstract);
		this.retrievedMetadata.add("Abstract");
	}

	public void setEditor(String editor) {
		this.editor = new String(editor);
		this.retrievedMetadata.add("Editor");
	}

	public void setPublisher(String publisher) {
		this.publisher = new String(publisher);
		this.retrievedMetadata.add("Publisher");
	}
	
	public double getSessionScore() {
		return this.sessionScore;
	}
	public void setSessionScore(double s) {
		this.sessionScore  = s;
	}

	public void incrementFrequencyReturned() {
		this.frequencyReturned++;
	}

	public void addQueryContext(QueryContext queryContext) {
		this.queryContexts.addElement(queryContext);
		if (this.frequencyReturned == 1)
			this.sessionScore = Double.parseDouble(queryContext.getScore());
	}

	public Vector<String> getAuthors() {
		return authors;
	}
	
	public Vector<String> getAuthor() {
		return getAuthors();
	}

	public Vector<String> getKeywords() {
		return keywords;
	}
	
	public Vector<String> getKeyword() {
		return getKeywords();
	}

	public String getTitle() {
		return title;
	}

	public String getDate() {
		return date;
	}

	public String getBooktitle() {
		return booktitle;
	}

	public String getPages() {
		return pages;
	}

	public String getJournal() {
		return journal;
	}

	public String getVolume() {
		return volume;
	}

	public String getNumber() {
		return number;
	}

	public String getAbstract() {
		return docabstract;
	}

	public String getEditor() {
		return editor;
	}

	public String getPublisher() {
		return publisher;
	}

	public int getFrequencyReturned() {
		return frequencyReturned;
	}
	
	public Vector<QueryContext> getQueryContexts() {
		return this.queryContexts;
	}

	public boolean metadataExists(String metadataTag) {
		return this.retrievedMetadata.contains(metadataTag);
	}
	

	protected String removeHTML(String s){
		return s.replaceAll("&quot;", "\"");
	}

}
