package org.expeditee.gui;

import java.awt.Color;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.image.ImageObserver;
import java.awt.image.VolatileImage;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.expeditee.actions.Simple;
import org.expeditee.io.Conversion;
import org.expeditee.items.Constraint;
import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.Item.HighlightMode;
import org.expeditee.items.ItemAppearence;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Line;
import org.expeditee.items.PermissionPair;
import org.expeditee.items.Text;
import org.expeditee.items.UserAppliedPermission;
import org.expeditee.items.XRayable;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.settings.UserSettings;
import org.expeditee.settings.templates.TemplateSettings;
import org.expeditee.simple.UnitTestFailedException;
import org.expeditee.stats.Formatter;
import org.expeditee.stats.SessionStats;

/**
 * Represents a Expeditee Frame that is displayed on the screen. Also is a
 * registered MouseListener on the Browser, and processes any MouseEvents
 * directly.
 * 
 * @author jdm18
 * 
 */
public class Frame implements ImageObserver {

	private boolean _protectionChanged = false;

	public boolean isReadOnly() {
		return !_frameName.hasPermission(UserAppliedPermission.full)
				&& !_protectionChanged;
	}

	// The various attributes of this Frame
	private String _frameset = null;

	private int _number = -1;

	private int _version = 0;

	private PermissionPair _permissionPair = null;

	private String _owner = null;

	private String _creationDate = null;

	private String _modifiedUser = null;

	private String _modifiedDate = null;

	private String _frozenDate = null;

	// Background color is clear
	private Color _background = null;

	// Foreground color is automatic by default
	private Color _foreground = null;

	private String path;

	private boolean _isLocal = true;

	private boolean _sorted = true;

	// The items contained in this Frame
	// records whether a change has been made to this Frame (for saving
	// purposes).
	private boolean _change = false;

	private boolean _saved = false;

	private static final class History {
		public enum Type {
			deletion,
			movement
		}
		public final List<Item> items;
		public final Type type;
		
		public History(Collection<Item> items, Type type) {
			this.items = new LinkedList<Item>(items);
			this.type = type;
		}
		
		public String toString() {
			return this.type.toString() + ":\n" + this.items.toString();
		}
	}
	
	// list of deleted items that can be restored
	private Stack<History> _undo = new Stack<History>();
	private Stack<History> _redo = new Stack<History>();

	// basically just a list of smaller objects?
	// maybe a hashtable (id -> item?)
	// Note: Needs to be able to be iterated through (for painting)
	private List<Item> _body = new ArrayList<Item>();

	// for drawing purposes
	private List<InteractiveWidget> _iWidgets = new ArrayList<InteractiveWidget>();

	private int _lineCount = 0;

	private int _itemCount = 1;

	// The frameName to display on the screen
	private Text _frameName = null;

	private Map<Overlay, Frame> _overlays = new HashMap<Overlay, Frame>();

	private List<Vector> _vectors = new ArrayList<Vector>();

	private Image _buffer = null;

	private boolean _validBuffer = true;

	private Time _activeTime = new Time(0);

	private Time _darkTime = new Time(0);

	private Collection<Item> _interactableItems = new LinkedHashSet<Item>();

	private Collection<Item> _overlayItems = new LinkedHashSet<Item>();

	private Collection<Item> _vectorItems = new LinkedHashSet<Item>();

	private Text _dotTemplate = TemplateSettings.DotTemplate.get().copy();

	/**
	 * Default constructor, nothing is set.
	 */
	public Frame() {
	}

	public void reset() {
		refreshItemPermissions(UserAppliedPermission.full);
		//System.out.println("Reset");
		resetDot();
		SessionStats.NewFrameSession();
	}

	private void resetDot() {
		_dotTemplate.setColor(TemplateSettings.ColorWheel.get()[1]);
		_dotTemplate.setFillColor(TemplateSettings.FillColorWheel.get()[0]);
	}

	public void nextDot() {
		_dotTemplate.setFillColor(ColorUtils.getNextColor(_dotTemplate
				.getFillColor(), TemplateSettings.FillColorWheel.get(), null));
		_dotTemplate.setColor(ColorUtils.getNextColor(_dotTemplate.getColor(),
				TemplateSettings.ColorWheel.get(), null));

		if (_dotTemplate.getColor() == null || _dotTemplate.getColor().equals(Color.white)) {
			resetDot();
		}
	}

	public Image getBuffer() {
		return _buffer;
	}

	public void setBuffer(Image newBuffer) {
		_buffer = newBuffer;
	}

	public boolean isBufferValid() {
		if (_buffer == null
				|| (_buffer instanceof VolatileImage && ((VolatileImage) _buffer)
						.contentsLost()))
			return false;

		return _validBuffer;
	}

	private void setBufferValid(boolean newValue) {
		_validBuffer = newValue;
	}

	public int getNextItemID() {
		return ++_itemCount;
	}

	public void updateIDs(List<Item> items) {
		for (Item i : items)
			if (!(i instanceof Line))
				i.setID(getNextItemID());
			else
				i.setID(++_lineCount);
	}

	/**
	 * 
	 * @return The interactive widgets that are currently anchored in this frame.
	 *         Hence it excludes free-widgets. Returns a copy
	 */
	public List<InteractiveWidget> getInteractiveWidgets() {
		LinkedList<InteractiveWidget> clone = new LinkedList<InteractiveWidget>();
		clone.addAll(this._iWidgets);
		return clone;
	}

	/**
	 * Returns whether this Frame has been changed and required saving to disk.
	 * 
	 * @return True if this Frame has been altered, false otherwise.
	 */
	public boolean hasChanged() {
		// virtual frames are never saved
		if (_number == -1)
			return false;

		return _change;
	}

	/**
	 * Sets whether this Frame should be saved to disk.
	 * 
	 * @param value
	 *            False if this Frame should be saved to disk, False otherwise.
	 */
	public void setChanged(boolean value) {
		// System.out.println(getName() + " " + value);
		boolean oldValue = _change;

		// if (value) {
		// notifyObservers();
		// }

		if (oldValue == value)
			return;

		_change = value;

		if (_change) {
			setBufferValid(false);
			_saved = false;
		}
	}

	// private static int updateCount = 0;

	/**
	 * Notify items observing the data on this frame that the frame content has
	 * changed.
	 * 
	 * @param recalculate
	 *            true if the frame should be recalculated first.
	 */
	public void notifyObservers(boolean bRecalculate) {
		if (bRecalculate) {
			recalculate();
		}
		// Notify the frame listeners that the frame has changed
		/*
		 * Avoid ConcurrentMod Exceptions when user anchors an item onto this
		 * frame which is observing this frame, by NOT using foreach loop.
		 * Calling update on a dataFrameWidget resets its subjects hence
		 * changing this frames observer list.
		 */
		Collection<FrameObserver> observersCopy = new LinkedList<FrameObserver>(
				_observers);
		// System.out.println(++updateCount + " update");

		for (FrameObserver fl : observersCopy) {
			if (/* !Item.isLocked(fl) && */fl.isVisible())
				fl.update();
		}
	}

	// indicates the frame has changed
	public void change() {
		setChanged(true);
		_interactableItems.clear();
	}

	/**
	 * Returns an ArrayList of all Items currently on the Frame (excludes Items
	 * attached to the cursor).
	 * 
	 * @return The list of Item objects that are on this Frame.
	 */
	public List<Item> getItems(boolean visible) {

		if (!_sorted) {
			Collections.sort(_body);
			_sorted = true;
		}

		List<Item> items = new ArrayList<Item>();

		for (Item i : _body) {
			if (i == null)
				continue;
			if (i.isVisible() || (!visible && !i.isDeleted())) {
				items.add(i);
			}
		}

		return items;
	}

	public List<Item> getItems() {
		return getItems(false);
	}

