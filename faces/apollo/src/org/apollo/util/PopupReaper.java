package org.apollo.util;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.expeditee.gui.MouseEventRouter;
import org.expeditee.gui.Popup;
import org.expeditee.gui.PopupManager;
import org.expeditee.gui.PopupManager.PopupAnimator;

/**
 * Yet another daemon .. whos job is to hide popups after their lifetimes have passed
 * 
 * @author Brook Novak
 */

public class PopupReaper {
	
	private Map<Popup, TemporaryPopup> tempPopups = new HashMap<Popup, TemporaryPopup>();// SHARED RESOURCE - the locker

	private ReapDaemon reaper = null;
	
	private static PopupReaper instance = new PopupReaper();
	private PopupReaper() {
	}
	public  static PopupReaper getInstance() {
		return instance;
	}

	/**
	 * Initializes a popups lifetime so that the popup will eventually hide at a given time period.
	 * 
	 * @param p
	 * 		The popup to hide in <code>lifetime</code> ms. Must not be null.
	 * 
	 * @param hideAnimation
	 * 		The animation to render when the popup hides when its lifetime has passed. Null for
	 *      no animation.
	 * 
	 * @param lifetime
	 * 		The new time for the given popup to live for.
	 * 		Must be larger or equal to zero. In milliseconds.
	 * 
	 * @return
	 * 		True if the given popups lifetime has been re-initialized. False if the popups
	 *		lifetime was either never initialized or has already been hidden or is in the proccesses of hiding.
	 * 
	 * @throws NullPointerException
	 * 		If p is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If lifetime is negative.
	 * 
	 */
	public void initPopupLifetime(Popup p, PopupAnimator hideAnimation, int lifetime) {
		if (p == null) throw new NullPointerException("p");
		if (lifetime < 0) throw new IllegalArgumentException("lifetime < 0");
		
		// Init daemon
		if (reaper == null) {
			reaper = new ReapDaemon();
			reaper.start();
		}

		synchronized(tempPopups) { // locks _tempPopups and _tempPopupQueue by convention
			
			TemporaryPopup tp = tempPopups.get(p);
			
			if (tp != null) {
				
				tp.setNewLifetime(lifetime);
				
			} else {
				tp = new TemporaryPopup(p, hideAnimation, lifetime);
				tempPopups.put(p, tp);
				
				// Notify reaper of new popup to reap .. eventually. It will set its new wait time
				reaper.interrupt();
			}
			

			
		}
		
	}
	
	/**
	 * Resets a popups lifetime to a new value.
	 * 
	 * @param p
	 * 		THe popup to hide in <code>lifetime</code> ms. Must not be null.
	 * 
	 * @param lifetime
	 * 		The new time for the given popup to live for.
	 * 		Must be larger or equal to zero. In milliseconds.
	 * 
	 * @return
	 * 		True if the given popups lifetime has been re-initialized. False if the popups
	 *		lifetime was either never initialized or has already been hidden or is in the proccesses of hiding.
	 * 
	 * @throws NullPointerException
	 * 		If p is null.
	 * 
	 * @throws IllegalArgumentException
	 * 		If lifetime is negative.
	 * 
	 */
	public boolean revivePopup(Popup p, int lifetime) {
		if (p == null) throw new NullPointerException("p");
		if (lifetime < 0) throw new IllegalArgumentException("lifetime < 0");
		
		synchronized(tempPopups) { // locks _tempPopups and _tempPopupQueue by convention
			
			TemporaryPopup tp = tempPopups.get(p);
			
			if (tp != null) {
				tp.setNewLifetime(lifetime);
				return true;
			} 
			
		}
		
		return false;
	}

	
	private class ReapDaemon extends Thread {
		
		ReapDaemon() {
			super("Popup Reaper");
			super.setPriority(Thread.MIN_PRIORITY);
			super.setDaemon(true);
		}
	
		public void run() {
			
			while (true) {
			
				long waitTime = reap();
				
				try {
					if (waitTime < 0) {
						sleep(10000); // some arbitary wait
					} else if (waitTime > 0) {
						sleep(waitTime);
					}
				} catch (InterruptedException e) {
				}

			} // keep reaping forever
			
		}
		
