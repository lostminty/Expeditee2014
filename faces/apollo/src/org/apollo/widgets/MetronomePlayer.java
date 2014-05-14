package org.apollo.widgets;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.Metronome;
import org.apollo.io.IconRepository;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;

/**
 * A widget to provide an interface for the simple Metronome facility in Apollo
 * 
 * MVC Design pattern
 * 
 * @author Brook Novak
 *
 */
public class MetronomePlayer extends InteractiveWidget implements Observer {
	
	private JLabel tempoLabel;
	private JSlider bpmSlider;
	private JButton startStopButton;
	private JComboBox beatsPerMeasureCombo;
	private JCheckBox enabledCheckbox;
	private boolean interfaceIsUpdating = false;
	
	private static final int MAX_BPM = 300;

	public MetronomePlayer(Text source, String[] args) {
		super(source, new JPanel(), 130, 130, 175, 175);
		
		tempoLabel = new JLabel();
		bpmSlider = new JSlider();
		bpmSlider.setMinimum(1);
		bpmSlider.setMaximum(MAX_BPM);
		bpmSlider.setMajorTickSpacing(20);
		bpmSlider.setValue(100);
		bpmSlider.setPreferredSize(new Dimension(120, 20));
		bpmSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				
				if (!interfaceIsUpdating) {
					try {
						Metronome.getInstance().setTempo(bpmSlider.getValue());
					} catch (MidiUnavailableException e1) {
						e1.printStackTrace();
					} 
				}
				
			}
		});
		
		startStopButton = new JButton();
		startStopButton.setPreferredSize(new Dimension(40, 40));
		startStopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Metronome.getInstance().isPlaying()) {
					Metronome.getInstance().stop();
				} else {
					try {
						Metronome.getInstance().start();
					} catch (MidiUnavailableException e1) {
						e1.printStackTrace();
					} 
				}
			}
		});

		beatsPerMeasureCombo = new JComboBox(new String[]{"1", "2", "3", "4", "5", "6", "7", "8"});
		beatsPerMeasureCombo.setPreferredSize(new Dimension(60, 25));
		beatsPerMeasureCombo.setEditable(false);
		beatsPerMeasureCombo.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				
				if (!interfaceIsUpdating) {
					Metronome.getInstance().setBeatsPerMeasure(beatsPerMeasureCombo.getSelectedIndex() + 1);
				}
				
			}
			
		});
		
		enabledCheckbox = new JCheckBox("Auto-start");
		enabledCheckbox.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (!interfaceIsUpdating) {
					Metronome.getInstance().setEnabled(enabledCheckbox.isSelected());
				}
			}
			
		});
		
		updateInterface();
		
		_swingComponent.add(tempoLabel);
		_swingComponent.add(bpmSlider);
		_swingComponent.add(startStopButton);
		_swingComponent.add(new JLabel("Beats per measure:"));
		_swingComponent.add(beatsPerMeasureCombo);
		_swingComponent.add(enabledCheckbox);

		setWidgetEdgeThickness(TrackWidgetCommons.STOPPED_TRACK_EDGE_THICKNESS);
		
	}
	
	private void updateInterface() {
		
		interfaceIsUpdating = true;
		
		try {
			
			tempoLabel.setText("Tempo: " + Metronome.getInstance().getTempo());
			bpmSlider.setValue(Math.min(Metronome.getInstance().getTempo(), MAX_BPM));
			
			if (Metronome.getInstance().isPlaying()) {
				startStopButton.setIcon(IconRepository.getIcon("metrostop.png"));
				setWidgetEdgeThickness(TrackWidgetCommons.PLAYING_TRACK_EDGE_THICKNESS);
	
			} else {
				startStopButton.setIcon(IconRepository.getIcon("metroplay.png"));
				setWidgetEdgeThickness(TrackWidgetCommons.STOPPED_TRACK_EDGE_THICKNESS);
			}
			
			int index = Math.min(beatsPerMeasureCombo.getItemCount(), Metronome.getInstance().getBeatsPerMeasure()) - 1;
			beatsPerMeasureCombo.setSelectedIndex(index);
			
			enabledCheckbox.setSelected(Metronome.getInstance().isEnabled());
			if (Metronome.getInstance().isEnabled()) {
				enabledCheckbox.setText("Auto-start on");
			} else {
				enabledCheckbox.setText("Auto-start off");
			}
			
		} finally {
			interfaceIsUpdating = false;
		}

	}
	
	@Override
	protected String[] getArgs() {
		return null;
	}

	public Subject getObservedSubject() {
		return Metronome.getInstance();
	}
	
	public void setObservedSubject(Subject parent) {
		// Ignore
	}

	public void modelChanged(Subject source, SubjectChangedEvent event) {
		
		if (source != Metronome.getInstance()) return;
		
		switch (event.getID()) {
		case ApolloSubjectChangedEvent.METRONOME_BEATS_PER_MEASURE_CHANGED:
		case ApolloSubjectChangedEvent.METRONOME_STARTED:
		case ApolloSubjectChangedEvent.METRONOME_STOPPED:
		case ApolloSubjectChangedEvent.METRONOME_TEMPO_CHANGED:
		case ApolloSubjectChangedEvent.METRONOME_ENABLED_CHANGED:
			updateInterface();
			break;
		}
		
	}

	@Override
	protected void onParentStateChanged(int eventType) {
		super.onParentStateChanged(eventType);
		
		// Be sure to remove self when removed from view .. so the widget does not hang
		// around in memory forever
		switch (eventType) {

		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
			Metronome.getInstance().removeObserver(this);
			break;

		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
			Metronome.getInstance().addObserver(this);
			break;

		}
	}
	
	@Override
	public boolean isWidgetEdgeThicknessAdjustable() {
		return false;
	}
	
}
