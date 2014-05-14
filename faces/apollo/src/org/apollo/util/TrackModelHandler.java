package org.apollo.util;

import org.apollo.audio.SampledTrackModel;

/**
 * A work around for sharing SampledTrackModel references - avoids loading multiple copies of the same audio
 * file and thus can enforce single playback of one audio file at a time.
 * 
 * @author Brook Novak
 *
 */
public interface TrackModelHandler {
	/**
	 * The contract is that any reference that is / will be available through this instance must be
	 * returned to avoid double loading of the same tracks.
	 * 
	 * This can be called from any thread...
	 * 
	 * @param localFileName
	 * 
	 * @return
	 */
	public SampledTrackModel getSharedSampledTrackModel(String localFileName);
}
