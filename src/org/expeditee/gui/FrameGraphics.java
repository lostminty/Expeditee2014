package org.expeditee.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.expeditee.items.Circle;
import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Line;
import org.expeditee.items.UserAppliedPermission;
import org.expeditee.items.XRayable;
import org.expeditee.items.Item.HighlightMode;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetEdge;
import org.expeditee.settings.UserSettings;

public class FrameGraphics {

	// the graphics used to paint with
	private static Graphics2D _DisplayGraphics;

	// the maximum size that can be used to paint on
	private static Dimension _MaxSize = null;

	// Final passes to rendering the current frame
	private static LinkedList<FrameRenderPass> _frameRenderPasses = new LinkedList<FrameRenderPass>();

	// modes
	public static final int MODE_NORMAL = 0;

	public static final int MODE_AUDIENCE = 1;

	public static final int MODE_XRAY = 2;

	// Start in XRay mode so that errors aren't thrown when parsing the profile
	// frame if it has images on it
	private static int _Mode = MODE_XRAY;

	private FrameGraphics() {
		// util constructor
	}

	/**
	 * If Audience Mode is on this method will toggle it to be off, or
	 * vice-versa. This results in the Frame being re-parsed and repainted.
	 */
	public static void ToggleAudienceMode() {
		Frame current = DisplayIO.getCurrentFrame();
		if (_Mode == MODE_XRAY) {
			ToggleXRayMode();
		}

		if (_Mode == MODE_AUDIENCE) {
			_Mode = MODE_NORMAL;
		} else {
			_Mode = MODE_AUDIENCE;
			ItemUtils.UpdateConnectedToAnnotations(current.getItems());
			for (Overlay o : current.getOverlays()) {
				ItemUtils.UpdateConnectedToAnnotations(o.Frame.getItems());
			}
			for (Vector v : current.getVectorsDeep()) {
				ItemUtils.UpdateConnectedToAnnotations(v.Frame.getItems());
			}
		}
		FrameUtils.Parse(current);
		DisplayIO.UpdateTitle();
		setMaxSize(new Dimension(_MaxSize.width, MessageBay
				.getMessageBufferHeight()
				+ _MaxSize.height));
		refresh(false);
	}

	/**
	 * If X-Ray Mode is on this method will toggle it to be off, or vice-versa.
	 * This results in the Frame being re-parsed and repainted.
	 */
	public static void ToggleXRayMode() {
		if (_Mode == MODE_AUDIENCE) {
			ToggleAudienceMode();
		}

		if (_Mode == MODE_XRAY) {
			setMode(MODE_NORMAL, true);
		} else {
			setMode(MODE_XRAY, true);
		}
		DisplayIO.getCurrentFrame().refreshSize();
		DisplayIO.UpdateTitle();
		FrameMouseActions.getInstance().refreshHighlights();
		FrameMouseActions.updateCursor();
		refresh(false);
	}

	public static void setMode(int mode, boolean parse) {
		if (_Mode == mode)
			return;
		_Mode = mode;
		if (parse) {
			Frame current = DisplayIO.getCurrentFrame();
			current.parse();
		}
	}

	/**
	 * @return True if Audience Mode is currently on, False otherwise.
	 */
	public static boolean isAudienceMode() {
		return _Mode == MODE_AUDIENCE;
	}

	/**
	 * @return True if X-Ray Mode is currently on, False otherwise.
	 */
	public static boolean isXRayMode() {
		return _Mode == MODE_XRAY;
	}

	public static void setMaxSize(Dimension max) {
		if (_MaxSize == null)
			_MaxSize = max;

		// Hide the message buffer if in audience mode
		int newMaxHeight = max.height
				- (isAudienceMode() ? 0 : MessageBay.MESSAGE_BUFFER_HEIGHT);
		if (newMaxHeight > 0) {
			_MaxSize.setSize(max.width, newMaxHeight);
		}
		Frame current = DisplayIO.getCurrentFrame();
		if (current != null) {
			current.setBuffer(null);
			current.refreshSize();
			if (DisplayIO.isTwinFramesOn()) {
				Frame opposite = DisplayIO.getOppositeFrame();

				/* When running the test suite opposite may be null! */
				if (opposite != null) {
					opposite.setBuffer(null);
					opposite.refreshSize();
				}
			}
		}

		if (newMaxHeight > 0) {
			MessageBay.updateSize();
		}
	}

	public static Dimension getMaxSize() {
		return _MaxSize;
	}

