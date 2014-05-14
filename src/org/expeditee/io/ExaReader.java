package org.expeditee.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;

import org.expeditee.gui.AttributeUtils;
import org.expeditee.gui.AttributeUtils.Attribute;
import org.expeditee.gui.Frame;
import org.expeditee.items.Constraint;
import org.expeditee.items.Item;
import org.expeditee.items.Line;

/**
 * Experimental format which is designed to be more readable
 * NON FUNCTIONAL DUE TO CHANGES TO AttributeUtils
 * 
 * @author jts21
 *
 */
public class ExaReader implements FrameReader {

	public static final String EXTENTION = ".exa";

	private String _frameName;
	private HashMap<Integer, Item> _linePoints = new HashMap<Integer, Item>();
	private Frame _frame;
	private BufferedReader _reader;
	
	public ExaReader(String frameName) {
		_frameName = frameName;
	}
	
	public Frame readFrame(String fullPath) throws IOException {
		Reader in = new InputStreamReader(new FileInputStream(fullPath), "UTF-8");
		return readFrame(new BufferedReader(in));
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
		
		AttributeUtils.ensureReady();
		
		_reader = reader;
		
		_frame = new Frame();
		// Framename must be set before setting the frame number
		_frame.setName(_frameName);
		
		// Read frame properties
		processHeader();
		// Read items
		processItems();
		// Read lines
		processLines();
		// Read constraints
		processConstraints();
		
		_reader.close();
		_frame.setChanged(false);
		
		return _frame;
	}
	
	/**
	 * Splits a line at the first space
	 *	
	 */
	private static String[] getAttributeValue(String line) {
		int firstSpace = line.indexOf(' ');
		// attribute/value are separated by a space, so there must always be a space
		if(firstSpace <= 0) {
			System.err.println("Invalid line '" + line + "'");
			return null;
		}
		String attribute = line.substring(0, firstSpace).trim();
		String value = line.substring(firstSpace).trim();
		
		// System.out.println(attribute + " : " + value);
		
		return new String[] { attribute, value };
	}
	
	/**
	 * Load the frame properties based on the list in AttributeUtils
	 *	
	 */
	private void processHeader() throws IOException {
		
		String line = "";
		
		// sections are separated by an empty newline
		while(_reader.ready() && (line = _reader.readLine()) != null && !line.equals("")) {
			
			String[] av = getAttributeValue(line);
			if(av == null) {
				continue;
			}
			// TODO: NON FUNCTIONAL DUE TO CHANGES TO AttributeUtils
//			Attribute attribute = AttributeUtils._FrameAttrib.get(av[0].toLowerCase());
//			if(attribute == null || attribute.saveSetter == null) {
//				System.err.println("Attribute '" + attribute + "' is not supported");
//				continue;
//			}
//			Object[] vals = Conversion.Convert(attribute.saveSetter, av[1]);
//			try {
//				attribute.saveSetter.invoke(_frame, vals);
//			} catch(Exception e) {
//				System.err.println("Error running method: " + attribute.saveSetter.getName());
//				e.printStackTrace();
//			}
			
		}
	}
	
	/**
	 * Load the items and their properties based on the list in AttributeUtils
	 *	
	 */
	private void processItems() throws IOException {
		
		String line = "";
		Item item = null;
		
		// sections are separated by an empty newline
		while(_reader.ready() && (line = _reader.readLine()) != null && !line.equals("")) {
			
			String[] av = getAttributeValue(line);
			if(av == null) {
				continue;
			}
			if(av[0].equalsIgnoreCase("Item")) {
				// System.out.println("Found next item");
				// System.out.println(item);
				String[] type_id = getAttributeValue(av[1]);
				if(type_id == null) {
					throw new IOException("Invalid id at line '" + line +"'");
				}
				// if we fail to load an item, then item will be set to null,
				// and it's properties will be ignored
				// (i.e. we just skip to the next item)
				try {
					// refresh the last item loaded
					if(item != null) {
						item.updatePolygon();
						item.invalidateAll();
					}
					item = (Item) Class.forName(type_id[0]).getConstructor(int.class).newInstance(Integer.parseInt(type_id[1]));
					_linePoints.put(item.getID(), item);
    				_frame.addItem(item);
				} catch(ReflectiveOperationException e) {
					item = null;
				}
			} else if(item != null) {
				// TODO: NON FUNCTIONAL DUE TO CHANGES TO AttributeUtils
//				Attribute attribute = AttributeUtils._Attrib.get(av[0].toLowerCase());
//				if(attribute == null || attribute.saveSetter == null) {
//					System.err.println("Attribute '" + av[0] + "' is not supported");
//					continue;
//				}
//				Object[] vals = Conversion.Convert(attribute.saveSetter, av[1]);
//				try {
//					attribute.saveSetter.invoke(item, vals);
//				} catch(Exception e) {
//					System.err.println("Error running method: " + attribute.saveSetter.getName());
//					e.printStackTrace();
//				}
			}
		}
		// refresh the last item loaded
		if(item != null) {
			item.updatePolygon();
			item.invalidateAll();
		}
	}
	