	/**
	 * @param i
	 *            Item to check if contained in this frame
	 * @return True if this frame contains i.
	 */
	public boolean containsItem(Item i) {
		if (i == null)
			throw new NullPointerException("i");
		return _body.contains(i);
	}

	/**
	 * Returns a list of all the non annotation text items on the frame which
	 * are not the title or frame name or special annotation items.
	 * 
	 * @param includeAnnotations
	 *            true if annotation items without special meaning should be
	 *            included
	 * @param includeLineEnds
	 *            true if text on the end of lines should be included in the
	 *            list
	 * @return the list of body text items.
	 */
	public List<Text> getBodyTextItems(boolean includeAnnotations) {
		List<Text> bodyTextItems = new ArrayList<Text>();
		for (Item i : getItems(true)) {
			// only add up normal body text items
			if ((i instanceof Text)
					&& ((includeAnnotations && !((Text) i)
							.isSpecialAnnotation()) || !i.isAnnotation())
					&& !i.isLineEnd()) {
				bodyTextItems.add((Text) i);
			}
		}
		bodyTextItems.remove(getTitleItem());

		return bodyTextItems;
	}

	public Collection<Item> getNonAnnotationItems(boolean removeTitle) {
		Collection<Item> items = new ArrayList<Item>();
		for (Item i : getItems(true)) {
			// only add up normal body text items
			if (!i.isAnnotation()) {
				items.add(i);
			}
		}
		if (removeTitle) {
			items.remove(getTitleItem());
		}
		return items;
	}

	/**
	 * Gets the last item on the frame that is a non annotation item but is also
	 * text.
	 * 
	 * @return the last non annotation text item.
	 */
	public Item getLastNonAnnotationTextItem() {
		List<Item> items = getItems();

		// find the last non-annotation text item
		for (int i = (items.size() - 1); i >= 0; i--) {
			Item it = items.get(i);

			if (it instanceof Text && !it.isAnnotation()) {
				return (Item) it;
			}
		}
		return null;
	}

	/**
	 * Iterates through the list of items on the frame, and returns one with the
	 * given id if one exists, otherwise returns null.
	 * 
	 * @param id
	 *            The id to search for in the list of items
	 * @return The item on this frame with the given ID, or null if one is not
	 *         found.
	 */
	public Item getItemWithID(int id) {
		for (Item i : _body)
			if (i.getID() == id)
				return i;

		return null;
	}

	/**
	 * Sets this Frame's Title which is displayed in the top left corner.
	 * 
	 * @param title
	 *            The title to assign to this Frame
	 */
	public void setTitle(String title) {
		if (title == null || title.equals(""))
			return;

		boolean oldchange = _change;

		// remove any numbering this title has
		title = title.replaceAll("^\\d*[.] *", "");
		Text frameTitle = getTitleItem();

		if (frameTitle == null) {
			if (TemplateSettings.TitleTemplate.get() == null) {
				frameTitle = new Text(getNextItemID(), title);
			} else {
				frameTitle = TemplateSettings.TitleTemplate.get().copy();
				frameTitle.setID(this.getNextItemID());
				frameTitle.setText(title);
			}
			/*
			 * Need to set the parent otherwise an exception is thrown when
			 * new profile is created
			 */
			frameTitle.setParent(this);
			frameTitle.resetTitlePosition();
			addItem(frameTitle);
		} else {
			// If it begins with a tag remove it

			// Remove the @ symbol if it is there
			// title = ItemUtils.StripTagSymbol(title);
			frameTitle.setText(title);
			// If the @ symbol is followed by numbering or a bullet remove that
			// too
			String autoBulletText = FrameKeyboardActions.getAutoBullet(title);
			if (autoBulletText.length() > 0)
				frameTitle.stripFirstWord();
		}
		// TODO Widgets... check this out
		// Brook: Cannot figure what is going on above... widget annot titles
		// should be stripped always
		if (ItemUtils.startsWithTag(frameTitle, ItemUtils
				.GetTag(ItemUtils.TAG_IWIDGET))) {
			frameTitle.stripFirstWord();
		}

		FrameUtils.Parse(this);

		// do not save if this is the only change
		setChanged(oldchange);
	}

	public Text getTitleItem() {
		List<Item> items = getVisibleItems();
		for (Item i : items) {
			if (i instanceof Text && i.getX() < UserSettings.TitlePosition.get()
					&& i.getY() < UserSettings.TitlePosition.get())
				return (Text) i;
		}

		return null;
	}

	public String getTitle() {
		Text title = getTitleItem();
		if (title == null)
			return getName();

		return title.getFirstLine();
	}

	public Item getNameItem() {
		return _frameName;
	}

	public Text getItemTemplate() {
		return getTemplate(TemplateSettings.ItemTemplate.get(),
				ItemUtils.TAG_ITEM_TEMPLATE);
	}

	public Text getAnnotationTemplate() {
		Text t = getTemplate(TemplateSettings.AnnotationTemplate.get(),
				ItemUtils.TAG_ANNOTATION_TEMPLATE);

		if (t == null) {
			t = getItemTemplate();
		}

		return t;
	}

	public Text getStatTemplate() {
		SessionStats.CreatedText();
		Text t = getTemplate(TemplateSettings.StatTemplate.get(),
				ItemUtils.TAG_STAT_TEMPLATE);

		if (t == null) {
			t = getItemTemplate();
		}

		return t;
	}
	
	public Item getTooltipTextItem(String tooltipText) {
		return getTextItem(tooltipText, TemplateSettings.TooltipTemplate.get().copy());
	}

	public Item getStatsTextItem(String itemText) {
		return getTextItem(itemText, getStatTemplate());
	}

	public Item getTextItem(String itemText) {
		return getTextItem(itemText, getItemTemplate());
	}

	private Item getTextItem(String itemText, Text template) {
		Text t = template;
		// We dont want the stats to wrap at all
		// t.setMaxWidth(Integer.MAX_VALUE);
		t.setPosition(DisplayIO.getMouseX(), FrameMouseActions.getY());
		// The next line is needed to make sure the item is removed from the
		// frame when picked up
		t.setParent(this);
		t.setText(itemText);
		return t;
	}

	public Text getCodeCommentTemplate() {
		Text t = getTemplate(TemplateSettings.CommentTemplate.get(), 
				ItemUtils.TAG_CODE_COMMENT_TEMPLATE);

		if (t == null) {
			t = getItemTemplate();
		}

		return t;
	}
	

	/**
	 * Returns any items on this frame that are within the given Shape. Also
	 * returns any Items on overlay frames that are within the Shape.
	 * 
	 * @param shape
	 *            The Shape to search for Items in
	 * @return All Items on this Frame or overlayed Frames for which
	 *         Item.intersects(shape) return true.
	 */
	public Collection<Item> getItemsWithin(Polygon poly) {
		Collection<Item> results = new LinkedHashSet<Item>();
		for (Item i : getVisibleItems()) {
			if (i.intersects(poly)) {
				if (i instanceof XRayable) {
					results.addAll(i.getConnected());
					// Dont add circle centers
					// TODO change this to be isCircle center
				} else if (!i.hasEnclosures()) {
					results.add(i);
				}
			}
		}

		for (Overlay o : _overlays.keySet())
			results.addAll(o.Frame.getItemsWithin(poly));

		for (Item i : getVectorItems()) {
			if (i.intersects(poly)) {
				// This assumes a results is a set
				results.add(i.getEditTarget());
			}
		}

		return results;
	}

	/**
	 * Sets the name of this Frame to the given String, to be displayed in the
	 * upper right corner.
	 * 
	 * @param name
	 *            The name to use for this Frame.
	 */
	public void setFrameset(String name) {
		_frameset = name;
	}

	public void setName(String framename) {
		int num = Conversion.getFrameNumber(framename);
		String frameset = Conversion.getFramesetName(framename, false);

		setName(frameset, num);
	}

