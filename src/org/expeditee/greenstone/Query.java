package org.expeditee.greenstone;


public class Query implements Cloneable
{
        private String queryText;
        private String stemming;
        private String casefolding;
        private String maxDocsToReturn;
        private String sortBy;
        private String matchMode;
        private String index;
        private String firstDoc;
        private String lastDoc;

	private QueryOutcome queryOutcome;

        public Query()
        {
                queryText = "the";
                stemming = "0";
                casefolding = "1";
                maxDocsToReturn = "5";
                sortBy = "1";
                matchMode = "all";
                index = "TX";
		firstDoc = "0";
		lastDoc = "0";

		queryOutcome = new QueryOutcome();
        }

        public Query clone() {
            try {
                return (Query)super.clone();
            }
            catch (CloneNotSupportedException e) {
                throw new InternalError(e.toString());
            }
        }

	public String toString() {
		String dump = "\nqueryText\t" + this.queryText + "\nstemming\t" + stemming + "\ncasefolding\t" + casefolding + "\nmaxDocs\t\t" + maxDocsToReturn;
		dump = dump + "\nsortBy\t\t" + sortBy + "\nmatchMode\t" + matchMode + "\nindex\t\t" + index;
		dump = dump + "\nfirstDoc\t" + firstDoc + "\nlastDoc\t\t" + lastDoc + "\n";

		return dump + queryOutcome.toString();
	}

	public void setQueryText(String queryText) {
		this.queryText = new String(queryText);
	}

	public void setStemming(String stemming) {
		this.stemming = new String(stemming);
	}

	public void setCasefolding(String casefolding) {
		this.casefolding = new String(casefolding);
	}

	public void setMaxDocsToReturn(String maxDocsToReturn) {
		this.maxDocsToReturn = new String(maxDocsToReturn);
	}
/*
	public void setSortBy(String sortBy) {
		this.sortBy = new String(sortBy);
	}
*/

	public void setMatchMode(String matchMode) {
		this.matchMode = new String(matchMode);
	}

	public void setIndex(String index) {
		this.index = new String(index);
	}

	public void setFirstDoc(String firstDoc) {
		this.firstDoc = new String(firstDoc);
	}

	public void setLastDoc(String lastDoc) {
		this.lastDoc = new String(lastDoc);
	}


	public void addQueryOutcome(QueryOutcome queryOutcome) {
		this.queryOutcome = queryOutcome;
	}




        public String getQueryText() {
                return this.queryText;
        }

        public String getStemming() {
                return this.stemming;
        }

        public String getCasefolding() {
                return this.casefolding;
        }

        public String getMaxDocsToReturn() {
                return this.maxDocsToReturn;
        }

        public String getSortBy() {
                return this.sortBy;
        }

        public String getMatchMode() {
                return this.matchMode;
        }

        public String getIndex() {
                return this.index;
        }

	public String getFirstDoc() {
		return this.firstDoc;
	}

	public String getLastDoc() {
		return this.lastDoc;
	}


	public QueryOutcome getQueryOutcome() {
		return this.queryOutcome;
	}
}
