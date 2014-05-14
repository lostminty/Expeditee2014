package org.apollo.audio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.TrackModelHandler;
import org.apollo.util.TrackModelLoadManager;

/**
 * A software playback mixer.
 * 
 * @author Brook Novak
 *
 */
public class ApolloPlaybackMixer extends AbstractSubject implements TrackModelHandler {
	
	/** The timeline frame represents the global frame counter which all tracks synchronize to. */
	private long timelineFrame = 0; // Wrapping ignored.

	private TreeSet<TrackSequence> sequenceGraph = new TreeSet<TrackSequence>();
	
	private PlaybackThread playbackThread = null;

	private float masterVolume = 1.0f;
	private boolean isMasterMuteOn = false;
	
	private boolean isSoloEnable = false;
	
	private static ApolloPlaybackMixer instance = new ApolloPlaybackMixer();
	private ApolloPlaybackMixer() {
		// When a TrackModel is loading - look in here to see if one is in memory
		TrackModelLoadManager.getInstance().addTrackModelHandler(this);
	}
	
	public static ApolloPlaybackMixer getInstance() {
		return instance;
	}
	
	/**
	 * Stops all playback... kills thread(s).
	 */
	public void releaseResources() {
		
		// Quickly stop playback thread
		if (playbackThread != null)
			playbackThread.stopPlayback(); // will die momenterially
		
		// Release references .. dispose of track memory
		stopAll();
		
	}
	

