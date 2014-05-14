package org.apollo.meldex;


/**
 * This class represents a monophonic melody: an ordered list of melody events. This class is
 * basically just a wrapper around an array of MelodyEvents (an array is used instead of a
 * Vector for efficiency reasons). This class is serializable since it must be able to be
 * written to the matching query collection files.
 */

public class Melody extends StandardisedMelody
{

	private static final long serialVersionUID = -8903591998036012467L;
	
	/** Pitch type: absolute pitch. */
    public static final int ABSOLUTE_PITCH = 1;
    /** Pitch type: relative pitch (interval/contour). */
    public static final int RELATIVE_PITCH = 2;

    /** The pitch type (absolute/relative) of this melody. */
    private int pitchType;


    /**
     * Creates a new Melody with the specified pitch type and the given melody events.
     *
     * @param pitchType The pitch type (absolute/relative) of the melody.
     * @param events The melody events that form the melody.
     */
    public Melody(int pitchType, MelodyEvent[] events)
    {
	super(events);
	this.pitchType = pitchType;
    }


    /**
     * Returns the pitch type (absolute/relative) of this melody.
     *
     * @return the pitch type (absolute/relative) of this melody.
     */
    public int getPitchType()
    {
	return pitchType;
    }
}
