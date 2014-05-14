package org.apollo.items;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Stroke;

import org.apollo.ApolloSystem;
import org.apollo.audio.SampledAudioManager;
import org.apollo.audio.structure.OverdubbedFrame;
import org.apollo.audio.structure.TrackGraphLoopException;
import org.apollo.audio.util.MultiTrackPlaybackController;
import org.apollo.audio.util.SoundDesk;
import org.apollo.audio.util.Timeline;
import org.apollo.audio.util.MultiTrackPlaybackController.MultitrackLoadListener;
import org.apollo.gui.FrameLayoutDaemon;
import org.apollo.gui.Strokes;
import org.apollo.io.IconRepository;
import org.apollo.util.AudioMath;
import org.apollo.util.Mutable;
import org.apollo.util.ODFrameHeirarchyFetcher;
import org.apollo.util.ODFrameHeirarchyFetcher.ODFrameReceiver;
import org.apollo.widgets.FramePlayer;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.items.ItemParentStateChangedEvent;

/**
 * A transient widget that destroys self once anchored...
 * once anchored it commences playback of a frame if there is something to play.
 * 
 * @author Brook Novak
 *
 */
public class FramePlaybackLauncher extends Item {
	
	private static int WIDTH = 80;
	private static int HEIGHT = 80;
	private static Stroke BORDER_STOKE = Strokes.SOLID_1;
	
	private static final Color BASE_COLOR = new Color(60, 148, 255);
	private static final Color HIGHLIGHT_COLOR = new Color(253, 255, 201);
	
	private String frameToPlay = null;
	
	/**
	 * @param frameToPlay
	 * 		The frame to begin playback from. Null to play current frame.
	 */
	public FramePlaybackLauncher(String frameToPlay) {
		this.setID(getParentOrCurrentFrame().getNextItemID());
		this.frameToPlay = frameToPlay;
	}

	@Override
	public Item copy() {
		FramePlaybackLauncher copy = new FramePlaybackLauncher(frameToPlay);
		
		DuplicateItem(this, copy);
		
		return copy;
	}

	@Override
	public Item merge(Item merger, int mouseX, int mouseY) {
		return null;
	}

	@Override
	public void paint(Graphics2D g) {
		if (Browser._theBrowser == null) return;
		
		Paint restore = g.getPaint();
		
		if (ApolloSystem.useQualityGraphics) {
			GradientPaint gp = new GradientPaint(
					_x + (WIDTH / 2), _y, HIGHLIGHT_COLOR,
					_x + (WIDTH / 2), _y + HEIGHT - (HEIGHT / 5), BASE_COLOR);
			g.setPaint(gp);
		} else {
			g.setColor(BASE_COLOR);
		}
		
		g.fillRect((int)_x, (int)_y, WIDTH, HEIGHT);
		
		g.setPaint(restore);
		
		g.setColor(Color.BLACK);
		g.setStroke(BORDER_STOKE);
		g.drawRect((int)_x, (int)_y, WIDTH, HEIGHT);
		
		IconRepository.getIcon("frameplay.png").paintIcon(
				Browser._theBrowser.getContentPane(), g, getX() + 25, getY() + 25);
		
	}

	@Override
	public void setAnnotation(boolean val) {
	}

	
	@Override
	public int getHeight() {
		return HEIGHT;
	}

	@Override
	public Integer getWidth() {
		return WIDTH;
	}

	@Override
	public void updatePolygon() {
		
		_poly = new Polygon();

		int x = (int)_x;
		int y = (int)_y;
		
		_poly.addPoint(x, y);
		_poly.addPoint(x + WIDTH, y);
		_poly.addPoint(x + WIDTH, y + HEIGHT);
		_poly.addPoint(x, y + HEIGHT);

	}

	@Override
	public void onParentStateChanged(ItemParentStateChangedEvent e) {
		super.onParentStateChanged(e);

		switch (e.getEventType()) {

		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
			// todo: Invoke later for concurrent modification possibility?
			selfDestruct();
			break;

		}
	}
	
