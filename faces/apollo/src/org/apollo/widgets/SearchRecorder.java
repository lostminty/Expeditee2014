package org.apollo.widgets;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apollo.agents.MelodySearch;
import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.AudioCapturePipe;
import org.apollo.audio.RecordManager;
import org.apollo.audio.SampledAudioManager;
import org.apollo.audio.util.MultiTrackPlaybackController;
import org.apollo.io.IconRepository;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.ApolloSystemLog;
import org.expeditee.actions.Actions;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;

public class SearchRecorder extends InteractiveWidget 
	implements ActionListener, Observer {
	
	private enum WidgetState {
		Ready, // Waiting to record a querry
		Recording, // ...
		Finalizing, // Capture stopped, reading last bytes from pipe
	}
	
	private WidgetState state = null;
	private RecordedByteReader audioByteReader = null;
	private long recordStartTime = 0; // system time
	
	private JButton recordButton;
	private JButton commitButton; // stop recording and commence search
	private JButton cancelButton;
	JLabel statusLabel;

	private boolean hasExplicityStopped = false;
	
	private final static int BUTTON_SIZE = 50;
	private final static int LABEL_HEIGHT = 30;
	
	public SearchRecorder(Text source, String[] args) {
		super(source, new JPanel(new BorderLayout()),
				BUTTON_SIZE * 2, 
				BUTTON_SIZE * 2,
				BUTTON_SIZE + LABEL_HEIGHT, 
				BUTTON_SIZE + LABEL_HEIGHT);
		
		// Create gui layout
		recordButton = new JButton();
		recordButton.setIcon(IconRepository.getIcon("searchmel.png"));
		recordButton.setToolTipText("Search for audio from live recording");
		recordButton.addActionListener(this);
		recordButton.setPreferredSize(new Dimension(BUTTON_SIZE * 2, BUTTON_SIZE));

		commitButton = new JButton();
		commitButton.setIcon(IconRepository.getIcon("commitquery.png"));
		commitButton.addActionListener(this);
		commitButton.setToolTipText("GO!");
		commitButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		commitButton.setVisible(false);
		
		cancelButton = new JButton();
		cancelButton.setToolTipText("Cancel");
		cancelButton.setIcon(IconRepository.getIcon("cancel.png"));
		cancelButton.addActionListener(this);
		cancelButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

		
		statusLabel = new JLabel();
		statusLabel.setPreferredSize(new Dimension(BUTTON_SIZE * 2, LABEL_HEIGHT));
		statusLabel.setHorizontalAlignment(JLabel.CENTER);
		statusLabel.setVerticalAlignment(JLabel.CENTER);
		
		// Layout GUI
		GridBagConstraints c;
		
		JPanel buttonPane = new JPanel(new GridBagLayout());
		buttonPane.setPreferredSize(new Dimension(BUTTON_SIZE * 2, BUTTON_SIZE));
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		buttonPane.add(recordButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		buttonPane.add(commitButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		buttonPane.add(cancelButton, c);
		
		// Assemble
		_swingComponent.add(buttonPane, BorderLayout.CENTER);
		_swingComponent.add(statusLabel, BorderLayout.SOUTH);
		
		_swingComponent.doLayout();

		setState(WidgetState.Ready, "Record Query");
	}
	
	/**
	 * All of the widget logic is centarlized here - according to the new state transition
	 * Must be in AWT thread
	 * 
	 * @param newState
	 */
	private void setState(WidgetState newState, String status) {
		if (state == newState) return;
		WidgetState oldState = state;
		state = newState;
		
		statusLabel.setText(status);

		if (newState == WidgetState.Ready) {

			recordButton.setVisible(true);
			cancelButton.setVisible(false);
			commitButton.setVisible(false);
			
			// Ensure that this is observing the record model
			SampledAudioManager.getInstance().addObserver(this);
			
		} else if (newState == WidgetState.Recording) {
			assert(oldState == WidgetState.Ready);
			
			hasExplicityStopped = false;
			
			recordButton.setVisible(false);
			cancelButton.setVisible(true);
			commitButton.setVisible(true);
			cancelButton.setEnabled(true);
			commitButton.setEnabled(true);

			setStatusLabelToRecordTime();
			
		} else if (newState == WidgetState.Finalizing) { // wait for pipe to finish reading bytes
			
			// This state can be skipped if pipe has finished before
			// stop event captured.
			recordButton.setVisible(false);
			cancelButton.setVisible(true);
			commitButton.setVisible(true);
			cancelButton.setEnabled(false);
			commitButton.setEnabled(false);
			
			// Thread reading from pipe will finish and trigger the finished
			
		} else assert(false);

	}
	
	private void setStatusLabelToRecordTime() {
		long elapsed = (System.currentTimeMillis() - recordStartTime);
		elapsed /= 1000;
		//statusLabel.setText("Recorded " + elapsed + " seconds");
		statusLabel.setText("query: " + elapsed + " secs");
	}

	/**
	 * {@inheritDoc} 
	 * SampleRecorderWidget is really stateless - they are means to capture audio only...
	 * That is, temporary.
	 */
	@Override
	protected String[] getArgs() {
		return null;
	}
	
	@Override
	protected void onParentStateChanged(int eventType) {
		super.onParentStateChanged(eventType);

		switch (eventType) {
		case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY:

			if (state == WidgetState.Recording) {
				// This will change the state of this widget
				RecordManager.getInstance().stopCapturing();
			}
			
			// Ensure that this can be disposed
			RecordManager.getInstance().removeObserver(this);
			SampledAudioManager.getInstance().removeObserver(this);
			break;

		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
			RecordManager.getInstance().addObserver(this);
			MultiTrackPlaybackController.getInstance().addObserver(this);
			SampledAudioManager.getInstance().addObserver(this);
			break;
			
		}

	}

	public Subject getObservedSubject() {
		return null;
	}

	public void setObservedSubject(Subject parent) {
	}

	public void modelChanged(Subject source, SubjectChangedEvent event) { // subject messages

		switch (event.getID()) {
		
		case ApolloSubjectChangedEvent.CAPTURE_STARTED:
			if (event.getState() == this) {
				// Change to recording state
				recordStartTime = System.currentTimeMillis();
				setState(WidgetState.Recording, "Recording query");
			}
			break;

			
		case ApolloSubjectChangedEvent.CAPTURE_STOPPED:
			
			if (state == WidgetState.Recording) {
				setState(WidgetState.Finalizing, "finalizing");
			}
			break;
			

		}
		
	}

	public void actionPerformed(ActionEvent e) { // For action button
		
		
		if (e.getSource() == commitButton) {
			assert (state == WidgetState.Recording);
			hasExplicityStopped = true;
			RecordManager.getInstance().stopCapturing();

		} else if (e.getSource() == recordButton) {
			assert (state == WidgetState.Ready);

			try {
				
				// Start capturing
				AudioCapturePipe pipe = RecordManager.getInstance().captureAudio(this);
				
				// Read bytes asynchronously ... buffer and render visual feedback
				audioByteReader = new RecordedByteReader(pipe);
				audioByteReader.start();
				
			} catch (LineUnavailableException ex) {
				ApolloSystemLog.printException("Failed to commence audio capture via record widget", ex);
				setState(WidgetState.Ready, "Bad Device");
			} catch (IOException ex) {
				ApolloSystemLog.printException("Failed to commence audio capture via record widget", ex);
				setState(WidgetState.Ready, "Failed");
			}
			
		} else if (e.getSource() == cancelButton) {
			assert (state == WidgetState.Recording);
			hasExplicityStopped = false; // not needed but to make clear
			RecordManager.getInstance().stopCapturing();
		}


	}
	
	/**
	 * Reads bytes asynchronously from a given pipe until it is finished or an exception occurs.
	 * Once finished excecution, the widget state will be set to finished.
	 * 
	 * The bytes read are buffered, rendered and sent to the AnimatedSampleGraph for drawing.
	 * The status label is also updated according to the record time. 
	 * 
	 * @author Brook Novak
	 *
	 */
	private class RecordedByteReader extends Thread {
		
		private PipedInputStream pin;
		private AudioFormat audioFormat;
		private int bufferSize;
		public ByteArrayOutputStream bufferedAudioBytes = null; // not null if started
		private DoUpdateLabel updateLabelTask = new DoUpdateLabel();;
		
		RecordedByteReader(AudioCapturePipe pipe)  {
			assert(pipe != null);
			this.pin = pipe.getPin();
			this.audioFormat = pipe.getAudioFormat();
			this.bufferSize = pipe.getBufferSize();
		}

		@Override
		public void run() {

			bufferedAudioBytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[bufferSize];
			int len = 0;
			int bytesBuffered;
			
			int lastUpdateStateTime = 0;

			try {
				while(true) {
					
					bytesBuffered = 0;
					
					while (bytesBuffered < buffer.length) { // Full the buffer (unless reaches uneven end)
						len = pin.read(buffer, bytesBuffered, buffer.length - bytesBuffered);
						if (len == -1) break; // done
						bytesBuffered += len; 
					}

					if (bytesBuffered > 0) {
						// Buffer bytes
						bufferedAudioBytes.write(buffer, 0, bytesBuffered);
					}
					
					if (len == -1) break; // done
					
					// For every second elapsed, update status
					if ((System.currentTimeMillis() - lastUpdateStateTime) >= 1000) {
						SwingUtilities.invokeLater(updateLabelTask);
					}
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				
				try {
					pin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					bufferedAudioBytes.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// Ensure that will enter finalized state
				SwingUtilities.invokeLater(new RecoringFinalizer());
				
			}

		}
	}
	
	class RecoringFinalizer implements Runnable {
		
		public void run() {
			
			if (hasExplicityStopped) {
				
				Frame sourceFrame = DisplayIO.getCurrentFrame();
				
				 if (sourceFrame != null &&
						 audioByteReader != null && audioByteReader.bufferedAudioBytes != null
						 && audioByteReader.bufferedAudioBytes.size() > 0) {
					 
					// Create melody search agent - setting raw audio for query data
					MelodySearch melSearchAgent = new MelodySearch();
					melSearchAgent.useRawAudio(
							audioByteReader.bufferedAudioBytes.toByteArray(), 
							 audioByteReader.audioFormat);
					
					Text launcherItem = new Text(sourceFrame.getNextItemID(), "Audio Query");

					// Run the melody search agent using recorded audio
					Actions.LaunchAgent(
							melSearchAgent, 
							sourceFrame, 
							launcherItem);
				}
				
				
			}
	
			setState(WidgetState.Ready, "Record Query");
		}
	}

	class DoUpdateLabel implements Runnable {
		public void run() {
			setStatusLabelToRecordTime();
		}
	}

}
