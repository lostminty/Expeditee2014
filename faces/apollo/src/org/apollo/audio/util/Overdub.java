package org.apollo.audio.util;

import org.apollo.audio.SampledTrackModel;

/**
 * Describes a track that is part of a trackgroup for synchronized multiplayback.
 * 
 * @author Brook Novak
 *
 */
final class Overdub {

	private SampledTrackModel tmodel; // immutable never null
	
	private String channelID; // immutable never null - points to track sequence data via sounddesk
	
	private long absInitiationFrame;
	
	int startFrame;
	
	int endFrame;
	
	int relativeInitiationFrame;
	
	Overdub(SampledTrackModel tmodel, String channelID, long absInitiationFrame) {
		assert(tmodel != null);
		assert(channelID != null);
		assert(absInitiationFrame >= 0);
		
		this.tmodel = tmodel;
		this.channelID = channelID;
		this.startFrame = -1;
		this.endFrame = -1;
		this.relativeInitiationFrame = -1;
		this.absInitiationFrame = absInitiationFrame;
	}

	public String getChannelID() {
		return channelID;
	}

	public int getEndFrame() {
		return endFrame;
	}

	public int getRelativeInitiationFrame() {
		return relativeInitiationFrame;
	}
	
	public long getABSInitiationFrame() {
		return absInitiationFrame;
	}

	public int getStartFrame() {
		return startFrame;
	}

	public SampledTrackModel getTrackModel() {
		return tmodel;
	}

	
}
