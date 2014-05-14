package org.apollo.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

import org.apollo.audio.structure.AudioStructureModel;
import org.apollo.util.ApolloSystemLog;
import org.apollo.util.RegExpFileFilter;

/**
 * Generates audio paths.
 * 
 * @author Brook Novak
 *
 */
public class AudioPathManager {

	private AudioPathManager() {
		// util constructor
	}

	/** Suffixed with the native file separator */
	public static String AUDIO_HOME_DIRECTORY = "audio" + File.separatorChar;
	
	/** Stores all virtual names created by this repository. */
	private static final String VIRTUAL_NAME_BASEFILE = ".vnames";

	public static final int PREAMBLE_LENGTH = 6;

	private static long counter = 0;

	private static Random rand = new Random(System.currentTimeMillis());

	/**
	 * Class initializer.
	 * Ensures that the home directory(ies) exist and preps the counter
	 */
	static {

		String expeditee_home = System.getProperty("expeditee.home");

		if (expeditee_home != null) {
		    AUDIO_HOME_DIRECTORY = expeditee_home + File.separator + AUDIO_HOME_DIRECTORY;		    
		}

		// Ensure audio directory exists
		File dir = new File(AUDIO_HOME_DIRECTORY);
		if (!dir.exists()) {
			if (!dir.mkdir()) {
				ApolloSystemLog.println("Unable to create missing audio home directory");
			}
		}

		// Load counter
		loadCounter(dir);
	}

	/**
	 * Loads the counter
	 * @param dir The dir where the audio files live.
	 */
	private static void loadCounter(File dir) {
		String[] files = getAudioFileNames(dir);
		if (files == null) { // Due to IO exception
			ApolloSystemLog.printError("Failed to read audio directory");
			return;
		}
		long count = 0;
		if (files != null) {
			for (String f : files) {
	
				int start = -1, end = -1;
				for (int i = 0; i < f.length(); i++) {
					if (Character.isDigit(f.charAt(i)) && start == -1) {
						start = i;
					} else if (!Character.isDigit(f.charAt(i)) && start != -1) {
						end = i;
						break;
					}
				}
				
				if (start >= 0 && end == -1) end = f.length();
	
				if (start > 0 && end >= start) { // not start >= 0 since missing preable
	
					String tmp = f.substring(start, end);
					long l = Long.parseLong(tmp);
					if (l >= count)
						count = l + 1;
				}
			}
		}

		counter = count;
	}
	
	/**
	 * @param dir
	 * @return
	 * 		Null if an IO exception occurs
	 */
	private static String[] getAudioFileNames(File dir) {
		return dir.list(new RegExpFileFilter("^[A-Z]+\\d+.*$"));
	}
	
	/**
	 * @return
	 * 		The list of audio files in the audio home directory.
	 * 		Can be empty. Null if directory does not exist or failed to
	 * 		read list of files.
	 */
	public static String[] getAudioFileNames() {
		File audioDir = new File(AUDIO_HOME_DIRECTORY);
		if (!audioDir.exists() || !audioDir.isDirectory()) return null;
		return getAudioFileNames(audioDir);
	}
	
	
	/**
	 * @return The current counter.
	 */
	public static long getCounter() {
		return counter;
	}

	/**
	 * Generates a filename that is unique within the AUDIO_HOME_DIRECTORY.
	 * 
	 * @param extension
	 * 			The extension of the filename to generate. Exclude period.
	 * 
	 * @return 
	 * 		A free unused filename with the given extension. Excludes directory.
	 * 		Even if the path return is not used, it will never be re-generated.
	 */
	public static String generateLocateFileName(String extension) {

		String filename = generateRandomFilenameBase() + "." + extension;
		
		File f = new File(AUDIO_HOME_DIRECTORY + filename);

		if (AudioStructureModel.getInstance().getTrackGraphInfo(filename, null) != null ||
				f.exists()) { // recurse: until found a free path:
			return generateLocateFileName(extension);
		}

		return filename;
	}
	
	private static String generateRandomFilenameBase() {
		
		counter++; 
		if (counter < 0) counter = 0; // wraps positivey

		// Generate random alpha preamble
		byte[] bytes = new byte[PREAMBLE_LENGTH];
		rand.nextBytes(bytes);
		

		for (int i = 0; i < PREAMBLE_LENGTH; i++) {
			if (bytes[i] < 0) bytes[i] *= -1;
			int alphacap = (65 + (Math.abs(bytes[i]) % 26)); // A-Z in US ASCII encoding
			bytes[i] = (byte) alphacap;
			
		}

		// Decode ASCII Byte array to javas UTF-16 encoding
		String preamble = null;

		try {
			preamble = new String(bytes, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			preamble = "AUDIO";
		}

		// Build the filename string
		return preamble + counter;
		
	}

	
	
	/**
	 * Lots of room for improvement here, ensures that a virtual filename is unique .. stores 
	 * allocated virtual filenames on file.
	 * 
	 * @return
	 * 			The new virtual filename.
	 */
	public static String generateVirtualFilename() {

		// Ensure that the name base file exists. Assumes the audio home directory exists.
		File vnameBase = new File(AUDIO_HOME_DIRECTORY + VIRTUAL_NAME_BASEFILE);
		if (!vnameBase.exists()) {
			try {
				vnameBase.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		String vname = null;
		
		while (vname == null) {
			vname = generateRandomFilenameBase() + ".vrt";
			assert(vname != null);
			
			BufferedReader in = null;
			String line = null;
			
			try {
				
				// Open the vbase for reading
				in = new BufferedReader(new FileReader(vnameBase)); 
				
				// Read the sbase file and check all names
				while ((line = in.readLine()) != null) {
					line = line.trim();
					if (line.equalsIgnoreCase(vname)) break;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				break; // just use the last generated filename...
			
			// Clean up
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			// Check to see if name is infact unique
			if (AudioStructureModel.getInstance().getLinkedTrackGraphInfo(vname, null) == null &&
					(line == null ||  !line.equalsIgnoreCase(vname))) {
				// If so, then enter new entry into the db.
				FileWriter out = null;
				
				try {
					// Open the vbase for appending
					out = new FileWriter(vnameBase, true); 
					out.write("\n" + vname);
					
				} catch (IOException e) {
					e.printStackTrace();
					
				// Clean up
				} finally {
					if (out != null) {
						try {
							out.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

			} else vname = null;
		}
		
		// Return the generated vname
		return vname;

	}
}
