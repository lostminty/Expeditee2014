package org.apollo.util;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apollo.agents.MelodySearch;
import org.apollo.io.AudioPathManager;
import org.apollo.widgets.SampledTrack;
import org.expeditee.settings.UserSettings;
import org.expeditee.settings.folders.FolderSettings;
import org.expeditee.items.ItemUtils;

/**
 * Purges a
 * @author Brook Novak
 *
 */
public final class AudioPurger {
	
	private AudioPurger() {
		
	}
	
	public static void purgeLocalAudio() {
		
		String[] audioFiles = AudioPathManager.getAudioFileNames();
		if (audioFiles == null || audioFiles.length == 0) return;
		
		
		// Search every profile and frameset
		Set<String> referencedAudio = new HashSet<String>();
		
		for (String path : FolderSettings.FrameDirs.get()) {
			
			File frameDir = new File(path);
			
			if (frameDir.exists() && frameDir.isDirectory()) {
				
				try {
					getReferenceAudioFiles(frameDir, referencedAudio);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// Delelete all unferenced audio thats using up precious disk space
		for (String audioFile : audioFiles) {
			if (!referencedAudio.contains(audioFile)) {
				
				File unreferencedFile = new File(AudioPathManager.AUDIO_HOME_DIRECTORY + audioFile);
				ApolloSystemLog.println("Deleting unreference audio file \"" + audioFile + "\"");
				unreferencedFile.delete();
				
				// Also delete any melody meta
				File melodyMetaFile = new File(
						AudioPathManager.AUDIO_HOME_DIRECTORY +
						MelodySearch.MELODY_METAFILE_PREFIX + 
						audioFile +
						MelodySearch.MELODY_METAFILE_SUFFIX);
				
				if (melodyMetaFile.exists()) melodyMetaFile.delete();
				
			}
		}

	}
	
	private static void getReferenceAudioFiles(File dir, Set<String> refedAudio) throws IOException {
		
		String[] filenames = dir.list();
		if (filenames == null) throw new IOException();
		
		String trackPrefix = ItemUtils.GetTag(ItemUtils.TAG_IWIDGET) 
			+ ": " +  SampledTrack.class.getName();
		
		int filenameMetaLength = SampledTrack.META_LOCALNAME_TAG.length();
		
		for (String frameFile : filenames) {
			
			File f = new File(dir.getPath() +  File.separatorChar + frameFile);
			if (!f.exists()) continue;
			
			// Recurse
			if (f.isDirectory()) {
				getReferenceAudioFiles(f, refedAudio);
				continue;
			}
			
			// Perform text search on sampled track widgets
			List<TextItemSearchResult> results = 
				ExpediteeFileTextSearch.prefixSearch(f.getAbsolutePath(), new String[] {trackPrefix});
			
			// For each track widget
			for (TextItemSearchResult result : results) {
				
				// For each peice of meta data
				if (result.data != null) {
					for (String meta : result.data) {
						
						// Find the local-filename meta
						if (meta.length() > filenameMetaLength &&
								meta.startsWith(SampledTrack.META_LOCALNAME_TAG)) {
							
							// Keep meta
							refedAudio.add(meta.substring(filenameMetaLength));
							break;
						}
					}
				}
			}
			
	
			
		}
	}
	
	public static void main(String[] args) {
		purgeLocalAudio();
	}

}
