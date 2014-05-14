package org.expeditee.items;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.AttributedString;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameKeyboardActions;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.FreeItems;
import org.expeditee.math.ExpediteeJEP;
import org.expeditee.settings.experimental.ExperimentalFeatures;
import org.nfunk.jep.Node;

/**
 * Represents text displayed on the screen, which may include multiple lines.
 * All standard text properties can be set (font, style, size).
 * 
 * @author jdm18
 * 
 */
public class Text extends Item {
	private static final int ADJUST_WIDTH_THRESHOLD = 200;

	public static String LINE_SEPARATOR = System.getProperty("line.separator");

	// public static char[] BULLETS = { '\u2219', '\u2218', '\u2217' };
	public static char[] BULLETS = { '\u25AA', '\u25AB', '\u2217' };

	private static char DEFAULT_BULLET = BULLETS[2];

	private static String DEFAULT_BULLET_STRING = DEFAULT_BULLET + " ";

	private String[] _processedText = null;

	public String[] getProcessedText() {
		return _processedText;
	}

	public void setProcessedText(String[] tokens) {
		_processedText = tokens;
	}

	public static final String FRAME_NAME_SEPARATOR = " on frame ";

	/**
	 * The default font used to display text items if no font is specified.
	 */
	public static final String DEFAULT_FONT = "Serif-Plain-18";

	public static final Color DEFAULT_COLOR = Color.BLACK;

	public static final int MINIMUM_RANGED_CHARS = 2;

	public static final int NONE = 0;

	public static final int UP = 1;

	public static final int DOWN = 2;

	public static final int LEFT = 3;

	public static final int RIGHT = 4;

	public static final int HOME = 5;

	public static final int LINE_HOME = 9;

	public static final int LINE_END = 10;

	public static final int END = 6;

	public static final int PAGE_DOWN = 7;

	public static final int PAGE_UP = 8;

	/*
	 * Set the width to be IMPLICIT, but as wide as possible, a negative width
	 * value is one that is implicitly set by the system... a positive value is
	 * one explicitly set by the user.
	 */
	private Integer _maxWidth = Integer.MIN_VALUE + 1;

	private Justification _justification = Justification.left;

	private float _spacing = -1;

	private int _word_spacing = -1;

	private float _initial_spacing = 0;

	private float _letter_spacing = 0;

	// used during ranging out
	private int _selectionStart = -1;

	private int _selectionEnd = -1;
	
	// whether autowrap is on/off for this item
	protected boolean _autoWrap = false;

	// text is broken up into lines
	private StringBuffer _text = new StringBuffer();

	private List<TextLayout> _textLayouts = new LinkedList<TextLayout>();

	private List<Integer> _lineOffsets = new LinkedList<Integer>();

	private FontRenderContext frc = null;
	private LineBreakMeasurer _lineBreaker = null;

