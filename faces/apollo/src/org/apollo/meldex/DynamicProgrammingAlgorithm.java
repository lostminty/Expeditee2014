package org.apollo.meldex;

/**
 * This abstract class implements all of the mechanics involved in performing a match using an
 * algorithm involving dynamic programming. Subclasses need to only implement a weighting
 * function for comparing two melody events. Subclasses can also easily implement special
 * functionality above the replacement/deletion/insertion that is standard.
 *
 * !! Can do matching at start better?? !!
 */

public abstract class DynamicProgrammingAlgorithm 
{
    public static final int MATCH_ANYWHERE = 1;
    public static final int MATCH_AT_START = 2;

    protected static final boolean debugOutput = true;


    public DynamicProgrammingAlgorithm()
    {
    }


    /**
     * Matches a melody against the query pattern and returns a distance measure.
     *
     * @param P The query pattern 
     * @param T The melody to match against the query pattern.
     * @param matchingMode How to match against the query pattern (start/anywhere etc.)
     *
     * @return A distance measure signifying the distance between the query and match patterns.
     */
    public int matchToPattern(Melody P, Melody T, int matchingMode)
    {
	// int maxk = options.getIntegerOptionValue("approximation");
	int maxk = Integer.MAX_VALUE;

	int m = P.getLength();
	int n = T.getLength();

	// For the comparison to be meaningful the pitch types must be the same
	if (P.getPitchType() != T.getPitchType())
	    throw new Error("Internal Error: Pitch type of query and match differs.");

	// Check whether the algorithm has some special functionality
	boolean hasSpecialFunctionality = hasSpecialFunctionality();

	// Allocate distance matrix
	int[][] D = new int[n + 1][m + 1];
	D[0][0] = 0;

	// Initialise top row of matrix
	if (matchingMode == MATCH_AT_START) {
	    // Match at start: have to pay to start further into the pattern
	    for (int i = 1; i <= n; i++)
		D[i][0] = D[i-1][0] + weightFunction(null, T.getEvent(i-1));
	}
	if (matchingMode == MATCH_ANYWHERE) {
	    // Match anywhere: free to start at any point in the pattern
	    for (int i = 1; i <= n; i++)
		D[i][0] = 0;
	}

	// Initialise left column of matrix
	for (int j = 1; j <= m; j++)
	    D[0][j] = D[0][j-1] + weightFunction(P.getEvent(j-1), null);

	// Fill the distance matrix
	for (int j = 1; j <= m; j++) {
	    MelodyEvent queryEvent = P.getEvent(j-1);
	    for (int i = 1; i <= n; i++) {
		MelodyEvent matchEvent = T.getEvent(i-1);

		// Case 1: The two events are the same or different (replacement)
		D[i][j] = D[i-1][j-1] + weightFunction(queryEvent, matchEvent);

		// Case 2: The two events are different (deletion)
		D[i][j] = Math.min(D[i][j], (D[i-1][j] + weightFunction(null, matchEvent)));

		// Case 3: The two events are different (insertion)
		D[i][j] = Math.min(D[i][j], (D[i][j-1] + weightFunction(queryEvent, null)));

		// If the algorithm has special functionality, give it a chance now
		if (hasSpecialFunctionality)
		    doAlgorithmSpecifics(D, i, j, P, T, queryEvent, matchEvent);
	    }
	}

	// Display the distance matrix
	if (debugOutput)
	     displayMatrix(D, m, P, n, T);

	// Find and return the minimum edit distance
	int minDist = Integer.MAX_VALUE;
	for (int i = 0; i <= n; i++) {  // Changed from i = 1 to i = 0 for McNab compatibility
	    if (D[i][m] < minDist)
		minDist = D[i][m];
	}

	// Return -1 if the minimum distance is greater than the approximation value
	return ((minDist > maxk) ? -1 : minDist);
    }


    /**
     * Returns a distance measure signifying the distance between the query and match events.
     * This must be implemented by concrete subclasses.
     *
     * @param queryEvent The event to compare from the query pattern.
     * @param matchEvent The event to compare from the match pattern.
     *
     * @return a distance measure signifying the distance between the query and match events.
     */
    protected abstract int weightFunction(MelodyEvent queryEvent, MelodyEvent matchEvent);


    /**
     * Returns whether the algorithm has special functionality or not.
     * This should be overridden by concrete subclasses that have special functionality.
     *
     * @return true if the algorithm has special functionality, false otherwise.
     */
    protected boolean hasSpecialFunctionality()
    {
	return false;
    }


    /**
     * Gives an algorithm a chance to perform any special functionality that it has.
     * This should be overridden by concrete subclasses that have special functionality.
     *
     * @param D The distance matrix at the current point in time.
     * @param i The current x position in the distance matrix.
     * @param j The current y position in the distance matrix.
     * @param T The melody that is being matched to the query pattern.
     * @param queryEvent The melody event at position j-1 in the query pattern.
     * @param matchEvent The melody event at position i-1 in the match pattern.
     */
    protected void doAlgorithmSpecifics(int[][] D, int i, int j, Melody P, Melody T,
					MelodyEvent queryEvent, MelodyEvent matchEvent)
    {
	// Nothing to do
    }


    /**
     * Displays a completed distance matrix.
     *
     * @param matrix The distance matrix to display.
     * @param P The query pattern (displayed down the left).
     * @param T The match pattern (displayed along the top).
     */
    private void displayMatrix(int[][] matrix, int m, Melody P, int n, Melody T)
    {
	int width = 5;

	// Display the top line (match pattern)
	System.out.print(rightJustifyString("", width));
	System.out.print("  ");
	System.out.print(rightJustifyString("", width));
	for (int i = 1; i <= n; i++) {
	    MelodyEvent matchEvent = T.getEvent(i-1);
	    int matchPitch = matchEvent.getPitch();
	    String pitchString;
	    if (matchPitch == MelodyEvent.REST_EVENT)
		pitchString = "REST";
	    else if (matchPitch == MelodyEvent.WILD_EVENT)
		pitchString = "WILD";
	    else
		pitchString = String.valueOf(matchPitch);

	    System.out.print(rightJustifyString(pitchString, width));
	}
	System.out.print("\n");

	// Display the second line
	System.out.print(rightJustifyString("", width));
	System.out.print(" -");
	for (int i = 0; i < ((n+1) * width); i++)
	    System.out.print("-");
	System.out.print("\n");

	// Display the remaining lines
	for (int j = 0; j <= m; j++) {
	    if (j == 0)
		System.out.print(rightJustifyString("", width));
	    else {
		MelodyEvent queryEvent = P.getEvent(j-1);
		int queryPitch = queryEvent.getPitch();
		String pitchString;
		if (queryPitch == MelodyEvent.REST_EVENT)
		    pitchString = "REST";
		else if (queryPitch == MelodyEvent.WILD_EVENT)
		    pitchString = "WILD";
		else
		    pitchString = String.valueOf(queryPitch);

		System.out.print(rightJustifyString(pitchString, width));
	    }
	    System.out.print(" |");

	    for (int i = 0; i <= n; i++)
		System.out.print(rightJustifyString(String.valueOf(matrix[i][j]), width));
	    System.out.print("\n");
	}
    }


    /**
     * Right-justifies a string by adding spaces before it.
     *
     * @param source The string to right-justify.
     * @param width The number of characters that the right-justified string will take up.
     *
     * @return The right-justified string, or the original string if the width is too small.
     */
    private String rightJustifyString(String source, int width)
    {
	String result = source;

	// Add width - source.length spaces to the start of the string
	for (int i = 0; i < (width - source.length()); i++)
	    result = " " + result;

	return result;
    }
}
