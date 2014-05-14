package org.expeditee.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.image.VolatileImage;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.actions.Misc;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

/**
 * The bay at the bottom of the expeditee browser which displays messages. TODO
 * make it thread safe!
 * 
 */
public final class MessageBay {


	public static final int MESSAGE_BUFFER_HEIGHT = 105;

	private static final int MESSAGE_LINK_Y_OFFSET = 100;

	private static final int MESSAGE_LINK_X = 50;

	public static final Color ERROR_COLOR = Color.red;

	public static final String MESSAGES_FRAMESET_NAME = "Messages";

	// messages shown in the message window
	private static List<Item> _messages = new LinkedList<Item>();
	private static Text _status = null;

	// buffer of the message window
	private static VolatileImage _messageBuffer = null;

	// creator for creating the message frames
	private static FrameCreator _creator = null;

	// font used for the messages
	private static Font _messageFont = Font.decode("Serif-Plain-16");

	// the number of messages currently shown (used for scrolling up)
	private static int _messageCount = 0;

	// if true, error messages are not shown to the user
	private static boolean _suppressMessages = false;

	// The link to the message frameset
	private static Item _messageLink = new Text(-2, "@"
			+ MESSAGES_FRAMESET_NAME, Color.black, Color.white);

	private static List<Rectangle> _dirtyAreas = new LinkedList<Rectangle>();

	private static String _lastMessage = null;

	private MessageBay() {
	}

	/**
	 * Syncs messsage bay size according to FrameGraphics max size.
	 * 
	 */
	static void updateSize() {

		_messageBuffer = null;

		for(Item i : _messages) {
			if(i != null) {
				i.setOffset(0, -FrameGraphics.getMaxFrameSize().height);
				// i.setMaxWidth(FrameGraphics.getMaxFrameSize().width);
			}
		}

		_messageLink.setOffset(0, -FrameGraphics.getMaxFrameSize().height);
		// _messageLink.setMaxWidth(FrameGraphics.getMaxFrameSize().width);
		// _messageLink.setPosition(FrameGraphics.getMaxFrameSize().width
		// - MESSAGE_LINK_Y_OFFSET, MESSAGE_LINK_X);
		updateLink();
		initBuffer();
	}

	/**
	 * @param i
	 * @return True if i is an item in the message bay
	 */
	public static boolean isMessageItem(Item i) {
		
		return _messages.contains(i) || i == _messageLink;
	}

	public synchronized static void addDirtyArea(Rectangle r) {
		_dirtyAreas.add(r);
	}

	static synchronized int getMessageBufferHeight() {
		if (_messageBuffer != null)
			return _messageBuffer.getHeight();
		return 0;
	}

	public synchronized static Item getMessageLink() {
		return _messageLink;
	}

	public synchronized static List<Item> getMessages() {
		return _messages;
	}

	public synchronized static boolean isDirty() {
		return !_dirtyAreas.isEmpty();
	}

	public synchronized static void invalidateFullBay() {
		if (_messageBuffer != null) {
			_dirtyAreas.clear();
			addDirtyArea(new Rectangle(0,
					FrameGraphics.getMaxFrameSize().height, _messageBuffer
							.getWidth(), _messageBuffer.getHeight()));
		}
	}

	private synchronized static boolean initBuffer() {
		if (_messageBuffer == null) {
			if (FrameGraphics.isAudienceMode()
					|| FrameGraphics.getMaxSize().width <= 0)
				return false;

			GraphicsEnvironment ge = GraphicsEnvironment
					.getLocalGraphicsEnvironment();
			_messageBuffer = ge.getDefaultScreenDevice()
					.getDefaultConfiguration().createCompatibleVolatileImage(
							FrameGraphics.getMaxSize().width,
							MESSAGE_BUFFER_HEIGHT);
		}
		return true;
	}

	private static boolean isLinkInitialized = false;

	private static void updateLink() {

		if (!isLinkInitialized && FrameGraphics.getMaxSize().width > 0) {
			// set up 'Messages' link on the right hand side
			_messageLink.setPosition(FrameGraphics.getMaxSize().width
					- MESSAGE_LINK_Y_OFFSET, MESSAGE_LINK_X);
			_messageLink.setOffset(0, -FrameGraphics.getMaxFrameSize().height);
			isLinkInitialized = true;

		} else {
			_messageLink.setPosition(FrameGraphics.getMaxSize().width
					- MESSAGE_LINK_Y_OFFSET, MESSAGE_LINK_X);
		}
	}

