package org.apollo.audio;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.SubjectChangedEvent;

/**
 * Provides sampled recording services.
 * 
 * @author Brook Novak
 *
 */
public class RecordManager extends AbstractSubject  {

	private AudioCaptureThread captureThread = null;

	private static RecordManager instance = new RecordManager(); // single design pattern
	
	/**
	 * @return The singleton instance of the SampledAudioManager
	 */
	public static RecordManager getInstance() { // single design pattern
		return instance;
	}

	private RecordManager() { // singleton design pattern
	}
		
	/**
	 * Stops all captures... kills thread. Halts until killed.
	 */
	public void releaseResources() {
		
		// Quickly stop playback thread
		if (isCapturing())
			stopCapturing(); // halts
	}
	
	/**
	 * @return True if audio is currently being captured.
	 */
	public boolean isCapturing() {
		return (captureThread != null && captureThread.isAlive());
	}

	/**
	 * Stops capturing audio if any is being captured. Waits until capture proccess
	 * stops.
	 * 
	 * A CAPTURE_STOPPED event will be raised by invoking this method if there was
	 * audio being captured prior to invoking this method.
	 */
	public void stopCapturing() {
		// Tell the capture thread to stop
		if (captureThread != null) {
			captureThread.stopCapturing();
			captureThread = null;
		}
	}

	/**
	 * Begins capturing audio from the current input mixture.
	 * 
	 * If audio is currently being captured, that capture process is stopped
	 * (hence a CAPTURE_STOPPED event will be raised). 
	 * 
	 * A CAPTURE_STARTED event will be raised by invoking this method. Once the record thread has started
	 * 
	 * @param state 
	 * 				An object for optional state info - this is included in the audio event.
	 *              The intention being that observers can ID whether the start/stop events are
	 *              relevelent to them or not...
	 *              
	 * @return 
	 * 			A AudioCapturePipe which the recorded bytes will be written to. 
	 *              
	 * @throws LineUnavailableException
	 * 				If a line from the current input mixer is not available. 
	 * 				(could be used by another proccess/application)
	 * 
	 * @throws IOException
	 * 				If failed to create a pipe.
	 */
	public synchronized AudioCapturePipe captureAudio(Object state)
			throws LineUnavailableException, IOException {

		// Check to see if input mixer is available
		if (SampledAudioManager.getInstance().getCurrentInputMixure() == null) {
			throw new LineUnavailableException("No input mixer avialable");
		}
		
		if (SampledAudioManager.getInstance().getDefaultCaptureFormat() == null) {
			throw new LineUnavailableException("The current input mixer does not support audio capture in a valid format");
		}
		
		
		// Stop capturing any audio
		stopCapturing();

		// Create pipe
		PipedOutputStream pout = new PipedOutputStream();
		PipedInputStream pin = new PipedInputStream(pout);

		// Begin capturing
		assert (captureThread == null || !captureThread.isAlive());
		captureThread = new AudioCaptureThread(pout, state);
		captureThread.start();
		
		return new AudioCapturePipe(pin, captureThread.format, captureThread.bufferSize);
	}
	
	
	/**
	 * This inner class is used for asynchronously read data from a target data line
	 * to a stream; i.e. audio capture.
	 * 
	 * Once started, the audio from a TargetDataLine which is linked ot the input mixer
	 * until either stopCapturing is invoked or the output stream is closed.
	 * 
	 * Fires Capture start/stop events once the thread starts / finishes respectively
	 * 
	 * @author Brook Novak
	 */
	class AudioCaptureThread extends Thread {
		
		private boolean isCancelled = false;

		private TargetDataLine tdl;

		private Object state;
		
		private PipedOutputStream pout;
		
		private AudioFormat format;
		
		private int bufferSize;

		/**
		 * Constructor.
		 * The input mixer must not be null.
		 * 
		 * @param state 
		 * 				An object for optional state info - this is included in the audio event.
		 *              The intention being that observers can ID whether the start/stop events are
		 *              relevelent to them or not...
		 *              
		 * @param pout 
		 * 				The output stream to write to.
		 *              
		 * @throws LineUnavailableException
		 * 				If a line from the current mixer is not available. 
		 * 				(could be used by another proccess/application)
		 * 
		 */
		AudioCaptureThread(PipedOutputStream pout, Object state)
				throws LineUnavailableException {
			
			assert(pout != null);
			assert(SampledAudioManager.getInstance().getCurrentInputMixure() != null);
			assert(SampledAudioManager.getInstance().getDefaultCaptureFormat() != null);

			this.state = state;
			this.pout = pout;
			
			this.format = SampledAudioManager.getInstance().getDefaultCaptureFormat();

			// Select a target data line
			DataLine.Info dlInfo = new DataLine.Info(TargetDataLine.class, format);

			// The current input mixer should always support the defaultCaptureFormat
			assert(SampledAudioManager.getInstance().getCurrentInputMixure().isLineSupported(dlInfo));
			
			tdl = (TargetDataLine) SampledAudioManager.getInstance().getCurrentInputMixure().getLine(dlInfo);
			
			tdl.open(format); // Throws LineUnavailableException
			
			// Ensure a multiple of frame size
			bufferSize = tdl.getBufferSize();
			bufferSize -= (bufferSize % format.getFrameSize());
			if (bufferSize <= 0) bufferSize = format.getFrameSize();
		}

		/**
		 * Stops capturing the stream. Waits until thread stopped
		 * Can return without the thread actually stopping (yet) if the calling thread
		 * is interupted during the wait.
		 */
		public void stopCapturing() {
			isCancelled = true;
			if (isAlive()) {
				tdl.stop(); // blocks (wait time can vary depending on formats..some may take awhile!)
				tdl.drain();
				tdl.close();
				try {
					join();
				} catch (InterruptedException e) { /* Consume */
				}
			}
		}

		@Override
		public void run() {
			
			// Notify observers
			fireSubjectChangedLaterOnSwingThread(new SubjectChangedEvent(
					ApolloSubjectChangedEvent.CAPTURE_STARTED, state));
			
			byte buffer[] = new byte[bufferSize];
			
			try {
				// Start capturing
				tdl.start();
				
				// Dump bytes into pipe
				while (!isCancelled) {
					
				    int count = tdl.read(buffer, 0, buffer.length);
					
				    if (count > 0) {
				    	pout.write(buffer, 0, count);
				    }
				 }
				
			} catch (IOException e) { // Failed to write to pipe
				e.printStackTrace();
				
			} finally {
				
				// If thread execution stopped in a way other than the TDL closing ... ensure that
				// it is actually closed.
				if (tdl.isOpen()) {
					tdl.close();
				}
				
				// Close off pipe - will release reader from blocking and notify them that finished
				try {
					pout.close();
				} catch (IOException e) { /* Consume */ }
				
				if (Metronome.getInstance().isEnabled() &&
						Metronome.getInstance().isPlaying()) {
					
				}
				
				// Notify observers
				fireSubjectChangedLaterOnSwingThread(new SubjectChangedEvent(
						ApolloSubjectChangedEvent.CAPTURE_STOPPED, 
						state));
			}

		}

	}

}
