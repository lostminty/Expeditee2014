package org.apollo.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apollo.io.IconRepository;
import org.expeditee.gui.Popup;

/**
 * A pure GUI for playback and mixing popup used for tracks and linked tracks.
 * Does not implement beahviour - just the gui.
 * 
 * @author Brook Novak
 *
 */
public abstract class PlaybackControlPopup extends Popup implements ActionListener {

	public JButton playPauseButton;
	public JButton stopButton;
	public JButton rewindButton;
	public JToggleButton muteButton;
	public JToggleButton soloButton;
	public JButton miscButton;
	public JSlider volumeSlider;
	
	private boolean isUpdatingGUI = false;

	private static final int BUTTON_SIZE = 40;
	
	protected PlaybackControlPopup() {
		super(new GridBagLayout());
		
		playPauseButton = new JButton();
		playPauseButton.setActionCommand("playpause");
		playPauseButton.addActionListener(this);
		playPauseButton.setIcon(IconRepository.getIcon("play.png"));
		playPauseButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		playPauseButton.setToolTipText("Play selection / Pause");
		
		stopButton = new JButton();
		stopButton.setEnabled(false);
		stopButton.addActionListener(this);
		stopButton.setActionCommand("stop");
		stopButton.setIcon(IconRepository.getIcon("stop.png"));
		stopButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		stopButton.setToolTipText("Stop playback");
		
		rewindButton = new JButton();
		rewindButton.addActionListener(this);
		rewindButton.setActionCommand("rewind");
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
				if (!PlaybackControlPopup.this.isUpdatingGUI) {
					muteChanged();
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
				if (!PlaybackControlPopup.this.isUpdatingGUI) {
					soloChanged();
				}
			}
		});
		
		miscButton = new JButton();
		miscButton.addActionListener(this);
		miscButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		
		final int VOLUME_SPACING = 6;
		volumeSlider = new JSlider(JSlider.HORIZONTAL);
		volumeSlider.setPreferredSize(new Dimension(2 * (BUTTON_SIZE - VOLUME_SPACING), BUTTON_SIZE));
		volumeSlider.setMinimum(0);
		volumeSlider.setMaximum(100);
		volumeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!PlaybackControlPopup.this.isUpdatingGUI) {
					volumeChanged();
				}
				// Update the icons
				updateButtonGUI();
			}
		});
		

		// Create the toolbar
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		add(playPauseButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		add(stopButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		add(rewindButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		add(soloButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		add(muteButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 5;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0,VOLUME_SPACING,0,VOLUME_SPACING);
		add(volumeSlider, c);
		
		c = new GridBagConstraints();
		c.gridx = 7;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		add(miscButton, c);
		
//
//		GridBagConstraints c = new GridBagConstraints();
//		c.gridx = 0;
//		c.gridy = 0;
//		c.fill = GridBagConstraints.BOTH;
//		this.add(soloButton, c);
//		
//		c = new GridBagConstraints();
//		c.gridx = 1;
//		c.gridy = 0;
//		c.fill = GridBagConstraints.BOTH;
//		this.add(muteButton, c);
//
//		c = new GridBagConstraints();
//		c.gridx = 2;
//		c.gridy = 0;
//		c.gridwidth = 2;
//		c.insets = new Insets(0,VOLUME_SPACING,0,VOLUME_SPACING);
//		c.fill = GridBagConstraints.BOTH;
//		this.add(volumeSlider, c);
//		
//		c = new GridBagConstraints();
//		c.gridx = 0;
//		c.gridy = 1;
//		c.fill = GridBagConstraints.BOTH;
//		this.add(playPauseButton, c);
//		
//		c = new GridBagConstraints();
//		c.gridx = 1;
//		c.gridy = 1;
//		c.fill = GridBagConstraints.BOTH;
//		this.add(stopButton, c);
//		
//		c = new GridBagConstraints();
//		c.gridx = 2;
//		c.gridy = 1;
//		c.fill = GridBagConstraints.BOTH;
//		this.add(rewindButton, c);
//
//		c = new GridBagConstraints();
//		c.gridx = 3;
//		c.gridy = 1;
//		c.fill = GridBagConstraints.BOTH;
//		this.add(miscButton, c);
//		
//		this.setSize(BUTTON_SIZE * 4, BUTTON_SIZE * 2);
		
		this.setSize(BUTTON_SIZE * 8, BUTTON_SIZE);
		
		this.doLayout();
		
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


	public abstract void actionPerformed(ActionEvent e);
	
	/**
	 * Invoke when the slider changes via the user (i.e. not from a model-changed to event)
	 */
	protected abstract void volumeChanged();
	
	/**
	 * Invoke when mute button toggles via the user (i.e. not from a model-changed to event)
	 */
	protected abstract void muteChanged();
	
	/**
	 * Invoke when solo button toggles via the user (i.e. not from a model-changed to event)
	 */
	protected abstract void soloChanged();
	
	
	/**
	 * Updates the volume GUI. 
	 * {@link #volumeChanged()} is not raised as a result of this call.
	 * 
	 * @param vol
	 * 		The volume ranging from 0 - 100. Clamped.
	 */
	public void updateVolume(int vol) {
		
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
	public void updateMute(boolean isMuted) {
		
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
	public void updateSolo(boolean isSolo) {
		
		if (soloButton.isSelected() == isSolo) return;

		isUpdatingGUI = true;
		
		soloButton.setSelected(isSolo);

		isUpdatingGUI = false;
	}
	
			
}