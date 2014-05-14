package org.apollo.gui;

import java.awt.Color;
import java.awt.Event;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.apollo.ApolloSystem;
import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.SampledTrackModel;
import org.apollo.audio.TrackSequence;
import org.apollo.audio.util.PlaybackClock;
import org.apollo.audio.util.SoundDesk;
import org.apollo.audio.util.TrackMixSubject;
import org.apollo.audio.util.PlaybackClock.PlaybackClockListener;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.expeditee.gui.Browser;
import org.expeditee.gui.FrameGraphics;

/**
 * A graphical view for SampledTrackModel's.
 * 
 * To use the view, just add a SampledTrackGraphView instance to a SampledTrackModel.
 * Can re-use a SampledTrackGraphView instance.
 * 
 * @author Brook Novak
 */
public class SampledTrackGraphView extends JComponent implements Observer, PlaybackClockListener {
	
	private static final long serialVersionUID = 1L;
	
	/** The observed subject. Can be null */
	private SampledTrackModel trackModel = null;
	
	/** The mix to use for the observed track. */
	private TrackMixSubject trackMix = null;
	
	/** The track sequence currently playing. Nullified when playback is stopped. */
	private TrackSequence currentTrackSequence = null;
	
	/** Negative = no playback */
	private int currentPlaybackFramePosition = -1;

	private WaveFormRenderProccessingUnit.WaveFormRenderTask renderTask = null;
	private BufferedImage backBuffer = null; // Shared resource
	private int bufferWidth = -1; // cached from backBuffer - so don't have to lock
	private int bufferHeight = -1; // cached from backBuffer - so don't have to lock
	
	// Model Data
	private int timescaleFrameStart = 0; // in frames
	private int timescaleFrameLength = 0; // in frames
	
	private final Font RENDER_MESSAGE_FONT = new Font("Arial", Font.ITALIC | Font.BOLD, 14);
	
	private boolean alwaysFullViewOn = false;
	
	public static final Color DEFAULT_BACKGROUND_COLOR = new Color(200, 200, 200);
	public static final Color DEFAULT_BACKGROUND_HIGHTLIGHTS_COLOR = new Color(240, 240, 240);
	
	private Color backColor = DEFAULT_BACKGROUND_COLOR;
	private Color backColorHighlights = DEFAULT_BACKGROUND_HIGHTLIGHTS_COLOR;
	
	public static final Color PLAYBACK_BAR_COLOR = new Color(225, 187, 15);
	
	/** The stroke used for drawing graph bars. E.G: The selection Start bar. */
	public static final Stroke GRAPH_BAR_STROKE = Strokes.SOLID_1;
	public static final int GRAPH_BAR_NWIDTH = 1;
	
	/** Used for safely invalidating playback positions at realtime. */
	private Point expediteePosition;
	
	private LinkedList<BackSection> backingSections = new LinkedList<BackSection>();
	private EffecientInvalidator invalidator = null;
	private List<TimelineChangeListener> timelineChangeListeners = new LinkedList<TimelineChangeListener>();
	
	public SampledTrackGraphView() {
		
		// Keep backings consistant with component resize
		this.addComponentListener(new ComponentListener() {
			public void componentHidden(ComponentEvent e) {
			}

			public void componentMoved(ComponentEvent e) {
				//updateExpediteePoint(); // Doesn;t work here - swing has nasty bug
			}

			public void componentResized(ComponentEvent e) {
				
				if (SampledTrackGraphView.this.trackModel != null) {
	
					for (BackSection bs : backingSections) {
						bs.updateBounds();
					}
				}
			}

			public void componentShown(ComponentEvent e) {
				//updateExpediteePoint(); // Doesn;t work here - swing has nasty bug
			}
		
		});
		

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		updateExpediteePoint(); // work around for Swing bug!
	}


