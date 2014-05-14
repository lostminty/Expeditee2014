package org.expeditee.greenstone;

public class QueryContext
{
        private int rank;
        private String score;
        private Query query;

        public QueryContext(int rank, String score, Query query)
        {
		this.rank = rank;
		this.score = score;
		this.query = query;
        }

	public String toString() {
		String dump = "\n\nQueryContext..\n";
		dump = dump + "rank\t\t" + rank + "\n";
		dump = dump + "score\t\t" + score ;
		dump = dump + query.toString();

		return dump;
	}

	public int getRank() {
		return this.rank;
	}

	public String getScore() {
		return this.score;
	}

	public Query getQuery() {
		return this.query;
	}
}
