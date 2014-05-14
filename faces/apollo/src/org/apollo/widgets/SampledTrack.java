package org.apollo.widgets;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;

import org.apollo.AudioFrameKeyboardActions;
import org.apollo.AudioFrameMouseActions;
import org.apollo.audio.ApolloPlaybackMixer;
import org.apollo.audio.ApolloSubjectChangedEvent;
import org.apollo.audio.SampledTrackModel;
import org.apollo.audio.TrackSequence;
import org.apollo.audio.structure.AudioStructureModel;
import org.apollo.audio.structure.TrackGraphNode;
import org.apollo.audio.util.SoundDesk;
import org.apollo.audio.util.Timeline;
import org.apollo.audio.util.TrackMixSubject;
import org.apollo.gui.EditableSampledTrackGraphView;
import org.apollo.gui.ExpandedTrackManager;
import org.apollo.gui.FrameLayoutDaemon;
import org.apollo.gui.PlaybackControlPopup;
import org.apollo.gui.SampledTrackGraphView;
import org.apollo.gui.Strokes;
import org.apollo.gui.SampledTrackGraphView.EffecientInvalidator;
import org.apollo.io.AudioIO;
import org.apollo.io.AudioPathManager;
import org.apollo.io.IconRepository;
import org.apollo.io.AudioIO.AudioFileLoader;
import org.apollo.items.EmulatedTextItem;
import org.apollo.items.EmulatedTextItem.TextChangeListener;
import org.apollo.mvc.Observer;
import org.apollo.mvc.Subject;
import org.apollo.mvc.SubjectChangedEvent;
import org.apollo.util.AudioMath;
import org.apollo.util.Mutable;
import org.apollo.util.PopupReaper;
import org.apollo.util.TrackModelHandler;
import org.apollo.util.TrackModelLoadManager;
import org.apollo.util.TrackNameCreator;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.MouseEventRouter;
import org.expeditee.gui.PopupManager;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.HeavyDutyInteractiveWidget;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.InteractiveWidgetInitialisationFailedException;
import org.expeditee.items.widgets.InteractiveWidgetNotAvailableException;

/**
 * The sampled track widgets in apollo.
 * 
 * @author Brook Novak
 *
 */
