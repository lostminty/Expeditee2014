package org.apollo.gui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import javax.sound.sampled.AudioFormat;

import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.SubjectChangedEvent;

public class WaveFormRenderProccessingUnit {
	
	/** Limit the amount of render threads. */
	private RenderThread[] renderThreads = new RenderThread[4];
	
	private LinkedList<WaveFormRenderTask> taskQueue = new LinkedList<WaveFormRenderTask>();

	private static WaveFormRenderProccessingUnit instance = new WaveFormRenderProccessingUnit(); // single design pattern
	
	/**
	 * @return The singleton instance of the WaveFormRenderProccessingUnit
	 */
	public static WaveFormRenderProccessingUnit getInstance() { // single design pattern
		return instance;
	}

	private WaveFormRenderProccessingUnit() { // singleton design pattern
	}

	/**
	 * Queues a task for rendering as soon as possible (asynchronously).
	 * 
	 * @param task
	 * 		The task to queue
	 * 
	 * @throws NullPointerException
	 * 		If task is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If task has been proccessed before (must not be re-used).
	 * 		Or if it is already queued for rendering.
	 */
	public void queueTask(WaveFormRenderTask task) {
		if (task == null) throw new NullPointerException("task");
		
		// Ensure not re-using a WaveFormRenderTask
		if (task.hasStarted()) throw new IllegalArgumentException("task");

		// Add to the queue
		synchronized (taskQueue) {
			
			// Check that not already on the queue
			if (taskQueue.contains(task)) {
				throw new IllegalArgumentException("task");
			}
			
			taskQueue.add(task);
		
		
			// If there is are dead thread, re-animate it to ensure
			// the task begins as soon as possible.
			for (int i = 0; i < renderThreads.length; i++) {
				
				if (renderThreads[i] == null
						||  renderThreads[i].isFinishing()
						|| !renderThreads[i].isAlive()) {
					
					renderThreads[i] = new RenderThread(i);
					renderThreads[i].start();
					break;
				}
				
			}
		
		}

		
	}
	
	/**
	 * Cancels a task from rendering.
	 * 
	 * @param task
	 * 		The task to cancel.
	 * 
	 * @throws NullPointerException
	 * 		If task is null.
	 */
	public void cancelTask(WaveFormRenderTask task) {
		if (task == null) throw new NullPointerException("task");
		
		task.keepRendering = false;
		synchronized (taskQueue) {
				taskQueue.remove(task);
		}
	}

	/**
	 * Render of wave forms is done in a dedicated thread because some graphs may have
	 * to proccess / render a lot of samples depending on the zoom / size of the audio track.
	 * 
	 * From a usability perspective, this means that the waveform can be computed accuratly
	 * because responce time is no longer an issue.
	 * 
	 * @author Brook Novak
	 *
	 */
	private class RenderThread extends Thread {
		
		private WaveFormRenderTask currentTask;
		
		private final static float AMPLITUDES_PER_PIXEL = 1.0f; // resolution: how many amplitudes per pixel to render 
		private final static int FRAME_RENDER_RATE = 2000; // The approximate amount of frames to render per pass
		
		private boolean isFinishing = false;
		
		public boolean isFinishing() {
			return isFinishing;
		}
		
		/**
		 * 
		 * @param audioBytes The reference to the audio bytes to render.
		 * 
		 * @param frameLength in frames.
		 */
		public RenderThread(int id) {
			super("RenderThread" + id);
			// Give renderering lowest priority
			setPriority(Thread.MIN_PRIORITY);
			
		}
		
		@Override
		public void run() {
			
			while (true) {
				// Grab a task from the queue
				synchronized(taskQueue) {
					
					if (taskQueue.isEmpty()) {
						isFinishing = true;
						return;
					}
					currentTask = taskQueue.remove();
				}
				
				// Perform rendering until the task has been cancelled
				doRender();
			}
		}
		