	private void selfDestruct() {
		
		Frame parent = getParent();
		parent.removeItem(this);
		
		if (DisplayIO.getCurrentFrame() == null) return;
		
		Frame audioFrame = null;
		
		// Flag as null if frame to play is actually current frame
		if (frameToPlay != null && frameToPlay.equals(DisplayIO.getCurrentFrame().getName()))
			frameToPlay = null;
		
		if (frameToPlay == null) {
			audioFrame = DisplayIO.getCurrentFrame();
		} else {
			audioFrame = FrameIO.LoadFrame(frameToPlay);
		}
		
		if (audioFrame == null) {
			if (frameToPlay != null)
				MessageBay.errorMessage("Unable to play frame \"" + frameToPlay + "\" - unknown frame");
			else 
				MessageBay.errorMessage("Unable to play current frame");
			
		} else {
			
			if (frameToPlay == null) {
				
				Timeline tl = FrameLayoutDaemon.getInstance().getLastComputedTimeline();
				if (tl == null)
					tl = FrameLayoutDaemon.inferTimeline(audioFrame);
				if (tl == null) return;

				long initiateMS = tl.getMSTimeAtX((int)_x);
				
				// Clamp
				if (initiateMS < tl.getFirstInitiationTime())
					initiateMS = tl.getFirstInitiationTime();
				
				else if (initiateMS > (tl.getFirstInitiationTime() + tl.getRunningTime())) 
					initiateMS  = tl.getFirstInitiationTime() + tl.getRunningTime();

				int startFrame = AudioMath.millisecondsToFrames(
						initiateMS - tl.getFirstInitiationTime(),  
						SampledAudioManager.getInstance().getDefaultPlaybackFormat());
				
				playFrame(audioFrame.getName(), startFrame);
				
			} else {
				// Fetch the audio graph for the frame
				GraphReceiver receiver = new GraphReceiver(DisplayIO.getCurrentFrame());
				ODFrameHeirarchyFetcher.getInstance().doFetch(audioFrame.getName(), receiver);
				// The callback will later begin playback
			}
			
	
		}
	}
	
	private void playFrame(String framename, int startFrame) {

		// To ensure that the frame channels are not in use
		MultiTrackPlaybackController.getInstance().stopPlayback();
		SoundDesk.getInstance().freeChannels(FramePlayer.FRAME_PLAYERMASTER_CHANNEL_ID);

		FramePlayer.playFrame(
				new MultitrackLoadListener() {

					public void multiplaybackLoadStatusUpdate(int id, Object state) {
						// Ignore
					}
					
				}, 
				framename, 
				false, 
				startFrame, 
				Integer.MAX_VALUE
				);

	}
	
	private class GraphReceiver implements ODFrameReceiver {
		
		private Frame fromFrame;
		
		public GraphReceiver(Frame fromFrame) {
			assert(fromFrame != null);
			this.fromFrame = fromFrame;
		}
		public void receiveResult(OverdubbedFrame odFrame, TrackGraphLoopException loopEx) {
			
			if(odFrame == null){
				if (loopEx != null)
					MessageBay.errorMessage("Unable to play frame \"" + frameToPlay + "\" - bad structure");
				return;
			}
			
			OverdubbedFrame fromODFrame = odFrame.getChild(fromFrame.getName());
			
			int startFrame = 0;
			
			// Get the timeline for the frame that the playback will be initated from
			Timeline tl = FrameLayoutDaemon.getInstance().getTimeline(fromFrame);
			
			if (tl == null || fromODFrame == null) {
				// The user tried to play a frame, unreachable from some other frame.
				// Thus the start frame will be hence taken as a percentage of the whole frame
				
				float percent = _x / Browser._theBrowser.getContentPane().getWidth();
				if (percent < 0) percent = 0;
				if (percent > 1) percent = 1;

				startFrame = AudioMath.millisecondsToFrames(
						(long)((float)odFrame.calculateRunningTime() * percent),  
						SampledAudioManager.getInstance().getDefaultPlaybackFormat());

			} else {
				
				// Find the frames absolute init time
				Mutable.Long childFrameABSStartTime = odFrame.getChildFrameInitiationTime(fromFrame.getName());

				if (childFrameABSStartTime == null)
					return; // nothing to play

				long deltaMS = tl.getMSTimeAtX((int)_x) - fromODFrame.getFirstInitiationTime();
				
				startFrame = AudioMath.millisecondsToFrames(
						childFrameABSStartTime.value + deltaMS,  
						SampledAudioManager.getInstance().getDefaultPlaybackFormat());
				
			}

			playFrame(odFrame.getFrameName(), startFrame);
				
		}
	}

}