public class SampledTrack extends HeavyDutyInteractiveWidget 
	implements TrackModelHandler, EffecientInvalidator, Observer {

	/** The observered subject. Can be null */
	private SampledTrackModel trackModel = null;
	
	/** Used for the loading phase. Can change serveal times of a lifetime. for example after import and save it becomes local */
	private String loadFilenameArgument = null; // preproceeds with ARG_IMPORT_TAG if importing.
	
	private String localFileName; // Immutable - assigned on constuction
	
	/** Used for dumping audio to a temp file when deleted - to free memory.
	 * Protocol: Set when the widget is deleted. Unset when loaded. */
	private File recoveryFile = null;
	
	private boolean shouldOmitAudioIndexing = false;
	
	private EditableSampledTrackGraphView fulltrackView;
	private EmulatedTextItem nameLabel; 
	private PlaybackPopup playbackControlPopup = null;
	private TrackMixSubject trackMix; // immutable - like local filename
	
	/** The amount of load bar that is allocated to file loading */
	private static final float FILE_LOADING_PERCENT_RANGE = 0.95f;

	/** For parsing arguments */
	public static final String ARG_IMPORT_TAG = "if=";
	
	/** For parsing metadata */
	public static final String META_LOCALNAME_TAG = "ln=";
	
	private static final Stroke FREESPACE_OUTLINING = Strokes.SOLID_2;
	
	private static final Color SEMI_TRANSPARENT_FREESPACE_BACKCOLOR = new Color(
			FREESPACE_BACKCOLOR.getRed(), FREESPACE_BACKCOLOR.getGreen(),
			FREESPACE_BACKCOLOR.getBlue(), 128);
	
	
	/** If a track widget has a data string thaty contains META_SHOULD_INDEX_AUDIO_TAG, then
	 * the track should not be indexed for searching the audio.... default isdoes not exist
	 */
	public static final String META_DONT_INDEX_AUDIO_TAG = "dontindexaudio";

	/**
	 * Constructor used for loading directly from given bytes.
	 * 
	 * @param source
	 * 
	 * @param audioBytes
	 * 
	 * @param format
	 * 
	 * @param mixTemplate
	 * 		Can be null
	 * 
	 */
	private SampledTrack(Text source, byte[] audioBytes, AudioFormat format, TrackMixSubject mixTemplate) {
		
		super(source, new EditableSampledTrackGraphView(), 
				100, -1, 
				FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, 
				TrackWidgetCommons.CACHE_DEPTH,
				true);

		// Must set upon construction - always
		localFileName = AudioPathManager.generateLocateFileName("wav");
		updateData(META_LOCALNAME_TAG, META_LOCALNAME_TAG + localFileName);
		
		trackModel = new SampledTrackModel(audioBytes, format, localFileName);
		
		// Ensure that the model is marked as modiefied so that it will save
		trackModel.setAudioModifiedFlag(true);
		
		trackModel.setName(getStrippedDataString(TrackWidgetCommons.META_NAME_TAG));

		// Create the immutable mix subject
		if (mixTemplate == null)
			trackMix = SoundDesk.getInstance().getOrCreateMix(SoundDesk.createPureLocalChannelID(this));
		
		else trackMix = SoundDesk.getInstance().createMix(
				SoundDesk.createPureLocalChannelID(this), 
				mixTemplate.getVolume(), 
				mixTemplate.isMuted());
				
		// Keep meta as constant as possible for best results
		updateData(TrackWidgetCommons.META_RUNNINGMSTIME_TAG, TrackWidgetCommons.META_RUNNINGMSTIME_TAG + getRunningMSTimeFromRawAudio());
		
		createGUI();
		
		initObservers();
		
	}

	/**
	 * Constructor called by Expeditee. Eventually loads audio from file.
	 * 
	 * @param source
	 * 
	 * @param args
	 * 		Can have a ARG_IMPORT_TAG argument...
	 */
	public SampledTrack(Text source, String[] args) {
		super(source, new EditableSampledTrackGraphView(), 
				100, -1, 
				FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, 
				TrackWidgetCommons.CACHE_DEPTH);
		
		// Read the metadata
		localFileName = getStrippedDataString(META_LOCALNAME_TAG);

		// Ensure the local filename is assigned - even if file does not exist...
		// Also it could be importing a file...
		if (localFileName == null) {
			localFileName = AudioPathManager.generateLocateFileName("wav");
			updateData(META_LOCALNAME_TAG, META_LOCALNAME_TAG + localFileName);
		}

		trackMix = SoundDesk.getInstance().getOrCreateMix(SoundDesk.createPureLocalChannelID(this));

		loadFilenameArgument = localFileName;
		if (args != null) { // parse args
			for (String arg : args) {
				if (arg != null && arg.length() > ARG_IMPORT_TAG.length()) {
					loadFilenameArgument = arg;
				}
			}
		}
		
		createGUI();

	}
	
	private void initObservers() {
		
		// Listen for model events
		trackModel.addObserver(this);
		trackModel.addObserver(fulltrackView);
		fulltrackView.setMix(trackMix); // use the same mix/channel for playback
		ExpandedTrackManager.getInstance().addObserver(this);
		
		// Show graph as being selected if expanded / pending to expand.
		if (ExpandedTrackManager.getInstance().isTrackInExpansionSelection(trackModel)) {
			fulltrackView.setBackColor(new Color(100, 100, 100), new Color(120, 120, 120));
		} else {
			fulltrackView.setBackColor(SampledTrackGraphView.DEFAULT_BACKGROUND_COLOR, SampledTrackGraphView.DEFAULT_BACKGROUND_HIGHTLIGHTS_COLOR);
		}

	}

	private void createGUI() {
		
		// Set widget as a fixed size widget- using width from last recorded width in the meta data.
		int width = getStrippedDataInt(TrackWidgetCommons.META_LAST_WIDTH_TAG, -1);
		if (width >= 0 && width < FrameLayoutDaemon.MIN_TRACK_WIDGET_WIDTH) {
			width = FrameLayoutDaemon.MIN_TRACK_WIDGET_WIDTH;
		}
		
		if (width < 0) {
			setSize(-1, -1, 
					FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, 
					FrameLayoutDaemon.TRACK_WIDGET_DEFAULT_WIDTH, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT);
		} else {
			setSize(width, width, 
					FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT, 
					width, FrameLayoutDaemon.TRACK_WIDGET_HEIGHT);
		}
		
		shouldOmitAudioIndexing = containsDataTrimmedIgnoreCase(META_DONT_INDEX_AUDIO_TAG);

		playbackControlPopup = new PlaybackPopup();
		
		fulltrackView = (EditableSampledTrackGraphView)_swingComponent;
		fulltrackView.setAlwaysFullView(true);
		
		/*fulltrackView.setInvalidator(new EffecientInvalidator() {
			public void onGraphDirty(SampledTrackGraphView graph, Rectangle dirty) {
		        dirty.translate(getX(), getY() - 1);
		        FrameGraphics.invalidateArea(dirty); 
		        FrameGraphics.refresh(true); 
			}
		});*/
		
		// Auto-show playback popup.
		// Block messages if the track is expanded
		fulltrackView.addMouseMotionListener(new MouseMotionListener() {

			public void mouseDragged(MouseEvent e) {
				if (nameLabel != null) {
					if (nameLabel.onMouseDragged(e)) {
						e.consume();
					}
				}
			}

			public void mouseMoved(MouseEvent e) {

				if (nameLabel != null) {
					nameLabel.onMouseMoved(e, fulltrackView);
				}
				
				if (playbackControlPopup == null) {
					playbackControlPopup = new PlaybackPopup();
				} 

				// Only show popup iff there is nothing expanded / expanding.
				if (!PopupManager.getInstance().isShowing(playbackControlPopup) &&
						!ExpandedTrackManager.getInstance().doesExpandedTrackExist()) {	
					
					// Get rid of all popups
					PopupManager.getInstance().hideAutohidePopups();
					
					Rectangle animationSource = _swingComponent.getBounds();
					
					// Determine where popup should show
					int x = SampledTrack.this.getX();
					int y = SampledTrack.this.getY() - playbackControlPopup.getHeight() - 2; // by default show above
					
					// I get sick.dizzy from the popup expanding from the whole thing...
					animationSource.height = 1;
					animationSource.width = Math.min(animationSource.width, playbackControlPopup.getWidth());
					
					if (y < 0) {
						y = SampledTrack.this.getY() + SampledTrack.this.getHeight() + 2;
						animationSource.y = y - 2;
					} 

					// Animate the popup
					PopupManager.getInstance().showPopup(
							playbackControlPopup, 
							new Point(x, y), 
							fulltrackView,
							PopupManager.getInstance().new ExpandShrinkAnimator(
									animationSource,
									Color.LIGHT_GRAY));
					
					PopupReaper.getInstance().initPopupLifetime(
							playbackControlPopup, 
							PopupManager.getInstance().new ExpandShrinkAnimator(
									animationSource,
									Color.LIGHT_GRAY), 
									TrackWidgetCommons.POPUP_LIFETIME);
					
				} else {
					PopupReaper.getInstance().revivePopup(playbackControlPopup, TrackWidgetCommons.POPUP_LIFETIME);
				}
				
			}

		});
		
		// Expand on double click
		// Block messages if the track is expanded
		fulltrackView.addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				if (trackModel == null) return;
				
				if (nameLabel != null) {
					if (nameLabel.onMouseClicked(e)) {
						e.consume();
						return;
					}
				}
				
				if (e.getClickCount() >= 2) {
					
					expand(e.isControlDown());
					
				}

			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
				if (nameLabel != null) {
					if (nameLabel.onMousePressed(e)) {
						e.consume();
						return;
					}
				}
				
				// Consume events if track is selected for expansion
				if (ExpandedTrackManager.getInstance().isTrackInExpansionSelection(trackModel) &&
						e.getButton() != MouseEvent.BUTTON1) { // but allow selection only
					e.consume();
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (nameLabel != null) {
					if (nameLabel.onMouseReleased(e)) {
						e.consume();
					}
				}
				
				 if (!e.isConsumed() && e.getButton() == MouseEvent.BUTTON2) {
						if (split(true))
							e.consume();
					}
				
				// Consume events if track is selected for expansion
				if (ExpandedTrackManager.getInstance().isTrackInExpansionSelection(trackModel)) {
					e.consume();
				}
		
			}
		
		});
		
		fulltrackView.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				if (!e.isControlDown() && nameLabel != null) {
					if (nameLabel.onKeyPressed(e, fulltrackView)) {
						e.consume();
					}
				}
			}

			public void keyReleased(KeyEvent e) {
				if (!e.isControlDown() && nameLabel != null) {
					if (nameLabel.onKeyReleased(e, fulltrackView)) {
						e.consume();
					}
				}
				
				// Toggle pitch-track indexing
				if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_I) {
					setShouldOmitIndexAudio(!shouldOmitIndexAudio());
					FrameGraphics.refresh(true);
				}
				
				// Delete-and-Split audio command
				else if (!e.isControlDown() && e.getKeyCode() == KeyEvent.VK_DELETE) {
					
					if (split(false))  // try Perform delete-split
						e.consume();
					
				}
				// Convert to linked track
				else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_L) {

					Frame current = DisplayIO.getCurrentFrame();
					
					if (current != null) {
						
						String name =  getName();
						if (name == null) name = "Unamed";
						
						Mutable.Long initTime = getInitiationTimeFromMeta();
						if (initTime == null) initTime = Mutable.createMutableLong(0);
						
						Text linkSource = new Text(current.getNextItemID());
						linkSource.addToData(TrackWidgetCommons.META_NAME_TAG + name);
						linkSource.setPosition(getPosition());
						linkSource.setParent(current);
						LinkedTrack linkedVersion = new LinkedTrack(linkSource, null);

						// Save any changes in the audio - or save for the first time
						if (trackModel.isAudioModified())
							saveWidgetData(); // a little lag is OK .. could make smarter if really need to
						
						// Create a new frame to hold the track
						Frame newFrame = FrameIO.CreateNewFrame(SampledTrack.this.getFirstCorner());
	
						// Remove track from current frame
						removeSelf();
						
						// Add to new frame
						newFrame.addAllItems(getItems());
						
						// Save changes
						FrameIO.SaveFrame(newFrame);
						
						// Link it
						linkedVersion.setLink(newFrame.getName(), null);
						
						// Add the new link
						current.addAllItems(linkedVersion.getItems());
						
						// Ensure initiation times are retained to the exact frame... avoiding loss due to resolution
						linkedVersion.setInitiationTime(initTime.value);
	

					}
	
				} else if (e.isControlDown() && 
						(e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT)) {
					e.consume();
					AudioFrameKeyboardActions.adjustInitationTime(SampledTrack.this, e.getKeyCode() == KeyEvent.VK_LEFT);
				} else if (e.isControlDown() && 
						(e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN)) {
					e.consume();
					AudioFrameKeyboardActions.adjustVerticlePosition(SampledTrack.this, e.getKeyCode() == KeyEvent.VK_UP);
				}

			}

			public void keyTyped(KeyEvent e) {
			}
			
		});
		
		
		
		nameLabel = new EmulatedTextItem(_swingComponent, new Point(10, 20));
		nameLabel.setBackgroundColor(Color.WHITE);	
		
		String metaName = getStrippedDataString(TrackWidgetCommons.META_NAME_TAG);
		if (metaName == null) metaName = "Untitled";
		nameLabel.setText(metaName); 

		nameLabel.addTextChangeListener(new TextChangeListener() { //  a little bit loopy!

			public void onTextChanged(Object source, String newLabel) {
				if (trackModel != null && !nameLabel.getText().equals(trackModel.getName())) {
					trackModel.setName(nameLabel.getText());
					SampledTrack.this.updateData(TrackWidgetCommons.META_NAME_TAG, trackModel.getName());
					
				}
			}

		});
		
		// Make sure the above mouse listeners get first serve
		fulltrackView.reAddListeners();
		
		// Make sure border color is correct
		updateBorderColor();
		setWidgetEdgeThickness(TrackWidgetCommons.STOPPED_TRACK_EDGE_THICKNESS);
	}

	/**
	 * Creates a {@link SampledTrack} instantly from audio bytes in memory and adds it to a given frame.
	 * 
	 * @param audioBytes
	 * 			The audio samples.
	 * 
	 * @param format
	 * 			The format of the audio samples.
	 * 
	 * @param targetFrame
	 * 			The frame that the widget will reside.
	 * 
	 * @param x
	 * 
	 * @param yinitTime
	 * 
	 * @param name
	 * 			The name of the new track. If null then a default name will be used.
	 * 
	 * @param mixTemplate 
	 * 			The mix data to clone for the new sampled tracks mix.
	 * 			If null then default mix settings will be used.
	 * 
	 * @return The {@link SampledTrack} instance added to the given frame.
	 * 
	 */
	public static SampledTrack createFromMemory(
			byte[] audioBytes, 
			AudioFormat format,
			Frame targetFrame,
			int x, int y,
			String name,
			TrackMixSubject mixTemplate) {
		
		assert (targetFrame != null);
		assert (audioBytes != null);
		assert (format != null);
		
		Text source = new Text(targetFrame.getNextItemID());
		source.setParent(targetFrame);
		

		long runningtime = AudioMath.framesToMilliseconds(
				audioBytes.length / format.getFrameSize(), format);
		
		Timeline tl = FrameLayoutDaemon.getInstance().getTimeline(targetFrame);
		
		int width = -1;
		if (tl != null) {
			width = (int)((double)runningtime / tl.getTimePerPixel());
		}	
		
		// Clamp width to something reasonable.
		if (width > 0 && width < FrameLayoutDaemon.MIN_TRACK_WIDGET_WIDTH) width = FrameLayoutDaemon.MIN_TRACK_WIDGET_WIDTH;

		long initTime = (tl == null) ? 0 : tl.getMSTimeAtX(x);

		source.setPosition(x, y);
			
		// Setup metadata
		LinkedList<String> data = new LinkedList<String>();

		data.add(TrackWidgetCommons.META_INITIATIONTIME_TAG + initTime);
		data.add(TrackWidgetCommons.META_LAST_WIDTH_TAG + width); // although layout manager will handle, just to quick set
		if (name != null) data.add(TrackWidgetCommons.META_NAME_TAG + name);
		
		source.setData(data);
		
		SampledTrack strack = new SampledTrack(source, audioBytes, format, mixTemplate);

		return strack;
	}
	
	
	/**
	 * Creates a {@link SampledTrack} from file - which is tyo be imported into apollos
	 * 
	 * @param targetFrame
	 * 			The frame that the widget will reside.
	 * 
	 * @param file
	 * 			The audio file to import
	 * 
	 * @return The {@link SampledTrack} instance added to the given frame.
	 * 
	 */
	public static SampledTrack createFromFile(File file, Frame targetFrame, int x, int y) {
		assert (targetFrame != null);
		assert (file != null);
		
	
		String[] args = new String[] { 
				ARG_IMPORT_TAG + file.getAbsolutePath()
				};
		
		Text source = new Text(
				targetFrame.getNextItemID(),
				ItemUtils.GetTag(ItemUtils.TAG_IWIDGET) + ":" + formatArgs(args));
		
		source.setParent(targetFrame);
		
		source.setPosition(x, y);
		
		long initTime = FrameLayoutDaemon.getInstance().getMSAtX(x, targetFrame);

		// Setup metadata
		LinkedList<String> data = new LinkedList<String>();

		data.add(TrackWidgetCommons.META_INITIATIONTIME_TAG + initTime);
		data.add(TrackWidgetCommons.META_LAST_WIDTH_TAG + FrameLayoutDaemon.TRACK_WIDGET_DEFAULT_WIDTH); // once loaded then the layout will handle proper size
		
		int extIndex = file.getName().lastIndexOf('.');
		String name = (extIndex > 0) ? file.getName().substring(0, extIndex) : file.getName();
		data.add(TrackWidgetCommons.META_NAME_TAG + name);
		
		source.setData(data);

		SampledTrack strack = new SampledTrack(source, args);

		return strack;
	}

	
	@Override
	protected String[] getArgs() {
		return null;
	}

	@Override
	protected List<String> getData() {

		List<String> data = new LinkedList<String>();
		
		data.add(META_LOCALNAME_TAG + localFileName);
		
		if (shouldOmitAudioIndexing) data.add(META_DONT_INDEX_AUDIO_TAG);
		
		String lastName = getName();
		
		if (lastName != null)
			data.add(TrackWidgetCommons.META_NAME_TAG + lastName);
		
		data.add(TrackWidgetCommons.META_LAST_WIDTH_TAG + getWidth());
		
		Mutable.Long initTime = null;
		
		Frame f = getParentFrame();
		if (f != null) {
			TrackGraphNode tinf = AudioStructureModel.getInstance().getTrackGraphInfo(localFileName, f.getName());
			if (tinf != null) {
				initTime = Mutable.createMutableLong(tinf.getInitiationTime());
			}
		}
		
		if (initTime == null) initTime = getInitiationTimeFromMeta(); // old meta
		if (initTime == null) initTime = Mutable.createMutableLong(0L);

		data.add(TrackWidgetCommons.META_INITIATIONTIME_TAG + initTime);
		
		data.add(TrackWidgetCommons.META_RUNNINGMSTIME_TAG + getRunningMSTimeFromRawAudio());

		return data;
	}
	
	
	@Override
	public InteractiveWidget copy() 
		throws InteractiveWidgetNotAvailableException, InteractiveWidgetInitialisationFailedException {
		
		if (trackModel == null) {
			return super.copy();
			
		} else {

			return SampledTrack.createFromMemory(
					trackModel.getAllAudioBytesCopy(),
					trackModel.getFormat(), 
					getSource().getParentOrCurrentFrame(), 
					getX(), 
					getY(),
					TrackNameCreator.getNameCopy(trackModel.getName()),
					trackMix);
		}
		
	}

	@Override
	public int getLoadDelayTime() {
		return 0;
	}

	@Override
	protected float loadWidgetData() {
		
		// Load audio from file
		File f = null;
		boolean isImporting = false;
		
		if (recoveryFile != null) { // are we recovering?
			
			setLoadScreenMessage("Recovering audio file...");
			
			if (recoveryFile.exists()) 
				f = recoveryFile;

		}
		
		if (f == null) { // must be importing or loading from local repository

			if (loadFilenameArgument != null && loadFilenameArgument.startsWith(ARG_IMPORT_TAG)) { // importing a file?
				
				setLoadScreenMessage("Importing audio file...");
				
				isImporting = true;
				f = new File(loadFilenameArgument.substring(ARG_IMPORT_TAG.length()));
				
			} else { // local
				assert(loadFilenameArgument == null || loadFilenameArgument == localFileName);
				
				setLoadScreenMessage("Loading audio file...");
				
				f = new File(
						AudioPathManager.AUDIO_HOME_DIRECTORY + localFileName);
			}
			
			// Nullify
			if (!f.exists() || !f.isFile()) f = null;

		}

		if (f == null) {
			if (recoveryFile != null) {
				setLoadScreenMessage("Recovery file missing");
			} else {
				setLoadScreenMessage("File missing");
			}
			
			return LOAD_STATE_FAILED;
		}
		
		if (trackModel != null && trackModel.getFilepath() != null &&
				trackModel.equals(f.getPath())) {
			
			// already have loaded
			assert(!isImporting);
			
		} else {

			try {
				
				trackModel = TrackModelLoadManager.getInstance().load(f.getPath(), localFileName, this, false);
				
				if (trackModel == null) { // load operation canceled
					assert(hasCancelBeenRequested()); 
					return LOAD_STATE_INCOMPLETED;
					
				} else if(isImporting) { // ensure that file path is null - since not yet saved
					trackModel.setFilepath(null);
					
				} else if (recoveryFile != null) {
					
					// If recovering - might be recovering an existing track that had been
					// saved to the repository .. thus re-use the old file
					trackModel.setFilepath(AudioPathManager.AUDIO_HOME_DIRECTORY + localFileName);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				setLoadScreenMessage("Failed to load audio file");
				return LOAD_STATE_FAILED;
				
			} catch (UnsupportedAudioFileException e) {
				e.printStackTrace();
				setLoadScreenMessage("Format not supported");
				return LOAD_STATE_FAILED;
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
				setLoadScreenMessage("Out of memory");
				return LOAD_STATE_FAILED;
			}
			
		}
		
		// If was imported / recovered, then set as being modified
		if (isImporting || recoveryFile != null) {
			trackModel.setAudioModifiedFlag(true);
		}
		
		// Set the name for this track
		String name = getStrippedDataString(TrackWidgetCommons.META_NAME_TAG);
		if (name != null) 
			trackModel.setName(name);
		
		initObservers(); // sets default name if non set
		
		// If was recovering - get rid of temp data
		if (recoveryFile != null) {
			recoveryFile.delete();
			recoveryFile = null; // ensure that out of a recovery state
		}
		
		// Keep meta as constant as possible for best results
		updateData(TrackWidgetCommons.META_RUNNINGMSTIME_TAG, TrackWidgetCommons.META_RUNNINGMSTIME_TAG + getRunningMSTimeFromRawAudio());
		
		// Must make sure that this track is on the track graph model
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				
				public void run() {
					
					Frame parent = getParentFrame();
					String pfname = (parent != null) ?  parent.getName() : null;
					
					TrackGraphNode tinf = AudioStructureModel.getInstance().getTrackGraphInfo(localFileName, pfname);
					if (tinf == null) {

						// Determine new initation time according to position
						long initTime = (parent != null) ? 
								FrameLayoutDaemon.getInstance().getMSAtX(getX(), parent)
								: 0;

						// Keep TrackGraphModel consistant
						AudioStructureModel.getInstance().onTrackWidgetAnchored(
								localFileName, 
								pfname, 
								initTime, 
								AudioMath.framesToMilliseconds(trackModel.getFrameCount(), trackModel.getFormat()),
								getName(),
								getY());
						
					}
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		
		// Notify layout manager - not really needed but to be extra safe force the daemon
		// to be super consistant
		FrameLayoutDaemon.getInstance().forceRecheck();

		return LOAD_STATE_COMPLETED;
	}
	
	/**
	 * Used by save manager
	 */
	public boolean doesNeedSaving() {
		return (trackModel != null && (trackModel.isAudioModified() || trackModel.getFilepath() == null));
	}

	/**
	 * Used by save manager
	 */
	public String getSaveName() {
		
		if (trackModel != null && trackModel.getName() != null)
			return "Sampled Track: " + trackModel.getName();
		else return "A Sampled Track";
		
	}
	
	/**
	 * Saves audio if the audio is loaded and modified / unsaved.
	 * Blocking.
	 */
	public void saveAudio() {
		if (trackModel != null && trackModel.isAudioModified()) {
			saveWidgetData();
		}
	}

	/**
	 * Saves the audio bytes to file
	 */
	@Override
	protected void saveWidgetData() {
		
		if (trackModel == null) return; // nothing to save
		
		// If saving for the file time then get a filename
		if (trackModel.getFilepath() == null) {
			trackModel.setFilepath(AudioPathManager.AUDIO_HOME_DIRECTORY + localFileName);
			loadFilenameArgument = localFileName; // set to now local, next load will be local
		}
		
		// Save audio bytes.
		try {
			
			AudioIO.savePCMAudioToWaveFile(
					trackModel.getFilepath(), 
					trackModel.getAllAudioBytes(), // Safe: arrays are immutable
					trackModel.getFormat());
			
			// Reset modified flag
			trackModel.setAudioModifiedFlag(false);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}

		// Ensure audio bytes can be collected if this has expired
		if (isExpired()) {

			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						releaseMemory(true);
					}
				});
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
		}
		
	
	}

	@Override
	public void onDelete() {
		super.onDelete();
		// Its nice to keep everything layed out as much as possible
		FrameLayoutDaemon.getInstance().resumeLayout(this);
	}

	@Override
	protected void unloadWidgetData() {

		// Still needs to save?
		if (doesNeedSaving() || trackModel == null)
			return; // Release memory later when saved

		try {
			// Release memory - on swing thread to avoid nullified model data wil painting / editing.
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					releaseMemory(true);
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		
	}
	
	@Override
	protected void tempUnloadWidgetData() { // basically this is deleting

		// Need unloading?
		if (trackModel == null) return;
		
		// Get rid of old temporary files
		if (recoveryFile != null && recoveryFile.exists()) {
			recoveryFile.delete();
		}
		
		// Get rid of local file
		File oldLocalFile = new File(AudioPathManager.AUDIO_HOME_DIRECTORY + localFileName);
		if (oldLocalFile.isFile() && oldLocalFile.exists()) {
			try {
				oldLocalFile.delete();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		// Dump memory to a temp file
		try {
			
			// Always be unique
			String uniqueID = AudioPathManager.generateLocateFileName("wav");
			recoveryFile = File.createTempFile("APOLLO_BACKUP" + uniqueID, null);
			
			// To avoid filling up the end-users hardisk, mark file to be
			// deleted on exit (if not already deleted).
			recoveryFile.deleteOnExit();
		
		} catch (IOException e) {
			e.printStackTrace();
			return; // cannot dump ... must keep in memory.
		}

		// Dump audio to file
		try {
			AudioIO.savePCMAudioToWaveFile(
					recoveryFile.getAbsolutePath(), 
					trackModel.getAllAudioBytes(), 
					trackModel.getFormat());
		} catch (IOException e1) {
			e1.printStackTrace();
			return; // cannot dump ... must keep in memory.
		} catch (UnsupportedAudioFileException e1) {
			e1.printStackTrace();
			return; // cannot dump ... must keep in memory.
		}
		
		try {
			// Release memory - on swing thread to avoid nullified model data wil painting / editing.
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					releaseMemory(false);
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

	}

	/**
	 * To be called by the swing thread only
	 * This is nessessary to avoid runtime memory leaks!!
	 */
	private void releaseMemory(boolean onlyIfExpired) {
		
		if (!onlyIfExpired || (onlyIfExpired && isExpired())) {
			
			if (trackModel != null) {
				
				trackModel.removeObserver(fulltrackView);
				trackModel.removeObserver(this);
				trackModel = null;
				
				assert (fulltrackView.getObservedSubject() == null);
				assert (fulltrackView.getMix() == null);
			}
			
			fulltrackView.releaseBuffer();
			
			ExpandedTrackManager.getInstance().removeObserver(this);

		}
		
	}

	/**
	 * Global Track model re-use ....
	 */
	public SampledTrackModel getSharedSampledTrackModel(String localfilename) {
		if (trackModel != null &&
				trackModel.getLocalFilename().equals(localfilename)) {
			assert(localfilename.equals(localFileName));
			return trackModel;
		}
		return null;

	}
	
	/**
	 * @see #getAudioFormat()
	 * 
	 * @return
	 * 		The audio bytes for this track widget. Null if not loaded.
	 */
	public byte[] getAudioBytes() {
		return (trackModel != null) ? trackModel.getAllAudioBytes() : null;
	}
	
	/**
	 * @see #getAudioBytes()
	 * 
	 * @return
	 * 		The audio format of the audio bytes for this track widget. Null if not loaded.
	 */
	public AudioFormat getAudioFormat() {
		return (trackModel != null) ? trackModel.getFormat() : null;
	}
	
	/**
	 * <b>Warning</b> if the path happens to be a path of a recovery file
	 * then it is deleted if the widget is reloaded.
	 * 
	 * @return 
	 * 		A full filepath to load the audio from. Never null. The path	
	 * 		may lead to a out of date file or no file.
	 */
	public String getLatestSavedAudioPath() {
		
		if (recoveryFile != null && recoveryFile.exists()) {
			return recoveryFile.getAbsolutePath();
		}
			
		return AudioPathManager.AUDIO_HOME_DIRECTORY + localFileName;
		
		
	}

	public void onGraphDirty(SampledTrackGraphView graph, Rectangle dirty) {
        dirty.translate(getX(), getY() - 1);
        FrameGraphics.invalidateArea(dirty); 
        FrameGraphics.refresh(true);
	}

	public Subject getObservedSubject() {
		return null;
	}
	

	@Override
	protected void onSizeChanged() {
		super.onSizeChanged();
		// Keep meta as constant as possible for best reults
		updateData(TrackWidgetCommons.META_LAST_WIDTH_TAG, TrackWidgetCommons.META_LAST_WIDTH_TAG + getWidth());
	}

	/**
	 * Responds to model changed events by updating the GUI ...
	 */
	public void modelChanged(Subject source, SubjectChangedEvent event) {
		
		// If the expansion selection has changed - check to see if was for this
		if (source == ExpandedTrackManager.getInstance()) {
			
			// Show graph as being selected if expanded / pending to expand.
			if (trackModel != null &&
					ExpandedTrackManager.getInstance().isTrackInExpansionSelection(trackModel)) {
				fulltrackView.setBackColor(new Color(100, 100, 100), new Color(120, 120, 120));
			} else {
				fulltrackView.setBackColor(SampledTrackGraphView.DEFAULT_BACKGROUND_COLOR, SampledTrackGraphView.DEFAULT_BACKGROUND_HIGHTLIGHTS_COLOR);
			}
			
			return;
		}
		
		Frame parent = null;

		switch (event.getID()) {
		
			case ApolloSubjectChangedEvent.LOAD_STATUS_REPORT:
				// If this widget is loading - then update the load status
				if (isInLoadProgress()) {
					
					// If the load has been cancelled, then cancel the loader
					if (hasCancelBeenRequested()) {
						
						AudioFileLoader loader = (AudioFileLoader)source;
						loader.cancelLoad();
						
					} else {
						float perc = ((Float)event.getState()).floatValue();
						if (perc > 1.0f) perc = 1.0f;
						perc *= FILE_LOADING_PERCENT_RANGE; // Dont stretch load to all of bar - still will have more work to do
						updateLoadPercentage(perc);
					}
					
				}
				
				break;
				
			case ApolloSubjectChangedEvent.NAME_CHANGED:
				if (trackModel != null && !nameLabel.getText().equals(trackModel.getName())) {
					nameLabel.setText(trackModel.getName());
				}
				
				// Get graph model consistant
				parent = getParentFrame();
				String pfname = (parent != null) ?  parent.getName() : null;
				
				AudioStructureModel.getInstance().onTrackWidgetNameChanged(
						localFileName, 
						pfname, 
						getName());
						
				break;
				
			case ApolloSubjectChangedEvent.AUDIO_INSERTED:
			case ApolloSubjectChangedEvent.AUDIO_REMOVED:
				
				long newRunningTime = getRunningMSTimeFromRawAudio();
				assert(newRunningTime > 0);
				
				long oldRunningTime = getRunningMSTimeFromMeta();
				
				// Keep meta as constant as possible for best reults
				updateData(TrackWidgetCommons.META_RUNNINGMSTIME_TAG, TrackWidgetCommons.META_RUNNINGMSTIME_TAG + newRunningTime);

				if (trackModel != null) {
					
					parent = getParentFrame();
					
					// Keep TrackGraphModel consistant
					AudioStructureModel.getInstance().onTrackWidgetAudioEdited(
							localFileName,
							(parent != null) ? parent.getName() : null, 
							newRunningTime);
					
					if (trackModel.getSelectionStart() == 0 && oldRunningTime > newRunningTime) {
						
					
						Mutable.Long inittime = getInitiationTimeFromMeta();
						if (inittime == null) inittime = Mutable.createMutableLong(0);
						inittime.value += (oldRunningTime - newRunningTime);
						
						updateData(TrackWidgetCommons.META_INITIATIONTIME_TAG, 
								TrackWidgetCommons.META_INITIATIONTIME_TAG + inittime);
						
						AudioStructureModel.getInstance().onTrackWidgetRemoved(localFileName, (parent != null) ? parent.getName() : null);
						AudioStructureModel.getInstance().onTrackWidgetAnchored(localFileName, (parent != null) ? parent.getName() : null, 
								inittime.value, newRunningTime, getName(), getY());
					}
					
				}
				
				break;
				

		}

	}
	
	public void setObservedSubject(Subject parent) {
	}

	@Override
	public void paintInFreeSpace(Graphics g) {
		paintInFreeSpace(g, false);
	}
	
	public void paintInFreeSpace(Graphics g, boolean isAtFinalPass) { 

		if (isLoaded()) { 
			
			
			if (ExpandedTrackManager.getInstance().isAnyExpandedTrackVisible() &&
					!isAtFinalPass) {
				// If a expanded track is in view .. then must render lastly
				return;
			} 
		
			// Check to see if dragging over a EditableSampledTrackGraphView
			MouseEvent me = MouseEventRouter.getCurrentMouseEvent();
			if (me != null) {
			
				if (me.getComponent() != fulltrackView && 
						me.getComponent() instanceof EditableSampledTrackGraphView &&
						!((EditableSampledTrackGraphView)me.getComponent()).isPlaying()) {
					

					Point containerPoint = SwingUtilities.convertPoint(me.getComponent(), 
							new Point(0,0), Browser._theBrowser.getContentPane());
					
					Shape clipBackUp = g.getClip();
					g.setClip(null);
					
					g.setColor(Color.ORANGE);
					((Graphics2D)g).setStroke(EditableSampledTrackGraphView.GRAPH_BAR_STROKE);
					g.drawLine(
							containerPoint.x + me.getX(),
							containerPoint.y, 
							containerPoint.x + me.getX(),
							containerPoint.y + me.getComponent().getHeight());
					
					FrameGraphics.invalidateArea(new Rectangle(
							containerPoint.x + me.getX(), 
							containerPoint.y, 
							1,
							containerPoint.y + me.getComponent().getHeight() + 1));
			
					// Restore clip
					g.setClip(clipBackUp);
					
					g.setColor(SEMI_TRANSPARENT_FREESPACE_BACKCOLOR);
					g.fillRect(getX(), getY(), getWidth(), getHeight());
					
					if (isAtFinalPass) { // final pass does not draw the borders... so must manually draw them
						g.setColor(Color.BLACK);
						((Graphics2D)g).setStroke(FREESPACE_OUTLINING);
						g.drawRect(getX(), getY(), getWidth(), getHeight());
					}
		
					return;
				}
				
			}
		
			
			
		}
		
		super.paintInFreeSpace(g);
		
		if (isLoaded()) {
			
			Shape clipBackUp = g.getClip();
			Rectangle tmpClip = (clipBackUp != null) ? clipBackUp.getBounds() : 
				new Rectangle(0, 0, 
						Browser._theBrowser.getContentPane().getWidth(), 
						Browser._theBrowser.getContentPane().getHeight());
				
			g.setClip(tmpClip.intersection(getBounds()));
		
			// Draw the name
			String name = getName();
			if (name == null) name = "Unnamed";

			g.setFont(TrackWidgetCommons.FREESPACE_TRACKNAME_FONT);
			g.setColor(TrackWidgetCommons.FREESPACE_TRACKNAME_TEXT_COLOR);
			
			// Center track name
			FontMetrics fm   = g.getFontMetrics(TrackWidgetCommons.FREESPACE_TRACKNAME_FONT);
			Rectangle2D rect = fm.getStringBounds(name, g);
			
			g.drawString(
					name, 
					this.getX() + (int)((getWidth() - rect.getWidth()) / 2), 
					this.getY() + (int)((getHeight() - rect.getHeight()) / 2) + (int)rect.getHeight()
					);
			
			g.setClip(clipBackUp);
		
		}
		
	}
	
	

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		if (isLoaded() && nameLabel != null) {
			nameLabel.paint(g);
			
			if (shouldOmitIndexAudio()) {
				
				int shiftOffset = SoundDesk.getInstance().isPlaying(trackMix.getChannelID()) ?
						-20 : 0;
				
				IconRepository.getIcon("omitindexed.png").paintIcon(
						_swingComponent, 
						g, 
						getX() + getWidth() - EditableSampledTrackGraphView.LOCK_ICON_CORNER_OFFSET + shiftOffset, 
						getY() + EditableSampledTrackGraphView.LOCK_ICON_CORNER_OFFSET - 16);
				
			}
		}
		
	}
	
	private boolean ignoreInjection = false;
	private MouseEvent lastInsertME = null;
	
	@Override
	protected void onParentStateChanged(int eventType) {
		super.onParentStateChanged(eventType);

		Frame parent = null;

		switch (eventType) {

			// Logic for injecting audio tracks into EditableSampledTrackGraphView's
			case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
			case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:

				// Resume any layouts suspended by this track widget
				FrameLayoutDaemon.getInstance().resumeLayout(this);
				
				if (trackModel != null) {

					parent = getParentFrame();

					// Determine new initation time according to anchored position...
					Mutable.Long initTime = getInitiationTimeFromMeta();
					
					// If the user is restricting-y-axis movement then they might be moving 
					// this tracks Y-position only for layout reasons as opposed to repositioning
					// where in the audio timeline the track should be. This must be accurate and
					// avoid loosing the exact initiation time due to pixel-resolutoin issues
					if (parent != null) {
						
						boolean inferInitTime = true;
						
						if (AudioFrameMouseActions.isYAxisRestictionOn()) {
							Mutable.Long ms = getInitiationTimeFromMeta();
							if (ms != null) {
								Mutable.Long timex = FrameLayoutDaemon.getInstance().getXAtMS(
										ms.value, 
										parent);
								if (timex != null && timex.value == getX()) {
									initTime = ms;
									inferInitTime = false;
								}
							}
						}
						
						inferInitTime &= 
							(AudioFrameMouseActions.isMouseAnchoring() || AudioFrameMouseActions.isMouseStamping());

						if (inferInitTime) 
							initTime = Mutable.createMutableLong(FrameLayoutDaemon.getInstance().getMSAtX(getX(), parent));
					}

					updateData(TrackWidgetCommons.META_INITIATIONTIME_TAG,
							TrackWidgetCommons.META_INITIATIONTIME_TAG + initTime);

					// Keep TrackGraphModel consistant
					AudioStructureModel.getInstance().onTrackWidgetAnchored(
							localFileName, 
							(parent != null) ? parent.getName() : null, 
							initTime.value, 
							AudioMath.framesToMilliseconds(trackModel.getFrameCount(), trackModel.getFormat()),
							getName(),
							getY());
					
					
					MouseEvent me = MouseEventRouter.getCurrentMouseEvent();
					
					if (!ignoreInjection && me != null && me != lastInsertME &&
							(me.getButton() == MouseEvent.BUTTON2 ||
									me.getButton() == MouseEvent.BUTTON3)) {
						
						// Although widgets filter out multiple parent changed events per mouse event - 
						// it also considers the event type, and because this it remove itself from the frame/freespace
						// it jumbles up the filtering so that this will be invoked for each corner.
						lastInsertME = me;
					
						if (me.getComponent() != fulltrackView && 
								me.getComponent() instanceof EditableSampledTrackGraphView) {

							EditableSampledTrackGraphView editableSampledTrack = 
								(EditableSampledTrackGraphView)me.getComponent();

							// Inject this into the widget
							try {
								injectAudio(editableSampledTrack, me.getX(), true);
							} catch (IOException ex) {
								ex.printStackTrace();
							}

						}
							
					} else if (!ignoreInjection && me == lastInsertME) {
						// Note due to a injection removing this widget while in the midst of
						// anchoring, the widget parent event filtering will not work thus
						// must keep removing self until the mouse event has done.
						removeSelf();
					}
					
					// Wakeup the daemon to note that it should recheck -- in the case that a track is
					// added to a non-overdubbed frame the audio structure model will not bother
					// adding the track until something requests for it.
					FrameLayoutDaemon.getInstance().forceRecheck();
				}
				
			case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
			case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:

				
/*
				// Listen for volume or mute changed events
				trackMix.addObserver(this);
				
				// Listen for solo events and track sequence creation events
				MixDesk.getInstance().addObserver(this);
				*/
				
			break; 
			

			case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
			case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY:

				// If yanking this from the frame into free space suspend the layout daemon
				// for this frame
				if (MouseEventRouter.getCurrentMouseEvent() != null &&
						MouseEventRouter.getCurrentMouseEvent().getButton() == MouseEvent.BUTTON2 &&
						MouseEventRouter.getCurrentMouseEvent() != lastInsertME) {
					Frame suspended = DisplayIO.getCurrentFrame();
					if (suspended != null) {
						FrameLayoutDaemon.getInstance().suspendLayout(suspended, this);
					}
				}
				
				if (trackModel != null) {
					parent = getParentFrame();
					// Keep TrackGraphModel consistant
					AudioStructureModel.getInstance().onTrackWidgetRemoved(localFileName, 
							(parent != null) ? parent.getName() : null);
				}
				
			case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
				/*
				trackMix.removeObserver(this);
				MixDesk.getInstance().removeObserver(this);*/
				
				break; 
				
		}

	}

	/**
	 * Injects this widgets bytes into a EditableSampledTrackGraphView
	 * and removes this widget from freespace/parent-frame.
	 * 
	 * Doesn't inject bytes if the target EditableSampledTrackGraphView is in a playing state.
	 * 
	 * @param target
	 * 		The target EditableSampledTrackGraphView to inject this widgets bytes into
	 * 
	 * @param graphX
	 * 		The X pixel in the target's graph. Must be in valid range.
	 * 
	 * @param destroySelf
	 * 		If want to destroy this widget
	 * 
	 * @throws IOException
	 * 		If the insert failed ... can occur if bytes need to be
	 * 		converted into targets format.
	 */
	public void injectAudio(EditableSampledTrackGraphView target, int graphX, boolean destroySelf)
		throws IOException {
		assert(target != null);
		assert(graphX >= 0);
		assert(graphX <= target.getWidth());
		
		// Cannot inject into EditableSampledTrackGraphView's while playing, although
		// won't break anything, this would be confusing for the user.
		if (target.isPlaying()) return;
		
		// Inject the audio at the graph poistion
		int insertFramePoint = target.frameAtX(graphX);

		// Inject audio 
		target.insertAudio(
				trackModel.getAllAudioBytes(), 
				trackModel.getFormat(), 
				insertFramePoint); 

		// Note: if removed from free space .. then there should be no
		// more references to this item therefore the memory will be freed
		// eventually after this invoke
		if (destroySelf) removeSelf();
	}
	
	/**
	 * 
	 * @return
	 * 		The unique loval filename for this track. Auto-assigned - and rememered. Never null.
	 */
	public String getLocalFileName() {
		return localFileName;
	}

	/**
	 * Determines the running time from the raw audio in memory.
	 * 
	 * @return
	 * 		The running time or this track in MS.
	 * 		-1 if not loaded.
	 */
	public long getRunningMSTimeFromRawAudio()
	{
		if (this.trackModel != null) {
			return AudioMath.framesToMilliseconds(trackModel.getFrameCount(), trackModel.getFormat());
		}
		
		return -1;
	}
	
	/**
	 * Determines the running time from the meta data.
	 * 
	 * @return
	 * 		The running time or this track in MS.
	 * 		-1 if unavilable.
	 */
	public long getRunningMSTimeFromMeta() {
		return getStrippedDataLong(TrackWidgetCommons.META_RUNNINGMSTIME_TAG, new Long(-1));
	}
	
	/**
	 * @return
	 * 		The name given to this widget... can be null.
	 */
	public String getName() {
		if (this.trackModel != null)
			return trackModel.getName();
		else if (this.nameLabel != null) {
			return nameLabel.getText();
		} 
		return getStrippedDataString(TrackWidgetCommons.META_NAME_TAG);
	}
	
	/**
	 * Determines the initiation time from the meta data.
	 * 
	 * @return
	 * 		The initiation time or this track in MS.
	 * 		null if unavilable.
	 */
	public Mutable.Long getInitiationTimeFromMeta() {
		Long l = getStrippedDataLong(TrackWidgetCommons.META_INITIATIONTIME_TAG, null);
		if (l != null) return Mutable.createMutableLong(l.longValue());
		return null;
	}
	
	/**
	 * Adjusts the initiation time for this track - to the exact millisecond.
	 * 
	 * The x-position is updated (which will be eventually done with possibly better
	 * accuracy if the layout daemon is on)
	 * 
	 * @param specificInitTime
	 * 		The new initiation time for this track in milliseconds
	 */
	public void setInitiationTime(long specificInitTime) {
		
		Frame parent = getParentFrame();
		
		// Update x position if it can
		if (parent != null) {
			Timeline tl = FrameLayoutDaemon.getInstance().getTimeline(parent);
			if (tl != null) {
				this.setPosition(tl.getXAtMSTime(specificInitTime), getY());
			}
		}
		
		updateData(TrackWidgetCommons.META_INITIATIONTIME_TAG,
				TrackWidgetCommons.META_INITIATIONTIME_TAG + specificInitTime);
		
		AudioStructureModel.getInstance().onTrackWidgetPositionChanged(
				localFileName, (parent != null) ? parent.getName() : null, specificInitTime, getY());
	
	}
	
	/**
	 * Sets Y position - retaining the init time.
	 * @param newY
	 */
	public void setYPosition(int newY) {

		if (getY() == newY || isFloating()) return;

		Frame parent = getParentFrame();
		
		Mutable.Long initTime = getInitiationTimeFromMeta();
		if (initTime == null) return;
		
		setPosition(getX(), newY);
		
		AudioStructureModel.getInstance().onTrackWidgetPositionChanged(
				localFileName, (parent != null) ? parent.getName() : null, initTime.value, newY);

	}
	
	
	private void updateBorderColor() {
		
		// Get border color currently used
		Color oldC = getSource().getBorderColor();
		
		Color newC = TrackWidgetCommons.getBorderColor(
				SoundDesk.getInstance().isSolo(trackMix.getChannelID()), 
				trackMix.isMuted());
		
		// Update the color
		if (!newC.equals(oldC)) {
			setWidgetEdgeColor(newC);
		}
	}
	
	/**
	 * State icons live at the top right corner
	 *
	 */
	private void invalidateStateIcons() {
		this.invalidateSelf(); // TODO
	}
	
	private void setShouldOmitIndexAudio(boolean shouldOmit) {
		this.shouldOmitAudioIndexing = shouldOmit;
		if (!shouldOmit) {
			removeData(META_DONT_INDEX_AUDIO_TAG);
		} else {
			addDataIfCaseInsensitiveNotExists(META_DONT_INDEX_AUDIO_TAG);
		}
		invalidateStateIcons();
	}
	
	public boolean shouldOmitIndexAudio() {
		return shouldOmitAudioIndexing;
	}
	
//	/**
//	 * Invalidates.
//	 * @param shouldLayout
//	 */
//	private void setShouldLayout(boolean shouldLayout) {
//		
//		if (shouldLayout) {
//			removeData(TrackWidgetCommons.META_OMIT_LAYOUT_TAG);
//		} else {
//			addDataIfCaseInsensitiveNotExists(TrackWidgetCommons.META_OMIT_LAYOUT_TAG);
//		}
//		
//		invalidateStateIcons();
//	}
//	

//	
//	public boolean shouldLayout() {
//		
//		return (!containsDataTrimmedIgnoreCase(TrackWidgetCommons.META_OMIT_LAYOUT_TAG));
//	}
	
	private boolean split(boolean extractSelection) {
		
		// First is operation valid?
		if (trackModel != null && trackModel.getSelectionLength() > 1 
				&& !fulltrackView.isPlaying()
				&& (trackModel.getFrameCount() - trackModel.getSelectionLength())  > EditableSampledTrackGraphView.MIN_FRAME_SELECTION_SIZE) {

			// If so... can a slip be performed? i.e. is there unselected audio to
			// the left and right of selection
			if (trackModel.getSelectionStart() > 0 && 
					(trackModel.getSelectionStart() + trackModel.getSelectionLength()) < trackModel.getFrameCount()) {
				
				// Perform split
				int rightSideStartFrame = trackModel.getSelectionStart() + trackModel.getSelectionLength();
				
				// Create a new track widget to contain the right-side audio
				byte[] rightsideAudio = new byte[(trackModel.getFrameCount() - rightSideStartFrame) * trackModel.getFormat().getFrameSize()];
				
				// Copy bytes into new location
				System.arraycopy(
						trackModel.getAllAudioBytes(), 
						rightSideStartFrame * trackModel.getFormat().getFrameSize(), 
						rightsideAudio, 
						0, rightsideAudio.length);
				
				byte[] selectedBytes = (extractSelection) ? trackModel.getSelectedFramesCopy() : null;
				
				// Let this widget keep the left-side audio
				trackModel.setSelection(trackModel.getSelectionStart(), trackModel.getFrameCount() - trackModel.getSelectionStart());
				trackModel.removeSelectedBytes();
				trackModel.setSelection(0,0);
				
				// Build the new neighbouring widget
				Frame target = getParentFrame();
				if (target == null) target = DisplayIO.getCurrentFrame();
				
				// Determine init time
				Mutable.Long initTime = getInitiationTimeFromMeta();
				
				if (initTime == null) initTime = Mutable.createMutableLong(0);
				
				initTime.value += AudioMath.framesToMilliseconds(rightSideStartFrame, trackModel.getFormat());
				
				SampledTrack rightSideTrack = SampledTrack.createFromMemory(
						rightsideAudio, 
						trackModel.getFormat(), 
						target, 
						0, // X Coord overridden
						getY(), 
						getName() + " part", 
						trackMix);
	
				// Anchor it
				rightSideTrack.ignoreInjection = true;
				target.addAllItems(rightSideTrack.getItems());
				rightSideTrack.ignoreInjection = false;
				
				// Adjust initiation time to be exact
				rightSideTrack.setInitiationTime(initTime.value);
				
				// If extracting audio then attatch it to the cursor
				if (selectedBytes != null) {
					assert(extractSelection);
	
					SampledTrack extractedTrack = SampledTrack.createFromMemory(
							selectedBytes, 
							trackModel.getFormat(), 
							target, 
							getX(), // X Coord overridden
							getY(), 
							getName() + " part", 
							trackMix);
					
					FrameMouseActions.pickup(extractedTrack.getItems());
					
				}
			}
		
			return true;
		}
		
		return false;
		
	}
	/**
	 * Doesn't expand if not anchored ... 
	 * @param addToExpand
	 */
	private void expand(boolean addToExpand) {
		
		Frame parent = getParentFrame();
		String pfname = (parent != null) ?  parent.getName() : null;
		if (pfname == null) return;

		if (!ExpandedTrackManager.getInstance().isTrackInExpansionSelection(trackModel)) {
			
			if (addToExpand) {
				
				// Show the expanded view for this track once control has been released
				ExpandedTrackManager.getInstance().addTrackToSelection(
						trackModel,
						pfname,
						_swingComponent.getBounds(),
						trackMix);

			} else {

				// Get rid of all popups
				PopupManager.getInstance().hideAutohidePopups();
				
				int start = trackModel.getSelectionStart();
				int length = trackModel.getSelectionLength();
				
				if (length <= 1) {
					start = 0;
					length = trackModel.getFrameCount();
				}
				
				// Show the expanded view for this track
				ExpandedTrackManager.getInstance().expandSingleTrack(
						trackModel, 
						_swingComponent.getBounds(),
						trackMix,
						pfname,
						start,
						length
						);
			
			}
			
		} else {
			
			// Take away track from being selected.
			ExpandedTrackManager.getInstance().removeTrackFromSelection(trackModel);
		}
		
	}

	/**
	 * The small popup for common actions.
	 * 
	 * @author Brook Novak
	 *
	 */
	private class PlaybackPopup extends PlaybackControlPopup implements Observer {

		private static final long serialVersionUID = 1L;
		
		public PlaybackPopup() {
			miscButton.setActionCommand("expand");
			miscButton.setIcon(IconRepository.getIcon("expand.png"));
			miscButton.setToolTipText("Expand");

		}

		@Override
		public void onHide() {
			super.onHide();
			
			// Listen for volume or mute changed events
			trackMix.removeObserver(this);
			
			// Listen for solo events and track sequence creation events
			SoundDesk.getInstance().removeObserver(this);
		}

		@Override
		public void onShow() {
			super.onShow();
			// Listen for volume or mute changed events
			trackMix.addObserver(this);
			
			// Listen for solo events and track sequence creation events
			SoundDesk.getInstance().addObserver(this);
			updateVolume((int)(100 * trackMix.getVolume()));
			updateMute(trackMix.isMuted());
			updateSolo(SoundDesk.getInstance().isSolo(trackMix.getChannelID()));
		}

		public void actionPerformed(ActionEvent e) {
			if (trackModel == null) return;
			
			if (e.getSource() == playPauseButton) {

				try {
	
					if (!SoundDesk.getInstance().isPlaying(trackMix.getChannelID())) { // play / resume
						
						int startFrame = -1, endFrame = -1;
						
						// Resume playback?
						if (SoundDesk.getInstance().isPaused(trackMix.getChannelID())) {
							startFrame = SoundDesk.getInstance().getLastPlayedFramePosition(trackMix.getChannelID());
							if (startFrame >= 0 && startFrame < trackModel.getFrameCount()) {
				
								// The user may have edited the audio track and reselected it
								// since the last pause. Thus select an appropriate end frame
								endFrame = (trackModel.getSelectionLength() > 1) ?
										trackModel.getSelectionStart() + trackModel.getSelectionLength():
											trackModel.getFrameCount() - 1;
									
								// Changed selection? it play range invalid?
								if (endFrame <= startFrame || startFrame < trackModel.getSelectionStart()) {
									startFrame = -1; // Play new selection (see below)
								
								} else if (endFrame >= trackModel.getFrameCount()) {
									endFrame = trackModel.getFrameCount() - 1;
								}		
								
							}
						}
						
						// Play from beginning of selection to end of selection
						if (startFrame < 0) {
							startFrame = trackModel.getSelectionStart();
							endFrame = (trackModel.getSelectionLength() > 1) ?
									startFrame + trackModel.getSelectionLength():
										trackModel.getFrameCount() - 1;
						}
	
						// Safety clamp:
						if (endFrame >= trackModel.getFrameCount()) {
							endFrame = trackModel.getFrameCount() - 1;
						}	
						
						if (startFrame < endFrame) {
							SoundDesk.getInstance().playSampledTrackModel(
									trackModel, 
									trackMix.getChannelID(), 
									startFrame, 
									endFrame, 
									0);
						}
	
					} else { // pause
						
						TrackSequence ts = SoundDesk.getInstance().getTrackSequence(trackMix.getChannelID());

						if (ts != null &&
								ts.isPlaying()) {
							
							// Mark channel as paused.
							SoundDesk.getInstance().setPaused(trackMix.getChannelID(), true);
							
							// Stop playback for this channel
							ApolloPlaybackMixer.getInstance().stop(ts);
							
						}
						
					}
					
				} catch (LineUnavailableException e1) {
					e1.printStackTrace();
				}
				
			} else if (e.getSource() == stopButton) {
				
				TrackSequence ts = SoundDesk.getInstance().getTrackSequence(trackMix.getChannelID());

				// reset any paused mark
				SoundDesk.getInstance().setPaused(trackMix.getChannelID(), false);
				
				if (ts != null &&
						ts.isPlaying()) {
					// Stop playback
					ApolloPlaybackMixer.getInstance().stop(ts);
				}
				
			} else if (e.getSource() == rewindButton) {
				
				trackModel.setSelection(0, 0);
				SoundDesk.getInstance().setPaused(trackMix.getChannelID(), false);
				
			} else if (e.getSource() == miscButton) {
				expand(false);
			}
		}
		
		public Subject getObservedSubject() {
			return null;
		}

		public void setObservedSubject(Subject parent) {
		}

		/**
		 * Receives events from the track model OR from the observed track sequence.
		 */
		public void modelChanged(Subject source, SubjectChangedEvent event) {

			// Synch GUI with track state
			switch (event.getID()) {
			
			case ApolloSubjectChangedEvent.TRACK_SEQUENCE_CREATED: // from sound desk
				
				if (event.getState().equals(trackMix.getChannelID())) {
					// The channel being played is the same as this one ...
					// even if the track model is unloaded must enter into a playing state
					// if the created track sequence will play
					TrackSequence ts = SoundDesk.getInstance().getTrackSequence(trackMix.getChannelID());
					assert(ts != null);
					assert(!ts.hasFinished());
					assert(!ts.isPlaying());
					ts.addObserver(this);
				}

				break;
				
			case ApolloSubjectChangedEvent.PLAYBACK_STARTED: // From observed track sequence
				stopButton.setEnabled(true);
				rewindButton.setEnabled(false);
				playPauseButton.setIcon(IconRepository.getIcon("pause.png"));
				
				invalidateStateIcons();

				SampledTrack.this.setWidgetEdgeThickness(TrackWidgetCommons.PLAYING_TRACK_EDGE_THICKNESS);
				//FrameGraphics.refresh(true);
					break;
 
				case ApolloSubjectChangedEvent.PLAYBACK_STOPPED: // From observed track sequence
				
				invalidateStateIcons();
					
				rewindButton.setEnabled(true);
				stopButton.setEnabled(false);
				playPauseButton.setIcon(IconRepository.getIcon("play.png"));
				
				// Note:
				// No need to remove self from observing the dead track since the track references this
				// and will get garbage collected
				
				SampledTrack.this.setWidgetEdgeThickness(TrackWidgetCommons.STOPPED_TRACK_EDGE_THICKNESS);

				break;
				
			case ApolloSubjectChangedEvent.PAUSE_MARK_CHANGED: // When stopped or paused
				/*
				if (ae.getState().equals(trackMix.getChannelID())) {
					
					if (MixDesk.getInstance().isPaused(trackMix.getChannelID())) {
						// Do nothing .. the paused mark is set prior to a stop
					} else {
						// Esnure that the GUI represents a stopped state
						stopButton.setEnabled(false);
						playPauseButton.setIcon(IconRepository.getIcon("play.png"));
					}
					
				}*/
				
				break;
				
			case ApolloSubjectChangedEvent.VOLUME: // From obseved track mix
				updateVolume((int)(100 * trackMix.getVolume()));
				break;
				
			case ApolloSubjectChangedEvent.MUTE: // From obseved track mix
				updateMute(trackMix.isMuted());
				updateBorderColor();
				break;
				
			case ApolloSubjectChangedEvent.SOLO_PREFIX_CHANGED: // From mix desk
				updateSolo(SoundDesk.getInstance().isSolo(trackMix.getChannelID()));
				updateBorderColor();
				break;
			}
			
	
		
		}

		@Override
		protected void volumeChanged() {
			trackMix.setVolume(((float)volumeSlider.getValue()) / 100.0f);
		}

		@Override
		protected void muteChanged() {
			trackMix.setMuted(muteButton.isSelected());
		}

		@Override
		protected void soloChanged() {
			SoundDesk.getInstance().setSoloIDPrefix(soloButton.isSelected() ?
					trackMix.getChannelID() : null
					);
		}
		
	}

	@Override
	public boolean isWidgetEdgeThicknessAdjustable() {
		return false;
	}
	
	
	
}