		private void doRender() {
			assert(currentTask != null);
			assert(!currentTask.hasStarted());
			
			// Quick check
			if (!currentTask.keepRendering) return;
			
			currentTask.setState(WaveFormRenderTask.STATE_RENDERING);
			
			Graphics2D g = null;
			
			try {
				
				int halfMaxHeight;
				int bufferWidth;
				
				// Create the graphics and prepare the buffer
				
				synchronized(currentTask.imageBuffer) {
					
					halfMaxHeight = currentTask.imageBuffer.getHeight() / 2;
					bufferWidth = currentTask.imageBuffer.getWidth();
					
					g = currentTask.imageBuffer.createGraphics();
	
					g.setStroke(Strokes.SOLID_1);

					// Clear the buffer with transparent pixels
					g.setBackground(ApolloColorIndexedModels.KEY_COLOR);
					g.clearRect(0, 0, bufferWidth, currentTask.imageBuffer.getHeight());
					
				}
				
				// Render waveforms in chunks - so that can cancel rendering and the
				// widget can show progress incrementally
				int lastXPosition = -1;
				int lastYPosition = -1;
			
				// Choose how many amplitudes to render in the graph. i.e. waveform resolution
				int totalAmplitudes = Math.min((int)(bufferWidth * AMPLITUDES_PER_PIXEL), currentTask.frameLength); // get amount of amps to render
				if (totalAmplitudes == 0) return;
				
				int currentAmplitude = 0;
	
				// Calculate the amount of frames to aggregate
				int aggregationSize = currentTask.frameLength / totalAmplitudes;
				assert(aggregationSize >= 1);
	
				// Limit the amount of amplitudes to render (the small chunks) based on the
				// aggregation size - since it correlation to proccess time.
				int amplitudeCountPerPass = (FRAME_RENDER_RATE / aggregationSize);
				if (amplitudeCountPerPass == 0) amplitudeCountPerPass = 1;
	
				int renderStart;
				int renderLength;
				
				// render until finished or cancelled
				while (currentTask.keepRendering && currentAmplitude < totalAmplitudes) { 
					
					renderStart = currentAmplitude * aggregationSize;
					
					// At the last pass, render the last remaining bytes
					renderLength = Math.min(
							amplitudeCountPerPass * aggregationSize, 
							currentTask.frameLength - renderStart);
					
					// At the last pass, be sure that the aggregate size does not exeed the render count
					// so that last samples are not ignored
					aggregationSize =  Math.min(aggregationSize, renderLength);
					
					// Perform the waveform rendering
					float[] amps = currentTask.renderer.getSampleAmplitudes(
							currentTask.audioBytes, 
							currentTask.startFrame + renderStart, 
							renderLength, 
							aggregationSize);
					
					// Draw the rendered waveform to the buffer
					synchronized(currentTask.imageBuffer) { 
						
						g.setColor(ApolloColorIndexedModels.WAVEFORM_COLOR);
						
						if (aggregationSize == 1) {
						
							for (float h : amps) {
								
								int x = (int)(currentAmplitude * ((float)bufferWidth / (float)(totalAmplitudes - 1))); 
								int y = halfMaxHeight - (int)(h * halfMaxHeight);
								
								if (currentAmplitude == 0) { // never draw line on first point
									
									lastXPosition = 0;
									lastYPosition = y;
									
								} else {
					
									g.drawLine(
											lastXPosition, 
											lastYPosition, 
											x, 
											y);
									
									lastXPosition = x;
									lastYPosition = y;
									
								}
								
								currentAmplitude ++;
							}
							
						} else { // dual version - looks nicer and conceptually easy to see audio
							
							for (int i = 0; i < amps.length; i+=2) {
								
								float peak = amps[i];
								float trough = amps[i + 1];
		
								int x = (int)(currentAmplitude * ((float)bufferWidth / (float)(totalAmplitudes - 1))); 
								int ypeak = halfMaxHeight - (int)(peak * halfMaxHeight);
								int ytrough = halfMaxHeight - (int)(trough * halfMaxHeight);
								
								if (currentAmplitude == 0) { // never draw line on first point
									
									lastXPosition = lastYPosition = 0;
									
								} else {
					
									g.drawLine(
											x, 
											ypeak, 
											x, 
											ytrough);
									
									lastXPosition = x;
									lastYPosition = 0;
									
								}
								
								currentAmplitude ++;
							}
							
						}
						
					}
					
					
				} // next pass
				
				// Lession learnt: do not request lots of requests to repaint
				// later on the AWT Event queu otherwise it will get congested
				// and will freeze up the interact for annoying periods of time.
				currentTask.setState(WaveFormRenderTask.STATE_STOPPED);
				
			} finally {
				
				if (g != null) g.dispose();
				
				// safety
				if (!currentTask.isStopped())
					currentTask.setState(WaveFormRenderTask.STATE_STOPPED);
				
				currentTask = null;
			}
		}
		
	}

