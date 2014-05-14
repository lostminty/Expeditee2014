package org.apollo.widgets;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.apollo.ApolloSystem;
import org.apollo.AudioFrameKeyboardActions;
import org.apollo.AudioFrameMouseActions;
import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.SampledAudioManager;
import org.apollo.audio.structure.AbsoluteTrackNode;
import org.apollo.audio.structure.AudioStructureModel;
import org.apollo.audio.structure.LinkedTracksGraphNode;
import org.apollo.audio.structure.OverdubbedFrame;
import org.apollo.audio.structure.TrackGraphLoopException;
import org.apollo.audio.util.MultiTrackPlaybackController;
import org.apollo.audio.util.PlaybackClock;
import org.apollo.audio.util.SoundDesk;
import org.apollo.audio.util.Timeline;
import org.apollo.audio.util.TrackMixSubject;
import org.apollo.audio.util.MultiTrackPlaybackController.MultitrackLoadListener;
import org.apollo.audio.util.PlaybackClock.PlaybackClockListener;
import org.apollo.gui.ExpandedTrackManager;
import org.apollo.gui.FrameLayoutDaemon;
import org.apollo.gui.PlaybackControlPopup;
import org.apollo.gui.SampledTrackGraphView;
import org.apollo.gui.Strokes;
import org.apollo.io.AudioPathManager;
import org.apollo.io.IconRepository;
import org.apollo.items.EmulatedTextItem;
import org.apollo.items.EmulatedTextItem.TextChangeListener;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.ApolloSystemLog;
import org.apollo.util.AudioMath;
import org.apollo.util.Mutable;
import org.apollo.util.ODFrameHeirarchyFetcher;
import org.apollo.util.PopupReaper;
import org.apollo.util.StringEx;
import org.apollo.util.ODFrameHeirarchyFetcher.ODFrameReceiver;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.MessageBay;
import org.expeditee.gui.MouseEventRouter;
import org.expeditee.gui.PopupManager;
import org.expeditee.items.Item;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.InteractiveWidgetInitialisationFailedException;
import org.expeditee.items.widgets.InteractiveWidgetNotAvailableException;

public class LinkedTrack extends InteractiveWidget implements ODFrameReceiver, Observer, MultitrackLoadListener, PlaybackClockListener {
	
	private String virtualFilename; // immutable 
	
	private TrackMixSubject masterMix; // immutable
	
	private OverdubbedFrame linkedOverdubbedFrame = null;
	
	// NOTE: Using meta data as name model data - since there are two views for the name cannot store in a single label
	private EmulatedTextItem nameLabel = null; 
	
	private PlaybackPopup playbackControlPopup = null;


	// private SoundDeskPopup soundDeskPopup = null;
	private MouseActions mouseActions = new MouseActions();
	private MasterControlActionListener masterActionListener = new MasterControlActionListener();
	
	/** All the track information for playing / editing / rendering.
	 * They ordered such that they are grouped by localfilename */
	private List<Track> tracks = new LinkedList<Track>();
	private int uniqueLocalTrackCount = 0; // the amount of different local filenames referenced by this track
	private long totalFrameLength = 0;
	private int currentPlaybackFramePosition = -1;
	
	
	private int selectionStartX = 0;
	private int selectionStart = 0;
	private int selectionLength = -1;
	
	
	private int state = NOT_INITIALIZED;
	private int pendingGraphFetchCount = 0; // used for discarding redundant graph fetches
	private String failMessage = null; // shown if in a failed state
	private String abortMessage = null;
	
	private static final Color FAILED_MESSAGE_COLOR = Color.RED;
	private static final Font MESSAGE_FONT = TrackWidgetCommons.FREESPACE_TRACKNAME_FONT;
	private static final Font HELPER_FONT = new Font("Arial", Font.BOLD, 12);
	
	private static final Color[] TRACK_COLOR_WHEEL = { 
							new Color(66, 145, 255), 
							new Color(255, 163, 33), 
							new Color(155, 227, 48), 
						};
	private static final Color TRACK_LOAD_COLOR = Color.BLACK;
	private static final Color TRACK_FAIL_COLOR = Color.RED;
	
	private static final Color SELECTION_COLOR = new Color(0,0,0,100);

	private static final Stroke TRACK_BORDER = Strokes.SOLID_1;
	private static final Stroke TRACK_SELECTED_BORDER = Strokes.SOLID_2;
	private static final Color TRACK_BORDER_COLOR = Color.BLACK;
	private static final Color TRACK_SELECTED_BORDER_COLOR = new Color(253, 255, 201);
	
	
	/** States - mutex in this widget but not really .. for example since can be loading tracks and the graph at the same time... */
	private  static final int NOT_INITIALIZED = 0; // Not showing...
	private  static final int LOADING_TRACK_GRAPH = 1; // Loading the track layout information
	private  static final int FAILED_STATE = 2; // e.g. Bad graph... cannot use
	private  static final int STOPPED = 3; // Graph loaded and ready to play.
	private  static final int PLAYBACK_LOADING = 4; // Loading tracks from file/cache to play/resume
	private  static final int PLAYING = 5; // Playing audio.
	private  static final int EMPTY_STATE = 6; // e.g. No link set OR points to noththing to play
	
	/** For parsing metadata */
	public static final String META_VIRTUALNAME_TAG = "vn=";
	
	/**
	 * Constructor called by Expeditee. 
	 * 
	 * @param source
	 * @param args
	 */
	public LinkedTrack(Text source, String[] args) {
		super(source, new JPanel(), 
				100, -1, 
				FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT);

		// Read the metadata
		virtualFilename = getStrippedDataString(META_VIRTUALNAME_TAG);

		if (virtualFilename == null) { // virtualFilename is immutable. Must set on construction.
			virtualFilename = AudioPathManager.generateVirtualFilename();
				updateData(META_VIRTUALNAME_TAG, META_VIRTUALNAME_TAG + virtualFilename);
		}
		
		masterMix = SoundDesk.getInstance().getOrCreateMix(SoundDesk.createMasterChannelID(this));

		// Set widget as a fixed size widget- using width from last recorded width in the meta data.
		int width = getStrippedDataInt(TrackWidgetCommons.META_LAST_WIDTH_TAG, -1);
		if (width >= 0 && width < FrameLayoutDaemon.MIN_TRACK_WIDGET_WIDTH) {
			width = FrameLayoutDaemon.MIN_TRACK_WIDGET_WIDTH;
		}
		
		if (width < 0) {
			setSize(-1, -1, 
					FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, 
					FrameLayoutDaemon.TRACK_WIDGET_DEFAULT_WIDTH, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT);
		} else {
			setSize(-1, -1, 
					FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, 
					width, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT);
		}
		
		playbackControlPopup = new PlaybackPopup();
		
		nameLabel = new EmulatedTextItem(_swingComponent, new Point(10, 20));
		nameLabel.setBackgroundColor(Color.WHITE);	
		
		String metaName = getStrippedDataString(TrackWidgetCommons.META_NAME_TAG);
		if (metaName == null) metaName = "Untitled";
		nameLabel.setText(metaName); 

		nameLabel.addTextChangeListener(new TextChangeListener() { 

			public void onTextChanged(Object source, String newLabel) {
	
				if (state != PLAYING && state != STOPPED) return;
				
				setName(nameLabel.getText());

			}

		});
		
		_swingComponent.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				if ((state != PLAYING && state != STOPPED) || e.isControlDown()) return;
				if (nameLabel.onKeyPressed(e, _swingComponent)) {
					e.consume();
					return;
				}
			}