	/**
	 * Sets the frame number of this Frame to the given integer
	 * 
	 * @param number
	 *            The number to set as the frame number
	 */
	public void setFrameNumber(int number) {
		assert (number >= 0);

		if (_number == number)
			return;

		_number = number;
		boolean oldchange = _change;

		int id;

		if (_frameName != null) {
			id = _frameName.getID();
		} else {
			id = -1 * getNextItemID();
		}
		_frameName = new Text(id);
		_frameName.setParent(this);
		_frameName.setText(getFramesetName() + _number);
		_frameName.resetFrameNamePosition();
		setChanged(oldchange);
	}

	/**
	 * Returns the number of this Frame.
	 * 
	 * @return The Frame number of this Frame or -1 if it is not set.
	 */
	public int getNumber() {
		return _number;
	}

	/**
	 * Increments the version of this Frame to the given String.
	 * 
	 * @param version
	 *            The version to use for this Frame.
	 */
	public void setVersion(int version) {
		_version = version;
	}

	/**
	 * Sets the protection of this Frame to the given String.
	 * 
	 * @param protection
	 *            The protection to use for this Frame.
	 */
	public void setPermission(PermissionPair permission) {
		if (_permissionPair != null && _permissionPair.getPermission(this._owner).equals(permission))
			_protectionChanged = true;

		_permissionPair = new PermissionPair(permission);

		if (_body.size() > 0)
			refreshItemPermissions(permission.getPermission(_owner));
	}

	/**
	 * Sets the owner of this Frame to the given String.
	 * 
	 * @param owner
	 *            The owner to use for this Frame.
	 */
	public void setOwner(String owner) {
		_owner = owner;
	}

	/**
	 * Sets the created date of this Frame to the given String.
	 * 
	 * @param date
	 *            The date to use for this Frame.
	 */
	public void setDateCreated(String date) {
		_creationDate = date;
		_modifiedDate = date;
		for (Item i : _body) {
			i.setDateCreated(date);
		}
	}

	/**
	 * Resets the dates and version numbers for newly created frames.
	 * 
	 */
	public void resetDateCreated() {
		setDateCreated(Formatter.getDateTime());
		resetTimes();
		setVersion(0);
	}

	private void resetTimes() {
		setActiveTime(new Time(0));
		setDarkTime(new Time(0));
	}

	/**
	 * Sets the last modifying user of this Frame to the given String.
	 * 
	 * @param user
	 *            The user to set as the last modifying user.
	 */
	public void setLastModifyUser(String user) {
		_modifiedUser = user;
	}

	/**
	 * Sets the last modified date of this Frame to the given String.
	 * 
	 * @param date
	 *            The date to set as the last modified date.
	 */
	public void setLastModifyDate(String date) {
		_modifiedDate = date;
	}

	/**
	 * Sets the last frozen date of this Frame to the given String.
	 * 
	 * @param date
	 *            The date to set as the last frozen date.
	 */
	public void setFrozenDate(String date) {
		_frozenDate = date;
	}

	public void setResort(boolean value) {
		_sorted = !value;
	}

	/**
	 * Adds the given Item to the body of this Frame.
	 * 
	 * @param item
	 *            The Item to add to this Frame.
	 */
	public void addItem(Item item) {
		addItem(item, true);
	}

	public void addItem(Item item, boolean recalculate) {
		if (item == null || item.equals(_frameName) || _body.contains(item))
			return;

		// When an annotation item is anchored the annotation list must be
		// refreshed
		if (item.isAnnotation()) {
			clearAnnotations();
		}

		if (item instanceof Line)
			_lineCount++;

		_itemCount = Math.max(_itemCount, item.getID());

		_body.add(item);
		item.setParent(this);
		item.setFloating(false); // esnure that it is anchored

		item.invalidateCommonTrait(ItemAppearence.Added);

		// If the item is a line end and has constraints with items already
		// on the frame then make sure the constraints hold
		if (item.isLineEnd()) {
			item.setPosition(item.getPosition());
		}

		_sorted = false;

		// item.setMaxWidth(FrameGraphics.getMaxFrameSize().width);
		// add widget items to the list of widgets
		if (item instanceof WidgetCorner) {
			InteractiveWidget iw = ((WidgetCorner) item).getWidgetSource();
			if (!this._iWidgets.contains(iw)) { // A set would have been
				if (FrameMouseActions.isControlDown())
					_iWidgets.add(iw);
				else
					_iWidgets.add(0, iw);
			}
		}

		item.onParentStateChanged(new ItemParentStateChangedEvent(this,
				ItemParentStateChangedEvent.EVENT_TYPE_ADDED));

		// if (recalculate && item.recalculateWhenChanged())
		// recalculate();

		change();
	}

	public void refreshSize() {
		// assert (size != null);
		boolean bReparse = false;
		for (Item i : getItems()) {
			Float anchorLeft   = i.getAnchorLeft();
			Float anchorRight  = i.getAnchorRight();
			Float anchorTop    = i.getAnchorTop();
			Float anchorBottom = i.getAnchorBottom();
	
			
			if (anchorLeft != null) {
				i.setAnchorLeft(anchorLeft);
				if (i.hasVector()) {
					bReparse = true;
				}
			}
			
			if (anchorRight != null) {
				i.setAnchorRight(anchorRight);
				if (i.hasVector()) {
					bReparse = true;
				}
			}
			
			if (anchorTop != null) {
				i.setAnchorTop(anchorTop);
				if (i.hasVector()) {
					bReparse = true;
				}
			}
			
			if (anchorBottom != null) {
				i.setAnchorBottom(anchorBottom);
				if (i.hasVector()) {
					bReparse = true;
				}
			}
		}

		// Do the anchors on the overlays
		for (Overlay o : getOverlays()) {
			o.Frame.refreshSize();
		}

		if (bReparse) {
			FrameUtils.Parse(this, false);
		}

		_frameName.resetFrameNamePosition();
	}

	public void addAllItems(Collection<Item> toAdd) {
		for (Item i : toAdd) {
			// If an annotation is being deleted clear the annotation list
			if (i.isAnnotation())
				i.getParentOrCurrentFrame().clearAnnotations();
			// TODO Improve efficiency when addAll is called
			addItem(i);
		}
	}

	public void removeAllItems(Collection<Item> toRemove) {
		for (Item i : toRemove) {
			// If an annotation is being deleted clear the annotation list
			if (i.isAnnotation())
				i.getParentOrCurrentFrame().clearAnnotations();
			removeItem(i);
		}
	}

	public void removeItem(Item item) {
		removeItem(item, true);
	}

	public void removeItem(Item item, boolean recalculate) {
		// If an annotation is being deleted clear the annotation list
		if (item.isAnnotation())
			item.getParentOrCurrentFrame().clearAnnotations();

		if (_body.remove(item)) {
			change();
			// Remove widgets from the widget list
			if (item != null) {
				item.onParentStateChanged(new ItemParentStateChangedEvent(this,
						ItemParentStateChangedEvent.EVENT_TYPE_REMOVED));
				if (item instanceof WidgetCorner) {
					_iWidgets.remove(((WidgetCorner) item).getWidgetSource());
				}
				item.invalidateCommonTrait(ItemAppearence.Removed);
			}
			// TODO Improve efficiency when removeAll is called
			// if (recalculate && item.recalculateWhenChanged())
			// recalculate();
		}
	}

	/**
	 * Adds the given History event to the stack.
	 * 
	 * @param stack The stack to add to
	 * @param items The items to put in the event
	 * @param type  The type of event that occurred
	 */
	private void addToUndo(Collection<Item> items, History.Type type) {
		if (items.size() < 1)
			return;
		
		// System.out.println("Added: " + items);

		_undo.push(new History(items, type));
	}
	
