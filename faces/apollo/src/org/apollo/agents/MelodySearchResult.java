package org.apollo.agents;


/**
 * An immutable search result for melody searches.
 * 
 * @author Brook Novak
 *
 */
public class MelodySearchResult implements Comparable<MelodySearchResult> {

	private String parentFrame;
	private float score;
	private String trackName ;
	private String trackLocalFileName;
	
	public MelodySearchResult(String parentFrame, float score, String trackName, String trackLocalFileName) {
		assert(parentFrame != null);
		assert(trackLocalFileName != null);
		assert(score >= 0);
		
		this.parentFrame = parentFrame;
		this.score = score;
		this.trackName = trackName;
		this.trackLocalFileName = trackLocalFileName;
	}

	public String getParentFrame() {
		return parentFrame;
	}

	public float getScore() {
		return score;
	}

	public String getTrackLocalFileName() {
		return trackLocalFileName;
	}

	/**
	 * Can be null.
	 * @return
	 */
	public String getTrackName() {
		return trackName;
	}

	public int compareTo(MelodySearchResult o) {
		return Float.compare(score, o.score);
	}

	/*public int compareTo(Object o) {
		
		if (o != null && o instanceof MelodySearchResult) {
			return Float.compare(score, ((MelodySearchResult)o).score);
		}
		throw new ClassCastException();
	}*/
	
	
	
	
	
	
}

