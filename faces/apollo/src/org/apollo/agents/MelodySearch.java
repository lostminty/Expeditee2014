package org.apollo.agents;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.swing.SwingUtilities;

import org.apollo.ApolloSystem;
import org.apollo.io.AudioPathManager;
import org.apollo.meldex.DynamicProgrammingAlgorithm;
import org.apollo.meldex.McNabMongeauSankoffAlgorithm;
import org.apollo.meldex.MeldexConversion;
import org.apollo.meldex.Melody;
import org.apollo.meldex.StandardisedMelody;
import org.apollo.meldex.WavSample;
import org.apollo.util.ExpediteeFileTextSearch;
import org.apollo.util.TextItemSearchResult;
import org.apollo.widgets.LinkedTrack;
import org.apollo.widgets.SampledTrack;
import org.apollo.widgets.TrackWidgetCommons;
import org.expeditee.agents.SearchAgent;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.settings.UserSettings;
import org.expeditee.settings.folders.FolderSettings;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.WidgetCorner;
import org.expeditee.items.widgets.WidgetEdge;

/**
 * Performs a melody search against track widgets within a whole frameset.
 * 
 * Uses meldex. Thanks David Bainbridge.
 * 
 * The agent runs a querry on the given track widget that launched it.
 * If the track launches it it does a full search for all tracks on the current frameset.
 * 
 * @author Brook Novak
 *
 */
public class MelodySearch extends SearchAgent {
	
	private long firstFrame = 1;

	private long maxFrame = Integer.MAX_VALUE;
	
	/** Either querry from raw audio or track widget: */
	private SampledTrack querryTrack = null; 
	
	/** Either querry from raw audio  or track widget: */
	private byte[] querryRawAudio = null;
	private AudioFormat querryRawAudioFormat = null;
	
	public static final String MELODY_METAFILE_PREFIX = ".";
	public static final String MELODY_METAFILE_SUFFIX = ".mel";
	
	/** The default score cut-off for omitting bad results above this value*/
	private static final float DEFAULT_SCORE_THRESHOLD = 400.0f;
	
	public MelodySearch() {
		super("MelodySearch");
	}
	
	/**
	 * 
	 * @param firstFrame
	 * 		The first frame number to start searching from (inclusive)
	 * 
	 * @param maxFrame
	 * 		The max frame number to start searching from (inclusive)
	 */
	public MelodySearch(long firstFrame, long maxFrame) {
		super("MelodySearch");
		this.firstFrame = firstFrame;
		this.maxFrame = maxFrame;
	}
	