	// The font to display this text in
	private Font _font;
	
	
	protected static void InitFontFamily(GraphicsEnvironment ge, File fontFamilyDir)
	{
		File[] fontFiles = fontFamilyDir.listFiles();

		for (File fontFile : fontFiles) {
			String ext = "";
			String fileName = fontFile.getName().toLowerCase();

			int i = fileName.lastIndexOf('.');
			int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

			if (i > p) {
				ext = fileName.substring(i+1);
			}

			if (ext.equals("ttf")) {

				try {
					Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);

					ge.registerFont(font);
				}
				catch (Exception e) {
					System.err.println("Failed to load custon font file: " + fontFile);
				}
			}
		}
	}
	
	public static void InitFonts() {
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

		if (ge != null) {

			File fontDirectory = new File(FrameIO.FONT_PATH);
			if (fontDirectory != null) {
				File[] fontFamilyDirs = fontDirectory.listFiles();
				if (fontFamilyDirs != null) {

					if (fontFamilyDirs.length>0) {
						System.out.println("Loading custom fonts:");
					}

					boolean first_item = true;		
					for (File fontFamilyDir : fontFamilyDirs) {
						if (fontFamilyDir.isDirectory()) {

							if (first_item) {
								System.out.print("  " + fontFamilyDir.getName());
								first_item = false;
							}
							else {
								System.out.print(", " + fontFamilyDir.getName());
							}
							System.out.flush();

							InitFontFamily(ge,fontFamilyDir);
						}
					}
					System.out.println();	  
				}
			}
		}
		else {
			System.err.println("No graphics environment detected.  Skipping the loading of the custom fonts");
		}
	}

	

	/**
	 * Creates a new Text Item with the given ID and text.
	 * 
	 * @param id
	 *            The id of this item
	 * @param text
	 *            The text to use in this item
	 */
	public Text(int id, String text) {
		super();
		_text.append(text);
		rebuild(false);

		setID(id);
	}

	/**
	 * Creates a text item which is not added to the frame.
	 * 
	 * @param text
	 */
	public Text(String text) {
		super();
		_text.append(text);
		rebuild(false);
		setID(-1);
	}

	/**
	 * Creates a new Text Item with the given ID
	 * 
	 * @param id
	 *            The ID to of this item
	 */
	public Text(int id) {
		super();
		setID(id);
	}

	public Text(int i, String string, Color foreground, Color background) {
		this(i, string);
		this.setColor(foreground);
		this.setBackgroundColor(background);
	}

	/**
	 * Sets the maximum width of this Text item when justifcation is used.
	 * passing in 0 or -1 means there is no maximum width
	 * 
	 * @param width
	 *            The maximum width of this item when justification is applied
	 *            to it.
	 */
	@Override
	public void setWidth(Integer width) {
		invalidateAll();

		if (width == null) {
			setJustification(Justification.left);
			setRightMargin(FrameGraphics.getMaxFrameSize().width, false);
			return;
		}

		_maxWidth = width;
		rebuild(true);
		invalidateAll();
	}

	/**
	 * Returns the maximum width of this Text item when justifcation is used. If
	 * the width is negative, it means no explicit width has been set
	 * 
	 * @return The maximum width of this Text item when justification is used
	 */
	@Override
	public Integer getWidth() {
		if (_maxWidth == null || _maxWidth <= 0)
			return null;
		return _maxWidth;
	}

	public Integer getAbsoluteWidth() {
		if (_maxWidth == null) {
			return Integer.MAX_VALUE;
		}

		return Math.abs(_maxWidth);
	}

	@Override
	public Color getHighlightColor() {
		if (_highlightColor.equals(getPaintBackgroundColor()))
			return ALTERNATE_HIGHLIGHT;
		return _highlightColor;
	}

	/**
	 * Sets the justification of this Text item. The given integer should
	 * correspond to one of the JUSTIFICATION constants defined in Item
	 * 
	 * @param just
	 *            The justification to apply to this Text item
	 */
	public void setJustification(Justification just) {
		invalidateAll();

		// Only justification left works with 0 width
		// if (just != null && just != Justification.left && !hasWidth()) {
		// // TODO Tighten this up so it subtracts the margin widths
		// setWidth(getBoundsWidth());
		// }

		_justification = just;
		rebuild(true);
		invalidateAll();
	}

	/**
	 * Returns the current justification of this Text item. The default value
	 * left justification.
	 * 
	 * @return The justification of this Text item
	 */
	public Justification getJustification() {
		if (_justification == null || _justification.equals(Justification.left))
			return null;
		return _justification;
	}

	private int getJustOffset(TextLayout layout) {
		if (getJustification() == Justification.center)
			return (int) ((getAbsoluteWidth() - layout.getAdvance()) / 2);
		else if (getJustification() == Justification.right)
			return (int) (getAbsoluteWidth() - layout.getAdvance());

		return 0;
	}

	/**
	 * Sets the text displayed on the screen to the given String. It does not
	 * reset the formula, attributeValuePair or other cached values.
	 * 
	 * @param text
	 *            The String to display on the screen when drawing this Item.
	 */
	@Override
	public void setText(String text) {
		setText(text, false);
	}

	public void setText(String text, Boolean clearCache) {
		// if (_text != null && text.length() < _text.length())
		invalidateAll();
		_text = new StringBuffer(text);

		/*
		 * Always clearingCach remove formulas when moving in and out of XRay
		 * mode
		 */
		if (clearCache) {
			clearCache();
		}

		rebuild(true);
		invalidateAll();
	}

	public void setTextList(List<String> text) {
		if (text == null || text.size() <= 0)
			return;

		invalidateAll();

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < text.size(); i++) {
			sb.append(text.get(i)).append('\n');
		}

		if (sb.length() > 0)
			sb.deleteCharAt(sb.length() - 1);

		setText(sb.toString());

		rebuild(true);
		invalidateAll();
	}

	public void setAttributeValue(String value) {
		AttributeValuePair avp = new AttributeValuePair(getText(), false);
		avp.setValue(value);
		setText(avp.toString());
	}

	/**
	 * Inserts the given String at the start of the first line of this Text
	 * Item.
	 * 
	 * @param text
	 *            The String to insert.
	 */
	public void prependText(String text) {

		_text.insert(0, text);
		rebuild(false);
	}

	/**
	 * If the first line of text starts with the given String, then it is
	 * removed otherwise no action is taken.
	 * 
	 * @param text
	 *            The String to remove from the first line of Text
	 */
	public void removeText(String text) {
		if (_text.length() > 0 && _text.indexOf(text) == 0) {
			// Need the invalidate all for dateStamp toggling
			invalidateAll();
			_text.delete(0, text.length());
		}

	}

	public void removeEndText(String textToRemove) {
		int length = _text.length();
		if (length > 0) {
			// invalidateAll();
			int pos = _text.indexOf(textToRemove);
			int textToRemoveLength = textToRemove.length();
			if (pos + textToRemoveLength == length) {
				_text.delete(pos, length);
			}
		}

	}

	/**
	 * Appends the given String to any text already present in this Item
	 * 
	 * @param text
	 *            The String to append.
	 */
	public void appendText(String text) {
		_text.append(text);
		rebuild(false);
	}

	/**
	 * Used by the frame reader to construct multi-line text items. It must run
	 * quickly, so that the system still responds well for long text items.
	 * 
	 * @param text
	 */
	public void appendLine(String text) {
		if (text == null)
			text = "";

		if (_text.length() > 0)
			_text.append('\n');

		_text.append(text);
		
		rebuild(true);
	}

	/**
	 * Tests if the first line of this Text starts with the given String.
	 * 
	 * @param text
	 *            The prefix to check for
	 * @return True if the first line starts with the given String, False
	 *         otherwise.
	 */
	public boolean startsWith(String text) {
		return startsWith(text, true);
	}

	public boolean startsWith(String text, boolean ignoreCase) {
		if (text == null || _text == null || _text.length() < 1)
			return false;

		if (ignoreCase)
			return _text.toString().toLowerCase()
					.startsWith(text.toLowerCase());
		else
			return _text.indexOf(text) == 0;
	}

	/**
	 * Inserts a character into the Text of this Item.
	 * 
	 * @param ch
	 *            The character insert.
	 * @param mouseX
	 *            The X position to insert the Strings at.
	 * @param mouseY
	 *            The Y position to insert the Strings at.
	 */
	public Point2D.Float insertChar(char ch, float mouseX, float mouseY) {
		if (ch != '\t') /* && ch != '\n' */
			return insertText("" + ch, mouseX, mouseY);

		return insertText(" " + ch, mouseX, mouseY);
	}

	/**
	 * @param index
	 * @return
	 */
	private char getNextBullet(char bullet) {
		for (int i = 0; i < BULLETS.length - 1; i++) {
			if (BULLETS[i] == bullet)
				return BULLETS[i + 1];
		}
		return BULLETS[0];
	}

	private char getPreviousBullet(char bullet) {
		for (int i = 1; i < BULLETS.length; i++) {
			if (BULLETS[i] == bullet)
				return BULLETS[i - 1];
		}
		return BULLETS[BULLETS.length - 1];
	}

	public Point2D.Float getLineEndPosition(float mouseY) {
		return getEdgePosition(getLinePosition(mouseY), false);
	}

	public Point2D.Float getLineStartPosition(float mouseY) {
		return getEdgePosition(getLinePosition(mouseY), true);
	}

	public Point2D.Float getParagraphEndPosition() {
		return getEdgePosition(_textLayouts.size() - 1, false);
	}

	public Point2D.Float getParagraphStartPosition() {
		return getEdgePosition(0, true);
	}

	private Point2D.Float getEdgePosition(int line, boolean start) {
		// if there is no text yet, or the line is invalid
		if (_text == null || _text.length() == 0 || line < 0
				|| line > _textLayouts.size() - 1)
			return new Point2D.Float(getX(), getY());

		TextLayout last = _textLayouts.get(line);
		TextHitInfo hit;
		if (start)
			hit = last.getNextLeftHit(1);
		else
			hit = last.getNextRightHit(last.getCharacterCount() - 1);

		// move the cursor to the new location
		float[] caret = last.getCaretInfo(hit);
		float y = getLineDrop(last) * line;

		float x = getX() + caret[0] + getJustOffset(last);
		x = Math
				.min(
						x,
						(getX() - Item.MARGIN_RIGHT - (2 * getGravity()) + getBoundsWidth()));
		return new Point2D.Float(x, getY() + y + caret[1]);
	}

	public void setSelectionStart(float mouseX, float mouseY) {
		// determine what line is being pointed to
		int line = getLinePosition(mouseY);

		// get the character being pointed to
		TextHitInfo hit = getCharPosition(line, mouseX);
		_selectionStart = hit.getInsertionIndex() + _lineOffsets.get(line);
		invalidateAll();
	}

	public void setSelectionEnd(float mouseX, float mouseY) {
		// determine what line is being pointed to
		int line = getLinePosition(mouseY);

		// get the character being pointed to
		TextHitInfo hit = getCharPosition(line, mouseX);
		_selectionEnd = hit.getInsertionIndex() + _lineOffsets.get(line);
		invalidateAll();
	}

	public void clearSelection() {
		_selectionStart = -1;
		_selectionEnd = -1;
		invalidateAll();
	}

	public void clearSelectionEnd() {
		_selectionEnd = -1;
		invalidateAll();
	}

	public String copySelectedText() {
		if (_selectionStart < 0 || _selectionEnd < 0)
			return null;
		else if (_selectionEnd > _text.length())
			_selectionEnd = _text.length();

		return _text.substring(Math.min(_selectionStart, _selectionEnd), Math
				.max(_selectionStart, _selectionEnd));
	}

	public String cutSelectedText() {
		return replaceSelectedText("");
	}

	public String replaceSelectedText(String newText) {
		if (_selectionStart < 0 || _selectionEnd < 0)
			return null;

		invalidateAll();

		if (_selectionEnd > _text.length())
			_selectionEnd = _text.length();

		int left = Math.min(_selectionStart, _selectionEnd);
		int right = Math.max(_selectionStart, _selectionEnd);

		// Trim the text to remove new lines on the beginning and end of the
		// string
		if (_text.charAt(left) == '\n') {
			// if the entire line is being removed then remove one of the new
			// lines, the first case checks if the last line is being removed
			if (right >= _text.length() || _text.charAt(right) == '\n') {
				_text.deleteCharAt(left);
				right--;
			} else {
				left++;
			}
		}
		// New lines are always at the start of the line for now...
		// if(_text.charAt(right - 1) == '\n' && left < right){
		// right--;
		// }
		String s = _text.substring(left, right);

		_text.delete(left, right);
		_text.insert(left, newText);
		rebuild(true);

		clearCache();

		invalidateAll();

		return s;
	}

	public int getSelectionSize() {
		if (_selectionEnd < 0 || _selectionStart < 0)
			return 0;

		// System.out.println(_selectionStart + ":" + _selectionEnd);

		return Math.abs(_selectionEnd - _selectionStart);
	}

	/**
	 * Inserts the given String into the Text at the position given by the
	 * mouseX and mouseY coordinates
	 * 
	 * @param text
	 *            The String to insert into this Text.
	 * @param mouseX
	 *            The X position to insert the String
	 * @param mouseY
	 *            The Y position to insert the String
	 * @return The new location that the mouse cursor should be moved to
	 */
	public Point2D.Float insertText(String text, float mouseX, float mouseY) {
		return insertText(text, mouseX, mouseY, -1);
	}

	public Point2D.Float insertText(String text, float mouseX, float mouseY,
			int insertPos) {
		TextHitInfo hit;
		TextLayout current = null;
		int line;

		invalidateAll();

		// check for empty string
		if (text == null || text.length() == 0)
			return new Point2D.Float(mouseX, mouseY);

		// if there is no text yet
		if (_text == null || _text.length() == 0) {
			_text = new StringBuffer().append(text);
			// create the linebreaker and layouts
			rebuild(true);
			assert (_textLayouts.size() == 1);
			current = _textLayouts.get(0);
			hit = current.getNextRightHit(0);
			line = 0;

			// otherwise, we are inserting text
		} else {
			clearCache();
			// determine what line is being pointed to
			line = getLinePosition(mouseY);

			// get the character being pointed to
			hit = getCharPosition(line, mouseX);

			int pos = hit.getInsertionIndex() + _lineOffsets.get(line);

			if (line > 0 && hit.getInsertionIndex() == 0) {
				// Only move forward a char if the line begins with a hard line
				// break... not a soft line break
				if (_text.charAt(pos) == '\n') {
					pos++;
				}
			}

			if (insertPos < 0)
				insertPos = pos;

			// if this is a backspace key
			if (text.charAt(0) == KeyEvent.VK_BACK_SPACE) {
				if (hasSelection()) {
					pos = deleteSelection(pos);
				} else if (insertPos > 0) {
					deleteChar(insertPos - 1);
					if (pos > 0)
						pos--;
				}
				// if this is a delete key
			} else if (text.charAt(0) == KeyEvent.VK_DELETE) {
				if (hasSelection()) {
					pos = deleteSelection(pos);
				} else if (insertPos < _text.length()) {
					deleteChar(insertPos);
				}
				// this is a tab
			} else if (text.charAt(0) == KeyEvent.VK_TAB) {
				// Text length greater than 1 signals a backwards tab
				if (text.length() > 1) {
					// Find the first non space char to see if its a bullet
					int index = 0;
					for (index = 0; index < _text.length(); index++) {
						if (!Character.isSpaceChar(_text.charAt(index)))
							break;
					}
					// Check if there is a space after the bullet
					if (index < _text.length() - 1
							&& _text.charAt(index + 1) == ' ') {
						// Change the bullet
						_text.setCharAt(index, getPreviousBullet(_text
								.charAt(index)));
					}
					// Remove the spacing at the start
					for (int i = 0; i < TAB_STRING.length(); i++) {
						if (_text.length() > 0
								&& Character.isSpaceChar(_text.charAt(0))) {
							deleteChar(0);
							pos--;
						} else
							break;
					}
					_lineBreaker = null;
				} else {
					// / Find the first non space char to see if its a bullet
					int index = 0;
					for (index = 0; index < _text.length(); index++) {
						if (!Character.isSpaceChar(_text.charAt(index)))
							break;
					}
					// Check if there is a space after the bullet
					if (index < _text.length() - 1
							&& _text.charAt(index + 1) == ' ') {
						char nextBullet = getNextBullet(_text.charAt(index));
						// Change the bullet
						_text.setCharAt(index, nextBullet);
					}
					// Insert the spacing at the start
					insertString(TAB_STRING, 0);
					pos += TAB_STRING.length();
				}
				// this is a normal insert
			} else {
				insertString(text, insertPos);
				pos += text.length();
			}

			if (_text.length() == 0) {
				rebuild(false);
				return new Point2D.Float(this._x, this._y);
			}

			int newLine = line;

			// if a rebuild is required
			rebuild(true, false);

			// determine the new position the cursor should have
			for (int i = 1; i < _lineOffsets.size(); i++) {
				if (_lineOffsets.get(i) >= pos) {
					newLine = i - 1;
					break;
				}
			}

			current = _textLayouts.get(newLine);
			pos -= _lineOffsets.get(newLine);

			if (newLine == line) {
				if (pos > 0)
					hit = current.getNextRightHit(pos - 1);
				else
					hit = current.getNextLeftHit(1);
			} else if (newLine < line) {
				hit = current.getNextRightHit(pos - 1);
			} else {
				hit = current.getNextRightHit(pos - 1);
			}

			line = newLine;
		}

		// move the cursor to the new location
		float[] caret = current.getCaretInfo(hit);
		float y = getLineDrop(current) * line;

		float x = getX() + caret[0] + getJustOffset(current);
		x = Math
				.min(
						x,
						(getX() - Item.MARGIN_RIGHT - (2 * getGravity()) + getBoundsWidth()));

		invalidateAll();

		return new Point2D.Float(Math.round(x), Math.round(getY() + y
				+ caret[1]));
	}

	/**
	 * 
	 */
	private void clearCache() {
		_attributeValuePair = null;
		setProcessedText(null);
		setFormula(null);
	}

	/**
	 * @param pos
	 * @return
	 */
	private int deleteSelection(int pos) {
		int selectionLength = getSelectionSize();
		cutSelectedText();
		clearSelection();
		pos -= selectionLength;
		return pos;
	}

	public Point2D.Float moveCursor(int direction, float mouseX, float mouseY,
			boolean setSelection, boolean wholeWord) {
		if (setSelection) {
			if (!hasSelection()) {
				setSelectionStart(mouseX, mouseY);
			}
		} else {
			// clearSelection();
		}

		Point2D.Float resultPos = null;

		// check for home or end keys
		switch (direction) {
		case HOME:
			resultPos = getParagraphStartPosition();
			break;
		case END:
			resultPos = getParagraphEndPosition();
			break;
		case LINE_HOME:
			resultPos = getLineStartPosition(mouseY);
			break;
		case LINE_END:
			resultPos = getLineEndPosition(mouseY);
			break;
		default:
			TextHitInfo hit;
			TextLayout current;
			int line;

			// if there is no text yet
			if (_text == null || _text.length() == 0) {
				return new Point2D.Float(mouseX, mouseY);
				// otherwise, move the cursor
			} else {
				// determine the line of text to check
				line = getLinePosition(mouseY);
				if (line < 0)
					line = _textLayouts.size() - 1;

				// if the cursor is moving up or down, change the line
				if (direction == UP)
					line = Math.max(line - 1, 0);
				else if (direction == DOWN)
					line = Math.min(line + 1, _textLayouts.size() - 1);

				hit = getCharPosition(line, mouseX);

				if (direction == LEFT) {
					if (hit.getInsertionIndex() > 0) {

						char prevChar = ' ';
						do {
							hit = _textLayouts.get(line).getNextLeftHit(hit);

							// Stop if at the start of the line
							if (hit.getInsertionIndex() == 0)
								break;
							// Keep going if the char to the left is a
							// letterOrDigit
							prevChar = _text.charAt(hit.getInsertionIndex() - 1
									+ _lineOffsets.get(line));
						} while (wholeWord
								&& Character.isLetterOrDigit(prevChar));
						// TODO Go to the start of the word instead of before
						// the word
						char nextChar = _text.charAt(hit.getInsertionIndex()
								+ _lineOffsets.get(line));
						/*
						 * This takes care of hard line break in
						 */
						if (line > 0 && nextChar == '\n') {
							line--;
							hit = _textLayouts.get(line)
									.getNextRightHit(
											_textLayouts.get(line)
													.getCharacterCount() - 1);
						}
						// This takes care of soft line breaks.
					} else if (line > 0) {
						line--;
						hit = _textLayouts.get(line).getNextRightHit(
								_textLayouts.get(line).getCharacterCount() - 1);
						/*
						 * Skip the spaces at the end of a line with soft
						 * linebreak
						 */
						while (hit.getCharIndex() > 0
								&& _text.charAt(_lineOffsets.get(line)
										+ hit.getCharIndex() - 1) == ' ') {
							hit = _textLayouts.get(line).getNextLeftHit(hit);
						}
					}
				} else if (direction == RIGHT) {
					if (hit.getInsertionIndex() < _textLayouts.get(line)
							.getCharacterCount()) {
						hit = _textLayouts.get(line).getNextRightHit(hit);
						// Skip whole word if needs be
						while (wholeWord
								&& hit.getCharIndex() > 0
								&& hit.getCharIndex() < _textLayouts.get(line)
										.getCharacterCount()
								&& Character.isLetterOrDigit(_text
										.charAt(_lineOffsets.get(line)
												+ hit.getCharIndex() - 1)))
							hit = _textLayouts.get(line).getNextRightHit(hit);
					} else if (line < _textLayouts.size() - 1) {
						line++;
						hit = _textLayouts.get(line).getNextLeftHit(1);
					}
				}
				current = _textLayouts.get(line);
			}

			// move the cursor to the new location
			float[] caret = current.getCaretInfo(hit);
			float y = getLineDrop(current) * line;

			resultPos = new Point2D.Float(getX() + caret[0]
					+ getJustOffset(current), getY() + y + caret[1]);
			break;
		}
		if (setSelection)
			setSelectionEnd(resultPos.x, resultPos.y);
		return resultPos;
	}

	/**
	 * Iterates through the given line string and returns the position of the
	 * character being pointed at by the mouse.
	 * 
	 * @param line
	 *            The index of the _text array of the String to be searched.
	 * @param mouseX
	 *            The X coordinate of the mouse
	 * @return The position in the string of the character being pointed at.
	 */
	public TextHitInfo getCharPosition(int line, float mouseX) {
		if (line < 0 || line >= _textLayouts.size())
			return null;

		TextLayout layout = _textLayouts.get(line);
		mouseX += getOffset().x;
		mouseX -= getJustOffset(layout);

		return layout.hitTestChar(mouseX - getX(), 0);
	}

	public int getLinePosition(float mouseY) {
		mouseY += getOffset().y;

		float y = getY();

		for (TextLayout text : _textLayouts) {
			// calculate X to ensure it is in the shape
			Rectangle2D bounds = text.getLogicalHighlightShape(0,
					text.getCharacterCount()).getBounds2D();

			if (bounds.getWidth() < 1)
				bounds.setRect(bounds.getMinX(), bounds.getMinY(), 10, bounds
						.getHeight());

			double x = bounds.getCenterX();

			if (bounds.contains(x, mouseY - getY() - (y - getY())))
				return _textLayouts.indexOf(text);

			// check if the cursor is between lines
			if (mouseY - getY() - (y - getY()) < bounds.getMinY())
				return Math.max(0, _textLayouts.indexOf(text) - 1);

			y += getLineDrop(text);
		}

		return _textLayouts.size() - 1;
	}

	/**
	 * Sets the Font that this text will be displayed with on the screen.
	 * 
	 * @param font
	 *            The Font to display the Text of this Item in.
	 */
	public void setFont(Font font) {
		invalidateAll();
		// all decoding occurs in the Utils class
		_font = font;
		// rejustify();
		rebuild(false);

		invalidateAll();
	}

	/**
	 * Returns the Font that this Text is currently using when painting to the
	 * screen
	 * 
	 * @return The Font used to display this Text on the screen.
	 */
	public Font getFont() {
		return _font;
	}

	public Font getPaintFont() {
		if (getFont() == null)
			return Font.decode(DEFAULT_FONT);

		return getFont();
	}

	public String getFamily() {
		return getPaintFont().getFamily();
	}

	public void setFamily(String newFamily) {
		String toDecode = newFamily + "-" + getFontStyle() + "-"
				+ Math.round(getSize());
		setFont(Font.decode(toDecode));
		
		setLetterSpacing(this._letter_spacing);
	}

	public String getFontStyle() {
		Font f = getPaintFont();
		String s = "";

		if (f.isPlain())
			s += "Plain";

		if (f.isBold())
			s += "Bold";

		if (f.isItalic())
			s += "Italic";

		return s;
	}

	public static final String MONOSPACED_FONT = "monospaced";

	public static final String[] FONT_WHEEL = { "sansserif", "monospaced",
			"serif", "dialog", "dialoginput" };

	public static final char[] FONT_CHARS = { 's', 'm', 't', 'd', 'i' };

	private static final int NEARBY_GRAVITY = 2;

	public static final int MINIMUM_FONT_SIZE = 6;

	public void toggleFontFamily() {
		String fontFamily = getFamily().toLowerCase();
		// set it to the first font by default
		setFamily(FONT_WHEEL[0]);

		for (int i = 0; i < FONT_WHEEL.length - 3; i++) {
			if (fontFamily.equals(FONT_WHEEL[i])) {
				setFamily(FONT_WHEEL[i + 1]);
				break;
			}
		}
	}

	public void toggleFontStyle() {
		invalidateAll();
		Font currentFont = getPaintFont();
		if (currentFont.isPlain())
			setFont(currentFont.deriveFont(Font.BOLD));
		else if (currentFont.isBold() && currentFont.isItalic())
			setFont(currentFont.deriveFont(Font.PLAIN));
		else if (currentFont.isBold())
			setFont(currentFont.deriveFont(Font.ITALIC));
		else
			setFont(currentFont.deriveFont(Font.ITALIC + Font.BOLD));
		rebuild(true);
		invalidateAll();
	}

	public void toggleBold() {
		invalidateAll();
		Font currentFont = getPaintFont();
		int newStyle = currentFont.getStyle();
		if (currentFont.isBold()) {
			newStyle -= Font.BOLD;
		} else {
			newStyle += Font.BOLD;
		}
		setFont(currentFont.deriveFont(newStyle));
		rebuild(true);
		invalidateAll();
	}

	public void toggleItalics() {
		invalidateAll();
		Font currentFont = getPaintFont();
		int newStyle = currentFont.getStyle();
		if (currentFont.isItalic()) {
			newStyle -= Font.ITALIC;
		} else {
			newStyle += Font.ITALIC;
		}
		setFont(currentFont.deriveFont(newStyle));
		rebuild(true);
		invalidateAll();
	}

	public void setFontStyle(String newFace) {
		if (newFace == null || newFace.trim().length() == 0) {
			setFont(getPaintFont().deriveFont(Font.PLAIN));
			return;
		}

		newFace = newFace.toLowerCase().trim();

		if (newFace.equals("plain") || newFace.equals("p")) {
			setFont(getPaintFont().deriveFont(Font.PLAIN));
		} else if (newFace.equals("bold") || newFace.equals("b")) {
			setFont(getPaintFont().deriveFont(Font.BOLD));
		} else if (newFace.equals("italic") || newFace.equals("i")) {
			setFont(getPaintFont().deriveFont(Font.ITALIC));
		} else if (newFace.equals("bolditalic") || newFace.equals("italicbold")
				|| newFace.equals("bi") || newFace.equals("ib")) {
			setFont(getPaintFont().deriveFont(Font.BOLD + Font.ITALIC));
		}

	}

	/**
	 * Returns a String array of this Text object's text, split up into separate
	 * lines.
	 * 
	 * @return The String array with one element per line of text in this Item.
	 */
	public List<String> getTextList() {
		if (_text == null)
			return null;
		try {
			List<String> list = new LinkedList<String>();

			// Rebuilding prevents errors when displaying frame bitmaps
			if (_lineOffsets.size() == 0) {
				rebuild(false);
			}

			int last = 0;
			for (int offset : _lineOffsets) {
				if (offset != last) {
					list
							.add(_text.substring(last, offset).replaceAll("\n",
									""));
				}
				last = offset;
			}

			return list;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		}
	}

	public String getText() {
		return _text.toString();
	}

	/**
	 * Returns the first line of text in this Text Item
	 * 
	 * @return The first line of Text
	 */
	public String getFirstLine() {
		if (_text == null || _text.length() == 0)
			return null;

		// start at the first non-newLine char
		int index = 0;
		while (_text.charAt(index) == '\n') {
			index++;
		}

		int nextNewLine = _text.indexOf("\n", index);

		/* If there are no more newLines return the remaining text */
		if (nextNewLine < 0)
			return _text.substring(index);

		return _text.substring(index, nextNewLine);
	}

	/**
	 * Sets the inter-line spacing (in pixels) of this text.
	 * 
	 * @param spacing
	 *            The number of pixels to allow between each line
	 */
	public void setSpacing(float spacing) {
		_spacing = spacing;
		updatePolygon();
	}

	/**
	 * Returns the inter-line spacing (in pixels) of this Text.
	 * 
	 * @return The spacing (inter-line) in pixels of this Text.
	 */
	public float getSpacing() {
		return _spacing;
	}

	private float getLineDrop(TextLayout layout) {
		if (getSpacing() < 0)
			return layout.getAscent() + layout.getDescent()
					+ layout.getLeading();

		return layout.getAscent() + layout.getDescent() + getSpacing();
	}

	public void setWordSpacing(int spacing) {
		_word_spacing = spacing;
	}

	public int getWordSpacing() {
		return _word_spacing;
	}

	/**
	 * Sets the spacing (proportional to the font size) between letters
	 * 
	 * @param spacing
	 *            Additional spacing to add between letters. See {@link java.awt.font.TextAttribute#TRACKING}
	 */
	public void setLetterSpacing(float spacing) {
		_letter_spacing = spacing;
		HashMap<TextAttribute, Object> attr = new HashMap<TextAttribute, Object>();
		attr.put(TextAttribute.TRACKING, spacing);
		
		if (this._font == null) {
			this._font = Font.decode(DEFAULT_FONT);
		}

		this.setFont(this._font.deriveFont(attr));
	}

	/**
	 * @return The spacing (proportional to the font size) between letters. See {@link java.awt.font.TextAttribute#TRACKING}
	 */
	public float getLetterSpacing() {
		return _letter_spacing;
	}

	public void setInitialSpacing(float spacing) {
		_initial_spacing = spacing;
	}

	public float getInitialSpacing() {
		return _initial_spacing;
	}

	@Override
	public boolean intersects(Polygon p) {
		if (super.intersects(p)) {
			float textY = getY();

			for (TextLayout text : _textLayouts) {
				// check left and right of each box
				Rectangle2D textOutline = text.getLogicalHighlightShape(0,
						text.getCharacterCount()).getBounds2D();
				textOutline
						.setRect(textOutline.getX() + getX() - 1, textOutline
								.getY()
								+ textY - 1, textOutline.getWidth() + 2,
								textOutline.getHeight() + 2);
				if (p.intersects(textOutline))
					return true;
				textY += getLineDrop(text);
			}
		}
		return false;
	}

	@Override
	public boolean contains(int mouseX, int mouseY) {
		return contains(mouseX, mouseY, getGravity() * NEARBY_GRAVITY);
	}

	public boolean contains(int mouseX, int mouseY, int gravity) {
		mouseX += getOffset().x;
		mouseY += getOffset().y;

		float textY = getY();
		float textX = getX();

		Rectangle2D outline = getPolygon().getBounds2D();

		// Check if its outside the top and left and bottom bounds
		if (outline.getX() - mouseX > gravity
				|| outline.getY() - mouseY > gravity
				|| mouseY - (outline.getY() + outline.getHeight()) > gravity
				|| mouseX - (outline.getX() + outline.getWidth()) > gravity) {
			return false;
		}

		for (TextLayout text : _textLayouts) {
			// check left and right of each box
			Rectangle2D textOutline = text.getLogicalHighlightShape(0,
					text.getCharacterCount()).getBounds2D();

			// check if the cursor is within the top, bottom and within the
			// gravity of right
			int justOffset = getJustOffset(text);

			if (mouseY - textY > textOutline.getY()
					&& mouseY - textY < textOutline.getY()
							+ textOutline.getHeight()
					&& mouseX - textX - justOffset < textOutline.getWidth()
							+ gravity + Item.MARGIN_RIGHT
			/* &&(justOffset == 0 || mouseX > textX + justOffset - gravity ) */)
				return true;
			textY += getLineDrop(text);
		}

		return false;
	}

	/**
	 * Updates the Polygon (rectangle) that surrounds this Text on the screen.
	 */
	public void updatePolygon() {
		// if there is no text, there is nothing to do
		if (_text == null)
			return;

		_poly = new Polygon();

		if (_textLayouts.size() < 1)
			return;

		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;

		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;

		float y = -1;
		
		// Fix concurrency error in ScaleFrameset
		List<TextLayout> tmpTextLayouts;
		synchronized(_textLayouts) {
			tmpTextLayouts = new LinkedList<TextLayout>(_textLayouts);
		}

		for (TextLayout layout : tmpTextLayouts) {
			Rectangle2D bounds = layout.getLogicalHighlightShape(0,
					layout.getCharacterCount()).getBounds2D();
		
			if (y < 0)
				y = 0;
			else
				y += getLineDrop(layout);

			maxX = Math.max(maxX, (int) bounds.getMaxX());
			minX = Math.min(minX, (int) bounds.getMinX());
			maxY = Math.max(maxY, (int) (bounds.getMaxY() + y));
			minY = Math.min(minY, (int) (bounds.getMinY() + y));
		}

		minX -= getLeftMargin();
		maxX += Item.MARGIN_RIGHT;

		// If its justification right or center then DONT limit the width
		if (getJustification() != null) {
			maxX = Item.MARGIN_RIGHT + getAbsoluteWidth();
		}

		_poly.addPoint(minX - getGravity(), minY - getGravity());
		_poly.addPoint(maxX + getGravity(), minY - getGravity());
		_poly.addPoint(maxX + getGravity(), maxY + getGravity());
		_poly.addPoint(minX - getGravity(), maxY + getGravity());

		_poly.translate(getX(), getY());
	}

	// TODO it seems like this method has some exponencial processing which
	// makes items copy really slowly when there are lots of lines of text!
	// This needs to be fixed!!
	public void rebuild(boolean limitWidth) {
		rebuild(limitWidth, true);
	}

	/**
	 * 
	 * @param limitWidth
	 * @param newLinebreakerAlways
	 *            true if a new line breaker should always be created.
	 */
	private void rebuild(boolean limitWidth, boolean newLinebreakerAlways) {
		// TODO make this more efficient so it only clears annotation list when
		// it really has to
		if (isAnnotation()) {
			Frame parent = getParent();
			// parent can be null when running tests
			if (parent != null) {
				parent.clearAnnotations();
			}
		}

		// if there is no text, there is nothing to do
		if (_text == null || _text.length() == 0) {
			// Frame parent = getParent();
			// if(parent != null)
			// parent.removeItem(this);
			return;
		}

		if (_lineBreaker == null || newLinebreakerAlways) {
			AttributedString paragraphText = new AttributedString(_text
					.toString());
			paragraphText.addAttribute(TextAttribute.FONT, getPaintFont());
			frc = new FontRenderContext(null, true, true);
			_lineBreaker = new LineBreakMeasurer(paragraphText.getIterator(), frc);
		}

		/* float width = Float.MAX_VALUE;

		if (limitWidth) {
			width = getAbsoluteWidth();
			// else if (getMaxWidth() > 0)
			// width = Math.max(50, getMaxWidth() - getX()
			// - Item.MARGIN_RIGHT);
		} */

		_textLayouts.clear();
		_lineOffsets.clear();
		// the first line always has a 0 offset
		_lineOffsets.add(0);

		TextLayout layout;
		
		float width;
		float lineHeight = Float.NaN;
		List<Point[]> lines = null;
		
		if(_autoWrap || ExperimentalFeatures.AutoWrap.get()) {
		lines = new LinkedList<Point[]>();
    		if(DisplayIO.getCurrentFrame() == null) {
    			return;
    		}
    		for(Item item : DisplayIO.getCurrentFrame().getItems()) {
    				if(item instanceof Line) {
    					lines.add(new Point[] { ((Line) item).getStartItem().getPosition(), ((Line) item).getEndItem().getPosition() });
    				}
    				if(item instanceof Picture) {
    					lines.add(new Point[] { item.getPosition(), new Point(item.getX(), item.getY() + item.getHeight()) });
    				}
    		}
    		for(Item item : FreeItems.getInstance()) {
    				if(item instanceof Line) {
    					lines.add(new Point[] { ((Line) item).getStartItem().getPosition(), ((Line) item).getEndItem().getPosition() });
    				}
    				if(item instanceof Picture) {
    					lines.add(new Point[] { item.getPosition(), new Point(item.getX(), item.getY() + item.getHeight()) });
    				}
    		}
    		width = getLineWidth(getX(), getY(), lines);
		} else {
			width = Float.MAX_VALUE;
     		if (limitWidth) {
     			if(_maxWidth == null) {
     				width = FrameGraphics.getMaxFrameSize().width - getX();
     			} else {
     				width = getAbsoluteWidth();
     			}
     			// else if (getMaxWidth() > 0)
     			// width = Math.max(50, getMaxWidth() - getX()
     			// - Item.MARGIN_RIGHT);
     		}
		}
		
		_lineBreaker.setPosition(0);
		boolean requireNextWord = false;

		// --- Get the output of the LineBreakMeasurer and store it in a
		while (_lineBreaker.getPosition() < _text.length()) {
			
			if(_autoWrap || ExperimentalFeatures.AutoWrap.get()) {
				requireNextWord = width < FrameGraphics.getMaxFrameSize().width - getX();
			}
			
			layout = _lineBreaker.nextLayout(width, _text.length(), requireNextWord);
			
			// lineBreaker does not break on newline
			// characters so they have to be check manually
			int start = _lineOffsets.get(_lineOffsets.size() - 1);
			
			// int y = getY() + (getLineDrop(layout) * (_lineOffsets.size() - 1) 
			
			// check through the current line for newline characters
			for (int i = start + 1; i < _text.length(); i++) {
				if (_text.charAt(i) == '\n') {// || c == '\t'){
					_lineBreaker.setPosition(start);
					layout = _lineBreaker.nextLayout(width, i, requireNextWord);
					break;
				}
			}

			_lineOffsets.add(_lineBreaker.getPosition());
			
			if(layout == null) {
				layout = new TextLayout(" ", getPaintFont(), frc);
			}

			if (/* hasWidth() && */getJustification() == Justification.full
					&& _lineBreaker.getPosition() < _text.length())
				layout = layout.getJustifiedLayout(width);
			
			_textLayouts.add(layout);
    						
			if(_autoWrap || ExperimentalFeatures.AutoWrap.get()) {
				
    			if(lineHeight != Float.NaN) {
    				lineHeight = getLineDrop(layout);
    			}
    			width = getLineWidth(getX(), getY() + (lineHeight * (_textLayouts.size() - 1)), lines);
			}
		}

		updatePolygon();

	}
	
	private float getLineWidth(int x, float y, List<Point[]> lines) {
		float width = FrameGraphics.getMaxFrameSize().width;
		for(Point[] l : lines) {
			// check for lines that cross over our y
			if((l[0].y >= y && l[1].y <= y) || (l[0].y <= y && l[1].y >= y)) {
				float dX = l[0].x - l[1].x;
				float dY = l[0].y - l[1].y;
				float newWidth;
				if(dX == 0) {
					newWidth = l[0].x;
				} else if(dY == 0) {
					newWidth = Math.min(l[0].x, l[1].x);
				} else {
					// System.out.print("gradient: " + (dY / dX));
					newWidth = l[0].x + (y - l[0].y) * dX / dY;
				}
				// System.out.println("dY:" + dY + " dX:" + dX + " width:" + newWidth);
				if(newWidth < x) {
				 	continue;
				}
				if(newWidth < width) {
					width = newWidth;
				}
			}
		}
		return width - x;
	}

	private boolean hasFixedWidth() {
		assert (_maxWidth != null);
		if (_maxWidth == null) {
			justify(false);
		}
		return _maxWidth > 0;
	}

	private int _alpha = -1;

	public void setAlpha(int alpha) {
		_alpha = alpha;
	}

	private Point getSelectedRange(int line) {
		if (_selectionEnd >= _text.length()) {
			_selectionEnd = _text.length();
		}

		if (_selectionStart < 0)
			_selectionStart = 0;

		if (_selectionStart < 0 || _selectionEnd < 0)
			return null;

		int selectionLeft = Math.min(_selectionStart, _selectionEnd);
		int selectionRight = Math.max(_selectionStart, _selectionEnd);

		// if the selection is after this line, return null
		if (_lineOffsets.get(line) > selectionRight)
			return null;

		// if the selection is before this line, return null
		if (_lineOffsets.get(line) < selectionLeft
				&& _lineOffsets.get(line)
						+ _textLayouts.get(line).getCharacterCount() < selectionLeft)
			return null;

		// Dont highlight a single char
		// if (selectionRight - selectionLeft <= MINIMUM_RANGED_CHARS)
		// return null;

		// the selection occurs on this line, determine where it lies on the
		// line
		int start = Math.max(0, selectionLeft - _lineOffsets.get(line));
		// int end = Math.min(_lineOffsets.get(line) +
		// _textLayouts.get(line).getCharacterCount(), _selectionEnd);
		int end = Math.min(selectionRight - _lineOffsets.get(line),
				+_textLayouts.get(line).getCharacterCount());

		// System.out.println(line + ": " + start + "x" + end + " (" +
		// _selectionStart + "x" + _selectionEnd + ")");
		return new Point(start, end);
	}

	/**
	 * @param mouseButton
	 *            Either MouseEvent.BUTTON1, MouseEvent.BUTTON2 or
	 *            MouseEvent.BUTTON3.
	 * 
	 * @return The color for the text selection based on the given mouse click
	 */
	protected Color getSelectionColor(int mouseButton) {

		/*
		 * Color main = getPaintColor(); Color back = getPaintBackgroundColor();
		 * 
		 * if (Math.abs(main.getRed() - back.getRed()) < 10 &&
		 * Math.abs(main.getGreen() - back.getGreen()) < 10 &&
		 * Math.abs(main.getBlue() - back.getBlue()) < 10) { selection = new
		 * Color(Math.abs(255 - main.getRed()), Math .abs(255 -
		 * main.getGreen()), Math.abs(255 - main.getBlue())); } else { selection =
		 * new Color((main.getRed() + (back.getRed() * 2)) / 3, (main.getGreen() +
		 * (back.getGreen() * 2)) / 3, (main .getBlue() + (back.getBlue() * 2)) /
		 * 3); }
		 */
		int green = 160;
		int red = 160;
		int blue = 160;

		if (FrameMouseActions.wasDeleteClicked()) {
			green = 235;
			red = 235;
			blue = 140;
		} else if (mouseButton == MouseEvent.BUTTON1) {
			red = 255;
		} else if (mouseButton == MouseEvent.BUTTON2) {
			green = 255;
		} else if (mouseButton == MouseEvent.BUTTON3) {
			blue = 255;
		}

		return new Color(red, green, blue);
	}

	@Override
	public void paint(Graphics2D g) {
		if (!isVisible())
			return;

		// if there is no text to paint, do nothing.
		if (_text == null || _text.length() == 0)
			return;
		
		if(_autoWrap || ExperimentalFeatures.AutoWrap.get()) {
			invalidateAll();
		
			rebuild(true);
		} else if (_textLayouts.size() < 1) {
			clipFrameMargin();
			rebuild(true);
			// return;
		}

		// check if its a vector item and paint all the vector stuff too if this
		// item is a free item
		// This will allow for dragging vectors around the place!
		if (hasVector() && isFloating()) {
			FrameGraphics.requestRefresh(false);
			// TODO make this use a more efficient paint method...
			// Have the text item return a bigger repaint area if it has an
			// associated vector
		}

		// the background is only cleared if required
		if (getBackgroundColor() != null) {
			Color bgc = getBackgroundColor();
			if (_alpha > 0) {
				bgc = new Color(bgc.getRed(), bgc.getGreen(), bgc.getBlue(),
						_alpha);
			}
			g.setColor(bgc);

			Color gradientColor = getGradientColor();
			if (gradientColor != null) {
				// The painting is not efficient enough for gradients...
				Shape s = getPolygon();
				if (s != null) {
					Rectangle b = s.getBounds();
					GradientPaint gp = new GradientPaint(
							(int) (b.x + b.width * 0.3), b.y, bgc,
							(int) (b.x + b.width * 1.3), b.y, gradientColor);
					g.setPaint(gp);
				}
			}

			g.fillPolygon(getPolygon());
		}

		if (hasVisibleBorder()) {
			g.setColor(getPaintBorderColor());
			Stroke borderStroke = new BasicStroke(getThickness(), CAP, JOIN);
			g.setStroke(borderStroke);
			g.drawPolygon(getPolygon());
		}

		if (hasFormula()) {
			g.setColor(getPaintHighlightColor());
			Stroke highlightStroke = new BasicStroke(1F, CAP, JOIN);
			g.setStroke(highlightStroke);

			Point2D.Float start = getEdgePosition(0, true);
			Point2D.Float end = getEdgePosition(0, false);
			g.drawLine(Math.round(start.x), Math.round(start.y), Math
					.round(end.x), Math.round(end.y));
		}

		if (isHighlighted()) {
			g.setColor(getPaintHighlightColor());
			Stroke highlightStroke = new BasicStroke(
					(float) getHighlightThickness(), CAP, JOIN);
			g.setStroke(highlightStroke);
			if (HighlightMode.Enclosed.equals(getHighlightMode()))
				g.fillPolygon(getPolygon());
			else
				g.drawPolygon(getPolygon());
		}

		float y = getY();
		Color c = getPaintColor();
		if (_alpha > 0)
			c = new Color(c.getRed(), c.getGreen(), c.getBlue(), _alpha);

		g.setColor(c);

		Color selection = getSelectionColor(FrameMouseActions
				.getLastMouseButton());

		// width -= getX();
		// int line = 0;
		// boolean tab = false;
		synchronized (_textLayouts) {
			for (int i = 0; i < _textLayouts.size(); i++) {
				TextLayout layout = _textLayouts.get(i);

				Point p = getSelectedRange(i);
				if (p != null) {
					AffineTransform at = new AffineTransform();
					AffineTransform orig = g.getTransform();
					at.translate(getX() + getJustOffset(layout), y);
					g.setTransform(at);

					g.setColor(selection);
					g.fill(layout.getLogicalHighlightShape(p.x, p.y));

					g.setTransform(orig);
					g.setColor(c);
				}

				int ldx = 1+getX()+getJustOffset(layout); // Layout draw x
				
				boolean debug = false;
				if (debug) {
					g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),40));
					Rectangle layout_rect = layout.getPixelBounds(null, ldx, y);
					g.fillRect(layout_rect.x, layout_rect.y, layout_rect.width, layout_rect.height);
					g.setColor(c);
				}
				
				
				layout.draw(g, ldx, y);

				/*
				 * AffineTransform at = new AffineTransform(); AffineTransform
				 * orig = g.getTransform(); at.translate(getX() +
				 * getJustOffset(layout), y); g.setTransform(at);
				 * g.draw(layout.getLogicalHighlightShape(0,
				 * layout.getCharacterCount())); g.setTransform(orig); /*
				 * if(_text.charAt(_lineOffsets.get(line) +
				 * (layout.getCharacterCount() - 1)) == '\t'){ tab = true; x =
				 * (int) (getX() + x + 20 + layout.getVisibleAdvance()); }else{
				 */
				y += getLineDrop(layout);
				/*
				 * tab = false; }
				 * 
				 * line++;
				 */
			}
		}

		paintLink(g);
	}

	@Override
	protected Rectangle getLinkDrawArea() { // TODO: Revise
		return getDrawingArea()[0];
	}

	/**
	 * Determines if this text has any text in it.
	 * 
	 * @return True if this Item has no text in it, false otherwise.
	 */
	public boolean isEmpty() {
		if (_text == null || _text.length() == 0)
			return true;

		return false;
	}

	@Override
	public Text copy() {
		Text copy = new Text(getID());
		// copy standard item values
		Item.DuplicateItem(this, copy);

		// copy values specific to text items
		copy.setSpacing(getSpacing());
		copy.setInitialSpacing(getInitialSpacing());

		copy.setWidth(getWidthToSave());
		copy.setJustification(getJustification());
		copy.setLetterSpacing(getLetterSpacing());
		copy.setWordSpacing(getWordSpacing());
		copy.setWidth(getWidthToSave());
		copy.setFont(getFont());
		if (hasFormula()) {
			copy.calculate(getFormula());
		} else {
			copy.setText(_text.toString());
		}
		copy.setHidden(!isVisible());
		return copy;
	}

	@Override
	public float getSize() {
		return getPaintFont().getSize2D();
	}

	/**
	 * Returns the number of characters in this Text, excluding new lines.
	 * 
	 * @return The sum of the length of each line of text
	 */
	public int getLength() {
		return _text.length();
	}

	@Override
	public void setSize(float size) {
		invalidateAll();
		// size *= UserSettings.ScaleFactor;
		// Dont want to have size set when duplicating a point which has size 0
		if (size < 0)
			return;

		if (size < MINIMUM_FONT_SIZE)
			size = MINIMUM_FONT_SIZE;
		setFont(getPaintFont().deriveFont(size));
		rebuild(true);
		invalidateAll();
	}

	@Override
	public void setAnnotation(boolean val) {
		float mouseX = DisplayIO.getFloatMouseX();
		float mouseY = FrameMouseActions.MouseY;
		Point2D.Float newPoint = new Point2D.Float();
		if (val) {
			// if this is already an annotation, do nothing
			if (isAnnotation())
				return;
			if (!isLineEnd() && _text.length() > 0
					&& _text.charAt(0) == DEFAULT_BULLET) {
				newPoint.setLocation(insertText(""
						+ (char) KeyEvent.VK_BACK_SPACE, mouseX, mouseY, 1));
				if (_text.length() > 0 && _text.charAt(0) == ' ')
					newPoint.setLocation(insertText(""
							+ (char) KeyEvent.VK_BACK_SPACE, newPoint.x,
							newPoint.y, 1));
			} else {
				newPoint.setLocation(insertText("@", mouseX, mouseY, 0));
			}
		} else {
			// if this is not an annotation, do nothing
			if (!isAnnotation())
				return;
			if (!isLineEnd() && _text.charAt(0) == '@') {
				newPoint.setLocation(insertText(""
						+ (char) KeyEvent.VK_BACK_SPACE, mouseX, mouseY, 1));
				newPoint.setLocation(insertText(DEFAULT_BULLET_STRING,
						newPoint.x, newPoint.y, 0));
			} else {
				newPoint.setLocation(insertText(""
						+ (char) KeyEvent.VK_BACK_SPACE, mouseX, mouseY, 1));
			}
		}
		FrameUtils.setLastEdited(this);
		rebuild(true);
		DisplayIO.setCursorPosition(newPoint.x, newPoint.y, false);
	}

	/**
	 * 
	 */
	private void insertString(String toInsert, int pos) {
		assert (toInsert.length() > 0);

		_text.insert(pos, toInsert);

		if (toInsert.length() > 1) {
			_lineBreaker = null;
		}

		if (_lineBreaker != null) {
			AttributedString inserting = new AttributedString(_text.toString());
			inserting.addAttribute(TextAttribute.FONT, getPaintFont());
			_lineBreaker.insertChar(inserting.getIterator(), pos);
		}
	}

	private void deleteChar(int pos) {
		_text.deleteCharAt(pos);

		if (_text.length() == 0) {
			if (this.isLineEnd()) {
				// Remove and replace with a dot
				FrameKeyboardActions.replaceText(this);
				DisplayIO.setCursorPosition(this._x, this._y);
			}
			return;
		}

		if (_lineBreaker != null) {
			AttributedString inserting = new AttributedString(_text.toString());
			inserting.addAttribute(TextAttribute.FONT, getPaintFont());
			_lineBreaker.deleteChar(inserting.getIterator(), pos);
		}

	}

	@Override
	public boolean isAnnotation() {
		if (_text != null && _text.length() > 0 && _text.charAt(0) == '@')
			return true;

		return false;
	}

	public boolean isSpecialAnnotation() {
		assert _text != null;
		String s = _text.toString().toLowerCase();
		if (s.length() > 0 && s.indexOf("@") == 0) {
			if (s.equals("@old") || s.equals("@ao")
					|| s.equals("@itemtemplate") || s.equals("@parent")
					|| s.equals("@next") || s.equals("@previous")
					|| s.equals("@first") || s.equals("@i") || s.equals("@iw")
					|| s.equals("@f"))
				return true;
		}

		return false;
	}

	@Override
	public Item merge(Item merger, int mouseX, int mouseY) {
		if (merger.isLineEnd()) {
			// Merging line ends onto non line end text is a no-op
			if (!isLineEnd())
				return null;

			if (merger instanceof Text)
				insertText(((Text) merger).getText(), mouseX, mouseY);

			// Set the position by moving the cursor before calling this
			// method!!

			List<Line> lines = new LinkedList<Line>();
			lines.addAll(merger.getLines());
			for (Line line : lines) {
				line.replaceLineEnd(merger, this);
			}
			merger.delete();
			this.setOffset(0, 0);
			return null;
		}

		if (!(merger instanceof Text))
			return merger;

		Text merge = (Text) merger;

		// insertText(merge.getText(), mouseX, mouseY);

		// if the item being merged has a link
		if (merge.getLink() != null) {
			// if this item has a link, keep it on the cursor
			if (getLink() != null) {
				merge.setText(merge.getLink());
				merge.setLink(null);
				// return merge;
				// TODO get this to return the merged item and attach it to the
				// cursor only when the user presses the middle button.
			} else
				setLink(merge.getLink());
		}

		return null;
	}

	/**
	 * Resets the position of the item to the default position for a title item.
	 * 
	 */
	public void resetTitlePosition() {
		setPosition(MARGIN_LEFT, MARGIN_LEFT + getBoundsHeight());
		Frame modelFrame = getParentOrCurrentFrame();
		if (modelFrame != null) {
			setRightMargin(modelFrame.getNameItem().getX() - MARGIN_LEFT, true);
		} else {
			System.out
					.print("Error: text.resetTitlePosition, getParent or currentFrame returned null");
			setRightMargin(MARGIN_LEFT, true);
		}
	}

	/**
	 * Removes the set of characters up to the first space in this text item.
	 * 
	 * @return the string that was removed.
	 */
	public String stripFirstWord() {
		int firstSpace = _text.toString().indexOf(' ');

		// if there is only one word just make it blank
		if (firstSpace < 0 || firstSpace + 1 >= _text.length()) {
			String text = _text.toString();
			setText("");
			return text;
		}

		String firstWord = _text.toString().substring(0, firstSpace);
		setText(_text.toString().substring(firstSpace).trim());

		return firstWord;
	}

	public String toString() {
		String message = "[" + getFirstLine() + "]" + FRAME_NAME_SEPARATOR;

		if (getParent() != null)
			return message + getParent().getName();
		return message + getDateCreated();
	}

	public Text getTemplateForm() {
		Text template = this.copy();
		template.setID(-1);
		// reset width of global templates so the widths of the items on the settings frames don't cause issues
		// this is in response to the fact that FrameCreator.addItem() sets rightMargin when it adds items
		template.setWidth(null);
		/*
		 * The template must have text otherwise the bounds height will be
		 * zero!! This will stop escape drop down from working if there is no
		 * item template
		 */
		template.setText("@");
		return template;
	}

	@Override
	public boolean isNear(int x, int y) {
		if (super.isNear(x, y)) {
			// TODO check that it is actually near one of the lines of space
			// return contains(x, y, getGravity() * 2 + NEAR_DISTANCE);
			// at the moment contains ignores gravity when checking the top and
			// bottom of text lines... so the cursor must be between two text
			// lines
			float textY = getY();
			float textX = getX();

			for (TextLayout text : _textLayouts) {
				// check left and right of each box
				Rectangle2D textOutline = text.getLogicalHighlightShape(0,
						text.getCharacterCount()).getBounds2D();

				// check if the cursor is within the top, bottom and within the
				// gravity of right
				if (y - textY > textOutline.getY() - NEAR_DISTANCE
						&& y - textY < textOutline.getY()
								+ textOutline.getHeight() + NEAR_DISTANCE
						&& x - textX < textOutline.getWidth() + NEAR_DISTANCE)
					return true;
				textY += getLineDrop(text);
			}
		}
		return false;
	}

	@Override
	public void anchor() {
		super.anchor();
		// ensure all text items have their selection cleared
		clearSelection();
		setAlpha(0);
		if (isLineEnd())
			DisplayIO.setCursor(Item.DEFAULT_CURSOR);

		String text = _text.toString().trim();

		clipFrameMargin();

		// Show the overlay stuff immediately if this is an overlay item
		if (hasLink() && (text.startsWith("@ao") || text.startsWith("@o"))) {
			FrameKeyboardActions.Refresh();
		}
	}
	
	private void clipFrameMargin() {
		if (!hasFixedWidth()) {
			int frameWidth = FrameGraphics.getMaxFrameSize().width;
			/*
			 * Only change width if it is more than 150 pixels from the right of
			 * the screen
			 */
			if (!_text.toString().contains(" ")) {
				Integer width = getWidth();
				if (width == null || width < 0)
					setWidth(Integer.MIN_VALUE + 1);
			} else if (frameWidth - getX() > ADJUST_WIDTH_THRESHOLD) {
				justify(false);
				// setRightMargin(frameWidth, false);
			}
		}
	}

	public void justify(boolean fixWidth) {
		
		// if autowrap is on, wrapping is done every time we draw
		if(ExperimentalFeatures.AutoWrap.get()) {
			return;
		}
		
		Integer width = FrameGraphics.getMaxFrameSize().width;

		// Check if that text item is inside an enclosing rectangle...
		// Set its max width accordingly
		Polygon enclosure = FrameUtils.getEnlosingPolygon();
		if (enclosure != null) {
			Rectangle bounds = enclosure.getBounds();
			if (bounds.width > 200 && getX() < bounds.width / 3 + bounds.x) {
				width = bounds.x + bounds.width;
			}
		}

		if (getWidth() == null)
			setRightMargin(width, fixWidth);

		// Check for the annotation that restricts the width of text items
		// on the frame
		String widthString;
		if ((widthString = getParentOrCurrentFrame().getAnnotationValue(
				"maxwidth")) != null) {
			try {
				int oldWidth = getWidth();
				int maxWidth = Integer.parseInt(widthString);
				if (maxWidth < oldWidth)
					setWidth(maxWidth);
			} catch (NumberFormatException nfe) {
			}

		}
	}

	public void resetFrameNamePosition() {
		Dimension maxSize = FrameGraphics.getMaxFrameSize();
		if (maxSize != null) {
			// setMaxWidth(maxSize.width);
			setPosition(maxSize.width - getBoundsWidth(), getBoundsHeight());
		}
	}

	@Override
	protected int getLinkYOffset() {
		if (_textLayouts.size() == 0)
			return 0;
		return Math.round(-(_textLayouts.get(0).getAscent() / 2));
	}

	@Override
	public String getName() {
		return getFirstLine();
	}

	public static final String TAB_STRING = "      ";

	public Point2D.Float insertTab(char ch, float mouseX, float mouseY) {
		return insertText("" + ch, mouseX, mouseY);
	}

	public Point2D.Float removeTab(char ch, float mouseX, float mouseY) {
		// Insert a space as a flag that it is a backwards tab
		return insertText(ch + " ", mouseX, mouseY);
	}

	public static boolean isBulletChar(char c) {
		for (int i = 0; i < BULLETS.length; i++) {
			if (BULLETS[i] == c)
				return true;
		}
		return c == '*' || c == '+' || c == '>' || c == '-' || c == 'o';
	}

	public boolean hasOverlay() {
		if (!isAnnotation() || getLink() == null)
			return false;
		String text = getText().toLowerCase();
		// TODO make it so can just check the _overlay variable
		// Mike cant remember the reason _overlay var cant be use! opps
		if (!text.startsWith("@"))
			return false;
		return text.startsWith("@o") || text.startsWith("@ao")
				|| text.startsWith("@v") || text.startsWith("@av");
	}

	public boolean hasSelection() {
		return getSelectionSize() > 0;
	}

	/**
	 * Dont save text items that are all white space.
	 */
	@Override
	public boolean dontSave() {
		String text = getText();
		assert (text != null);
		return text.trim().length() == 0 || super.dontSave();
	}

	@Override
	public boolean calculate(String formula) {
		if (FrameGraphics.isXRayMode())
			return false;

		super.calculate(formula);
		if (isFloating() || formula == null || formula.length() == 0) {
			return false;
		}
		formula = formula.replace(':', '=');

		String lowercaseFormula = formula.toLowerCase();
		ExpediteeJEP myParser = new ExpediteeJEP();

		int nextVarNo = 1;

		// Add variables from the containing rectangle if the item being
		// calculated is inside the enclosing rectangle
		Collection<Item> enclosed = getItemsInSameEnclosure();
		for (Item i : enclosed) {
			if (i == this)
				continue;
			if (i instanceof Text && !i.isAnnotation()) {
				AttributeValuePair pair = i.getAttributeValuePair();
				if (pair.hasPair()) {
					try {
						double value = pair.getDoubleValue();
						myParser.addVariable(pair.getAttribute(), value);
						// myParser.addVariable("$" + nextVarNo++, value);
					} catch (NumberFormatException nfe) {
						continue;
					} catch (Exception e) {
						e.printStackTrace();
					}
				} // else {
				// Add anonomous vars
				try {
					double value = pair.getDoubleValue();
					if (value != Double.NaN)
						myParser.addVariable("$" + nextVarNo++, value);
				} catch (NumberFormatException nfe) {
					continue;
				} catch (Exception e) {
					e.printStackTrace();
				}
				// }
			}
		}

		// Add the variables from this frame
		myParser.addVariables(this.getParentOrCurrentFrame());
		String linkedFrame = getAbsoluteLink();
		// Add the relative frame variable if the item is linked
		if (linkedFrame != null) {
			Frame frame = FrameIO.LoadFrame(linkedFrame);
			myParser.addVariables(frame);
			// If the frame is linked add vector variable for the frame
			if (lowercaseFormula.contains("$frame")) {
				myParser.addVectorVariable(frame.getNonAnnotationItems(true),
						"$frame");
			}
		}
		// Add the relative box variable if this item is a line end
		if (this.isLineEnd()) {
			// if its a line end add the enclosed stuff as an @variable
			if (lowercaseFormula.contains("$box")) {
				myParser.addVectorVariable(getEnclosedItems(), "$box");
			}
		}
		myParser.resetObserver();
		try {
			Node node = myParser.parse(formula);
			String result = myParser.evaluate(node);
			if (result != null) {
				this.setText(result);
				this.setFormula(formula);

				if (!this.hasAction()) {
					setActionMark(false);
					setAction("extract formula");
				}
			}
		} catch (Throwable e) {
			//e.printStackTrace();
			String formula2 = getFormula();
			this.setText(formula2);
			this.setFormula(formula2);
			return false;
		}

		_attributeValuePair = null;

		return true;
	}

	/**
	 * Gets items which are in the same enclosure as this item. 
	 * In the event more than one enclosure meets this criteria, then
	 * the one returned is the one with the smallest area.
	 * TODO: Improve the efficiency of this method
	 * 
	 * @return
	 */
	public Collection<Item> getItemsInSameEnclosure() {
		Collection<Item> sameEnclosure = null;
		Collection<Item> seen = new HashSet<Item>();
		Frame parent = getParentOrCurrentFrame();
		double enclosureArea = Double.MAX_VALUE;
		for (Item i : parent.getVisibleItems()) {
			/*
			 * Go through all the enclosures looking for one that includes this
			 * item
			 */
			if (!seen.contains(i) && i.isEnclosed()) {
				seen.addAll(i.getEnclosingDots());
				Collection<Item> enclosed = i.getEnclosedItems();
				// Check if we have found an enclosure containing this item
				// Check it is smaller than any other enclosure found containing
				// this item
				if (enclosed.contains(this)
						&& i.getEnclosedArea() < enclosureArea) {
					sameEnclosure = enclosed;
				}
			}
		}

		if (sameEnclosure == null)
			return new LinkedList<Item>();

		return sameEnclosure;
	}
	
	/**
	 * Returns true if items of the parent frame should be recalculated when
	 * this item is modified
	 */
	public boolean recalculateWhenChanged() {
		if (/*
			 * !isAnnotation() &&
			 */(hasFormula() || isLineEnd()))
			return true;
		try {
			AttributeValuePair avp = getAttributeValuePair();

			if (!avp.getDoubleValue().equals(Double.NaN))
				return true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public float getLineHeight() {
		return getLineDrop(_textLayouts.get(0));
	}

	@Override
	public void setAnchorLeft(Float anchor) {
		if (!isLineEnd()) {
			super.setAnchorLeft(anchor);
			// Subtract off the link width
			if (anchor != null) {
				setX(anchor + getLeftMargin());
			}
			return;
		}
		invalidateFill();
		invalidateCommonTrait(ItemAppearence.PreMoved);
		
		this._anchorLeft = anchor;
		this._anchorRight = null;
		
		int oldX = getX();
		if (anchor != null) {
			float deltaX = anchor + getLeftMargin() - oldX;
			anchorConnected(AnchorEdgeType.Left, deltaX);
		}

		invalidateCommonTrait(ItemAppearence.PostMoved);
		invalidateFill();
	}

	@Override
	public void setAnchorRight(Float anchor) {
		if (!isLineEnd()) {
			super.setAnchorRight(anchor);
			// Subtract off the link width
			if (anchor != null) {
				setX(FrameGraphics.getMaxFrameSize().width - anchor
						- getBoundsWidth() + getLeftMargin());
			}
			return;
		}
		invalidateFill();
		invalidateCommonTrait(ItemAppearence.PreMoved);
		
		this._anchorRight = anchor;
		this._anchorLeft = null;
		
		int oldX = getX();
		if (anchor != null) {
			float deltaX = FrameGraphics.getMaxFrameSize().width - anchor
					- getBoundsWidth() + getLeftMargin() - oldX;
			anchorConnected(AnchorEdgeType.Right, deltaX);
		}

		invalidateCommonTrait(ItemAppearence.PostMoved);
		invalidateFill();
	}



	@Override
	public void setAnchorTop(Float anchor) {
		if (!isLineEnd()) {
			super.setAnchorTop(anchor);
			if (anchor != null) {
				setY(anchor + _textLayouts.get(0).getAscent());
			}
			return;
		}
		invalidateFill();
		invalidateCommonTrait(ItemAppearence.PreMoved);
		
		this._anchorTop = anchor;
		this._anchorBottom = null;
		
		int oldY = getY();
		if (anchor != null) {
			float deltaY = anchor - oldY;
			anchorConnected(AnchorEdgeType.Top, deltaY);
		}

		invalidateCommonTrait(ItemAppearence.PostMoved);
		invalidateFill();
	}

	@Override
	public void setAnchorBottom(Float anchor) {
		if (!isLineEnd()) {
			super.setAnchorBottom(anchor);
			if (anchor != null) {
				setY(FrameGraphics.getMaxFrameSize().height - (anchor + this.getBoundsHeight() - _textLayouts.get(0).getAscent() - _textLayouts.get(0).getDescent()));
			}
			return;
		}
		invalidateFill();
		invalidateCommonTrait(ItemAppearence.PreMoved);
		
		this._anchorBottom = anchor;
		this._anchorTop = null;
		
		int oldY = getY();
		if (anchor != null) {

			float deltaY = FrameGraphics.getMaxFrameSize().height - anchor - oldY;
			anchorConnected(AnchorEdgeType.Bottom, deltaY);
		}

		invalidateCommonTrait(ItemAppearence.PostMoved);
		invalidateFill();
	}

	@Override
	public void scale(Float scale, int originX, int originY) {
		setSize(getSize() * scale);

		Integer width = getWidth();
		if (width != null) {
			setWidth(Math.round(width * scale));
		}
		
		super.scale(scale, originX, originY);
		rebuild(true);
	}

	
	
	public Rectangle getPixelBoundsUnion()
	{
		synchronized (_textLayouts) {
			
			int x = getX();
			int y = getY();
	
			int min_xl = Integer.MAX_VALUE;
			int max_xr = Integer.MIN_VALUE;
	
			int min_yt = Integer.MAX_VALUE;
			int max_yb = Integer.MIN_VALUE;
		
			
			for (int i = 0; i < _textLayouts.size(); i++) {
				TextLayout layout = _textLayouts.get(i);

				int ldx = 1+x+getJustOffset(layout); // Layout draw x				
				Rectangle layout_rect = layout.getPixelBounds(null, ldx, y);
					
				int xl = layout_rect.x;
				int xr = xl + layout_rect.width -1;
				
				int yt = layout_rect.y;
				int yb = yt + layout_rect.height -1;
				
				min_xl = Math.min(min_xl,xl);
				max_xr = Math.max(max_xr,xr);
				
				min_yt = Math.min(min_yt,yt);
				max_yb = Math.max(max_yb,yb);
			}
			
			if ((min_xl >= max_xr) || (min_yt >= max_yb)) {
				// No valid rectangle are found
				return null;
			}
			
			return new Rectangle(min_xl,min_yt,max_xr-min_xl+1,max_yb-min_yt+1);
			
		}
					
	}
	/*
	 * Returns the SIMPLE statement contained by this text item.
	 * 
	 */
	public String getStatement() {
		return getText().split("\\s+")[0];
	}
	
	public boolean getAutoWrap() {
		return _autoWrap;
	}
	
	// workaround since true is the default value and would not be displayed normally
	public String getAutoWrapToSave() {
		if(!_autoWrap) {
			return null;
		}
		return "true";
	}
	
	public void setAutoWrap(boolean autoWrap) {
		_autoWrap = autoWrap;
	}
}