	/**
	 * Load the lines
	 * This one's pretty hardcoded due to the Lines requiring their endpoints in the constructor
	 * Could possibly change that in the future
	 * 
	 */
	private void processLines() throws IOException {
		
		String line = "";
		int startID = -1;
		int endID = -1;
		int ID = -1;
		
		while(_reader.ready() && (line = _reader.readLine()) != null && !line.equals("")) {
			
			String[] av = getAttributeValue(line);
			if(av == null) {
				continue;
			}
			if(av[0].equalsIgnoreCase("Line")) {
				if(ID > 0 && startID > 0 && endID > 0) {
					_frame.addItem(new Line(_linePoints.get(startID), _linePoints.get(endID), ID));
				}
				ID = Integer.parseInt(av[1]);
				startID = -1;
				endID = -1;
			} else if(av[0].equalsIgnoreCase("Start")) {
				startID = Integer.parseInt(av[1]);
			} else if(av[0].equalsIgnoreCase("End")) {
				endID = Integer.parseInt(av[1]);
			}
		}
		if(ID > 0 && startID > 0 && endID > 0) {
			_frame.addItem(new Line(_linePoints.get(startID), _linePoints.get(endID), ID));
		}
	}
	
	/**
	 * Load the constraints
	 * As with the lines, this is pretty hardcoded
	 */
	private void processConstraints() throws IOException {
		
		String line = "";
		int type = -1;
		int startID = -1;
		int endID = -1;
		int ID = -1;
		
		while(_reader.ready() && (line = _reader.readLine()) != null && !line.equals("")) {
			
			String[] av = getAttributeValue(line);
			if(av == null) {
				continue;
			}
			if(av[0].equalsIgnoreCase("Constraint")) {
				
				if(type > 0 && ID > 0 && startID > 0 && endID > 0) {
					new Constraint(_linePoints.get(startID), _linePoints.get(endID), ID, type);
				}
				
				String[] type_id = getAttributeValue(av[1]);
				if(type_id == null) {
					throw new IOException("Invalid id at line '" + line +"'");
				}
				type = Integer.parseInt(type_id[0]);
				ID = Integer.parseInt(type_id[1]);
				startID = -1;
				endID = -1;
			} else if(av[0].equalsIgnoreCase("Start")) {
				startID = Integer.parseInt(av[1]);
			} else if(av[0].equalsIgnoreCase("End")) {
				endID = Integer.parseInt(av[1]);
			}
		}
		if(type > 0 && ID > 0 && startID > 0 && endID > 0) {
			new Constraint(_linePoints.get(startID), _linePoints.get(endID), ID, type);
		}
	}
	
	public static int getVersion(String fullpath) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fullpath));
			String line = "";
			// First read the header lines until we get the version number
			while(reader.ready() && (line = reader.readLine()) != null && !line.equals("")) {
				
				String[] av = getAttributeValue(line);
				if(av == null) {
					continue;
				}
				if(av[0].equalsIgnoreCase("Version")) {
					reader.close();
					return Integer.parseInt(av[1]);
				}
			}
		} catch (Exception e) {
		}
		if(reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
		return -1;
	}
	
}
