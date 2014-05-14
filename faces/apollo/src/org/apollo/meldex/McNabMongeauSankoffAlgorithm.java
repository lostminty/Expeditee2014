package org.apollo.meldex;

/**
 * An implementation of an approximate string matching algorithm described by Marcel Mongeau
 * and David Sankoff ("Comparison of Musical Sequences", Computers and the Humanities, Vol 24
 * (1990), p161 - 175) and modified by Rodger McNab for music matching (REF).
 * <p>To do: Test fragmentation and consolidation.
 */
public class McNabMongeauSankoffAlgorithm extends DynamicProgrammingAlgorithm
{
    private static final boolean doFragmentationConsolidation = false;

    /** Constant values used by this algorithm */
    private final double K1_FACTOR = 112.0;


    /**
     * Creates a new McNabMongeauSankoffAlgorithm and initialises it for use.
     */
    public McNabMongeauSankoffAlgorithm()
    {
    }


    /**
     * Returns whether this algorithm has special functionality that is enabled.
     *
     * @return whether this algorithm has special functionality that is enabled.
     */
    protected boolean hasSpecialFunctionality()
    {
	return 	doFragmentationConsolidation;
    }


    /**
     * Lets the algorithm perform its special functionality (fragmentation and consolidation).
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
	if (doFragmentationConsolidation) {
	    int pitchType = T.getPitchType();

	    // Do fragmentations (one query event matches many match events)
	    int fragPitch = matchEvent.getPitch();
	    double queryDuration = queryEvent.getDuration();
	    double totalDuration = matchEvent.getDuration();

	    // Subsequent pitches must be the same
	    int requiredPitch = 0;
	    if (pitchType == Melody.ABSOLUTE_PITCH)
		requiredPitch = fragPitch;

	    // Sum the fragmented weights of the previous k match events
	    int k = 2;
	    while ((k <= i) && (fragPitch == requiredPitch)) {
		// Total fragmentation length should not exceed query event length
		if (totalDuration > queryDuration)
		    break;

		// All fragments must have the same pitch
		MelodyEvent fragEvent = T.getEvent(i-k);
		fragPitch = fragEvent.getPitch();
		if (pitchType == Melody.ABSOLUTE_PITCH && fragPitch != requiredPitch)
		    break;

		// Calculate the new pitch and duration cost for this fragmentation
		double wPitch = weightPitch(queryEvent.getPitch(), fragPitch);
		totalDuration += fragEvent.getDuration();
		double wDuration = weightDuration(queryDuration, totalDuration);
		double shortestDuration = (queryDuration < totalDuration ? queryDuration : totalDuration);

		// Calculate the total cost for this fragmentation
		int fragWeight = (int) ((wPitch * shortestDuration) + (K1_FACTOR * wDuration));

		if (debugOutput) {
		    System.out.println("Frag: k = " + k + "\tDi-k,j-1 = " + D[i-k][j-1] + "\tPitch weight: " + wPitch + "\tDuration weight: " + wDuration + "\tTotal weight: " + fragWeight);
		}

		// Case 4: Many events are equivalent to one (fragmentation)
		D[i][j] = Math.min(D[i][j], (D[i-k][j-1] + fragWeight));
		k++;
	    }

	    // Do consolidations (one match event matches many query events)
	    int consPitch = queryEvent.getPitch();
	    double matchDuration = matchEvent.getDuration();
	    totalDuration = queryEvent.getDuration();

	    // Subsequent pitches must be the same
	    requiredPitch = 0;
	    if (pitchType == Melody.ABSOLUTE_PITCH)
		requiredPitch = consPitch;

	    // Sum the fragmented weights of the previous k query events
	    k = 2;
	    while ((k <= j) && (consPitch == requiredPitch)) {
		// Total consolidation length should not exceed match event length
		if (totalDuration > matchDuration)
		    break;

		// All fragments must have the same pitch
		MelodyEvent consEvent = P.getEvent(j-k);
		consPitch = consEvent.getPitch();
		if (pitchType == Melody.ABSOLUTE_PITCH && consPitch != requiredPitch)
		    break;

		// Calculate the new pitch and duration cost for this consolidation
		double wPitch = weightPitch(matchEvent.getPitch(), consPitch);
		totalDuration += consEvent.getDuration();
		double wDuration = weightDuration(matchDuration, totalDuration);
		double shortestDuration = (matchDuration < totalDuration ? matchDuration : totalDuration);

		// Calculate the total cost for this consolidation
		int consWeight = (int) ((wPitch * shortestDuration) + (K1_FACTOR * wDuration));

		if (debugOutput) {
		    System.out.println("Cons: k = " + k + "\tDi-1,j-k = " + D[i-1][j-k] + "\tPitch weight: " + wPitch + "\tDuration weight: " + wDuration + "\tTotal weight: " + consWeight);
		}

		// Case 5: Many events are equivalent to one (consolidation)
		D[i][j] = Math.min(D[i][j], (D[i-1][j-k] + consWeight));
		k++;
	    }
	}
    }


    /**
     * Returns a distance measure signifying the distance between the query and match events.
     *
     * @param queryEvent The event to compare from the query pattern.
     * @param matchEvent The event to compare from the match pattern.
     *
     * @return a distance measure signifying the distance between the query and match events.
     */
    protected int weightFunction(MelodyEvent queryEvent, MelodyEvent matchEvent)
    {
	// Calculate the pitch weighting
	int queryPitch = ((queryEvent == null) ? 0 : queryEvent.getPitch());
	int matchPitch = ((matchEvent == null) ? 0 : matchEvent.getPitch());
	double wPitch = weightPitch(queryPitch, matchPitch);

	return (int)wPitch;

	/*

	// Calculate the duration weighting
	double queryLength = ((queryEvent == null) ? 0.0 : queryEvent.getDuration());
	double matchLength = ((matchEvent == null) ? 0.0 : matchEvent.getDuration());
     	double wDuration = weightDuration(queryLength, matchLength);
	double shortestLength = (queryLength < matchLength ? queryLength : matchLength);

	// Calculate the final weighting (linear combination of pitch and duration)
	return (int) ((wPitch * shortestLength) + (K1_FACTOR * wDuration));
	*/

    }


