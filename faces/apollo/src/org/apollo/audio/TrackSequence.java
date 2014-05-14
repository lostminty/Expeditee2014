package org.apollo.audio;

import javax.sound.sampled.AudioFormat;

import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.SubjectChangedEvent;

/**
 * 
 * A TrackSequence is a track which can reside in a track graph... for playing back at 
 * a scheduled time and mixing with other tracks in the graph.
 * 
 * TrackSequence's are immutable (outside package). They can only be played once.
 * 
 * @author Brook Novak
 *
 */
public final class TrackSequence extends AbstractSubject implements Comparable {

	/** FRIENDLY: ONLY TO BE ACCESSED BY AUDIO MIXER. */
	int currentFrame; // buffer position. Not live.
	
	private int startFrame; // within audio
	
	/** The frame at which this track sequence was suspended at (in the audio mixers live timeline) */
	private int suspendedFrame;
	
	/** The frame at which this track began at (in the audio mixers live timeline) */
	long commencedFrame;
	
	/** FRIENDLY: ONLY TO BE ACCESSED BY AUDIO MIXER. */
	int endFrame; // inclusive
	
	private int relativeInitiationFrame; 
	
	/** FRIENDLY: ONLY TO BE ACCESSED BY AUDIO MIXER. */
	long initiationFrame; // used in pipeline. Absolute. CAn be negitive
	
	/** FRIENDLY: ONLY TO BE ACCESSED BY AUDIO MIXER. */
	boolean isBigEndian;
	
	// Not copied, Arrays are immutable (although contents are not!).
	/** FRIENDLY: ONLY TO BE ACCESSED BY AUDIO MIXER. */
	byte[] playbackAudioBytes; // snapshot of track bytes, avoid editing of model audio bytes while playing
	
	private boolean isPlaying;
	
	float volume; // friendly - for fast access in audio mixing pipeline, ... and carefully by the track model
	
	boolean isMuted; // friendly - for fast access in audio mixing pipeline ... and carefully by the track model
	
	boolean isSolo; // friendly - for fast access in audio mixing pipeline ... 
	
	private Object invoker;
	
	/** FRIENDLY: ONLY TO BE ACCESSED BY AUDIO MIXER. */
	boolean stopPending;
	
	/**
	 * Prepares track sequence for playing. Must not be in a playing state.
	 * 
	 * @param playbackAudioBytes
	 * 		The audio bytes to play.
	 * 
	 * @param format
	 * 		The format of the audio bytes.
	 * 
	 * @param startFrame
	 * 		The frame <i>within the track</i> where to start playback. Inclusive.
	 * 
	 * @param endFrame
	 * 		The frame <i>within the track</i> where to end playback. Inclusive.
	 * 
	 * @param relativeInitiationFrame
	 * 		Where to initiate the frame. This is relative to where the track should start playing
	 * 		in frames, when queued in the track graph.
	 * 
	 * @param invoker
	 * 		A state object that may be of use. 
	 * 
	 * @throws IllegalArgumentException
	 * 		If startFrame >= endFrame. Or startFrame is not in audio range. or end frame is not in audio range
	 * 
	 * @throws NullPointerException
	 * 		If audioBytes or format is null.
	 * 
	 */
	public TrackSequence(byte[] playbackAudioBytes,
			AudioFormat format,
			int startFrame,
			int endFrame,
			int relativeInitiationFrame,
			Object invoker) {

		if (startFrame >= endFrame)
			throw new IllegalArgumentException("startFrame >= endFrame");

		if (format == null)
			throw new NullPointerException("format");
		
		if (playbackAudioBytes == null)
			throw new NullPointerException("audioBytes");
		
		assert(SampledAudioManager.getInstance().isFormatSupportedForPlayback(format));

		this.startFrame = this.currentFrame = startFrame;
		this.endFrame = endFrame;
		this.relativeInitiationFrame = relativeInitiationFrame;
		this.initiationFrame = -1;
		this.suspendedFrame = -1;
		this.commencedFrame = -1;
		this.isBigEndian = format.isBigEndian();
		this.playbackAudioBytes = playbackAudioBytes;
		this.isPlaying = false;
		this.invoker = invoker;
		this.stopPending = false;
		
		assert (currentFrame >= 0 && currentFrame < (playbackAudioBytes.length / format.getFrameSize()));
		assert (endFrame > currentFrame && endFrame < (playbackAudioBytes.length / format.getFrameSize()));
		
	}