		/**
		 * 
		 * @return
		 * 		A Positive wait time, or a negative for nothing to to wait on.
		 */
		private long reap() {

			Collection<TemporaryPopup> snapshot;
			
			// Grab a temp popup
			synchronized(tempPopups) { 
				snapshot = new ArrayList<TemporaryPopup>(tempPopups.values());
			}
			
			long nextDelay = -1;
			
			for (TemporaryPopup tp : snapshot) {
				
				long del = tp.getDelay();
				if (del <= 0) { // this popups lifetime is up .. maybe - if the mouse is not over it
	
					DoHide hider = new DoHide(tp);
					
					try {
						SwingUtilities.invokeAndWait(hider);
					} catch (InterruptedException e) { 
						return 0; // retry straight away
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
					
					if (hider.didHide) {
						
						synchronized(tempPopups) { 
							tempPopups.remove(tp.popup);
						}
						
					} else { // did not hide because mouse cursor is over pop / invoker
						tp.resetLifetime();
						del = tp.getDelay();
					}
	
				}
				
				if (nextDelay < 0 || (del >= 0 && del < nextDelay)) {
					// Smallest delay time
					nextDelay = del;
				}
	
			}
			
			return nextDelay;
			
		}
		
	}

	/**
	 * Hides a temp popup on the swing thread.
	 * {@link #didHide} is set to true on hide, false if did not hide because mouse cursor
	 * was over the popup / popup invoker.
	 * 
	 * @author Brook Novak
	 *
	 */
	private class DoHide implements Runnable {
		
		private TemporaryPopup tempPopup;
		public boolean didHide = false;
		
		public DoHide(TemporaryPopup tempPopup) {
			this.tempPopup = tempPopup;
		}
		
		/**
		 * 
		 * @param c
		 * 
		 * @param screenPoint
		 * 
		 * @return
		 * 		True if screenPoint is over c.
		 */
		private boolean isPointInComponant(Component c, Point screenPoint) {
			assert(c != null);
			assert(screenPoint != null);
			
			Point compPositoinOnScreen = new Point(0,0);
			SwingUtilities.convertPointToScreen(compPositoinOnScreen, c);
			Rectangle bounds = c.getBounds();
			bounds.x = compPositoinOnScreen.x;
			bounds.y = compPositoinOnScreen.y;
			
			return bounds.contains(screenPoint);
		}
	
		public void run() {
			assert(tempPopup != null);
			
			MouseEvent me = MouseEventRouter.getCurrentMouseEvent();
			if (me != null) {
				
				if (isPointInComponant(tempPopup.popup, me.getLocationOnScreen()))
					return;
				
				Component invoker = PopupManager.getInstance().getInvoker(tempPopup.popup);
				if (invoker != null && isPointInComponant(invoker, me.getLocationOnScreen()))
					return;
				
			}

			if (tempPopup.hideAnim != null) {
				PopupManager.getInstance().hidePopup(tempPopup.popup, tempPopup.hideAnim);
			} else {
				PopupManager.getInstance().hidePopup(tempPopup.popup);
			}
			
			didHide = true;
			
		}
	}
	
	/**
	 * Represents a popup that will eventually hide by the reaper.
	 * 
	 * @author Brook Novak
	 *
	 */
	private class TemporaryPopup implements Comparable {

		private Popup popup;
		private PopupAnimator hideAnim;
		private long destructionTime;
		private int lifetime;
		
		/**
		 * Constructor.
		 * 
		 * @param popup
		 * 			The encapsulated popup - must not be null.
		 * 
		 * @param anim
		 * 			Null for no anim...
		 * 
		 * @param lifetime
		 * 			The time from now to which the popup should hide. In MS
		 * 
		 *
		 */
		public TemporaryPopup(Popup popup, PopupAnimator anim, int lifetime) {
			assert(popup != null);
			assert(lifetime >= 0);
			
			this.popup = popup;
			this.hideAnim = anim;
			
			setNewLifetime(lifetime);
		}

		public void setNewLifetime(int lifetime) {
			assert(lifetime >= 0);
			this.lifetime = lifetime;
			this.destructionTime = System.currentTimeMillis() + lifetime;
		}
		
		public void resetLifetime() {
			setNewLifetime(lifetime);
		}
		
		public long getDelay() {
			return destructionTime - System.currentTimeMillis();
		}
		
		/**
		 * {@inheritDoc}
		 */
		public int compareTo(Object o) {
			
			long dt2 = ((TemporaryPopup)o).destructionTime;
			
			if (destructionTime < dt2)
		         return -1;
			
		      if (destructionTime > dt2)
		         return 1;
		      
			return 0;
		}
		

	}
}
