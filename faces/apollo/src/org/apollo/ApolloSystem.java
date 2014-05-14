package org.apollo;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apollo.actions.ApolloActions;
import org.apollo.agents.MelodySearch;
import org.apollo.audio.ApolloPlaybackMixer;
import org.apollo.audio.RecordManager;
import org.apollo.audio.SampledAudioManager;
import org.apollo.audio.util.MultiTrackPlaybackController;
import org.apollo.audio.util.PlaybackClock;
import org.apollo.audio.util.SoundDesk;
import org.apollo.gui.FrameLayoutDaemon;
import org.apollo.gui.FramePlaybackBarRenderer;
import org.apollo.gui.FrameRenderPasses;
import org.apollo.io.SampledAudioFileImporter;
import org.apollo.util.ApolloSystemLog;
import org.apollo.util.Mutable;
import org.apollo.widgets.FramePlayer;
import org.expeditee.actions.Actions;
import org.expeditee.gui.Browser;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameKeyboardActions;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.settings.UserSettings;
import org.expeditee.importer.FrameDNDTransferHandler;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

/**
 * Provides initialization and shutdown services for the Apollo system.
 * 
 * @author Brook Novak
 *
 */
public final class ApolloSystem {
	
	// TODO: Create actual frames
	public static final String SYSTEM_FRAMESET_NAME = "apollosystem";
	public static final String HELP_TOP_FRAMENAME = SYSTEM_FRAMESET_NAME + 2;
	
//	 TODO: How to get good results: collection (moteef) and querry
	// TODO: How to omit indexing on tracks 
	public static final String HELP_MELODYSEARCH_FRAMENAME = SYSTEM_FRAMESET_NAME + 3; 
	
	public static final String SETTINGS_NAME_TIMELINE_RMARGIN = "timelinerightmargin";
	public static final String SETTINGS_NAME_TIMELINE_LMARGIN = "timelineleftmargin";
	


	public static boolean useQualityGraphics = true;
	
	private ApolloSystem() {
	}

	private static boolean hasInitialized = false;
	
