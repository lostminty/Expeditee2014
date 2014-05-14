package org.apollo.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.SampledAudioManager;
import org.apollo.audio.util.MultiTrackPlaybackController;
import org.apollo.audio.util.SoundDesk;
import org.apollo.audio.util.TrackMixSubject;
import org.apollo.audio.util.MultiTrackPlaybackController.MultitrackLoadListener;
import org.apollo.gui.FrameLayoutDaemon;
import org.apollo.io.AudioPathManager;
import org.apollo.io.IconRepository;
import org.apollo.items.FramePlaybackLauncher;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.ApolloSystemLog;
import org.apollo.util.AudioMath;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.DisplayIOObserver;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;

/**
 * This a last minute hack ... should be revised
 * 
 * @author Brook Novak
 *
 */
public class FramePlayer extends InteractiveWidget 
	implements Observer, MultitrackLoadListener, ActionListener, DisplayIOObserver {

	private static final int BUTTON_SIZE = 40;
	
	private static TrackMixSubject masterMix = null;
	private static String currentPlayingFrame = null;
	
	private JButton playPauseButton;
	private JButton stopButton;
	private JButton rewindButton;
	private JButton playLauncherButton;
	private JComboBox frameSelection;
	private JToggleButton muteButton;
	private JSlider volumeSlider;

	private int state = READY;
	private String abortMessage = null;
	
	private boolean isUpdatingGUI = false;
	
	private static final String CURRENT_FRAME_SPECIFIER = "Current Frame";
	
	public static String FRAME_PLAYERMASTER_CHANNEL_ID = "#$frameplayer#master$";
	
	private static final Color LOADING_BORDER_COLOR = new Color(22, 205, 5);
	//private static final Color FAILED_MESSAGE_COLOR = Color.RED;
	//private static final Font MESSAGE_FONT = TrackWidgetCommons.FREESPACE_TRACKNAME_FONT;
	
	/** States - mutex in this widget but not really .. for example since can be loading tracks and the graph at the same time... */
	private  static final int READY = 1; // Waiting for user interaction
	private  static final int PLAYBACK_LOADING = 2; // Loading tracks from file/cache to play/resume
	private  static final int PLAYING = 3; // Playing audio.
	
	private static final int TYPED_FRAME_CAPACITY = 10;
	private static final int FRAME_HISTORY_CAPACITY = 5;
	
	private static final String TYPED_FRAMED_HISTORY_FILE = ".typedframes";
	
	private static Deque<String> typedFrameNames = new LinkedList<String>();
	private Set<String> frameComboModelData = new HashSet<String>();
	
	static {
		masterMix = SoundDesk.getInstance().getOrCreateMix(FRAME_PLAYERMASTER_CHANNEL_ID);
		
		// Read typed frames
		File f = new File(AudioPathManager.AUDIO_HOME_DIRECTORY + TYPED_FRAMED_HISTORY_FILE);
		if (f.exists()) {
			BufferedReader in = null;
			String line = null;
			try {
				
				// Open the vbase for reading
				in = new BufferedReader(new FileReader(f)); 
				
				// Read the sbase file and check all names
				while ((line = in.readLine()) != null && typedFrameNames.size() < TYPED_FRAME_CAPACITY) {
					line = line.trim();
					if (!line.isEmpty() && !typedFrameNames.contains(line)) 
						typedFrameNames.add(line);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			// Clean up
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * Saves typed frames to file
	 */
	public static void saveTypedFrames() {
		
		FileWriter out = null;
		
		try {
			// Open the vbase for appending
			out = new FileWriter(AudioPathManager.AUDIO_HOME_DIRECTORY + TYPED_FRAMED_HISTORY_FILE, false); 
			for (String str : typedFrameNames) {
				out.write(str + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
			
		// Clean up
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	/**
	 * Constructor called by Expeditee. 
	 * 
	 * @param source
	 * @param args
	 */
	public FramePlayer(Text source, String[] args) {
		super(source, new JPanel(new GridBagLayout()), 
				BUTTON_SIZE * 12, BUTTON_SIZE * 12, 
				BUTTON_SIZE, BUTTON_SIZE);

		playPauseButton = new JButton();
		playPauseButton.addActionListener(this);
		playPauseButton.setIcon(IconRepository.getIcon("play.png"));
		playPauseButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		playPauseButton.setToolTipText("Play selection / Pause");
		
		stopButton = new JButton();
		stopButton.setEnabled(false);
		stopButton.addActionListener(this);
		stopButton.setIcon(IconRepository.getIcon("stop.png"));
		stopButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		stopButton.setToolTipText("Stop playback");
		
		rewindButton = new JButton();
		rewindButton.addActionListener(this);
		rewindButton.setIcon(IconRepository.getIcon("rewind.png"));
		rewindButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		rewindButton.setToolTipText("Rewind to start");
		
		// Icon changes
		muteButton = new JToggleButton();
		muteButton.setSelectedIcon(IconRepository.getIcon("volmute.png"));
		muteButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		muteButton.setToolTipText("Toggle mute");
		muteButton.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!FramePlayer.this.isUpdatingGUI) {
					muteChanged();
				}
			}
		});
		
		playLauncherButton = new JButton();
		playLauncherButton.addActionListener(this);
		playLauncherButton.setIcon(IconRepository.getIcon("frameplay.png"));
		playLauncherButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		playLauncherButton.setToolTipText("Play from a specific position");

		final int VOLUME_SPACING = 6;
		volumeSlider = new JSlider(JSlider.HORIZONTAL);
		volumeSlider.setMinimum(0);
		volumeSlider.setMaximum(100);
		volumeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!FramePlayer.this.isUpdatingGUI) {
					volumeChanged();
				}
				// Update the icons
				updateButtonGUI();
			}
		});
		volumeSlider.setPreferredSize(new Dimension((3 * BUTTON_SIZE) - (2 * VOLUME_SPACING), BUTTON_SIZE));

		frameSelection = new JComboBox();
		frameSelection.setEditable(true);
		frameSelection.setPreferredSize(new Dimension(4 * BUTTON_SIZE, BUTTON_SIZE));

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		_swingComponent.add(playPauseButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		_swingComponent.add(playLauncherButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		_swingComponent.add(stopButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		_swingComponent.add(rewindButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		_swingComponent.add(muteButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 5;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0,VOLUME_SPACING,0,VOLUME_SPACING);
		_swingComponent.add(volumeSlider, c);
		
		c = new GridBagConstraints();
		c.gridx = 6;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		_swingComponent.add(frameSelection, c);
		
		updateButtonGUI();
		updateBorderColor();
		updateFrameSelection();
		
		setWidgetEdgeThickness(TrackWidgetCommons.STOPPED_TRACK_EDGE_THICKNESS);

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
		if (this.state == newState) return; // no need to process request.

		switch(newState) {
		case READY: 

			rewindButton.setEnabled(true);
			stopButton.setEnabled(false);
			playPauseButton.setEnabled(true);
			playPauseButton.setIcon(IconRepository.getIcon("play.png"));
			playLauncherButton.setEnabled(true);

			setWidgetEdgeThickness(TrackWidgetCommons.STOPPED_TRACK_EDGE_THICKNESS);
			
			break;
			
		case PLAYBACK_LOADING: 
			playLauncherButton.setEnabled(false);
			stopButton.setEnabled(false);
			rewindButton.setEnabled(false);
			playPauseButton.setEnabled(false);
			break;
			
		case PLAYING: 
			stopButton.setEnabled(true);
			rewindButton.setEnabled(false);
			playLauncherButton.setEnabled(true);
			playPauseButton.setEnabled(true);
			playPauseButton.setIcon(IconRepository.getIcon("pause.png"));

			setWidgetEdgeThickness(TrackWidgetCommons.PLAYING_TRACK_EDGE_THICKNESS);

			break;

		}

		state = newState;
		
		updateBorderColor();
		
		invalidateSelf();
		FrameGraphics.refresh(true);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onParentStateChanged(int eventType) {
		super.onParentStateChanged(eventType);
		

		//Frame currentFrame = DisplayIO.getCurrentFrame();
	//	String currentFrameName = (currentFrame != null) ? currentFrame.getName() : null;

		switch (eventType) {

		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY: 
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:

			// Setup observers
			SoundDesk.getInstance().addObserver(this); // for solo 
			MultiTrackPlaybackController.getInstance().addObserver(this); // the core!
			masterMix.addObserver(this);
			DisplayIO.addDisplayIOObserver(this);
		
			// Evaluate the state of this add set the state accordingly
			if (currentPlayingFrame != null
					&& MultiTrackPlaybackController.getInstance().isLoading(
							currentPlayingFrame, 
							masterMix.getChannelID())) {
				
				// Ensure that am receiving notifiactions:
				List<String> loaded = MultiTrackPlaybackController.getInstance().attachLoadListener(this);
				assert(loaded != null);
				
				setState(PLAYBACK_LOADING);
				
			} else if (currentPlayingFrame != null 
					&& MultiTrackPlaybackController.getInstance().isPlaying(
							currentPlayingFrame, 
							masterMix.getChannelID())) {
				
				setState(PLAYING);
				
			} else {
				setState(READY);
			}
			
			updateFrameSelection();

			break;
			
		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:

			// Cancel loading of audio
			if (state == PLAYBACK_LOADING) {
				assert(currentPlayingFrame != null);
				
				MultiTrackPlaybackController.getInstance().cancelLoad(
						currentPlayingFrame, 
						masterMix.getChannelID());
			}

		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY: // TODO revise - and in sampled track widget
		case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
			
			// Remove observers
			DisplayIO.removeDisplayIOObserver(this);
			SoundDesk.getInstance().removeObserver(this);
			MultiTrackPlaybackController.getInstance().removeObserver(this);
			masterMix.removeObserver(this);
			
			setState(READY);
			break;

		}
	}



	/**
	 * {@inheritDoc}
	 */
	public void multiplaybackLoadStatusUpdate(int id, Object state) {

		// NOTE: Could switch to an unloaded state
		if (this.state != PLAYBACK_LOADING) return;
		
		switch(id) {
		case MultitrackLoadListener.LOAD_CANCELLED:
			setState(READY);
			break;
		case MultitrackLoadListener.LOAD_COMPLETE:
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
			((Exception)state).printStackTrace();
			break;
		case MultitrackLoadListener.TRACK_LOADED:
			break;

		}
		
		if (abortMessage != null) {
			ApolloSystemLog.println("Aborted playback - " + abortMessage);
			setState(READY);
		}
		
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
	public void modelChanged(Subject source, SubjectChangedEvent event) {

		// Synch GUI with track state
		switch (event.getID()) {
			
		case ApolloSubjectChangedEvent.PLAYBACK_STARTED:
			
			if (currentPlayingFrame != null &&
				MultiTrackPlaybackController.getInstance().isCurrentPlaybackSubject(
						currentPlayingFrame, masterMix.getChannelID())) {
				setState(PLAYING);
			} 
			
			break;
			
		case ApolloSubjectChangedEvent.PLAYBACK_STOPPED:
			
			if (state == PLAYING) {
				//assert(currentPlayingFrame != null);

				// Transition into new state
				setState(READY);

			}
			break;
			
		case ApolloSubjectChangedEvent.MULTIPLAYBACK_LOADING: 
			
			// Adjust state accordingly
			if (currentPlayingFrame != null &&
					MultiTrackPlaybackController.getInstance().isCurrentPlaybackSubject(
							currentPlayingFrame, masterMix.getChannelID()) && state != PLAYBACK_LOADING) {
				MultiTrackPlaybackController.getInstance().attachLoadListener(this);
				setState(PLAYBACK_LOADING);
			}
			break;
			
		case ApolloSubjectChangedEvent.VOLUME: // From obseved track mix
			updateVolume();
			break;
			
		case ApolloSubjectChangedEvent.MUTE: // From obseved track mix
			updateMute();
			updateBorderColor();
			break;
			
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setObservedSubject(Subject parent) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String[] getArgs() {
		return null;
	}

	/**
	 * @return
	 * 		The selected frame name. Null if not valid.
	 */
	private String getSelectedFrameName() {
		
		if ((frameSelection.getSelectedItem() != null && frameSelection.getSelectedItem().equals(CURRENT_FRAME_SPECIFIER))
				|| frameSelection.getSelectedItem() == null)
			return DisplayIO.getCurrentFrame().getName();
		
		String frameSpecifier = (String)frameSelection.getSelectedItem();
		
		if (frameSpecifier.isEmpty()) {
			return DisplayIO.getCurrentFrame().getName();
		}
		
		frameSpecifier = frameSpecifier.trim();
		int metaIndex = frameSpecifier.indexOf('<');
		if (metaIndex > 0) {
			frameSpecifier = frameSpecifier.substring(0, metaIndex).trim();
		}
		
		if (!FrameIO.isValidFrameName(frameSpecifier)) return null;

		return frameSpecifier; 
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void actionPerformed(ActionEvent e) {
		
		if (!(state == PLAYING || state == READY)) return; // safety

		String selectedFrameName = getSelectedFrameName();
		
		if (e.getSource() == playPauseButton) {
			
			if (state == READY) { // comence playback
				
				if (selectedFrameName == null) {
					
					// Bad frame specifier
					MessageBay.displayMessage("Cannot play frame \"" + 
							frameSelection.getSelectedItem() + 
							"\" - bad name");
					
				} else {
					
					int startFrame = -1, endFrame = -1;
					
					// Remember typed frames
					if (frameSelection.getSelectedItem() != null && !frameSelection.getSelectedItem().equals(CURRENT_FRAME_SPECIFIER)) {
						
						String typedFrame = (String)frameSelection.getSelectedItem();
					
						if (!frameComboModelData.contains(typedFrame.toLowerCase())) {
						
							typedFrameNames.addFirst(selectedFrameName);
							if (typedFrameNames.size() >= TYPED_FRAME_CAPACITY)
								typedFrameNames.removeLast();
							updateFrameSelection();
							
						}
						
					}
	
					// Resume playback?
					if (currentPlayingFrame != null && 
							MultiTrackPlaybackController.getInstance().isMarkedAsPaused(
									currentPlayingFrame, masterMix.getChannelID())) {
						
						long runningTime = FrameLayoutDaemon.inferCurrentTotalMSTime();
						long inferredTotalFrameLength = -1;
						if (runningTime > 0) {
							inferredTotalFrameLength = AudioMath.millisecondsToFrames(
									runningTime, 
									SampledAudioManager.getInstance().getDefaultPlaybackFormat());
						}
						
	
						startFrame = MultiTrackPlaybackController.getInstance().getLastSuspendedFrame();
			
						if (inferredTotalFrameLength > 0 &&
								startFrame >= 0 && startFrame < inferredTotalFrameLength) {
							// Work around for playing to end of frame: give biggest value
							// since it is eventually clamped:
							endFrame =  Integer.MAX_VALUE;
						}
					}
					
					// Play from beginning of selection to end of selection
					if (startFrame < 0) {
						startFrame = 0;
						endFrame =  Integer.MAX_VALUE; // see notes about for workaround hack
					}
				
					if (startFrame < endFrame) {
	
						playFrame(this, 
								selectedFrameName, 
								false, // TODO: Set appropriatly
								startFrame, 
								endFrame);
						
						setState(PLAYBACK_LOADING);
						
					}
				}

			} else { // pause

				MultiTrackPlaybackController.getInstance().setPauseMark(true);
				MultiTrackPlaybackController.getInstance().stopPlayback();

			}

			
		} else if (e.getSource() == stopButton) {
			assert(currentPlayingFrame != null);
			
			if (MultiTrackPlaybackController.getInstance().isCurrentPlaybackSubject(
					currentPlayingFrame, masterMix.getChannelID())) {
				MultiTrackPlaybackController.getInstance().setPauseMark(false);
				MultiTrackPlaybackController.getInstance().stopPlayback();
			}
			
			
		} else if (e.getSource() == rewindButton) {
			assert(state != PLAYING);
			MultiTrackPlaybackController.getInstance().setPauseMark(false);
			
		} else if (e.getSource() == playLauncherButton) {
			
			String target = getSelectedFrameName();
			if (target == DisplayIO.getCurrentFrame().getName()) target = null;
			
			// Create the launcher
			FramePlaybackLauncher launcher = new FramePlaybackLauncher(target);
			launcher.setPosition(FrameMouseActions.MouseX, FrameMouseActions.MouseY);

			// Pick it up
			FrameMouseActions.pickup(launcher);
		}
		
	}
	

	private void updateBorderColor() {
		
		// Get border color currently used
		Color oldC = getSource().getBorderColor();
		Color newC = null;
		
		if (this.state == PLAYBACK_LOADING) {
			
			newC = LOADING_BORDER_COLOR;
			
		} else {

			newC = TrackWidgetCommons.getBorderColor(
					SoundDesk.getInstance().isSolo(masterMix.getChannelID()), 
					masterMix.isMuted());

		}
		
		// Update the color
		if (!newC.equals(oldC)) {
			setWidgetEdgeColor(newC);
		}
	}
	
	/**
	 * Sets the mute icon to represent the current volume value in the slider.
	 * Note: this is not the icon if mute is on.
	 */
	private void updateButtonGUI() {
		
		Icon newIcon = null;
		if (volumeSlider.getValue() <= 25) 
				newIcon = IconRepository.getIcon("vol25.png");
		else if (volumeSlider.getValue() <= 50) 
			newIcon = IconRepository.getIcon("vol50.png");
		else if (volumeSlider.getValue() <= 75) 
			newIcon = IconRepository.getIcon("vol75.png");
		else // maxing
				newIcon = IconRepository.getIcon("vol100.png");
		
		muteButton.setIcon(newIcon);
	}
	

	public void volumeChanged() {
		masterMix.setVolume(((float)volumeSlider.getValue()) / 100.0f);
	}

	public void muteChanged() {
		masterMix.setMuted(muteButton.isSelected());
	}

	

	/**
	 * Updates the volume GUI for all views
	 */
	public void updateVolume() {
		int volume = (int)(100 * masterMix.getVolume());

		if (volumeSlider.getValue() == volume) return;
		
		isUpdatingGUI = true;
		
		volumeSlider.setValue(volume);
		
		isUpdatingGUI = false;
	}
	

	/**
	 * Updates the mute button GUI for all views. 
	 */
	public void updateMute() {
		if (muteButton.isSelected() == masterMix.isMuted()) return;

		isUpdatingGUI = true;
		
		muteButton.setSelected(masterMix.isMuted());

		isUpdatingGUI = false;
	}

	/**
	 * Updates the combo box on frame changes
	 */
	public void frameChanged() {
		updateFrameSelection();
	}
	
	private void updateFrameSelection() {
		
		
		frameComboModelData.clear();
		List<String> orderedModelData = new LinkedList<String>();
		
		orderedModelData.add(CURRENT_FRAME_SPECIFIER);
		
		// Place last visited frames
		List<String> history = DisplayIO.getUnmodifiableVisitedList();
		
		for (int i = history.size() - 1; i >= 0; i--) {
			String str = history.get(i);
			
			if ((history.size() - i) >= FRAME_HISTORY_CAPACITY) break;
			
			String frameName = str;
			for (int j = 0; j <= (history.size() - i); j++) frameName += "<";
			
			orderedModelData.add(frameName);
		}
		
		// Append last typed frames
		for (String str : typedFrameNames) {
			if (!history.contains(str))
				orderedModelData.add(str);
		}
		
	    ComboBoxModel model = new DefaultComboBoxModel(orderedModelData.toArray());
	    
	    // Remove helper tags from model data
	    
	    frameComboModelData = new HashSet<String>();
	    for (String str : orderedModelData) {
	    	int index = str.indexOf('<');
	    	assert(index != 0);
	    	if (index > 1) {
	    		frameComboModelData.add(str.substring(0, index).trim().toLowerCase());
	    	} else {
	    		frameComboModelData.add(str.trim().toLowerCase());
	    	}
	    }
	    
	    Object prevSelected = frameSelection.getSelectedItem();
	    
		frameSelection.setModel(model);
		
		if (prevSelected == null)
			frameSelection.setSelectedIndex(0);
		else 
			frameSelection.setSelectedItem(prevSelected);
		
		
		
	}

	/**
	 * Plays a frame with the FramePlayer mix.
	 * 
	 * @see MultiTrackPlaybackController#playFrame(MultitrackLoadListener, String, String, boolean, int, int, int)
	 * 
	 */
	public static void playFrame(
			MultitrackLoadListener loadListener,
			String rootFrameName,
			boolean resume,
			int startFrame,
			int endFrame) {
		
		if (loadListener == null) throw new NullPointerException("loadListener");
		if (rootFrameName == null) throw new NullPointerException("rootFrameName");
		
		
		currentPlayingFrame = rootFrameName;
		
		MultiTrackPlaybackController.getInstance().playFrame(
				loadListener, 
				rootFrameName, 
				masterMix.getChannelID(),
				resume,
				startFrame, 
				endFrame);
	}
	
}
