package org.apollo.gui;

import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.structure.AbstractTrackGraphNode;
import org.apollo.audio.structure.AudioStructureModel;
import org.apollo.audio.structure.LinkedTracksGraphNode;
import org.apollo.audio.structure.OverdubbedFrame;
import org.apollo.audio.structure.TrackGraphLoopException;
import org.apollo.audio.structure.TrackGraphNode;
import org.apollo.audio.util.Timeline;
import org.apollo.io.AudioIO;
import org.apollo.io.AudioPathManager;
import org.apollo.mvc.AbstractSubject;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.Mutable;
import org.apollo.widgets.LinkedTrack;
import org.apollo.widgets.SampledTrack;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.DisplayIOObserver;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FreeItems;
import org.expeditee.gui.MouseEventRouter;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;

/**
 * A daemon that lays out overdubbed frames.
 * 
 * It is a singletone. Also it is a subject which raises empty events whenever
 * a new timeline is created.
 * 
 * @author Brook Novak
 *
 */
public class FrameLayoutDaemon extends AbstractSubject implements Observer, DisplayIOObserver {
	
	/** The frame that should not be layout out at any point in time. */
	private Frame suspendedFrame = null;
	
	/** The state info passed while suspending layout on {@link #suspendedFrame} */
	private Object suspendendUserData = null;
	
	private LayoutDaemon daemon = null;
	private Object daemonWaker = new Object();
	private boolean layoutInvalid = false;
	
	private Timeline currentTimeline = null;
	private Frame timelineOwner = null;
	
	private int leftMargin = DEFAULT_LAYOUT_LEFT_MARGIN;
	private int rightMargin = DEFAULT_LAYOUT_RIGHT_MARGIN;

    /*	
	public static final int DEFAULT_LAYOUT_RIGHT_MARGIN = 30;
	public static final int DEFAULT_LAYOUT_LEFT_MARGIN = 30;
	public static final int MAX_TIMELINE_MARGIN_AREA = 400;
    */

	public static final int DEFAULT_LAYOUT_RIGHT_MARGIN = 300;
	public static final int DEFAULT_LAYOUT_LEFT_MARGIN = 100;
	public static final int MAX_TIMELINE_MARGIN_AREA = 500;


	public static final int LAYOUT_MAX_INITIATION_PIXEL = 100;
	public static final int MIN_TRACK_WIDGET_WIDTH = 20;
	public static final int FREE_SPACE_MARGINS = 30;
	
	public static final int TRACK_WIDGET_HEIGHT = 100;
	
	public static final int TRACK_WIDGET_DEFAULT_WIDTH = 350;

	private static FrameLayoutDaemon instance = new FrameLayoutDaemon();
	