	public void addToUndoDelete(Collection<Item> items) {
		addToUndo(items, History.Type.deletion);
	}
	public void addToUndoMove(Collection<Item> items) {
		addToUndo(items, History.Type.movement);
	}

	public void undo() {
		boolean bReparse = false;
		boolean bRecalculate = false;

		if (_undo.size() <= 0)
			return;

		History undo = _undo.pop();
		
		// System.out.println("Undoing: " + undo);

		switch(undo.type) {
		case deletion:
			_redo.push(undo);
    		for(Item i : undo.items) {
    			_body.add(i);
				bReparse |= i.hasOverlay();
				bRecalculate |= i.recalculateWhenChanged();
				if (i instanceof Line) {
					Line line = (Line) i;
					line.getStartItem().addLine(line);
					line.getEndItem().addLine(line);
				} else {
					i.setOffset(0, 0);
				}
    		}
    		break;
		case movement:
			List<Item> changed = new LinkedList<Item>(_body);
			changed.retainAll(undo.items);
			_redo.push(new History(changed, History.Type.movement));
			for(Item i : undo.items) {
				int index;
				if(i.isVisible() && (index = _body.indexOf(i)) != -1) {
					_body.set(index, i);
				}
    		}
			break;
		}
		change();
		FrameMouseActions.getInstance().refreshHighlights();
		if (bReparse) {
			FrameUtils.Parse(this, false, false);
		} else {
			notifyObservers(bRecalculate);
		}
		// always request a refresh otherwise filled shapes
		// that were broken by a deletion and then reconnected by the undo
		// don't get filled until the user otherwise causes them to redraw
		FrameGraphics.requestRefresh(false);
		FrameGraphics.Repaint();
		ItemUtils.EnclosedCheck(_body);
	}
	
	public void redo() {
		boolean bReparse = false;
		boolean bRecalculate = false;

		if (_redo.size() <= 0)
			return;

		History redo = _redo.pop();
		
		// System.out.println("Redoing: " + redo);

		switch(redo.type) {
		case deletion:
			_undo.push(redo);
    		for(Item i : redo.items) {
    			_body.remove(i);
				bReparse |= i.hasOverlay();
				bRecalculate |= i.recalculateWhenChanged();
				if (i instanceof Line) {
					Line line = (Line) i;
					line.getStartItem().removeLine(line);
					line.getEndItem().removeLine(line);
				} else {
					i.setOffset(0, 0);
				}
    		}
    		break;
		case movement:
			List<Item> changed = new LinkedList<Item>(_body);
			changed.retainAll(redo.items);
			_undo.push(new History(changed, History.Type.movement));
			for(Item i : redo.items) {
				int index;
				if(i.isVisible() && (index = _body.indexOf(i)) != -1) {
					_body.set(index, i);
				}
    		}
			break;
		}
		change();
		FrameMouseActions.getInstance().refreshHighlights();
		if (bReparse) {
			FrameUtils.Parse(this, false, false);
		} else {
			notifyObservers(bRecalculate);
		}
		// always request a refresh otherwise filled shapes
		// that were broken by a deletion and then reconnected by the undo
		// don't get filled until the user otherwise causes them to redraw
		FrameGraphics.requestRefresh(false);
		FrameGraphics.Repaint();
		ItemUtils.EnclosedCheck(_body);
	}

	/**
	 * Returns the frameset of this Frame
	 * 
	 * @return The name of this Frame's frameset.
	 */
	public String getFramesetName() {
		return _frameset;
	}

	public String getName() {
		return getFramesetName() + _number;
	}

	/**
	 * Returns the format version of this Frame
	 * 
	 * @return The version of this Frame.
	 */
	public int getVersion() {
		return _version;
	}

	public PermissionPair getPermission() {
		return _permissionPair;
	}
	
	public UserAppliedPermission getUserAppliedPermission() {
		return getUserAppliedPermission(UserAppliedPermission.full);
	}

	public UserAppliedPermission getUserAppliedPermission(UserAppliedPermission defaultPermission) {
		if (_permissionPair == null)
			return defaultPermission;

		return _permissionPair.getPermission(_owner);
	}

	
	public String getOwner() {
		return _owner;
	}

	public String getDateCreated() {
		return _creationDate;
	}

	public String getLastModifyUser() {
		return _modifiedUser;
	}

	public String getLastModifyDate() {
		return _modifiedDate;
	}

	public String getFrozenDate() {
		return _frozenDate;
	}

	public void setBackgroundColor(Color back) {
		_background = back;
		change();

		if (this == DisplayIO.getCurrentFrame()) {
			FrameGraphics.refresh(false);
		}
	}

	public Color getBackgroundColor() {
		return _background;
	}

	public Color getPaintBackgroundColor() {
		// If null... return white
		if (_background == null) {
			return Item.DEFAULT_BACKGROUND;
		}

		return _background;
	}

	public void setForegroundColor(Color front) {
		_foreground = front;
		change();
		// FrameGraphics.Repaint();
	}

	public Color getForegroundColor() {
		return _foreground;
	}

	public Color getPaintForegroundColor() {
		final int GRAY = Color.gray.getBlue();
		final int THRESHOLD = 10;

		if (_foreground == null) {
			Color back = getPaintBackgroundColor();
			if (Math.abs(back.getRed() - GRAY) < THRESHOLD
					&& Math.abs(back.getBlue() - GRAY) < THRESHOLD
					&& Math.abs(back.getGreen() - GRAY) < THRESHOLD)
				return Color.WHITE;

			Color fore = new Color(
					Math.abs(Conversion.RGB_MAX - back.getRed()), Math
							.abs(Conversion.RGB_MAX - back.getGreen()), Math
							.abs(Conversion.RGB_MAX - back.getBlue()));
			return fore;
		}

		return _foreground;
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(String.format("Name: %s%d%n", _frameset, _number));
		s.append(String.format("Version: %d%n", _version));
		// s.append(String.format("Permission: %s%n", _permission.toString()));
		// s.append(String.format("Owner: %s%n", _owner));
		// s.append(String.format("Date Created: %s%n", _creationDate));
		// s.append(String.format("Last Mod. User: %s%n", _modifiedUser));
		// s.append(String.format("Last Mod. Date: %s%n", _modifiedDate));
		s.append(String.format("Items: %d%n", _body.size()));
		return s.toString();
	}

	public Text getTextAbove(Text current) {
		Collection<Text> currentTextItems = FrameUtils.getCurrentTextItems();
		List<Text> toCheck = new ArrayList<Text>();
		if (currentTextItems.contains(current)) {
			toCheck.addAll(currentTextItems);
		} else {
			toCheck.addAll(getTextItems());
		}
		// Make sure the items are sorted
		Collections.sort(toCheck);

		int ind = toCheck.indexOf(current);
		if (ind == -1)
			return null;

		// loop through all items above this one, return the first match
		for (int i = ind - 1; i >= 0; i--) {
			Text check = toCheck.get(i);
			if (FrameUtils.inSameColumn(check, current))
				return check;
		}

		return null;
	}

	/**
	 * Updates any Images that require it from their ImageObserver (Principally
	 * Animated GIFs)
	 */
	public boolean imageUpdate(Image img, int infoflags, int x, int y,
			int width, int height) {
		FrameGraphics.ForceRepaint();

		if (DisplayIO.getCurrentFrame() == this)
			return true;

		return false;
	}

