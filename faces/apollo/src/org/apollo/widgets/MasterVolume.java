package org.apollo.widgets;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apollo.audio.ApolloPlaybackMixer;
import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.gui.VolumeControlButton;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;

/**
 * Provides master volume control
 * 
 * @author Brook Novak
 *
 */
public class MasterVolume extends InteractiveWidget implements Observer {
	
	private VolumeControlButton vcButton;

	public MasterVolume(Text source, String[] args) {
		super(source, new VolumeControlButton("Master Volume"), 40, 40, 40, 40);
		vcButton = (VolumeControlButton)super._swingComponent;
		
		vcButton.addVolumeChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				float f = (float)vcButton.getVolumeValue();
				f /= 100.0f;
				ApolloPlaybackMixer.getInstance().setMasterVolume(f);
				if (f == 0.0f) { // auto-mute
					ApolloPlaybackMixer.getInstance().setMasterMute(true);
					// this will raise a mute event and thus will change the button to muted state
				} else { // auto-un-mute
					ApolloPlaybackMixer.getInstance().setMasterMute(false);
				}
			}
		});
		
		
		vcButton.setVolumeValue(
				(int)(ApolloPlaybackMixer.getInstance().getMasterVolume() * 100.0f)
				);
		vcButton.setMuted(ApolloPlaybackMixer.getInstance().isMasterMuteOn());

	}
	
	@Override
	protected String[] getArgs() {
		return null;
	}
	
	public Subject getObservedSubject() {
		return ApolloPlaybackMixer.getInstance();
	}

	
	@Override
	protected void onParentStateChanged(int eventType) {
		super.onParentStateChanged(eventType);
		
		switch (eventType) {

		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
			ApolloPlaybackMixer.getInstance().removeObserver(this);
			break;

		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
			ApolloPlaybackMixer.getInstance().addObserver(this);
			break;

		}
	}

	public void modelChanged(Subject source, SubjectChangedEvent event) {

		if (event.getID() == ApolloSubjectChangedEvent.VOLUME) {
			
			vcButton.setVolumeValue(
					(int)(ApolloPlaybackMixer.getInstance().getMasterVolume() * 100.0f)
					);
			
		} else if (event.getID() == ApolloSubjectChangedEvent.MUTE) {

			vcButton.setMuted(ApolloPlaybackMixer.getInstance().isMasterMuteOn());

		} 
		
	}

	public void setObservedSubject(Subject parent) {
	}

}
