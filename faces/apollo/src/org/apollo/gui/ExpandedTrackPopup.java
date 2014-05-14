package org.apollo.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apollo.audio.ApolloPlaybackMixer;
import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.SampledTrackModel;
import org.apollo.audio.TrackSequence;
import org.apollo.audio.structure.AudioStructureModel;
import org.apollo.audio.util.SoundDesk;
import org.apollo.audio.util.TrackMixSubject;
import org.apollo.io.IconRepository;
import org.apollo.items.EmulatedTextItem;
import org.apollo.items.EmulatedTextItem.TextChangeListener;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.AudioMath;
import org.apollo.widgets.TrackWidgetCommons;
import org.expeditee.gui.Browser;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.Popup;
import org.expeditee.gui.PopupManager;

public class ExpandedTrackPopup extends Popup implements ActionListener, Observer {
	
	private static final long serialVersionUID = 1L;
	
	/** The observed subject. Can never be null */
	private SampledTrackModel trackModel; // immutable
	
	private String trackSourceFrameName; // immutable
	
	private TrackMixSubject trackMix; // immutable

	private JButton playPauseButton;
	private JButton stopButton;
	private JButton rewindButton;
	private JButton closeButton;
	private JToggleButton soloButton;
	private JToggleButton muteButton;
	private JSlider volumeSlider;
	JPanel nameLabelParent;
	
	private EmulatedTextItem nameLabel; 
	
	private JSplitPane splitPane;
	private SampledTrackGraphViewPort zoomController;
	private EditableSampledTrackGraphView zoomedTrackView;
	private TimeAxisPanel timeAxis;
	
	private boolean isUpdatingGUI = false;
	
