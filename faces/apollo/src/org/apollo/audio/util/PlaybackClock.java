package org.apollo.audio.util;

import java.util.LinkedList;
import java.util.List;

import org.apollo.audio.ApolloPlaybackMixer;
import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.AudioMath;
import org.expeditee.gui.FrameGraphics;

/**
 * The audio playback clock ticks at {@link #CLOCK_RESOLUTION} milliseconds and
 * invokes all registered clock listeners <b>on the same thread as the clock<b>.
 * Thus the clock is not entiry acurrate: its intention of use is for updating
 * a GUI according to the live frame position in the software mixer.
 * 
 * Thus all thread safety precautions and efficiencies are at the users discretion.
 * The ideal usage would be to simply update some data without the need of locking
 * and returning asap
 * 
 * @see {@link ApolloPlaybackMixer}
 * 
 * @author Brook Novak
 *
 */
public class PlaybackClock {
	
	public static final long CLOCK_RESOLUTION = 300;
	
	private Clock clockThread = null;
	
	private List<PlaybackClockListener> playbackClockListeners = new LinkedList<PlaybackClockListener>();
	
	private static PlaybackClock instance = new PlaybackClock();
	public  static PlaybackClock getInstance() {
		return instance;
	}

	/**
	 * Singleton constructor preps the PlaybackClock for listening for
	 * audio events from the software mixer
	 *
	 */
	private PlaybackClock() {
		
		ApolloPlaybackMixer.getInstance().addObserver(new Observer() {

			public Subject getObservedSubject() {
				return ApolloPlaybackMixer.getInstance();
			}
			public void setObservedSubject(Subject parent) {
			}

			public void modelChanged(Subject source, SubjectChangedEvent event) {
				
				if (event.getID() == ApolloSubjectChangedEvent.PLAYBACK_STARTED) {
					
					// Start the clock
					assert(clockThread == null);
					clockThread = new Clock();
					clockThread.start();
					
				} else if (event.getID() == ApolloSubjectChangedEvent.PLAYBACK_STOPPED) {
					
					// Kill the clock
					clockThread.interrupt();
					clockThread = null; // it will die in its own time
				}
			}

		});
	}
	
	/**
	 * Adds a PlaybackClockListener for receiveing notifications whenever audio is played.
	 * 
	 * @param listener
	 * 		The listener to add. If already added then it won't be added again.
	 * 
	 * @throws NullPointerException
	 * 		If listener is null.
	 */
	public void addPlaybackClockListener(PlaybackClockListener listener) {
		if (listener == null) throw new NullPointerException("listener");
		synchronized(playbackClockListeners) {
			if (!playbackClockListeners.contains(listener))
				playbackClockListeners.add(listener);
		}
	}
	
	/**
	 * Removes a listener such that it wil stop receiving notifications.
	 * 
	 * @param listener
	 *      The listener to remove.
	 *      
	 * @throws NullPointerException
	 * 		If listener is null.
	 */
	public void removePlaybackClockListener(PlaybackClockListener listener) {
		if (listener == null) throw new NullPointerException("listener");
		synchronized(playbackClockListeners) {
			playbackClockListeners.remove(listener);
		}
	}
	
	/**
	 * The clock periodically wakes and updates playback-related GUI according to the
	 * live position of the playback. 
	 * 
	 * The clock thread dies once its interupted.
	 * 
	 * @author Brook Novak
	 *
	 */
	private class Clock extends Thread {
		
		Clock() {
			super("Playback Clock");
		}
		
		public void run() {
			
			try {
				long framePos, msPos;
				boolean shouldRepaint;
				
				while (!interrupted()) {

					// Get the live time in frames - directly from the hardware
					framePos = ApolloPlaybackMixer.getInstance().getLiveFramePosition();
	
					shouldRepaint = false;
					
					if (framePos >= 0) { // is playing still?
						
						msPos = AudioMath.framesToMilliseconds(
								framePos,  
								ApolloPlaybackMixer.getInstance().getLiveAudioFormat());
		
						// Update the GUI. Because the clock awakes in small time slices must
						// avoid updating the GUI on the swing thread: otherwise the AWT Event
						// queue could easily become saturated.
						synchronized(playbackClockListeners) { // arg, the most expensive operation!

							for (PlaybackClockListener listener : playbackClockListeners) {
	
								listener.onTick(framePos, msPos); // trusting that an exception does not occur
								// If one does occur the worst that can happen is the the user doesn't
								// have playback freedback.
								shouldRepaint = true;
							}
							
						}
						
					}
					
					// Do a repaint is something was listening
					if (shouldRepaint) {
						FrameGraphics.requestRefresh(true); // later - should not congest AWT queue
					}
					
					sleep(CLOCK_RESOLUTION);
					
				} // Keep updating GUI until interupted
			} catch (InterruptedException e) { // consume
				// Done
			}
			
			// Notify that have completed
			synchronized(playbackClockListeners) {
				
				for (PlaybackClockListener listener : playbackClockListeners) {
					
					listener.onTick(-1, -1);
					
				}
				
			} 
			
		}
	
	}
	
	/**
	 * A PlaybackClockListener is periodically notified while there is playback occuring.
	 * 
	 * @author Brook Novak
	 *
	 */
	public interface PlaybackClockListener {
		
		/**
		 * At every tick, this is invoked <i>directly from the clock thread</i>
		 * 
		 * @see {@link PlaybackClock} for implementation considerations.
		 * 
		 * @param framePosition
		 * 		The current position of the playback in frames.
		 * 		Negative if playback has finished.
		 * 
		 * @param msPosition
		 * 		The current position of the playback in milliseconds.
		 * 		Negative if playback has finished.
		 * 
		 */
		public void onTick(long framePosition, long msPosition);
		
	}
}