	/**
	 * Gets the text items that are in the same column and below a specified
	 * item. Frame title and name are excluded from the column list.
	 * 
	 * @param from
	 *            The Item to get the column for.
	 */
	public List<Text> getColumn(Item from) {
		// Check that this item is on the current frame
		if (!_body.contains(from))
			return null;

		if (from == null) {
			from = getLastNonAnnotationTextItem();
		}

		if (from == null)
			return null;

		// Get the enclosedItems
		Collection<Text> enclosed = FrameUtils.getCurrentTextItems();
		List<Text> toCheck = null;
		if (enclosed.contains(from)) {
			toCheck = new ArrayList<Text>();
			toCheck.addAll(enclosed);
		} else {
			toCheck = getBodyTextItems(true);
		}

		List<Text> column = new ArrayList<Text>();
		if (toCheck.size() > 0) {

			// Make sure the items are sorted
			Collections.sort(toCheck);

			// Create a list of items consisting of the item 'from' and all the
			// items below it which are also in the same column as it
			int index = toCheck.indexOf(from);

			// If its the title index will be 0
			if (index < 0)
				index = 0;

			for (int i = index; i < toCheck.size(); i++) {
				Text item = toCheck.get(i);
				if (FrameUtils.inSameColumn(from, item))
					column.add(item);
			}
		}

		return column;
	}

	/**
	 * Adds the given Vector to the list of vector Frames being drawn with this
	 * Frame.
	 * 
	 * @param vector
	 *            The Vector to add
	 * 
	 * @throws NullPointerException
	 *             If overlay is null.
	 */
	protected boolean addVector(Vector toAdd) {
		// make sure we dont add this frame as an overlay of itself
		if (toAdd.Frame == this)
			return false;
		_vectors.add(toAdd);
		// Items must be notified that they have been added or removed from this
		// frame via the vector...
		int maxX = 0;
		int maxY = 0;
		HighlightMode mode = toAdd.Source.getHighlightMode();
		if (mode != HighlightMode.None)
			mode = HighlightMode.Connected;
		Color highlightColor = toAdd.Source.getHighlightColor();
		for (Item i : ItemUtils.CopyItems(toAdd.Frame.getVectorItems(), toAdd)) {
			i.onParentStateChanged(new ItemParentStateChangedEvent(this,
					ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY,
					toAdd.permission));
			i.setEditTarget(toAdd.Source);
			i.setHighlightMode(mode, highlightColor);
			_vectorItems.add(i);
			i.invalidateAll();
			i.invalidateFill();
			// Get the right most x and bottom most y pos
			int itemRight = i.getX() + i.getBoundsWidth();
			if (itemRight > maxX)
				maxX = itemRight;
			int itemBottom = i.getY() + i.getBoundsHeight();
			if (itemBottom > maxY)
				maxY = itemBottom;
		}
		toAdd.setSize(maxX, maxY);
		return true;
	}

	public Collection<Vector> getVectors() {
		Collection<Vector> l = new LinkedList<Vector>();
		l.addAll(_vectors);
		return l;
	}

	public Collection<Overlay> getOverlays() {
		return new LinkedList<Overlay>(_overlays.keySet());
	}

	/**
	 * @return All vectosr seen by this frame (including its vector's vectors).
	 */
	public List<Vector> getVectorsDeep() {
		List<Vector> l = new LinkedList<Vector>();
		getVectorsDeep(l, this, new LinkedList<Frame>());
		return l;
	}

	private boolean getVectorsDeep(List<Vector> vectors, Frame vector,
			List<Frame> seenVectors) {

		if (seenVectors.contains(vector))
			return false;

		seenVectors.add(vector);

		for (Vector o : vector.getVectors()) {
			if (getVectorsDeep(vectors, o.Frame, seenVectors)) {
				vectors.add(o);
			}
		}

		return true;
	}

	// private boolean getOverlaysDeep(List<Overlay> overlays, Frame overlay,
	// List<Frame> seenOverlays) {
	//
	// if (seenOverlays.contains(overlay))
	// return false;
	//
	// seenOverlays.add(overlay);
	//
	// for (Overlay o : overlay.getOverlays()) {
	// if (getOverlaysDeep(overlays, o.Frame, seenOverlays)) {
	// overlays.add(o);
	// }
	// }
	//
	// return true;
	// }

	/**
	 * Gets the overlay on this frame which owns the given item.
	 * 
	 * @param item
	 *            The item - must not be null.
	 * @return The overlay that contains the itm. Null if no overlay owns the
	 *         item.
	 */
	public Overlay getOverlayOwner(Item item) {
		if (item == null)
			throw new NullPointerException("item");

		for (Overlay l : getOverlays()) {
			if (item.getParent() == l.Frame)
				return l;
		}

		// TODO return the correct vector... not just the first vector matching
		// the vectorFrame
		for (Vector v : getVectors()) {
			if (item.getParent() == v.Frame)
				return v;
		}

		return null;
	}

	public void clearVectors() {
		_vectors.clear();

		for (Item i : _vectorItems) { // TODO: Rethink where this should live
			i.invalidateAll();
			i.invalidateFill();
		}
		_vectorItems.clear();

	}

	protected boolean removeVector(Vector toRemove) {
		if (!_vectors.remove(toRemove))
			return false;
		for (Item i : toRemove.Frame.getVectorItems()) {
			i.invalidateAll();
			i.invalidateFill();
			_vectorItems.remove(i);
			i.onParentStateChanged(new ItemParentStateChangedEvent(this,
					ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY,
					toRemove.permission));

		}
		return true;
	}

	public void clearOverlays() {
		for (Overlay o : _overlays.keySet()) {
			for (Item i : o.Frame.getItems()) {
				i
						.onParentStateChanged(new ItemParentStateChangedEvent(
								this,
								ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY,
								o.permission));
			}
		}
		_overlayItems.clear();
		_overlays.clear();
		assert (_overlays.isEmpty());
	}

	protected boolean removeOverlay(Frame f) {
		for (Overlay o : _overlays.keySet()) {
			if (o.Frame == f) {
				_overlays.remove(o);
				for (Item i : f.getItems()) {
					_overlayItems.remove(i);
					i
							.onParentStateChanged(new ItemParentStateChangedEvent(
									this,
									ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY,
									o.permission));
				}
				return true;
			}
		}
		return false;
	}

	public void addAllVectors(List<Vector> vectors) {
		for (Vector v : vectors) {
			addVector(v);
		}
	}

	public void addAllOverlays(Collection<Overlay> overlays) {
		for (Overlay o : overlays) {
			addOverlay(o);
		}
	}

	protected boolean addOverlay(Overlay toAdd) {
		// make sure we dont add this frame as an overlay of itself
		if (toAdd.Frame == this)
			return false;
		// Dont add the overlay if there is already one for this frame
		if (_overlays.values().contains(toAdd.Frame))
			return false;
		// Add the overlay to the map of overlays on this frame
		_overlays.put(toAdd, toAdd.Frame);
		// Add all the overlays from the overlay frame to this frame
		for (Overlay o : toAdd.Frame.getOverlays())
			addOverlay(o);

		// Add all the vectors from the overlay frame to this frame
		for (Vector v : toAdd.Frame.getVectors())
			addVector(v);

		// Now add the items for this overlay
		UserAppliedPermission permission = UserAppliedPermission.min(toAdd.Frame.getUserAppliedPermission(),toAdd.permission);

		// Items must be notified that they have been added or removed from this
		// frame via the overlay...
		for (Item i : toAdd.Frame.getVisibleItems()) {
			i.onParentStateChanged(new ItemParentStateChangedEvent(this,
					ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY,
					permission));
			// i.setPermission(permission);
			_overlayItems.add(i);
		}

		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof String) {
			return (String.CASE_INSENSITIVE_ORDER
					.compare((String) o, getName()) == 0);
		}

		if (o instanceof Frame) {
			return getName().equals(((Frame) o).getName());
		}

