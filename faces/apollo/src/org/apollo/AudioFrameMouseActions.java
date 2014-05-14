package org.apollo;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apollo.util.Mutable;
import org.apollo.widgets.LinkedTrack;
import org.apollo.widgets.SampledTrack;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FreeItems;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.widgets.InteractiveWidget;

/**
 * Apollos mouse actions extensions. Overrides expeditees frame mouse actions and
 * forwards them onto expeditee (filtering out certain non-conflicting key-sequences).
 * 
 * @author Brook Novak
 *
 */
public class AudioFrameMouseActions implements MouseMotionListener, MouseListener {
	
	private static final int COARSE_X_PLACEMENT_TOLERANCE = 80; // The minium distance from a track widet to auto-align free space items to

	private static final int COARSE_Y_PLACEMENT_TOLERANCE = 20; 
	
	private static boolean isAnchoring = false;
	private static boolean isStamping = false;
	
	private static List<Item> anchoringItems = new LinkedList<Item>();

	public void mouseDragged(MouseEvent e) {
		FrameMouseActions.getInstance().mouseDragged(e);
	}

	public void mouseMoved(MouseEvent e) {

		boolean forwareToExpeditee = true;
		
		if (AudioFrameKeyboardActions.isControlDown != e.isControlDown()) {
			AudioFrameKeyboardActions.isControlDown = e.isControlDown();
			FrameGraphics.refresh(false); // For special post effects
		}
		
		AudioFrameKeyboardActions.isShiftDown = e.isShiftDown();
		
		if (isYAxisRestictionOn() && !FreeItems.getInstance().isEmpty()) { 

			// Restrict movement of free items to Y-Axis only
			forwareToExpeditee = false;

			int smallestY = FreeItems.getInstance().get(0).getY();

			for (Item i : FreeItems.getInstance()) {
				if (i.getY() < smallestY) smallestY = i.getY();
			}

			for (Item i : FreeItems.getInstance()) {
				i.setY(e.getY() + (i.getY() - smallestY));
			}
			
			// Avoids no-op suppression in expeditee once anchored free items
			FrameMouseActions.MouseX = e.getX();
			FrameMouseActions.MouseY = e.getY();
			
		} else if (isSnapOn() &&
				!FreeItems.getInstance().isEmpty()) { 
			
			// Couse movement of free items: Restraining left-most pixel in items
			// to the closest anchored track widgets x position.
			
			Frame currentFrame = DisplayIO.getCurrentFrame();
			if (currentFrame != null) {
				
				// Search all anchored track x positions for the current frame
				List<InteractiveWidget> widgets = currentFrame.getInteractiveWidgets();
				
				int closestXPosition = -1;
				int closestYPosition = -1;
				int closestXDistance = -1;
				int closestYDistance = -1;
				
				for (InteractiveWidget iw : widgets) {
					
					if (iw instanceof SampledTrack || iw instanceof LinkedTrack) {
						
						// Determine TOP-LEFT Snapping
						int xDistance = Math.abs(e.getX() - iw.getX());
						if (closestXDistance < 0 || xDistance < closestXDistance) {
							closestXDistance = xDistance;
							closestXPosition = iw.getX();
						}
						
						int yDistance = Math.abs(e.getY() - iw.getY());
						if (closestYDistance < 0 || yDistance < closestYDistance) {
							closestYDistance = yDistance;
							closestYPosition = iw.getY();
						}

						// Determine BOTTOM-RIGHT Snapping
						xDistance = Math.abs(e.getX() - (iw.getX() + iw.getWidth()));
						if (closestXDistance < 0 || xDistance < closestXDistance) {
							closestXDistance = xDistance;
							closestXPosition = iw.getX() + iw.getWidth();
						}
						
						yDistance = Math.abs(e.getY() - (iw.getY() + iw.getHeight()));
						if (closestYDistance < 0 || yDistance < closestYDistance) {
							closestYDistance = yDistance;
							closestYPosition = iw.getY() + iw.getHeight();
						}
					}
						
				}
				
				//Determine top-left positoin of free items
				int smallestX = FreeItems.getInstance().get(0).getX();
				int smallestY = FreeItems.getInstance().get(0).getY();
				for (Item i : FreeItems.getInstance()) {
					if (i.getX() < smallestX) smallestX = i.getX();
					if (i.getY() < smallestY) smallestY = i.getY();
				}

				for (Item i : FreeItems.getInstance()) {
					
					int x;
					int y;
					
					if (closestXDistance > 0 && closestXDistance < COARSE_X_PLACEMENT_TOLERANCE) 
						x = closestXPosition + (i.getX() - smallestX);
					else 
						x = e.getX() + (i.getX() - smallestX);
					
					if (closestYDistance > 0 && closestYDistance < COARSE_Y_PLACEMENT_TOLERANCE)
						y = closestYPosition + (i.getY() - smallestY);
					else 
						y = e.getY() + (i.getY() - smallestY);
					
					i.setPosition(x, y);
					
				}

				forwareToExpeditee = false;

			}
			
		}
		
		// Expeditees frame mouse actions uses an offset and fights over free-item
		// movement if it listens to the mouse event router... therefore add an extra
		// layer to avoid this... otherwise auto-aligned items jitter like crazy while
		// moving the cursus
		if (forwareToExpeditee) {
			FrameMouseActions.getInstance().mouseMoved(e);
		} else {
			FrameGraphics.refresh(true);
		}
	}
	