	/**
	 * Prepares track sequence for playing. Must not be in a playing state.
	 * 
	 * @param playbackAudioBytes
	 * 		The audio bytes to play.
	 * 
	 * @param format
	 * 		The format of the audio bytes.
	 * 
	 * @param startFrame
	 * 		The frame <i>within the track</i> where to start playback. Inclusive.
	 * 
	 * @param endFrame
	 * 		The frame <i>within the track</i> where to end playback. Inclusive.
	 * 
	 * @param relativeInitiationFrame
	 * 		Where to initiate the frame. This is relative to where the track should start playing
	 * 		in frames, when queued in the track graph.
	 * 
	 * @throws IllegalArgumentException
	 * 		If startFrame >= endFrame. Or startFrame is not in audio range. or end frame is not in audio range
	 * 
	 * @throws NullPointerException
	 * 		If audioBytes or format is null.
	 * 
	 */
	TrackSequence(
			byte[] playbackAudioBytes,
			AudioFormat format,
			int startFrame,
			int endFrame,
			int relativeInitiationFrame) {
		this(playbackAudioBytes, format, startFrame, endFrame, relativeInitiationFrame, null);
	}
	
	/**
	 * Order ascending by initiation frame
	 */
	public final int compareTo(Object o) {
		TrackSequence other = (TrackSequence)o; // throws class cast exception
	
		int cmp = (new Long(initiationFrame)).compareTo(other.initiationFrame);

		if (cmp == 0) {
			
			return (new Integer(hashCode()).compareTo(other.hashCode()));
		}
		
		return cmp;
	}

	/**
	 * @return
	 * 		Where the track initiates/initiated. This is relative to where the track should start playing
	 * 		in frames, when queued in the track graph.
	 */
	public int getRelativeInitiationFrame() {
		return relativeInitiationFrame;
	}

	/**
	 * @return
	 * 		The frame position of where in the track the bytes have been mixed and buffered - not nessessarily
	 * 		been played back. This is not its live playback position.
	 */
	public int getCurrentFrame() {
		return currentFrame;
	}

	/**
	 * @return
	 * 		The last rendered frame when this was stopped.
	 * 		Negitive if not played and stopped yet.
	 */
	public int getSuspendedFrame() {
		return suspendedFrame;
	}

	/**
	 * FRIENDLY: ONLY TO BE ACCESSED BY AUDIO MIXER.
	 * Fires playback started event - later on swing thread.
	 */
	final void onInitiated(long commencedFrame) {
		this.commencedFrame = commencedFrame;
		isPlaying = true;
		fireSubjectChangedLaterOnSwingThread(
				new SubjectChangedEvent(
						ApolloSubjectChangedEvent.PLAYBACK_STARTED));
	}
	
	/**
	 * FRIENDLY: ONLY TO BE ACCESSED BY AUDIO MIXER.
	 * Fires playback stopped event - later on swing thread.
	 * 
	 * @param suspendedFramePosition
	 * 		Must be in range of start frame and end frame (inclusive)
	 */
	final void onStopped(int suspendedFramePosition) {
		isPlaying = false;
		
		assert(suspendedFramePosition >= startFrame);
		assert(suspendedFramePosition <= endFrame);
		
		this.suspendedFrame = suspendedFramePosition;
		
		// Dispose of audio bytes
		playbackAudioBytes = null;
		
		fireSubjectChangedLaterOnSwingThread(
				new SubjectChangedEvent(
						ApolloSubjectChangedEvent.PLAYBACK_STOPPED));
		
	}
	
	/**
	 * @return 
	 * 			true if the track is playing
	 */
	public boolean isPlaying() {
		return isPlaying;
	}
	
	/**
	 * 
	 * @return
	 * 			True if this sequence has been played before.
	 * 
	 */
	public boolean hasFinished() {
		return (playbackAudioBytes == null);
	}

	/**
	 * @return
	 * 		The object which invoked this track sequence. Can be null.
	 * 		Only used externally for 3rd party benifits.
	 */
	public Object getInvoker() {
		return invoker;
	}
	
	/**
	 * Note that the track won't solo unless the mixer is in solo mode.
	 * 
	 * @param isSolo
	 * 		True to set this track as a solo subject.
	 */
	public void setSoloFlag(boolean isSolo) {
		this.isSolo = isSolo;
	}

	public boolean isMuted() {
		return isMuted;
	}

	public void setMuted(boolean isMuted) {
		this.isMuted = isMuted;
	}

	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		// Clamp volume argument
		if (volume < 0.0f) volume = 0.0f;
		else if (volume > 1.0f) volume = 1.0f;
		this.volume = volume;
	}

	/**
	 * @return
	 * 		The absolute frame when this track is to / was played in the software mixers timeline.
	 */
	public long getInitiationFrame() {
		return initiationFrame;
	}
	
	/**
	 * @return
	 * 		The frame at which this track began at (in the audio mixers live timeline)
	 */
	public long getCommencedFrame() {
		return commencedFrame;
	}

	/**
	 * 
	 * @return
	 * 		The start frame <i>within the track</i> at which this track sequence starts / started
	 */
	public int getStartFrame() {
		return startFrame;
	}

	public int getEndFrame() {
		return endFrame;
	}
	
	
	
	
	

}
