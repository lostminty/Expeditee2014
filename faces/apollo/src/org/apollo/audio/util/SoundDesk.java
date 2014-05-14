package org.apollo.audio.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.sound.sampled.LineUnavailableException;

import org.apollo.audio.ApolloPlaybackMixer;
import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.SampledTrackModel;
import org.apollo.audio.TrackSequence;
import org.apollo.audio.structure.LinkedTracksGraphNode;
import org.apollo.audio.structure.OverdubbedFrame;
import org.apollo.audio.structure.TrackGraphNode;
import org.apollo.io.AudioPathManager;
import org.apollo.io.MixIO;
import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.widgets.LinkedTrack;
import org.apollo.widgets.SampledTrack;
import org.expeditee.gui.Frame;

/**
 * A high level tool using the Apollo API providing the following features:
 * 
 * <ul>
 *   <li> Playback / resume of track models
 *   <li> Playback / resume of linked tracks
 *   <li> Pause / Resume control
 *   <li> Solo tracks
 *   <li> Central mix repository
 *   <li> Saving and Loading of mixes
 *   <li> Saving and loading of master mix
 * </ul>
 * 
 * Can think of this as the bridge between the Apollo API and Expeditee widgets.
 * 
 * By Apollo API I mean the {@link org.apollo.audio} package.
 * 
 * TODO: This is a mess and needs to be rethought - possibly completely removed. Originally
 * designed for allowing for many mix options but this is probably to confusing for the user.
 * 
 * @author Brook Novak
 *
 */
public class SoundDesk extends AbstractSubject implements Observer {
	
	/** Channel ID (All types) -> Channel. */
	private HashMap<String, Channel> channels = new HashMap<String, Channel>();
	
	/** TrackSequence indexed Channel Set. */
	private HashMap<TrackSequence, Channel> tseqIndexedChannels = new HashMap<TrackSequence, Channel>();
	
	/** Channel ID (Pure local) -> List of channels indirectly using the channel with channel-ID. */
	private HashMap<String, LinkedList<Channel>> linkedChannels = new HashMap<String, LinkedList<Channel>>();
	
	/** Channel ID (Master) -> List of channels using the master mix. */
	private HashMap<String, LinkedList<Channel>> childChannels = new HashMap<String, LinkedList<Channel>>();
	
	/** A whole branch of mixes can be soloed (even if not created yet) thif the prefix matches
	 *	the solo prefix
	 **/
	private String soloIDPrefix = null;
	
	/** Where the mixes are saved to. */
	private static final String MIX_BANKS_FILEPATH = AudioPathManager.AUDIO_HOME_DIRECTORY + ".banks";
	
	private static final String MASTER_MIX_FILEPATH = AudioPathManager.AUDIO_HOME_DIRECTORY + ".mastermix";
	
	private static final String NO_SOLO_PREFIX = "~#SOLOOFF#~";
	
	private static final char CHANNEL_ID_SEPORATOR = 2357;
	private static final char LOCAL_CHANNEL_ID_TAG = 3421;
	
	/**
	 * Singleton design pattern.
	 */
	private static SoundDesk instance = new SoundDesk();
	public  static SoundDesk getInstance() {
		return instance;
	}
	
	/**
	 * Singleton constructor - loads the mixes from file.
	 */
	private SoundDesk() {
		loadMasterMix();
		loadMixes();
	}
	
	private void loadMasterMix() {
		
		TrackMixSubject mmix = MixIO.loadMasterMix(MASTER_MIX_FILEPATH);
		
		if (mmix != null) {
		
			ApolloPlaybackMixer.getInstance().setMasterMute(mmix.isMuted());
		
			ApolloPlaybackMixer.getInstance().setMasterVolume(mmix.getVolume());
			
			soloIDPrefix = mmix.getChannelID().equals(NO_SOLO_PREFIX) ?
					null : mmix.getChannelID();

		}

	}
	
	private void loadMixes() {

		List<TrackMixSubject> banks = new LinkedList<TrackMixSubject>();
		
		if (MixIO.loadBanks(banks, MIX_BANKS_FILEPATH)) {

			for (int i = 0; i < banks.size(); i++) {
				
				TrackMixSubject mix = banks.get(i);
				assert(mix != null);

				createChannel(mix);
			}
			
		}
	}
	
	public void saveMixes() {
		
		ArrayList<TrackMixSubject> banks = new ArrayList<TrackMixSubject>(channels.size());

		for (Channel chan : channels.values()) {
			banks.add(chan.mix);
		}
		
		MixIO.saveBanks(MIX_BANKS_FILEPATH, banks);
	}
	
	public void saveMasterMix() {
		String id = soloIDPrefix;
		if (id == null) id = NO_SOLO_PREFIX;
		
		MixIO.saveMasterMix(MASTER_MIX_FILEPATH, 
				new TrackMixSubject(id, 
						ApolloPlaybackMixer.getInstance().getMasterVolume(),
						ApolloPlaybackMixer.getInstance().isMasterMuteOn()));
	}
	