	/**
	 * Singleton constructor,
	 */
	private FrameLayoutDaemon() {
		daemon = new LayoutDaemon();
		daemon.start();
		AudioStructureModel.getInstance().addObserver(this);
		DisplayIO.addDisplayIOObserver(this);
		
		// Listen for resize events
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				// Keep expanded view centered to the browser window
				if (Browser._theBrowser != null) {
					Browser._theBrowser.addComponentListener(new ComponentListener() {

						public void componentHidden(ComponentEvent e) {
						}

						public void componentMoved(ComponentEvent e) {
						}

						public void componentResized(ComponentEvent e) {
							forceRecheck();
						}

						public void componentShown(ComponentEvent e) {
						}
						
					});
				}
				
			}
		});
		
	}

	/**
	 * {@inheritDoc}
	 */
	public void frameChanged() {
		forceRecheck();
	}

	/**
	 * 
	 * @return
	 * 		The singleton instance.
	 */
	public static FrameLayoutDaemon getInstance() {
		return instance;
	}
	
	/**
	 * @see #getTimelineOwner() for determining which frame the timeline was for.
	 * 
	 * @return
	 * 		The last format hence the current timeline. Can be null
	 * 		if no format has not occured yet or last format
	 * 		had no timeline... i.e no tracks were found...
	 */
	public Timeline getLastComputedTimeline() {
		return currentTimeline;
	}
	
	/**
	 * @see #getLastComputedTimeline()
	 * 
	 * @return
	 * 		Can be null if a timeline has not yet been computed.
	 */
	public Frame getTimelineOwner() {
		return timelineOwner;
	}
	
	public void forceRecheck() {
		assert(daemon != null && daemon.isAlive());
		layoutInvalid = true;
		synchronized(daemonWaker) {
			daemonWaker.notify();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Subject getObservedSubject() {
		return AudioStructureModel.getInstance();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setObservedSubject(Subject parent) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modelChanged(Subject source, SubjectChangedEvent event) {
		if (event.getID() != ApolloSubjectChangedEvent.NAME_CHANGED)
			forceRecheck();
	}
	
	private void setCurrentTimeline(Timeline newTimeline, Frame timelineOwner) {
		this.currentTimeline = newTimeline;
		this.timelineOwner = timelineOwner;
		fireSubjectChanged(SubjectChangedEvent.EMPTY);
	}

	/**
	 * Suspends the layout daemon from laying out a frame.
	 * Resumes any previous suspended layouts.
	 * 
	 * @see #resumeLayout(Object)
	 * 
	 * @param frame
	 * 		The frame to suspend
	 * 
	 * @param userData
	 * 		Any user data to attatch to frame layout.
	 * 
	 */
	public void suspendLayout(Frame frame, Object userData) {
		assert(frame != null);
		
		resumeLayout(null);
		
		suspendedFrame = frame;
		suspendendUserData = userData;
		
	}
	
	/**
	 * Resumes the suspended layout if has one.
	 * 
	 * @see #suspendLayout(Frame, Object)
	 * 
	 * @param userData
	 * 	 	Null to resume layout regardless, otherwise it will only resume
	 * 		layout if the user data matches the user data passed when calling
	 * 		the suspension.
	 */
	public void resumeLayout(Object userData) {
		if (suspendedFrame == null) return;
		
		if (userData == null || userData == suspendendUserData) {
			suspendedFrame = null;
			suspendendUserData = null;
		}
		
	}

	public int getLeftMargin() {
		return leftMargin;
	}

	public int getRightMargin() {
		return rightMargin;
	}
	
	/**
	 * Sets the layout margins. Values are clamped.
	 * 
	 * @param left
	 * 		The left margin in pixels.
	 * 
	 * @param right
	 * 		The right margin in pixels.
	 * 
	 */
	public void setTimelineMargins(int left, int right) {
		
		if (left < 0) left = 0;
		if (right < 0) right = 0;
		
		if ((left + right) >= MAX_TIMELINE_MARGIN_AREA) {
			left = right = (MAX_TIMELINE_MARGIN_AREA / 2);
		}

		leftMargin = left;
		rightMargin = right;
	}
	
	/**
	 * Lays out frames according to the current frame state...
	 * 
	 * @author Brook Novak
	 *
	 */
	private class LayoutDaemon extends Thread {
		
		private TrackFormatter trackFormatter = new TrackFormatter();
		
		LayoutDaemon() {
			super("TrackLayoutDaemon");
			super.setDaemon(true);
		}
		
		public void run() {
			
			while (true) { // keep daemon running for app lifetime
				
				if (!layoutInvalid) {
					try {
						synchronized(daemonWaker) {
							daemonWaker.wait();
						}
					} catch (InterruptedException e) {
						// Time to do some layout
					}
				}
				
				layoutInvalid = false;
				
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
				
				
				// Note that accessing expeditee model in another thread --
				// Being real careful not to do anything unsafe
				Frame currentFrame = DisplayIO.getCurrentFrame();
				if (currentFrame == null) continue;

				boolean hasFetched = false;
				do {
					try {
						AudioStructureModel.getInstance().fetchGraph(currentFrame.getName());
						hasFetched = true;
					} catch (InterruptedException e) { // cancelled
						/* Consume */
					} catch (TrackGraphLoopException e) { // contains loop
						e.printStackTrace();
						break;
					}
				} while (!hasFetched);
				
				trackFormatter.toFormat = currentFrame;
				
				try {
					SwingUtilities.invokeAndWait(trackFormatter);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
				
			}
	
		}
		
		private class TrackFormatter implements Runnable {
			
			Frame toFormat = null;

			public void run() {
				
				assert(toFormat != null);
				
				// To late?
				if (toFormat != DisplayIO.getCurrentFrame()) return;
				
				boolean supressSpatialLayout = suspendedFrame == toFormat;
	
				long firstInitiationTime = 0;
				int initiationXPixel = 0;
				int rightMostPixel = 0; // FOR SUSPENDED LAYOUTS ONLY
				long longestEndTime = 0;
				boolean hasRunningTime = false; // A flag set to true if there is actual audio to [lay
				
				Map<InteractiveWidget, AbstractTrackGraphNode> widgetsToFormat = new HashMap<InteractiveWidget, AbstractTrackGraphNode>();
	
				for (InteractiveWidget iw : toFormat.getInteractiveWidgets()) {
					
					AbstractTrackGraphNode abinf = null;

					if (iw instanceof SampledTrack) {
						SampledTrack sampledTrackWidget = (SampledTrack)iw;
			

						abinf = AudioStructureModel.getInstance().getTrackGraphInfo(
								sampledTrackWidget.getLocalFileName(), 
								toFormat.getName());

				
					} else if (iw instanceof LinkedTrack) { 
						LinkedTrack linkedTrackWidget = (LinkedTrack)iw;
						
						if (linkedTrackWidget.shouldLayout()) {
							// TrackGraphModel provides best data
							abinf = AudioStructureModel.getInstance().getLinkedTrackGraphInfo(
									linkedTrackWidget.getVirtualFilename(), 
									toFormat.getName());
						}
						
					}
					
					if (abinf != null) {
						
						if (widgetsToFormat.isEmpty() || 
								abinf.getInitiationTime() < firstInitiationTime) {
							firstInitiationTime = abinf.getInitiationTime();
							initiationXPixel = iw.getX();
						}
						
						// Remember for suspended layouts
						if (widgetsToFormat.isEmpty() || 
								(iw.getX() + iw.getWidth()) > rightMostPixel) {
							rightMostPixel = iw.getX() + iw.getWidth();
						}
						
						
						long endTime = abinf.getInitiationTime() + abinf.getRunningTime();
						if (widgetsToFormat.isEmpty() || endTime > longestEndTime) {
							longestEndTime = endTime;
						}
						
						widgetsToFormat.put(iw, abinf);
						
						// Set flag 
						if (!hasRunningTime) {
							hasRunningTime = abinf.getRunningTime() > 0;
						}
						
					}
						
				}
				
				// Anything to format?
				if (!hasRunningTime || 
						widgetsToFormat.isEmpty() || 
						longestEndTime == firstInitiationTime) {
					
					// NOTE: longestEndTime == firstInitiationTime indicates linked tracks 
					// linking to no actual audio data.
					
					// Reset the current timeline
					setCurrentTimeline(null, toFormat);
					return;
				}
				
				assert (longestEndTime > firstInitiationTime);
				assert(rightMostPixel >= initiationXPixel);
				
				int timelineWidth = TRACK_WIDGET_DEFAULT_WIDTH;
				
				if (widgetsToFormat.size() > 1) {
					
					// Ensure that left most pixel is at a reasonable position
					if (!supressSpatialLayout && (initiationXPixel < 0 ||
							(initiationXPixel > Browser._theBrowser.getWidth() && Browser._theBrowser.getWidth() > leftMargin))) {
						initiationXPixel = leftMargin;
					} else if (!supressSpatialLayout && initiationXPixel > LAYOUT_MAX_INITIATION_PIXEL) {
						initiationXPixel = LAYOUT_MAX_INITIATION_PIXEL;
					}

					if (supressSpatialLayout) {
						timelineWidth = rightMostPixel - initiationXPixel;
					} else {
						timelineWidth = Browser._theBrowser.getWidth() - initiationXPixel - rightMargin;
					}
					
					if (timelineWidth < 0) timelineWidth = 1;
					
				} else {
					
					// If only has one widget - the time line pixel length is set to whatever that is set to
					timelineWidth = widgetsToFormat.keySet().iterator().next().getWidth();
					
				}
				
				// Set the new timelinetimelineWidth
				setCurrentTimeline(
						new Timeline(
								firstInitiationTime,
								longestEndTime - firstInitiationTime,
								initiationXPixel, 
								timelineWidth),
								toFormat);

				// Do the actual formatting
				if (!supressSpatialLayout) {
					
					for (InteractiveWidget iw : widgetsToFormat.keySet()) {
						
						AbstractTrackGraphNode atgi = widgetsToFormat.get(iw);
						assert(atgi != null);
						
						// Update width first
						int width = (int)((double)atgi.getRunningTime() / currentTimeline.getTimePerPixel());
						
						// Clamp to something sensible
						if (width < MIN_TRACK_WIDGET_WIDTH) 
							width = MIN_TRACK_WIDGET_WIDTH;
						
						Collection<Item> enclosed = iw.getItems().get(0).getEnclosedItems();
						Map<Item, Float> enclosedPortions = new HashMap<Item, Float>();
						if (enclosed != null) {
							for (Item i : enclosed) {
								if (!(i instanceof WidgetCorner || i instanceof WidgetEdge)) {
									float f = i.getX() - iw.getX();
									f /= iw.getWidth();
									enclosedPortions.put(i, f);
								}
								
							}
						}
						
						if (widgetsToFormat.size() == 1) {
							
							if (iw.isFixedSize()) {
								iw.setSize(
										-1, -1,   // minWidth, maxWidth, 
										iw.getHeight(), iw.getHeight(),  // minHeight, maxHeight
										iw.getWidth(), iw.getWidth());  // newWidth, newHeight
							}
							
							
						} else {
						
							if (iw.getWidth() != width || !iw.isFixedSize()) {
								iw.setSize(
										width, width,   // minWidth, maxWidth, 
										iw.getHeight(), iw.getHeight(),  // minHeight, maxHeight
										width, width);  // newWidth, newHeight
							}
							
						}
	
						// Update position last
						int xpos = currentTimeline.getXAtMSTime(atgi.getInitiationTime());	
						iw.setPosition(xpos, iw.getY());
						
						for (Item i : enclosedPortions.keySet()) {
							i.setX(xpos + (enclosedPortions.get(i) * iw.getWidth()));
						}
	
					}
	
				}
				
				// Also since this daemons responisiblilty it to layout track widgets on frames ...
				// it should also take care of free space items
			
				Item i = FreeItems.getItemAttachedToCursor();
				InteractiveWidget freespaceTrackToFormat = null;
				if (i != null && i instanceof WidgetEdge) {
					freespaceTrackToFormat = ((WidgetEdge)i).getWidgetSource();
				} else if (i != null && i instanceof WidgetCorner) {
					freespaceTrackToFormat = ((WidgetCorner)i).getWidgetSource();
				}
			
				if (freespaceTrackToFormat != null) {
					
					int width = -1;
					if (freespaceTrackToFormat instanceof SampledTrack) {
						
						long rt = ((SampledTrack)freespaceTrackToFormat).getRunningMSTimeFromRawAudio();
						if (rt <= 0) rt = ((SampledTrack)freespaceTrackToFormat).getRunningMSTimeFromMeta();
						if (rt > 0) {
							width = (int)(currentTimeline.getPixelPerTime() * rt);
						}
		
					} else if (freespaceTrackToFormat instanceof LinkedTrack) {
						
						long rt = ((LinkedTrack)freespaceTrackToFormat).getRunningMSTimeFromMeta();
						if (rt > 0) {
							width = (int)(currentTimeline.getPixelPerTime() * rt);
						}
					}
					
					if (width > 0) {
						
						if (width > (Browser._theBrowser.getWidth() - (2 * FREE_SPACE_MARGINS)))
							width = Browser._theBrowser.getWidth() - (2 * FREE_SPACE_MARGINS);

						freespaceTrackToFormat.setSize(
								width, width, 
								freespaceTrackToFormat.getHeight(), freespaceTrackToFormat.getHeight(), 
								width, freespaceTrackToFormat.getHeight());
						
						// Keep the floating track on the cursor
						MouseEvent me = MouseEventRouter.getCurrentMouseEvent();
						Point containerPoint = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), Browser._theBrowser.getContentPane());
						
						if (!ItemUtils.expandRectangle(freespaceTrackToFormat.getBounds(), 20).contains(containerPoint)) {
							// If due to resize the track is not longer over the cursor then just center it.
							int offsetX = width / 2; 
							int offsetY = freespaceTrackToFormat.getHeight() / 2;
							freespaceTrackToFormat.setPosition(
									containerPoint.x - offsetX, 
									containerPoint.y - offsetY);
							FrameMouseActions.resetOffset();
		
						}
						
		
						
		
					}
					
				}
				
			}
			
		}
		
	}
	
	/**
	 * Selects best timeline for given frame - may have to infer.
	 * 
	 * @param targetFrame
	 * 
	 * @return
	 * 		The best timeline for the given frame - null if could not infer (if had to).
	 */
	public Timeline getTimeline(Frame targetFrame) {
		assert(targetFrame != null);
		
		if (currentTimeline != null && targetFrame == timelineOwner) return currentTimeline;
		
		return inferTimeline(targetFrame);
	}

	/**
	 * Determines the ms time at a x pixel poistion on a target frame.
	 * 
	 * @see #getMSTimeAndWidth(int, long, Frame) for efficient use.
	 * 
	 * @param x
	 * 		The x pixel posotion to determine the ms time from.
	 * 
	 * @param targetFrame
	 * 		The target frame that the ms will be derived from.
	 * 
	 * @return
	 * 		The ms time at the x pixel poistion.
	 */
	public long getMSAtX(int x, Frame targetFrame) {
		return getMSAtX(x, getTimeline(targetFrame));
	}
	
	private long getMSAtX(int x, Timeline tl) {
		if (tl != null) return tl.getMSTimeAtX(x);
		return 0; // Default to zero with no timeline available.
	}
	
	/**
	 * @param ms
	 * 		The time to translate the x position from
	 * 
	 * @param targetFrame
	 * 		The frame to determine the timeline information from
	 * 
	 * @return
	 * 		Null if the timeline for the target frame could not
	 * 		be determined/inferred or has no timeline. Otherwise the
	 * 		X pixal position at the given time.
	 */
	public Mutable.Long getXAtMS(long ms, Frame targetFrame) {
		assert(targetFrame != null);
		
		Timeline tl = getTimeline(targetFrame);
		if (tl == null) return null;
		
		return Mutable.createMutableLong(tl.getXAtMSTime(ms));
	}
	
	/**
	 * Determines the pixel width from a time range (ms) for a target frame.
	 * <b>NOTE</B> Width is clamped to it is never smaller than {@link #MIN_TRACK_WIDGET_WIDTH}
	 * 
	 * @see #getMSTimeAndWidth(int, long, Frame) for efficient use.
	 * 
	 * @param ms
	 * 		The amount of time to determine a width from (in milliseconds)
	 * 
	 * @param targetFrame
	 * 		The target frame that the <i>clamped</i> width will be derived from.
	 * 
	 * @return
	 * 		The width in pixels of the ms range. -1 if there is no timeline
	 */
	public int getWidthFromMS(long ms, Frame targetFrame) {
		return getWidthFromMS(ms, getTimeline(targetFrame));
	}
	
	private int getWidthFromMS(long ms, Timeline tl) {
		int width = -1;
		if (tl != null) {
			width = (int)((double)ms / tl.getTimePerPixel());
		}	
		
		// Clamp width to something reasonable.
		if (width > 0 && width < MIN_TRACK_WIDGET_WIDTH) width = MIN_TRACK_WIDGET_WIDTH;

		return width;
	}
	
	/**
	 * Gets the ms time and width in one call - more efficient than
	 * calling {@link #getMSAtX(int, Frame)} and {@link #getWidthFromMS(long, Frame)}
	 * seperatly.
	 * 
	 * <b>NOTE</B> Width is clamped to it is never smaller than {@link #MIN_TRACK_WIDGET_WIDTH}
	 * 
	 * @param x
	 * 		The x pixel posotion to determine the ms time from.
	 * 
	 * @param ms
	 * 		The amount of time to determine a width from (in milliseconds)
	 * 
	 * @param targetFrame
	 * 		The target frame that the ms and width will be derived from.
	 * 
	 * @return
	 * 		A Two dimensional array of the ms time and <i>clamped</i> width, respectivly.
	 */
	public long[] getMSTimeAndWidth(int x, long ms, Frame targetFrame) {
		
		// Re-use timeline
		Timeline tl = getTimeline(targetFrame);
		
		return new long[] { getMSAtX(x, tl), getWidthFromMS(ms, tl)};
	}

	
	/**
	 * <b>MUST BE ON EXPEDITEE THREAD</b>
	 * 
	 * If the frame is not loaded in TrackGraphModel then the timeline will
	 * be a rough estimation using meta data. Otherwise it will be accurate.
	 * Fast operation - does not recurse. 
	 * 
	 * @param frame
	 * 		The target frame for inferring the timeline.
	 * 
	 * @return
	 * 		A timeline. Null if there are no track widgets fround on the frame.
	 */
	public static Timeline inferTimeline(Frame frame) {
		assert(frame != null);
		
		// Unavilable times relative to zero - and used if no availabel times exist.
		Integer unavilableInitiationXPixel = null;
		Mutable.Long unavilableLongestEndTime = null; 
		Integer unavilableLongestEndTimeRightPixel = null;

		Mutable.Long firstInitiationTime = null;
		int initiationXPixel = 0;

		long longestEndTime = -1; 
		int longestEndTimeRightPixel = 0;

		// Look for all track widgets
		for (InteractiveWidget iw : frame.getInteractiveWidgets()) {
			
			if (iw instanceof SampledTrack) {
				SampledTrack sampledTrackWidget = (SampledTrack)iw;

				// TrackGraphModel provides best data
				TrackGraphNode tgi = AudioStructureModel.getInstance().getTrackGraphInfo(
						sampledTrackWidget.getLocalFileName(), 
						frame.getName());
		
				// Is the initiation time available?
				Mutable.Long it = (tgi != null) ? 
						Mutable.createMutableLong(tgi.getInitiationTime()) :
						sampledTrackWidget.getInitiationTimeFromMeta();
				
				// If not - keep record left-most unavilable x pixel
				if (it == null && 
						(unavilableInitiationXPixel == null || iw.getX() < unavilableInitiationXPixel)) {
					unavilableInitiationXPixel = iw.getX();
				
				// If so - check to see if it is the first initiation time on the frame
				} else if(it != null && (firstInitiationTime == null ||
						it.value < firstInitiationTime.value)) {
					firstInitiationTime = it;
					initiationXPixel = iw.getX();
				}
				
				// Determine running time
				long runningTime = sampledTrackWidget.getRunningMSTimeFromRawAudio();
				
				if (runningTime <= 0 && tgi != null) {
					runningTime = tgi.getRunningTime();
					
				} else if (runningTime <= 0) {
					runningTime = sampledTrackWidget.getRunningMSTimeFromMeta();
				}
				
				// Note that this will be in rare cases .. the meta should rarely be missing
				// so cann afford to hang the thread a little for checking the actual time...
				if (runningTime <= 0) {
					File f = new File(AudioPathManager.AUDIO_HOME_DIRECTORY +
							sampledTrackWidget.getLocalFileName());
					if (f.exists()) {
						try {
							runningTime = AudioIO.getRunningTime(f.getPath());
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
				
				// If all failed, just set to one... this would be rare, and by setting to
				// 1 it will just give a incorrect timeline - nothing catistrophic.
				if (runningTime <= 0) {
					runningTime = 1;
				}
				
				if (it == null) {
					if (unavilableLongestEndTime == null ||
							runningTime > unavilableLongestEndTime.value) {
						unavilableLongestEndTime = Mutable.createMutableLong(runningTime);
						unavilableLongestEndTimeRightPixel = iw.getX() + iw.getWidth();
					}
				} else {
					long endTime = it.value + runningTime;
					if (endTime > longestEndTime) {
						longestEndTime = endTime;
						longestEndTimeRightPixel = iw.getX() + iw.getWidth();
					}
				}
				
			} else if (iw instanceof LinkedTrack) { 
				LinkedTrack linkedTrackWidget = (LinkedTrack)iw;
	
				// TrackGraphModel provides best data
				LinkedTracksGraphNode ltgi = AudioStructureModel.getInstance().getLinkedTrackGraphInfo(
						linkedTrackWidget.getVirtualFilename(), 
						frame.getName());

				Mutable.Long it = (ltgi != null) ? 
						Mutable.createMutableLong(ltgi.getInitiationTime()) :
							linkedTrackWidget.getInitiationTimeFromMeta();
						
				if (it == null && 
						(unavilableInitiationXPixel == null || iw.getX() < unavilableInitiationXPixel)) {
					unavilableInitiationXPixel = iw.getX();
				} else if(it != null && (firstInitiationTime == null ||
						it.value < firstInitiationTime.value)) {
					firstInitiationTime = it;
					initiationXPixel = iw.getX();
				}
				
				// Determine running time
				long runningTime = (ltgi != null) ?
						ltgi.getRunningTime() :
							linkedTrackWidget.getRunningMSTimeFromMeta();

				// Note that not recusing through the links because the frames might
				// not be loaded yet. The TrackGraphModel handles such operations - 
				// and it would be to expensive / overkill to frame search here...
	
				
				// If all failed, just set to one...
				if (runningTime <= 0) {
					runningTime = 1;
				}

				if (it == null) {
					if (unavilableLongestEndTime == null ||
							runningTime > unavilableLongestEndTime.value) {
						unavilableLongestEndTime = Mutable.createMutableLong(runningTime);
						unavilableLongestEndTimeRightPixel = iw.getX() + iw.getWidth();
					}
				} else {
					long endTime = it.value + runningTime;
					if (endTime > longestEndTime) {
						longestEndTime = endTime;
						longestEndTimeRightPixel = iw.getX() + iw.getWidth();
					}
				}

			}
			
		}
		
		if (firstInitiationTime == null) firstInitiationTime = Mutable.createMutableLong(0L);
		
		if (longestEndTime > 0) {
			
			assert(longestEndTime > firstInitiationTime.value);
			assert(longestEndTimeRightPixel > initiationXPixel);
			
			long rttime = longestEndTime - firstInitiationTime.value;
			int pxWidth = longestEndTimeRightPixel - initiationXPixel;
			if (unavilableLongestEndTime != null &&
					unavilableLongestEndTime.value > rttime) {
				assert(unavilableInitiationXPixel != null);
				rttime = unavilableLongestEndTime.value;
				pxWidth = unavilableLongestEndTimeRightPixel - unavilableInitiationXPixel;
			}

			return new Timeline(
					firstInitiationTime.value, 
					rttime, 
					initiationXPixel,
					pxWidth);
			
		} else if(unavilableInitiationXPixel != null) { // default
			
			assert(unavilableInitiationXPixel != null);
			assert(unavilableLongestEndTimeRightPixel != null);
			
			return new Timeline(
					0, 
					unavilableLongestEndTime.value, 
					unavilableInitiationXPixel,
					unavilableLongestEndTimeRightPixel - unavilableInitiationXPixel);
			
		}
		
		return null;

	}
	
	/**
	 * @return
	 * 		The total ms time for the current frame. zero or negative if failed or there are no
	 * 		tracks to play.
	 * 		
	 */
	public static long inferCurrentTotalMSTime() {
		
		Frame currentFrame = DisplayIO.getCurrentFrame();
		String currentFrameName = (currentFrame != null) ? currentFrame.getName() : null;
		
		if (currentFrameName == null) return -1;

		OverdubbedFrame odf =  AudioStructureModel.getInstance().getOverdubbedFrame(currentFrameName);
		
		if (odf != null) {
			return odf.calculateRunningTime();
		}
		
		// Infer time from meta
		Timeline tl = FrameLayoutDaemon.inferTimeline(currentFrame);
		
		if (tl != null) {
			return tl.getRunningTime();
		}
		
		return -1;
		
	}

	
}
