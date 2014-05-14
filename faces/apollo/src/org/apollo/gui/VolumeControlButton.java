package org.apollo.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apollo.io.IconRepository;
import org.expeditee.gui.Popup;
import org.expeditee.gui.PopupManager;

/**
 * A volume control button which can be reused in multiple situations... Follows swings MVC pattern.
 * 
 * @author Brook Novak
 */
public class VolumeControlButton extends JButton {

	private static final long serialVersionUID = 1L;

	private VolumeControlPopup vcPopup;
	
	private int volumeValue = 0;
	private boolean muted = false;
	
	private List<ChangeListener> changeListeners = new LinkedList<ChangeListener>();
	
	private String volumeTag;
	
	private Color SELECTED_BACKCOLOR = Color.GRAY;
	private Color UNSELECTED_BACKCOLOR = null;
	
	private static final int VOL_ADJUSTMENT = 10;
	private static final int VOL_HEIGHT = 150;

	/**
	 * @param volumeTag
	 * 		The tag line to appear before the volume value in the tip tool text.
	 * 		Null = no tag line.
	 * 		E.G. "Master Volume".
	 */
	public VolumeControlButton(String volumeTag) {
		super();
		UNSELECTED_BACKCOLOR = getBackground();
		vcPopup = new VolumeControlPopup();
		vcPopup.setVisible(false);
		this.volumeTag = volumeTag;
		toggleSelectionState(false);
		updateButtonGUI();
	}
	
	/**
	 * Adds a change listener which will be notified whenever the volume value
	 * changes via the GUI.
	 * 
	 * @param listener
	 */
	public void addVolumeChangeListener(ChangeListener listener) {
		assert(listener != null);
		changeListeners.add(listener);
	}
	
	private void fireVolumeChanged() {
		for (ChangeListener a : changeListeners) {
			a.stateChanged(new ChangeEvent(this));
		}
	}

	private void toggleSelectionState(boolean selected) {
		if (selected) setBackground(SELECTED_BACKCOLOR);
		else setBackground(UNSELECTED_BACKCOLOR);
	}
	
	/**
	 * {@inheritDoc}
	 * Displays the volume control popup when the button is pressed
	 */
	@Override
	protected void fireActionPerformed(ActionEvent event) {
		super.fireActionPerformed(event);
		
		if (!vcPopup.isVisible()) { // show popup
			
			vcPopup.showPopup();

		} else { // hide popup

			PopupManager.getInstance().hidePopup(vcPopup, 
					PopupManager.getInstance().new ExpandShrinkAnimator(
							VolumeControlButton.this.getBounds(),
							Color.LIGHT_GRAY));
		}
	}

