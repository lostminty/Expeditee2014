package org.apollo;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.apollo.util.Mutable;
import org.apollo.widgets.LinkedTrack;
import org.apollo.widgets.SampledTrack;
import org.apollo.widgets.TrackWidgetCommons;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameKeyboardActions;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.FreeItems;
import org.expeditee.items.UserAppliedPermission;
import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Permission;
import org.expeditee.items.Text;
import org.expeditee.items.Item.HighlightMode;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;

/**
 * Apollos keyboards actions extensions.
 * 
 * @author Brook Novak
 *
 */
public class AudioFrameKeyboardActions implements KeyListener {
	
	static boolean isControlDown = false;
	static boolean isShiftDown = false;
	
	private static final long TRACK_WIDGET_TIMELINE_ADJUSTMENT = 50; // In audio-frames
	private static final int TRACK_WIDGET_VERTICLE_ADJUSTMENT = 5; // In Pixels

	public void keyPressed(KeyEvent e) {
		
		isControlDown = e.isControlDown();
		isShiftDown = e.isShiftDown();
		
		boolean forwardToExpeditee = true;
		
		switch(e.getKeyCode()) { 
		
			case KeyEvent.VK_CONTROL:
				FrameGraphics.refresh(false); // For special post effects
				break;
				
			// Ultra-fine timeline placement of audio tracks
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_LEFT:
				
				if (isControlDown) {
					InteractiveWidget selectedIW = null;
					
					if (FrameMouseActions.getlastHighlightedItem() != null &&
							FrameMouseActions.getlastHighlightedItem() instanceof WidgetEdge) {
						selectedIW = ((WidgetEdge)FrameMouseActions.getlastHighlightedItem()).getWidgetSource();
					} else if (FrameMouseActions.getlastHighlightedItem() != null &&
							FrameMouseActions.getlastHighlightedItem() instanceof WidgetCorner) {
						selectedIW = ((WidgetCorner)FrameMouseActions.getlastHighlightedItem()).getWidgetSource();
					}
					
					if (selectedIW != null && selectedIW instanceof SampledTrack) {
						adjustInitationTime((SampledTrack)selectedIW, e.getKeyCode() == KeyEvent.VK_LEFT);
						forwardToExpeditee = false;
					} else if (selectedIW != null && selectedIW instanceof LinkedTrack) {
						adjustInitationTime((LinkedTrack)selectedIW, e.getKeyCode() == KeyEvent.VK_LEFT);
						forwardToExpeditee = false;
					}
							
				}

				break;
				
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
				
				if (isControlDown) {
					InteractiveWidget selectedIW = null;
					
					if (FrameMouseActions.getlastHighlightedItem() != null &&
							FrameMouseActions.getlastHighlightedItem() instanceof WidgetEdge) {
						selectedIW = ((WidgetEdge)FrameMouseActions.getlastHighlightedItem()).getWidgetSource();
					} else if (FrameMouseActions.getlastHighlightedItem() != null &&
							FrameMouseActions.getlastHighlightedItem() instanceof WidgetCorner) {
						selectedIW = ((WidgetCorner)FrameMouseActions.getlastHighlightedItem()).getWidgetSource();
					}
					
					if (selectedIW != null && selectedIW instanceof SampledTrack) {
						adjustVerticlePosition((SampledTrack)selectedIW, e.getKeyCode() == KeyEvent.VK_UP);
						forwardToExpeditee = false;
					} else if (selectedIW != null && selectedIW instanceof LinkedTrack) {
						adjustVerticlePosition((LinkedTrack)selectedIW, e.getKeyCode() == KeyEvent.VK_UP);
						forwardToExpeditee = false;
					}
							
				}

				break;
				
			case KeyEvent.VK_F:
				if (e.isControlDown() && e.isShiftDown()) {
					Frame current = DisplayIO.getCurrentFrame();
					int maxX = Browser._theBrowser.getContentPane().getWidth();
					int maxY = Browser._theBrowser.getContentPane().getHeight();
					
					for (Item i : current.getItems()) {
						Point pos = i.getPosition();
						pos.x %= maxX;
						pos.y %= maxY;
						i.setPosition(pos);
					}
				}
				break;
				
				// Create linked tracks on enclosed items
			case KeyEvent.VK_L:
				
				if (e.isControlDown() && FrameUtils.getCurrentItem() == null) {

					Collection<Item> enclosedItems = FrameUtils.getCurrentItems(null);
					if (enclosedItems != null) {
						
						forwardToExpeditee = false;
						Frame current = DisplayIO.getCurrentFrame();
						
						Collection<Item> toRemove = new LinkedHashSet<Item>(enclosedItems.size());
						LinkedList<LinkedTrack> enclosedLinkedTracks = new LinkedList<LinkedTrack>();
						LinkedList<SampledTrack> enclosedTracks = new LinkedList<SampledTrack>();
						
						// Get enclosing shape and enclosed tracks
						for (Item ip : enclosedItems) {
							if (ip.getParent() != current) continue;
							
							if (ip.hasPermission(UserAppliedPermission.full)) {
								// Only include lines if one of their enpoints are also  being removed
								if (ip instanceof Line) {
									Line l = (Line) ip;
									Item end = l.getEndItem();
									Item start = l.getStartItem();

									// If one end of a line is being delted, remove the
									// other end if all its connecting lines are being
									// delted
									if (enclosedItems.contains(end)) {
										if (!enclosedItems.contains(start)
												&& enclosedItems.containsAll(start.getLines())) {
											toRemove.add(start);
										}
									} else if (enclosedItems.contains(start)) {
										if (enclosedItems.containsAll(end.getLines())) {
											toRemove.add(end);
										}
									} else {
										continue;
									}
								} else if (ip instanceof WidgetCorner){
									InteractiveWidget iw = ((WidgetCorner)ip).getWidgetSource();
									if (!enclosedTracks.contains(iw) && iw instanceof SampledTrack) {
										enclosedTracks.add((SampledTrack)iw);
									} else if (!enclosedLinkedTracks.contains(iw) && iw instanceof LinkedTrack) {
										enclosedLinkedTracks.add((LinkedTrack)iw);
									}
								} else {
									if ((ip instanceof Line || ip instanceof Dot) && ip.getHighlightMode() == HighlightMode.Enclosed) {
										toRemove.add(ip);
										toRemove.addAll(ip.getConnected());
									}
								}
								
							}
						} // End searching enclosed items
						
						
						// If there was some enclosed tracks
						if (current != null && !toRemove.isEmpty() && (!enclosedLinkedTracks.isEmpty() || !enclosedTracks.isEmpty())) {
		
							// Remove enclosure
							current.removeAllItems(toRemove);
							
							// Determine initiation time of group
							Mutable.Long initTime = null;
							Point initiatePosition = null;
							
							for (SampledTrack st : enclosedTracks) {
								
								if (initTime == null) {
									initTime = st.getInitiationTimeFromMeta();
									initiatePosition = st.getPosition();
								}
								else if (st.getInitiationTimeFromMeta() != null &&
										st.getInitiationTimeFromMeta().value < initTime.value) {
									initTime = st.getInitiationTimeFromMeta();
									initiatePosition = st.getPosition();
								}
							}
							for (LinkedTrack lt : enclosedLinkedTracks) {

								if (initTime == null) {
									initTime = lt.getInitiationTimeFromMeta();
									initiatePosition = lt.getPosition();
								}
								else if (lt.getInitiationTimeFromMeta() != null &&
										lt.getInitiationTimeFromMeta().value < initTime.value) {
									initTime = lt.getInitiationTimeFromMeta();
									initiatePosition = lt.getPosition();
								}
							}
							
							assert(initTime != null);
							assert(initiatePosition != null);
							
							// Creat the link to contain all tracks
							String name = current.getTitle();
							if (name == null) name = "Unnamed";
							name += " group";
							
							Text linkSource = new Text(current.getNextItemID());
							linkSource.addToData(TrackWidgetCommons.META_NAME_TAG + name);
							linkSource.addToData(TrackWidgetCommons.META_INITIATIONTIME_TAG + initTime);
							linkSource.setPosition(initiatePosition);
							linkSource.setParent(current);
							LinkedTrack linkedVersion = new LinkedTrack(linkSource, null);
							
							// Create a new frame to hold the track group
							Frame newFrame = FrameIO.CreateNewFrame(linkedVersion.getItems().get(0));
	
							for (SampledTrack st : enclosedTracks) {
								st.saveAudio(); // Blocking
								current.removeAllItems(st.getItems());
								newFrame.addAllItems(st.getItems());
							}
							
							for (LinkedTrack lt : enclosedLinkedTracks) {
								current.removeAllItems(lt.getItems());
								newFrame.addAllItems(lt.getItems());
							}
							
							FrameIO.SaveFrame(newFrame);
							
							// Link the linked tracks
							linkedVersion.setLink(newFrame.getName(), null);
							
							// Add the new link
							current.addAllItems(linkedVersion.getItems());
							
							// Ensure initiation time is retained to the exact frame... avoiding loss due to resolution
							linkedVersion.setInitiationTime(initTime.value);

						}
						
					}

				}
				break;
		}
		
		if (forwardToExpeditee) {
			FrameKeyboardActions.getInstance().keyPressed(e);
		}
	
	}

