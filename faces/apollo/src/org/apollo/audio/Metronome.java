package org.apollo.audio;

import java.util.Timer;
import java.util.TimerTask;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;

/**
 * A midi-based metronome for Apollo
 * @author Brook Novak
 *
 */
public class Metronome extends AbstractSubject {
	
	private Receiver receiver = null;
	
	private ShortMessage accentBeatOn; // Accents on new meause
	private ShortMessage accentBeatOff;
	
	private ShortMessage beatOn;
	private ShortMessage beatOff;
	
	// Model data
	private int tempoBPM = 100;
	private int beatsPerMeasure = 4; // i.e. upper time sig
	private boolean enabled = false; // purely used by outside

	// Internel data
	private int currentBeat; // wraps per measure
	private int usecDelayBetweenBeats;
	
	private Timer timer;
	
	private static final int PERCUSSION_CHANNEL = 9;
	private static final int ACOUSTIC_BASS = 35;
	//private static final int ACOUSTIC_SNARE = 38;
	//private static final int HAND_CLAP = 39;
	//private static final int PEDAL_HIHAT = 44;
	//private static final int LO_TOM = 45;
	private static final int CLOSED_HIHAT = 42;
	//private static final int CRASH_CYMBAL1 = 49;
	//private static final int HI_TOM = 50;
	//private static final int RIDE_BELL = 53;
	
	private static Metronome instance = new Metronome(); 	
	private Metronome() {
		
		try {
			beatOn = createNoteOnMsg(CLOSED_HIHAT, 90);
			beatOff = createNoteOffMsg(CLOSED_HIHAT);
			accentBeatOn = createNoteOnMsg(ACOUSTIC_BASS,127);
			accentBeatOff = createNoteOffMsg(ACOUSTIC_BASS);
			
		} catch (InvalidMidiDataException e1) {
			e1.printStackTrace();
		}
		
		RecordManager.getInstance().addObserver(new Observer() {

			public Subject getObservedSubject() {
				return null;
			}

			public void modelChanged(Subject source, SubjectChangedEvent event) {
				
				if (!isEnabled()) return;
				
				if (event.getID() == ApolloSubjectChangedEvent.CAPTURE_STARTED) {
					
					if (isPlaying()) 
						stop();
					
					try {
						start();
					} catch (MidiUnavailableException e) {
						e.printStackTrace();
					}

				} else if (event.getID() == ApolloSubjectChangedEvent.CAPTURE_STOPPED) {
					stop();
				}

			}

			public void setObservedSubject(Subject parent) {	
			}
			
		});
	}
	
	/**
	 * @return
	 * 		The singleton instance
	 */
	public static Metronome getInstance() {
		return instance;
	}
    
	private void intialize() throws MidiUnavailableException {
		if (receiver == null) {
			receiver = MidiSystem.getReceiver();
		}
	}
	
	/**
	 * In order to play sampled audio the midi receiver must be closed via this method.
	 */
	public void release() {
		stop();
		if (receiver != null) {
			receiver.close();
			receiver = null;
		}
	}

	 private ShortMessage createNoteOnMsg(int note, int velocity) throws InvalidMidiDataException {
		 ShortMessage msg = new ShortMessage();
		 msg.setMessage(ShortMessage.NOTE_ON, PERCUSSION_CHANNEL, note, velocity);
		 return msg;
	 }

	 private ShortMessage createNoteOffMsg(int note) throws InvalidMidiDataException {
		 ShortMessage msg = new ShortMessage();
		 msg.setMessage(ShortMessage.NOTE_OFF, PERCUSSION_CHANNEL, note, 0);
		 return msg;
	 }

	 /**
	  * Starts the metronome. If the metronome is already playing then nothing will result on the
	  * invocation of this method.
	  * 
	  * Fires a ApolloSubjectChangedEvent.METRONOME_STARTED event if started.
	  * 
	  * @throws MidiUnavailableException
	  * 	If failed to initialize MIDI receiver.
	  * 
	  * @throws InvalidMidiDataException
	  * 	If failed to initialize MIDI messages.
	  *  
	  */
	public void start() throws MidiUnavailableException {
		start(false);
	}
	