	public static Dimension getMaxFrameSize() {
		if (DisplayIO.isTwinFramesOn()) {
			return new Dimension((_MaxSize.width / 2), _MaxSize.height);
		} else
			return _MaxSize;
	}

	/**
	 * Sets the Graphics2D object that should be used for all painting tasks.
	 * Note: Actual painting is done by using g.create() to create temporary
	 * instances that are then disposed of using g.dispose().
	 * 
	 * @param g
	 *            The Graphics2D object to use for all painting
	 */
	public static void setDisplayGraphics(Graphics2D g) {
		_DisplayGraphics = g;
	}

	/*
	 * Displays the given Item on the screen
	 */
	static void PaintItem(Graphics2D g, Item i) {
		if (i == null || g == null)
			return;

		// do not paint annotation items in audience mode
		if (!isAudienceMode()
				|| (isAudienceMode() && !i.isConnectedToAnnotation() && !i
						.isAnnotation()) || i == FrameUtils.getLastEdited()) {

			Graphics2D tg = (Graphics2D) g.create();
			i.paint(tg);
			tg.dispose();
		}
	}

	/**
	 * Adds all the scaled vector items for a frame into a list. It uses
	 * recursion to get the items from nested vector frames.
	 * 
	 * @param items
	 *            the list into which the vector items will be placed.
	 * @param vector
	 *            the frame containing vecor items.
	 * @param seenVectors
	 *            the vectors which should be ignored to prevent infinate loops.
	 * @param origin
	 *            start point for this frame or null if it is a top level frame.
	 * @param scale
	 *            the factor by which the item on the vector frame are to be
	 *            scaled.
	 */
	// public static void AddAllVectorItems(List<Item> items, Vector vector,
	// Collection<Frame> seenVectors) {
	// // Check all the vector items and add the items on the vectors
	// if (seenVectors.contains(vector))
	// return;
	// seenVectors.add(vector);
	//
	// float originX = origin == null ? 0 : origin.x;
	// float originY = origin == null ? 0 : origin.y;
	//
	// for (Vector o : vector.getVectors())
	// AddAllVectorItems(items, o.Frame, new HashSet<Frame>(seenVectors),
	// new Point2D.Float(originX + o.Origin.x * scale, originY
	// + o.Origin.y * scale), o.Scale * scale,
	// o.Foreground, o.Background);
	// // if its the original frame then were done
	// if (origin == null) {
	// ItemUtils.EnclosedCheck(items);
	// return;
	// }
	// // Put copies of the items shifted to the origin of the VectorTag
	// items.addAll(ItemUtils
	// .CopyItems(vector.getNonAnnotationItems(), vector));
	//		
	// }
	/**
	 * Recursive function similar to AddAllOverlayItems.
	 * 
	 * @param widgets
	 *            The collection the widgets will be added to
	 * @param overlay
	 *            An "overlay" frame - this intially will be the parent frame
	 * @param seenOverlays
	 *            Used for state in the recursion stack. Pass as an empty
	 *            (non-null) list.
	 */
	public static void AddAllOverlayWidgets(List<InteractiveWidget> widgets,
			Frame overlay, List<Frame> seenOverlays) {
		if (seenOverlays.contains(overlay))
			return;

		seenOverlays.add(overlay);

		for (Overlay o : overlay.getOverlays())
			AddAllOverlayWidgets(widgets, o.Frame, seenOverlays);

		widgets.addAll(overlay.getInteractiveWidgets());
	}

	private static Image Paint(Frame toPaint, Area clip) {
		return Paint(toPaint, clip, true, true);
	}

	/**
	 * 
	 * @param toPaint
	 * @param clip
	 *            If null, then no clip applied.
	 * @param isActualFrame
	 * @return
	 */
	private static Image Paint(Frame toPaint, Area clip, boolean isActualFrame,
			boolean createVolitile) {
		if (toPaint == null)
			return null;

		// the buffer is not valid, so it must be recreated
		if (!toPaint.isBufferValid()) {
			Image buffer = toPaint.getBuffer();
			if (buffer == null) {
				GraphicsEnvironment ge = GraphicsEnvironment
						.getLocalGraphicsEnvironment();
				if (createVolitile) {
					buffer = ge.getDefaultScreenDevice()
							.getDefaultConfiguration()
							.createCompatibleVolatileImage(_MaxSize.width,
									_MaxSize.height);
				} else {
					buffer = new BufferedImage(_MaxSize.width, _MaxSize.height,
							BufferedImage.TYPE_INT_ARGB);
				}
				toPaint.setBuffer(buffer);
			}

			Graphics2D bg = (Graphics2D) buffer.getGraphics();
			paintFrame(toPaint, clip, isActualFrame, createVolitile, bg);

			bg.dispose();
		}

		return toPaint.getBuffer();
	}