	/**
	 * Hidden constuctor: only ExpandedTrackManager can create these.
	 * 
	 * @see {@link ExpandedTrackManager}
	 * 
	 * @param trackModel
	 * 
	 * @param trackMix
	 * 
	 * @param trackSourceFrameName
	 * 		Where the expanded tracks track source was expanded from.
	 * 		Must not be null
	 * 
	 * @param frameStart
	 * 		the initial zoom
	 * 
	 * @param frameLength
	 * 		the initial zoom
	 * 
	 * @throws NullPointerException
	 * 		If trackModel or trackMix is null.
	 */
	private ExpandedTrackPopup(
			SampledTrackModel trackModel, 
			TrackMixSubject trackMix,
			String trackSourceFrameName,
			int frameStart,
			int frameLength) {
		super(new BorderLayout());
		super.setConsumeBackClick(true);
		
		if (trackModel == null) throw new NullPointerException("trackModel");
		if (trackMix == null) throw new NullPointerException("trackMix");
		if (trackSourceFrameName == null) throw new NullPointerException("trackSourceFrameName");
		
		this.trackModel = trackModel;
		this.trackMix = trackMix;

		final int BUTTON_SIZE = 40;
		
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
				if (!ExpandedTrackPopup.this.isUpdatingGUI) {
					ExpandedTrackPopup.this.trackMix.setMuted(muteButton.isSelected());
					
				}
			}
		});
		
		soloButton = new JToggleButton();
		soloButton.setIcon(IconRepository.getIcon("solo.png"));
		soloButton.setSelectedIcon(IconRepository.getIcon("soloon.png"));
		soloButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		soloButton.setToolTipText("Toggle solo");
		soloButton.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!ExpandedTrackPopup.this.isUpdatingGUI) {
					SoundDesk.getInstance().setSoloIDPrefix(soloButton.isSelected() ?
							ExpandedTrackPopup.this.trackMix.getChannelID() : null
							);
					
				}
			}
		});
		
		closeButton = new JButton();
		closeButton.addActionListener(this);
		closeButton.setIcon(IconRepository.getIcon("close.png"));
		closeButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		closeButton.setToolTipText("Close");
		
		final int VOLUME_SPACING = 6;
		volumeSlider = new JSlider(JSlider.HORIZONTAL);
		volumeSlider.setMinimum(0);
		volumeSlider.setMaximum(100);
		volumeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!ExpandedTrackPopup.this.isUpdatingGUI) {
					ExpandedTrackPopup.this.trackMix.setVolume(((float)volumeSlider.getValue()) / 100.0f);
				}
				// Update the icons
				updateButtonGUI();
			}
		});
		volumeSlider.setPreferredSize(new Dimension(100 - (2 * VOLUME_SPACING), BUTTON_SIZE));
		
		zoomedTrackView = new EditableSampledTrackGraphView();
		zoomedTrackView.setSelectAllOnDoubleClick(true);
		
		zoomController = new SampledTrackGraphViewPort();
		
		// Keep timescale in zoomedTrackView consistance with zoom controller
		zoomController.addZoomChangeListener(new ZoomChangeListener() {

			public void zoomChanged(Event e) {
				
				int minFrame = Math.min(
						zoomController.getZoomStartFrame(), zoomController.getZoomEndFrame());
				
				int maxFrame = Math.max(
						zoomController.getZoomStartFrame(), zoomController.getZoomEndFrame());
				
				
				zoomedTrackView.setTimeScale(minFrame, maxFrame - minFrame);
			}
			
		});
		
		timeAxis = new TimeAxisPanel(zoomedTrackView);
		timeAxis.setPreferredSize(new Dimension(100, 40));
		
		nameLabelParent = new JPanel();
		
		nameLabelParent.addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				if (nameLabel != null) {
					if (nameLabel.onMouseClicked(e)) {
						e.consume();
						return;
					}
				}
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
				if (nameLabel != null) {
					if (nameLabel.onMousePressed(e)) {
						e.consume();
					}
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (nameLabel != null) {
					if (nameLabel.onMouseReleased(e)) {
						e.consume();
					}
				}
			}
		
		});
		
		nameLabelParent.addMouseMotionListener(new MouseMotionListener() {

			public void mouseDragged(MouseEvent e) {
				if (nameLabel != null) {
					if (nameLabel.onMouseDragged(e)) {
						e.consume();
					}
				}
			}

			public void mouseMoved(MouseEvent e) {
				if (nameLabel != null) {
					nameLabel.onMouseMoved(e, nameLabelParent);
				}
			}

		});
		
		nameLabelParent.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				if (nameLabel != null) {
					if (nameLabel.onKeyPressed(e, nameLabelParent)) {
						e.consume();
					}
				}
			}

			public void keyReleased(KeyEvent e) {
				if (nameLabel != null) {
					if (nameLabel.onKeyReleased(e, nameLabelParent)) {
						e.consume();
					}
				}

			}

			public void keyTyped(KeyEvent e) {
			}
			
		});
		
		nameLabel = new EmulatedTextItem(nameLabelParent, new Point(10, 25));
		nameLabel.setFontStyle(Font.BOLD);
		nameLabel.setFontSize(16);
		nameLabel.setBackgroundColor(Color.WHITE);	
		nameLabel.setText(trackModel.getName()); 

		nameLabel.addTextChangeListener(new TextChangeListener() { //  a little bit loopy!

			public void onTextChanged(Object source, String newLabel) {
				if (ExpandedTrackPopup.this.trackModel != null && !nameLabel.getText().equals(ExpandedTrackPopup.this.trackModel.getName())) {
					ExpandedTrackPopup.this.trackModel.setName(nameLabel.getText());
				}
			}

		});
		
		JPanel workArea = new JPanel(new BorderLayout());
		
		workArea.add(zoomedTrackView, BorderLayout.CENTER);
		workArea.add(timeAxis, BorderLayout.SOUTH);

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setTopComponent(workArea);
		splitPane.setBottomComponent(zoomController);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(1.0);

		// Create the toolbar
		JPanel toolBarPanel = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		toolBarPanel.add(playPauseButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		toolBarPanel.add(stopButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		toolBarPanel.add(rewindButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		toolBarPanel.add(soloButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		toolBarPanel.add(muteButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 5;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0,VOLUME_SPACING,0,VOLUME_SPACING);
		toolBarPanel.add(volumeSlider, c);
		
		c = new GridBagConstraints();
		c.gridx = 6;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0f;
		toolBarPanel.add(nameLabelParent, c);
		
		c = new GridBagConstraints();
		c.gridx = 7;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		toolBarPanel.add(closeButton, c);
		
		this.add(toolBarPanel, BorderLayout.NORTH);
		this.add(splitPane, BorderLayout.CENTER);
		
		// Observe track model
		trackModel.addObserver(zoomedTrackView);
		trackModel.addObserver(zoomController);
		trackModel.addObserver(this);
		
		// Setup mix to use
		zoomedTrackView.setMix(trackMix);
		
		// Setup the initial zoom
		zoomController.setZoom(frameStart, frameStart + frameLength);
		
		updateBorderColor();
	}
	
	/**
	 * Giver/receiver design pattern for only allowing the ExpandedTrackManager singleton
	 * to create expanded views.
	 * 
	 * @param trackModel
	 * 		The track model to observe.
	 * 
	 * @param trackMix
	 * 		The mix to use.
	 * 
	 * @param trackSourceFrameName
	 * 		Where the expanded tracks track source was expanded from.
	 * 		Must not be null
	 * 
	 * @param frameStart
	 * 		the initial zoom - clamped
	 * 
	 * @param frameLength
	 * 		the initial zoom - clamped
	 * 
	 * @throws NullPointerException
	 * 		If trackModel or trackMix or trackSourceFrameName or localFilename is null.
	 * 
	 */
	static void giveToExpandedTrackManager(
			SampledTrackModel trackModel, 
			TrackMixSubject trackMix,
			String trackSourceFrameName,
			int frameStart,
			int frameLength) {
		
		ExpandedTrackPopup xtp = new ExpandedTrackPopup(
				trackModel, 
				trackMix,
				trackSourceFrameName,
				frameStart, 
				frameLength);
		
		ExpandedTrackManager.getInstance().receiveExpandedTrackPopup(xtp);
	}

	@Override
	public void onShowing() {

	}
	
	@Override
	public void onShow() {
		
		if (splitPane.getHeight() < 140) {
			splitPane.setDividerLocation(splitPane.getHeight() - 30);
		} else {
			splitPane.setDividerLocation(splitPane.getHeight() - 60);
		}
		
		if (zoomedTrackView.getMix() != null) {
			updateVolume((int)(100 * zoomedTrackView.getMix().getVolume()));
			updateMute(zoomedTrackView.getMix().isMuted());
			updateSolo(SoundDesk.getInstance().isSolo(zoomedTrackView.getMix().getChannelID()));
		}
		
		SoundDesk.getInstance().addObserver(this);
		trackMix.addObserver(this);
		
		// Invalidate full frame for shading
		if (Browser._theBrowser != null) {
			FrameGraphics.refresh(false);
		}
	}

	@Override
	public void onHide() {
		ExpandedTrackManager.getInstance().expandedTrackPopupHidden(this);
		releaseMemory(); // get rid of buffer etc..
		
		SoundDesk.getInstance().removeObserver(this);
		trackMix.removeObserver(this);
		
		// Invalidate full frame for shading
		if (Browser._theBrowser != null) {
			FrameGraphics.refresh(false);
		}
	}
	
	


	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		g.translate(-getX(), -getY());
		nameLabel.paint(g);
		g.translate(getX(), getY());
		
	}

	public void actionPerformed(ActionEvent e) {
		if (trackModel == null) return;
		
		if (trackMix == null) return;

		if (e.getSource() == playPauseButton) {

			try {

				if (!SoundDesk.getInstance().isPlaying(trackMix.getChannelID())) { // play / resume
					
					int startFrame = -1, endFrame = -1;
					
					// Resume playback?
					if (SoundDesk.getInstance().isPaused(trackMix.getChannelID())) {
						startFrame = SoundDesk.getInstance().getLastPlayedFramePosition(trackMix.getChannelID());
						if (startFrame >= 0 && startFrame < trackModel.getFrameCount()) {
			
							// The user may have edited the audio track and reselected it
							// since the last pause. Thus select an appropriate end frame
							endFrame = (trackModel.getSelectionLength() > 1) ?
									trackModel.getSelectionStart() + trackModel.getSelectionLength():
										trackModel.getFrameCount() - 1;
								
							// Changed selection? it play range invalid?
							if (endFrame <= startFrame || startFrame < trackModel.getSelectionStart()) {
								startFrame = -1; // Play new selection (see below)
							
							} else if (endFrame >= trackModel.getFrameCount()) {
								endFrame = trackModel.getFrameCount() - 1;
							}		
							
						}
					}
					
					// Play from beginning of selection to end of selection
					if (startFrame < 0) {
						startFrame = trackModel.getSelectionStart();
						endFrame = (trackModel.getSelectionLength() > 1) ?
								startFrame + trackModel.getSelectionLength():
									trackModel.getFrameCount() - 1;
					}

					// Safety clamp:
					if (endFrame >= trackModel.getFrameCount()) {
						endFrame = trackModel.getFrameCount() - 1;
					}	
					
					if (startFrame < endFrame) {
						SoundDesk.getInstance().playSampledTrackModel(
								trackModel, 
								trackMix.getChannelID(), 
								startFrame, 
								endFrame, 
								0);
					}

				} else { // pause
					
					TrackSequence ts = SoundDesk.getInstance().getTrackSequence(trackMix.getChannelID());

					if (ts != null &&
							ts.isPlaying()) {
						
						// Mark channel as paused.
						SoundDesk.getInstance().setPaused(trackMix.getChannelID(), true);
						
						// Stop playback for this channel
						ApolloPlaybackMixer.getInstance().stop(ts);
						
					}
					
				}
				
			} catch (LineUnavailableException e1) {
				e1.printStackTrace();
			}
			
		} else if (e.getSource() == stopButton) {
			
			TrackSequence ts = SoundDesk.getInstance().getTrackSequence(trackMix.getChannelID());

			// reset any paused mark
			SoundDesk.getInstance().setPaused(trackMix.getChannelID(), false);
			
			if (ts != null &&
					ts.isPlaying()) {
				// Stop playback
				ApolloPlaybackMixer.getInstance().stop(ts);
			}
			
		} else if (e.getSource() == rewindButton) {
			
			trackModel.setSelection(0, 0);
			SoundDesk.getInstance().setPaused(trackMix.getChannelID(), false);
			
		} else if (e.getSource() == closeButton) {
			
			// If there are more expanded tracks showing then the ExpandedTrackManager
			// will automatically re-layout the remaing views.
			PopupManager.getInstance().hidePopup(this, 
					PopupManager.getInstance().new ExpandShrinkAnimator(
					new Rectangle(0, getY(), 1, getHeight()),
							Color.LIGHT_GRAY));
			
		}
		
	}
	
	/**
	 * Gets rid of image buffers and threads... Frees this instance from
	 * being referenced from the track model - very very important.
	 */
	void releaseMemory() {
		

		trackModel.removeObserver(zoomedTrackView);
		trackModel.removeObserver(zoomController);
		trackModel.removeObserver(this);
		
		assert (zoomedTrackView.getObservedSubject() == null);
		assert (zoomController.getObservedSubject() == null);

		zoomedTrackView.releaseBuffer();
		zoomController.releaseBuffer();
		
		SoundDesk.getInstance().removeObserver(this);
		trackMix.removeObserver(this);
		
	}
	
	/**
	 * @return The observed track model. Never null
	 */
	public Subject getObservedSubject() {
		return trackModel;
	}
	
	/**
	 * @return The observed track model.
	 */
	SampledTrackModel getObservedTrackModel() {
		return trackModel;
	}


	public void setObservedSubject(Subject parent) {
	}
	
	public void modelChanged(Subject source, SubjectChangedEvent event) {
		

		// Synch GUI with track state
		switch (event.getID()) {
		
		case ApolloSubjectChangedEvent.TRACK_SEQUENCE_CREATED: // from sound desk
			
			if (event.getState().equals(trackMix.getChannelID())) {
				// The channel being played is the same as this one ...
				// even if the track model is unloaded must enter into a playing state
				// if the created track sequence will play
				TrackSequence ts = SoundDesk.getInstance().getTrackSequence(trackMix.getChannelID());
				assert(ts != null);
				assert(!ts.hasFinished());
				assert(!ts.isPlaying());
				ts.addObserver(this);
			}

			break;
			
		case ApolloSubjectChangedEvent.PLAYBACK_STARTED: // From observed track sequence
			stopButton.setEnabled(true);
			rewindButton.setEnabled(false);
			playPauseButton.setIcon(IconRepository.getIcon("pause.png"));

			setBorderThickness(TrackWidgetCommons.PLAYING_TRACK_EDGE_THICKNESS);

			break;

		case ApolloSubjectChangedEvent.PLAYBACK_STOPPED: // From observed track sequence
			
			rewindButton.setEnabled(true);
			stopButton.setEnabled(false);
			playPauseButton.setIcon(IconRepository.getIcon("play.png"));
			
			// Note:
			// No need to remove self from observing the dead track since the track references this
			// and will get garbage collected
			
			setBorderThickness(TrackWidgetCommons.STOPPED_TRACK_EDGE_THICKNESS);


			break;
			
		case ApolloSubjectChangedEvent.PAUSE_MARK_CHANGED: // When stopped or paused
			/*
			if (ae.getState().equals(trackMix.getChannelID())) {
				
				if (MixDesk.getInstance().isPaused(trackMix.getChannelID())) {
					// Do nothing .. the paused mark is set prior to a stop
				} else {
					// Esnure that the GUI represents a stopped state
					stopButton.setEnabled(false);
					playPauseButton.setIcon(IconRepository.getIcon("play.png"));
				}
				
			}*/
			
			break;
			
		case ApolloSubjectChangedEvent.VOLUME: // From obseved track mix
			updateVolume((int)(100 * trackMix.getVolume()));
			break;
			
		case ApolloSubjectChangedEvent.MUTE: // From obseved track mix
			updateMute(trackMix.isMuted());
			updateBorderColor();
			break;
			
		case ApolloSubjectChangedEvent.SOLO_PREFIX_CHANGED: // From mix desk
			updateSolo(SoundDesk.getInstance().isSolo(trackMix.getChannelID()));
			updateBorderColor();
			break;
			
		case ApolloSubjectChangedEvent.NAME_CHANGED:
			if (!nameLabel.getText().equals(trackModel.getName())) {
				nameLabel.setText(trackModel.getName());
			}
			
			// Keep graph model consistant
			AudioStructureModel.getInstance().onTrackWidgetNameChanged(
					trackModel.getLocalFilename(), // may not be in model ... but does not matter in such a case
					null, 
					trackModel.getName());
			
			break;
			
		case ApolloSubjectChangedEvent.AUDIO_INSERTED: // from track model
		case ApolloSubjectChangedEvent.AUDIO_REMOVED:
			
			long newRunningTime = AudioMath.framesToMilliseconds(
					trackModel.getFrameCount(), trackModel.getFormat());
			assert(newRunningTime > 0);
			
			if (trackModel != null) {
				
				// Keep TrackGraphModel consistant
				AudioStructureModel.getInstance().onTrackWidgetAudioEdited(
						trackModel.getLocalFilename(), 
						null, 
						newRunningTime);
			}
			
			break;
		}
		
	}
	

	
	/**
	 * Updates the volume GUI. 
	 * {@link #volumeChanged()} is not raised as a result of this call.
	 * 
	 * @param vol
	 * 		The volume ranging from 0 - 100. Clamped.
	 */
	protected void updateVolume(int vol) {
		
		if (volumeSlider.getValue() == vol) return;

		// Clamp
		if(vol < 0) vol = 0;
		else if (vol > 100) vol = 100;
		
		isUpdatingGUI = true;
		
		volumeSlider.setValue(vol);
		
		isUpdatingGUI = false;
	}
	

	/**
	 * Updates the mute button GUI. 
	 * {@link #muteChanged()} is not raised as a result of this call.
	 * 
	 * @param isMuted
	 * 		True if set gui to muted.
	 */
	protected void updateMute(boolean isMuted) {
		
		if (muteButton.isSelected() == isMuted) return;

		isUpdatingGUI = true;
		
		muteButton.setSelected(isMuted);

		isUpdatingGUI = false;
	}


	/**
	 * Updates the solo button GUI. 
	 * {@link #muteChanged()} is not raised as a result of this call.
	 * 
	 * @param isSolo
	 * 		True if set gui to solo on.
	 */
	protected void updateSolo(boolean isSolo) {
		
		if (soloButton.isSelected() == isSolo) return;

		isUpdatingGUI = true;
		
		soloButton.setSelected(isSolo);

		isUpdatingGUI = false;
	}
	
	private void updateBorderColor() {
		
		// Get border color currently used
		Color oldC = getBorderColor();
		
		Color newC = TrackWidgetCommons.getBorderColor(
				SoundDesk.getInstance().isSolo(trackMix.getChannelID()), 
				trackMix.isMuted());

		// Update the color
		if (!newC.equals(oldC)) {
			setBorderColor(newC);
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

	/**
	 * @return
	 * 		The frame at where this expanded view came from.
	 * 		Never null.
	 */
	public String getTrackSourceFrameName() {
		return trackSourceFrameName;
	}
	
	
}