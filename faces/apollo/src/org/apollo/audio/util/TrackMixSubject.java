package org.apollo.audio.util;

import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.SubjectChangedEvent;

/**
 * A TrackMixSubject is basic in Apollo: where a track mix consists
 * of a volume and a mute flag.
 * 
 * Mixes have immutable IDs (represented as strings) to identify them...
 * 
 * @author Brook Novak
 *
 */
public class TrackMixSubject extends AbstractSubject {
	
	private String channelID;
	
	private float volume;
	
	private boolean isMuted;

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		Must not be null / empty.
	 * 
	 * @param volume
	 * 		The initial volume.
	 * 
	 * @param isMuted
	 * 		The initial mute flag.
	 * 
	 * @throws NullPointerException
	 * 		If id is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If id is empty.
	 */
	TrackMixSubject(String id, float volume, boolean isMuted) {
		if (id == null) throw new NullPointerException("id");
		if (id.length() == 0) throw new IllegalArgumentException("id is empty");
		this.channelID = id;
		this.volume = volume;
		this.isMuted = isMuted;
	}

	/**
	 * @return True if this clip is muted. false otherwise.
	 */
	public boolean isMuted() {
		return isMuted;
	}

	/**
	 * Mutes / un-mutes this clip. Can be called even if not loaded.
	 * 
	 * Raises an ApolloSubjectChangedEvent.MUTE event.
	 * Null is given as the value if the clip does not support mute. Otherwise
	 * true/false is given as the value of the mixed mute.
	 * The event is only raised if the mute is different ot the current mute.
	 * 
	 * @param isMuted True to mute. False to unmute.
	 * 
	 */
	public void setMuted(boolean isMuted) {
		
		if (this.isMuted == isMuted) return;
		this.isMuted = isMuted;

		fireSubjectChanged(new SubjectChangedEvent(
				ApolloSubjectChangedEvent.MUTE));
	}


	/**
	 * @return The volume for this clip.
	 */
	public float getVolume() {
		return volume;
	}
	
	/**
	 * Sets the volume for this clip. Can be called even if not loaded.
	 * 
	 * raises a {@link ApolloSubjectChangedEvent.VOLUME} event.
	 * 
	 * Null is given as the value if the clip does not support master gain.
	 * The event is only raised if the volume is different ot the current volume.
	 * 
	 * Note that this will not actually set the volume of any track sequences, to update
	 * the volume you must subsequently call {@link #updateVolume(Object)}. This is to
	 * avoid setting volumes over all track sequences.
	 * 
	 * @param vol 
	 * 		The volume to set. Ranges from 0-1. Clamped.
	 * 
	 */
	public void setVolume(float vol) {

		// Clamp volume argument
		if (vol < 0.0f)
			vol = 0.0f;
		else if (vol > 1.0f)
			vol = 1.0f;

		if (vol == this.volume)
			return;
		
		volume = vol;

		fireSubjectChanged(new SubjectChangedEvent(
				ApolloSubjectChangedEvent.VOLUME));
	}
	
	/**
	 * The unique ID associated with this track.
	 * 
	 * @return
	 * 		The immutable ID. Never null.
	 */
	public String getChannelID() {
		return channelID;
	}

	/**
	 * A {@link #TrackMixSubject} is equal to another if they have the same ID.
	 */
	@Override
	public boolean equals(Object obj) {
		return channelID.equals(obj);
	}

	@Override
	public int hashCode() {
		return channelID.hashCode();
	}
	
	private static final char SEPERATOR = 7423; // something obscure

	public String toParseableString() {
		return toParseableString(channelID, volume, isMuted);
	}

	static String toParseableString(String id, float volume, boolean isMuted) {
		return id + SEPERATOR + volume + SEPERATOR + ((isMuted) ? "1" : "0");
	}

	public static TrackMixSubject fromString(String st) {
		
		if (st == null) return null;
		
		String[] parts = st.split(Character.toString(SEPERATOR));
		
		if (parts.length != 3) return null;
		
		if (parts[0].length() == 0) return null;
		
		float vol;
		boolean isMuted;
		
		try {
			vol = Float.parseFloat(parts[1]);
		} catch (NumberFormatException e) {
			return null;
		}
		
		isMuted = parts[2].length() > 0 && parts[2].charAt(0) == '1';
		
		return new TrackMixSubject(parts[0], vol, isMuted);
		
	}


}