	/**
	 * @param toPaint
	 * @param clip
	 * @param isActualFrame
	 * @param createVolitile
	 * @param bg
	 */
	public static void paintFrame(Frame toPaint, Area clip,
			boolean isActualFrame, boolean createVolitile, Graphics2D bg) {

		// Prepare render passes
		if (isActualFrame) {
			currentClip = clip;
			for (FrameRenderPass pass : _frameRenderPasses) {
				currentClip = pass.paintStarted(currentClip);
				clip = currentClip;
			}
		}

		bg.setClip(clip);

		// TODO: Revise images and clip - VERY IMPORTANT

		// Nicer looking lines, but may be too jerky while
		// rubber-banding on older machines
		if (UserSettings.AntiAlias.get())
			bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
		// If we are doing @f etc... then have a clear background if its the
		// default background color
		Color backgroundColor = null;
		// Need to allow transparency for frameImages
		if (createVolitile) {
			backgroundColor = toPaint.getPaintBackgroundColor();
		} else {
			backgroundColor = toPaint.getBackgroundColor();
			if (backgroundColor == null)
				backgroundColor = Item.TRANSPARENT;
		}

		// if(isActual)
		bg.setColor(backgroundColor);
		if (isActualFrame) {
			// bg.setColor(Color.white); // TODO: TEMP FOR DEBUGGING

			// System.out.println("paintPic");
		}
		bg.fillRect(0, 0, _MaxSize.width, _MaxSize.height);

		List<Item> visibleItems = new LinkedList<Item>();
		List<InteractiveWidget> paintWidgets;

		if (isActualFrame) {
			// Add all the items for this frame and any other from other
			// frames
			visibleItems.addAll(toPaint.getAllItems());
			paintWidgets = new LinkedList<InteractiveWidget>();
			AddAllOverlayWidgets(paintWidgets, toPaint, new LinkedList<Frame>());
		} else {
			visibleItems.addAll(toPaint.getVisibleItems());
			visibleItems.addAll(toPaint.getVectorItems());
			paintWidgets = toPaint.getInteractiveWidgets();
		}

		HashSet<Item> paintedFillsAndLines = new HashSet<Item>();
		// FIRST: Paint widgets swing gui (not expeditee gui) .
		// Note that these are the anchored widgets
		ListIterator<InteractiveWidget> widgetItor = paintWidgets
				.listIterator(paintWidgets.size());
		while (widgetItor.hasPrevious()) {
			// Paint first-in-last-serve ordering - like swing
			// If it is done the other way around then widgets are covered up by
			// the box that is supposed to be underneath
			InteractiveWidget iw = widgetItor.previous();
			if (clip == null || clip.intersects(iw.getComponant().getBounds())) {
				iw.paint(bg);
				PaintItem(bg, iw.getItems().get(4));
				paintedFillsAndLines.addAll(iw.getItems());
			}
		}

		// Filter out items that do not need to be painted
		List<Item> paintItems;
		HashSet<Item> fillOnlyItems = null; // only contains items that do
		// not need drawing but fills
		// might

		if (clip == null) {
			paintItems = visibleItems;
		} else {
			fillOnlyItems = new HashSet<Item>();
			paintItems = new LinkedList<Item>();
			for (Item i : visibleItems) {
				if (i.isInDrawingArea(clip)) {
					paintItems.add(i);
				} else if (i.isEnclosed()) {
					// just add all fill items despite possibility of fills
					// not being in clip
					// because it will be faster than having to test twice
					// for fills that do need
					// repainting.
					fillOnlyItems.add(i);
				}
			}
		}
		// Only paint files and lines once ... between anchored AND free
		// items
		PaintPictures(bg, paintItems, fillOnlyItems, paintedFillsAndLines);
		PaintLines(bg, visibleItems);

		// Filter out free items that do not need to be painted
		// This is efficient in cases with animation while free items exist

		List<Item> freeItemsToPaint = new LinkedList<Item>();
		// Dont paint the free items for the other frame in twin frames mode
		// if (toPaint == DisplayIO.getCurrentFrame()) {
		if (clip == null) {
			freeItemsToPaint = FreeItems.getInstance();
		} else {
			freeItemsToPaint = new LinkedList<Item>();
			fillOnlyItems.clear();
			for (Item i : FreeItems.getInstance()) {
				if (i.isInDrawingArea(clip)) {
					freeItemsToPaint.add(i);
				} else if (i.isEnclosed()) {
					fillOnlyItems.add(i);
				}
			}
		}
		// }

		if (isActualFrame && toPaint == DisplayIO.getCurrentFrame())
			PaintPictures(bg, freeItemsToPaint, fillOnlyItems,
					paintedFillsAndLines);
		// TODO if we can get transparency with FreeItems.getInstance()...
		// then text can be done before freeItems
		PaintNonLinesNonPicture(bg, paintItems);

		// toPaint.setBufferValid(true);

		if (isActualFrame && !isAudienceMode()) {
			PaintItem(bg, toPaint.getNameItem());
		}

		if (DisplayIO.isTwinFramesOn()) {
			List<Item> lines = new LinkedList<Item>();
			for (Item i : freeItemsToPaint) {
				if (i instanceof Line) {
					Line line = (Line) i;

					if (toPaint == DisplayIO.getCurrentFrame()) {
						// If exactly one end of the line is floating...

						if (line.getEndItem().isFloating()
								^ line.getStartItem().isFloating()) {
							// Line l = TransposeLine(line,
							// line.getEndItem(),
							// toPaint, 0, 0);
							// if (l == null)
							// l = TransposeLine(line,
							// line.getStartItem(), toPaint, 0, 0);
							// if (l == null)
							// l = line;
							// lines.add(l);
						} else {
							// lines.add(line);
						}
					} else {
						// if (line.getEndItem().isFloating()
						// ^ line.getStartItem().isFloating()) {
						// lines.add(TransposeLine(line,
						// line.getEndItem(), toPaint,
						// FrameMouseActions.getY(), -DisplayIO
						// .getMiddle()));
						// lines.add(TransposeLine(line, line
						// .getStartItem(), toPaint,
						// FrameMouseActions.getY(), -DisplayIO
						// .getMiddle()));
						// }
					}
				}
			}
			if (isActualFrame)
				PaintLines(bg, lines);
		} else {
			// Dont paint the
			if (isActualFrame)
				PaintLines(bg, freeItemsToPaint);
		}

		if (isActualFrame && toPaint == DisplayIO.getCurrentFrame())
			PaintNonLinesNonPicture(bg, freeItemsToPaint);

		// Repaint popups / drags... As well as final passes
		if (isActualFrame) {
			for (FrameRenderPass pass : _frameRenderPasses) {
				pass.paintPreLayeredPanePass(bg);
			}
			PopupManager.getInstance().paintLayeredPane(bg, clip);
			for (FrameRenderPass pass : _frameRenderPasses) {
				pass.paintFinalPass(bg);
			}
		}
		
		// paint tooltip
		if(!FreeItems.itemsAttachedToCursor()) {
			Item current = FrameUtils.getCurrentItem();
			if(current != null) {
				current.paintTooltip(bg);
			} else {
				Item.clearTooltipOwner();
			}
		}

		if (FreeItems.hasCursor()
				&& DisplayIO.getCursor() == Item.DEFAULT_CURSOR)
			PaintNonLinesNonPicture(bg, FreeItems.getCursor());
	}