		return super.equals(o);
	}

	/**
	 * Merge one frames contents into another.
	 * 
	 * @param toMergeWith
	 */
	private void merge(Frame toMergeWith) {
		if (toMergeWith == null)
			return;

		List<Item> copies = ItemUtils.CopyItems(toMergeWith.getItems());
		copies.remove(toMergeWith.getNameItem());

		for (Item i : copies) {
			if (i.getID() >= 0) {
				i.setID(this.getNextItemID());
				addItem(i);
			}
		}
	}

	/**
	 * This method is for merging frames or setting frame attributes via
	 * injecting a text item into the frameName item.
	 * 
	 * @param toMerge
	 * @return the items that cant be merged
	 */
	public List<Item> merge(List<Item> toMerge) {
		ArrayList<Item> remain = new ArrayList<Item>(0);

		for (Item i : toMerge) {
			if (!(i instanceof Text))
				remain.add(i);
			else {
				if (!AttributeUtils.setAttribute(this, (Text) i)) {
					if (i.getLink() != null)
						merge(FrameIO.LoadFrame(i.getAbsoluteLink()));
					else if (FrameIO
							.isValidFrameName(((Text) i).getFirstLine())) {
						// If we get hear we are merging frames
						merge(FrameIO.LoadFrame(((Text) i).getFirstLine()));
					}
				}
			}
		}

		return remain;
	}

	/**
	 * Removes all non-title non-annotation items from this Frame. All removed
	 * items are added to the backup-stack.
	 */
	public void clear(boolean keepAnnotations) {
		List<Item> newBody = new ArrayList<Item>(0);
		Item title = getTitleItem();
		if (title != null) {
			newBody.add(title);
			_body.remove(title);
		}
		if (keepAnnotations) {
			for (Item i : _body) {
				if (i.isAnnotation())
					newBody.add(i);
			}
		}
		_body.removeAll(newBody);
		addToUndoDelete(_body);
		_body = newBody;
		change();

		if (!keepAnnotations && _annotations != null)
			_annotations.clear();
	}

	/**
	 * Creates a new text item with the given text.
	 * 
	 * @param text
	 * @return
	 */
	public Text createNewText(String text) {
		Text t = createBlankText(text);
		t.setText(text);
		return t;
	}

	/**
	 * Creates a new Text Item with no text. The newly created Item is a copy
	 * the ItemTemplate if one is present, and inherits all the attributes of
	 * the Template
	 * 
	 * @return The newly created Text Item
	 */
	public Text createBlankText(String templateType) {
		SessionStats.CreatedText();
		Text t;
		if (templateType.length() == 0)
			t = getItemTemplate().copy();
		else
			t = getItemTemplate(templateType.charAt(0));

		// reset attributes
		t.setID(getNextItemID());
		t.setPosition(DisplayIO.getMouseX(), FrameMouseActions.getY());
		t.setText("");
		t.setParent(this);

		// Set the width if the template doesnt have a width
		// Make it the width of the page
		// t.setMaxWidth(FrameGraphics.getMaxFrameSize().width);
		// if (t.getWidth() <= 0) {
		// String maxWidthString = getAnnotationValue("maxwidth");
		// int width = FrameGraphics.getMaxFrameSize().width;
		// if (maxWidthString != null) {
		// try {
		// width = Math.min(width, Integer.parseInt(maxWidthString));
		// } catch (NumberFormatException nfe) {
		// }
		// }
		//
		// t.setRightMargin(width);
		// }
		addItem(t);
		return t;
	}

	public Item createDot() {
		Item dot = new Dot(DisplayIO.getMouseX(), FrameMouseActions.getY(),
				getNextItemID());

		Item template = getTemplate(_dotTemplate, ItemUtils.TAG_DOT_TEMPLATE);
		float thickness = template.getThickness();
		if (thickness > 0)
			dot.setThickness(template.getThickness());
		if (template.getLinePattern() != null)
			dot.setLinePattern(template.getLinePattern());
		dot.setColor(template.getColor());
		dot.setFillColor(template.getFillColor());
		// reset attributes
		dot.setParent(this);
		return dot;
	}

	private Text getTemplate(Text defaultTemplate, int templateTag) {
		Text t = null;

		// check for an updated template...
		for (Item i : this.getItems()) {
			if (ItemUtils.startsWithTag(i, templateTag)) {
				t = (Text) i;
				break;
			}
		}

		if (t == null) {
			if (defaultTemplate == null) {
				return null;
			}
			t = defaultTemplate;
		}

		// If the item is linked apply any attribute pairs on the child frame
		String link = t.getAbsoluteLink();
		// need to get link first because copy doesnt copy the link
		t = t.copy();
		t.setTooltip(null);
		if (link != null) {
			t.setLink(null);
			Frame childFrame = FrameIO.LoadFrame(link);
			if (childFrame != null) {
				// read in attribute value pairs
				for (Text attribute : childFrame.getBodyTextItems(false)) {
					AttributeUtils.setAttribute(t, attribute);
				}
			}
		}
		return t;
	}

	public Text getItemTemplate(char firstChar) {
		switch (firstChar) {
		case '@':
			return getAnnotationTemplate();
		case '/':
		case '#':
			return getCodeCommentTemplate();
		default:
			return getItemTemplate();
		}
	}

	public Text createNewText() {
		return createNewText("");
	}

	public Text addText(int x, int y, String text, String action) {
		Text t = createNewText(text);
		t.setPosition(x, y);
		t.addAction(action);
		return t;
	}

	public Item addText(int x, int y, String text, String action, String link) {
		Item t = addText(x, y, text, action);
		t.setLink(link);
		return t;
	}

	public Item addDot(int x, int y) {
		Item d = new Dot(x, y, getNextItemID());
		addItem(d);
		return d;
	}

	/**
	 * Adds a rectangle to the frame
	 * 
	 * @param x
	 *            X coordinate of the top-left corner of the rectangle
	 * @param y
	 *            Y coordinate of the top-left corner of the rectangle
	 * @param width
	 *            Width of the rectangle
	 * @param height
	 *            Height of the rectangle
	 * @param borderThickness
	 *            Thickness, in pixels, of the rectangle's border/outline
	 * @param borderColor
	 *            Color of the rectangle's border/outline
	 * @param fillColor
	 *            Color to fill the rectangle with
	 */
	public List<Item> addRectangle(int x, int y, int width, int height, float borderThickness, Color borderColor, Color fillColor) {
		List<Item> rectComponents = new ArrayList<Item>();
		Item[] corners = new Item[4];

		// Top Left
		corners[0] = this.createDot();
		corners[0].setPosition(x, y);

		// Top Right
		corners[1] = this.createDot();
		corners[1].setPosition(x + width, y);

		// Bottom Right
		corners[2] = this.createDot();
		corners[2].setPosition(x + width, y + height);

		// Bottom Left
		corners[3] = this.createDot();
		corners[3].setPosition(x, y + height);

		// Add corners to the collection and setting their attributes
		for (int i = 0; i < corners.length; i++) {
			corners[i].setThickness(borderThickness);
			corners[i].setColor(borderColor);
			corners[i].setFillColor(fillColor);
			rectComponents.add(corners[i]);
		}

		// create lines between the corners
		rectComponents.add(new Line(corners[0], corners[1], this.getNextItemID()));
		rectComponents.add(new Line(corners[1], corners[2], this.getNextItemID()));
		rectComponents.add(new Line(corners[2], corners[3], this.getNextItemID()));
		rectComponents.add(new Line(corners[3], corners[0], this.getNextItemID()));

		// Add constraints between each corner
		new Constraint(corners[0], corners[1], this.getNextItemID(), Constraint.HORIZONTAL);
		new Constraint(corners[2], corners[3], this.getNextItemID(), Constraint.HORIZONTAL);
		new Constraint(corners[1], corners[2], this.getNextItemID(), Constraint.VERTICAL);
		new Constraint(corners[3], corners[0], this.getNextItemID(), Constraint.VERTICAL);

		List<Item> rect = new ArrayList<Item>(rectComponents);
		this.addAllItems(rectComponents);
		FrameMouseActions.anchor(rectComponents);
		return rect;
		// rectComponents.clear();
	}

	public boolean isSaved() {
		return _saved;
	}

	public void setSaved() {
		// System.out.println(getName() + " saved");
		_saved = true;
		_change = false;
	}

	public static boolean rubberbandingLine() {
		return FreeItems.getInstance().size() == 2
				&& (FreeItems.getInstance().get(0) instanceof Line || FreeItems
						.getInstance().get(1) instanceof Line);
	}

	/**
	 * Tests if an item is a non title, non frame name, non special annotation
	 * text item.
	 * 
	 * @param it
	 *            the item to be tested
	 * @return true if the item is a normal text item
	 */
	public boolean isNormalTextItem(Item it) {
		if (it instanceof Text && it != getTitleItem() && it != _frameName
				&& !((Text) it).isSpecialAnnotation()) {
			return true;
		}

		return false;
	}

	/**
	 * Moves the mouse to the end of the text item with a specified index.
	 * 
	 * @param index
	 */
	public boolean moveMouseToTextItem(int index) {
		List<Item> items = getItems();
		int itemsFound = 0;
		for (int i = 0; i < items.size(); i++) {
			Item it = items.get(i);
			if (isNormalTextItem(it))
				itemsFound++;
			if (itemsFound > index) {
				DisplayIO.setCursorPosition(((Text) it)
						.getParagraphEndPosition().x, it.getY());
				DisplayIO.resetCursorOffset();
				FrameGraphics.Repaint();
				return true;
			}
		}

		return false;
	}

	/*
	 * public boolean moveMouseToNextTextItem(int index) { List<Item> items =
	 * getItems(); int itemsFound = 0; for (int i = 0; i < items.size(); i++) {
	 * Item it = items.get(i); if ( isNormalTextItem(it)) itemsFound++; if
	 * (itemsFound > index) {
	 * DisplayIO.setCursorPosition(((Text)it).getEndParagraphPosition().x,
	 * it.getY()); DisplayIO.resetCursorOffset(); FrameGraphics.Repaint();
	 * return true; } }
	 * 
	 * return false; }
	 */

	/**
	 * Searches for an annotation item called start to be used as the default
	 * cursor location when TDFC occurs.
	 */
	public boolean moveMouseToDefaultLocation() {
		List<Item> items = getItems();

		for (Item it : items) {
			if (it instanceof Text) {
				Text t = (Text) it;
				if (t.getText().toLowerCase().startsWith("@start")
						|| t.getText().toLowerCase().equals("@start:")) {
					// Used to allow users the option of putting an initial
					// bullet after the @start
					// This was replaced by width
					// t.stripFirstWord();
					t.setText("");

					if (t.getText().equals(""))
						DisplayIO.getCurrentFrame().removeItem(t);
					if (!FreeItems.itemsAttachedToCursor()) {
						DisplayIO.setCursorPosition(((Text) it)
								.getParagraphEndPosition());
						DisplayIO.resetCursorOffset();
					}
					FrameGraphics.Repaint();
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Gets the file name that actions should use to export files created by
	 * running actions from this frame.
	 * 
	 * @return the fileName if the frame contains an '@file' tag. Returns the
	 *         name of the frame if the tag isnt on the frame.
	 */
	public String getExportFileName() {
		String fileName = getExportFileTagValue();

		if (fileName == null) {
			fileName = getTitle();

			if (fileName == null) {
				fileName = getName();
			}
		}

		return fileName;
	}

	public void toggleBackgroundColor() {
		setBackgroundColor(ColorUtils.getNextColor(_background,
				TemplateSettings.BackgroundColorWheel.get(), null));
	}

	public void setName(String frameset, int i) {
		setFrameset(frameset);
		setFrameNumber(i);
	}

	/**
	 * Sets the item permissions to match the protection for the frame.
	 * No longer sets item permissions, since items can have their own permissions now (but still default to frame permissions) 
	 * 
	 */
	public void refreshItemPermissions(UserAppliedPermission maxPermission) {
		if(_frameName == null)
			return;
		
		UserAppliedPermission permission = UserAppliedPermission.min(maxPermission, getUserAppliedPermission());

		switch (permission) {
		case none:
			_frameName.setBackgroundColor(new Color(255, 220, 220));
			break;
		case followLinks:
			_frameName.setBackgroundColor(new Color(255, 230, 135));
			break;
		case copy:
			_frameName.setBackgroundColor(new Color(255, 255, 155));
			break;
		case createFrames:
			_frameName.setBackgroundColor(new Color(220, 255, 220));
			break;
		case full:
			_frameName.setBackgroundColor(null);
			break;
		default:
			assert (false);
			break;
		}

		for (Overlay o : getOverlays()) {
			for(Item i : o.Frame._body) {
				i.setOverlayPermission(o.permission);
			}
			o.Frame.refreshItemPermissions(o.permission);
		}
	}

	public boolean isTestFrame() {
		Text title = getTitleItem();
		if (title == null)
			return false;
		String action = title.getFirstAction();
		if (action == null)
			return false;
		action = action.toLowerCase();
		return action.startsWith(Simple.RUN_FRAME_ACTION)
				|| action.startsWith(Simple.DEBUG_FRAME_ACTION);
	}

	public void setActiveTime(String activeTime) {
		try {
			_activeTime = new Time(Time.valueOf(activeTime).getTime() + 12 * 60
					* 60 * 1000);
		} catch (Exception e) {
			_activeTime = new Time(0);
		}
	}

	public void setActiveTime(Time activeTime) {
		_activeTime = activeTime;
	}

	public void setDarkTime(Time darkTime) {
		_darkTime = darkTime;
	}

	public void setDarkTime(String darkTime) {
		try {
			_darkTime = new Time(Time.valueOf(darkTime).getTime() + 12 * 60
					* 60 * 1000);
		} catch (Exception e) {
			_darkTime = new Time(0);
		}
	}

	/**
	 * Returns null if their is no backup frame or if it is invalid.
	 * 
	 * @return the backup frame for this frame
	 */
	public Frame getBackupFrame() {
		Text backupTag = _annotations.get("old");
		if (backupTag == null)
			return null;
		// TODO want another way to deal with updating of annotations items
		// without F12 refresh
		// Reparse the frame if annotation item has been modified
		String[] processedText = backupTag.getProcessedText();
		if (processedText == null) {
			// Reparse the frame if this item has not yet been parsed
			FrameUtils.Parse(this);
			return getBackupFrame();
		}
		// Now return the name of the backed up frame
		String link = backupTag.getAbsoluteLink();
		if (link == null || link.equalsIgnoreCase(getName()))
			return null;

		Frame backup = FrameIO.LoadFrame(link);
		return backup;
	}

	public Time getDarkTime() {
		return _darkTime;
	}

	public Time getActiveTime() {
		return _activeTime;
	}

	/**
	 * Gets the number of backed up versions of this frame are saved plus 1 for
	 * this frame.
	 * 
	 * @return the number of frames in the backed up comet
	 */
	public int getCometLength() {
		Frame backup = getBackupFrame();
		return 1 + (backup == null ? 0 : backup.getCometLength());
	}

	public void addAnnotation(Text item) {
		if (_annotations == null) {
			_annotations = new HashMap<String, Text>();
		}
		// Check if this item has already been processed
		String[] tokens = item.getProcessedText();
		if (tokens != null) {
			if (tokens.length > 0) {
				_annotations.put(tokens[0], item);
			}
			return;
		}

		String text = item.getText().trim();
		assert (text.charAt(0) == '@');
		// Ignore annotations with spaces after the tag symbol
		if (text.length() < 2 || !Character.isLetter(text.charAt(1))) {
			item.setProcessedText(new String[0]);
			return;
		}
		// The separator char must come before the first non letter otherwise we
		// ignore the annotation item
		for (int i = 2; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (!Character.isLetterOrDigit(ch)) {
				// Must have an attribute value pair
				if (ch == AttributeValuePair.SEPARATOR_CHAR) {
					// Get the attribute
					String attribute = text.substring(1, i).toLowerCase();
					String value = "";
					if (text.length() > 1 + i) {
						value = text.substring(i + 1).trim();
					}
					item.setProcessedText(new String[] { attribute, value });
					_annotations.put(attribute, item);
					return;
				} else {
					item.setProcessedText(new String[0]);
					return;
				}
			}
		}
		// If it was nothing but letters and digits save the tag
		String lowerCaseText = text.substring(1).toLowerCase();
		item.setProcessedText(new String[] { lowerCaseText });
		_annotations.put(lowerCaseText, item);
	}

	public boolean hasAnnotation(String annotation) {
		if (_annotations == null)
			refreshAnnotationList();
		return _annotations.containsKey(annotation.toLowerCase());
	}

	/**
	 * Returns the annotation value in full case.
	 * 
	 * @param annotation
	 *            the annotation to retrieve the value of.
	 * @return the annotation item value in full case or null if the annotation
	 *         is not on the frame or has no value.
	 */
	public String getAnnotationValue(String annotation) {
		if (_annotations == null)
			refreshAnnotationList();

		Text text = _annotations.get(annotation.toLowerCase());
		if (text == null)
			return null;

		String[] tokens = text.getProcessedText();
		if (tokens != null && tokens.length > 1)
			return tokens[1];
		return null;
	}

	Map<String, Text> _annotations = null;

	private Collection<FrameObserver> _observers = new HashSet<FrameObserver>();

	public void clearAnnotations() {
		_annotations = null;
	}

	public List<Item> getVisibleItems() {
		return getItems(true);
	}

	private void refreshAnnotationList() {
		if (_annotations == null)
			_annotations = new HashMap<String, Text>();
		else
			_annotations.clear();
		for (Text text : getTextItems()) {
			if (text.isAnnotation()) {
				addAnnotation(text);
			}
		}
	}

	public Collection<Text> getAnnotationItems() {
		if (_annotations == null) {
			refreshAnnotationList();
		}
		return _annotations.values();
	}

	/**
	 * Gets a list of items to be saved to file by text file writers.
	 * 
	 * @return the list of items to be saved to a text file
	 */
	public List<Item> getItemsToSave() {

		if (!_sorted) {
			Collections.sort(_body);
			_sorted = true;
		}

		// iWidgets are handled specially since 8 items are written as one
		Collection<InteractiveWidget> seenWidgets = new LinkedHashSet<InteractiveWidget>();

		List<Item> toSave = new ArrayList<Item>();

		for (Item i : _body) {
			if (i == null || i.dontSave()) {
				continue;
			}

			// Ensure only one of the WidgetCorners represent a single widget
			if (i instanceof WidgetCorner) {
				InteractiveWidget iw = ((WidgetCorner) i).getWidgetSource();
				if (seenWidgets.contains(iw))
					continue;
				seenWidgets.add(iw);
				toSave.add(iw.getSource());
			} else if (i instanceof XRayable) {
				XRayable x = (XRayable) i;
				toSave.addAll(x.getItemsToSave());
			}// Circle centers are items with attached enclosures
			else if (i.hasEnclosures()) {
				continue;
			} else {
				toSave.add(i);
			}
		}

		for (Vector v : getVectors()) {
			toSave.add(v.Source);
		}

		return toSave;
	}

	public Collection<Item> getOverlayItems() {
		return _overlayItems;
	}

	/**
	 * Returns true if this frame has and overlays for the specified frame.
	 * 
	 * @param frame
	 * @return
	 */
	public boolean hasOverlay(Frame frame) {
		return _overlays.containsValue(frame);
	}

	public Collection<Item> getAllItems() {
		Collection<Item> allItems = new LinkedHashSet<Item>(_body);
		allItems.addAll(_overlayItems);
		allItems.addAll(_vectorItems);
		return allItems;
	}

	public Collection<Item> getVectorItems() {
		Collection<Item> vectorItems = new LinkedHashSet<Item>(_vectorItems);
		vectorItems.addAll(getNonAnnotationItems(false));
		return vectorItems;
	}

	/**
	 * Gets a list of all the text items on the frame.
	 * 
	 * @return
	 */
	public Collection<Text> getTextItems() {
		Collection<Text> textItems = new ArrayList<Text>();
		for (Item i : getItems(true)) {
			// only add up normal body text items
			if ((i instanceof Text)) {
				textItems.add((Text) i);
			}
		}
		return textItems;
	}

	public Text getAnnotation(String annotation) {
		if (_annotations == null)
			refreshAnnotationList();

		return _annotations.get(annotation.toLowerCase());
	}

	public void recalculate() {

		for (Item i : getItems()) {
			if (i.hasFormula() && !i.isAnnotation()) {
				i.calculate(i.getFormula());
			}
		}
	}

	public void removeObserver(FrameObserver observer) {
		_observers.remove(observer);
	}

	public void addObserver(FrameObserver observer) {
		_observers.add(observer);
	}

	public void clearObservers() {
		for (FrameObserver fl : _observers) {
			fl.removeSubject(this);
		}
		// The frame listener will call the frames removeListener method
		assert (_observers.size() == 0);
	}

	public Collection<Text> getNonAnnotationText(boolean removeTitle) {
		Collection<Text> items = new LinkedHashSet<Text>();
		for (Item i : getItems(true)) {
			// only add up normal body text items
			if (i instanceof Text && !i.isAnnotation()) {
				items.add((Text) i);
			}
		}
		if (removeTitle) {
			items.remove(getTitleItem());
		}
		return items;
	}

	public void dispose() {
		clearObservers();
		for (Item i : _body) {
			i.dispose();
		}
		_frameName.dispose();
		_body = null;
		_frameName = null;
	}

	public void parse() {
		for (Overlay o : getOverlays()) {
			o.Frame.parse();
		}
		// Must parse the frame AFTER the overlays
		FrameUtils.Parse(this);
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void setLocal(boolean isLocal) {
		this._isLocal = isLocal;
	}

	public boolean isLocal() {
		return _isLocal;
	}

	public String getExportFileTagValue() {
		return getAnnotationValue("file");
	}

	public void assertEquals(Frame frame2) {
		// Check that all the items on the frame are the same
		List<Item> items1 = getVisibleItems();
		List<Item> items2 = frame2.getVisibleItems();
		if (items1.size() != items2.size()) {
			throw new UnitTestFailedException(items1.size() + " items", items2
					.size()
					+ " items");
		} else {
			for (int i = 0; i < items1.size(); i++) {
				Item i1 = items1.get(i);
				Item i2 = items2.get(i);
				String s1 = i1.getText();
				String s2 = i2.getText();
				if (!s1.equals(s2)) {
					throw new UnitTestFailedException(s1, s2);
				}
			}
		}
	}

	public boolean hasObservers() {
		return _observers != null && _observers.size() > 0;
	}

	public Collection<? extends Item> getInteractableItems() {
		/*
		 * TODO: Cache the interactableItems list so we dont have to recreate it
		 * every time this method is called
		 */
		if (_interactableItems.size() > 0)
			return _interactableItems;

		for (Item i : _body) {
			if (i == null) {
				continue;
			}
			if (i.isVisible()) {
				_interactableItems.add(i);
			}
		}

		for (Item i : _overlayItems) {
			if (i.hasPermission(UserAppliedPermission.followLinks)) {
				_interactableItems.add(i);
			}
		}

		for (Item i : _vectorItems) {
			if (i.hasPermission(UserAppliedPermission.none)) {
				_interactableItems.add(i);
			}
		}

		return _interactableItems;
	}
}
