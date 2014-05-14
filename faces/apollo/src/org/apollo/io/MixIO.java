package org.apollo.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apollo.audio.util.TrackMixSubject;

/**
 * A naive approach for saving and loading of mixes....
 * Very hacky at this point and if released should re-think how banks are saved and loaded.
 * 
 * @author Brook Novak
 *
 */
public class MixIO {

	/**
	 * Loads the master mix
	 * 
	 * @param masterMixFilepath
	 * @return
	 * 		The solo prefix.
	 */
	public static TrackMixSubject loadMasterMix(String masterMixFilepath) {
		assert(masterMixFilepath != null);
		
		File mixFile = new File(masterMixFilepath);
		if (!mixFile.exists()) {
			try {
				mixFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("ERROR: The master mix has been lost");
				return null;
			}
		}

		BufferedReader in = null;
		
		TrackMixSubject masterMix = null;
		
		try {
			
			// Open the vbase for reading
			in = new BufferedReader(new FileReader(mixFile)); 
			
			String line = null;
			// Read the sbase file and check all names
			while ((line = in.readLine()) != null) {
				line = line.trim();
				masterMix = TrackMixSubject.fromString(line);
				if (masterMix != null) break;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
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

		return masterMix;
	}
	
	/**
	 * Saves a master mix
	 * 
	 * @param masterMixFilepath
	 * 		Where to save
	 * 
	 * @param mmix
	 * 		The mix to save as a master mix
	 */
	public static void saveMasterMix(String masterMixFilepath, TrackMixSubject mmix) {
		
		assert(masterMixFilepath != null);
		assert(mmix != null);
		
		File mixFile = new File(masterMixFilepath);
		if (!mixFile.exists()) {
			try {
				mixFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("ERROR: Unable to save master mix");
				return;
			}
		}
		
		FileWriter out = null;
		
		try {
			out = new FileWriter(mixFile, false); 
			out.write(mmix.toParseableString());
			
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
		
	}
	

	/**
	 * Saves banks to file
	 * 
	 * @param mixBatchPath
	 * 		Where to save
	 * 
	 * @param banks
	 * 		The mixes to write to file
	 * 
	 */
	public static void saveBanks(String mixBatchPath, List<TrackMixSubject> banks) {
		
		assert(mixBatchPath != null);
		assert(banks != null);
		
		File mixFile = new File(mixBatchPath);
		if (!mixFile.exists()) {
			try {
				mixFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("ERROR: Unable to save mixes");
				return;
			}
		}
		
		FileWriter out = null;
		
		try {

			out = new FileWriter(mixFile, false); 
			
			for (int i = 0; i < banks.size(); i++) {
				
				TrackMixSubject mix = banks.get(i);
				assert(mix != null);
				out.write(mix.toParseableString() + "\n");
			}

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
		
	}
	
	/**
	 * 
	 * @param banksFilepath
	 * 		The filepath of the banks file to load.
	 * 
	 * @param banks
	 * 		A list to load the banks into. Must not be null - must be empty.
	 * 
	 * @return
	 * 		True of success. False if IO operatoin failed / file does not exist. 
	 */
	public static boolean loadBanks(
			List<TrackMixSubject> banks,
			String banksFilepath) {
		
		assert(banks != null);
		assert(banksFilepath != null);
		assert(banks.isEmpty());
		
		File mixFile = new File(banksFilepath);
		if (!mixFile.exists()) {
			try {
				mixFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("ERROR: The master mix has been lost");
			}
			return false;
		}

		BufferedReader in = null;
		
		try {
			
			// Open the vbase for reading
			in = new BufferedReader(new FileReader(mixFile)); 
			
			String line = null;
			// Read the sbase file...
			while ((line = in.readLine()) != null) {
				
				line = line.trim();
				if (line.length() == 0) continue;
				
				TrackMixSubject mix = TrackMixSubject.fromString(line);
			
				if (mix != null) 
					banks.add(mix);
	
			}

		} catch (Exception e) {
			e.printStackTrace();
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

		return true;
	}
	
}