	/**
	 * Repaints the message bay. Updates the message bay buffer and draws to
	 * given graphics.
	 * 
	 * @param useInvalidation
	 *            Set to true of repinting dirty areas only. Otherwise false for
	 *            full-repaint.
	 * 
	 * @param g
	 * 
	 * @param background
	 *            The color of the message background
	 */
	public static synchronized void refresh(boolean useInvalidation,
			Graphics g, Color background) {

		if(g == null)
			return;
		
		if (FrameGraphics.getMaxSize().width <= 0)
			return;

		Area clip = null;

		if (useInvalidation) { // build clip

			if (!_dirtyAreas.isEmpty()) {

				for (Rectangle r : _dirtyAreas) {
					r.y = (r.y < 0) ? 0 : r.y;
					r.x = (r.x < 0) ? 0 : r.x;
					if (clip == null)
						clip = new Area(r);
					else
						clip.add(new Area(r));
				}
			} else
				return; // nothing to render
		}

		_dirtyAreas.clear();

		// Update the buffer
		updateBuffer(background, clip);

		// Now repaint to screen
		if (!FrameGraphics.isAudienceMode()) {

			// Translate clip to messagebox coords
			// clip.transform(t) // TODO
			// g.setClip(clip);

			g.drawImage(_messageBuffer, 0, FrameGraphics.getMaxFrameSize().height, null);
		}

	}

	private static void updateBuffer(Color background, Area clip) {
		if (!initBuffer())
			return;

		Graphics2D g = _messageBuffer.createGraphics();

		g.setClip(clip);

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(background);
		g.fillRect(0, 0, FrameGraphics.getMaxSize().width,
				MESSAGE_BUFFER_HEIGHT);
		g.setFont(_messageFont);
		g.setColor(Color.BLACK);
		g.drawLine(0, 0, FrameGraphics.getMaxSize().width, 0);

		for (Item t : _messages) {
			if (t == null)
				continue;
			if (clip == null || t.isInDrawingArea(clip))
				FrameGraphics.PaintItem(g, t);
		}
		if(_status != null)
			FrameGraphics.PaintItem(g, _status);

		if (// _messageLink.getLink() != null
		// &&
		(clip == null || _messageLink.isInDrawingArea(clip))) {
			FrameGraphics.PaintItem(g, _messageLink);
		}
		g.dispose();

	}

	private static Text displayMessage(String message, String link,
			List<String> actions, Color color) {
		return displayMessage(message, link, actions, color, true);
	}

	public synchronized static Text displayMessage(String message, String link, Color color,
			boolean displayAlways, String action) {
		List<String> actions = new LinkedList<String>();
		if (action != null)
			actions.add(action);
		return displayMessage(message, link, actions, color, displayAlways);
	}
	
	private static Text newMessage(String message, String link, List<String> actions, Color color) {
		Text t = new Text(getMessagePrefix(true) + message);
		t.setPosition(20, 15 + _messages.size() * 25);
		t.setOffset(0, -FrameGraphics.getMaxFrameSize().height);
		t.setColor(color);
		t.setLink(link);
		t.setActions(actions);
		t.setFont(_messageFont);
		_creator.addItem(t.copy(), true);
		if(link == null) t.setLink(_creator.getCurrent());
		return t;
	}
	
	private synchronized static Text displayMessage(String message, String link,
			List<String> actions, Color color, boolean displayAlways, boolean redraw) {
		System.out.println(message);
		assert (message != null);

		// Invalidate whole area
		invalidateFullBay();

		if (_suppressMessages)
			return null;

		if (!displayAlways && message.equals(_lastMessage)) {
			Misc.beep();
			return null;
		}
		_lastMessage = message;

		if (_creator == null) {
			_creator = new FrameCreator(MESSAGES_FRAMESET_NAME,
					FrameIO.MESSAGES_PATH, MESSAGES_FRAMESET_NAME, true, false);
		}

		// set up 'Messages' link on the right hand side
		updateLink();

		if(_messages.size() >= 3) {
			_messages.remove(0);
			for(Item i : _messages) {
				i.setY(i.getY() - 25);
			}
		}
		
		Text t = newMessage(message, link, actions, color);
		
		_messages.add(t);
		
		// update the link to the latest message frame
		_messageLink.setLink(_creator.getCurrent());

		if(redraw) {
    		Graphics g = FrameGraphics.createGraphics();
    		if (g != null) {
    		    refresh(false, g, Item.DEFAULT_BACKGROUND);
    		}
		}

		return t;
	}

	public synchronized static Text displayMessage(String message, String link,
			List<String> actions, Color color, boolean displayAlways) {
		return displayMessage(message, link, actions, color, displayAlways, true);
	}