	/**
	 * Initializes Apollo mod for expeditee - prepares all subsystems.
	 */
	public static void initialize() {
		
		if (hasInitialized) return;

		ApolloSystemLog.println("Initializing...");
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				 try {

					URL url = ClassLoader.getSystemResource("org/apollo/icons/mainicon.png");
					
					if (url != null) {
						 Image img = Toolkit.getDefaultToolkit().getImage(url);
						 Browser._theBrowser.setIconImage(img);
					}
						
	
					 
				 } catch (Exception e) {
					 e.printStackTrace();
				 }
				 
				// Ensure that resources are released before the application is closed.
				Browser._theBrowser.addWindowListener(new WindowListener() {
		
					public void windowActivated(WindowEvent e) {
					}
		
					public void windowClosed(WindowEvent e) {
					}
		
					public void windowClosing(WindowEvent e) {
						ApolloSystem.shutdown();
					}
		
					public void windowDeactivated(WindowEvent e) {
					}
		
					public void windowDeiconified(WindowEvent e) {
					}
		
					public void windowIconified(WindowEvent e) {
					}
		
					public void windowOpened(WindowEvent e) {
					}
				
				});
				
				Browser._theBrowser.getContentPane().removeKeyListener(FrameKeyboardActions.getInstance());
				Browser._theBrowser.removeKeyListener(FrameKeyboardActions.getInstance());
				
				Browser._theBrowser.getContentPane().addKeyListener(new AudioFrameKeyboardActions());
				Browser._theBrowser.addKeyListener(new AudioFrameKeyboardActions());
				
				// Filter out some special mouse move cases
				AudioFrameMouseActions apolloMouseFilter = new AudioFrameMouseActions();
				Browser._theBrowser.getMouseEventRouter().removeExpediteeMouseMotionListener(
						FrameMouseActions.getInstance());
				Browser._theBrowser.getMouseEventRouter().addExpediteeMouseMotionListener(apolloMouseFilter);
				
				Browser._theBrowser.getMouseEventRouter().removeExpediteeMouseListener(
						FrameMouseActions.getInstance());
				Browser._theBrowser.getMouseEventRouter().addExpediteeMouseListener(apolloMouseFilter);
			}
		});
		
		// Set title
		 //Browser._theBrowser.setTitle("Apollo");
		
		loadSettings();
		
		ApolloSystemLog.println("  Preparing sub-systems...");
		
		SampledAudioManager.getInstance();
		
		RecordManager.getInstance();
		
		ApolloPlaybackMixer.getInstance();
		
		FrameLayoutDaemon.getInstance();
		
		FrameRenderPasses.getInstance();
		
		PlaybackClock.getInstance();
		
		FramePlaybackBarRenderer.getInstance();
		
		// Setup for importing audio
		FrameDNDTransferHandler.getInstance().addCustomFileImporter(
				new SampledAudioFileImporter());

		ApolloSystemLog.println("  Loading actions and agents...");
		
		// Add apollo actions
		Actions.LoadMethods(ApolloActions.class);
		
		Set<String> agents = new HashSet<String>();
		agents.add(MelodySearch.class.getName());
		
		Collection<String> omitted = Actions.addAgents(agents);
		
		for (String agent : omitted) {
			
			if (agent == null || agent.length() == 0) continue;
			
			String name = agent;
			
			int index = agent.lastIndexOf('.');
			if (index > 0 && agent.length() > (index - 1)) {
				name = agent.substring(index);
			}
			ApolloSystemLog.println("  WARNING: Failed to add agent \"" + name + "\"");
			
		}

		ApolloSystemLog.println("  Loading banks...");
		SoundDesk.getInstance(); // loads upon creation

		ApolloSystemLog.println("Initialized");
		
		hasInitialized = true;
	}
	
	/**
	 * TODO: This is temporary and should be integrated with expeditees settings system.. once it is desinged
	 * to support plugins...
	 * 
	 * When invoked, the apollo setting frame is loaded and parsed... setting apollo-specific settings.
	 */
	public static void loadSettings() {

		// Load apollo settings frame from the default profile
		Frame profile = FrameIO.LoadProfile(UserSettings.DEFAULT_PROFILE_NAME);
		if (profile == null) {
			try {
				profile = FrameIO.CreateNewProfile(UserSettings.DEFAULT_PROFILE_NAME);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		assert(profile != null);
		
		for (Item i : profile.getItems()) {
			if (i instanceof Text) {
				Text textItem = (Text)i;
				
				if (textItem.getText().toLowerCase().trim().startsWith(SETTINGS_NAME_TIMELINE_LMARGIN)) {
					Mutable.Integer val = stripNameValueStringInteger(textItem.getText());
					if (val != null) {
						FrameLayoutDaemon.getInstance().setTimelineMargins(
								val.value, 
								FrameLayoutDaemon.getInstance().getRightMargin());
					}
					
				} else if (textItem.getText().toLowerCase().trim().startsWith(SETTINGS_NAME_TIMELINE_RMARGIN)) {
					Mutable.Integer val = stripNameValueStringInteger(textItem.getText());
					if (val != null) {
						FrameLayoutDaemon.getInstance().setTimelineMargins(
								FrameLayoutDaemon.getInstance().getLeftMargin(), 
								val.value);
					}
				}
			}
		}

	}
	
	private static Mutable.Integer stripNameValueStringInteger(String namevalue) {
		assert (namevalue != null);
		int valueIndex = namevalue.indexOf(':') + 1;
		if (valueIndex == 0 || valueIndex >= (namevalue.length() - 1)) return null;
		
		try {
			int value = Integer.parseInt(namevalue.substring(valueIndex));
			return Mutable.createMutableInteger(value);
		} catch (NumberFormatException e) { /* Consume*/ }
		
		return null;
	}
	
	

	/**
	 * Releases all resources currently used by the SampledAudioManager.
	 */
	public static void shutdown() {
		
		ApolloSystemLog.println("Saving banks...");
		SoundDesk.getInstance().saveMasterMix();
		SoundDesk.getInstance().saveMixes();
		
		FramePlayer.saveTypedFrames();
		
		ApolloSystemLog.println("Releasing resources...");
		
		ApolloPlaybackMixer.getInstance().releaseResources();
		RecordManager.getInstance().releaseResources(); // blocking
		MultiTrackPlaybackController.getInstance().releaseResources();
		
		ApolloSystemLog.println("Audio subsystems shutdown");
	}

	/**
	 * @return
	 * 		True if has initialized.
	 */
	public static boolean isInitialized() {
		return hasInitialized;
	}
	


	/**
	 * The apollo main just ensures that apollo is initialized during startup.
	 * 
	 * This may eventually become redundant once Expeditee implements a plugin system
	 * that allows a plugin / mod to initialize itself at started...
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
	        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
	    } 
	    catch (Exception e) {
	       e.printStackTrace();
	       return;
	    }
		
		// Run expeditee
		Browser.main(args);
		
		// Initialize apollo
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ApolloSystem.initialize();
			}
		});
	}
	
}