	// creates a new line so that lines are shown correctly when spanning
	// across frames in TwinFrames mode
	// private static Line TransposeLine(Line line, Item d, Frame toPaint,
	// int base, int adj) {
	// Line nl = null;
	//
	// if (toPaint != DisplayIO.getCurrentFrame() && d.getParent() == null
	// && line.getOppositeEnd(d).getParent() == toPaint) {
	// nl = line.copy();
	// if (d == line.getStartItem())
	// d = nl.getStartItem();
	// else
	// d = nl.getEndItem();
	//
	// if (DisplayIO.FrameOnSide(toPaint) == 0)
	// d.setX(base);
	// else
	// d.setX(base + adj);
	//
	// } else if (toPaint == DisplayIO.getCurrentFrame()
	// && d.getParent() == null
	// && line.getOppositeEnd(d).getParent() != toPaint) {
	// nl = line.copy();
	//
	// if (d == line.getStartItem())
	// d = nl.getEndItem();
	// else
	// d = nl.getStartItem();
	//
	// if (DisplayIO.FrameOnSide(toPaint) == 1)
	// d.setX(d.getX() - DisplayIO.getMiddle());
	// else
	// d.setX(d.getX() + DisplayIO.getMiddle());
	// }
	// if (nl != null) {
	// nl.invalidateAll();
	// FrameGraphics.requestRefresh(true);
	// }
	// return nl;
	// }

