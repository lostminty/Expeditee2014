package org.expeditee.greenstone;

public class Result {
	private String docID;
	private int rank;
	private String score;

	Result(String docID, int rank, String score) {
		this.docID = docID;
		this.rank = rank;
		this.score = score;
	}

	public String toString() {
		String dump = this.rank + "\t" + this.score + "\t" + this.docID + "\n";
		return dump;
	}

	public String getDocID() {
		return this.docID;
	}

	public int getRank() {
		return this.rank;
	}

	public String getScore() {
		return this.score;
	}
}
