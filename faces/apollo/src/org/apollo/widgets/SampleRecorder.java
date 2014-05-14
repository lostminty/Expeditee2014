package org.apollo.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.AudioCapturePipe;
import org.apollo.audio.RecordManager;
import org.apollo.audio.SampledAudioManager;
import org.apollo.audio.util.MultiTrackPlaybackController;
import org.apollo.audio.util.SoundDesk;
import org.apollo.audio.util.Timeline;
import org.apollo.audio.util.MultiTrackPlaybackController.MultitrackLoadListener;
import org.apollo.gui.FrameLayoutDaemon;
import org.apollo.gui.PeakTroughWaveFormRenderer;
import org.apollo.gui.WaveFormRenderer;
import org.apollo.io.IconRepository;
import org.apollo.items.RecordOverdubLauncher;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.AudioMath;
import org.apollo.util.ApolloSystemLog;
import org.apollo.util.TrackNameCreator;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;

/**
 * Records sampled audio ... the cornerstone widget to Apollo.
 * 
 * @author Brook Novak
 *
 */
public class SampleRecorder extends InteractiveWidget 
	implements ActionListener, Observer, MultitrackLoadListener {
	
	private enum WidgetState {
		Ready, // Waiting to record
		CountingDown,
		LoadingPlayback,
		Recording, // ...
		Finalizing, // Capture stopped, reading last bytes from pipe
		Finished // Removed from expeditee world, spawned audio track widget in place of...
	}

	private Subject observeredSubject = null;
	
	private boolean shouldPlayback = false;
	private WidgetState state = null;
	private long recordStartTime = 0; // system time
	private RecordedByteReader audioByteReader = null;
	private CountDownTimer countdownThread = null;
	private String playbackFrameName;
	private long initiationTime = -1;
	
	private JButton recordButton;
	private JButton recordSynchedButton; // record while playing
	private JButton stopButton;
	private JSpinner countDownSpinner;
	private JLabel countDownSpinnerLabel;
	private JLabel statusLabel;
	
	private AnimatedSampleGraph sampleGraph;
	private boolean hasExplicityStopped = false;
	
	private boolean isTemporary = false;
	
	private final static int BUTTON_HEIGHT = 50;
	private final static int LABEL_HEIGHT = 30;
	private final static int COUNTDOWN_SETTINGS_HEIGHT = 30;
	private final static int HORO_SPACING = 2;
	private final static int VERT_SPACING = 0;
	
	private final static int MAX_COUNTDOWN_TIME = 60;
	
	private static final Color GRAPH_BACKCOLOR = Color.BLACK;
	private static final Color GRAPH_WAVECOLOR = Color.GREEN;
	
	private final static int RENDER_POINTS_PER_SECOND = 20; // how many points to render each second
	
	private final static String COUNTDOWN_META = "countdown=";
	
	public SampleRecorder(Text source, String[] args) {
		this(source, args, false);
	}
	
	public SampleRecorder(Text source, String[] args, boolean isTemporary) {
		super(source, new JPanel(new GridBagLayout()),
				AnimatedSampleGraph.GRAPH_WIDTH + (2 * HORO_SPACING), 
				AnimatedSampleGraph.GRAPH_WIDTH + (2 * HORO_SPACING),
				COUNTDOWN_SETTINGS_HEIGHT + BUTTON_HEIGHT + LABEL_HEIGHT + AnimatedSampleGraph.GRAPH_HEIGHT + (4 * VERT_SPACING), 
				COUNTDOWN_SETTINGS_HEIGHT + BUTTON_HEIGHT + LABEL_HEIGHT + AnimatedSampleGraph.GRAPH_HEIGHT + (4 * VERT_SPACING));
		
		this.isTemporary = isTemporary;
		
		int countdown = getStrippedDataInt(COUNTDOWN_META, 0);
		if (countdown < 0) countdown = 0;
		else if (countdown > MAX_COUNTDOWN_TIME)
			countdown = MAX_COUNTDOWN_TIME;

		// Create gui layout
		recordButton = new JButton();
		recordButton.setIcon(IconRepository.getIcon("record.png"));
		recordButton.addActionListener(this);
		recordButton.setPreferredSize(new Dimension(AnimatedSampleGraph.GRAPH_WIDTH / 2, BUTTON_HEIGHT));
		
		recordSynchedButton = new JButton();
		recordSynchedButton.setIcon(IconRepository.getIcon("recordplay.png"));
		recordSynchedButton.addActionListener(this);
		recordSynchedButton.setPreferredSize(new Dimension(AnimatedSampleGraph.GRAPH_WIDTH / 2, BUTTON_HEIGHT));

		stopButton = new JButton();
		stopButton.setIcon(IconRepository.getIcon("stop.png"));
		stopButton.addActionListener(this);
		stopButton.setPreferredSize(new Dimension(AnimatedSampleGraph.GRAPH_WIDTH, BUTTON_HEIGHT));
		stopButton.setVisible(false);

		sampleGraph = new AnimatedSampleGraph();
		
		statusLabel = new JLabel();
		statusLabel.setPreferredSize(new Dimension(AnimatedSampleGraph.GRAPH_WIDTH, LABEL_HEIGHT));
		statusLabel.setHorizontalAlignment(JLabel.CENTER);
		statusLabel.setVerticalAlignment(JLabel.CENTER);
		
		SpinnerModel model =
	        new SpinnerNumberModel(0, 0, MAX_COUNTDOWN_TIME, 1);
		countDownSpinner = new JSpinner(model);
		countDownSpinner.setPreferredSize(new Dimension(50, COUNTDOWN_SETTINGS_HEIGHT));
		countDownSpinner.setValue(countdown);

		countDownSpinnerLabel = new JLabel("Count down:");
		countDownSpinnerLabel.setPreferredSize(new Dimension(AnimatedSampleGraph.GRAPH_WIDTH - 50, COUNTDOWN_SETTINGS_HEIGHT));

		// Layout GUI
		GridBagConstraints c;
		
		JPanel countdownPane = new JPanel(new GridBagLayout());
		countdownPane.setPreferredSize(new Dimension(AnimatedSampleGraph.GRAPH_WIDTH, COUNTDOWN_SETTINGS_HEIGHT));
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0f;
		c.fill = GridBagConstraints.BOTH;
		countdownPane.add(countDownSpinnerLabel, c);
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		countdownPane.add(countDownSpinner, c);
		
		JPanel buttonPane = new JPanel(new GridBagLayout());
		buttonPane.setPreferredSize(new Dimension(AnimatedSampleGraph.GRAPH_WIDTH, BUTTON_HEIGHT));

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		buttonPane.add(recordButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		buttonPane.add(recordSynchedButton, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		buttonPane.add(stopButton, c);

		// Assemble
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		_swingComponent.add(buttonPane, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		_swingComponent.add(countdownPane, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.BOTH;
		_swingComponent.add(sampleGraph, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 3;
		c.fill = GridBagConstraints.BOTH;
		_swingComponent.add(statusLabel, c);
		
		_swingComponent.doLayout();

		setState(WidgetState.Ready, "Ready");
	}
	
	/**
	 * Starts recording with playback... counts down if one is set
	 *
	 */
	public void commenceOverdubRecording() {
		shouldPlayback = true;
		setState(WidgetState.CountingDown, "Counting down...");
	}
	
	/**
	 * 
	 * @param countdown
	 * 		The countdown in seconds
	 * 
	 * @throws IllegalArgumentException 
	 * 			if countdown isn't allowed (due to restraints, i.e. can't be negative)
	 */
	public void setCountdown(int countdown) {
		this.countDownSpinner.setValue(countdown);
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
		
		sampleGraph.alternateText = null;
		sampleGraph.invalidateGraph();
		
		statusLabel.setText(status);

		if (newState == WidgetState.Ready) {

			// Self destructable recorders are only temporary.. is returned back to a ready state then
			// get rid of them completely
			if (oldState != null && isTemporary) {
				removeSelf();
			}
			
			recordButton.setVisible(true);
			recordSynchedButton.setVisible(true);
			stopButton.setVisible(false);
			
			if (oldState != null) sampleGraph.clear();
			
		} else if (newState == WidgetState.CountingDown) {
			assert(oldState == WidgetState.Ready);
			
			assert (countdownThread == null || !countdownThread.isAlive());
			
			int countDown = (Integer)countDownSpinner.getValue();
			
			if (countDown == 0) {
				commenceRecording(shouldPlayback);
			} else {

				recordButton.setVisible(false);
				recordSynchedButton.setVisible(false);
				stopButton.setVisible(true);
				stopButton.setEnabled(true);
	
				sampleGraph.alternateText = Integer.toString(countDown);
				sampleGraph.alternateTextColor = (countDown > 3) ? Color.WHITE : Color.RED;
				sampleGraph.invalidateGraph();
				FrameGraphics.refresh(true);
				
				countdownThread = new CountDownTimer(countDown);
				countdownThread.start();
			}
			
		} else if (newState == WidgetState.LoadingPlayback) {
			assert(oldState == WidgetState.Ready || oldState == WidgetState.CountingDown);
			
			recordButton.setVisible(false);
			recordSynchedButton.setVisible(false);
			stopButton.setVisible(true);
			stopButton.setEnabled(false);
			
			// TODO: Cancel load on users demand.

			
		} else if (newState == WidgetState.Recording) {
			assert(oldState == WidgetState.Ready || 
					oldState == WidgetState.CountingDown || 
					oldState == WidgetState.LoadingPlayback);
			
			hasExplicityStopped = false;
			
			recordButton.setVisible(false);
			recordSynchedButton.setVisible(false);
			stopButton.setVisible(true);
			stopButton.setEnabled(true);

			setStatusLabelToRecordTime();
			
		} else if (newState == WidgetState.Finalizing) { // wait for pipe to finish reading bytes
			
			// This state can be skipped if pipe has finished before
			// stop event captured.
			
			recordButton.setVisible(false);
			recordSynchedButton.setVisible(false);
			stopButton.setVisible(true);
			stopButton.setEnabled(false);
			
			// Thread reading from pipe will finish and trigger the finished
			
		} else if (newState == WidgetState.Finished) {
			
			// The widget could have been removed while recording or finializing.
			// In such cases then do not do anything as the user has suggested
			// that the want away with it all...
			if (!hasExplicityStopped) {
				
				// Reset the state
				setState(WidgetState.Ready, "Ready"); 
				
			} else {
				
				if (isTemporary) {
					// Remove this temporary widget
					removeSelf();
				}
			
				// Spawn an audio track using the actual bytes and audio format buffered from
				// the pipe. This will load instantly, saving of bytes is its responsibility.
				 if (audioByteReader != null && audioByteReader.bufferedAudioBytes != null
						 && audioByteReader.bufferedAudioBytes.size() > 0) {
					 
					 // Get frame to anchor track to
					 Frame targetFrame = getParentFrame();
					 if (targetFrame == null) {
						 targetFrame = DisplayIO.getCurrentFrame();
					 }
					 
					 assert(targetFrame != null);
					 
					 if (!shouldPlayback) initiationTime = -1;
					 
					 SampledTrack trackWidget = SampledTrack.createFromMemory(
							 audioByteReader.bufferedAudioBytes.toByteArray(), 
							 audioByteReader.audioFormat, 
							 targetFrame, 
							 getX(), 
							 getY(),
							 TrackNameCreator.getNameCopy(targetFrame.getTitle() + "_"),
							 null);
					 
					 if (isTemporary) {
						 
						 targetFrame.addAllItems(trackWidget.getItems());
						 
						 // Must be exact
						 trackWidget.setInitiationTime(initiationTime);

					 } else {
						 
						 FrameMouseActions.pickup(trackWidget.getItems());
							
						// Reset the state
						setState(WidgetState.Ready, "Ready"); 
						
					 }
	
					 
				 }
			}
			
		}
		
		// Ensure that if not recording or loading playback then stop/cancel multiplaybackl controller events
		// that are commenced via this widget.
		if (
				newState != WidgetState.LoadingPlayback && newState != WidgetState.Recording
				&& (oldState == WidgetState.LoadingPlayback || oldState == WidgetState.Recording)
				&& playbackFrameName != null && shouldPlayback
				&& MultiTrackPlaybackController.getInstance().isCurrentPlaybackSubject(
						playbackFrameName, 
						FramePlayer.FRAME_PLAYERMASTER_CHANNEL_ID)) {
			
			MultiTrackPlaybackController.getInstance().cancelLoad(
				playbackFrameName, 
				FramePlayer.FRAME_PLAYERMASTER_CHANNEL_ID);
			
			MultiTrackPlaybackController.getInstance().stopPlayback();
		}

	}
	
	private void setStatusLabelToRecordTime() {
		long elapsed = (System.currentTimeMillis() - recordStartTime);
		elapsed /= 1000;
		//statusLabel.setText("Recorded " + elapsed + " seconds");
		statusLabel.setText(elapsed + " seconds");
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
	protected List<String> getData() {

		List<String> data = new LinkedList<String>();
		
		data.add(COUNTDOWN_META + countDownSpinner.getValue());

		return data;
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
				
			} else if (state == WidgetState.CountingDown) {
				countdownThread.abortCountdown();
				
			} else if (state == WidgetState.LoadingPlayback && playbackFrameName != null) {
				MultiTrackPlaybackController.getInstance().cancelLoad(
						playbackFrameName, 
						FramePlayer.FRAME_PLAYERMASTER_CHANNEL_ID);
			}
			
			// Ensure that this can be disposed
			RecordManager.getInstance().removeObserver(this);
			MultiTrackPlaybackController.getInstance().removeObserver(this);
			break;

		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
			RecordManager.getInstance().addObserver(this);
			MultiTrackPlaybackController.getInstance().addObserver(this);
			break;
			
		}

	}

	public Subject getObservedSubject() {
		return observeredSubject;
	}

	public void setObservedSubject(Subject parent) {
		observeredSubject = parent;
	}

	public void modelChanged(Subject source, SubjectChangedEvent event) { // subject messages

		switch (event.getID()) {
		
		case ApolloSubjectChangedEvent.CAPTURE_STARTED:
			if (event.getState() == this) {
				// Change to recording state
				recordStartTime = System.currentTimeMillis();
				setState(WidgetState.Recording, "Record started");
			}
			break;

			
		case ApolloSubjectChangedEvent.CAPTURE_STOPPED:
			
			if (state == WidgetState.Recording) {
				
			//	assert (event.getState() == this);
				setState(WidgetState.Finalizing, "finalizing");
			}
			break;
			
		case ApolloSubjectChangedEvent.PLAYBACK_STARTED: // from multi playback controller

			if (state == WidgetState.LoadingPlayback && playbackFrameName != null &&
					MultiTrackPlaybackController.getInstance().isCurrentPlaybackSubject(
					playbackFrameName, FramePlayer.FRAME_PLAYERMASTER_CHANNEL_ID)) {
				commenceRecording(false);
			}
			break;

		}
		
	}


	public void multiplaybackLoadStatusUpdate(int id, Object state) {
	
		if (state != WidgetState.LoadingPlayback) return;
		String abortMessage = null;
		
		switch(id) {
		case MultitrackLoadListener.LOAD_CANCELLED:
			abortMessage = "Playback/Record cancelled"; 
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
			setState(WidgetState.Ready, abortMessage);
		}

	}

	public void actionPerformed(ActionEvent e) { // For action button
		
		
		if (e.getSource() == stopButton) {
			assert (state == WidgetState.Recording || state == WidgetState.CountingDown);
			
			if (state == WidgetState.CountingDown) {
				assert (countdownThread != null);
				countdownThread.abortCountdown();
			} else {
				hasExplicityStopped = true;
				RecordManager.getInstance().stopCapturing();
			}
			
		} else if (e.getSource() == recordButton) {
			assert (state == WidgetState.Ready);
			shouldPlayback = false;
			setState(WidgetState.CountingDown, "Counting down...");
		} else if (e.getSource() == recordSynchedButton) {
			assert (state == WidgetState.Ready);
			
			Frame target = DisplayIO.getCurrentFrame();
			if (target  == null) return;

			// Create the launcher
			
			RecordOverdubLauncher launcher = new RecordOverdubLauncher((Integer)countDownSpinner.getValue());
			launcher.setPosition(FrameMouseActions.MouseX, FrameMouseActions.MouseY);

			// Pick it up
			FrameMouseActions.pickup(launcher);
			
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
		private int aggregationSize;
		public ByteArrayOutputStream bufferedAudioBytes = null; // not null if started
		private DoUpdateLabel updateLabelTask = new DoUpdateLabel();
		private WaveFormRenderer waveFormRenderer = null;
		
		RecordedByteReader(AudioCapturePipe pipe)  {
			assert(pipe != null);
			this.pin = pipe.getPin();
			this.audioFormat = pipe.getAudioFormat();
			this.bufferSize = pipe.getBufferSize();
			waveFormRenderer = new PeakTroughWaveFormRenderer(audioFormat);
			
			// Aggregate size depends on samplerate
			aggregationSize = (int)(audioFormat.getFrameRate() / RENDER_POINTS_PER_SECOND);
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
						
						// Render bytes
						float[] waveforms = waveFormRenderer.getSampleAmplitudes(
								buffer, 
								0, 
								bytesBuffered / audioFormat.getFrameSize(), 
								aggregationSize);
							
						// Send renderings to graph for drawing
						SwingUtilities.invokeLater(new DoRenderGraph(waveforms));

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
				SwingUtilities.invokeLater(new DoFinished());
				
			}

		}
	}
	
	class DoFinished implements Runnable {
		public void run() {
			setState(WidgetState.Finished, "Finished");
		}
	}

	class DoUpdateLabel implements Runnable {
		public void run() {
			setStatusLabelToRecordTime();
		}
	}
	
	class DoRenderGraph implements Runnable {
		private float[] waveForms;
		public DoRenderGraph(float[] waveForms) {
			this.waveForms = waveForms;
		}
		public void run() {
			assert (audioByteReader != null);
			sampleGraph.updateVisualization(waveForms);
		}
	}
	
	/**
	 * The state will change eventually (or instantly is failed)
	 * 
	 * @param withPlayback
	 * 		True to commence frame and enter in a loading state.
	 */
	private void commenceRecording(boolean withPlayback) {
		
		if (withPlayback) {
			
			assert (state != WidgetState.LoadingPlayback);
			assert (state != WidgetState.Recording);
			
			setState(WidgetState.LoadingPlayback, "Loading tracks...");
			
			Frame currentFrame = DisplayIO.getCurrentFrame();
			if (currentFrame == null || currentFrame.getName() == null) {
				
				setState(WidgetState.Ready, "No frame to play");
				return;
			}
			
			playbackFrameName = currentFrame.getName();
			

			Timeline tl = FrameLayoutDaemon.getInstance().getLastComputedTimeline();
			
			if (tl == null || FrameLayoutDaemon.getInstance().getTimelineOwner() == null ||
					FrameLayoutDaemon.getInstance().getTimelineOwner() != currentFrame) {
				tl = FrameLayoutDaemon.inferTimeline(currentFrame);
			}
			
			if (tl != null) {
		
				initiationTime = tl.getMSTimeAtX(getX());
				
				// Clamp
				if (initiationTime < tl.getFirstInitiationTime())
					initiationTime = tl.getFirstInitiationTime();
				
				else if (initiationTime > (tl.getFirstInitiationTime() + tl.getRunningTime())) 
					initiationTime  = tl.getFirstInitiationTime() + tl.getRunningTime();

				int startFrame = AudioMath.millisecondsToFrames(
						initiationTime - tl.getFirstInitiationTime(),  
						SampledAudioManager.getInstance().getDefaultPlaybackFormat());
				
				// To ensure that the frame channels are not in use
				MultiTrackPlaybackController.getInstance().stopPlayback();
				SoundDesk.getInstance().freeChannels(FramePlayer.FRAME_PLAYERMASTER_CHANNEL_ID);

				FramePlayer.playFrame(
					this, 
					currentFrame.getName(), 
					false, 
					startFrame, 
					Integer.MAX_VALUE);
				
			} else {
				commenceRecording(false); // without playback
				return;
			}
			
		} else {
	
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
		}
	}
	
	/**
	 * Renders a waveform to a panel at realtime
	 */
	private class AnimatedSampleGraph extends JPanel {
		
		protected static final long serialVersionUID = 0L;
		
		static final int GRAPH_WIDTH = 140;
		static final int GRAPH_HEIGHT = 40;
		static final int HALF_GRAPH_HEIGHT = GRAPH_HEIGHT / 2;
		static final int SAMPLE_PIXEL_SPACING = 1; 

		private BufferedImage imageBuffer;
		private Graphics2D imageGraphics;
		private int lastSampleHeight = HALF_GRAPH_HEIGHT;
		
		private String alternateText = null;
		private Color alternateTextColor = Color.WHITE;

		AnimatedSampleGraph() {
			imageBuffer = new BufferedImage(GRAPH_WIDTH, GRAPH_HEIGHT, BufferedImage.TYPE_INT_RGB);
			imageGraphics = (Graphics2D)imageBuffer.getGraphics();
			imageGraphics.setColor(GRAPH_BACKCOLOR);
			imageGraphics.fillRect(0, 0, GRAPH_WIDTH, GRAPH_HEIGHT);
			setPreferredSize(new Dimension(GRAPH_WIDTH, GRAPH_HEIGHT));
		}
		
		/**
		 * Clears the graph and invlaidates itself
		 */
		public void clear() {
			imageGraphics.setColor(GRAPH_BACKCOLOR);
			imageGraphics.fillRect(0, 0, GRAPH_WIDTH, GRAPH_HEIGHT);
			invalidateGraph();
		}

		@Override
		public void paint(Graphics g) {
			
			if (alternateText != null) {
				
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, getWidth(), getHeight());
				
				g.setFont(TrackWidgetCommons.FREESPACE_TRACKNAME_FONT);
				g.setColor(alternateTextColor);
				
				// Center track name
				FontMetrics fm   = g.getFontMetrics(TrackWidgetCommons.FREESPACE_TRACKNAME_FONT);
				Rectangle2D rect = fm.getStringBounds(alternateText, g);
				
				g.drawString(
						alternateText, 
						(int)((getWidth() - rect.getWidth()) / 2), 
						(int)((getHeight() - rect.getHeight()) / 2) + (int)rect.getHeight()
						);
				
			} else {
				g.drawImage(imageBuffer, 0, 0, null);
			}
		}
		
		/**
		 * Renders the audio bytes to the graph
		 * @param audioBytes
		 */
		public void updateVisualization(float[] waveForms) {

			if (waveForms == null || waveForms.length == 0) return;
			
			int pixelWidth = waveForms.length * SAMPLE_PIXEL_SPACING;
		
			// Translate buffer back pixelWidth pixels
			AffineTransform transform = new AffineTransform();
			transform.translate(-pixelWidth, 0.0);
			//transform.translate(pixelWidth, 0.0);
			//AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
			//imageBuffer = op.filter(imageBuffer, null);
			imageGraphics.drawImage(imageBuffer, transform, this);
			
			// Render backcolor
			imageGraphics.setColor(GRAPH_BACKCOLOR);
			imageGraphics.fillRect(GRAPH_WIDTH - pixelWidth, 0, pixelWidth, GRAPH_HEIGHT);
			
			// Render wave forms from lastSampleHeight
			imageGraphics.setColor(GRAPH_WAVECOLOR);
			int currentPixelX = GRAPH_WIDTH - pixelWidth;

			for (int i = 0; i < waveForms.length; i++) {
				
				int currentHeight = HALF_GRAPH_HEIGHT + (int)(waveForms[i] * HALF_GRAPH_HEIGHT);
				
				// Draw a line
				imageGraphics.drawLine(currentPixelX - 1, lastSampleHeight, currentPixelX, currentHeight);
				
				currentPixelX += SAMPLE_PIXEL_SPACING;
				lastSampleHeight = currentHeight;
			}
			
			invalidateGraph();
		}
		
		private void invalidateGraph() {

	        // For fastest refreshing - invalidate directy via expedtiee
	        Rectangle dirty = this.getBounds();
	        dirty.translate(SampleRecorder.this.getX(), SampleRecorder.this.getY());
	        FrameGraphics.invalidateArea(dirty); 
	        FrameGraphics.refresh(true); 
	        
		}
		
	}
		
	/**
	 * Updates the timer / widget state.
	 * 
	 * @author Brook Novak
	 *
	 */
	private class CountDownTimer extends Thread {
		
		private int currentCountdown;
		private boolean returnToReadyState = false;
		
		public CountDownTimer(int countdownSecs) {
			assert(countdownSecs > 0);
			currentCountdown = countdownSecs;
		}
		
		public void abortCountdown() {
			interrupt();
		}
		
		public void run() {

			try {
				
				while (currentCountdown > 0) {
					
					if (interrupted()) { 
						returnToReadyState = true;
						break;
					}
					
					sleep(1000);
					currentCountdown--;
					
					// Update graph
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							sampleGraph.alternateText = Integer.toString(currentCountdown);
							sampleGraph.alternateTextColor = (currentCountdown > 3) ?
										Color.WHITE : Color.RED;
							sampleGraph.invalidateGraph();
							FrameGraphics.refresh(true);
						}
					});
					
				}
				
			} catch (InterruptedException e) {
				returnToReadyState = true;
			}
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (returnToReadyState) {
						setState(WidgetState.Ready, "Ready");
					} else {
						commenceRecording(shouldPlayback);
					}
				}
			});

		}
	}
	
}
