package org.apollo.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apollo.audio.ApolloPlaybackMixer;
import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.structure.OverdubbedFrame;
import org.apollo.audio.util.MultiTrackPlaybackController;
import org.apollo.audio.util.PlaybackClock;
import org.apollo.audio.util.Timeline;
import org.apollo.audio.util.PlaybackClock.PlaybackClockListener;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.AudioMath;
import org.apollo.widgets.FramePlayer;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;

/**
 * Renders playback bars while the multi playback controller is playing.
 * Depending on which frame the user is on etc...
 * 
 * @author Brook Novak
 *
 */
public class FramePlaybackBarRenderer implements PlaybackClockListener, Observer {
	
	/** Relative to the current multi-track groups root frame. */
	private long currentMSPosition = 0;
	
	//private long liveFrameOffset = 0;
	
	private List<Integer> pixelPositions = new LinkedList<Integer>();
	private String pixelPositionsParent = null; // framename
	
	private Timeline currentTimeline = null;
	
	private PlaybackFrameBarUpdator updator = new PlaybackFrameBarUpdator();
	
	private static final int BAR_STROKE_THICKNESS = 2;
	private static final Stroke BAR_STROKE = new BasicStroke(BAR_STROKE_THICKNESS);
	private static final Color BAR_COLOR = Color.DARK_GRAY;
	
	private static FramePlaybackBarRenderer instance = new FramePlaybackBarRenderer();
	
	private FramePlaybackBarRenderer() {
		MultiTrackPlaybackController.getInstance().addObserver(this);
		//DisplayIO.addDisplayIOObserver(this);
		FrameLayoutDaemon.getInstance().addObserver(this);
	}
	
	public static FramePlaybackBarRenderer getInstance() {
		return instance;
	}

	/**
	 * {@inheritDoc}
	 */
	public Subject getObservedSubject() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setObservedSubject(Subject parent) {
	}

	/**
	 * {@inheritDoc}
	 */
	public void modelChanged(Subject source, SubjectChangedEvent event) {

		if (source == FrameLayoutDaemon.getInstance()) {
			currentTimeline = FrameLayoutDaemon.getInstance().getLastComputedTimeline();
			
			updator.run();
			// Asumming refresh will occur

			return;
		}
		
		switch (event.getID()) {
		case ApolloSubjectChangedEvent.PLAYBACK_STARTED:
			
			/*liveFrameOffset = ApolloPlaybackMixer.getInstance().getLiveFramePosition();
			if (liveFrameOffset < 0) liveFrameOffset = 0;
			System.out.println("liveFrameOffset=" + liveFrameOffset);*/
			
			PlaybackClock.getInstance().addPlaybackClockListener(this);
			break;
			
		case ApolloSubjectChangedEvent.PLAYBACK_STOPPED:
			PlaybackClock.getInstance().removePlaybackClockListener(this);
			
			if (MultiTrackPlaybackController.getInstance().isMarkedAsPaused() &&
					FramePlayer.FRAME_PLAYERMASTER_CHANNEL_ID.equals(
							MultiTrackPlaybackController.getInstance().getCurrentMasterChannelID())) {

				currentMSPosition = AudioMath.framesToMilliseconds(
						MultiTrackPlaybackController.getInstance().getLastSuspendedFrame(),  
						ApolloPlaybackMixer.getInstance().getLiveAudioFormat());
				
				updator.run();
				
			} else {
				invalidate();
				currentMSPosition = -1;
				pixelPositions.clear();
			}
			
			FrameGraphics.refresh(true);
			
			
			break;
			
		case ApolloSubjectChangedEvent.PAUSE_MARK_CHANGED:
			
			
			invalidate();
			currentMSPosition = -1;
			pixelPositions.clear();
			FrameGraphics.refresh(true);
			
			break;
		}
			
		
	}
	
	private void invalidate() {
		if (Browser._theBrowser == null) return;
		
		int height = Browser._theBrowser.getHeight();
		
		for (Integer n : pixelPositions) {
			FrameGraphics.invalidateArea(new Rectangle(n - 1, 0, BAR_STROKE_THICKNESS + 2, height));
		}
	}
	
	public void paint(Graphics2D g) {
		if (Browser._theBrowser == null) return;
		
		Frame currentFrame = DisplayIO.getCurrentFrame();
		
		if (currentFrame == null || currentFrame.getName() == null || pixelPositionsParent == null ||
				!currentFrame.getName().equals(pixelPositionsParent)) return;
		
		int height = Browser._theBrowser.getHeight();
		
		g.setColor(BAR_COLOR);
		g.setStroke(BAR_STROKE);
		
		for (Integer n : pixelPositions) {
			g.drawLine(n, 0, n, height);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void onTick(long framePosition, long msPosition) {
		
		if (framePosition < 0) return; // let stop event handle it
		
		//framePosition -= liveFrameOffset;
		
		// Convert the audio mixers frame position to the multiplaybacks frame position
		
		int fpos = (int)(framePosition 
				- MultiTrackPlaybackController.getInstance().getLastInitiationFrame()
				+ MultiTrackPlaybackController.getInstance().getLastStartFrame());
	
		// Clamp
		if (fpos > MultiTrackPlaybackController.getInstance().getLastEndFrame())
			fpos = MultiTrackPlaybackController.getInstance().getLastEndFrame();
		
		currentMSPosition = AudioMath.framesToMilliseconds(
				fpos,  
				ApolloPlaybackMixer.getInstance().getLiveAudioFormat());
		
		// Notes: the clock will queue a refresh for the frame after this
		// event proccesses ... 
		SwingUtilities.invokeLater(updator);
	}
	
	/**
	 * Note: refreshing is up to caller
	 * 
	 * @author Brook Novak
	 */
	private class PlaybackFrameBarUpdator implements Runnable {
		public void run() {
			
			if (currentMSPosition == -1 || currentTimeline == null) return;
			
			Frame currentFrame = DisplayIO.getCurrentFrame();
			
			if (currentFrame == null || currentFrame.getName() == null) return;

			OverdubbedFrame od = MultiTrackPlaybackController.getInstance().getCurrentODFrame();
			if (od == null) return;

			// Invalidate old positions
			invalidate();
			pixelPositions.clear();
			
			long firstInitTime = od.getFirstInitiationTime();
			
			List<Integer> msPositions = od.getMSPositions(
					currentFrame.getName(), 
					currentMSPosition + firstInitTime);
			
			// Convert ms positions to pixel positions according to the current timeline
			for (Integer n : msPositions) {
				pixelPositions.add(new Integer(currentTimeline.getXAtMSTime(n + currentTimeline.getFirstInitiationTime())));
			}

			pixelPositionsParent = currentFrame.getName();
				
			// Invalidate new positions
			invalidate();
		}
	}
	
	

}