	public void keyReleased(KeyEvent e) {
		
		boolean wasYRestricted = AudioFrameMouseActions.isYAxisRestictionOn();
		
		isControlDown = e.isControlDown();
		isShiftDown = e.isShiftDown();

		switch(e.getKeyCode()) {
			case  KeyEvent.VK_CONTROL:
				FrameGraphics.refresh(false); // For special post effects
				break;
			case KeyEvent.VK_G:
				if (isControlDown) {
					ApolloSystem.useQualityGraphics = !ApolloSystem.useQualityGraphics;
				}
				break;
				
		}
		
		// Restore free items position with respected to cursur
		if (wasYRestricted && !FreeItems.getInstance().isEmpty()) {
			// TODO
		}
		
		FrameKeyboardActions.getInstance().keyReleased(e);
	}

	public void keyTyped(KeyEvent e) {
		FrameKeyboardActions.getInstance().keyTyped(e);
	}
	

	/**
	 * @return True if control is down.
	 */
	public static boolean isControlDown() {
		return isControlDown;
	}
	
	
	/**
	 * @return True if shift is down.
	 */
	public static boolean isShiftDown() {
		return isShiftDown;
	}
	
	public static void adjustInitationTime(SampledTrack track, boolean left) {
		long frameAjdustment = TRACK_WIDGET_TIMELINE_ADJUSTMENT;
		if (left) frameAjdustment *= -1;
		Mutable.Long oldIt = track.getInitiationTimeFromMeta();
		if (oldIt == null) oldIt = Mutable.createMutableLong(0);
		track.setInitiationTime(oldIt.value + frameAjdustment);
	}
	
	public static void adjustInitationTime(LinkedTrack track, boolean left) {
		long frameAjdustment = TRACK_WIDGET_TIMELINE_ADJUSTMENT;
		if (left) frameAjdustment *= -1;
		Mutable.Long oldIt = track.getInitiationTimeFromMeta();
		if (oldIt == null) oldIt = Mutable.createMutableLong(0);
		track.setInitiationTime(oldIt.value + frameAjdustment);
	}
	
	public static void adjustVerticlePosition(SampledTrack track, boolean up) {
		int pixelAjdustment = TRACK_WIDGET_VERTICLE_ADJUSTMENT;
		if (up) pixelAjdustment *= -1;
		track.setYPosition(track.getY() + pixelAjdustment);
	}
	
	public static void adjustVerticlePosition(LinkedTrack track, boolean up) {
		int pixelAjdustment = TRACK_WIDGET_VERTICLE_ADJUSTMENT;
		if (up) pixelAjdustment *= -1;
		track.setYPosition(track.getY() + pixelAjdustment);
	}

}
