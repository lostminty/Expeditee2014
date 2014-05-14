package org.expeditee.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.net.Authenticator;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.expeditee.AbsoluteLayout;
import org.expeditee.actions.Actions;
import org.expeditee.actions.Simple;
import org.expeditee.agents.mail.MailSession;
import org.expeditee.importer.FrameDNDTransferHandler;
import org.expeditee.io.ExpClipReader;
import org.expeditee.io.ItemSelection;
import org.expeditee.io.ProxyAuth;
import org.expeditee.io.ItemSelection.ExpDataHandler;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.WidgetCacheManager;
import org.expeditee.network.FrameShare;
import org.expeditee.settings.Settings;
import org.expeditee.settings.UserSettings;
import org.expeditee.stats.Logger;
import org.expeditee.stats.StatsLogger;
import org.expeditee.taskmanagement.EntitySaveManager;
import org.expeditee.taskmanagement.SaveStateChangedEvent;
import org.expeditee.taskmanagement.SaveStateChangedEventListener;

/**
 * The Main GUI class, comprises what people will see on the screen.<br>
 * Note: Each Object (Item) is responsible for drawing itself on the screen.<br>
 * Note2: The Frame is registered as a MouseListener and KeyListener, and
 * processes any Events.
 * 
 * @author jdm18
 * 
 */