	/**
	 * @param isAdjusting
	 * 		True to suppress event and avoid reseting beat counter and delay the playback
	 * 		to give seemless-like playback while adjust tempo in realtime
	 */
	private void start(boolean isAdjusting) throws MidiUnavailableException {
		
		intialize();
		
		if (timer == null) {
			
			long preDelay;
			
			if (isAdjusting) {
				assert(usecDelayBetweenBeats > 0);
				preDelay = 100;
			} else {
				currentBeat = 0;
				preDelay = 0;
			}
			
			usecDelayBetweenBeats = 60000000 / tempoBPM;
			


			timer = new Timer("Metronome timer", false);
			timer.scheduleAtFixedRate(new MetronomeTask(), preDelay, usecDelayBetweenBeats / 1000);
			
			if(!isAdjusting) {
				fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.METRONOME_STARTED));
			}

		} 
	}
	
	/**
	 * Stops the metronome from playing. If not playing then nothing will result in this call.
	 * 
	 * Fires a ApolloSubjectChangedEvent.METRONOME_STOPPED event if stopped.
	 *
	 */
	public void stop() {
		stop(false);
	}
	
	private void stop(boolean supressEvent) {
		if (timer != null) {
			timer.cancel();
			timer = null;
			if (!supressEvent) {
				fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.METRONOME_STOPPED));
			}
		}
	}
	
	/**
	 * @return
	 * 		True if and only if the metronome is playing.
	 */
	public boolean isPlaying() {
		return timer != null;
	}
	
	/**
	 * Sets the beats-per-measure.
	 * 
	 * Fires a ApolloSubjectChangedEvent.METRONOME_BEATS_PER_MEASURE_CHANGED event.
	 * 
	 * @param bpm
	 * 		The amount of beats per measure. Must be larger than zero.
	 * 
	 * @throws IllegalArgumentException
	 * 		If bpm is smaller or equal to zero
	 */
	public void setBeatsPerMeasure(int bpm) {
		if (bpm <= 0) throw new IllegalArgumentException("bpm <= 0");
		if (this.beatsPerMeasure == bpm) return;
		this.beatsPerMeasure = bpm;
		
		fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.METRONOME_BEATS_PER_MEASURE_CHANGED));
	}
	
	/**
	 * @return
	 * 		The current beats-per-measure setting. Never smaller or equal to zero.
	 */
	public int getBeatsPerMeasure() {
		return beatsPerMeasure;
	}
	
	
	/**
	 * Sets the tempo. Adjusts the time in realtime if it is currently playing.
	 * 
	 * Fires a ApolloSubjectChangedEvent.METRONOME_TEMPO_CHANGED event.
	 * 
	 * @param bpm
	 * 		In beats per minute. Must be larger than zero.
	 * 
	 * @throws IllegalArgumentException
	 * 		If bpm is smaller or equal to zero
	 * 
	  * @throws MidiUnavailableException
	  * 	If failed to initialize MIDI receiver if it had to be re-initialized.
	  * 
	  * @throws InvalidMidiDataException
	  * 	If failed to initialize MIDI messages if they had to be re-initialized.
	 */
	public void setTempo(int bpm) throws MidiUnavailableException {
		if (bpm <= 0) throw new IllegalArgumentException("bpm <= 0");
		if (this.tempoBPM == bpm) return;
		this.tempoBPM = bpm;
		
		// When the tempo changes while the metronome is playing, must reset the 
		// timer
		if (timer != null) {
			stop(true);
			start(true);
		}
		
		fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.METRONOME_TEMPO_CHANGED));
	}
	/**
	 * 
	 * @return
	 * 		The current tempo setting in Beats Per Minute. Never smaller or equal to zero.
	 */
	public int getTempo() {
		return tempoBPM;
	}
	
	private class MetronomeTask extends TimerTask {

		public void run() {
			
			if (receiver == null) return;
			
			// Select beat tone
			ShortMessage selectedBeatOn;
			ShortMessage selectedBeatOff;
			
			if (currentBeat == 0) {
				selectedBeatOn = accentBeatOn;
				selectedBeatOff = accentBeatOff;
			} else {
				selectedBeatOn = beatOn;
				selectedBeatOff = beatOff;
			}
			
			currentBeat ++;
			if (currentBeat >= beatsPerMeasure) currentBeat = 0; // wrap per measure

			// Play selected beat
			receiver.send(selectedBeatOn, -1);
			receiver.send(selectedBeatOff, 1);

		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			fireSubjectChanged(new SubjectChangedEvent(ApolloSubjectChangedEvent.METRONOME_ENABLED_CHANGED));
		}
	}
	
	
	
	/**
	 * For debuf purposes
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			
			getInstance().start();
			
			
		} catch (MidiUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		

		try {
			Thread.sleep(3000);
			getInstance().setBeatsPerMeasure(2);
			Thread.sleep(3000);
			getInstance().setBeatsPerMeasure(3);
			getInstance().setTempo(140);
			Thread.sleep(4000);
			getInstance().stop();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MidiUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