			public void keyReleased(KeyEvent e) {
				if ((state != PLAYING && state != STOPPED)) return;
				else if (e.isControlDown()) {
					if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
						AudioFrameKeyboardActions.adjustInitationTime(LinkedTrack.this, e.getKeyCode() == KeyEvent.VK_LEFT);
					} else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
						AudioFrameKeyboardActions.adjustVerticlePosition(LinkedTrack.this, e.getKeyCode() == KeyEvent.VK_UP);
					}
				} else if (nameLabel.onKeyReleased(e, _swingComponent)) {
					e.consume();
					return;
				}
			}

			public void keyTyped(KeyEvent e) {
			}
			
		});
		
		
		updateBorderColor();

		setWidgetEdgeThickness(TrackWidgetCommons.STOPPED_TRACK_EDGE_THICKNESS);
		
		
	}
	
	/**
	 * Must set naem via this centralized method.
	 * @param newName
	 */
	private void setName(String newName) {

		String modelLabel = LinkedTrack.this.getStrippedDataString(TrackWidgetCommons.META_NAME_TAG);
		
		if (!StringEx.equals(modelLabel, newName)) {
			updateData(TrackWidgetCommons.META_NAME_TAG, newName);
		} 
		
		if (!StringEx.equals(nameLabel.getText(), newName)) {
			nameLabel.setText(newName);
		}
			
//		if (soundDeskPopup != null && 
//				!StringEx.equals(soundDeskPopup.nameLabel.getText(), newName)) {
//			soundDeskPopup.nameLabel.setText(newName);
//		}
				
		// Keep graph model consistant
		
		Frame parent = getParentFrame();
		
		AudioStructureModel.getInstance().onLinkedTrackWidgetNameChanged(
				virtualFilename, 
				(parent != null) ? parent.getName() : null, 
				newName);
		
	}
	
	public boolean shouldLayout() {
		return (state != EMPTY_STATE && state != FAILED_STATE);
	}
	
	/**
	 * Sets the GUI and all the popups to reflect the current state.
	 * Must be on the swing thread.
	 * 
	 * @param newState
	 * 			The new state. The same state then it is ignored, unless
	 * 			its LOADING_TRACK_GRAPH - in that case the graph is reloaded.
	 */
	private void setState(int newState) {
		if (this.state == newState && newState != LOADING_TRACK_GRAPH) return; // no need to process request.
		
		//assert ((state == NOT_INITIALIZED && !isVisible()) || 
		//		(state != NOT_INITIALIZED && isVisible()));
		
		evaluate_state:
			
		switch(newState) {
		case NOT_INITIALIZED: // became invisible
			
			playbackControlPopup.stopButton.setEnabled(false);
			playbackControlPopup.rewindButton.setEnabled(false);
			playbackControlPopup.playPauseButton.setEnabled(false);
			tracks.clear();
			break;
			
		case LOADING_TRACK_GRAPH:
			
			// Note: Can be invoked even if already in this state
			
			// Get the frame that this widget is linking to
			String linkedFrame = getAbsoluteLink(); // must be in famename form
			
			if (linkedFrame != null) {
				
				// Request a fetch from the model - eventually will return a result
				// and the graph image will be build
				pendingGraphFetchCount++;
				ODFrameHeirarchyFetcher.getInstance().doFetch(linkedFrame, this);
				// Once finished the callback will construct the model data / graph.
			}  else {
				// No link - switch state to empty
				newState = EMPTY_STATE;
				break evaluate_state; // re-eval
			}
			
			playbackControlPopup.stopButton.setEnabled(false);
			playbackControlPopup.rewindButton.setEnabled(false);
			playbackControlPopup.playPauseButton.setEnabled(false);

			break;
			
		case FAILED_STATE: 
			assert(failMessage != null);
			playbackControlPopup.stopButton.setEnabled(false);
			playbackControlPopup.rewindButton.setEnabled(false);
			playbackControlPopup.playPauseButton.setEnabled(false);
			
			setSize(100, -1, 
					FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, 
					200, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT);
			
			break;
			
		case STOPPED:
			playbackControlPopup.rewindButton.setEnabled(true);
			playbackControlPopup.stopButton.setEnabled(false);
			playbackControlPopup.playPauseButton.setEnabled(true);
			playbackControlPopup.playPauseButton.setIcon(IconRepository.getIcon("play.png"));
		

			setWidgetEdgeThickness(TrackWidgetCommons.STOPPED_TRACK_EDGE_THICKNESS);
			
			break;
			
		case PLAYBACK_LOADING: 
			playbackControlPopup.stopButton.setEnabled(false);
			playbackControlPopup.rewindButton.setEnabled(false);
			playbackControlPopup.playPauseButton.setEnabled(false);
			break;
			
		case PLAYING: 
			playbackControlPopup.stopButton.setEnabled(true);
			playbackControlPopup.rewindButton.setEnabled(false);
			playbackControlPopup.playPauseButton.setEnabled(true);
			playbackControlPopup.playPauseButton.setIcon(IconRepository.getIcon("pause.png"));

			setWidgetEdgeThickness(TrackWidgetCommons.PLAYING_TRACK_EDGE_THICKNESS);
			
			PlaybackClock.getInstance().addPlaybackClockListener(this); // listen to ticks

			break;
			
		case EMPTY_STATE:
			setSize(100, -1, 
					FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, 
					200, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT);
			playbackControlPopup.stopButton.setEnabled(false);
			playbackControlPopup.rewindButton.setEnabled(false);
			playbackControlPopup.playPauseButton.setEnabled(false);
			break;
			
			default:
				assert(false);

		}
		
		if (newState != PLAYING) { // safety
			PlaybackClock.getInstance().removePlaybackClockListener(this);
		}
		
		// If in empty state then clear mouse listeners to give expeditee events
		if (newState == EMPTY_STATE) {
			_swingComponent.removeMouseListener(mouseActions);
			_swingComponent.removeMouseMotionListener(mouseActions);
			
		} else {
			
			if (_swingComponent.getMouseListeners().length == 0) {
				_swingComponent.addMouseListener(mouseActions);
			}
		
			if (_swingComponent.getMouseMotionListeners().length == 0) {
				_swingComponent.addMouseMotionListener(mouseActions);
			}
		}
		
		state = newState;
		
		invalidateSelf();
		FrameGraphics.refresh(true);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Subject getObservedSubject() {
		return null; // many
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setObservedSubject(Subject parent) {
		// many
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modelChanged(Subject source, SubjectChangedEvent event) {
		
		// Note: no need to check for audio structure model changing since can only change
		// if in another frame.

		// Synch GUI with track state
		switch (event.getID()) {
			
		case ApolloSubjectChangedEvent.PLAYBACK_STARTED:
			
			if (state == PLAYBACK_LOADING && linkedOverdubbedFrame != null && 
				MultiTrackPlaybackController.getInstance().isCurrentPlaybackSubject(
					this.linkedOverdubbedFrame.getFrameName(), masterMix.getChannelID())) {
				setState(PLAYING);
			} 
			break;
			
		case ApolloSubjectChangedEvent.PLAYBACK_STOPPED:
			
			if (state == PLAYING) {
				assert(linkedOverdubbedFrame != null);

				int playbackFramePos = -1;
				
				// The stop event might be due to a pause:
				if (MultiTrackPlaybackController.getInstance().isMarkedAsPaused(
						linkedOverdubbedFrame.getFrameName(), masterMix.getChannelID())) {
					playbackFramePos = MultiTrackPlaybackController.getInstance().getLastSuspendedFrame();
				}
				
				// Has the user edited the audio such that the playback position is invalid?
				if (playbackFramePos > totalFrameLength) 
					playbackFramePos = -1; 
	
				// Set the playback position to the exact suspended frame pos.
				updatePlaybackPosition(playbackFramePos);
				
				// Transition into new state
				setState(STOPPED);

			}
			break;
			
		case ApolloSubjectChangedEvent.VOLUME: // From obseved track mix
			updateVolume();
			break;
			
		case ApolloSubjectChangedEvent.MUTE: // From obseved track mix
			updateMute();
			updateBorderColor();
			break;
			
		case ApolloSubjectChangedEvent.SOLO_PREFIX_CHANGED: // From mix desk
			updateSolo();
			updateBorderColor();
			break;
			
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void onTick(long framePosition, long msPosition) {
		
		if (framePosition < 0) return; // Done
		
		// The frame position / ms position is according to the software mixers timeline - not
		// neccessarily starting off when the observed multi track sequences started. Thus must
		// determine the actual frame position relative to the current rack sequence being played
		// for this view:
		
		int fpos = (int)(framePosition 
				- MultiTrackPlaybackController.getInstance().getLastInitiationFrame()
				+ MultiTrackPlaybackController.getInstance().getLastStartFrame());
		
		// Clamp
		if (fpos > MultiTrackPlaybackController.getInstance().getLastEndFrame())
			fpos = MultiTrackPlaybackController.getInstance().getLastEndFrame();

		updatePlaybackPosition(fpos);

	}

	/**
	 * {@inheritDoc}
	 */
	public void multiplaybackLoadStatusUpdate(int id, Object state) {
		
		// NOTE: Could switch to an unloaded state
		if (this.state != PLAYBACK_LOADING) return;
		
		switch(id) {
		case MultitrackLoadListener.LOAD_CANCELLED:
			setState(STOPPED);
			break;
		case MultitrackLoadListener.LOAD_COMPLETE:
			// Ensure that all tracks area dsiplayed and being loaded.
			for (Track t : tracks) setTrackState(t, TRACKSTATE_READY);
			invalidateSelf();
			FrameGraphics.refresh(true);
			break;
		case MultitrackLoadListener.LOAD_FAILED_BAD_GRAPH:
			abortMessage = "Graph contains loops";
			((Exception)state).printStackTrace();
			break;
		case MultitrackLoadListener.LOAD_FAILED_GENERIC:
			abortMessage = "Unexpected error";
			((Exception)state).printStackTrace();
			break;
		case MultitrackLoadListener.LOAD_FAILED_PLAYBACK:
			abortMessage = "Unable to aquire sound device";
			break;
		case MultitrackLoadListener.NOTHING_TO_PLAY:
			abortMessage = "Nothing to play"; // could be due to user slecting empty space
			break;
		case MultitrackLoadListener.TRACK_LOAD_FAILED_IO:
			// This is special... the loader does not abort... and it tries to load more.
			setTrackState((String) state, TRACKSTATE_FAILED);
			((Exception)state).printStackTrace();
			break;
		case MultitrackLoadListener.TRACK_LOADED:
			setTrackState((String) state, TRACKSTATE_READY);
			invalidateSelf();
			FrameGraphics.refresh(true);
			break;

		}
		
		if (abortMessage != null) {
			ApolloSystemLog.println("Aborted playback - " + abortMessage);
			setState(STOPPED);
		}
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String[] getArgs() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<String> getData() {

		List<String> data = new LinkedList<String>();
		
		data.add(META_VIRTUALNAME_TAG + virtualFilename);
		
		String name = getName();
	
		if (name != null)
			data.add(TrackWidgetCommons.META_NAME_TAG + name);

		data.add(TrackWidgetCommons.META_LAST_WIDTH_TAG + getWidth());
		
		Mutable.Long initTime = null;
		
		Frame f = getParentFrame();
		if (f != null) {
			LinkedTracksGraphNode ltinf = AudioStructureModel.getInstance().getLinkedTrackGraphInfo(virtualFilename, f.getName());
			if (ltinf != null) {
				initTime = Mutable.createMutableLong(ltinf.getInitiationTime());
			}
		}
		
		if (initTime == null) initTime = getInitiationTimeFromMeta(); // old meta
		if (initTime == null) initTime = Mutable.createMutableLong(0L);

		data.add(TrackWidgetCommons.META_INITIATIONTIME_TAG + initTime);

		return data;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public InteractiveWidget copy() 
		throws InteractiveWidgetNotAvailableException, InteractiveWidgetInitialisationFailedException {
		
		Item source = getCurrentRepresentation().copy();
		String clonedAnnotation = getAnnotationString();
		source.setText(clonedAnnotation);
		
		// But since this is a copy: must allocate a new virtual filename of its own...
		// thus remove the vname meta
		List<String> data = getData();

		for (int i = 0; i < data.size(); i++) {
			String str = data.get(i);
			if (str.trim().startsWith(META_VIRTUALNAME_TAG)) {
				data.remove(i);
				i--; // continue - super safety to get rid of any hint of vname meta
			}
		}

		source.setData(data);
		
		return InteractiveWidget.createWidget((Text)source);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onParentStateChanged(int eventType) {
		super.onParentStateChanged(eventType);
		
		Frame parent = this.getParentFrame();
		String parentName = (parent != null) ? parent.getName() : null;

		switch (eventType) {

		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
			
			// Resume any layouts suspended by this track widget
			FrameLayoutDaemon.getInstance().resumeLayout(this);
			
			// Update model.
			String link = getAbsoluteLink();

			// Determine new initation time according to anchored position...
			Mutable.Long initTime = getInitiationTimeFromMeta();
			
			// If the user is restricting-y-axis movement then they might be moving 
			// this tracks Y-position only for layout reasons as opposed to repositioning
			// where in the audio timeline the track should be. This must be accurate and
			// avoid loosing the exact initiation time due to pixel-resolution issues
			if (parent != null) {
				
				boolean inferInitTime = true;
	
				if (AudioFrameMouseActions.isYAxisRestictionOn()) {
					Mutable.Long ms = getInitiationTimeFromMeta();
					if (ms != null) {
						
						Mutable.Long timex = FrameLayoutDaemon.getInstance().getXAtMS(
								ms.value, 
								parent);
						
						if (timex != null && timex.value == getX()) {
							initTime = ms;
							inferInitTime = false;
						}
					}
				}
				
				// Also must not set initiation time if the frame is simply being displayed
				inferInitTime &= 
					(AudioFrameMouseActions.isMouseAnchoring() || AudioFrameMouseActions.isMouseStamping());

				if (inferInitTime) {
					initTime = Mutable.createMutableLong(FrameLayoutDaemon.getInstance().getMSAtX(getX(), parent));
				}
			}
			
			updateData(TrackWidgetCommons.META_INITIATIONTIME_TAG,
					TrackWidgetCommons.META_INITIATIONTIME_TAG + initTime);
			
			if (link != null) {
				
				AudioStructureModel.getInstance().onLinkedTrackWidgetAnchored(
						virtualFilename, 
						parentName, 
						initTime.value,
						link,
						getName(),
						getY());

				// Wakeup the daemon to note that it should recheck -- in the case that a track is
				// added to a non-overdubbed frame the audio structure model will not bother
				// adding the track until something requests for it.
				FrameLayoutDaemon.getInstance().forceRecheck();
			}
			
			// Fall through ... and load the graph
		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY: // TODO revise - and in sampled track widget
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:

			// Setup observers
			SoundDesk.getInstance().addObserver(this);
			MultiTrackPlaybackController.getInstance().addObserver(this);
			masterMix.addObserver(this);
			
			// Load the graph
			setState(LOADING_TRACK_GRAPH);

			break;
			
		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
			
			// If yanking this from the frame into free space suspend the layout daemon
			// for this frame
			if (MouseEventRouter.getCurrentMouseEvent() != null &&
					MouseEventRouter.getCurrentMouseEvent().getButton() == MouseEvent.BUTTON2) {
				Frame suspended = DisplayIO.getCurrentFrame();
				if (suspended != null) {
					FrameLayoutDaemon.getInstance().suspendLayout(suspended, this);
				}
			}
			
			// Update model.
			AudioStructureModel.getInstance().onLinkedTrackWidgetRemoved(
					virtualFilename, 
					parentName);
			
			// Cancel loading of audio
			if (state == PLAYBACK_LOADING) {
				assert(linkedOverdubbedFrame != null);
				MultiTrackPlaybackController.getInstance().cancelLoad(
						linkedOverdubbedFrame.getFrameName(), 
						masterMix.getChannelID());
			}
			
			
			// TODO: Temp workaround - why are these widgets not refreshing?
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					invalidateSelf();
					FrameGraphics.refresh(true);
				}
			}); // NOTE TO REPEAT: Pick up a linked track and delete it directly from freespace

		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY: // TODO revise - and in sampled track widget
		case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
			
			// Remove observers
			SoundDesk.getInstance().removeObserver(this);
			MultiTrackPlaybackController.getInstance().removeObserver(this);
			masterMix.removeObserver(this);
			
			setState(NOT_INITIALIZED);
			break;

		}
		
	}
	
	

	@Override
	public void onDelete() {
		super.onDelete();
		// Resume any layouts suspended by this track widget
		FrameLayoutDaemon.getInstance().resumeLayout(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLink(String link, Text linker) {
		
		String oldABSLink = getAbsoluteLink();
		
		super.setLink(link, linker);
		
		String newABSLink = getAbsoluteLink();
		
		if (StringEx.equals(oldABSLink, newABSLink)) return;
		
		Frame parent = this.getParentFrame();
		String parentName = (parent != null) ? parent.getName() : null;
		
		// Changing the link is the same as removing a track-link in the graph model (if had one)
		AudioStructureModel.getInstance().onLinkedTrackWidgetRemoved(
				virtualFilename, 
				parentName);

		// And adding a new one (if applicable)
		if (parent != null && newABSLink != null) {
			
			Mutable.Long initTime = (parent != null) ? 
					Mutable.createMutableLong(FrameLayoutDaemon.getInstance().getMSAtX(getX(), parent)) :
						getInitiationTimeFromMeta();

			if (initTime == null) initTime = Mutable.createMutableLong(0);
			
			String newName = (linker != null && linker.getText() != null) ? linker.getText() : getName();
			if (newName != getName()) {
				setName(newName);
			}

			AudioStructureModel.getInstance().onLinkedTrackWidgetAnchored(
					virtualFilename, 
					parentName, 
					initTime.value, // TODO: Revise: actually would be better to get from model .. if possible
					newABSLink,
					newName,
					getY());

		}
		
		// Reload track graph
		setState(LOADING_TRACK_GRAPH);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onSizeChanged() {
		super.onSizeChanged();
		
		// Re-adjust track sizes
		updateGraphLayout();
		
	}

	/**
	 * @return
	 * 		The virtual filename. Never null.
	 */
	public String getVirtualFilename() {
		return virtualFilename;
	}

	/**
	 * Determines the running time from the meta data.
	 * 
	 * @return
	 * 		The running time or this track in MS.
	 * 		-1 if unavilable.
	 */
	public long getRunningMSTimeFromMeta() {
		return getStrippedDataLong(TrackWidgetCommons.META_RUNNINGMSTIME_TAG, new Long(-1));
	}
	
	/**
	 * Determines the initiation time from the meta data.
	 * 
	 * @return
	 * 		The initiation time or this track in MS.
	 * 		null if unavilable.
	 */
	public Mutable.Long getInitiationTimeFromMeta() {
		Long l = getStrippedDataLong(TrackWidgetCommons.META_INITIATIONTIME_TAG, null);
		if (l != null) return Mutable.createMutableLong(l.longValue());
		return null;
	}
	

	/**
	 * Adjusts the initiation time for this track - to the exact millisecond.
	 * 
	 * The x-position is updated (which will be eventually done with possibly better
	 * accuracy if the layout daemon is on)
	 * 
	 * @param specificInitTime
	 * 		The new initiation time for this track in milliseconds
	 */
	public void setInitiationTime(long specificInitTime) {
		
		Frame parent = getParentFrame();
		
		// Update x position if it can
		if (parent != null) {
			Timeline tl = FrameLayoutDaemon.getInstance().getTimeline(parent);
			if (tl != null) {
				this.setPosition(tl.getXAtMSTime(specificInitTime), getY());
			}
		}
		
		updateData(TrackWidgetCommons.META_INITIATIONTIME_TAG, 
				TrackWidgetCommons.META_INITIATIONTIME_TAG + specificInitTime);
		
		AudioStructureModel.getInstance().onLinkedTrackWidgetPositionChanged(
			virtualFilename, (parent != null) ? parent.getName() : null, specificInitTime, getY());


	}
	
	public void setYPosition(int newY) {

		if (getY() == newY || isFloating()) return;

		Frame parent = getParentFrame();
		
		Mutable.Long initTime = getInitiationTimeFromMeta();
		
		if (initTime == null) return;
		
		setPosition(getX(), newY);
		
		AudioStructureModel.getInstance().onLinkedTrackWidgetPositionChanged(
				virtualFilename, (parent != null) ? parent.getName() : null, initTime.value, newY);

	}
	
	
	/**
	 * @return
	 * 		The name given to this widget... can be null.
	 */
	public String getName() {
		
		String name = nameLabel.getText();

		if (name == null) {
			name = getStrippedDataString(TrackWidgetCommons.META_NAME_TAG);
		}

		return name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void paint(Graphics g) {
		
		if (Browser._theBrowser == null) return;
		
		// Ensure that load bar or any text doesn't spill over widgets invalidation area
		Area clip = FrameGraphics.getCurrentClip();
		
		Shape clipBackUp = g.getClip();
		
		Area tmpClip = (clip != null) ? clip : 
			new Area(new Rectangle(0, 0, 
					Browser._theBrowser.getContentPane().getWidth(), 
					Browser._theBrowser.getContentPane().getHeight()));
		
		tmpClip.intersect(new Area(getBounds()));

		if (tmpClip.isEmpty()) return;

		g.setClip(tmpClip);
		
		// Paint backing
		g.setColor(SampledTrackGraphView.DEFAULT_BACKGROUND_COLOR);
		g.fillRect(getX(), getY(), getWidth(), getHeight());
		
		String centralizedMessage = null;

		// Render according to the currently widget state
		switch(state) {
		case NOT_INITIALIZED: // became invisible
			break;

		case FAILED_STATE: 
		case EMPTY_STATE:
		case LOADING_TRACK_GRAPH:
			
			if (state == FAILED_STATE) {
				centralizedMessage = failMessage;
				assert(failMessage != null);
				g.setColor(FAILED_MESSAGE_COLOR);
			} else if (state == LOADING_TRACK_GRAPH){
				centralizedMessage = "Loading...";
				g.setColor(Color.DARK_GRAY);
			} else { // empty
				if (getLink() == null) {
					centralizedMessage = "Click to create";
				} else {
					centralizedMessage = "Nothing to play";
				}
				
				g.setColor(Color.DARK_GRAY);
			}

			break;
			
		case STOPPED:
		case PLAYBACK_LOADING: 
		case PLAYING: 
			int x = getX();
			int y = getY();
			Track selected = null;

			for (Track track : tracks) {
				track.paintTrackArea((Graphics2D)g, x, y);
				if (track.isSelected) {
					selected = track;
				} else {
					track.paintTrackBorder(g, x, y);
					
					if (track.area.height > 15) { // don't clutter up busy linked tracks
						drawTrackName(track, g, Color.DARK_GRAY);
					}
	
				}

			}
			
			// Selection can spill over other track areas so to avoid z fighting always ensure that
			// the selected border is drawn last
			if (selected != null) {
				selected.paintTrackBorder(g, x, y);
				drawTrackName(selected, g, Color.BLACK);
			}

			if (abortMessage != null) {
				String name = getName();
				if (name == null) name = "Unamed";
				MessageBay.errorMessage(name + " linked track: " + abortMessage);
			}
			
			if (state == PLAYBACK_LOADING) {
				
				centralizedMessage = "Loading dubs...";
				g.setColor(Color.BLACK);
				
			} else {

				// Paint the selection range
				int selLeftX = XatFrame(selectionStart);
				if (selectionLength > 1) {
					g.setColor(SELECTION_COLOR);
					g.fillRect(	getX() + selLeftX, 
								getY(), 
								XatFrame(selectionStart + selectionLength) - selLeftX, 
								getHeight());
				}
				
				// Draw selection start bar
				((Graphics2D)g).setStroke(SampledTrackGraphView.GRAPH_BAR_STROKE);
				
				// Note that if the start line is near the edges of the panel it can be concealed - thus
				// set a thick line on the edges
				x = selLeftX + getX();
				if (x == 0) {
					x = 1;
				} else  if (x == getWidth()){
					x = getWidth() - 1;
				}
				
				g.setColor(Color.RED);
				g.drawLine(
						x, 
						getY(), 
						x,
						getY() + getHeight());
	
				
				// Paint the playback bar
				if (currentPlaybackFramePosition >= 0 && linkedOverdubbedFrame != null) {
					
					x = XatFrame(currentPlaybackFramePosition) + getX();
					
					((Graphics2D)g).setStroke(SampledTrackGraphView.GRAPH_BAR_STROKE);
					g.setColor(SampledTrackGraphView.PLAYBACK_BAR_COLOR);
					g.drawLine(x, getY(), x, getY() + getHeight());
				
				}
			
			}
			

			
			break;
			
		}
		
		// Draw centralized message string
		if (centralizedMessage != null) {

			FontMetrics fm   = g.getFontMetrics(MESSAGE_FONT);
			Rectangle2D rect = fm.getStringBounds(centralizedMessage, g);

			// Calc centered position
			int x = getX() + ((getWidth() / 2) - (int)(rect.getWidth() / 2));
			int y = getY() + ((getHeight() / 2) + (int)(rect.getHeight() / 2) - 4);
			
			// Draw text
			g.setFont(MESSAGE_FONT);
			g.drawString(centralizedMessage, x, y);
			
			// Draw add icon if waiting for a link
			if (getLink() == null) {
				IconRepository.getIcon("goto.png").paintIcon(null,
						g, 
						x - 34, 
						getY() + (getHeight() / 2) - 12);
			}
				
		}

		// Paint name label over everything
		if (state != NOT_INITIALIZED && state != FAILED_STATE && state != EMPTY_STATE)
			nameLabel.paint(g);
		
		g.setClip(clipBackUp);
		
		super.paintLink((Graphics2D)g);

	}
	
	private void drawTrackName(Track track, Graphics g, Color textColor) {

		// Draw helper message for user to give meaningful information of what they
		// are actually highlighting
		String helperMessage = track.getTrackName();
		if (helperMessage == null) helperMessage = "<Unamed>";
		helperMessage = helperMessage + " (" + track.getParentFrameName() +  ")";
		
		FontMetrics fm   = g.getFontMetrics(HELPER_FONT);
		Rectangle2D rect = fm.getStringBounds(helperMessage, g);

		// Calc centered position on track bounds -- which can be smaller than the font bounds`
		int xoff = (int)((track.area.width - rect.getWidth()) / 2);
		int yoff = (int)((track.area.height / 2) + (rect.getHeight() / 2));
		
		xoff += (getX() + track.area.x);
		yoff += (getY() + track.area.y);
		
		if (xoff < getX()) 
			xoff = getX();
		else if ((xoff + rect.getWidth()) > (getX() + getWidth())) 
			xoff = (int)(getX() + getWidth() - rect.getWidth());
		
		// Draw text
		g.setColor(textColor);
		g.setFont(HELPER_FONT);
		g.drawString(helperMessage, xoff, yoff);

	}
	
	@Override
	protected void paintInFreeSpace(Graphics g) {

		super.paintInFreeSpace(g);

		Shape clipBackUp = g.getClip();
		Rectangle tmpClip = (clipBackUp != null) ? clipBackUp.getBounds() : 
			new Rectangle(0, 0, 
					Browser._theBrowser.getContentPane().getWidth(), 
					Browser._theBrowser.getContentPane().getHeight());
			
		g.setClip(tmpClip.intersection(getBounds()));
	
		// Draw the name
		String name = getName();
		if (name == null) {
			name = "Unnamed";
		}

		g.setFont(TrackWidgetCommons.FREESPACE_TRACKNAME_FONT);
		g.setColor(TrackWidgetCommons.FREESPACE_TRACKNAME_TEXT_COLOR);
		
		// Center track name
		FontMetrics fm   = g.getFontMetrics(TrackWidgetCommons.FREESPACE_TRACKNAME_FONT);
		Rectangle2D rect = fm.getStringBounds(name, g);
		
		g.drawString(
				name, 
				this.getX() + (int)((getWidth() - rect.getWidth()) / 2), 
				this.getY() + (int)((getHeight() - rect.getHeight()) / 2) + (int)rect.getHeight()
				);
		
		g.setClip(clipBackUp);
	

	}

	private void updatePlaybackPosition(int absFramePos) {

		if (currentPlaybackFramePosition != absFramePos) {

			int height = getHeight();
			int offsetX = getX();
			int offsetY = getY();
			int x;

			// Invalidate old pos
			if (currentPlaybackFramePosition >= 0) {
				x = XatFrame(currentPlaybackFramePosition);
				
				FrameGraphics.invalidateArea(new Rectangle(
					x + offsetX, 
					offsetY, 
					SampledTrackGraphView.GRAPH_BAR_NWIDTH, // be consistent
					height));
			}

			// Set new pos
			currentPlaybackFramePosition = absFramePos;
			
			if (currentPlaybackFramePosition >= 0) {
				
				// Invalidate new pos
				x = XatFrame(currentPlaybackFramePosition);
		
				FrameGraphics.invalidateArea(new Rectangle(
						x + offsetX , 
						offsetY, 
						SampledTrackGraphView.GRAPH_BAR_NWIDTH, // be consistent
						height));
			}

		}

		
	}
	
	private int XatFrame(int frame) {
		float width = getWidth();
		float div = totalFrameLength;
		div = ((float)frame) / div;
		return (int)(width * div);
	}
	
	private int frameAtX(int x) {
		float totalFrames = totalFrameLength;
		float div = getWidth();
		div = ((float)x) / div;
		return (int)(totalFrames * div);
	}
	
	/**
	 * {@inheritDoc}
	 * Once a result is received, the graph is build
	 */
	public void receiveResult(OverdubbedFrame odFrame, TrackGraphLoopException loopEx) {
		
		linkedOverdubbedFrame = odFrame;
		
		pendingGraphFetchCount --;
		
		// If there are new requests pending for rebuilding the graph then leave the state
		// as loading graph.
		if (pendingGraphFetchCount > 0)  {
			linkedOverdubbedFrame = null;
			return;
		}
		
		// Reset tracks
		tracks.clear();
		uniqueLocalTrackCount = 0;
		totalFrameLength = 0;
		
		// Only continue to build the graph iff this widget is still actaully visible
		// If allowed the graph to build, then could consume up precious memory :(
		if (!isVisible()) return;
		
		// Before any construction of a graph - the requested graph is only for the overdubbed
		// frame that this widget is linking to. However, this linked widget may not be
		// at all valid, that is, it might be the cause of a loop! If it is then this widget
		// would be un-usable.
		Frame parent = getParentFrame();
		String parentFrameName = (parent != null) ? parent.getName() : null;
		if (parentFrameName != null) {
			OverdubbedFrame odframe = AudioStructureModel.getInstance().getOverdubbedFrame(parentFrameName);
			if (odframe != null && !odframe.containsLinkedTrack(virtualFilename)) {
				failMessage = "Bad link: contains a loop";
				setState(FAILED_STATE);
				return;
			}
		}

		if (loopEx != null) {
			failMessage = "Bad link: contains a loop";
			setState(FAILED_STATE);
		} else if (odFrame == null) { // In this case either the frame does not exist or is an empty OD frame
			setState(EMPTY_STATE);
		} else {
			
			// Infer total frame length
			totalFrameLength = AudioMath.millisecondsToFrames(
					odFrame.calculateRunningTime(), SampledAudioManager.getInstance().getDefaultPlaybackFormat());
			
			List<AbsoluteTrackNode> absNodes = odFrame.getAbsoluteTrackLayoutDeep(
					masterMix.getChannelID());
			
			if (absNodes.isEmpty()) {
				setState(EMPTY_STATE);
			} else {
				
				while (!absNodes.isEmpty()) {
					
					// Group tracks in list by type so can be rendered such that they look 
					// like they represent the same audio... but also retaining the virtual y
					// ordering...
					String localFilename = absNodes.get(0).getTrackNode().getLocalFilename();

					for (int i = 0; i < absNodes.size(); i++) {
						AbsoluteTrackNode node = absNodes.get(i);
						if (node.getTrackNode().getLocalFilename().equals(localFilename)) {
							tracks.add(new Track(absNodes.remove(i)));
							i--;
						}
					}
					
					uniqueLocalTrackCount++;
	
				}
				
				// Update the layout
				updateGraphLayout();
				
				// Evaluate the state of this add set the state accordingly
				if (MultiTrackPlaybackController.getInstance().isLoading(
						linkedOverdubbedFrame.getFrameName(), 
						masterMix.getChannelID())) {
					
					// Ensure that am receiving notifiactions:
					List<String> loaded = MultiTrackPlaybackController.getInstance().attachLoadListener(this);
					assert(loaded != null);
					
					setState(PLAYBACK_LOADING);

					// Update GUI according to current load state
					for (String ln : loaded)
						setTrackState(ln, TRACK_LOADED);
					
				} else if (MultiTrackPlaybackController.getInstance().isPlaying(
						linkedOverdubbedFrame.getFrameName(), 
						masterMix.getChannelID())) {
					
					setState(PLAYING);
					
				} else {
					setState(STOPPED);
				}

				
			}
			
			
			
		}
		
	}
	
	/**
	 * Lays out any tracks to fix the current widget size.
	 */
	private void updateGraphLayout() {
		
		if (tracks.isEmpty() || !this.isVisible() || linkedOverdubbedFrame == null)
			return;
		
		long totalRunningTime = linkedOverdubbedFrame.calculateRunningTime();
		
		// Tracks are assumed to be ordered such that they are grouped. (If not then
		// graph may look confusing).
		String groupLocalFilename = tracks.get(0).getLocalFilename();
		int currentColorIndex = 0;
		float currentY = 0;
		
		// Helpers:
		int width = getWidth();
		int height = getHeight();
		
		float trackHeight = (float)height / uniqueLocalTrackCount;
		int nTrackHeight = (int)trackHeight;
		if (nTrackHeight == 0) nTrackHeight = 1;
		
		for (Track track : tracks) {
			
			float x = track.getABSStartTime();
			x /= totalRunningTime;
			
			float trackWidth = track.getRunningTime();
			trackWidth /= totalRunningTime;

			if (!groupLocalFilename.equals(track.getLocalFilename())) {
				currentColorIndex++;
				if (currentColorIndex >= TRACK_COLOR_WHEEL.length) currentColorIndex = 0;
				currentY += trackHeight;
				groupLocalFilename = track.getLocalFilename();
			}
			
			track.area = new Rectangle(
					(int) (x * width), 
					(int) currentY,
					(int) (trackWidth * width),
					nTrackHeight
					);
			
			// Have some give to become selectable / viewable
			if (track.area.width <= 1) {
				
				track.area.width = 2;
				if ((track.area.x + 2) > width)
					track.area.x = width - 2;
			}

			track.baseColor = TRACK_COLOR_WHEEL[currentColorIndex];

		}
	}
	
	/**
	 * Sets the selection. Invalidates
	 * 
	 * @param start 
	 * 			In frames. Clamped to be positive
	 * @param length 
	 * 			In frames. If less or equal to one, then the selection length is one frame.
	 * 			Otherwise selection is ranged.
	 */
	public void setSelection(int start, int length) {
		
		if (start < 0) start = 0;
		selectionStart = start;
		selectionLength = length;
		
		invalidateSelf();
	}
	
	private void setTrackState(String localFilename, int newState) {
		assert(localFilename != null);
		
		for (Track track : tracks) {
			if (localFilename.equals(track.getLocalFilename())) {
				track.state = newState;
			}
		}
		
	}
	
	private void setTrackState(Track track, int newState) {
		track.state = newState;
	}

	private static final int TRACKSTATE_READY = 1;
	private static final int TRACKSTATE_LOADING = 2;
	private static final int TRACKSTATE_FAILED = 3;
	
	
	
	private void updateBorderColor() {
		
		// Get border color currently used
		Color oldC = getSource().getBorderColor();
		
		Color newC = TrackWidgetCommons.getBorderColor(
				SoundDesk.getInstance().isSolo(masterMix.getChannelID()), 
						masterMix.isMuted());
		
		// Update the color
		if (!newC.equals(oldC)) {
			setWidgetEdgeColor(newC);
		}
	}
	
	/**
	 * Updates the volume GUI for all views
	 */
	public void updateVolume() {
		int volume = (int)(100 * masterMix.getVolume());
		if (playbackControlPopup != null)
			playbackControlPopup.updateVolume(volume);
		
//		if (soundDeskPopup != null) 
//			soundDeskPopup.updateVolume(volume);
	}
	

	/**
	 * Updates the mute button GUI for all views. 
	 */
	public void updateMute() {
		if (playbackControlPopup != null)
			playbackControlPopup.updateMute(masterMix.isMuted());
		
//		if (soundDeskPopup != null) 
//			soundDeskPopup.updateMute(masterMix.isMuted());
	}


	/**
	 * Updates the solo button GUI for all views. 
	 */
	public void updateSolo() {
		boolean isSolo = SoundDesk.getInstance().isSolo(masterMix.getChannelID());
		if (playbackControlPopup != null)
			playbackControlPopup.updateSolo(isSolo);
//		if (soundDeskPopup != null)
//			soundDeskPopup.updateSolo(isSolo);
		
	}
	

	/**
	 * The small popup for common actions.
	 * 
	 * @author Brook Novak
	 *
	 */
	private class PlaybackPopup extends PlaybackControlPopup {

		private static final long serialVersionUID = 1L;
		
		public PlaybackPopup() {
			miscButton.setActionCommand("goto");
			miscButton.setIcon(IconRepository.getIcon("goto.png"));
			miscButton.setToolTipText("Goto linked frame");
		}

		@Override
		public void onHide() {
			super.onHide();
		}

		@Override
		public void onShow() {
			super.onShow();
			updateVolume((int)(100 * masterMix.getVolume()));
			updateMute(masterMix.isMuted());
			updateSolo(SoundDesk.getInstance().isSolo(masterMix.getChannelID()));
		}

		public void actionPerformed(ActionEvent e) {
			
			if (e.getSource() == miscButton) {
				
				String absLink = LinkedTrack.this.getAbsoluteLink();
				if (absLink != null) {
					FrameUtils.DisplayFrame(absLink);
				}
				
			} else {
				
				// Relay shared action
				masterActionListener.actionPerformed(e);
				
			}
		}

		@Override
		protected void volumeChanged() {
			masterActionListener.volumeChanged(volumeSlider);
		}

		@Override
		protected void muteChanged() {
			masterActionListener.muteChanged(muteButton);
		}

		@Override
		protected void soloChanged() {
			masterActionListener.soloChanged(soloButton);
		}

	}

	/**
	 * Common actions shared by the small playback popup and the sound desk popup.
	 * Unlike the track widget these are managed within the linked widget since they
	 * only are visible if the track widget is in view.
	 *  
	 * @author Brook Novak
	 */
	private class MasterControlActionListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			
			if (!(state == PLAYING || state == STOPPED)) return;
			
			assert(linkedOverdubbedFrame != null);
			
			String actionCommand = (e.getActionCommand() == null) ? "" : e.getActionCommand();
			
			if (actionCommand.equals("playpause")) {

				if (!MultiTrackPlaybackController.getInstance().isPlaying(
						linkedOverdubbedFrame.getFrameName(), masterMix.getChannelID())) {
					
					int startFrame = -1, endFrame = -1;
		
					// Resume playback?
					if (MultiTrackPlaybackController.getInstance().isMarkedAsPaused(
							linkedOverdubbedFrame.getFrameName(), masterMix.getChannelID())) {
						
						startFrame = MultiTrackPlaybackController.getInstance().getLastSuspendedFrame();
			
						if (startFrame >= 0 && startFrame < totalFrameLength) {
							
							assert(selectionStart >= 0);
			
							// The user may have edited the audio track and reselected it
							// since the last pause. Thus select an appropriate end frame
							endFrame = (int)((selectionLength > 1) ?
									selectionStart + selectionLength : totalFrameLength - 1);
								
							// Changed selection? it play range invalid?
							if (endFrame <= startFrame || startFrame < selectionStart) {
								startFrame = -1; // Play new selection (see below)
							
							} else if (endFrame >= totalFrameLength) {
								endFrame = (int)totalFrameLength - 1;
							}		
							
						}
					}
					
					// Play from beginning of selection to end of selection
					if (startFrame < 0) {
						startFrame = selectionStart;
						endFrame = (int)((selectionLength > 1) ?
								startFrame + selectionLength:
									totalFrameLength - 1);
					}
				
					if (startFrame < endFrame) {
						
						MultiTrackPlaybackController.getInstance().playFrame(
								LinkedTrack.this, 
								linkedOverdubbedFrame.getFrameName(), 
								masterMix.getChannelID(), 
								false, // TODO: Set appropriatly
								startFrame, 
								endFrame);
						
						setState(PLAYBACK_LOADING);
						
					}

				} else { // pause

					MultiTrackPlaybackController.getInstance().setPauseMark(true);
					MultiTrackPlaybackController.getInstance().stopPlayback();
	
				}

				
			} else if (actionCommand.equals("stop")) {
				
				if (MultiTrackPlaybackController.getInstance().isCurrentPlaybackSubject(
						linkedOverdubbedFrame.getFrameName(), masterMix.getChannelID())) {
					MultiTrackPlaybackController.getInstance().setPauseMark(false);
					MultiTrackPlaybackController.getInstance().stopPlayback();
				}
				
				
			} else if (actionCommand.equals("rewind")) {
				assert(state != PLAYING);

				MultiTrackPlaybackController.getInstance().setPauseMark(false);
				currentPlaybackFramePosition = -1;
				setSelection(0, 0); // invalidates
				FrameGraphics.refresh(true);
				
			}
		}
		
		public void volumeChanged(JSlider volumeSlider) {
			masterMix.setVolume(((float)volumeSlider.getValue()) / 100.0f);
		}

		public void muteChanged(JToggleButton muteButton) {
			masterMix.setMuted(muteButton.isSelected());
		}

		public void soloChanged(JToggleButton soloButton) {
			SoundDesk.getInstance().setSoloIDPrefix(soloButton.isSelected() ?
					masterMix.getChannelID() : null
					);
		}
		
	}
	
	private class MouseActions implements MouseListener, MouseMotionListener {

		public void mouseClicked(MouseEvent e) {
			assert(state != EMPTY_STATE);
			
			if (state != PLAYING && state != STOPPED) return;
			
			if (nameLabel.onMouseClicked(e)) {
				e.consume();
				return;
			}
			
		}


		public void mouseEntered(MouseEvent e) {
			assert(state != EMPTY_STATE);
		}


		public void mouseExited(MouseEvent e) {
			assert(state != EMPTY_STATE);
			
		}


		public void mousePressed(MouseEvent e) {
			
			assert(state != EMPTY_STATE);
			
			if (nameLabel.onMousePressed(e)) {
				e.consume();
				return;
			}
			
			selectionStartX = e.getX();
			
		
			// Set selection start, and length as a single frame
			setSelection(frameAtX(selectionStartX), 1);
			
			FrameGraphics.refresh(true);
		}


		public void mouseReleased(MouseEvent e) {
			

			assert(state != EMPTY_STATE);
			
			
			// Was the release a simple click
			if (selectionStartX == e.getX() && e.getButton() == MouseEvent.BUTTON1) {
				
				// Traverse to the parent from of the selected track
				String link = null;
				for (Track track : tracks) {
					if (track.area.contains(e.getPoint())) {
						link = track.getParentFrameName();
						break;
					}
				}
	
				if (link == null) link = LinkedTrack.this.getAbsoluteLink();
				
				assert(link != null); // should be in an empty state otherwise

				FrameUtils.DisplayFrame(link);
			}

			if (nameLabel.onMouseReleased(e)) {
				e.consume();
				return;
			}

			
		}
		

		public void mouseDragged(MouseEvent e) {
			
			assert(state != EMPTY_STATE);
			
			if (state != PLAYING && state != STOPPED) return;

			if (nameLabel.onMouseDragged(e)) {
				e.consume();
				return;
			}
		
			if (selectionStartX < 0) return;
			
			// Clamp mouse selection range
			int width =  getWidth();
			int x = e.getX();
			
			if (x > width) x = width;
			else if (x < 0) x = 0;
			
			// Set selection range
			int frameAtCursor = frameAtX(x);
			int frameAtStartPoint = frameAtX(selectionStartX);
			
			int start = Math.min(frameAtCursor, frameAtStartPoint);
			int length = Math.max(frameAtCursor, frameAtStartPoint) - start;
			if (length > 0) {
				setSelection(start, length);
			}
			
			FrameGraphics.refresh(true);
		}

		public void mouseMoved(MouseEvent e) {
			assert(state != EMPTY_STATE);
			
			if (state != PLAYING && state != STOPPED) return;

			if (nameLabel.onMouseMoved(e, _swingComponent)) {
				e.consume();
				return;
			}

			if (playbackControlPopup == null) {
				playbackControlPopup = new PlaybackPopup();
			} 

			// Only show popup iff there is nothing expanded / expanding.
			if (!PopupManager.getInstance().isShowing(playbackControlPopup) &&
					!ExpandedTrackManager.getInstance().doesExpandedTrackExist()) {	
				
				// Get rid of all popups
				PopupManager.getInstance().hideAutohidePopups();
				
				Rectangle animationSource = _swingComponent.getBounds();
				
				// Determine where popup should show
				int x = LinkedTrack.this.getX();
				int y = LinkedTrack.this.getY() - playbackControlPopup.getHeight() - 2; // by default show above
				
				// I get sick.dizzy from the popup expanding from the whole thing...
				animationSource.height = 1;
				animationSource.width = Math.min(animationSource.width, playbackControlPopup.getWidth());
				
				if (y < 0) {
					y = LinkedTrack.this.getY() + LinkedTrack.this.getHeight() + 2;
					animationSource.y = y - 2;
				} 

				// Animate the popup
				PopupManager.getInstance().showPopup(
						playbackControlPopup, 
						new Point(x, y), 
						_swingComponent,
						PopupManager.getInstance().new ExpandShrinkAnimator(
								animationSource,
								Color.LIGHT_GRAY));
				
				PopupReaper.getInstance().initPopupLifetime(
						playbackControlPopup, 
						PopupManager.getInstance().new ExpandShrinkAnimator(
								animationSource,
								Color.LIGHT_GRAY), 
								TrackWidgetCommons.POPUP_LIFETIME);
				
			} else {
				PopupReaper.getInstance().revivePopup(playbackControlPopup, TrackWidgetCommons.POPUP_LIFETIME);
			}
			

			// Handle selection / invalidation of mouse hovering over track segments
			boolean isOneSelected = false;
			boolean shouldInvalidate = false;
			for (Track track : tracks) {
				
				if (isOneSelected && track.isSelected) {
					track.isSelected = false;
					shouldInvalidate = true;
				} else if (!isOneSelected) {
					isOneSelected = track.area.contains(e.getPoint());
					shouldInvalidate |= (isOneSelected != track.isSelected);
					track.isSelected = isOneSelected;
				}
	
			}
			
			if (shouldInvalidate) {
				invalidateSelf();
				FrameGraphics.refresh(true);
			}
		}
	}
	

	/**
	 * A wrapper for {@link AbsoluteTrackNode} - including graphical representation data.
	 * @author Brook Novak
	 */
	private class Track {
		
		private AbsoluteTrackNode absNode; // immutable. Never null
		
		private Rectangle area;
		
		private Color baseColor;
		
		boolean isSelected = false;
		
		private int state = TRACKSTATE_READY;
		
		Track(AbsoluteTrackNode absNode) {
			assert(absNode != null);
			this.absNode = absNode;
		}
		
		public String getChannelID() {
			return absNode.getChannelID();
		}
		
		public long getABSStartTime() {
			return absNode.getABSStartTime();
		}
		
		public String getLocalFilename() {
			return absNode.getTrackNode().getLocalFilename();
		}

		public long getRunningTime() {
			return absNode.getTrackNode().getRunningTime();
		}
		
		public String getParentFrameName() {
			return absNode.getParentFrameName();
		}
		
		public String getTrackName() {
			return absNode.getTrackNode().getName();
		}
		
		public void paintTrackArea(Graphics2D g, int x, int y) {
			
			if (state == TRACKSTATE_LOADING && LinkedTrack.this.state == PLAYBACK_LOADING) {
				g.setColor(TRACK_LOAD_COLOR);
			} else if (state == TRACKSTATE_FAILED) {
				g.setColor(TRACK_FAIL_COLOR);
			} else {
				g.setColor(baseColor);
			}
			
			Paint restore = g.getPaint();
			if (ApolloSystem.useQualityGraphics) {

				GradientPaint gp = new GradientPaint(
						(int) (area.x + x + (area.width / 2)), area.y + y + (int)(area.height * 0.8), baseColor,
						(int) (area.x + x + (area.width / 2)), area.y + y, TRACK_SELECTED_BORDER_COLOR);
				g.setPaint(gp);
			}
			
			// Draw a filled rect - represented a track in the heirarchy
			g.fillRect(area.x + x, area.y + y, area.width, area.height);
			
			if (ApolloSystem.useQualityGraphics) {
				g.setPaint(restore);
			}

		}
		
		public void paintTrackBorder(Graphics g, int x, int y) {
			
			if (isSelected) {
				g.setColor(TRACK_SELECTED_BORDER_COLOR);
				((Graphics2D)g).setStroke(TRACK_SELECTED_BORDER);
			} else {
				g.setColor(TRACK_BORDER_COLOR);
				((Graphics2D)g).setStroke(TRACK_BORDER); // todo muted colors etc...
			}
			
			g.drawRect(area.x + x, area.y + y, area.width, area.height);
		}
		
	}
	
	@Override
	public boolean isWidgetEdgeThicknessAdjustable() {
		return false;
	}
	
	

}