public class Browser extends JFrame implements ComponentListener,
		WindowListener, WindowStateListener, SaveStateChangedEventListener {

	/**
	 * Default version - just to stop eclipse from complaining about it.
	 */
	private static final long serialVersionUID = 1L;

	// private static final JScrollPane scrollPane = new JScrollPane();

	public static Browser _theBrowser = null;
	
	public static ProxyAuth proxyAuth = new ProxyAuth();

	public static boolean _hasExited = false;
	
	private MouseEventRouter _mouseEventRouter;

	// A flag which is set once the application is exiting.
	private boolean _isExiting = false;

	private boolean _minimum_version6 = false;

	public boolean isMinimumVersion6() {
		return _minimum_version6;
	}

	private static boolean _initComplete = false;
	
	private static String _startFrame = null;

	/**
	 * Constructs a new Browser object, then launches it
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length > 0) {
			_startFrame = args[0];
			if(! Character.isDigit(_startFrame.charAt(_startFrame.length() - 1)))
				_startFrame = _startFrame + "1";
		}



		// Prepare all expeditee and swing data on the AWT event thread.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// MessageBay.supressMessages(true);

				// MessageBay.supressMessages(false);

				_theBrowser = new Browser();
				
				DisplayIO.refreshCursor();
				_theBrowser.requestFocus();
				FrameMouseActions.MouseX = MouseInfo.getPointerInfo()
						.getLocation().x
						- _theBrowser.getOrigin().x;
				FrameMouseActions.MouseY = MouseInfo.getPointerInfo()
						.getLocation().y
						- _theBrowser.getOrigin().y;
				_initComplete = true;
				
				Authenticator.setDefault(proxyAuth);
			}
		});

	}

	public Point getOrigin() {
		return getContentPane().getLocationOnScreen();
	}

	/**
	 * @return The mouse event router used for this browser. Never null after
	 *         browser constructed.
	 */
	public MouseEventRouter getMouseEventRouter() {
		return _mouseEventRouter;
	}

	/**
	 * @return
	 * 
	 * True if the application is about to exit. False if not. Not that this is
	 * only set once the window is in its closed state (not closing) or if the
	 * application has explicity being requested to exit.
	 * 
	 * @see Browser#exit()
	 * 
	 */
	public boolean isExisting() {
		return _isExiting;
	}

	public static boolean isInitComplete() {
		return _initComplete;
	}

	public void setSizes(Dimension size) {
		setSize(size);
		setPreferredSize(size);
		Dimension paneSize = getContentPane().getSize();
		FrameGraphics.setMaxSize(paneSize);
	}

	private Browser() {
		// center the frame on the screen
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		double xpos = screen.getWidth() / 2;
		double ypos = screen.getHeight() / 2;
		setLocation((int) (xpos - (UserSettings.InitialWidth.get() / 2)),
				(int) (ypos - (UserSettings.InitialHeight.get() / 2)));

		addWindowListener(this);
		addWindowStateListener(this);

		DisplayIO.addDisplayIOObserver(WidgetCacheManager.getInstance());
		DisplayIO.addDisplayIOObserver(PopupManager.getInstance());

		
		// set up the image used for the icon
	    try
        {
          URL iconURL = ClassLoader.getSystemResource("org/expeditee/assets/icons/expediteeicon128.png");
          if (iconURL != null)
          {
            Image localImage = Toolkit.getDefaultToolkit().getImage(iconURL);
            this.setIconImage(localImage);
          }
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
	    
	    setSizes(new Dimension(UserSettings.InitialWidth.get(), UserSettings.InitialHeight.get()));
	    
	    // set the layout to absolute layout for widgets
		this.getContentPane().setLayout(new AbsoluteLayout());

		_mouseEventRouter = new MouseEventRouter(getJMenuBar(),
				getContentPane());

		// enable the glasspane-for capturing all mouse events
		this.setGlassPane(_mouseEventRouter);

		this.getGlassPane().setVisible(true);
		this.getContentPane().setBackground(Color.white);
		this.getContentPane().setFocusTraversalKeysEnabled(false);

		addComponentListener(this);
		pack();

		// Reset windows to user specified size
		// Must be done after initialising the content pane above!
		setSizes(new Dimension(UserSettings.InitialWidth.get(), UserSettings.InitialHeight.get()));
	    
		// UserSettings.ProfileName.set(FrameIO.ConvertToValidFramesetName(System.getProperty("user.name")));
		String userName = UserSettings.ProfileName.get();
		//UserSettings.UserName.set(UserSettings.ProfileName.get());
		
		// Load documentation and start pages
		FrameUtils.extractResources(false);
		// Load fonts before loading any frames so the items on the frames will be able to access their fonts
		Text.InitFonts();

		Frame profile = loadProfile(userName);

		// Need to display errors once things have been init otherwise
		// exceptions occur if there are more than four messages neededing to be
		// displayed.

		Frame defaultProfile = loadProfile(UserSettings.DEFAULT_PROFILE_NAME);

		Collection<String> warningMessages = new LinkedList<String>();
		warningMessages.addAll(FrameUtils.ParseProfile(defaultProfile));
		//Save the cursor if the defaultProfile had a custom cursor
		Collection<Item> cursor = null;
		if(FreeItems.hasCursor()){
			cursor = new ArrayList<Item>();
			cursor.addAll(FreeItems.getCursor());
		}
		warningMessages.addAll(FrameUtils.ParseProfile(profile));
		if(cursor != null && !FreeItems.hasCursor()){
			FreeItems.setCursor(cursor);
		}

		/*
		 * See Java bug ID 4016934. They say that window closed events are
		 * called once the JFrame is disposed.
		 */
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Expeditee handles its own repainting of AWT/Swing components
		RepaintManager.setCurrentManager(ExpediteeRepaintManager.getInstance());

		// Listen for save status to display during and after runtime
		EntitySaveManager.getInstance().addSaveStateChangedEventListener(this);

		String full_version = System.getProperty("java.version");
		String[] version_parts = full_version.split("\\.");
		if (version_parts.length>=2) {
			String version_str = version_parts[0] + "." + version_parts[1];
			double version = Double.parseDouble(version_str);
			
			if (version >= 1.6) {
				// Set the drag and drop handler
				_minimum_version6 = true;
				setTransferHandler(FrameDNDTransferHandler.getInstance());
			} else {
				System.err.println("Upgrade to a (minimum) of Java 1.6 to enable drag and drop support in Expeditee");
			}
		}
		else {
			System.err.println("Unable to parse Java version number " + full_version + " to determin if Drag and Drop supported");
			
		}
		

		try {
			warningMessages.addAll(Actions.Init());
			
			Settings.Init();

			DisplayIO.Init(this);
			// Set visible must be just after DisplayIO.Init for the message box
			// to
			// be the right size
			setVisible(true);

			setupGraphics();

			// required to accept TAB key
			setFocusTraversalKeysEnabled(false);

			// Must be loaded after setupGraphics if images are on the frame
			// Turn off XRay mode and load the first frame
			FrameGraphics.setMode(FrameGraphics.MODE_NORMAL, false);
			
			// Go to the start frame if specified, otherwise go to the profile frame
			Frame start = null;
			if(_startFrame == null) {
				_startFrame = UserSettings.StartFrame.get();
				if(_startFrame != null && !Character.isDigit(_startFrame.charAt(_startFrame.length() - 1)))
					_startFrame = _startFrame + "1";
			}
    		if((start = FrameIO.LoadFrame(_startFrame)) != null) {
    			// Make sure HomeFrame gets set
    			if (UserSettings.HomeFrame.get() == null)
    				UserSettings.HomeFrame.set(profile.getName());
       			// Make sure the user can get back to the profile frame easily
       			DisplayIO.addToBack(profile);
       			// Go to the start frame
       			DisplayIO.setCurrentFrame(start, true);
    		} else {
    			// If an invalid start frame was specified, show a warning
    			if(_startFrame != null) {
    				warningMessages.add("Unknown frame: " + _startFrame);
    			}
    			// Go to the profile frame
    			FrameUtils.loadFirstFrame(profile);
    		}
			DisplayIO.UpdateTitle();

			/*
			 * I think this can be moved back up to the top of the Go method
			 * now... It used to crash the program trying to print error
			 * messages up the top
			 */
			for (String message : warningMessages)
				MessageBay.warningMessage(message);

			this.getContentPane().addKeyListener(FrameKeyboardActions.getInstance());
			this.addKeyListener(FrameKeyboardActions.getInstance());

			_mouseEventRouter.addExpediteeMouseListener(FrameMouseActions.getInstance());
			_mouseEventRouter.addExpediteeMouseMotionListener(FrameMouseActions.getInstance());
			_mouseEventRouter.addExpediteeMouseWheelListener(FrameMouseActions.getInstance());

			// Dont refresh for the profile frame otherwise error messages are shown twice
			if (!DisplayIO.getCurrentFrame().equals(profile)) {
				FrameKeyboardActions.Refresh();
			// If it's the profile frame just reparse it in order to display images/circles/widgets correctly
			} else {
				FrameUtils.Parse(profile);
			}
			// setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.Log(e);
		}
	}

	/**
	 * @param userName
	 * @return
	 */
	private Frame loadProfile(String userName) {
		Frame profile = FrameIO.LoadProfile(userName);
		if (profile == null) {
			try {
				profile = FrameIO.CreateNewProfile(userName);
			} catch (Exception e) {
				// TODO tell the user that there was a problem creating the
				// profile frame and close nicely
				e.printStackTrace();
				assert (false);
			}
		}
		return profile;
	}

	public Graphics2D g;

	private void setupGraphics() {
		if (g != null)
			g.dispose();
		g = (Graphics2D) this.getContentPane().getGraphics();
		assert (g != null);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(g.getFont().deriveFont(40f));
		FrameGraphics.setDisplayGraphics(g);
	}

	// private int count = 0;
	@Override
	public void paint(Graphics g) {
		// All this does is make sure the screen is repainted when the browser
		// is moved so that some of the window is off the edge of the display
		// then moved back into view
		super.paint(g);
		FrameGraphics.ForceRepaint();
		// System.out.println("Paint " + count++);
	}

	/**
	 * @inheritDoc
	 */
	public void componentResized(ComponentEvent e) {
		setSizes(this.getSize());
		setupGraphics();
		FrameIO.RefreshCasheImages();
		FrameGraphics.ForceRepaint();
	}

	/**
	 * @inheritDoc
	 */
	public void componentMoved(ComponentEvent e) {
		// FrameGraphics.setMaxSize(this.getSize());
	}

	/**
	 * @inheritDoc
	 */
	public void componentShown(ComponentEvent e) {
	}

	/**
	 * @inheritDoc
	 */
	public void componentHidden(ComponentEvent e) {
	}

	public void windowClosing(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
		exit();
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowStateChanged(WindowEvent e) {
	}

	public int getDrawingAreaX() {
		// return scrollPane.getLocationOnScreen().x;
		return this.getLocationOnScreen().x;
	}

	public int getDrawingAreaY() {
		// return scrollPane.getLocationOnScreen().y;
		return this.getLocationOnScreen().y;
	}

	public void saveCompleted(SaveStateChangedEvent event) {
		// if (isExisting()) {

		// } else {
		MessageBay.displayMessage("Save finished!", Color.BLUE);
		// }
	}

	public void saveStarted(SaveStateChangedEvent event) {
		// if (isExisting()) {

		// } else {
		String name = event.getEntity().getSaveName();
		if (name == null)
			name = "data";
		MessageBay.displayMessage("Saving " + name + "...", Color.BLUE);
		// }
	}

	/**
	 * Closes the browser and ends the application. Performs saving operations -
	 * halting until saves have completed. Feedback is given to the user while
	 * the application is exiting. Must call on the swing thread.
	 */
	public void exit() {

		// Set exiting flag
		_isExiting = true;

		MessageBay.displayMessage("System exiting...");

		/**
		 * TODO: Prompt the user etc.
		 */

		// TODO: Should we should a popup with a progress bar for user feedback?
		// this would be nice and easy to do.
		// Exit on a dedicated thread so that feedback can be obtained
		new Exiter().start(); // this will exit the application
	}

	/**
	 * The system must exit on a different thread other than the swing thread so
	 * that the save threads can fire save-feedback to the swing thread and thus
	 * provide user feedback on asynchronous save operations.
	 * 
	 * @author Brook Novak
	 * 
	 */
	private class Exiter extends Thread {

		@Override
		public void run() {

			// The final save point for saveable entities
			EntitySaveManager.getInstance().saveAll();
			try {
				EntitySaveManager.getInstance().waitUntilAllSavingFinished();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// The final phase must save on the swing thread since dealing with
			// the expeditee data model
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {

					// Stop any agents or simple programs
					Simple.stop();
					Actions.stopAgent();
					// Wait for them to stop
					try {
						MessageBay
								.displayMessage("Stopping Simple programs..."); // TODO:
						/**
						 * Only stop if need to...
						 */
						while (Simple.isProgramRunning()) {
							Thread.sleep(100);
							/* Brook: What purpose does this serve? */
						}
						MessageBay.displayMessage("Stopping Agents...");
						/* TODO: Only stop if need to... */
						while (Actions.isAgentRunning()) {
							Thread.sleep(100); // Brook: What purpose does this
							// serve?
						}
					} catch (Exception e) {

					}

					MessageBay.displayMessage("Saving current frame...");
					FrameIO.SaveFrame(DisplayIO.getCurrentFrame());

					MessageBay.displayMessage("Saving stats...");
					StatsLogger.WriteStatsFile();

					if (MailSession.getInstance() != null) {
						if (MailSession.getInstance().finalise()) {
							// TODO display this message before the finalising
							// is done but only if the mail needs closing
							MessageBay.displayMessage("Closed ExpMail...");
						}
					}

					if (FrameShare.getInstance() != null) {
						MessageBay.displayMessage("Stopping FrameServer...");
						FrameShare.getInstance().finalise();
					}

					MessageBay.displayMessage("System exited");

					// Finally remove the messages frameset
					FrameIO.moveFrameset("messages", FrameIO.MESSAGES_PATH);

					/*
					 * Create a new messages folder so that it doesn't throw
					 * Exceptions when two Expeditee's open at once and the
					 * second tries to save its messages
					 */
					File file = new File(FrameIO.MESSAGES_PATH + "messages");
					file.mkdirs();

					Browser._hasExited = true;
					
					System.exit(0);
				}
			});
		}
	}

	/**
	 * Used to set up the the browser for use in testing.
	 * 
	 * @return
	 */
	public static Browser initializeForTesting() {
		if (Browser._theBrowser == null) {
			FrameShare.disableNetworking = true;
			MailSession._autoConnect = false;

			Browser.main(null);
			try {
				while (!isInitComplete()) {
					Thread.sleep(10);
				}
			} catch (Exception e) {
			}
		}
		return _theBrowser;
	}

}
