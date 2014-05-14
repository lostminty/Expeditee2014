package org.apollo.gui;

/**
 * 
 * @author Brook Novak
 * @deprecated
 */
public class SoundDeskPopup {

	
	//
//		
//		/**
//		 * Why doesn't java support partial classes!
//		 * 
//		 * @author Brook Novak
//		 *
//		 */
//		private class SoundDeskPopup extends Popup implements ActionListener {
//			
//			private JButton playPauseButton;
//			private JButton stopButton;
//			private JButton rewindButton;
//			private JButton closeButton;
//			private JToggleButton soloButton;
//			private JToggleButton muteButton;
//			private JSlider volumeSlider;
//			private JPanel nameLabelParent;
//			private JTree trackTree;
//			private JButton scrollLeftButton;
//			private JButton scrollRightButton;
//			private JPanel channelPane;
//			
//			private EmulatedTextItem nameLabel; 
//			
//			private boolean isUpdatingGUI = false;
//			
//			/**
//			 * Initializes the GUI
//			 */
//			SoundDeskPopup() {
//				
//				super(new BorderLayout());
	//
//				JPanel toolbar = createToolbar();
//				
//				JPanel optionsPane = createOptionsPane();
//				
//				JSplitPane centerStage = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
//				
//				
//				
//			}
//			
//			/**
//			 * Creates the "toolbar" like appearance at the top
//			 * 
//			 * @return
//			 * 		The JPanel containing the toolbar buttons.
//			 */
//			private JPanel createToolbar() {
//				
//				final int BUTTON_SIZE = 40;
//				
//				playPauseButton = new JButton();
//				playPauseButton.addActionListener(this);
//				playPauseButton.setIcon(IconRepository.getIcon("play.png"));
//				playPauseButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
//				playPauseButton.setToolTipText("Play selection / Pause");
//				
//				stopButton = new JButton();
//				stopButton.setEnabled(false);
//				stopButton.addActionListener(this);
//				stopButton.setIcon(IconRepository.getIcon("stop.png"));
//				stopButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
//				stopButton.setToolTipText("Stop playback");
//				
//				rewindButton = new JButton();
//				rewindButton.addActionListener(this);
//				rewindButton.setIcon(IconRepository.getIcon("rewind.png"));
//				rewindButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
//				rewindButton.setToolTipText("Rewind to start");
//				
//				// Icon changes
//				muteButton = new JToggleButton();
//				muteButton.setSelectedIcon(IconRepository.getIcon("volmute.png"));
//				muteButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
//				muteButton.setToolTipText("Toggle mute");
//				muteButton.addChangeListener(new ChangeListener() {
//					public void stateChanged(ChangeEvent e) {
//						if (!isUpdatingGUI) {
//							masterMix.setMuted(muteButton.isSelected());
//							
//						}
//					}
//				});
//				
//				soloButton = new JToggleButton();
//				soloButton.setIcon(IconRepository.getIcon("solo.png"));
//				soloButton.setSelectedIcon(IconRepository.getIcon("soloon.png"));
//				soloButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
//				soloButton.setToolTipText("Toggle solo");
//				soloButton.addChangeListener(new ChangeListener() {
//					public void stateChanged(ChangeEvent e) {
//						if (!isUpdatingGUI) {
//							SoundDesk.getInstance().setSoloIDPrefix(soloButton.isSelected() ?
//									masterMix.getChannelID() : null
//									);
//							
//						}
//					}
//				});
//				
//				closeButton = new JButton();
//				closeButton.addActionListener(this);
//				closeButton.setIcon(IconRepository.getIcon("close.png"));
//				closeButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
//				closeButton.setToolTipText("Close");
//				
//				final int VOLUME_SPACING = 6;
//				volumeSlider = new JSlider(JSlider.HORIZONTAL);
//				volumeSlider.setMinimum(0);
//				volumeSlider.setMaximum(100);
//				volumeSlider.addChangeListener(new ChangeListener() {
//					public void stateChanged(ChangeEvent e) {
//						if (!isUpdatingGUI) {
//							masterMix.setVolume(((float)volumeSlider.getValue()) / 100.0f);
//						}
//						// Update the icons
//						updateButtonGUI();
//					}
//				});
//				
//				volumeSlider.setPreferredSize(new Dimension(100 - (2 * VOLUME_SPACING), BUTTON_SIZE));
//				
	//
//				nameLabelParent = new JPanel();
//				
//				nameLabelParent.addMouseListener(new MouseListener() {
	//
//					public void mouseClicked(MouseEvent e) {
//						if (nameLabel != null) {
//							if (nameLabel.onMouseClicked(e)) {
//								e.consume();
//								return;
//							}
//						}
//					}
	//
//					public void mouseEntered(MouseEvent e) {
//					}
	//
//					public void mouseExited(MouseEvent e) {
//					}
	//
//					public void mousePressed(MouseEvent e) {
//						if (nameLabel != null) {
//							if (nameLabel.onMousePressed(e)) {
//								e.consume();
//							}
//						}
//					}
	//
//					public void mouseReleased(MouseEvent e) {
//						if (nameLabel != null) {
//							if (nameLabel.onMouseReleased(e)) {
//								e.consume();
//							}
//						}
//					}
//				
//				});
//				
//				nameLabelParent.addMouseMotionListener(new MouseMotionListener() {
	//
//					public void mouseDragged(MouseEvent e) {
//						if (nameLabel != null) {
//							if (nameLabel.onMouseDragged(e)) {
//								e.consume();
//							}
//						}
//					}
	//
//					public void mouseMoved(MouseEvent e) {
//						if (nameLabel != null) {
//							nameLabel.onMouseMoved(e, nameLabelParent);
//						}
//					}
	//
//				});
//				
//				nameLabelParent.addKeyListener(new KeyListener() {
	//
//					public void keyPressed(KeyEvent e) {
//						if (nameLabel != null) {
//							if (nameLabel.onKeyPressed(e, nameLabelParent)) {
//								e.consume();
//							}
//						}
//					}
	//
//					public void keyReleased(KeyEvent e) {
//						if (nameLabel != null) {
//							if (nameLabel.onKeyReleased(e, nameLabelParent)) {
//								e.consume();
//							}
//						}
	//
//					}
	//
//					public void keyTyped(KeyEvent e) {
//					}
//					
//				});
//				
//				nameLabel = new EmulatedTextItem(nameLabelParent, new Point(10, 25));
//				nameLabel.setFontStyle(Font.BOLD);
//				nameLabel.setFontSize(16);
//				nameLabel.setBackgroundColor(Color.WHITE);	
//				nameLabel.setText(LinkedTrack.this.getName()); 
	//
//				nameLabel.addTextChangeListener(new TextChangeListener() { //  a little bit loopy!
	//
//					public void onTextChanged(Object source, String newLabel) {
//						if (!nameLabel.getText().equals(LinkedTrack.this.getName())) {
//							SoundDeskPopup.this.setName(nameLabel.getText());
//						}
//					}
	//
//				});
//				
//				// Create the toolbar
//				JPanel toolBarPanel = new JPanel(new GridBagLayout());
	//
//				GridBagConstraints c = new GridBagConstraints();
//				c.gridx = 0;
//				c.gridy = 0;
//				c.fill = GridBagConstraints.BOTH;
//				toolBarPanel.add(playPauseButton, c);
//				
//				c = new GridBagConstraints();
//				c.gridx = 1;
//				c.gridy = 0;
//				c.fill = GridBagConstraints.BOTH;
//				toolBarPanel.add(stopButton, c);
//				
//				c = new GridBagConstraints();
//				c.gridx = 2;
//				c.gridy = 0;
//				c.fill = GridBagConstraints.BOTH;
//				toolBarPanel.add(rewindButton, c);
//				
//				c = new GridBagConstraints();
//				c.gridx = 3;
//				c.gridy = 0;
//				c.fill = GridBagConstraints.BOTH;
//				toolBarPanel.add(soloButton, c);
//				
//				c = new GridBagConstraints();
//				c.gridx = 4;
//				c.gridy = 0;
//				c.fill = GridBagConstraints.BOTH;
//				toolBarPanel.add(muteButton, c);
//				
//				c = new GridBagConstraints();
//				c.gridx = 5;
//				c.gridy = 0;
//				c.fill = GridBagConstraints.BOTH;
//				c.insets = new Insets(0,VOLUME_SPACING,0,VOLUME_SPACING);
//				toolBarPanel.add(volumeSlider, c);
//				
//				c = new GridBagConstraints();
//				c.gridx = 6;
//				c.gridy = 0;
//				c.fill = GridBagConstraints.BOTH;
//				c.weightx = 1.0f;
//				toolBarPanel.add(nameLabelParent, c);
//				
//				c = new GridBagConstraints();
//				c.gridx = 7;
//				c.gridy = 0;
//				c.fill = GridBagConstraints.BOTH;
//				toolBarPanel.add(closeButton, c);
//				
//				return toolBarPanel;
	//
//			}
//			
//			private JPanel createOptionsPane() {
//				
//				JPanel panel = new JPanel(new BorderLayout());
//				
//				JLabel tmpLabl = new JLabel("todo: Options - check boxes");
//				tmpLabl.setPreferredSize(new Dimension(100, 30));
//				panel.add(tmpLabl, BorderLayout.CENTER);
//				
//				return panel;
//			}
//			
//			private JSplitPane createCenterStage() {
//				
//				DefaultMutableTreeNode root = new DefaultMutableTreeNode();
//				createTreeNodes(root);
//				trackTree = new JTree(root);
//				
//				
//				scrollRightButton = new JButton(">");
//		
//				scrollLeftButton = new JButton("<"); 
//				
//				channelPane = new JPanel();
//				
//				
//				JPanel leftComposite = new JPanel(new BorderLayout());
//		
//				JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
//				
//				// todo: Wrap with scroll pane
//				
//				sp.setLeftComponent(trackTree);
//				sp.setLeftComponent(trackTree);
//				
//				return sp;
//			}
//			
//			private void createTreeNodes(DefaultMutableTreeNode root) {
//				
//			}
//			
//			public void actionPerformed(ActionEvent e) {
//				
//				if (!(state == PLAYING || state == STOPPED)) return;
//				
//				if (e.getSource() == closeButton) {
//					
//					// todo ... close
//					
//				} else {
//					
//					// Relay shared action
//					masterActionListener.actionPerformed(e);
//					
//				}
//				
//			}
//			
//			/**
//			 * Sets the mute icon to represent the current volume value in the slider.
//			 * Note: this is not the icon if mute is on.
//			 */
//			private void updateButtonGUI() {
//				
//				Icon newIcon = null;
//				if (volumeSlider.getValue() <= 25) 
//						newIcon = IconRepository.getIcon("vol25.png");
//				else if (volumeSlider.getValue() <= 50) 
//					newIcon = IconRepository.getIcon("vol50.png");
//				else if (volumeSlider.getValue() <= 75) 
//					newIcon = IconRepository.getIcon("vol75.png");
//				else // maxing
//						newIcon = IconRepository.getIcon("vol100.png");
//				
//				muteButton.setIcon(newIcon);
//			}
//			
//			/**
//			 * Updates the volume GUI. 
//			 * {@link #volumeChanged()} is not raised as a result of this call.
//			 * 
//			 * @param vol
//			 * 		The volume ranging from 0 - 100. Clamped.
//			 */
//			protected void updateVolume(int vol) {
//				
//				if (volumeSlider.getValue() == vol) return;
	//
//				// Clamp
//				if(vol < 0) vol = 0;
//				else if (vol > 100) vol = 100;
//				
//				isUpdatingGUI = true;
//				
//				volumeSlider.setValue(vol);
//				
//				isUpdatingGUI = false;
//			}
//			
	//
//			/**
//			 * Updates the mute button GUI. 
//			 * {@link #muteChanged()} is not raised as a result of this call.
//			 * 
//			 * @param isMuted
//			 * 		True if set gui to muted.
//			 */
//			protected void updateMute(boolean isMuted) {
//				
//				if (muteButton.isSelected() == isMuted) return;
	//
//				isUpdatingGUI = true;
//				
//				muteButton.setSelected(isMuted);
	//
//				isUpdatingGUI = false;
//			}
	//
	//
//			/**
//			 * Updates the solo button GUI. 
//			 * {@link #muteChanged()} is not raised as a result of this call.
//			 * 
//			 * @param isSolo
//			 * 		True if set gui to solo on.
//			 */
//			protected void updateSolo(boolean isSolo) {
//				
//				if (soloButton.isSelected() == isSolo) return;
	//
//				isUpdatingGUI = true;
//				
//				soloButton.setSelected(isSolo);
	//
//				isUpdatingGUI = false;
//			}
//			
//			
//		}
}
