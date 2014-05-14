package org.expeditee.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

/**
 * A centralized container for all custom popups in expeditee.
 * 
 * @author Brook Novak
 */
public final class PopupManager implements DisplayIOObserver {
	
	/** Singleton */
	private PopupManager() {} 
	
	/** Singleton */
	private static PopupManager _instance = new PopupManager();
	
	/**
	 * @return The singleton instance.
	 */
	public static PopupManager getInstance() {
		return _instance;
	}
	
	// popup->invoker
	private HashMap<Popup, Component> _popups = new HashMap<Popup, Component>();
	// quick ref to invokers
	private HashSet<Component> _invokers = new HashSet<Component>();
	
	private LinkedList<AnimatedPopup> _animatingPopups = new LinkedList<AnimatedPopup>();
	private AnimationThread _animationThread = null;

	private final int ANIMATION_DURATION = 180; // Tume its takes for a maximize . minimize to animate. In ms.
	private final int ANIMATION_RATE = 30; // in ms
	
	/**
	 * Determines whether a given point is over a popup.
	 * 
	 * @param p
	 * 
	 * @return True if p is over a popup
	 * 
	 * @throws NullPointerException
	 * 		If p is null
	 */
	public boolean isPointOverPopup(Point p) {
		if (p == null) throw new NullPointerException("p");
		for (Popup pp : _popups.keySet()) {
			if (pp.getBounds().contains(p)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Tests a component to see if it is in invoker of an existing popup.
	 * 
	 * @param c
	 * 	Must not be null.
	 * 
	 * @return
	 * 		True if c is an invoker
	 * 
	 * @throws NullPointerException
	 * 		If c is null
	 */
	public boolean isInvoker(Component c) {
		if (c == null) throw new NullPointerException("c");
		return _invokers.contains(c);
	}
	
	/**
	 * Gets an invoker for a popup
	 * 
	 * @param p
	 * 		The popup to get the invoker for.
	 * 
	 * @return
	 * 		The invoker for the given popup.
	 * 		Null if popup does not exist.
	 */
	public Component getInvoker(Popup p) {
		if (p == null) throw new NullPointerException("p");
		return _popups.get(p);
	}
	
	/**
	 * Use this instead of isVisible to determine if the popup is showing or not,
	 * since it considers animation.
	 * 
	 * @return
	 * 		True if this popup is showing. <b>IMPORTANT:</b> This includes
	 * 		if the popup is not yet visible, but in an animation sequence for showing...
	 * 
	 * @throws NullPointerException
	 * 		If p is null
	 */
	public boolean isShowing(Popup p) {
		if (p == null) throw new NullPointerException("p");
		return _popups.containsKey(p);
	}
	
	/**
	 * @return
	 * 		True if a poup is showing. False otherwise.
	 */
	public boolean isAnyPopupsShowing () {
		return !_popups.isEmpty();
	}
	
	/**
	 * @return
	 * 		True if the mouse click event for going back a frame should be consumed
	 * 		Due to a popup requesting this event to be consumed currently showing.
	 */
	public boolean shouldConsumeBackClick() {
		for (Popup p : _popups.keySet()) {
			if (p.shouldConsumeBackClick())
				return true;
		}
		
		return false;
	}
	
	/**
	 * Clears all popups from the browser that are autohidden.
	 * Stops animations.
	 */
	public void hideAutohidePopups() {
		

		// Get rid of all animations that are not non-auto-hidden pops that are expanding
		synchronized (_animatingPopups) {
			
			LinkedList<AnimatedPopup> animationsToClear = new LinkedList<AnimatedPopup>();
			
			for (AnimatedPopup ap : _animatingPopups) {
				if (!(ap.popup != null && !ap.popup.doesAutoHide())) {
					animationsToClear.add(ap);
				}
			}
			
			_animatingPopups.removeAll(animationsToClear);
			
		}
		
		LinkedList<Popup> popupsToClear = new LinkedList<Popup>();
		LinkedList<Component> invokersToClear = new LinkedList<Component>();

		// Get ride of the actual popups
		for (Popup p : _popups.keySet()) {
			if (p.doesAutoHide()) {
				
				popupsToClear.add(p);
				invokersToClear.add(_popups.get(p));
				
				p.invalidateAppearance();
				p.setVisible(false);
				Browser._theBrowser.getLayeredPane().remove(p);
				p.onHide();
			}
		}
		
		assert (popupsToClear.size() == invokersToClear.size());
		
		for (int i = 0; i < popupsToClear.size(); i++) {
			_popups.remove(popupsToClear.get(i));
			_invokers.remove(invokersToClear.get(i));
		}
		

		
	}
	
	
	
//	public void hideAllPopups() {
//		
//		for (Popup p : _popups.keySet()) {
//			invalidatePopup(p);
//			p.setVisible(false);
//			Browser._theBrowser.getLayeredPane().remove(p);
//			p.onHide();
//		}
//		_popups.clear();
//		_invokers.clear();
//		
//		// Get rid of all animations
//		synchronized (_animatingPopups) {
//			_animatingPopups.clear();
//		}
//	}
	
	public void frameChanged() {
		// Remove any popups that are showing on the current frame
		hideAutohidePopups();
	}

	/**
	 * Hides a popup - if not already hidden.
	 * 
	 * @param p
	 * 		Must not be null.
	 * 
	 * @throws NullPointerException
	 * 		If p is null
	 */
	public void hidePopup(Popup p) {
		if (p == null) throw new NullPointerException("p");
		
		if (!isShowing(p) && (!p.isVisible() || p.getParent() == null)) return;
		

		// Cancel any showing animations
		synchronized (_animatingPopups) {
			AnimatedPopup toRemove = null;
			for (AnimatedPopup ap : _animatingPopups) {
				if (ap.popup == p) {
					toRemove = ap;
					break;
				}
			}
			if (toRemove != null)
				_animatingPopups.remove(toRemove);
		}

		p.invalidateAppearance();
		p.setVisible(false);
		Browser._theBrowser.getLayeredPane().remove(p);
		Component invoker = _popups.remove(p);
		if (invoker != null) _invokers.remove(invoker);
		p.onHide();
		
	}
	
	/**
	 * Hides a popup - with animation. - if not already hidden.
	 * 
	 * @param p
	 * 		Must not be null.
	 * @param animator
	 * 		Must not be null.
	 * 
	 * @throws NullPointerException
	 * 		If p or animator is null
	 * 
	 */
	public void hidePopup(Popup p, PopupAnimator animator) {
		
		if (p == null) throw new NullPointerException("p");
		if (animator == null) throw new NullPointerException("animator");
		
		if (!isShowing(p) && (!p.isVisible() || p.getParent() == null)) return;
		
		hidePopup(p);
		AnimatedPopup ap = new AnimatedPopup(
				animator, 
				System.currentTimeMillis(), 
				null,
				false,
				p.getLocation());

		animator.starting(false, p.getBounds());
		
		synchronized (_animatingPopups) {
			_animatingPopups.add(ap);
		}

		if (_animationThread == null || !_animationThread.isAlive() || _animationThread.willDie) {
			_animationThread = new AnimationThread();
			_animationThread.start();
		}
	}
	
	
	
	/**
	 * Displays a popup at a specific location.
	 * 
	 * @param p
	 * 		Must not be null.
	 * 
	 * @param invoker 
	 * 			The component responsible for showing the popup. can be null.
	 * 			Used such that when invoker pressed, the popup will not auto hide.
	 * 
	 * @param loc
	 * 		Must not be null.
	 * 
	 * @throws NullPointerException
	 * 		If p or loc is null
	 * 
	 */
	public void showPopup(Popup p, Point loc, Component invoker) {
		if (p == null) throw new NullPointerException("p");
		if (loc == null) throw new NullPointerException("animator");
		
		
		if (_popups.containsKey(p)) return;

		p.prepareToPaint();
		p.setLocation(loc);
		p.setVisible(true);

		Browser._theBrowser.getLayeredPane().add(p, JLayeredPane.POPUP_LAYER, 0);

		_popups.put(p, invoker);
		if (invoker != null) _invokers.add(invoker);
		
		p.onShowing();
		p.onShow();
		
		// Invalidate the popup border
		if (p.getBorderThickness() > 0.0f) {
			p.invalidateAppearance();
		}
	}
	
	/**
	 * Displays a popup at a specific location - with animation.
	 * 
	 * @param p
	 * 		Must not be null.
	 * 
	 * @param invoker 
	 * 			The component responsible for showing the popup. can be null.
	 * 			Used such that when invoker pressed, the popup will not auto hide.
	 * 
	 * @param loc
	 * 		Must not be null.
	 * 
	 * @param animator
	 * 		Must not be null.
	 * 
	 * @throws NullPointerException
	 * 		If p, animator or loc is null
	 */
	public void showPopup(Popup p, Point loc, Component invoker, PopupAnimator animator) {
		if (animator == null)
			throw new NullPointerException("animator");
		if (p == null)
			throw new NullPointerException("p");
		if (loc == null)
			throw new NullPointerException("loc");
		
		if (_popups.containsKey(p)) return;
		
		_popups.put(p, invoker);
		if (invoker != null) _invokers.add(invoker);
		
		
		AnimatedPopup ap = new AnimatedPopup(
				animator, 
				System.currentTimeMillis(), 
				p,
				true,
				loc);
		
		
		animator.starting(true, new Rectangle(loc.x, loc.y, p.getWidth(), p.getHeight()));
		
		p.onShowing();
		
		synchronized (_animatingPopups) {
			_animatingPopups.add(ap);
		}

		if (_animationThread == null || !_animationThread.isAlive() || _animationThread.willDie) {
			_animationThread = new AnimationThread();
			_animationThread.start();
		}

	}
	
	/**
	 * Does a pure asynch animation with no popups involved.
	 * 
	 * For example you may want to have an effect such that an item is expanding
	 * or moving into a new location on the screen.
	 * 
	 * @param animator
	 * 		Must not be null.
	 * 
	 * @param target
	 * 		Must not be null.
	 * 
	 * @throws NullPointerException
	 * 		If animator or target is null
	 * 
	 */
	public void doPureAnimation(PopupAnimator animator, Rectangle target) {
		
		if (animator == null)
			throw new NullPointerException("animator");
		if (target == null)
			throw new NullPointerException("target");
		
		AnimatedPopup ap = new AnimatedPopup(
				animator, 
				System.currentTimeMillis(), 
				null,
				false,
				target.getLocation());
		
		
		animator.starting(true, target);

		synchronized (_animatingPopups) {
			_animatingPopups.add(ap);
		}

		if (_animationThread == null || !_animationThread.isAlive() || _animationThread.willDie) {
			_animationThread = new AnimationThread();
			_animationThread.start();
		}
		
	}
	

	/**
	 * Paints all popups in the browsers popup pane with the given graphics.
	 * 
	 * @param g
	 * 		Where to paint to.
	 * 
	 * @param clip
	 */
	void paintLayeredPane(Graphics g, Area clip) {
		 if (Browser._theBrowser == null) return;
		 
		Component[] compsOnPopup = Browser._theBrowser.getLayeredPane().getComponentsInLayer(JLayeredPane.POPUP_LAYER);

		for (int i = 0; i < compsOnPopup.length; i++) {
			Component c = compsOnPopup[i];
	
			Point p = c.getLocation();

			if (clip == null || clip.intersects(c.getBounds())) {
				g.translate(p.x, p.y);
				c.paint(g);
				g.translate(-p.x, -p.y);
			}
			
		} 
	}

	/**
	 * Paints current popup animations to the expeditee browser content pane.
	 */
	void paintAnimations() {
		
		if (Browser._theBrowser == null
				|| Browser._theBrowser.g == null) return;
		
		Graphics g = Browser._theBrowser.g;
		
		synchronized (_animatingPopups) {
			
			for (AnimatedPopup ap : _animatingPopups) {
				ap.animator.paint(g);
			}
			
		}
	}
	
	/**
	 * Proccesses animation on a dedicated thread.
	 * 
	 * @author Brook Novak
	 *
	 */
	private class AnimationThread extends Thread {
		
		private boolean willDie = false;
		
		@Override
		public void run() {
			
			
			 LinkedList<AnimatedPopup> finishedAnimations;
			 
			 while (true) {
			 
				 // Perform animation logic
				 finishedAnimations = animate();
				
				 // Check if finished all animations
				 if (finishedAnimations == null) return; // done
				 
				 // Check for finalization of animation. That is, adding the popups to the layered pane
				 boolean needsFinalization = false;
				 for (AnimatedPopup ap : finishedAnimations) {
					 if (ap.isShowing) {
						 needsFinalization = true;
						 break;
					 } 
					 
					 // FInal invalidation
					 FrameGraphics.invalidateArea(ap.animator.getCurrentDrawingArea());
					 
				 }
				 
				 if (needsFinalization) {
					 SwingUtilities.invokeLater(new AnimationFinalizor(finishedAnimations));
					 // Will repaint when a popup becomes anchored...
				 } else {
					 FrameGraphics.requestRefresh(true);
				 }

				 // Limit animation rate
				 try {
					 sleep(ANIMATION_RATE);
				 } catch (InterruptedException e) {
					e.printStackTrace();
				 }
			
			 }
			
		}
		
		/**
		 * Performs animations
		 * @return
		 */
		private LinkedList<AnimatedPopup> animate() {
			
			LinkedList<AnimatedPopup> finishedPopups = null;
			
			synchronized (_animatingPopups) {
				
				if (_animatingPopups.isEmpty()) {
					willDie = true;
					return null;
				}

				long currentTime = System.currentTimeMillis();
				
				finishedPopups = new LinkedList<AnimatedPopup>();
				
				for (AnimatedPopup ap : _animatingPopups) {
					
					long duration = currentTime - ap.startTime;
					
					if (duration >= ANIMATION_DURATION) { // check if complete
						
						finishedPopups.add(ap);
	
					} else {
						
						float percent = ((float)duration / (float)ANIMATION_DURATION);
						assert (percent >= 0.0f);
						assert (percent < 1.0f);
						
						Rectangle dirty = ap.animator.update(percent);
						if (dirty != null)
							FrameGraphics.invalidateArea(dirty);
						
					}

				}
				
				_animatingPopups.removeAll(finishedPopups);
				
			}
			
			return finishedPopups;
			
		}
		
		/**
		 * Adds popups to layered pane
		 * @author Brook Novak
		 *
		 */
		private class AnimationFinalizor implements Runnable {

			private LinkedList<AnimatedPopup> finished;
			
			AnimationFinalizor(LinkedList<AnimatedPopup> finished) {
				this.finished = finished;
			}
			
			public void run() {
				
				for (AnimatedPopup ap : finished) {
					
					if (ap.isShowing && _popups.containsKey(ap.popup)) {

						ap.popup.prepareToPaint();
						ap.popup.setLocation(ap.popupLocation);
						ap.popup.setVisible(true);

						Browser._theBrowser.getLayeredPane().add(ap.popup, JLayeredPane.POPUP_LAYER, 0);
						
						ap.popup.onShow();
						
						// Invalidate the popup border
						if (ap.popup.getBorderThickness() > 0.0f) {
							ap.popup.invalidateAppearance();
						}
					}
					
				}
				
			}
		}
	}
	
	private class AnimatedPopup {
		
		PopupAnimator animator;
		long startTime;
		Popup popup = null;
		boolean isShowing;
		Point popupLocation;
		
		public AnimatedPopup(PopupAnimator animator, long startTime, Popup popup, 
				boolean isShowing, Point popupLocation) {
			
			assert(animator != null);
			assert(popupLocation != null);
			
			// Only have popup if showing
			assert (!isShowing && popup == null || (isShowing && popup != null));
			
			
			this.animator = animator;
			this.startTime = startTime;
			this.popup = popup;
			this.isShowing = isShowing;
			this.popupLocation = popupLocation;

		}

	}
	
	/**
	 * Provides animations for a popup when hiding or when showing.
	 * 
	 * Note that {@link PopupAnimator#paint(Graphics)} and {@link PopupAnimator#update(float)} 
	 * will always be invoked at seperate times - so do not have to worry about thread-saefty.
	 * 
	 * These should only be used once... one per popup at a time.
	 * 
	 * @author Brook Novak
	 *
	 */
	public interface PopupAnimator {
		
		/**
		 * Invoked before showing. Any prepaations are done here.
		 * 
		 * @param isShowing
		 * 		True if this animation will be for a popup that is showing.
		 * 		False if this animation will be for a popup that is hiding.
		 * 
		 * @param popupBounds
		 * 		The location of the popup. I.E. where it is, or where it will be.
		 * 
		 */
		void starting(boolean isShowing, Rectangle popupBounds);
		
		/**
		 * 
		 * Called on an animation thread.
		 * 
		 * @param percent
		 * 		The percent complete of the animations.
		 * 		Rangles from 0 to 1.
		 * 
		 * @return dirty area that needs painting for last update... Null for no invalidation
		 * 		
		 * 
		 */
		Rectangle update(float percent);
		
		/**
		 * Paints the animation - on the swing thread.
		 * Note that this is always on the content pane - not the expeditee frame buffer.
		 * 
		 * @param g
		 * 
		 */
		void paint(Graphics g);
		
		
		/**
		 * 
		 * @return
		 * 		The area which the animation is drawn. Used for final invalidation. Null for no final invaliation
		 */
		Rectangle getCurrentDrawingArea();
		
	}
	
	
	public class ExpandShrinkAnimator implements PopupAnimator {
		
		private boolean isShowing;
		private Rectangle popupBounds;
		private Rectangle sourceRectangle;
		private Rectangle currentRectangle;
		private Color fillColor;
		
		private final Stroke stroke = new BasicStroke(2.0f);
		
		/**
		 * 
		 * @param sourceRectangle
		 * @param fillColor
		 * 		The fill color of the animated rectangle. Null for no fill.
		 */
		public ExpandShrinkAnimator(Rectangle sourceRectangle, Color fillColor) {
			if (sourceRectangle == null) throw new NullPointerException("sourceRectangle");
			
			this.fillColor = fillColor;
			this.sourceRectangle = (Rectangle)sourceRectangle.clone();
			this.currentRectangle = (Rectangle)sourceRectangle.clone();
		}

		public void paint(Graphics g) {
			
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillRect(currentRectangle.x, currentRectangle.y, currentRectangle.width, currentRectangle.height);
			}
			
			g.setColor(Color.BLACK);
			((Graphics2D)g).setStroke(stroke);
			g.drawRect(currentRectangle.x, currentRectangle.y, currentRectangle.width, currentRectangle.height);
		}

		public void starting(boolean isShowing, Rectangle popupBounds) {
			this.isShowing = isShowing;
			this.popupBounds = popupBounds;
			
			if (isShowing) {
				this.currentRectangle = (Rectangle)sourceRectangle.clone();
			} else {
				this.currentRectangle = (Rectangle)sourceRectangle.clone();
			}
			
		}

		public Rectangle update(float percent) {

			Rectangle oldBounds = currentRectangle;
			
			if (!isShowing) { // if minimizing just reverse percent
				percent = 1 - percent;
			}
			
			// update X
			currentRectangle.x = sourceRectangle.x + 
				(int)((popupBounds.x - sourceRectangle.x) * percent);
			
			// update Y
			currentRectangle.y = sourceRectangle.y + 
				(int)((popupBounds.y - sourceRectangle.y) * percent);
			
			// update width
			currentRectangle.width = sourceRectangle.width + 
				(int)((popupBounds.width - sourceRectangle.width) * percent);
			
			// update height
			currentRectangle.height = sourceRectangle.height + 
				(int)((popupBounds.height - sourceRectangle.height) * percent);
			
			int x = Math.min(oldBounds.x, currentRectangle.x);
			int y = Math.min(oldBounds.y, currentRectangle.y);
			int width = Math.min(oldBounds.x + oldBounds.width, currentRectangle.x + currentRectangle.width) - x;
			int height = Math.min(oldBounds.y + oldBounds.height, currentRectangle.y + currentRectangle.height) - y;
			
			return new Rectangle(x, y, width, height);
			
		}
		
		public Rectangle getCurrentDrawingArea() {
			return currentRectangle;
		}


	}
	

}