	public synchronized static void overwriteMessage(String message) {
		overwriteMessage(message, null);
	}

	public synchronized static void overwriteMessage(String message, Color color) {
		_messages.remove(_messages.size() - 1);
		Text t = newMessage(message, null, null, color);
		_messages.add(t);
		Graphics g = FrameGraphics.createGraphics();
		if (g != null) {
		    refresh(false, g, Item.DEFAULT_BACKGROUND);
		}
	}
	
	private static String getMessagePrefix(int counter) {
		return "@" + counter + ": ";
	}

	private static String getMessagePrefix(boolean incrementCounter) {
		if (incrementCounter)
			_messageCount++;
		return getMessagePrefix(_messageCount);
	}

	/**
	 * Checks if the error message ends with a frame name after the
	 * frameNameSeparator symbol
	 * 
	 * @param message
	 *            the message to be displayed
	 */
	public synchronized static Text linkedErrorMessage(String message) {
		if (_suppressMessages)
			return null;
		Misc.beep();
		String[] tokens = message.split(Text.FRAME_NAME_SEPARATOR);
		String link = null;
		if (tokens.length > 1)
			link = tokens[tokens.length - 1];
		return displayMessage(message, link, null, ERROR_COLOR);
	}

	public synchronized static Text errorMessage(String message) {
		if (_suppressMessages)
			return null;
		Misc.beep();
		return displayMessage(message, null, null, ERROR_COLOR, false);
	}

	/**
	 * Displays the given message in the message area of the Frame, any previous
	 * message is cleared from the screen.
	 * 
	 * @param message
	 *            The message to display to the user in the message area
	 */
	public synchronized static Text displayMessage(String message) {
		return displayMessageAlways(message);
	}

	public synchronized static Text displayMessageOnce(String message) {
		return displayMessage(message, null, null, Color.BLACK, false);
	}

	public synchronized static Text displayMessage(String message, Color textColor) {
		return displayMessage(message, null, null, textColor);
		// Misc.Beep();
	}

	public synchronized static Text displayMessage(Text message) {
		Text t = null;
		String link = message.getLink();
		List<String> action = message.getAction();
		Color color = message.getColor();
		for (String s : message.getTextList()) {
			t = displayMessage(s, link, action, color);
		}
		return t;
		// Misc.Beep();
	}

	public synchronized static Text displayMessageAlways(String message) {
		return displayMessage(message, null, null, Color.BLACK);
		// Misc.Beep();
	}

	public synchronized static Text warningMessage(String message) {
		return displayMessage(message, null, null, Color.MAGENTA);
		// Misc.Beep();
	}

	public synchronized static void suppressMessages(boolean val) {
		_suppressMessages = val;
	}
	
	public synchronized static void setStatus(String status) {
		if (_status == null) {
			_status = new Text(status);
			_status.setPosition(0, 85);
			_status.setOffset(0, FrameGraphics.getMaxFrameSize().height);
			_status.setLink(null); // maybe link to a help frame?
			_status.setFont(Font.decode(Text.MONOSPACED_FONT));
		} else {
			_status.setText(status);
		}
		Graphics g = FrameGraphics.createGraphics();
		if (g != null) {
			refresh(false, g, Item.DEFAULT_BACKGROUND);
		}
	}
	
	public static final class Progress {
		
		private static final String filled = "\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592";
		private static final String unfilled = "\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591";
		
		String message;
		Text text;
		
		protected Progress(String text) {
			this.text = displayMessage(text, null, null, Color.GREEN.darker(), true, false);
			//this.text.setFont(Font.decode(Text.MONOSPACED_FONT + "-16"));
			this.message = this.text.getText();
			this.text.setText(this.message + " [" + unfilled + "] 0%");
			refresh(false, FrameGraphics.createGraphics(), Item.DEFAULT_BACKGROUND);
		}
		
		/**
		 * 
		 * @param progress progress value from 0 to 100
		 * @return true if the progress was updated, false if the progress was off the screen
		 * @throws Exception if progress out of bounds
		 */
		public boolean set(int progress) throws Exception {
			if(progress < 0 || progress > 100) throw new Exception("Progress value out of bounds");
			int p = progress / 5;
			if(isMessageItem(this.text)) {
				this.text.setText(this.message + " [" + filled.substring(0, p) + unfilled.substring(p) + "] " + progress + "%");
				refresh(false, FrameGraphics.createGraphics(), Item.DEFAULT_BACKGROUND);
				return true;
			}
    		return false;
    	}
	}
	
	public synchronized static Progress displayProgress(String message) {
		return new Progress(message);
	}

}