	public void useRawAudio(byte[] querryRawAudio, AudioFormat querryRawAudioFormat) {

		if (querryRawAudio == null || querryRawAudioFormat == null) return;
		this.querryRawAudio = querryRawAudio;
		this.querryRawAudioFormat = querryRawAudioFormat;
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public boolean initialise(Frame frame, Item item) {
		if (!super.initialise(frame, item)) return false;
		
		// Get the track to querry .. if given one
		querryTrack = null;
		
		if (item != null) {
			
			InteractiveWidget iw = null;
			
			if (item instanceof WidgetCorner) {
				
				iw = ((WidgetCorner)item).getWidgetSource();
			} if (item instanceof WidgetEdge) {
				
				iw = ((WidgetEdge)item).getWidgetSource();
			}

			if (iw != null && iw instanceof SampledTrack) {
				querryTrack = (SampledTrack)iw;
			}
			
		}
		
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Frame process(Frame frame) {

		try {
			if(frame == null) {
				frame = FrameIO.LoadFrame(_startName + '0');
			}
			
			String path = frame.getPath();
			
			int count = FrameIO.getLastNumber(_startName);
			
			String trackPrefix = ItemUtils.GetTag(ItemUtils.TAG_IWIDGET) + ": ";
			String linkedTrackPrefix = trackPrefix;
			
			trackPrefix += SampledTrack.class.getName();
			linkedTrackPrefix += LinkedTrack.class.getName();
			
			Melody querryMelody = null;
			// Maps FrameName -> MelodySearchResult
			List<MelodySearchResult> melodyScores = new LinkedList<MelodySearchResult>();
			
			// If querrying a track widget then get its melody
			if (querryTrack != null) {
				assert(querryRawAudio == null);
				assert(querryRawAudioFormat == null);
				
				try {
					querryMelody = MeldexConversion.toMelody(querryTrack);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (OutOfMemoryError ex) {
					ex.printStackTrace();
				}
				
				if (querryMelody == null) { // abort - failed to get audio
					SwingUtilities.invokeLater(new ExpediteeMessageBayFeedback(
							"Melody search aborted: Failed to load tracks audio"));
					_results.addText("Melody search aborted: querry data not good enough to search with", 
							Color.RED, null, null, false);
					_results.addText("Click here for help on melody searches", 
							new Color(0, 180, 0), ApolloSystem.HELP_MELODYSEARCH_FRAMENAME, null, false);
					
					_results.save();
					return null;
				}
				
			// If querrying raw audio then get its melody
			} else if (querryRawAudio != null && querryRawAudioFormat != null) {
				assert(querryTrack == null);
				
				try {
					querryMelody = MeldexConversion.toMelody(querryRawAudio, querryRawAudioFormat);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (OutOfMemoryError ex) {
					ex.printStackTrace();
				}
				
				if (querryMelody == null) { // abort - failed to get audio
					SwingUtilities.invokeLater(new ExpediteeMessageBayFeedback(
							"Melody search aborted: Failed to proccess querry data"));
					_results.addText("Melody search aborted: querry data not good enough to search with", Color.RED, null, null, false);
					_results.addText("Click here for help on melody searches", 
							new Color(0, 180, 0), ApolloSystem.HELP_MELODYSEARCH_FRAMENAME, null, false);
					_results.save();
					return null;
				}
				
				
			}
			
			// Support range searching... i.e. frame100 - frame500
			for (long i = firstFrame;i <= maxFrame && i <= count; i++) {
				
				// Has requested stop?
				if (_stop) {
					break;
				}
				
				String frameName = _startName + i;
				
	
				overwriteMessage("Searching " + frameName); // RISKY
				// Note: cannot invoke later otherwise can congest the swing queue!
	
				// Perform prefix search
				List<TextItemSearchResult> results = null;
				try {
					
					String fullpath = getFullPath(frameName, path);
					
					if (fullpath != null) {
						results = ExpediteeFileTextSearch.prefixSearch(
								fullpath, 
								new String[] {trackPrefix, linkedTrackPrefix});
					}
					
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (results == null) continue; // frame does not exist or an error occured
				
				// If the frame exists / succeeded to perform a prefix search then increment the 
				// searched frame count...
				_frameCount++;
				
				// Is doing a full search for all tracks?
				if (querryMelody == null) {
					if(!results.isEmpty()) { 
						_results.addText(frameName + "(" + results.size() + ")", null, frameName, null, false);
						FrameGraphics.requestRefresh(true);
					}
					
				} else { // meldex querry
					
					MelodySearchResult bestScore = null;
					
					for (TextItemSearchResult res : results) {
						if (res.data == null) continue;
		
						// should this widget be indexed?
						if (res.containsData(SampledTrack.META_DONT_INDEX_AUDIO_TAG)) continue;
						
						// get the local filename
						String localFilename = null;
						String trackName = null;
						
						// Parse meta
						for (String data : res.data) {
							if (data.startsWith(SampledTrack.META_LOCALNAME_TAG) &&
									data.length() > SampledTrack.META_LOCALNAME_TAG.length()) {
								localFilename = data.substring(SampledTrack.META_LOCALNAME_TAG.length());
								if (trackName != null) break;
							} else if (data.startsWith(TrackWidgetCommons.META_NAME_TAG) &&
									data.length() > TrackWidgetCommons.META_NAME_TAG.length()) {
								trackName = data.substring(TrackWidgetCommons.META_NAME_TAG.length());
								if (localFilename != null) break;
							}
							
						}
						
						if (localFilename == null)
							continue;
						
						// Safety: omit this if it is infact the very widget we are searching
						if (querryTrack != null && querryTrack.getLocalFileName().equals(localFilename))
							continue;
						
						Melody testMelody = null;
						
						// Get cached melody from file if it is up to date
						String metaFilePath = 
							AudioPathManager.AUDIO_HOME_DIRECTORY 
							+ MELODY_METAFILE_PREFIX
							+ localFilename
							+ MELODY_METAFILE_SUFFIX;
						
						File localFile = new File(AudioPathManager.AUDIO_HOME_DIRECTORY + localFilename);
						if (!localFile.exists()) continue;
						
						File metaFile = new File(metaFilePath);
						
						// If there is a metafile that contains the serialized melody and is up to date...
						if (metaFile.exists() && metaFile.lastModified() >= localFile.lastModified()) {
							
							try {
								StandardisedMelody sm = StandardisedMelody.readMelodyFromFile(metaFilePath);
								if (sm != null && sm instanceof Melody) {
									testMelody = (Melody)sm;
								}
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
							}
							
						}
						
						if (_stop) break;
						
						// If did not manage to load any melody meta ... calculate the meta
						if (testMelody == null) {
	
							// Load wave file
							WavSample sampleTrack = new WavSample();
							if (!sampleTrack.loadFromFile(localFile)) {
								continue;
							}
							if (_stop) break;
							
							// Transcribe melody
							try {
								testMelody = MeldexConversion.toMelody(sampleTrack);
							} catch (IOException ex) {
								ex.printStackTrace();
							} catch (OutOfMemoryError ex) {
								ex.printStackTrace();
							}
							
							if (testMelody == null) continue;
							if (_stop) break;
	
							// Save meta
							if (metaFile.exists()) metaFile.delete();
							
							try {
								StandardisedMelody.writeMelodyToFile(metaFilePath, testMelody);
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
							
						}
		
						assert(testMelody != null);
						assert(querryMelody != null);
						
						// Omit bogus melody transcriptions for which audio must not of have enough rests
						// to give a strong enough representation. This avoids bogus meta getting better results
						// than something actually more meaningful.
						if (testMelody.getLength() <= 1) continue;
						
						DynamicProgrammingAlgorithm dpa = new McNabMongeauSankoffAlgorithm();
			
						float score = dpa.matchToPattern(querryMelody, testMelody, DynamicProgrammingAlgorithm.MATCH_ANYWHERE);
						
						if (bestScore == null || bestScore.getScore() < score) {
							
							bestScore = new MelodySearchResult(
									frameName, 
									score,
									trackName,
									localFilename
									);
						}
						
						
					} // next result in frame
	
					if (_stop) {
						break;
					}
					
					if (bestScore != null) {
						melodyScores.add(bestScore);
					}
	
				}
				
				
			} // Next frame
			
			
			// Did do melody matching? add ordered results
			if (querryMelody != null) {
				
				float threshold = DEFAULT_SCORE_THRESHOLD; // TODO: Have tolerance option
				
				// Order results descending
				Collections.sort(melodyScores);

				if (melodyScores.isEmpty() || melodyScores.get(0).getScore() > threshold) {
					_results.addText("No matches", Color.RED, null, null, false);
					_results.addText("Click here to find out how to improve your melody searches", 
							new Color(0, 180, 0), ApolloSystem.HELP_MELODYSEARCH_FRAMENAME, null, false);
				} else {
					
					int rank = 1;
					for (MelodySearchResult melRes : melodyScores) {

						if (melRes.getScore() <= threshold) {
							
							String name = melRes.getTrackName();
							if (name == null || name.length() == 0)
								name = "Unnamed";
		
							_results.addText(rank + ": " + melRes.getParentFrame() + " ("
										+ name + ")",
										null, 
										melRes.getParentFrame(), null, false);
							
							
						}
						
						rank ++;
						
					}
					
				}
	
			}
			
			if (_stop) {
				_results.addText("Search cancelled", Color.RED, null, null, false);
			}
			
			// Spit out result(s)
			_results.save();
	
			String resultFrameName = _results.getName();
			if (_clicked != null)
				_clicked.setLink(resultFrameName);
	
			return _results.getFirstFrame();
			
			}
		finally {
			querryRawAudio = null; // Free memory
		}
		
	}
	

	
	
	/**
	 * Gets the full path from a given framename and path
	 * @param frameName
	 * @param path
	 * @return
	 */
	private String getFullPath(String frameName, String path) {
		
		String fullPath = null;
		if (path == null) {
			for (String possiblePath : FolderSettings.FrameDirs.get()) {
				fullPath = FrameIO.getFrameFullPathName(possiblePath, frameName);
				if (fullPath != null)
					break;
			}
		} else {
			fullPath = FrameIO.getFrameFullPathName(path, frameName);
		}
		
		return fullPath;

	}
	
	/**
	 * Safely outputs a message on the messagebay .. if ran on swing thread.
	 * @author Brook Novak
	 *
	 */
	private class ExpediteeMessageBayFeedback implements Runnable {
		
		String feedbackMessage; 
		
		public ExpediteeMessageBayFeedback(String feedbackMessage) {
			assert(feedbackMessage != null);
			this.feedbackMessage = feedbackMessage;
		}
		
		
		public void run() {
			assert(feedbackMessage != null);
			overwriteMessage(feedbackMessage);
		}
	}
	
	
}