	/**
	 * Sets the volume GUI. This will not fire any event - it will just update the gui.
	 * @param n
	 * return true if changed
	 */
	public boolean setVolumeValue(int n) {
		
		// Clamp
		if (n > 100) n = 100;
		else if (n < 0) n = 0;
		
		if (n != volumeValue) {
			volumeValue = n;
			updateButtonGUI();
			vcPopup.updatePopupGUI();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Sets the volume GUI. This will not fire any event - it will just update the gui.
	 * 
	 * @param mute
	 * 
	 * @return
	 */
	public void setMuted(boolean mute) {
		muted = mute;
		updateButtonGUI();
	}
		

	private void updateButtonGUI() {
		
		Icon newIcon;
		if(muted)  
				newIcon = IconRepository.getIcon("volmute.png");
		else if (volumeValue <= 25) 
				newIcon = IconRepository.getIcon("vol25.png");
		else if (volumeValue <= 50) 
			newIcon = IconRepository.getIcon("vol50.png");
		else if (volumeValue <= 75) 
			newIcon = IconRepository.getIcon("vol75.png");
		else // maxing
				newIcon = IconRepository.getIcon("vol100.png");

		if (newIcon != getIcon()) setIcon(newIcon);
		
		if (muted) {
			if (volumeTag != null) setToolTipText(volumeTag + ": Muted");
			else setToolTipText("Muted");
		}
		else {
			if (volumeTag != null) setToolTipText(volumeTag + ": " + volumeValue + "%");
			else setToolTipText(volumeValue + "%");
		}
	}

	/**
	 * 
	 * @return The volume value of the gui
	 */
	public int getVolumeValue() {
		return volumeValue;
	}
	

	/**
	 * 
	 * @return The mute value of the gui
	 */
	public boolean getMuteValue() {
		return muted;
	}

	private class VolumeControlPopup extends Popup {
		
		private static final long serialVersionUID = 1L;
	
		private JSlider volumeSlider;
		private JButton increaseButton;
		private JButton decreaseButton;
		
		/**
		 * Constructor.
		 */
		public VolumeControlPopup() {
			super();

			volumeSlider = new JSlider(JSlider.VERTICAL);
			volumeSlider.setMinimum(0);
			volumeSlider.setMaximum(100);
			volumeSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					onVolumeSliderChanged();
				}
			});

			increaseButton = new JButton();
			//increaseButton.setIcon(IconRepository.getIcon("incvol.ico"));
			increaseButton.setText("+");
			increaseButton.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) { 
					onIncrementVolume();
				}
			});
			
			decreaseButton = new JButton();
			//decreaseButton.setIcon(IconRepository.getIcon("decvol.ico"));
			decreaseButton.setText("-");
			decreaseButton.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) { 
					onDeincrementVolume();
				}
			});
	
			updatePopupGUI();
		}
		
		private void onIncrementVolume() {
			if (setVolumeValue(volumeValue + VOL_ADJUSTMENT)) {
				fireVolumeChanged();
			}
		}
		
		private void onDeincrementVolume() {
			if (setVolumeValue(volumeValue - VOL_ADJUSTMENT)) {
				fireVolumeChanged();
			}
		}
		
		private void onVolumeSliderChanged() {
			if (setVolumeValue(volumeSlider.getValue())) {
				fireVolumeChanged();
			}
		}
		
		private void updatePopupGUI() {
			volumeSlider.setValue(volumeValue);
			increaseButton.setEnabled(volumeValue < 100);
			decreaseButton.setEnabled(volumeValue > 0);
		}
		
		private void prepLayout() {
			
			if (this.getComponents().length == 0) {
				
				final int INNER_SPACING = 0;
				final int OUTER_SPACING = 0;
				final int YPAD = 0;
				final int BUTTON_MARGIN = 0;
				
				int targetWidth = VolumeControlButton.this.getWidth();
				Dimension buttonSizes = new Dimension(targetWidth,  targetWidth);
				
				decreaseButton.setPreferredSize(buttonSizes);
				increaseButton.setPreferredSize(buttonSizes); 
				volumeSlider.setPreferredSize(new Dimension(volumeSlider.getWidth(), VOL_HEIGHT));
				decreaseButton.setSize(buttonSizes);
				increaseButton.setSize(buttonSizes); 
				volumeSlider.setSize(new Dimension(volumeSlider.getWidth(), VOL_HEIGHT));

				increaseButton.setMargin(new Insets(BUTTON_MARGIN,BUTTON_MARGIN,BUTTON_MARGIN,BUTTON_MARGIN));
				decreaseButton.setMargin(increaseButton.getMargin());
	
				GridBagLayout layout = new GridBagLayout();
		
				layout.rowHeights = new int[] {
						increaseButton.getHeight(), volumeSlider.getHeight(), decreaseButton.getHeight()
				};
				
				setLayout(layout);

				GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 0;
				c.fill = GridBagConstraints.BOTH;
				c.insets = new Insets(OUTER_SPACING,OUTER_SPACING,INNER_SPACING,OUTER_SPACING);
				c.ipady = YPAD;
				this.add(increaseButton, c);
				
				c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 1;
				c.fill = GridBagConstraints.BOTH;
				c.insets = new Insets(INNER_SPACING,OUTER_SPACING,INNER_SPACING,OUTER_SPACING);
				c.ipady = YPAD;
				this.add(volumeSlider, c);
				
				c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 2;
				c.fill = GridBagConstraints.BOTH;
				c.insets = new Insets(INNER_SPACING,OUTER_SPACING,OUTER_SPACING,OUTER_SPACING);
				c.ipady = YPAD;
				this.add(decreaseButton, c);
				
				setSize(targetWidth, volumeSlider.getHeight() + (2 * buttonSizes.height) 
						+ (2 * OUTER_SPACING));
				this.doLayout();
			}

		}
		
		private void showPopup() {
			prepLayout();

			// Determine where popup should show
			int x = VolumeControlButton.this.getX();
			int y = VolumeControlButton.this.getY() - getHeight() - 2; // by default show above
			
			if (y < 0) {
				y = VolumeControlButton.this.getY() + VolumeControlButton.this.getHeight() + 2;
			}

			PopupManager.getInstance().showPopup(
					this, 
					new Point(x, y), 
					VolumeControlButton.this,
					PopupManager.getInstance().new ExpandShrinkAnimator(
							VolumeControlButton.this.getBounds(),
							Color.LIGHT_GRAY));
		}

		@Override
		public void onHide() {
			// Toggle state = volume popup showing
			toggleSelectionState(false);
		}

		@Override
		public void onShow() {
			// Toggle state = volume popup showing
			toggleSelectionState(true);
		}

		@Override
		public boolean shouldConsumeBackClick() {
			return true;
		}
		
		
		
	}

}