	/**
	 * A task descriptor for rendering wave forms via the WaveFormRenderProccessingUnit.
	 * 
	 * Raises {@value ApolloSubjectChangedEvent#RENDER_TASK_INVALIDATION_RECOMENDATION} 
	 * when the WaveFormRenderProccessingUnit recommends a refresh
	 * of the BufferedImage. This includes when the WaveFormRenderTask state changes
	 * 
	 * The BufferedImage is always locked before handled by the render thread.
	 * 
	 * Only can use these once... per render task. I.E. Do not re-use
	 * 
	 * @author Brook Novak
	 *
	 */
	public class WaveFormRenderTask extends AbstractSubject {
		
		private boolean keepRendering = true; // a cancel flag
		
		private int state = STATE_PENDING;
		
		private static final int STATE_PENDING = 1;
		private static final int STATE_RENDERING = 2;
		private static final int STATE_STOPPED = 3;
		
		private BufferedImage imageBuffer; // nullified when stopped.
		private byte[] audioBytes; // nullified when stopped. - Arrays (not contents) are immutable so no need to worry about threading issues with indexes
		private final int startFrame;
		private final int frameLength; // in frames
		private final WaveFormRenderer renderer;
		private final boolean recommendInvalidations;
		
		public WaveFormRenderTask(
				BufferedImage imageBuffer, 
				byte[] audioBytes, 
				AudioFormat format,
				int startFrame, 
				int frameLength, 
				boolean recommendInvalidations) {
			
			assert(audioBytes != null);
			assert(format != null);
			assert(imageBuffer != null);
			assert(((startFrame + frameLength) * format.getFrameSize()) <= audioBytes.length);

			this.imageBuffer = imageBuffer;
			this.audioBytes = audioBytes;
			this.startFrame = startFrame;
			this.frameLength = frameLength;
			this.recommendInvalidations = recommendInvalidations;
			renderer = new DualPeakTroughWaveFormRenderer(format);
		}
		
		private void setState(int newState) {
			this.state = newState;
			
			if (keepRendering) { // invalidate if cancel not requested
				recommendInvalidation();
			}
			
			// Nullify expensive references when stopped so they can be freed by the garbage collector
			if (state == STATE_STOPPED) {
				audioBytes = null;
				imageBuffer = null;
			}
			
		}
		
		/**
		 * 
		 * @return
		 * 		True if this is rendering .. thus being proccessed.
		 */
		public boolean isRendering() {
			return state == STATE_RENDERING;
		}

		
		/**
		 * <b>WARNING:</b> Think about the race conditions ... now cannot no when this may start therefore
		 * it is always safe to lock buffer if in pending state.
		 * 
		 * @return
		 * 		True if this has not started to render
		 */
		public boolean hasStarted() {
			return state != STATE_PENDING;
		}

		/**
		 * 
		 * @return
		 * 		True if has stopped
		 */
		public boolean isStopped() {
			return state == STATE_STOPPED;
			
		}
		
		private void recommendInvalidation() {
			if (recommendInvalidations)
				fireSubjectChangedLaterOnSwingThread(
					new SubjectChangedEvent(ApolloSubjectChangedEvent.RENDER_TASK_INVALIDATION_RECOMENDATION));
		}
		
	}
	
}