	/**
	 * Sets the master volume of the output mixer.
	 * 
	 * The master volume is not adjusted directly in the hardware due to java sounds
	 * sketchy API... the master volume is simulated in software.
	 * 
	 * An AudioControlValueChangedEvent event is fired with a FloatControl.Type.VOLUME
	 * type and the volume (a float, range of 0-1 clamped) is passed as the value
	 * of the new volume. 
	 * 
	 * The volumes are updated for all audios created with the SampledAudioManager
	 * to the new mix.
	 * 
	 * @param volume The new volume to set it to. Ranges from 0-1.
	 */
	public void setMasterVolume(float volume) {
		
		// Clamp volume argument
		if (volume < 0.0f)
			volume = 0.0f;
		else if (volume > 1.0f)
			volume = 1.0f;

		if (volume == masterVolume)
			return;

		masterVolume = volume;

		// Notify obsevers
		fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.VOLUME));
	}
	

	/**
	 * Sets the master mute of the output mixer.
	 * 
	 * The master mute is not adjusted directly in the hardware due to java sounds
	 * sketchy API... the master mute is simulated in software.
	 * 
	 * An AudioSubjectChangedEvent.MUTE event is fired with a BooleanControl.Type.MUTE
	 * type and the mute is passed as the value...
	 * 
	 * The mutes are updated for all audios created with the SampledAudioManager
	 * to the new mix.
	 * 
	 * @param mute
	 */
	public void setMasterMute(boolean muteOn) {
		if (muteOn == isMasterMuteOn)
			return;

		isMasterMuteOn = muteOn;
		
		// Notify obsevers
		fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.MUTE));

	}
	
	/** 
	 * @return
	 * 		True if master mute is on.
	 */
	public boolean isMasterMuteOn() {
		return isMasterMuteOn;
	}

	/**
	 * @return
	 * 		The master volume. Always between 0 and 1.
	 */
	public float getMasterVolume() {
		return masterVolume;
	}
	
	/**
	 * @return
	 * 		True if the mixer is in solo mode.
	 */
	public boolean isSoloEnable() {
		return isSoloEnable;
	}

	/**
	 * In solomode, the only track sequences that are played are those with
	 * the solo flag set.
	 * 
	 * @param isSoloEnable
	 * 		True to set into solo mode.
	 */
	public void setSoloEnable(boolean isSoloEnable) {
		this.isSoloEnable = isSoloEnable;
	}
	
	/**
	 * Sets all track sequences that are playing / queued to play - solo flag to false.
	 */
	public void unsetAllSoloFlags() {
		synchronized(sequenceGraph) {
			for (TrackSequence ts : sequenceGraph) ts.isSolo = false;
		}
	}

	
	/**
	 * Plays a track at the given relative initiation time to the current
	 * (global) playback position.
	 * 
	 * If the track is already playing, or is about to play, then nothing will result in
	 * invoking this call.
	 * 
	 * @param track
	 * 		The track to play.
	 * 
	 * @return
	 * 		True if queued for playing. False if track already is in the track graph.
	 * 
	 * @throws NullPointerException
	 * 		If track is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If track has already been played before.
	 * 
	 * @throws LineUnavailableException
	 * 		If failed to get data line to output device
	 */
	public boolean play(TrackSequence track) throws LineUnavailableException {
		if (track == null) throw new NullPointerException("track");
		if (track.hasFinished()) throw new IllegalArgumentException("track is stale, must create new instance");
		
		while (true) {
			// Add to graph
			synchronized(sequenceGraph) {
				
				if (sequenceGraph.contains(track)) {
					return false; // if already playing / queued to play then ignore
				}
				
				if (playbackThread == null || !playbackThread.isAlive() || !playbackThread.isStopping()) {
					// Set initiation to commence relative to the current timeline.
					track.initiationFrame = timelineFrame + track.getRelativeInitiationFrame();
					
					sequenceGraph.add(track);
					
					break;
				}
	
			}
			
			// Cannot play if mixer is in a stopping state since it will stop all tracks when thread terminates
			try {
				playbackThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false; 
			}
		}
		
		// Ensure that the added track will play
		commencePlayback();
		
		return true;
		
	}
	
	/**
	 * Plays a group of tracks exactly at the scheduled frame-time with respect to each other.
	 * 
	 * For tracks in the given set that are already playing, or are queued for playing, then
	 * they will be ignored.
	 * 
	 * @param tracks
	 * 		A set of sequence tracks to play together exactly at their relative initiation points.
	 * 
	 * @throws NullPointerException
	 * 		If tracks is null.
	 * 
	 * @throws LineUnavailableException
	 * 		If failed to get data line to output device
	 * 
	 * @throws IllegalArgumentException
	 * 		If tracks is empty
	 */
	public void playSynchronized(Set<TrackSequence> tracks) throws LineUnavailableException {
		if (tracks == null) throw new NullPointerException("tracks");
		if (tracks.isEmpty())
			throw new IllegalArgumentException("tracks is empty");

		while (true) {

			// Add to graph
			synchronized(sequenceGraph) {
				
				if (playbackThread == null || !playbackThread.isAlive() || !playbackThread.isStopping()) {

					long initiationTimeOffset = timelineFrame;
					
					// If the playback thread is running... and the new tracks will begin playback automatically, then
					// schedule the group of tracks to play in the next pass so they all begin together.
					if (playbackThread != null && !playbackThread.isStopping() && playbackThread.isAlive()) { 
						initiationTimeOffset += playbackThread.bufferFrameLength;
					}
					
					for (TrackSequence ts : tracks) {
						
						if (ts == null) continue;
						
						if (sequenceGraph.contains(ts)) 
							continue; // if already playing / queued to play then ignore

						ts.initiationFrame = ts.getRelativeInitiationFrame() + initiationTimeOffset;
						sequenceGraph.add(ts);
						
					}
					
					// Playback commencable
					break;
					
				}
				
			}

			// Cannot play if mixer is in a stopping state since it will stop all tracks when thread terminates
			try {
				playbackThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			
		}
			
		// Ensure that the added tracks will play 
		commencePlayback();
		
	}
	
	/**
	 * Ensures that the playback thread is playing / will keep playing.
	 * If a new track is added top the sequence graph, then by calling this it will
	 * ensure that it will be played.
	 * 
	 * @throws LineUnavailableException
	 * 		If failed to get data line to output device
	 */
	private void commencePlayback() throws LineUnavailableException {

		// Should not be in a stopping state...
		assert (!(
				playbackThread != null && playbackThread.isAlive() && playbackThread.isStopping()));
		
		if (playbackThread != null && !playbackThread.isAlive()) playbackThread = null;
		
		// If playbackThread is not null at this point, then it is assumed that it is still playing
		// therefore does not need to be started/restarted.
		
		// Before playback is commenced, ensure that the MIDI device is
		Metronome.getInstance().release();
		
		// If the playback thread is dead, create a new one to initiate playback
		if (playbackThread == null) {
		
			playbackThread = new PlaybackThread();
			playbackThread.start();
			
		} 	
		
	}
	
	/**
	 * Stops many track sequences. This is better than calling
	 * {@link #stop(TrackSequence)} because ensures that all tracks are stopped
	 * at the same time.
	 * 
	 * @param tracks
	 * 		The tracks to stop.
	 */
	public void stop(Collection<TrackSequence> tracks) {
		if (tracks == null || playbackThread == null) return;


		synchronized(sequenceGraph) {
			for (TrackSequence track : tracks) {
				stop(track);
			}
		}
		
	}
	
	/**
	 * Stops a track sequence. Nonblocking, the actual stopp will occur when the mixer has a change to
	 * respond.
	 * 
	 * @param track
	 * 		The track to stop. If null then will return with no effect.
	 */
	public void stop(TrackSequence track) {
		if (track == null || playbackThread == null) return;

		track.stopPending = true;
	}

	/**
	 * Stops all track sequences from playback.
	 */
	public void stopAll() {
		
		synchronized(sequenceGraph) {
			for (TrackSequence track : sequenceGraph)
				track.stopPending = true;
		}
		
	}

	/**
	 * {@inheritDoc}
	 */
	public SampledTrackModel getSharedSampledTrackModel(String localfilename) {
		if (localfilename == null) return null;
		
		// Get a snapshot of the graph
		ArrayList<TrackSequence> snapshot = null;
		synchronized(sequenceGraph) {
			snapshot = new ArrayList<TrackSequence>(sequenceGraph);
		}
		
		// Look for SampledTrackModel-invoked sequences
		for (TrackSequence ts : snapshot) {
			Object invoker = ts.getInvoker();
			
			if (invoker != null && invoker instanceof SampledTrackModel) {
				
				// Match?
				if (localfilename.equals(((SampledTrackModel)invoker).getLocalFilename())) {
					return (SampledTrackModel)invoker; // found match
				}
			}
			
		}
		
		// Nothing matched
		return null;
	}

	
	/**
	 * @return
	 * 		The actual frame position in the playback stream - that is, the amount of
	 * 		frames that have been rendered since playback commenced.
	 * 		Negative if there is no playback.
	 */
	public long getLiveFramePosition() {
		
		if (playbackThread != null) {
			if (playbackThread.srcDataLine.isOpen()) {
				
				// The timelineFrame should always be larger or equal to the live frame position
			//	assert(timelineFrame >= playbackThread.srcDataLine.getLongFramePosition());
				
				return playbackThread.srcDataLine.getLongFramePosition();
			}
		}
		
		return -1;
	}
	
	/**
	 * @return
	 * 		The audio format of the current playback data line. Null if not avilable - never
	 * 		null if in a playing state.
	 */
	public AudioFormat getLiveAudioFormat() {
		if (playbackThread != null) {
			return playbackThread.srcDataLine.getFormat();
		}
		return null;		
	}
	
	/**
	 * The Audio Mixing pipeline.
	 * 
	 * All audio mixing math/logic is done within this thread.
	 * Keeps running until the sequenceGraph is empty.
	 * Removes tracks from the sequenceGraph automatically when they are finished.
	 * 
	 * 
	 * @author Brook Novak
	 *
	 */
	private class PlaybackThread extends Thread {
		
		private SourceDataLine srcDataLine; // never null

		private boolean isStopping = false;
		
		private int bufferFrameLength;
		private boolean isOutputBigEndian;
		
		/**
		 * Initantly prepares for audio playback: Opens the source data line for output
		 * 
		 * @throws LineUnavailableException
		 */
		PlaybackThread() throws LineUnavailableException {
			super("Apollo Playback Mixer Thread");
			super.setPriority(Thread.MAX_PRIORITY);
			
			assert(playbackThread == null); // there should be only one instance of this ever.
			
			// Upon creation, open a source data line
			aquireSourceDataLine();
			
			
			// Reset the global timeline frame to match the live frame position. i.e. wrap back at zero.
			synchronized(sequenceGraph) { // probably will be empty, but just for safety...
				
				for (TrackSequence ts : sequenceGraph) {
					ts.initiationFrame -= timelineFrame;
					if (ts.initiationFrame < 0)
						ts.initiationFrame = 0;
				}
				
				timelineFrame = 0;
			}
		}
		
		/**
		 * Opens the source data line for output.
		 * 
		 * @throws LineUnavailableException
		 * 		If failed to acquire the source data line.
		 */
		private void aquireSourceDataLine() throws LineUnavailableException {

			// Select an audio output format
			DataLine.Info info = new DataLine.Info(
					SourceDataLine.class, 
					getAudioFormat());

			// Get the source data line to output.
			srcDataLine = (SourceDataLine) 
				SampledAudioManager.getInstance().getOutputMixure().getLine(info); // LineUnavailableException
			
			srcDataLine.open(); // LineUnavailableException
			
			// Cache useful data
			bufferFrameLength = srcDataLine.getBufferSize() / 2;
			isOutputBigEndian = srcDataLine.getFormat().isBigEndian();
			
			assert(bufferFrameLength > 0);
			assert(srcDataLine.getFormat().getSampleSizeInBits() == 16);
		
		}
		
		/**
		 * Closes the data line so that if currently writing bytes or the next time 
		 * bytes are written, the thread will end.
		 * Does not block calling thread. Thus may take a little while for thread to
		 * end after call returned.
		 */
		public void stopPlayback() {
			srcDataLine.close();
		}
		
		/**
		 * Note: even if all tracks have been proccessed in the audio pipeline, it will
		 * commence another pass to check for new tracks added to the graph before finishing.
		 * 
		 * @return True if stopping and will not play any tracks added to the queue.
		 */
		public boolean isStopping() {
			return isStopping || !srcDataLine.isOpen();
		}
				
		
		/**
		 * @return the best audio format for playback...
		 */
		private AudioFormat getAudioFormat() {
			return SampledAudioManager.getInstance().getDefaultPlaybackFormat();
		}

		/**
		 * The audio mixing pipeline
		 */
		public void run() {

			// Notify observers that some audio has started playing
			ApolloPlaybackMixer.this.fireSubjectChangedLaterOnSwingThread(
				new SubjectChangedEvent(ApolloSubjectChangedEvent.PLAYBACK_STARTED));
			
			// All tracks to play per pass
			List<TrackSequence> tracksToPlay = new LinkedList<TrackSequence>();
			
			// Keeps track of tracks to remove
			List<TrackSequence> completedTracks = new LinkedList<TrackSequence>();
			
			// The buffer written directly to the source data line
			byte[] sampleBuffer = new byte[2 * bufferFrameLength];
			
			// The mixed frames, where each element refers to a frame
			int[] mixedFrameBuffer = new int[bufferFrameLength];
			
			// Helpers declared outside loop for mz efficiency
			int msb, lsb;
			int sample;
			int totalFramesMixed;
			int trackCount; // tracks to play at a given pass
			boolean isMoreQueued; // True if there are more tracks queued. 
			int frameIndex; 
			int i;
			
			// Begin writing to the source data line
			if (srcDataLine.isOpen())
				srcDataLine.start();
			else return;

			// keep playing as long as line is open (and there is something to play)
			try 
			{
				while (srcDataLine.isOpen()) { // The audio mixing pipline

					// First decide on which tracks to play ... and remove any finished tracks.
					synchronized(sequenceGraph) {
						
						// If there are no more tracks queued for playing, then exit the
						// playback thread.
						if (sequenceGraph.isEmpty()) 
							return;
		
						isMoreQueued = false;
						completedTracks.clear();
						tracksToPlay.clear();
						
						for (TrackSequence ts : sequenceGraph) {
							
							// Has this track sequence finished?
							if (ts.currentFrame > ts.endFrame || ts.stopPending)
								completedTracks.add(ts);
							
							// Is this track playing / is meant to start laying in this pass?
							else if (ts.initiationFrame <= (timelineFrame + bufferFrameLength))
								tracksToPlay.add(ts);
	
							// If it is not time to play the track yet, then
							// neither will it for all proceeding tracks either
							// since they are ordered by their initiation time.
							else break;
							
						}
						
						// Get rid of tracks that have finished playing. Notify models that they have stopped
						for (TrackSequence staleTS : completedTracks) {
							
							sequenceGraph.remove(staleTS);
							
							staleTS.onStopped((staleTS.currentFrame > staleTS.endFrame)
									? staleTS.endFrame : staleTS.currentFrame);
							
							//removeTrackFromGraph(staleTS, staleTS.endFrame);
						}

						trackCount = tracksToPlay.size();
						isMoreQueued = sequenceGraph.size() > trackCount;
						
						// If there is nothing queued and there are no tracks to play,
						// then playback is finished.
						if (!isMoreQueued && trackCount == 0) 
							return;
						
					} // release lock
					
					totalFramesMixed = 0; // this will be set to the maximum amount of frames that were mixed accross all tracks
					
					// Clear audio buffer
					for (i = 0; i < bufferFrameLength; i++) // TODO: Effecient way of clearing buffer?
						mixedFrameBuffer[i] = 0; 
					
					// Perform Mixing : 
					// Convert the sample size to 16-bit always for best precision while
					// proccessing audio in the mix pipeline....
					for (TrackSequence ts : tracksToPlay) {
						
						// Notify model that initiated
						if (!ts.isPlaying()) ts.onInitiated(timelineFrame);

						// Skip muted / unsoloed tracks - they add nothing to the sample mix
						if (ts.isMuted || (isSoloEnable && !ts.isSolo)) { 
							
							// Make sure start where initiated, if not already initiated
							if (ts.initiationFrame >= timelineFrame && ts.initiationFrame < (timelineFrame + bufferFrameLength)) {
								
								// Get index in frame buffer where to initiate
								frameIndex = (int)(ts.initiationFrame - timelineFrame);
								
								// Calcuate the length of frames to buffer - adjust silent tracks position
								ts.currentFrame += (bufferFrameLength - frameIndex);

							} else { // skip full buffer of bytes ... silenced
								
								ts.currentFrame += bufferFrameLength; // currentFrame can go outside endframe boundry of the track
				
							}
							
							totalFramesMixed = bufferFrameLength;
							
						} else { // Get samples and add to mix
	
							// If the track is yet to initiate - part way through the buffer, then start adding bytes
							// at initiation point
							if (ts.initiationFrame >= timelineFrame && ts.initiationFrame < (timelineFrame + bufferFrameLength)) {
								
								frameIndex = (int)(ts.initiationFrame - timelineFrame);
								
							} else { 
								
								frameIndex = 0; 
								
							}
		
							// For each frame
							for (;frameIndex < bufferFrameLength && ts.currentFrame <= ts.endFrame; frameIndex++) {
								
								// Get sample according to byte order
								if (ts.isBigEndian) {
									
									// First byte is MSB (high order)
									msb = (int)ts.playbackAudioBytes[ts.currentFrame + ts.currentFrame];
									 
									 // Second byte is LSB (low order)
									lsb = (int)ts.playbackAudioBytes[ts.currentFrame + ts.currentFrame + 1];
								
								 } else {
									 
									// First byte is LSB (low order)
									lsb = (int)ts.playbackAudioBytes[ts.currentFrame + ts.currentFrame];
									 
									// Second byte is MSB (high order)
									msb = (int)ts.playbackAudioBytes[ts.currentFrame + ts.currentFrame + 1];
								}
								
								sample = (msb << 0x8) | (0xFF & lsb);

								// Apply track volume
								sample = (int)(sample * ts.volume);
								
								// Add to current mix
								mixedFrameBuffer[frameIndex] += sample;
			
								// Get next sample
								ts.currentFrame++;
							}
							
							
							// Keep track of total frames mixed in buffer
							if (frameIndex > totalFramesMixed)
								totalFramesMixed = frameIndex;
						}
		
					} // Mix in next track
	
					// totalFramesMixed is the amount of frames to play.
					// If it is zero then it means that there are tracks yet to be intiated, and nothing currently playing
					assert (totalFramesMixed <= bufferFrameLength);
					assert (totalFramesMixed > 0 ||
							(totalFramesMixed == 0 && trackCount == 0 && isMoreQueued));
					
					// Post mix with master settings 
					if (isMasterMuteOn) { // Silence sample buffer if master mute is on
	
						for (i = 0; i < sampleBuffer.length; i++) {
							sampleBuffer[i] = 0;
						}
						
						// Let the muted bytes play
						totalFramesMixed = bufferFrameLength;
						
					} else { // otherwise apply master volume
						
						for (i = 0; i < totalFramesMixed; i++) {
		
							// Average tracks
							//mixedFrameBuffer[i] /= trackCount; // depreciated
							
							// Apply mastar volume
							mixedFrameBuffer[i] = (int)(mixedFrameBuffer[i] * masterVolume);
							
							// Clip
							if (mixedFrameBuffer[i] > Short.MAX_VALUE) mixedFrameBuffer[i] = Short.MAX_VALUE;
							else if (mixedFrameBuffer[i] < Short.MIN_VALUE) mixedFrameBuffer[i] = Short.MIN_VALUE; 
							
							// Convert to output format
							lsb = (mixedFrameBuffer[i] & 0xFF);
							msb = ((mixedFrameBuffer[i] >> 8) & 0xFF);
							
							if (isOutputBigEndian) {
								sampleBuffer[i+i] = (byte)msb;
								sampleBuffer[i+i+1] = (byte)lsb;
							} else {
								sampleBuffer[i+i] = (byte)lsb;
								sampleBuffer[i+i+1] = (byte)msb;
							}

						}

					}
					
					// Generate silence only if there are more tracks to be played.
					// Note that this could be false, but a track might have been queued after
					// setting the isMoreQueued flag. In such cases... silence is not wanted anyway!
					if (isMoreQueued) {
						for (i = totalFramesMixed; i < bufferFrameLength; i++) { // will skip if no need to generate silence
							sampleBuffer[i+i] = 0;
							sampleBuffer[i+i+1] = 0;
						}
						// Ensure that full buffer is played ... including the silence
						totalFramesMixed = bufferFrameLength;
					}
	
					// Write proccessed bytes to line out stream and update the timeline frame
					srcDataLine.write(
							sampleBuffer, 
							0, 
							totalFramesMixed * 2);
					
					// Update timeline counter for sequencing management
					timelineFrame += totalFramesMixed;
					
					// The timelineFrame should always be larger or equal to the live frame position
					assert(timelineFrame >= srcDataLine.getLongFramePosition());
					
				} // Next pass
			
			} finally { 
				
				isStopping = true;
				
				// Ensure line freed
				if (srcDataLine.isOpen()) {
					srcDataLine.drain(); // avoids chopping off last buffered chunk
					srcDataLine.close();
				}
				
				// Clear sequence graph. 
				synchronized(sequenceGraph) {
					
					for (TrackSequence track : sequenceGraph) {
						
						track.onStopped((track.currentFrame > track.endFrame)
								? track.endFrame : track.currentFrame);
					}
					
					sequenceGraph.clear();
					
				}

				// Notify observers that playback has finished.
				ApolloPlaybackMixer.this.fireSubjectChangedLaterOnSwingThread(
						new SubjectChangedEvent(ApolloSubjectChangedEvent.PLAYBACK_STOPPED));
				
			}
			
		}
		

		
	}

	
}