	/**
	 * Saves this views position in expeditee space.
	 */
	private void updateExpediteePoint() {
		if (Browser._theBrowser == null) return;
		
		expediteePosition = SwingUtilities.convertPoint(
				this, 
				0, 0, 
				Browser._theBrowser.getContentPane());
	}
	
	/**
	 * Take care: can be null if not intialized.
	 * @return
	 * 		The point at which this view is in a expeditee frame.
	 */
	protected Point getExpediteePoint() {
		return expediteePosition;
	}

	public void addTimeScaleChangeListener(TimelineChangeListener tcl) {
		if (!timelineChangeListeners.contains(tcl))
			timelineChangeListeners.add(tcl);
	}
	
	protected void fireTimelineChanged() {
		Event e = new Event(this, 0, null);
		for (TimelineChangeListener listener : timelineChangeListeners)
			listener.timelineChanged(e);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Subject getObservedSubject() {
		return trackModel;
	}
	
	
	/**
	 * @return
	 * 		The observed track model. Null if none is being viewed.
	 */
	protected SampledTrackModel getSampledTrackModel() {
		return trackModel;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setObservedSubject(Subject parent) {
		
		// Note: parent can be an observer task. But that type of parent will never
		// send null.
		if (parent == null) { // Reset view to view nothing
			trackModel = null;
			if (trackMix != null) // avoid infinite recursion (deadlock until out of memory)
				setMix(null); // stop observing the mix desk - otherwise this reference will never be garbage collected
			releaseBuffer();
			
		} else if (parent instanceof SampledTrackModel) { // Prepares the view

			trackModel = (SampledTrackModel)parent;
			
			if (alwaysFullViewOn) {
				timescaleFrameStart = 0;
				timescaleFrameLength = trackModel.getFrameCount();
				fireTimelineChanged();
			} else  {
				// Ensure that the timeline is valid - even though will probably change
				clampCurrentTimescale();
			}
		} 
		
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modelChanged(Subject source, SubjectChangedEvent event) {
		if (trackModel == null) return;
		

		switch(event.getID()) {
		
			// Invalidate dirty graph - according to render proccessor
			case ApolloSubjectChangedEvent.RENDER_TASK_INVALIDATION_RECOMENDATION: // event from render proccessor
				invalidateAll();
				break;

			case ApolloSubjectChangedEvent.AUDIO_REMOVED: // event from track model
			case ApolloSubjectChangedEvent.AUDIO_INSERTED: // event from track model
				if (alwaysFullViewOn) {
					timescaleFrameStart = 0;
					timescaleFrameLength = trackModel.getFrameCount();
					fireTimelineChanged();
				} else  {
					// Ensure that the timeline is valid - even though will probably change
					clampCurrentTimescale();
				}
				redrawBuffer();
				break;
				
			case ApolloSubjectChangedEvent.TRACK_SEQUENCE_CREATED: // event from mix desk
				assert(trackMix != null);
				assert(event.getState() != null);
				
				// OK a track sequence has just been created, but is it for this
				// track mix?
				if (event.getState().equals(trackMix.getChannelID())) {
					
					// If so - then observe the created track sequence for playback messages
					TrackSequence ts = SoundDesk.getInstance().getTrackSequence(trackMix.getChannelID());
					assert(ts != null);
					ts.addObserver(this); // NOTE: No need to remove self since these are garbage collected rather quickly
				}
				
				break;
				
			case ApolloSubjectChangedEvent.PLAYBACK_STARTED: // event from track sequence
				
				// Important to set here - since a track can be created but fail to initiate
				// playback ... thus must set reference only when gauranteed that it can be unset.
				currentTrackSequence = (TrackSequence)source;
				
				PlaybackClock.getInstance().addPlaybackClockListener(this); // listen to ticks
				
				onPlaybackStarted();
				break;
				
			case ApolloSubjectChangedEvent.PLAYBACK_STOPPED: // event from track sequence
				PlaybackClock.getInstance().removePlaybackClockListener(this); // stop listening to ticks
				
				int playbackFramePos = -1;
				
				// The stop event might be due to a pause:
				if (currentTrackSequence != null && trackMix != null && 
						SoundDesk.getInstance().isPaused(trackMix.getChannelID())) {
					playbackFramePos = currentTrackSequence.getSuspendedFrame();
				}
				
				// Has the user edited the audio such that the playback position is invalid?
				if (playbackFramePos > trackModel.getFrameCount()) 
					playbackFramePos = -1; 
					
				// Note: although these nullify the audio bytes when stopped, the state
				// information carries a reference to the original track model at the moment
				// which stores a lot of data and thus keeping the reference could
				// hog the memory.
				currentTrackSequence = null;

				updatePlaybackPosition(playbackFramePos);

				onPlaybackStopped();
				break;
				
				
			case ApolloSubjectChangedEvent.PAUSE_MARK_CHANGED:

				if (trackMix != null && event.getState().equals(trackMix.getChannelID())) {
					
					if (!SoundDesk.getInstance().isPaused(trackMix.getChannelID())) {
						updatePlaybackPosition(-1);
					}
					
				}
				break;

		}
	}
	
	protected void onPlaybackStarted() {}
	protected void onPlaybackStopped() {}
	
	/**
	 * @return
	 * 		True if this view is viewing a track sequence that is playing
	 */
	public final boolean isPlaying() {
		return (currentTrackSequence != null && currentTrackSequence.isPlaying());
	}
	
	private int getPlaybackXPos(int playbackFrame) {
		
		if (playbackFrame >= timescaleFrameStart && 
				playbackFrame <= (timescaleFrameStart + timescaleFrameLength)) {
			
			return XatFrame(playbackFrame);
		}

		return -1;
	}
	
	private void updatePlaybackPosition(int absFramePos) {

		if (currentPlaybackFramePosition != absFramePos) {

			int height = getHeight();
			int viewWidth = getWidth();
			int x;

			// Invalidate old pos
			if(expediteePosition != null) {
			 
				x = getPlaybackXPos(currentPlaybackFramePosition);
				
				if (x >= 0 && x <= viewWidth) { // is the playback position in view?
					FrameGraphics.invalidateArea(new Rectangle(
						x + expediteePosition.x, 
						expediteePosition.y, 
						GRAPH_BAR_NWIDTH, 
						height));
				}
			}

			// Set new pos
			currentPlaybackFramePosition = absFramePos;
			
			// Invalidate new pos
			if(expediteePosition != null && currentPlaybackFramePosition >= 0) {
				
				x = getPlaybackXPos(currentPlaybackFramePosition);
				
				if (x >= 0 && x <= viewWidth) { // is the playback position in view?
					FrameGraphics.invalidateArea(new Rectangle(
						x + expediteePosition.x , 
						expediteePosition.y, 
						GRAPH_BAR_NWIDTH, 
						height));

				}
			}

		}
		
		
	}

	/**
	 * {@inheritDoc}
	 */
	public void onTick(long framePosition, long msPosition) {
		
		if (framePosition < 0) return; // Done
		
		// The frame position / ms position is according to the software mixers timeline - not
		// neccessarily starting off when the observed track sequence started. Thus must
		// determine the actual frame position relative to the current rack sequence being played
		// for this view:
		int fpos = (int)(framePosition - currentTrackSequence.getInitiationFrame() + currentTrackSequence.getStartFrame());
		if (fpos > currentTrackSequence.getEndFrame()) fpos = currentTrackSequence.getEndFrame();
		updatePlaybackPosition(fpos);
		
		// Note: the clock issues render requests
	}

	/**
	 * Sets the mix to use for playback / and to identify the channel for observering
	 * live playback...
	 * 
	 * This resets to null whenever this view is set to no longer observe a track model.
	 * 
	 * @param trackMix
	 * 		The new track mix to use. Null to use none.
	 * 
	 */
	public void setMix(TrackMixSubject trackMix) {
		
		this.trackMix = trackMix;

		if (trackMix != null) {
			// Listen for track seq creation events ...
			// thus if a track seq is created for this new ID then we can listen
			// for playback events and render playback bars.
			SoundDesk.getInstance().addObserver(this);
			
			// If the mix is already playing - switch to a playing state.
			if (SoundDesk.getInstance().isPlaying(trackMix.getChannelID())) {
				
				currentTrackSequence = SoundDesk.getInstance().getTrackSequence(trackMix.getChannelID());
				
				if (currentTrackSequence != null) {
					
					// Note: that there are no race conditions because track sequence events
					// are riased on this thread.
					//currentPlaybackFramePosition = currentTrackSequence.getCurrentFrame(); // no invalidation
					currentTrackSequence.addObserver(this);
					PlaybackClock.getInstance().addPlaybackClockListener(this);
					onPlaybackStarted();
					
				} 
				
			} else if (SoundDesk.getInstance().isPaused(trackMix.getChannelID())){
				
				int suspos = SoundDesk.getInstance().getLastPlayedFramePosition(trackMix.getChannelID());
				updatePlaybackPosition(suspos);
				
			}
			
		} else {
			SoundDesk.getInstance().removeObserver(this);
		}
		
	}
	
	/**
	 * @return
	 * 		The mix set to be used for this view. Can be null.
	 */
	public TrackMixSubject getMix() {
		return trackMix;   
	}

	public void setAlwaysFullView(boolean alwaysFullViewOn) {
		this.alwaysFullViewOn = alwaysFullViewOn;
	}
	                    
	private void clampCurrentTimescale() {
		int[] newTimeScale = getClampedTimeScale(timescaleFrameStart, timescaleFrameLength);
		
		if (timescaleFrameStart !=  newTimeScale[0]
		   || timescaleFrameLength !=  newTimeScale[1]) {
			
			timescaleFrameStart = newTimeScale[0];
			timescaleFrameLength = newTimeScale[1];
			fireTimelineChanged();
		}
	}
	
	/**
	 * Calculates an appropriate timescale from the given start and length.
	 * 
	 * trackModel Must not be null.
	 * 
	 * @param frameStart
	 * 
	 * @param frameLength
	 * 
	 * @return 
	 * 		Array of two ints. First element is clamped start frame,
	 * 		The second element is the clamped length.
	 */
	private int[] getClampedTimeScale(int frameStart, int frameLength) {
		assert(trackModel != null);
		
		if (frameLength < 1) frameLength = 1;

		if (frameLength > trackModel.getFrameCount()) {
			
			frameLength = trackModel.getFrameCount();
			frameStart = 0;
			
		} else if (frameStart < 0) { // watch out for start 
				
			frameStart = 0;
			
		} else if ((frameStart + frameLength) > trackModel.getFrameCount()) { // and end
				
			frameStart = trackModel.getFrameCount() - frameLength;
			
		}

		assert (frameStart >= 0);
		assert ((frameStart + frameLength) <= trackModel.getFrameCount());

		return new int[] {frameStart, frameLength};
	}

	/**
	 * Sets the time scale for the graph viewport. 
	 * As a result, the graph will be re-rendered asynchronously.
	 * 
	 * @param frameStart
	 * @param frameLength
	 */
	public void setTimeScale(int frameStart, int frameLength) {
		
		int[] newScale = getClampedTimeScale(frameStart, frameLength);

		if (newScale[0] == timescaleFrameStart && newScale[1] == timescaleFrameLength) 
			return;
		
		// Set new model data
		timescaleFrameStart = newScale[0];
		timescaleFrameLength = newScale[1];
		
		// Re-render graph with new time scale
		redrawBuffer();
		
		fireTimelineChanged();
	}
	
	/**
	 * @return
	 * 		The start <i>frame</i> of the current timescale.
	 * 		Always in valid position if subject is set.
	 * 		
	 */
	public int getTimeScaleStart() {
		return timescaleFrameStart;
	}
	
	/**
	 * @return
	 * 		The length of the timescale in <i>frames</i>.
	 * 		Always larger or equal to one if subject is set.
	 */
	public int getTimeScaleLength() {
		return timescaleFrameLength;
	}
	
	/**
	 * Re-renders the waveform buffers to match the graph size if it needs to.
	 * This will avoid pixelation artifacts.
	 */
	public void updateBuffer() {
		// Check for resize of view or if not initialized yet
		if (bufferWidth != getWidth() || bufferHeight != getHeight() || backBuffer == null) {
			redrawBuffer();
		}
	}

	/**
	 * Releases the image buffer.
	 * 
	 * <b>Important:</b> Must call on the swing thread.
	 */
	public void releaseBuffer() {

		if (renderTask != null && !renderTask.isStopped())
			WaveFormRenderProccessingUnit.getInstance().cancelTask(renderTask);
		
		renderTask = null; // must be on swing thread because could be painting
		if (backBuffer != null) backBuffer.flush();
		backBuffer = null; // must be on swing thread because could be painting
	}

	/**
	 * Initiates redrawing. If currently redrawing then it will be cancelled and will start from scratch.
	 * Re-creates the waveform buffer based on the panels current width and height.
	 * 
	 * The graph panel will be fully invalidated
	 * 
	 * If the trackModel is not set, then the buffer will not be redrawn.
	 * 
	 */
	protected void redrawBuffer() {
		
		if (trackModel == null)
			return;

		int width = getWidth();
		int height = getHeight();

		if (width <= 0 || height <= 0) return;
		
		// Remember width for re-rendering when graph size changes
		bufferWidth = width;
		bufferHeight = height;
		
		// Cancel current rendering task
		if (renderTask != null && !renderTask.isStopped())
			WaveFormRenderProccessingUnit.getInstance().cancelTask(renderTask);
		
		// Re-create the back buffer if need to
		Object bufferLock = (backBuffer == null) ? new Object() : backBuffer;
		synchronized(bufferLock) {

			if (backBuffer == null ||
					backBuffer.getWidth() != width
					|| backBuffer.getHeight() != height) {
				
				// Create new sized buffer
				backBuffer = new BufferedImage(
						width, 
						height,
						BufferedImage.TYPE_BYTE_INDEXED,
						ApolloColorIndexedModels.graphIndexColorModel);
			}
		}

		
		// Describe render task
		renderTask = WaveFormRenderProccessingUnit.getInstance().new WaveFormRenderTask(
				backBuffer, 
				trackModel.getAllAudioBytes(),
				trackModel.getFormat(),
				timescaleFrameStart,
				timescaleFrameLength,
				true
				);
		
		// Wait for invalidation recomendation messages
		renderTask.addObserver(this);

		// Begin rendering ASAP
		WaveFormRenderProccessingUnit.getInstance().queueTask(renderTask);
	}

	public void invalidateAll() { 
		invalidate(this.getBounds());
	}

	/**
	 * @param dirty
	 * 		The diry area with respect to this views origin... may want to
	 * 		do an efficient invalidation.
	 * 		For example if the parent of this is an iwidgets top most container
	 * 		then may want to explicitly invalidate rather than having to 
	 * 		create an AWT message...
	 */
	protected void invalidate(Rectangle dirty) {
		if (invalidator != null) invalidator.onGraphDirty(this, dirty);
		else super.repaint(); // the slower way
	}
	
	public void setInvalidator(EffecientInvalidator invalidator) {
		this.invalidator = invalidator;
	}

	/**
	 * Adds a backing section to the graph - will be painted once visible.
	 * Does not invalidate the graph.
	 * 
	 * Intended use: One BackSection for the lifetime of the instance.
	 * Just keep updateing the backing section.
	 * 
	 * @param bs
	 */
	protected void addBackingSection(BackSection bs) {
		
		if (!backingSections.contains(bs)) {
			backingSections.add(bs);
		}
	}
	
	protected void removeBackingSection(BackSection bs) {
		backingSections.remove(bs);
	}

	@Override
	public void paint(Graphics g) {
		
		// only performs rendering if widths are different.
		// Also note that this is not invoked when widget is resizing because
		// the widget is painted another way
		updateBuffer(); // First paint will probably initialize the back buffer

		// Draw backing sections
		paintBacking((Graphics2D)g);
		
		// Draw the wave forms
		if (backBuffer != null) { // zoomed scale view
			
			if (renderTask != null && 
					(renderTask.isRendering()
					|| !renderTask.hasStarted())) {
				
				// If rendering or pending - lock the buffer
				synchronized(backBuffer) {
	
					g.drawImage(backBuffer, 
							0, 
							0, 
							getWidth(), 
							getHeight(), 
							null);
					
				}
				
				// .. and give feedback to user about rendering progress
				paintRenderProgress(g);
				
			} else { // Not rendering
			
				// No need to synchronize waveformBuffer because there is no thread accessing it,
				// and a new render thread is always created on this thread.
				g.drawImage(backBuffer, 
						0, 
						0, 
						getWidth(), 
						getHeight(), 
						null);
				
			}
			
		}
		
		// Get the playback bar position
		//int playbackFramePos = currentPlaybackFramePosition;
		
		// If not playing but is suspended....
		/*if (playbackFramePos < 0 && trackMix != null
				&& MixDesk.getInstance().isPaused(trackMix.getChannelID())) {
			
			// Get the last playback pos - can be negitive if not played yet
			playbackFramePos = MixDesk.getInstance().getLastPlayedFramePosition(trackMix.getChannelID());
			
			// Has the user edited the audio such that the playback position is invalid?
			if (playbackFramePos > trackModel.getFrameCount()) 
				playbackFramePos = -1; 
		}*/
		
		// Paint the playback bar
		if (currentPlaybackFramePosition >= 0) {
			
			int x = getPlaybackXPos(currentPlaybackFramePosition);
			
			if (x >= 0 && x <= getWidth()) {
				
				((Graphics2D)g).setStroke(GRAPH_BAR_STROKE);
				g.setColor(PLAYBACK_BAR_COLOR);
				g.drawLine(x, 0, x, getHeight());
			}
			
		}
		
		if (getComponentCount() > 0) {
			paintChildren(g);
		}
		
	}

	private void paintBacking(Graphics2D g) {

		// Clear background
		Paint restore = g.getPaint();
		
		if (ApolloSystem.useQualityGraphics) {

			GradientPaint gp = new GradientPaint(
					(getWidth() / 2), (int)(getHeight() * 0.8), backColor,
					(getWidth() / 2), 0, backColorHighlights);
			g.setPaint(gp);
			
		} else {
			g.setColor(backColor);
		}

		g.fillRect(0, 0, getWidth(), getHeight());

		if (ApolloSystem.useQualityGraphics) {
			g.setPaint(restore);
		}
		
		// Paint sections
		for (BackSection bs : backingSections) {
			bs.paint(g);
		}
		
	}

	private void paintRenderProgress(Graphics g) {
		
		g.setColor(Color.BLACK);
		
		g.setFont(RENDER_MESSAGE_FONT);
		
		String message = (renderTask.isRendering()) ? "Rendering..." : "Pending...";
/*
		Area clip = FrameGraphics.getCurrentClip();
		Shape clipBackUp = g.getClip();
		Area tmpClip = (clip != null) ? clip : 
			new Area(new Rectangle(0, 0, 
					Browser._theBrowser.getContentPane().getWidth(), 
					Browser._theBrowser.getContentPane().getHeight()));
		
		Rectangle r = getBounds();
		r.translate(expediteePosition.x, expediteePosition.y);
		tmpClip.intersect(new Area(r));*/

	//	if (!tmpClip.isEmpty()) {
			
			//g.setClip(tmpClip);
			g.drawString(message, (getWidth() / 2) - 40, 
					(getHeight() / 2) + (RENDER_MESSAGE_FONT.getSize() / 2)); // center roughly
		//	g.setClip(clipBackUp);
		//}

	}

	

	/**
	 * At a given x position in the graph, the matching frame number is returned.
	 * This considers the current timescale
	 * 
	 * @param x
	 * 
	 * @return Frame in track model
	 */
	public int frameAtX(int x) {
		return frameAtX(x, timescaleFrameStart, timescaleFrameLength);
	}
	
	private int frameAtX(int x, int frameStart, int frameLength) {
		
		double viewPortPercent = x;
		viewPortPercent /= getWidth();
		
		// clamp
		int frame = (int)(frameLength * viewPortPercent);
		frame += frameStart;
		
		if (frame >= (frameStart + frameLength)) 
			frame = frameStart + frameLength;
		else if(frame < frameStart) 
			frame = frameStart;

		return frame;
	}
	
	

	/**
	 * At a given frame position in the track model, the matching x coordinate in the graph is returned.
	 * This considers the current timescale. Clamped to graph bounds.
	 * 
	 * @param frame
	 * 
	 * @return X pos in graph
	 */
	public int XatFrame(long frame) {
		return XatFrame(frame, timescaleFrameStart, timescaleFrameLength);
	}
	
	private int XatFrame(long frame, long frameStart, long frameLength) {
		
		double framePercent = frame - frameStart;
		framePercent /= frameLength;
		
		int x =  (int)(getWidth() * framePercent);
		
		// clamp
		if (x > getWidth()) x = getWidth();
		else if (x < 0) x = 0;

		return x;
	}
	
	
	/**
	 * A back section of a graph... intended for ranged highlighting.
	 * 
	 * @author Brook Novak
	 */
	protected class BackSection {

		int left;
		
		/** If less or equal to 1, then nothing is drawn. */
		int width;
		
		/** A flag for showing/hiding backing */
		boolean visible;
		Color color;
		
		BackSection() {
			left = 0;
			width = 0;
			visible = false;
			color = Color.BLACK;
		}
	
		void paint(Graphics g) {
			if (visible && width > 1) {
				g.setColor(color);
				g.fillRect(left, 0, width, getHeight());
			}
		}
		
		/**
		 * Invoked when panel resized
		 */
		void updateBounds() {
		}
		
	}
	
	/**
	 * A SampledTrackGraphView can have a EffecientInvalidator where instead of
	 * invalidating via swing AWT event it invalidates via the implementor of
	 * a EffecientInvalidator if one is set.
	 * 
	 * @author Brook Novak
	 *
	 */
	public interface EffecientInvalidator {
		public void onGraphDirty(SampledTrackGraphView graph, Rectangle dirty);
	}

	public Color getBackColor() {
		return backColor;
	}

	/**
	 * Changes the background color and invalidates the graph.
	 * Doesnt invalidate if the color has not changed.
	 * 
	 * @param backColor
	 * 		The new Background color. Must not be null.
	 * 	
	 */
	public void setBackColor(Color backColor, Color highlights) {
		setBackColor(backColor, highlights, true);
	}
	
	/**
	 * Changes the background color.
	 * 
	 * @param backColor
	 * 		The new Background color. Must not be null.
	 * 
	 * @param invalidate
	 * 		True to invalidate the graph. False to miss invalidation.
	 * 		Doesnt invalidate if the color has not changed.
	 */
	public void setBackColor(Color backColor, Color highlights, boolean invalidate) {
		if (backColor == null) throw new NullPointerException("backColor");
		if (this.backColor != backColor || backColorHighlights != highlights) {
			this.backColor = backColor;
			backColorHighlights = highlights;
			if (invalidate) invalidateAll();
		}
	}

	
	
	

}