	public void mouseClicked(MouseEvent e) {
		FrameMouseActions.getInstance().mouseClicked(e);
	}

	public void mouseEntered(MouseEvent e) {
		FrameMouseActions.getInstance().mouseEntered(e);
	}

	public void mouseExited(MouseEvent e) {
		FrameMouseActions.getInstance().mouseExited(e);
	}

	public void mousePressed(MouseEvent e) {
		isAnchoring = (e.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) != 0;
		isStamping = (e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0;
		FrameMouseActions.getInstance().mousePressed(e);
	}

	public void mouseReleased(MouseEvent e) {
		
		// Widget -> delta IT time
		Map<LinkedTrack, Mutable.Long> anchoredLinkedTracks = null;
		Map<SampledTrack, Mutable.Long> anchoredTracks = null;
		
		if (isAnchoring) {
			if (FreeItems.getInstance().size() > 1) {
				List<InteractiveWidget> anchoringWidgets = ItemUtils.extractWidgets(FreeItems.getInstance());

				Mutable.Long firstInitTime = null;
				anchoredLinkedTracks = new HashMap<LinkedTrack, Mutable.Long>();
				anchoredTracks = new HashMap<SampledTrack, Mutable.Long>();
				
				for (InteractiveWidget iw : anchoringWidgets) {
					
					Mutable.Long it = null;
					
					if (iw instanceof LinkedTrack) {
						
						LinkedTrack lt = (LinkedTrack)iw;
						it = lt.getInitiationTimeFromMeta();
						if (it == null) it = Mutable.createMutableLong(0);
						
						anchoredLinkedTracks.put(lt, it);
						
					} else if (iw instanceof SampledTrack) {
						
						SampledTrack st = (SampledTrack)iw;
						it = st.getInitiationTimeFromMeta();
						if (it == null) it = Mutable.createMutableLong(0);
						
						anchoredTracks.put(st, it);
					
					} else continue;
					
					if (firstInitTime == null) {
						firstInitTime = Mutable.createMutableLong(it.value); // Important to crewate new instance
					} else if (it.value < firstInitTime.value) {
						firstInitTime = Mutable.createMutableLong(it.value); // Important to crewate new instance
					}
				}
				
				// Should do a accurate anchor with init times properly aligned?
				if ((anchoredLinkedTracks.size() + anchoredTracks.size()) > 1) {
					
					assert(firstInitTime != null);
					
					// Then calc all the deltas
					for (LinkedTrack lt : anchoredLinkedTracks.keySet()) {
						Mutable.Long it = anchoredLinkedTracks.get(lt);
						it.value -= firstInitTime.value;
					}
					
					for (SampledTrack st : anchoredTracks.keySet()) {
						Mutable.Long it = anchoredTracks.get(st);
						it.value -= firstInitTime.value;
					}
	
				} else {
					anchoredLinkedTracks = null;
					anchoredTracks = null;
				}
			
			}
			anchoringItems.clear();
			anchoringItems.addAll(FreeItems.getInstance());
		}
		

		FrameMouseActions.getInstance().mouseReleased(e);
		
		// If anchored a group of tracks .. adjust initiation times to
		if (anchoredLinkedTracks != null && FreeItems.getInstance().isEmpty()) {
			
			Mutable.Long firstInitTime = null;
			
			for (LinkedTrack lt : anchoredLinkedTracks.keySet()) {
				Mutable.Long it = anchoredLinkedTracks.get(lt);
				if (it.value == 0) {
					firstInitTime = lt.getInitiationTimeFromMeta();
					break;
				}
			}
			if (firstInitTime == null) {
				for (SampledTrack st : anchoredTracks.keySet()) {
					Mutable.Long it = anchoredTracks.get(st);
					if (it.value == 0) {
						firstInitTime = st.getInitiationTimeFromMeta();
						break;
					}
				}
			}
			assert(firstInitTime != null);
			
			for (LinkedTrack lt : anchoredLinkedTracks.keySet()) {
				Mutable.Long it = anchoredLinkedTracks.get(lt);
				if (it.value == 0) continue;
				lt.setInitiationTime(firstInitTime.value + it.value);
			}
			
			for (SampledTrack st : anchoredTracks.keySet()) {
				Mutable.Long it = anchoredTracks.get(st);
				if (it.value == 0) continue;
				st.setInitiationTime(firstInitTime.value + it.value);
			}
			
		}
		
		isAnchoring = false;
		isStamping = false;
		anchoringItems.clear();
	}

	/**
	 * @return
	 * 		True if the user is restricting movement on the y-axis only
	 */
	public static boolean isYAxisRestictionOn() {
		return AudioFrameKeyboardActions.isControlDown && !AudioFrameKeyboardActions.isShiftDown;
	}
	
	public static boolean isSnapOn() {
		return AudioFrameKeyboardActions.isShiftDown && !AudioFrameKeyboardActions.isControlDown;
	}
	
	public static boolean isMouseAnchoring() {
		return isAnchoring;
	}
	
	public static boolean isMouseStamping() {
		return isStamping;
	}
	
	public static List<Item> getAnchoringItems() {
		return anchoringItems;
	}
	
}