    /**
     * Returns a measure signifying the distance between the query and match pitches.
     *
     * @param queryPitch The pitch of the query event.
     * @param matchPitch The pitch of the match event.
     *
     * @return a measure signifying the distance between the query and match pitches.
     */
    private double weightPitch(int queryPitch, int matchPitch)
    {
	// If either of the events is a wild then the pitch weight is 0
	if ((queryPitch == MelodyEvent.WILD_EVENT) || (matchPitch == MelodyEvent.WILD_EVENT))
	    return 0.0;

	// If one or other of the events is a rest, use a special pitch factor of 80.0
	if ((queryPitch == MelodyEvent.REST_EVENT) ^ (matchPitch == MelodyEvent.REST_EVENT))
	    return 80.0;

	// Otherwise calculate a pitch factor based on the pitch difference
	int pitchDifference = Math.abs((queryPitch - matchPitch));

	// Special case absolute pitch
	/* if (P.getPitchType() == Melody.ABSOLUTE_PITCH) {
	    pitchDifference = pitchDifference % 12;
	    if (pitchDifference > 6)
		pitchDifference = 12 - pitchDifference;
		} */

	return (pitchDifference * 32.0);
    }


    /**
     * Returns a measure signifying the distance between the query and match durations.
     *
     * @param queryLength The duration of the query event.
     * @param matchLength The duration of the match event.
     *
     * @return a measure signifying the distance between the query and match durations.
     */
    private double weightDuration(double queryLength, double matchLength)
    {
	// The duration weight is directly proportional to the distance between the two lengths
	return Math.abs((queryLength - matchLength));
    }
}