	/**
	 * In order to solo a track sequence, use this to solo a group of
	 * track sequences depending of their id starts with the given prefix.
	 * Not that the effect is applied immediatly and the ApolloPlaybackMixer
	 * mode changes to solo-on/off accordingly. 
	 * 
	 * Raises a {@link ApolloSubjectChangedEvent#SOLO_PREFIX_CHANGED} event if changed.
	 * 
	 * @param channelIDPrefix
	 * 		The solo ID. Null for no solo.
	 */
	public void setSoloIDPrefix(String channelIDPrefix) {
		
		if (this.soloIDPrefix == channelIDPrefix) return;
		
		this.soloIDPrefix = channelIDPrefix;
		
		// Set flags
		for (Channel ch : channels.values()) {
			if (ch.tseq != null) {
				if (channelIDPrefix == null) ch.tseq.setSoloFlag(false);
				else ch.tseq.setSoloFlag(ch.mix.getChannelID().startsWith(channelIDPrefix));
			}
		}
		
		// Note, any track sequences create in the future will have their solo flag set
		// according to the current soloIDPrefix.
		
		// Set solo state
		ApolloPlaybackMixer.getInstance().setSoloEnable(channelIDPrefix != null);
		
		// Notify observers
		fireSubjectChanged(new SubjectChangedEvent(
				ApolloSubjectChangedEvent.SOLO_PREFIX_CHANGED));
	}
	
	/**
	 * Determines whether a track id is part of the current solo group.
	 * 
	 * @param channelID
	 * 		Must not be null.
	 * 
	 * @return
	 * 		True if the given id is in the solo group.
	 * 
	 * @throws NullPointerException
	 * 		If mixID is null.
	 */
	public boolean isSolo(String channelID) {
		if (channelID == null) throw new NullPointerException("channelID");
		if (soloIDPrefix == null) return false;
		return channelID.startsWith(soloIDPrefix);
	}
	
	/**
	 * Determines if a specific mix is playing.
	 * 
	 * @param channelID
	 * 		The id to test - must not be null.
	 * 
	 * @return
	 * 		True if a channel with the given ID is playing.
	 * 
	 * @throws NullPointerException
	 * 		If channelID is null.
	 */
	public boolean isPlaying(String channelID) {
		TrackSequence ts = getTrackSequence(channelID);
		return ts != null && ts.isPlaying();
	}
	
	/**
	 * Gets the last frame psoition that a given chanel was played ... that is,
	 * the last frame position <i>within the track sequences bytes</i>. I.e. not
	 * refering to the software mixers timeline.
	 * 
	 * Note that this is specific to the audio bytes in the tracksequence that
	 * was last played with the mix.
	 * 
	 * @param channelID
	 * 		Must not be null.
	 * 
	 * @return
	 * 		The last stopped frame position for the given channel ID.
	 * 		negative if does not exist or has not been played.
	 * 
	 * @throws NullPointerException
	 * 		If channelID is null
	 */
	public int getLastPlayedFramePosition(String channelID) {
		if (channelID == null) throw new NullPointerException("channelID");
		
		Channel chan = channels.get(channelID);
		
		if (chan != null) return chan.stoppedFramePosition;
		return -1;
	}
	
	/**
	 * Checks if a channel has been marked as being paused.
	 * 
	 * A channel can be marked as paused via {@link #setPaused(String, boolean)}.
	 * The mark is reset whenever a channel is next played (or explicitly reset).
	 * 
	 * Using this flag in conjunction with {@link #getLastPlayedFramePosition(String)}
	 * pause/resume functionality can be acheived.
	 * 
	 * @param channelID
	 * 		Must not be null.
	 * 
	 * @return
	 * 		True if the channel exists and is marked as paused.
	 * 
	 * @throws NullPointerException
	 * 		If channelID is null
	 */
	public boolean isPaused(String channelID) {
		if (channelID == null) throw new NullPointerException("channelID");
		
		Channel chan = channels.get(channelID);
		
		if (chan != null) return chan.isPaused;
		return false;
	}
	
	/**
	 * Marks a channel as paused / not-paused.
	 * 
	 * The mark of a channel auto-resets whenever the channel is next played.
	 * 
	 * Raises a {@link ApolloSubjectChangedEvent#PAUSE_MARK_CHANGED} event
	 * if true is returned.
	 * 
	 * @param channelID
	 * 		Must not be null.
	 * 
	 * @param isPaused
	 * 		True to mark as paused. False to reset mark.
	 * 
	 * @return
	 * 		True if the mark was set. False if it wasn't set because the
	 * 		channel did not exist or was already the same.
	 * 
	 * @throws NullPointerException
	 * 		If channelID is null
	 */
	public boolean setPaused(String channelID, boolean isPaused) {
		if (channelID == null) throw new NullPointerException("channelID");
		
		Channel chan = channels.get(channelID);
		
		if (chan != null && chan.isPaused != isPaused) {
			
			chan.isPaused = isPaused;
			
			fireSubjectChanged(new SubjectChangedEvent(
					ApolloSubjectChangedEvent.PAUSE_MARK_CHANGED, channelID));

			return true;
		}
		
		return false;
	}
	
	/**
	 * Gets a track sequence assoicated with the track id.
	 * 
	 * @param channelID
	 * 		Must not be null.
	 * 
	 * @return
	 * 		The track sequence assoicated with the channelID. Null if none exists.
	 * 
	 * @throws NullPointerException
	 * 		If channelID is null.
	 */
	public TrackSequence getTrackSequence(String channelID) {
		if (channelID == null) throw new NullPointerException("channelID");
		Channel chan = channels.get(channelID);
		return (chan == null) ? null : chan.tseq;
	}
	
