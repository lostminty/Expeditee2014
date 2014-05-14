package org.expeditee.greenstone;

import java.util.Vector;
import java.util.Enumeration;

public class QueryOutcome
{
        private String docsMatched;
        private String docsReturned;

	private Vector<Result> results;

        public QueryOutcome()
        {
		docsMatched = "0";
		docsReturned = "0";
		results = new Vector<Result>();
        }

	public String toString() {
		String dump = this.docsMatched + " documents matched the query \n";
		dump = dump + this.docsReturned + " documents were returned\n";


        	for (Enumeration e = this.results.elements(); e.hasMoreElements();) {
                	Result result = (Result) e.nextElement();
			dump = dump + result.toString();
		}
		return dump;
	}

	public void setHowManyDocsMatched(String docsMatched) {
		this.docsMatched = new String(docsMatched);
	}

	public void setHowManyDocsReturned(String docsReturned) {
		this.docsReturned = new String(docsReturned);
	}

	public void addResult(String docID, int rank, String score) {
		this.results.addElement(new Result(docID, rank, score));
	}



        public String getHowManyDocsMatched() {
                return this.docsMatched;
        }

        public String getHowManyDocsReturned() {
                return this.docsReturned;
        }

	public Vector<Result> getResults() {
		return this.results;
	}
}
