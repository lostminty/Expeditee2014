package org.apollo.widgets;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apollo.audio.ApolloPlaybackMixer;
import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.SampledAudioManager;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;

public class AudioConfiguration extends InteractiveWidget implements Observer {
	
	private static MixerItem[] inputMixerItems = null;

	private JComboBox inputMixureCombo;
	private JSlider masterVolumeSlider;
	private JCheckBox masterMuteCheckBox;
	private JLabel inputMixerDescription;
	
	public AudioConfiguration(Text source, String[] args) {
		super(source, new JPanel(), 260, -1, 150, 150);
		
		Font titleFont = new Font("Arial", Font.BOLD, 12);
		Font descFont = new Font("Arial", Font.PLAIN, 12);

		inputMixureCombo = new JComboBox(getInputMixerItems());
		
		inputMixureCombo.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) { 
				if (inputMixerItems.length > 0) {
					SampledAudioManager.getInstance().setCurrentInputMixure(
							((MixerItem)inputMixureCombo.getSelectedItem())._inf);
				}
			}
		});
		
		masterVolumeSlider = new JSlider(); 
		masterVolumeSlider.setMinimum(0);
		masterVolumeSlider.setMaximum(100);
		masterVolumeSlider.setMaximumSize(new Dimension(300, 20));

		masterVolumeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				float f = (float)masterVolumeSlider.getValue();
				f /= 100.0f;
				ApolloPlaybackMixer.getInstance().setMasterVolume(f);
			}
		});
		
		masterMuteCheckBox = new JCheckBox(); 
		masterMuteCheckBox.setText("Master Mute");
		masterMuteCheckBox.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				ApolloPlaybackMixer.getInstance().setMasterMute(masterMuteCheckBox.isSelected());
			}
		});
		
		
		inputMixerDescription = new JLabel();
		inputMixerDescription.setFont(descFont);
		
		JLabel masterVolumeLevelLabel = new JLabel("Master volume:");
		masterVolumeLevelLabel.setFont(titleFont);
		
		JLabel deviceLabel = new JLabel("Input device:");
		deviceLabel.setFont(titleFont);
		// Assemble
		JPanel mixerPane = new JPanel(new GridBagLayout());
		
		

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0,10,0,0);
		mixerPane.add(deviceLabel, c);
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.0f;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0,10,0,10);
		mixerPane.add(inputMixureCombo, c);
		
		mixerPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		masterVolumeLevelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		inputMixerDescription.setAlignmentX(Component.CENTER_ALIGNMENT);
		masterVolumeSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
		masterMuteCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		_swingComponent.add(Box.createRigidArea(new Dimension(1, 12)));
		_swingComponent.setLayout(new BoxLayout(_swingComponent, BoxLayout.Y_AXIS));
		_swingComponent.add(mixerPane);
		_swingComponent.add(Box.createRigidArea(new Dimension(1, 4)));
		_swingComponent.add(inputMixerDescription);
		_swingComponent.add(Box.createRigidArea(new Dimension(1, 12)));
		_swingComponent.add(masterVolumeLevelLabel);
		_swingComponent.add(masterVolumeSlider);
		_swingComponent.add(Box.createRigidArea(new Dimension(1, 12)));
		_swingComponent.add(masterMuteCheckBox);
		_swingComponent.add(Box.createRigidArea(new Dimension(1, 12)));
		
	
		
		// Set state to match SampledAudioManager
		updateSelectedMixers();
		updateMasterVolumeControl();
		updateMasterMuteControl();
		
		_swingComponent.doLayout();
		
	}
	
	@Override
	protected void onParentStateChanged(int eventType) {
		super.onParentStateChanged(eventType);
		
		switch (eventType) {

		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
			ApolloPlaybackMixer.getInstance().removeObserver(this);
			SampledAudioManager.getInstance().removeObserver(this);
			break;

		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
			ApolloPlaybackMixer.getInstance().addObserver(this);
			SampledAudioManager.getInstance().addObserver(this);
			break;

		}
	}
	
	
	/**
	 * Syncs the I/O mixer combos to the current I/O mixers set in the SampledAudioManager
	 *
	 */
	private void updateSelectedMixers() {
		
		// Update selected input mixer
		Mixer.Info inf = SampledAudioManager.getInstance().getCurrentInputMixureInfo();
		
		for (MixerItem mi : inputMixerItems) {
			if (mi._inf.equals(inf)) {
				if (inputMixureCombo.getSelectedItem() != mi)
					inputMixureCombo.setSelectedItem(mi);
			}
		}
		
		/*StringBuilder desc = new StringBuilder(inf.getDescription().length());
		int i = 0;
		final int CUTTOFF_LENGTH = 10;
		while (i < inf.getDescription().length()) {
			String part = null;
			
			if ((i + CUTTOFF_LENGTH) > inf.getDescription().length()) {
				part = inf.getDescription().substring(i);
			} else {
				part = inf.getDescription().substring(i, i + CUTTOFF_LENGTH);
			}
			desc.append(part);
			desc.append('\n');
			i += CUTTOFF_LENGTH;
		}*/
		
		inputMixerDescription.setText(inf.getDescription());
		
	}
	
	/**
	 * Synchs volumeSlider with master volume state.
	 */
	private void updateMasterVolumeControl() {
		
		int mv = (int)(ApolloPlaybackMixer.getInstance().getMasterVolume() * 100.0f);
		
		if (masterVolumeSlider.getValue() != mv) {
			masterVolumeSlider.setValue(mv);
		}
		
	}

	/**
	 * Synchs mute checkbox with master mute state.
	 */
	private void updateMasterMuteControl() {
		
		if (this.masterMuteCheckBox.isSelected() != ApolloPlaybackMixer.getInstance().isMasterMuteOn()) {
			masterMuteCheckBox.setSelected(!masterMuteCheckBox.isSelected());
		}
		
		if (masterMuteCheckBox.isSelected()) {
			masterMuteCheckBox.setText("Master Mute (on)");
		} else {
			masterMuteCheckBox.setText("Master Mute (off)");
		}
		
	}

	
	@Override
	protected String[] getArgs() {
		return null;
	}
	
	/**
	 * Obtain currently available mixers to choose from
	 */
	private MixerItem[] getInputMixerItems() {
		
		List<Mixer.Info> mixers;
		
		if (inputMixerItems == null) {
			
			mixers = SampledAudioManager.getInstance().getSupportedInputMixures();
			inputMixerItems = new MixerItem[mixers.size()];
			for (int i = 0; i < inputMixerItems.length; i++) {
				inputMixerItems[i] = new MixerItem(mixers.get(i));
			}
			
		}
		return inputMixerItems;
	}
	
	public Subject getObservedSubject() {
		return SampledAudioManager.getInstance();
	}

	public void modelChanged(Subject source, SubjectChangedEvent event) {

		switch (event.getID()) {
		case ApolloSubjectChangedEvent.VOLUME:
			updateMasterVolumeControl();
			break;
		case ApolloSubjectChangedEvent.MUTE:
			updateMasterMuteControl();
			break;
		case ApolloSubjectChangedEvent.INPUT_MIXER:
			updateSelectedMixers();
			break;
		}

	}

	public void setObservedSubject(Subject parent) {
	}

	
	/**
	 * Used by combobox for selecting a mixer
	 * @author Brook Novak
	 */
	class MixerItem {
		
		Mixer.Info _inf;
		int targetlineCount;
		
		MixerItem(Mixer.Info inf) {
			if (inf == null) throw new NullPointerException("inf");
			_inf = inf;
			targetlineCount = AudioSystem.getMixer(_inf).getTargetLineInfo().length;
		}
		
		@Override
		public String toString() {
			return _inf.getName();
			// return _inf.getVendor() + " " +_inf.getName() + " " +_inf.getVersion();
			//return _inf.getDescription() + " [" + targetlineCount + " target lines]";
		}

	}
}
