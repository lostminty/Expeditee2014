package org.apollo.util;

import java.util.LinkedList;
import java.util.Queue;

import javax.swing.SwingUtilities;

import org.apollo.audio.structure.OverdubbedFrame;
import org.apollo.audio.structure.TrackGraphLoopException;
import org.apollo.audio.structure.AudioStructureModel;
import org.expeditee.gui.FrameIO;

/**
 * Fetches overdubbed frame heirarchies on a dedicated thread.
 * 
 * @author Brook Novak
 *
 */
public class ODFrameHeirarchyFetcher {

	private FetchWorker fetcher = null;
	
	/** Using a FIFO queue implementation */
	private Queue<FetchRequest> fetchRequestQueue = new LinkedList<FetchRequest>(); // SHARED RESOURCE
	
	/** Singleton design pattern */
	private static ODFrameHeirarchyFetcher instance = new ODFrameHeirarchyFetcher();
	private ODFrameHeirarchyFetcher() { }
	public static ODFrameHeirarchyFetcher getInstance() {
		return instance;
	}
	
	/**
	 * Asynchronously (non-blocking) performs a fetch for a given frame name. Once the fetch is
	 * complete the odRec's {@link ODFrameReceiver#receiveResult(OverdubbedFrame, TrackGraphLoopException)}
	 * is invoked.
	 * 
	 * If there is a request for the given ODFrameReceiver that is already pending, the old request is replaced
	 * if the rootFrameName differs, otherwise the old fetch remains and the new fetch is ignored.
	 * 
	 * @see {@link ODFrameReceiver)}
	 * 
	 * @param rootFrameName
	 * 		The frame name to get the heirarchy for. Must be a valid framename
	 * 
	 * @param odRec
	 * 		The ODFrameReceiver to receive the result.
	 * 
	 * @throws NullPointerException
	 * 		If rootFrameName or odRec is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If rootFrameName is not a valid framename.
	 */
	public void doFetch(String rootFrameName, ODFrameReceiver odRec) {
		if (rootFrameName == null) throw new NullPointerException("rootFrameName");
		if (!FrameIO.isValidFrameName(rootFrameName)) 
			throw new IllegalArgumentException("rootFrameName is an invalid framename");
		
		if (odRec == null) throw new NullPointerException("odRec");

		
		FetchRequest newfr = new FetchRequest(rootFrameName, odRec);
		
		synchronized(fetchRequestQueue) {
			
			// See if a fetch for the receiver already exists
			FetchRequest match = null;
			for (FetchRequest existingfr : fetchRequestQueue) {
				if (newfr.equals(existingfr)) {
					match = existingfr;
					break;
				}
			}
			
			// If a request already exists
			if (match != null) {
				// Check if the new fetch request is for a different odframe
				if (!match.getRootFrameName().equals(rootFrameName)) {
					fetchRequestQueue.remove(match); // replace redundant request
					fetchRequestQueue.add(newfr);
				}
			} else {
				fetchRequestQueue.add(newfr);
			}
			
			// Ensure that the fetch thread is running
			if (fetcher == null || !fetcher.isAlive()) {
				fetcher = new FetchWorker();
				fetcher.start();
			}
		}

	}
	
	
	private class FetchWorker extends Thread {

		FetchWorker() {
			super("FetchWorker");
		}
		
		public void run() {
			
			while (true) {

				FetchRequest request = null;
				
				synchronized(fetchRequestQueue) {
	
					if (!fetchRequestQueue.isEmpty())
						request = fetchRequestQueue.remove();
					
					if (request == null) return;
				}
	
				boolean hasUpdated = false;
				do {
					try {
						AudioStructureModel.getInstance().waitOnUpdates();
						hasUpdated = true;
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;
					}
				} while (!hasUpdated);

				while (true) {
					try {
						request.odFrameResult = AudioStructureModel.getInstance().fetchGraph(request.getRootFrameName());
						break;
					} catch (InterruptedException e) { // cancelled, retry
						/* Consume */
						
					} catch (TrackGraphLoopException e) { // contains loop
						request.loopExResult = e;
						break;
					}
				} 
				
				// Run result notiofaction for this request on swing thread
				SwingUtilities.invokeLater(request);
				
			}
				
				
		
		}
	}
	
	/**
	 * Immutable two-tuple for storing fetch requests.
	 * 
	 * @author Brook Novak
	 */
	private class FetchRequest implements Runnable {
		
		private String rootFrameName;
		private ODFrameReceiver receiver;
		
		OverdubbedFrame odFrameResult = null;
		TrackGraphLoopException loopExResult = null;
		
		/**
		 * 
		 * @param rootFrameName
		 * 			Must be a valid framename.
		 * 
		 * @param receiver
		 */
		public FetchRequest(String rootFrameName, ODFrameReceiver receiver) {
			this.rootFrameName = rootFrameName;
			this.receiver = receiver;
			assert(rootFrameName != null);
			assert(FrameIO.isValidFrameName(rootFrameName)) ;
			assert(receiver != null);
		}

		@Override
		public boolean equals(Object obj) {
			return receiver.equals(obj);
		}

		@Override
		public int hashCode() {
			return receiver.hashCode();
		}

		public ODFrameReceiver getReceiver() {
			return receiver;
		}

		public String getRootFrameName() {
			return rootFrameName;
		}
		
		/**
		 * To be invoked on swing thread
		 */
		public void run() {
			receiver.receiveResult(odFrameResult, loopExResult);
		}

	}
	
	/**
	 * A frame receive can request for fetches and have their 
	 * {@link #receiveResult(OverdubbedFrame, TrackGraphLoopException)}
	 * method invoked once a result has occured.
	 * 
	 * @author Brook Novak
	 */
	public interface ODFrameReceiver {
		
		/**
		 * Invoked once a request has been proccessed. <b>This is invoked on the swing thread.</b>
		 * 
		 * @see ODFrameHeirarchyFetcher#doFetch(String, org.apollo.util.ODFrameHeirarchyFetcher.ODFrameReceiver)
		 * 
		 * @param odFrame
		 * 		The overdubbed frame. Null if the frame does not exist - or if it does exist but there are no track widgets on it.
		 * 		Or Null if there was a loop.
		 * 		
		 * @param loopEx
		 * 		If there was a loop, then this will be set containing the loop info.
		 */
		public void receiveResult(OverdubbedFrame odFrame, TrackGraphLoopException loopEx);
	
	}

}