	/**
	 * 
	 * @param channelID
	 * 		A non-null indirect cahnnel id.
	 * 
	 * @param isUsingLocalMix
	 * 		The flag to set. True to use the local mix settings instead of thwe indrect local
	 * 		mix... otherwise set to false to override the lcal mix settings and use its own.
	 * 
	 * @return
	 * 		True if set. False if cahnnel does not exist.
	 * 
	 * @throws IllegalArgumentException
	 * 		If the channel exists and the type is not an indirect local channel.
	 * 
	 * @throws NullPointerException
	 * 		If channelID is null.
	 */
	public boolean setUseLocalTrackMix(String channelID) {
		if (channelID == null) throw new NullPointerException("channelID");
		Channel chan = channels.get(channelID);
		

		if (chan != null) {

			if (chan.type != ChannelIDType.IndirectLocal)
				throw new IllegalArgumentException("The channel ID \"" + channelID + "\" is no an indirect local ID");
			
			updateMixSettings(chan);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Creates a empty mix: adding it to the mix set as well as observing it.
	 * 
	 * Assumes that the id has not been created yet.
	 * 
	 * @param channelID
	 * 		The id to create the mix with
	 * 
	 * @return
	 * 		The empty mix - never null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If channelID is invalid.
	 */
	private Channel createDefaultChannel(String channelID) {
		return createChannel(channelID, 1.0f, false); // defaults
	}
	
	/**
	 * Creates a mix. Note that this will be saved in the mix batch file.
	 * 
	 * @param channelID
	 *		A unique <i>unused</i> channel ID for the mix. Must not be null. 
	 * 
	 * @param volume
	 * 		The volume for the new mix. Between 0 and 1. Clamped.
	 * 
	 * @param isMuted
	 * 		Whether the mute of the mix is on or off.
	 * 
	 * @param isUsingLocalMix
	 * 		True if the channel using the mix should instead use the local mix .. this only
	 * 		applies to {@link ChannelIDType#IndirectLocal} channels.
	 * 
	 * @return
	 * 		The created mix. Null if already exists.
	 * 
	 * @throws IllegalArgumentException
	 * 		If channelID is invalid.
	 */
	public TrackMixSubject createMix(String channelID, float volume, boolean isMuted) {
		if (channels.containsKey(channelID)) return null;
		return createChannel(channelID, volume, isMuted).mix;
	}
	
	/**
	 * 
	 * @param channelID
	 * 
	 * @param volume
	 * 
	 * @param isMuted
	 * 
	 * @param isUsingLocalMix
	 * 
	 * @return
	 * 
	 * @throws IllegalArgumentException
	 * 		If channelID is invalid.
	 */
	private Channel createChannel(String channelID, float volume, boolean isMuted) {
		assert(!channels.containsKey(channelID));
		return createChannel(new TrackMixSubject(channelID, volume, isMuted));
	}

	/**
	 * @see #createChannel(String, float, boolean)
	 * 
	 * @param mix
	 * 		The mix to create.
	 * 
	 * @return
	 * 		NULL if the mix already exists
	 * 
	 * @throws IllegalArgumentException
	 * 		If the mix's channel ID is invalid.
	 */
	private Channel createChannel(TrackMixSubject mix) {
		assert(mix != null);
		if (channels.containsKey(mix.getChannelID())) return null;

		Channel chan = new Channel(mix); // throws IllegalArgumentException

		channels.put(mix.getChannelID(), chan);
		chan.mix.addObserver(this); // Keep track sequence data synched with mix
		
		// If the channel is an indirect local channel then update the linked track map
		if (chan.type == ChannelIDType.IndirectLocal) {
			
			// Get the local channel ID that this channel points to
			String local = extractLocalChannelID(mix.getChannelID());
			assert(local != null);
			
			LinkedList<Channel> linked = linkedChannels.get(local);
			
			// Add the channel to the linked list ....
			if (linked == null) {
				linked = new LinkedList<Channel>();
				linkedChannels.put(local, linked);
			}
			
			if (!linked.contains(chan)) {
				linked.add(chan);
			}
			
			String parent = extractMasterChannelID(mix.getChannelID());
			assert(parent != null);
			
			LinkedList<Channel> children = childChannels.get(parent);
			if (children != null && !children.contains(chan))
				children.add(chan);
			
		} else if (chan.type == ChannelIDType.Master) {
			
			LinkedList<Channel> children = new LinkedList<Channel>();
			
			for (Channel c : channels.values()) {
				if (c == chan) continue;
				if (c.masterMixID.equals(chan.mix.getChannelID())) {
					assert(c.type == ChannelIDType.IndirectLocal);
					children.add(c);
				}
			}
			
			childChannels.put(chan.mix.getChannelID(), children);
			
		}

		
		return chan;
	}

	/**
	 * Either gets an existing from memory or creates a new mix for the given ID.
	 * If had to create, default values of the mix are chosen (non-muted and full volume)
	 * 
	 * @param channelID
	 * 		The ID of the mix to get / create. Must not be null.
	 * 
	 * @return
	 * 		The retreived/constructed mix. Never null.
	 * 
	 * @throws NullPointerException
	 * 		If id is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If channelID is invalid.
	 */
	public TrackMixSubject getOrCreateMix(String channelID) {
		
		if (channelID == null) throw new NullPointerException("channelID");
		
		TrackMixSubject tmix = getMix(channelID);
		
		if (tmix == null) {
			Channel chan = createDefaultChannel(channelID); // throws illegal arg
			return chan.mix;
			
		} else return tmix;
	}

	/**
	 * Retreives a mix.
	 * 
	 * @param channelID
	 * 		The ID of the mix to get. Can be null.
	 * 
	 * @return
	 * 		The Mix that is assoicated with the given ID.
	 * 		Null if no mix exists OR if id was null.
	 * 
	 */
	public TrackMixSubject getMix(String channelID) {
		if (channelID == null) return null;
		
		Channel chan = channels.get(channelID);
		if (chan != null) return chan.mix;
		return null;
	}
	
	/**
	 * Raises {@link ApolloSubjectChangedEvent#TRACK_SEQUENCE_CREATED} events
	 * for each overdub
	 * 
	 * @param overdubs
	 * 		A non-null list of overdubs. Must not have overdubbs sharing
	 * 		channels...
	 * 
	 * @return
	 * 		The set of track sequences that were created for playing the overdubs.
	 * 		Empty if there were none to play if the given overdub list is empty or
	 * 		if none of them are in playing range.
	 * 
	 * @throws LineUnavailableException
	 * 		If failed to get data line to output device.
	 * 
	 * @throws NullPointerException
	 * 		if overdubs is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		if overdubs contains a null reference.
	 * 		If overdubs channelID is invalid or a master channel
	 * 		If contains two or more overdubs using the same channel.
	 * 		If overdubs startFrame larger or equal to endFrame. 
	 * 		Or overdubs startFrame is not in audio range. 
	 * 		Or overdubs end frame is not in audio range
	 * 
	 * @throws IllegalStateException
	 * 		If a channel with the overdubs channelID is already playing.
	 */
	public Set<TrackSequence> playOverdubs(
			List<Overdub> overdubs) 
			throws LineUnavailableException {
		
		if (overdubs == null) throw new NullPointerException("overdubs");
		
		Set<Channel> toPlay = new HashSet<Channel>(); // used for checking for dup channels
		Set<TrackSequence> tracSeqs = new HashSet<TrackSequence>();
		
		// Anything to play?
		if (overdubs.isEmpty()) 
			return tracSeqs;
		
		boolean succeeded = false;
		
		try {
		
			// Get or create channels from the given IDs.
			for (Overdub od : overdubs) {
				
				if (od == null) 
					throw new IllegalArgumentException("overdubs contains a null reference"); 
				
				Channel chan = channels.get(od.getChannelID()); 

				
				if (chan == null) { // Create
					chan = createDefaultChannel(od.getChannelID()); // Throws IllegalArgumentException
					
				} else if (toPlay.contains(chan)) { // dissallow re-use of channels within a playback group...
					throw new IllegalArgumentException("Duplicate use of channel in multiplayback request");
				}

				// Check that the channel ID is not a master mix:
				if (chan.type == ChannelIDType.Master)
					throw new IllegalArgumentException(
							"Master channels do not directly playback tracks (channelID = " 
							+ od.getChannelID() + ")");
	
				// Check that the target channel is not already playing
				if (chan.tseq != null && chan.tseq.isPlaying())
					throw new IllegalStateException("The channel " + od.getChannelID() + " is already in use");
				
				// Create the (new) track sequence to associate the mix with
				TrackSequence ts = new TrackSequence( // throws exceptions
						od.getTrackModel().getAllAudioBytes(), 
						od.getTrackModel().getFormat(),
						od.getStartFrame(), 
						od.getEndFrame(), 
						od.getRelativeInitiationFrame(),
						od.getTrackModel()); // Important: set as track model so it can be re-used
				
				ts.addObserver(this);

				// Reset pause flag
				chan.isPaused = false;
				chan.tseq = ts;
				
				tseqIndexedChannels.put(ts, chan);
				tracSeqs.add(ts);
				
				// Synch TS with mix
				ts.setSoloFlag(isSolo(od.getChannelID()));
				updateMixSettings(chan);
		
				// Allow listeners to register to the track sequence so that they can
				// catch playback events for the mix
				fireSubjectChanged(new SubjectChangedEvent(
						ApolloSubjectChangedEvent.TRACK_SEQUENCE_CREATED, od.getChannelID()));

			}
			
			
		//	if (!tracSeqs.isEmpty()) {  // if nothing is in range can become empty
				// Commence playback... when its scheduled to
				ApolloPlaybackMixer.getInstance().playSynchronized(tracSeqs);
			//}
			
			succeeded = true;
		
		} finally {
			
			// Did fail in any way? 
			if (!succeeded) {
			
				// Ensure that track sequences are all removed since some may not be able
				// to be erased by observation... (modelchanged)
				for (Channel chan : toPlay) {
					if (chan.tseq != null && 
							!chan.tseq.isPlaying()) { // if is created but has not started
						tseqIndexedChannels.remove(chan.tseq);
						chan.tseq = null;
					}
				}
				
			}
		}

		return tracSeqs;
	}
	
	/**
	 * Raises an {@link ApolloSubjectChangedEvent#TRACK_SEQUENCE_CREATED} event 
	 * just before playback commences. Note that the event can occur but the track sequence
	 * may fail to playback.
	 * 
	 * @param tmodel
	 * 		The track model to play. Must not be null.
	 * 
	 * @param channelID
	 * 		The channel ID to use for the playback. Note that a channel can only be
	 * 		in use one at a time 
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
	 * @throws LineUnavailableException
	 * 		If failed to get data line to output device
	 * 
	 * @throws NullPointerException
	 * 		if channelID or tmodel is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If channelID is empty, or invalid, or a master channel
	 * 		If startFrame larger or equal to endFrame. 
	 * 		Or startFrame is not in audio range. 
	 * 		Or end frame is not in audio range
	 * 
	 * @throws IllegalStateException
	 * 		If the channel with the given channelID is already playing.
	 * 
	 */
	public void playSampledTrackModel(	
			SampledTrackModel tmodel, 
			String channelID,
			int startFrame,
			int endFrame,
			int relativeInitiationFrame) 
			throws LineUnavailableException {
		
		if (tmodel == null) throw new NullPointerException("tmodel");
		if (channelID == null) throw new NullPointerException("channelID");
		if (channelID.length() == 0) throw new IllegalArgumentException("channelID");
		
		// Get or create a channel from the given ID.
		Channel chan = channels.get(channelID);
		
		if (chan == null) { // Create
			chan = createDefaultChannel(channelID); // Throws IllegalArgumentException
		}
		
		// Check that the channel ID is not a master mix:
		if (chan.type == ChannelIDType.Master)
			throw new IllegalArgumentException(
					"Master channels do not directly playback tracks (channelID = " 
					+ channelID + ")");
		
		// Check that the mix is not already playing
		if (chan.tseq != null && chan.tseq.isPlaying())
			throw new IllegalStateException("The channel " + channelID + " is already in use");
		
		// Create the (new) track sequence to associate the mix with
		TrackSequence ts = new TrackSequence(
				tmodel.getAllAudioBytes(), 
				tmodel.getFormat(), 
				startFrame, 
				endFrame, 
				relativeInitiationFrame,
				tmodel); // Important: set as track model so it can be re-used

		boolean succeeded = false;
		
		try {

			// Whenever the track is stopped, then auto-remove the track sequence and nullify
			// the ts reference so that the referenced SampledTrackModel can be collected
			// the the garbage collector
			ts.addObserver(this);
		
			
			// Reset pause flag
			chan.isPaused = false;
			chan.tseq = ts;
			
			tseqIndexedChannels.put(ts, chan);
			
			// Synch TS with mix
			ts.setSoloFlag(isSolo(channelID));
			updateMixSettings(chan);
	
			// Allow listeners to register to the track sequence so that they can
			// catch playback events for the mix
			fireSubjectChanged(new SubjectChangedEvent(
					ApolloSubjectChangedEvent.TRACK_SEQUENCE_CREATED, channelID));
			

			// Commence playback... when its scheduled to
			ApolloPlaybackMixer.getInstance().play(ts);
			succeeded = true;
			
		} finally {
			if (!succeeded) {
				chan.tseq = null;
				tseqIndexedChannels.remove(ts);
			}
		}

	}
	
	/**
	 * Ensures that a channels mix settings are synched as well as all other
	 * channels that depend on it... (e.g. a pure local channel updating its indirect tracks).
	 * 
	 * Omittedly, this is way to over the top than it needs to be :(
	 * 
	 * @param chan
	 * 			Must not be null.
	 */
	private void updateMixSettings(Channel chan) {
		assert(chan != null);
		
		// Update track sequence playing on the channel at the moment ..
		// Only if this track mix is to be used for the track sequence
		if (chan.tseq != null && 
				chan.type != ChannelIDType.Master) {
			
			boolean isMuted;
			float volume;

			if (chan.type == ChannelIDType.IndirectLocal) {

				String localChannelID = extractLocalChannelID(chan.mix.getChannelID());
				assert(localChannelID != null); // since type is not master
				
				Channel baseChan = channels.get(localChannelID);
				
				if (baseChan != null) { // it is possible for local channels to be missing
					assert(baseChan.type == ChannelIDType.PureLocal);
					isMuted = baseChan.mix.isMuted() || chan.mix.isMuted();
					volume = baseChan.mix.getVolume() * chan.mix.getVolume();
				} else {
					isMuted = chan.mix.isMuted();
					volume = chan.mix.getVolume();
				}
				
				// pre-mix with master mix - if has one
				Channel master = channels.get(chan.masterMixID);
				if (master != null) {
					assert(master.type == ChannelIDType.Master);
					isMuted |= master.mix.isMuted();
					volume *= master.mix.getVolume();
				}

			} else {
				isMuted = chan.mix.isMuted();
				volume = chan.mix.getVolume();
			}
	
			chan.tseq.setMuted(isMuted);
			chan.tseq.setVolume(volume);
		}
		
		// Update any channels that are linking to this (local) channel's mix at the momment
		if (chan.type == ChannelIDType.PureLocal) {
			
			List<Channel> lchans = linkedChannels.get(chan.mix.getChannelID());
			
			if (lchans != null) {
				for (Channel lchan : lchans) {
					if (lchan.tseq != null) 
						updateMixSettings(lchan);
				}
			}
			
		// Update any channels that are using this master mix at the moment
		} else if (chan.type == ChannelIDType.Master) {

			List<Channel> childChans = childChannels.get(chan.mix.getChannelID());
			
			if (childChans != null) {
				for (Channel cchan : childChans) {
					assert(cchan.masterMixID.equals(chan.mix.getChannelID()));
					assert(cchan.type == ChannelIDType.IndirectLocal);
					
					// Is playing? i.e. do need to update?
					if (cchan.tseq != null) 
						updateMixSettings(cchan);
				}
			}
		}
	}

	public Subject getObservedSubject() {
		return null;
	}
	public void setObservedSubject(Subject parent) {
	}

	/**
	 * Synchs the track sequences with the mixes.
	 * Assumes event is a ApolloSubjectChangedEvent and source
	 * is a TrackMixSubject
	 */
	public void modelChanged(Subject source, SubjectChangedEvent event) {
		
		if (source instanceof TrackMixSubject) {
			
			TrackMixSubject tmix = (TrackMixSubject)source;
			
			Channel chan = channels.get(tmix.getChannelID());
			assert(chan != null);
			assert(chan.mix == tmix);
			
			updateMixSettings(chan);

		} else if (source instanceof TrackSequence) {

			if (event.getID() == ApolloSubjectChangedEvent.PLAYBACK_STOPPED) {
				
				Channel chan = tseqIndexedChannels.get(source);
				if (chan != null) {
					freeChannel(chan, (TrackSequence)source);
				}
			}
		}
		
	}
	
	public void freeChannels(String channelPrefix) {
		assert (channelPrefix != null);
		assert (channelPrefix.length() > 0);
		
		for (Channel chan : channels.values()) {
			
			if (chan.tseq != null && chan.mix.getChannelID().startsWith(channelPrefix)) {
				
				freeChannel(chan, chan.tseq);
			}
		}
		
	}
	
	private void freeChannel(Channel chan, TrackSequence ts) {
		assert (chan != null);
		assert (ts != null);

		chan.stoppedFramePosition = (chan.tseq != null) ? chan.tseq.getSuspendedFrame() : -1;
		chan.tseq = null; // Free SampledTrackModel reference for garbage collection
		tseqIndexedChannels.remove(ts); // "  ditto  "
		
	}

	/**
	 * Creates a <i>pure local</i> channel ID.
	 * 
	 * @param sampledTrackWidget
	 * 		The widget to create the ID from. Must not be null.
	 * 			
	 * @return
	 * 		The channel ID... never null.
	 */
	public static String createPureLocalChannelID(SampledTrack sampledTrackWidget) {
		assert(sampledTrackWidget != null);
		return createPureLocalChannelID(sampledTrackWidget.getLocalFileName());
	}
	
	/**
	 * Creates a <i>pure local</i> channel ID.
	 * 
	 * @param tinf
	 * 		The TrackGraphInfo to create the ID from. Must not be null.
	 * 			
	 * @return
	 * 		The channel ID... never null.
	 */
	public static String createPureLocalChannelID(TrackGraphNode tinf) {
		assert(tinf != null);
		return createPureLocalChannelID(tinf.getLocalFilename());
	}
	
	/**
	 * Creates a <i>pure local</i> channel ID.
	 * 
	 * @param localFilename
	 * 		The local filename to create the channel ID from
	 * 			
	 * @return
	 * 		The channel ID... never null.
	 */
	public static String createPureLocalChannelID(String localFilename) {
		assert(localFilename != null && localFilename.length() > 0);
		return localFilename + LOCAL_CHANNEL_ID_TAG;
	}
	
	/**
	 * Creates an <i>indirect</i> local channel id from a given virtual path.
	 * 
	 * @param virtualPath
	 * 		The virtual path to create the id from. Must not be null.
	 * 
	 * @param localFilename
	 * 		The local filename that the virtual path points to. Must not be null.
	 * 
	 * @return
	 * 		An indirect local channel id... Never null.
	 */
	public static String createIndirectLocalChannelID(List<String> virtualPath, String localFilename) {
		assert(virtualPath != null);
		assert(localFilename != null);
		
		// Build prefix
		StringBuilder sb = new StringBuilder();
		
		for (String virtualName : virtualPath) {
			sb.append(virtualName);
			sb.append(CHANNEL_ID_SEPORATOR);
		}
		
		sb.append(createPureLocalChannelID(localFilename));
		
		return sb.toString();
	}
	
	public static String createMasterChannelID(LinkedTrack lwidget) {
		return lwidget.getVirtualFilename();
	}
	/**
	 * Creates a master channel ID from a frame.
	 * 
	 * @param frame
	 * 		The frame to create the master mix ID from.
	 * 		Must not be null.
	 * 
	 * @return
	 * 		The master mix for the given frame.
	 */
	public static String createMasterChannelID(Frame frame) {
		assert(frame != null);
		return frame.getName();
	}
	
	/**
	 * Creates a master channel ID from a OverdubbedFrame.
	 * 
	 * @param odFrame
	 * 		The frame to create the master mix ID from.
	 * 		Must not be null.
	 * 
	 * @return
	 * 		The master mix for the given odFrame.
	 */
	public static String createMasterChannelID(OverdubbedFrame odFrame) {
		assert(odFrame != null);
		return odFrame.getFrameName();
	}
	
	/**
	 * Creates a master channel ID from a LinkedTracksGraphInfo.
	 * 
	 * @param ltracks
	 * 		The linked track to create the master mix ID from.
	 * 		Must not be null.
	 * 
	 * @return
	 * 		The master mix for the given ltracks.
	 */
	public static String createMasterChannelID(LinkedTracksGraphNode ltracks) {
		assert(ltracks != null);
		return ltracks.getVirtualFilename();
	}
	
	
	/**
	 * Recursively creates channel IDs from a OverdubbedFrame. I.e. for all its
	 * desending tracks. Linked tracks are exluded and only used for recursing.
	 * 
	 * <b>Exlcudes the ID for odFrame (the master channel)</b>
	 * 
	 * @param odFrame
	 * 		The frame to start recursivly building the ID's from.
	 * 
	 * @return
	 * 		The list of linked ID's (to actual tracks) from this frame...
	 * 		Never null. Can be empty if there are no tracks from  the
	 * 		given frame.
	 */
	public static List<String> createChannelIDs(OverdubbedFrame odFrame) {
		assert(odFrame != null);

		Stack<String> virtualPath = new Stack<String>();
		virtualPath.push(createMasterChannelID(odFrame)); // Master mix
		List<String> ids = new LinkedList<String>();
		createChannelIDs(odFrame, ids, new Stack<OverdubbedFrame>(), virtualPath);
		assert(virtualPath.size() == 1);
		
		return ids;
	}
	
	/**
	 * Recursively creates channel IDs from a LinkedTracksGraphInfo. I.e. for all its
	 * desending tracks. Linked tracks are exluded and only used for recursing.
	 * 
	 * <b>Exlcudes the ID for ltracks (the master channel)</b>
	 * 
	 * <b>Note:</b> calling {@link #createChannelIDs(OverdubbedFrame)} with the links
	 * OverdubbedFrame is not the equivelent of calling this... because it uses a
	 * different master channel ID.
	 * 
	 * @param ltracks
	 * 		The link to start recursivly building the ID's from.
	 * 
	 * @return
	 * 		The list of linked ID's (to actual tracks) from this frame...
	 * 		Never null. Can be empty if there are no tracks from the
	 * 		given ltracks.
	 */
	public static List<String> createChannelIDs(LinkedTracksGraphNode ltracks) {
		assert(ltracks != null);
		
		Stack<String> virtualPath = new Stack<String>();
		virtualPath.push(createMasterChannelID(ltracks));
		List<String> ids = new LinkedList<String>();
		createChannelIDs(ltracks.getLinkedFrame(), ids, new Stack<OverdubbedFrame>(), virtualPath);
		assert(virtualPath.size() == 1);
		
		return ids;
	}
	
	/**
	 * Recursively creates channel ID.
	 * @param current
	 * 		The root at which to recuse at.
	 * 
	 * @param ids
	 * 		A list of strings to populate with channel ids.
	 * 
	 * @param visited
	 * 		Non-null Empty.
	 * 
	 * @param virtualPath
	 * 		Preload with IDs if needed.
	 */
	private static void createChannelIDs(
			OverdubbedFrame current,
			List<String> ids,
			Stack<OverdubbedFrame> visited,
			Stack<String> virtualPath) {
		
		// Avoid recursion .. never can be to safe.
		if (visited.contains(current)) return;
		visited.push(current);

		if (current.trackExists()) {
			
			// Build prefix
			StringBuilder prefixBuilder = new StringBuilder();
			
			for (String virtualName : virtualPath) {
				prefixBuilder.append(virtualName);
				prefixBuilder.append(CHANNEL_ID_SEPORATOR);
			}
			
			String prefix = prefixBuilder.toString();
			assert(prefix.length() > 0);
			assert(prefix.charAt(prefix.length() - 1) == CHANNEL_ID_SEPORATOR);
			
			// Create id's
			for (TrackGraphNode tinf : current.getUnmodifiableTracks()) {
				String id = prefix + createPureLocalChannelID(tinf);
				assert(!ids.contains(id));
				ids.add(id);
			}
		}
		
		// Recurse
		for (LinkedTracksGraphNode ltinf : current.getUnmodifiableLinkedTracks()) {
			virtualPath.push(ltinf.getVirtualFilename()); // build vpath
			createChannelIDs(ltinf.getLinkedFrame(), ids, visited, virtualPath);
			virtualPath.pop(); // maintain vpath
		}

		visited.pop();

	}

	
	/**
	 * Extracts a pure local channelID...
	 * Thus for a given channel ID made up of virtual links its all the virtual
	 * ids are stripped and the remaining local channel ID is returned.
	 * 
	 * @param channelID
	 * 		The channel ID to extract the local channel ID from.
	 * 		Must not be null or empty.
	 * 
	 * @return
	 * 		The local channel ID. Null if channelID does not refer to a local channel ID:
	 * 		in that case the channel ID would either be invalid or be a pure link channel.
	 * 
	 */
	public static String extractLocalChannelID(String channelID) {
		assert (channelID != null);
		assert(channelID.length() > 0);
		
		if (channelID.charAt(channelID.length() - 1) == LOCAL_CHANNEL_ID_TAG) {
			
			// Starting at the end... search bacward for the start or a seporator
			int i = channelID.length() - 2;
			while (i > 0 && channelID.charAt(i) != CHANNEL_ID_SEPORATOR) i--;
			
			if (channelID.charAt(i) == CHANNEL_ID_SEPORATOR) i++;

			if (((channelID.length() - 1) - i) == 0) return null;
			
			return channelID.substring(i, channelID.length()); // include the local tag
			
		}
		
		// No local part
		return null;
	}
	
	/**
	 * Pure local or indirect local Channel IDs are encoded with local track names, 
	 * 
	 * @param channelID
	 * 		The channel ID to extract the local filename from.
	 * 
	 * @return
	 * 		The local filename from the ID. Null if the channel ID is not a 
	 * 		pure local / indirect local id.
	 * 		
	 */
	public static String extractLocalFilename(String channelID) {
		String localID = extractLocalChannelID(channelID);
		if (localID != null && localID.length() > 1) 
			return localID.substring(0, localID.length() - 1);
		
		return null;
	}
	
	/**
	 * Extracts the master ID from a channel ID.
	 * If the channel id is a pure local ID then it will return channelID.
	 * 
	 * @param channelID
	 * 		The channel id to extract the master id from. Must not be null.		
	 * 
	 * @return
	 * 		The master ID. Null if channelID is null or invalid in some way...
	 */
	public static String extractMasterChannelID(String channelID) {
		assert (channelID != null);
		
		// master id = top-level id... that is, a pure virtual name, framename or local name
		
		int i;
		for (i = 0; i < channelID.length(); i++) {
			if (channelID.charAt(i) == CHANNEL_ID_SEPORATOR) break;
		}
		
		if (i > 1) {
			return channelID.substring(0, i);
		}
		
		return null;
		
	}
	
	/**
	 * Gets the classification of a channel ID.
	 * 
	 * @see {@link ChannelIDType} for info on the channel types
	 * 
	 * @param channelID
	 * 		The channel id to classify. Must not be null or empty.
	 * 
	 * @return
	 * 		Either the classification for the given ID or Null if the ID is invalid.
	 */
	public static ChannelIDType getChannelIDType(String channelID) {
		assert (channelID != null);
		assert (channelID.length() > 0);
		
		if (channelID.indexOf(CHANNEL_ID_SEPORATOR) >= 0) {
			
			if (channelID.charAt(channelID.length() - 1) != LOCAL_CHANNEL_ID_TAG)
				return null; // Invalid!
			
			return ChannelIDType.IndirectLocal;
			
		} else if (channelID.charAt(channelID.length() - 1) == LOCAL_CHANNEL_ID_TAG) {
				return ChannelIDType.PureLocal;
		}
		
		return ChannelIDType.Master;
		
	}

	/**
	 * There is only ever one mix per channel, and one channel per mix
	 *  (i.e. 1 to 1 relationship).
	 * 
	 * @author Brook Novak
	 *
	 */
	private class Channel {
		
		ChannelIDType type; // "immutable" never null.

		/** "immutable" - never null */
		TrackMixSubject mix;
	
		/** Apart from the systems master mix - this is a pre-mix to aggregate 
		 * with this channels actual/indirect mix. For example a channel that is
		 * linked might need to mix with its top-level link mix... 
		 * Man that is a bad explanation! 
		 * 
		 * Only meaningful for indirect local channels - otherwise is just the same
		 * as the TrackMixSubject channel ID.
		 **/
		String masterMixID; // never null, itself can be a mastermix
		
		TrackSequence tseq; // resets to null to avoid holding expensive state ref (track model)
		
		// Used for pausing / resuming
		int stoppedFramePosition = -1;
		
		// A mark used outside of this package
		boolean isPaused = false;
		
		/**
		 * Consturctor.
		 * 
		 * @param mixSub
		 * @param isUsingLocalMix
		 * 
		 * @throws IllegalArgumentException
		 * 		If mixSub's channel ID is invalid.
		 */
		Channel(TrackMixSubject mixSub) {
			assert(mixSub != null);
			mix = mixSub;
			tseq = null;
			this.type = getChannelIDType(mixSub.getChannelID());

			if (type == null)
				throw new IllegalArgumentException("mixSub contains bad channel ID");
			
			masterMixID = extractMasterChannelID(mixSub.getChannelID());
				
			assert(integrity());
		}
		
		private boolean integrity() {
			
			assert(mix != null);
			assert(type != null);
			assert(masterMixID != null);
			
			if(type == ChannelIDType.Master) {
				assert(masterMixID.equals(mix.getChannelID())); 
				assert(tseq == null);
			} else if(type == ChannelIDType.PureLocal) {
				assert(masterMixID.equals(mix.getChannelID())); 
			} else if(type == ChannelIDType.IndirectLocal) {
				assert(!masterMixID.equals(mix.getChannelID())); // must have two parts at least
			}

			return true;
		}
	}
	
	/**
	 * Channel IDs have different classifications.
	 * 
	 * @author Brook Novak
	 *
	 */
	public enum ChannelIDType {
		
		/**
		 * Pure local channels have mixes that can be used by {@link ChannelIDType#IndirectLocal}
		 * channels.
		 */
		PureLocal, // never links to local mixes .. since is local.
		
		/**
		 * Indirect local channels are channels that have their own mix, but can also use a
		 * pure local mix instead. Furthermore, they always pre-mix with a
		 * {@link ChannelIDType#Master} channel.
		 */
		IndirectLocal, // Combined of virtual/frame ids and local ids...
		
		/**
		 * Master channels are essentially channels containing mix information. They
		 * can be linked by many {@link ChannelIDType#IndirectLocal} channels - where the
		 * linked channels combine their curent mix with it's master mix.
		 * 
		 * This is seperate from the systems master mix which is awlays applied in the 
		 * {@link ApolloPlaybackMixer} for every track.
		 * 
		 */
		Master, // i.e. a channel that is purely used for mixing with child channels
	}

}
