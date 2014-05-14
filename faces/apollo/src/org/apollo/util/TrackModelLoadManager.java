package org.apollo.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;

import org.apollo.audio.SampledTrackModel;
import org.apollo.io.AudioIO;
import org.apollo.io.LoadedAudioData;
import org.apollo.mvc.Observer;
import org.expeditee.items.widgets.HeavyDutyInteractiveWidget;
import org.expeditee.items.widgets.WidgetCacheManager;

/**
 * Purpose: to avoid race conditions when loading track models from many threads.
 * 
 * All sharing of track models is done here.
 * 
 * Looked at disposed design patterns... but they become way to deep!
 * 
 * @author Brook Novak
 *
 */
public class TrackModelLoadManager {

	private Set<TrackModelHandler> trackModelHandlers = new HashSet<TrackModelHandler>();
	
	private static TrackModelLoadManager instance = new TrackModelLoadManager();
	
	public static TrackModelLoadManager getInstance() {
		return instance;
	}
	
	private TrackModelLoadManager() {
	}
	
	
	/**
	 * Don't go adding everything possible... only add a handler if the handlers lifetime is
	 * for the whole application, or is "disposable".
	 * 
	 * @param handler
	 */
	public void addTrackModelHandler(TrackModelHandler handler) { 
		assert(handler != null);
		synchronized(trackModelHandlers) {
			trackModelHandlers.add(handler);
		}
	}
	
	/**
	 * Always remove to get rid of reference and keep memory finding routines fast
	 * 
	 * @param handler
	 * 
	 */
	public void removeTrackModelHandler(TrackModelHandler handler) { 
		assert(handler != null);
		synchronized(trackModelHandlers) {
			trackModelHandlers.remove(handler);
		}
	}
	
	/**
	 * Loads a SampledTrackModel. First it attempts to find is in memory, if fails then it
	 * will resort to loading from file.
	 * 
	 * Note that if loaded from file and required conversions, the SampledTrackModel will
	 * be marked as being modified (requiring saving).
	 * 
	 * @param filepath
	 * 		The filepath of the audio file to load. The SampledTrackModel will set its filename as
	 * 		this path value.
	 * 
	 * @param checkExpediteeMemory
	 * 		If set to true, then it will search for SampledTrackModels in expeditee cache
	 * 		on the swing thread. Therefore MUST <b>NOT</b> BE ON THE SWINGTHREAD IF THIS IS SET TO TRUE.
	 * 
	 * @param observer
	 * 		If given (can be null), then the observer will be notified with 
	 * 		LOAD_STATUS_REPORT AudioSubjectChangedEvent events (on the calling thread).
	 * 		The state will be a float with the current percent (between 0.0 and 1.0 inclusive).
	 * 		The subject source will be this instance.
	 * 
	 * @param localFilename
	 * 		The local filename to associate the loaded track with.
	 * 		Must not be null.
	 * 
	 * @return
	 * 			Null iff cancelled.
	 * 
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 */
	public synchronized SampledTrackModel load(
			String filepath,
			String localfilename,
			Observer observer,
			boolean checkExpediteeMemory) throws IOException, UnsupportedAudioFileException {
		
		assert(localfilename != null);

		// Check to see if already in memory
		synchronized(trackModelHandlers) {
			for (TrackModelHandler handler : trackModelHandlers) {
				SampledTrackModel tm = handler.getSharedSampledTrackModel(localfilename);
				if (tm != null) {
					return tm;
				}
			}
		}
		
		if (checkExpediteeMemory) {
			// MUST NOT BE ON SWING THREAD!
			// THIS IS IMPORTANT FOR WHEN FRAMES HAVE BEEN CHECK AND THEY WHERE NOT LOADED AT
			// THE TIME AND THUS CACHED WIDGETS NOT IN MEMORY, BUT LATER IS CACHED ...
			
			CachedTrackModelLocator locator = new CachedTrackModelLocator(localfilename);
			
			try {
				SwingUtilities.invokeAndWait(locator);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
			if (locator.located != null) {
				return locator.located;
			}
			
		}


		// If cannot find in memory, then attempt to load from file.
		LoadedAudioData loadedAudio = AudioIO.loadAudioFile(new File(filepath), observer);
		
		// Could have been cancelled
		if (loadedAudio == null) return null;
		
		// Create the track
		SampledTrackModel track = new SampledTrackModel(
				loadedAudio.getAudioBytes(), 
				loadedAudio.getAudioFormat(),
				localfilename);

		track.setFilepath(filepath);
		
		// Set as modified if the audio bytes had to be converted
		if (loadedAudio.wasConverted()) {
			track.setAudioModifiedFlag(true);
		}
		
		return track;
		
	}
	
	

	/**
	 * Look in expeditee memory - this should be quick - will hold up the user
	 */
	private class CachedTrackModelLocator implements Runnable
	{
		private String localfileName;
		private SampledTrackModel located = null;
		
		CachedTrackModelLocator(String localfileName) {
			assert(localfileName != null);
			this.localfileName = localfileName;
		}
		
		public void run() {
		
			// Exploit knowledge that all SampledTrack are Cached HDW
			for (HeavyDutyInteractiveWidget hdw : WidgetCacheManager.getTransientWidgets()) {
				if (hdw instanceof TrackModelHandler) {

					SampledTrackModel strack = ((TrackModelHandler)hdw).getSharedSampledTrackModel(localfileName);
					
					if (strack != null) {
						assert(strack.getLocalFilename().equals(localfileName));
						
						// found a match that is cahced
						located = strack;
						return;
					}
				}
			}
			
		}
		
	}
	

}
