package org.apollo.meldex;

import java.io.Serializable;


/**
 * This class represents a single event in a monophonic melody: a pitch-duration pair. This
 * class is serializable since it must be able to be written to the matching query collection
 * files.
 */
public class MelodyEvent implements Serializable
{

	private static final long serialVersionUID = -2160853951522226811L;
	
	/** Melody event type: this event is a rest (has no pitch). */
    public static final int REST_EVENT = -128;
    /** Melody event type: this event is a wild (matches any other melody event). */
    public static final int WILD_EVENT =  128;

    /** The pitch of this melody event, or the event type if it is not a note. */
    private int pitch;
    /** The duration of this melody event. */
    private float duration;


    /**
     * Creates a new melody event.
     *
     * @param eventPitch The pitch of the melody event, or the event type if it is not a note.
     * @param eventDuration The duration of the melody event.
     */
    public MelodyEvent(int eventPitch, float eventDuration)
    {
	pitch = eventPitch;
	duration = eventDuration;
    }


    /**
     * Returns the pitch of the melody event, or the event type if it is not a note.
     *
     * @return the pitch of the melody event, or the event type if it is not a note.
     */
    public int getPitch()
    {
	return pitch;
    }


    /**
     * Returns the duration of the melody event.
     *
     * @return the duration of the melody event.
     */
    public float getDuration()
    {
	return duration;
    }
}
