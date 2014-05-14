package org.apollo.audio;

/**
 * Provides Apollos IDs for subject changed events
 * 
 * @author Brook Novak
 *
 */
public final class ApolloSubjectChangedEvent {
	
	private ApolloSubjectChangedEvent() {}

	public static final int INPUT_MIXER = 1;
	
	public static final int VOLUME = 2;
	
	public static final int MUTE = 3;
	
	public static final int SOLO_PREFIX_CHANGED = 4;

	/** Raised from swing thread. */
	public static final int PLAYBACK_STARTED = 9;
	
	/** Raised from swing thread. */
	public static final int PLAYBACK_STOPPED = 10;
	
	/** Raised from swing thread. */
	public static final int CAPTURE_STARTED = 11;
	
	/** Raised from swing thread. */
	public static final int CAPTURE_STOPPED = 12;
	
	/** The state information is the channel ID */
	public static final int PAUSE_MARK_CHANGED = 12;
	
	public static final int SELECTION_CHANGED = 100;
	
	/** In this case, the selection will have changed also. */
	public static final int AUDIO_REMOVED = 101; 
	
	
	public static final int AUDIO_INSERTED = 110;
	
	/** For named models - when the name has changed. */
	public static final int NAME_CHANGED = 115;
	
	/** The added TrackGraphInfo is passed as the state info. Tis can be used
	 * if the initation time of a track was updated.  */
	public static final int GRAPH_TRACK_ADDED = 120;
	
	/** The removed TrackGraphNode is passed as the state info. */
	public static final int GRAPH_TRACK_REMOVED = 121;
	
	/** The edit TrackGraphInfo is passed as the state info. */
	public static final int GRAPH_TRACK_EDITED = 122;
	
	/** The added linked tracks virtual filename is passed. */
	public static final int GRAPH_LINKED_TRACK_ADDED = 125;
	
	/** The added linked tracks virtual filename is passed. */
	public static final int GRAPH_LINKED_TRACK_REMOVED = 126;
	
	/** In terms of Y-Pixel OR Initiation time (implicit X pixel)*/
	public static final int GRAPH_TRACK_POSITION_CHANGED = 127;
	
	/** In terms of Y-Pixel OR Initiation time (implicit X pixel)*/
	public static final int GRAPH_LINKED_TRACK_POSITION_CHANGED = 128;
	
	/** The channel ID of the track sequence is passed as state info. */
	public static final int TRACK_SEQUENCE_CREATED = 150;
	
	public static final int MULTIPLAYBACK_LOADING = 151;
	
	public static final int LOAD_STATUS_REPORT = 200;
	
	public static final int RENDER_TASK_INVALIDATION_RECOMENDATION = 300;
	
	public static final int METRONOME_BEATS_PER_MEASURE_CHANGED = 600;
	public static final int METRONOME_TEMPO_CHANGED = 601;
	public static final int METRONOME_STARTED = 602;
	public static final int METRONOME_STOPPED = 603;
	public static final int METRONOME_ENABLED_CHANGED = 604;
}