	private static void Paint(Graphics g, Image left, Image right,
			Color background) {

		// if TwinFrames mode is on, then clipping etc has to be set
		if (DisplayIO.isTwinFramesOn()) {
			// draw the two lines down the middle of the window
			if (left != null)
				left.getGraphics().drawLine(DisplayIO.getMiddle() - 2, 0,
						DisplayIO.getMiddle() - 2, _MaxSize.height);

			if (right != null)
				right.getGraphics().drawLine(0, 0, 0, _MaxSize.height);

			// set the clipping area
			((Graphics2D) g).setClip(0, 0, DisplayIO.getMiddle() - 1,
					_MaxSize.height);
			g.drawImage(left, 0, 0, Item.DEFAULT_BACKGROUND, null);
			((Graphics2D) g).setClip(null);
			g.drawImage(right, DisplayIO.getMiddle() + 1, 0,
					Item.DEFAULT_BACKGROUND, null);

			// otherwise, just draw whichever side is active
		} else {
			if (DisplayIO.getCurrentSide() == 0)
				g.drawImage(left, 0, 0, Item.DEFAULT_BACKGROUND, null);
			else
				g.drawImage(right, 0, 0, Item.DEFAULT_BACKGROUND, null);
		}

	}

	public static void Clear() {
		Graphics g = _DisplayGraphics.create();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, _MaxSize.width, _MaxSize.height);
		g.dispose();
	}

	private static void PaintNonLinesNonPicture(Graphics2D g, List<Item> toPaint) {
		for (Item i : toPaint)
			if (!(i instanceof Line) && !(i instanceof XRayable))
				PaintItem(g, i);
	}

	/**
	 * Paint the lines that are not part of an enclosure.
	 * 
	 * @param g
	 * @param toPaint
	 */
	private static void PaintLines(Graphics2D g, List<Item> toPaint) {
		// Use this set to keep track of the items that have been painted
		Collection<Item> done = new HashSet<Item>();
		for (Item i : toPaint)
			if (i instanceof Line) {
				Line l = (Line) i;
				if (done.contains(l)) {
					l.paintArrows(g);
				} else {
					// When painting a line all connected lines are painted too
					done.addAll(l.getAllConnected());
					if (l.getStartItem().getEnclosedArea() == 0)
						PaintItem(g, i);
				}
			}
	}

	/**
	 * Paint filled areas and their surrounding lines as well as pictures. Note:
	 * floating widgets are painted as fills
	 * 
	 * @param g
	 * @param toPaint
	 */
	private static void PaintPictures(Graphics2D g, List<Item> toPaint,
			HashSet<Item> fillOnlyItems, HashSet<Item> done) {

		List<Item> toFill = new LinkedList<Item>();
		for (Item i : toPaint) {
			// Ignore items that have already been done!
			// Also ignore invisible items..
			// TODO possibly ignore invisible items before coming to this
			// method?
			if (done.contains(i))
				continue;
			if (i instanceof XRayable) {
				toFill.add(i);
				done.addAll(i.getConnected());
			} else if (i.hasEnclosures()) {
				for (Item enclosure : i.getEnclosures()) {
					if (!toFill.contains(enclosure))
						toFill.add(enclosure);
				}
				done.addAll(i.getConnected());
			} else if (i.isLineEnd()
					&& (!isAudienceMode() || !i.isConnectedToAnnotation())) {
				toFill.add(i);
				done.addAll(i.getAllConnected());
			}
		}

		if (fillOnlyItems != null) {
			for (Item i : fillOnlyItems) {
				if (done.contains(i))
					continue;
				else if (!isAudienceMode() || !i.isConnectedToAnnotation()) {
					toFill.add(i);
				}
				done.addAll(i.getAllConnected());
			}
		}

		// Sort the items to fill
		Collections.sort(toFill, new Comparator<Item>() {
			public int compare(Item a, Item b) {
				Double aArea = a.getEnclosedArea();
				Double bArea = b.getEnclosedArea();
				int cmp = aArea.compareTo(bArea);
				if (cmp == 0) {
					// System.out.println(a.getEnclosureID() + " " +
					// b.getID());\
					// Shapes to the left go underneath
					Polygon pA = a.getEnclosedShape();
					Polygon pB = b.getEnclosedShape();
					if (pA == null || pB == null)
						return 0;
					return new Integer(pA.getBounds().x).compareTo(pB
							.getBounds().x);
				}
				return cmp * -1;
			}
		});
		for (Item i : toFill) {
			if (i instanceof XRayable) {
				PaintItem(g, i);
			} else {
				// Paint the fill and lines
				i.paintFill(g);
				List<Line> lines = i.getLines();
				if (lines.size() > 0)
					PaintItem(g, lines.get(0));
			}
		}
	}

	/**
	 * Highlights an item on the screen Note: All graphics are handled by the
	 * Item itself.
	 * 
	 * @param i
	 *            The Item to highlight.
	 * @param val
	 *            True if the highlighting is being shown, false if it is being
	 *            erased.
	 * @return the item that was highlighted
	 */
	public static Item Highlight(Item i) {
		if ((i instanceof Line)) {
			// Check if within 20% of the end of the line
			Line l = (Line) i;
			Item toDisconnect = l.getEndPointToDisconnect(Math
					.round(FrameMouseActions.MouseX), Math
					.round(FrameMouseActions.MouseY));

			// Brook: Widget Edges do not have such a context
			if (toDisconnect != null && !(i instanceof WidgetEdge)) {
				Item.HighlightMode newMode = toDisconnect.getHighlightMode();
				if (FreeItems.itemsAttachedToCursor())
					newMode = Item.HighlightMode.Normal;
				// unhighlight all the other dots
				for (Item conn : toDisconnect.getAllConnected()) {
					conn.setHighlightMode(Item.HighlightMode.None);
				}
				l.setHighlightMode(newMode);
				// highlight the dot that will be in disconnect mode
				toDisconnect.setHighlightMode(newMode);
				i = toDisconnect;
			} else {
				if(FrameMouseActions.isShiftDown()) {
					for(Item j : i.getAllConnected()) {
        				if(j instanceof Dot && !j.equals(i)) {
        					j.setHighlightMode(HighlightMode.None);
        				}
        			}
        			l.getStartItem().setHighlightMode(HighlightMode.Connected);
        			l.getEndItem().setHighlightMode(HighlightMode.Connected);
        		} else {
        			for(Item j : i.getAllConnected()) {
        				if(j instanceof Dot && !j.equals(i)) {
        					j.setHighlightMode(HighlightMode.Connected);
        				}
        			}
        		}
//				Collection<Item> connected = i.getAllConnected();
//				for (Item conn : connected) {
//					conn.setHighlightMode(Item.HighlightMode.Connected);
//				}
			}
		} else if (i instanceof Circle) {
			i.setHighlightMode(Item.HighlightMode.Connected);
		} else if (!i.isVisible()) {
			changeHighlightMode(i, Item.HighlightMode.Connected, null);
		} else if (i instanceof Dot) {
			// highlight the dot
			if (i.hasPermission(UserAppliedPermission.full)) {
				changeHighlightMode(i, Item.HighlightMode.Normal, Item.HighlightMode.None);
			} else {
				changeHighlightMode(i, Item.HighlightMode.Connected, Item.HighlightMode.Connected);
			}
			// highlight connected dots, but only if there aren't items being carried on the cursor
			if(FreeItems.getInstance().size() == 0) {
    			if(FrameMouseActions.isShiftDown()) {
        			for(Item j : i.getAllConnected()) {
        				if(j instanceof Dot && !j.equals(i)) {
        					j.setHighlightMode(HighlightMode.Connected);
        				}
        			}
        		} else {
        			for(Item j : i.getAllConnected()) {
        				if(j instanceof Dot && !j.equals(i)) {
        					j.setHighlightMode(HighlightMode.None);
        				}
        			}
        			for(Line l : i.getLines()) {
        				Item j = l.getOppositeEnd(i);
       					j.setHighlightMode(HighlightMode.Connected);
        			}
        		}
			}
    	} else {
			// FrameGraphics.ChangeSelectionMode(i,
			// Item.SelectedMode.Normal);
			// For polygons need to make sure all other endpoints are
			// unHighlighted
			if (i.hasPermission(UserAppliedPermission.full))
				changeHighlightMode(i, Item.HighlightMode.Normal,
						Item.HighlightMode.None);
			else
				changeHighlightMode(i, Item.HighlightMode.Connected,
						Item.HighlightMode.Connected);
		}
		Repaint();
		return i;
	}

	public static void changeHighlightMode(Item item, Item.HighlightMode newMode) {
		changeHighlightMode(item, newMode, newMode);
	}

	public static void changeHighlightMode(Item item,
			Item.HighlightMode newMode, Item.HighlightMode connectedNewMode) {
		if (item == null)
			return;

		if (item.hasVector()) {
			for (Item i : item.getParentOrCurrentFrame().getVectorItems()) {
				if (i.getEditTarget() == item) {
					i.setHighlightMode(newMode);
				}
			}
			item.setHighlightMode(newMode);
		} else {
			// Mike: TODO comment on why the line below is used!!
			// I forgot already!!Opps
			boolean freeItem = FreeItems.getInstance().contains(item);
			for (Item i : item.getAllConnected()) {
				if (/* freeItem || */!FreeItems.getInstance().contains(i)) {
					i.setHighlightMode(connectedNewMode);
				}
			}
			if (!freeItem && newMode != connectedNewMode)
				item.setHighlightMode(newMode);
		}
		Repaint();
	}

	/**
	 * Repaints the buffer of the given Frame.
	 * 
	 * @param toUpdate
	 *            The Frame whose buffer is to be repainted.
	 */

	public static void UpdateBuffer(Frame toUpdate, boolean paintOverlays,
			boolean useVolitile) {
		toUpdate.setBuffer(getBuffer(toUpdate, paintOverlays, useVolitile));
	}

	public static Image getBuffer(Frame toUpdate, boolean paintOverlays,
			boolean useVolitile) {
		if (toUpdate == null)
			return null;

		return Paint(toUpdate, null, paintOverlays, useVolitile);
	}

	public static int getMode() {
		return _Mode;
	}

	public static Graphics createGraphics() {
		// Error messages on start up will call this message before
		// _DisplayGraphics has been initialised
		if (_DisplayGraphics == null)
			return null;
		return _DisplayGraphics.create();
	}

	// Damaged areas pending to render. Accessessed by multiple threads
	private static HashSet<Rectangle> damagedAreas = new HashSet<Rectangle>();

	/** The clip used while paiting */
	private static Area currentClip;

	/**
	 * The current clip that is used for painting at this instant.
	 * 
	 * Intention: for extra clipping within an items paint method - the clip is
	 * lost in the graphics object for which can be regained via this method.
	 * 
	 * @return The current clip. Null if no clip (e.g. full screen render).
	 */
	public static Area getCurrentClip() {
		return (currentClip != null) ? (Area) currentClip.clone() : null;
	}

	/**
	 * Checks that the item is visible (on current frame && overlays) - if
	 * visible then damaged area will be re-rendered on the next refresh.
	 * 
	 * @param damagedItem
	 * @param toRepaint
	 */
	public static void invalidateItem(Item damagedItem, Rectangle toRepaint) {
		// Only add area to repaint if item is visible...
		if (ItemUtils.isVisible(damagedItem)) {
			synchronized (damagedAreas) {
				damagedAreas.add(toRepaint);
			}
		} else if (MessageBay.isMessageItem(damagedItem)) {
			MessageBay.addDirtyArea(toRepaint);
		}
	}

	/**
	 * The given area will be re-rendered in the next refresh. This is the
	 * quicker version and is more useful for re-rendering animated areas.
	 * 
	 * @param toRepaint
	 */
	public static void invalidateArea(Rectangle toRepaint) {
		synchronized (damagedAreas) {
			damagedAreas.add(toRepaint);
		}
	}

	public static void clearInvalidAreas() {
		synchronized (damagedAreas) {
			damagedAreas.clear();
		}
	}

	/**
	 * Invalidates the buffered image of the current Frame and forces it to be
	 * repainted on to the screen. Repaints all items. This is more expensive
	 * than refresh.
	 */
	public static void ForceRepaint() { // TODO: TEMP: Use refresh
		Frame current = DisplayIO.getCurrentFrame();

		if (current == null)
			return;
		refresh(false);
	}

	public static void Repaint() { // TODO: TEMP: Use refresh
		refresh(true);
	}

	/**
	 * Called to refresh the display screen. Thread safe.
	 */
	public static void refresh(boolean useInvalidation) {

		if (_DisplayGraphics == null || _MaxSize.width <= 0
				|| _MaxSize.height <= 0)
			return;

		currentClip = null;
		if (useInvalidation) { // build clip

			synchronized (damagedAreas) {
				if (!damagedAreas.isEmpty()) {

					for (Rectangle r : damagedAreas) {
						if (currentClip == null)
							currentClip = new Area(r);
						else
							currentClip.add(new Area(r));
					}
					damagedAreas.clear();

				} else if (MessageBay.isDirty()) {
					// Paint dirty message bay
					Graphics dg = _DisplayGraphics.create();
					MessageBay.refresh(true, dg, Item.DEFAULT_BACKGROUND);
					return;

				} else
					return; // nothing to render
			}

		} else {
			synchronized (damagedAreas) {
				damagedAreas.clear();
			}
			// System.out.println("FULLSCREEN REFRESH"); // TODO: REMOVE
		}

		Frame[] toPaint = DisplayIO.getFrames();
		Image left = Paint(toPaint[0], currentClip);
		Image right = Paint(toPaint[1], currentClip);

		Graphics dg = _DisplayGraphics.create();

		// Paint frame to window
		Paint(dg, left, right, Item.DEFAULT_BACKGROUND);

		// Paint any animations
		PopupManager.getInstance().paintAnimations();

		// Paint message bay
		MessageBay.refresh(useInvalidation, dg, Item.DEFAULT_BACKGROUND);

		dg.dispose();
	}

	/**
	 * If wanting to refresh from another thread - other than the main thread
	 * that handles the expeditee datamodel (modifying / accessing / rendering).
	 * Use this method for thread safety.
	 */
	public static synchronized void requestRefresh(boolean useInvalidation) {

		_requestMarsheller._useInvalidation = useInvalidation;

		if (_requestMarsheller._isQueued) {
			return;
		}

		_requestMarsheller._isQueued = true;
		EventQueue.invokeLater(_requestMarsheller); // Render on AWT thread
	}

	private static RenderRequestMarsheller _requestMarsheller = new FrameGraphics().new RenderRequestMarsheller();

	/**
	 * Used for marshelling render requests from foreign threads to the event
	 * dispatcher thread... (AWT)
	 * 
	 * @author Brook Novak
	 */
	private class RenderRequestMarsheller implements Runnable {

		boolean _useInvalidation = true;

		boolean _isQueued = false;

		public void run() {
			refresh(_useInvalidation);
			_isQueued = false;
			_useInvalidation = true;
		}

	}

	/**
	 * Adds a FinalFrameRenderPass to the frame-render pipeline...
	 * 
	 * Note that the last added FinalFrameRenderPass will be rendered at the
	 * very top.
	 * 
	 * @param pass
	 *            The pass to add. If already added then nothing results in the
	 *            call.
	 * 
	 * @throws NullPointerException
	 *             If pass is null.
	 */
	public static void addFrameRenderPass(FrameRenderPass pass) {
		if (pass == null)
			throw new NullPointerException("pass");

		if (!_frameRenderPasses.contains(pass))
			_frameRenderPasses.add(pass);
	}

	/**
	 * Adds a FinalFrameRenderPass to the frame-render pipeline...
	 * 
	 * Note that the last added FinalFrameRenderPass will be rendered at the
	 * very top.
	 * 
	 * @param pass
	 *            The pass to remove
	 * 
	 */
	public static void removeFrameRenderPass(FrameRenderPass pass) {
		_frameRenderPasses.remove(pass);
	}

	/**
	 * A FinalFrameRenderPass is invoked at the very final stages for rendering
	 * a frame: that is, after the popups are drawn.
	 * 
	 * There can be many applications for FinalFrameRenderPass. Such as tool
	 * tips, balloons, or drawing items at the highest Z-order in special
	 * situations.
	 * 
	 * Although if there are multiples FinalFrameRenderPasses attatach to the
	 * frame painter then it is not garaunteed to be rendered very last.
	 * 
	 * @see FrameGraphics#addFinalFrameRenderPass(org.expeditee.gui.FrameGraphics.FrameRenderPass)
	 * @see FrameGraphics#removeFinalFrameRenderPass(org.expeditee.gui.FrameGraphics.FrameRenderPass)
	 * 
	 * @author Brook Novak
	 */
	public interface FrameRenderPass {

		/**
		 * 
		 * @param currentClip
		 * 
		 * @return The clip that the pass should use instead. i.e. if there are
		 *         any effects that cannot invladate prior to paint call.
		 */
		Area paintStarted(Area currentClip);

		void paintFinalPass(Graphics g);

		void paintPreLayeredPanePass(Graphics g);
	}

}
