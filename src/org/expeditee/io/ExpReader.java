package org.expeditee.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.expeditee.gui.Frame;
import org.expeditee.items.Constraint;
import org.expeditee.items.Dot;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Text;
import org.expeditee.stats.SessionStats;

/**
 * Reads in Exp format files and constructs the Frame and Item objects they
 * contain.
 * 
 * @author mrww1
 * 
 */
public class ExpReader extends DefaultFrameReader {

	public static final String EXTENTION = ".exp";

	private BufferedReader _reader = null;

	private String _frameName;

	/**
	 * Does nothing, location must be set before use
	 */
	public ExpReader(String frameName) {
		super();

		_frameName = frameName;
	}

	/**
	 * Determines whether a string begins with tag.
	 * 
	 * @param s
	 *            a line of text
	 * @return true if s begins with a tag
	 */
	protected static boolean isValidLine(String s) {
		return s.length() >= 2 && s.charAt(1) == ' '
				&& Character.isLetter(s.charAt(0));
	}

	/**
	 * Reads a file with the given name from disk.
	 * 
	 * @param frameName
	 *            the name of the Frame to read in from a file.
	 * @return A new Frame object that contains any Items described in the file.
	 * @throws IOException
	 *             Any exceptions occured by the BufferedReader.
	 */
	public Frame readFrame(BufferedReader reader) throws IOException {
		_reader = reader;
		String next = "";
		Frame newFrame = new Frame();

		try {
			// Framename must be set before setting the frame number
			newFrame.setName(_frameName);

			// First read all the header lines
			while (_reader.ready() && !(next = _reader.readLine()).equals("Z")) {
				if (isValidLine(next)) {
					processHeaderLine(newFrame, next);
				}
			}

			// Now read all the items
			Item currentItem = null;
			while (_reader.ready() && !(next = _reader.readLine()).equals("Z")) {
				// if this is the start of a new item add a new item
				if (isValidLine(next)) {
					if (getTag(next) == 'S') {
						String value = getValue(next);
						int id = Integer.parseInt(value.substring(2));

						switch (value.charAt(0)) {
						case 'P': // check if its a point
							currentItem = new Dot(id);
							break;
						default:
							currentItem = new Text(id);
							break;
						}
						_linePoints.put(currentItem.getID(), currentItem);
						newFrame.addItem(currentItem);
					} else if (currentItem != null) {
						processBodyLine(currentItem, next);
					}
				}
			}

			// Read the lines
			while (_reader.ready() && !(next = _reader.readLine()).equals("Z")) {
				if (isValidLine(next)) {
					java.awt.Point idtype = separateValues(next.substring(2));
					// The next line must be the endpoints
					if (!_reader.ready())
						throw new Exception("Unexpected end of file");
					next = _reader.readLine();
					java.awt.Point startend = separateValues(next.substring(2));
					int start = startend.x;
					int end = startend.y;

					if (_linePoints.get(start) != null
							&& _linePoints.get(end) != null) {
						newFrame.addItem(new Line(_linePoints.get(start),
								_linePoints.get(end), idtype.x));
					} else {
						System.out
								.println("Error reading line with unknown end points");
					}
				}
			}

			// Read the constraints
			while (_reader.ready() && !(next = _reader.readLine()).equals("Z")) {
				if (isValidLine(next)) {
					java.awt.Point idtype = separateValues(next.substring(2));
					// The next line must be the endpoints
					if (!_reader.ready())
						throw new Exception("Unexpected end of file");
					next = _reader.readLine();
					java.awt.Point startend = separateValues(next.substring(2));

					Item a = _linePoints.get(startend.x);
					Item b = _linePoints.get(startend.y);

					new Constraint(a, b, idtype.x, idtype.y);
				}
			}

			// Read the stats
			while (_reader.ready() && ((next = _reader.readLine()) != null)) {
				if (next.startsWith(SessionStats.ACTIVE_TIME_ATTRIBUTE)) {
					try {
						String value = next.substring(SessionStats.ACTIVE_TIME_ATTRIBUTE.length()).trim();
						newFrame.setActiveTime(value);
					} catch (Exception e) {
					}

				} else if (next.startsWith(SessionStats.DARK_TIME_ATTRIBUTE)) {
					try {
						String value = next.substring(SessionStats.DARK_TIME_ATTRIBUTE.length()).trim();
						newFrame.setDarkTime(value);
					} catch (Exception e) {
					}

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error reading frame file line: " + next + " "
					+ e.getMessage());
		}

		//newFrame.refreshItemPermissions();
		_reader.close();
		newFrame.setChanged(false);

		return newFrame;
	}

	// Stores points used when constructing lines
	private HashMap<Integer, Item> _linePoints = new HashMap<Integer, Item>();

	/**
	 * Processes the body section of the Exp file, which contains all Items
	 * 
	 * @param frame
	 *            The Frame to add any created Items to.
	 * @param line
	 *            The line of text read in from the file to process.
	 */
	protected void processBodyLine(Item item, String line) {
		// separate the tag from the value
		Character tag = getTag(line);
		String value = getValue(line);

		Method toRun = _ItemTags.get(tag);
		if (toRun == null)
			System.out.println("Error accessing tag method: " + tag);
		Object[] vals = Conversion.Convert(toRun, value);

		try {
			if (vals != null)
				toRun.invoke(item, vals);
		} catch (Exception e) {
			System.out.println("Error running tag method: " + tag);
			e.printStackTrace();
		}
	}

	protected static Character getTag(String line) {
		assert (line.length() > 0);
		return line.charAt(0);
	}

	protected static String getValue(String line) {
		if (line.length() > 2)
			return line.substring(2);
		else
			return "";
	}

	/**
	 * Reads the header section of the file, which contains information about
	 * the Frame.
	 * 
	 * @param frame
	 *            The Frame to assign the read values to.
	 * @param line
	 *            The line from the file to process.
	 * @return False if the end of the header has been reached, True otherwise.
	 */
	private void processHeaderLine(Frame frame, String line) throws IOException {
		// first separate the tag from the text
		Character tag = getTag(line);
		String value = getValue(line);
		Method toRun = _FrameTags.get(tag);

		if (toRun == null) {
			if (tag != 'v') {
				System.out.println("Tag '" + tag + "' in '" + line
						+ "' is not supported.");
			}
			return;
		}

		Object[] vals = Conversion.Convert(toRun, value);
		try {
			toRun.invoke(frame, vals);
		} catch (Exception e) {
			System.out.println("Error running method: "
					+ toRun.toGenericString());
			e.printStackTrace();
		}
	}

	// Returns a point from a String containing two ints separated by a space
	protected java.awt.Point separateValues(String line) {
		int x = Integer.parseInt(line.substring(0, line.indexOf(" ")));
		int y = Integer.parseInt(line.substring(line.indexOf(" ") + 1));

		return new java.awt.Point(x, y);
	}

	public static int getVersion(String fullpath) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fullpath));
			String next = "";
			// First read the header lines until we get the version number
			while (reader.ready() && !(next = reader.readLine()).equals("Z")) {
				if (isValidLine(next)) {
					Character tag = getTag(next);
					String value = getValue(next);
					if (tag.equals('V'))
						return Integer.parseInt(value);
				}
			}
		} catch (Exception e) {
		}
		return -1;
	}
}
