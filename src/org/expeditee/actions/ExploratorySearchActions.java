package org.expeditee.actions;

import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameKeyboardActions;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FreeItems;
import org.expeditee.items.Item;
import org.expeditee.items.Justification;
import org.expeditee.items.Text;
import org.expeditee.settings.exploratorysearch.ExploratorySearchSettings;
import org.expeditee.settings.network.NetworkSettings;

/**
 * List of actions which Exploratory Search uses.
 * @author csl14
 */
public class ExploratorySearchActions {
	
	/**
	 * Adds a text item to the cursor which is linked to a new frame with the web browser active overlay and a JavaFX browser.
	 * @param caller The item that the action is called from
	 */
	public static void startBrowserSession(Item caller) { 
		try {
			String url;
			
			Text urlItem = FreeItems.getTextAttachedToCursor();
			
			// If there is a text item attached to the cursor, use it as the URL to load
			if (urlItem != null) {
				url = urlItem.getText();
				
				// Remove the item, since the link to the browser session will added to the cursor
				urlItem.delete();
			} else {
				// Otherwise use the home page specified in the settings
				url = NetworkSettings.HomePage.get();
			}
			
			Text linkToBrowserSession = DisplayIO.getCurrentFrame().addText(FrameMouseActions.getX(), FrameMouseActions.getY(), " - Web Browser Session", null);
			linkToBrowserSession.setParent(DisplayIO.getCurrentFrame());
			FrameKeyboardActions.AddDate(linkToBrowserSession);
			FrameMouseActions.pickup(linkToBrowserSession);
			
			// Create new frame
			Frame frame = FrameIO.CreateNewFrame(linkToBrowserSession);
			
			// link this text item to new frame
			linkToBrowserSession.setLink("" + frame.getNumber());
			
			// Remove everything from new frame
			frame.removeAllItems(frame.getItems());
			
			// Add web browser active overlay and @old
			Text t = (Text) frame.addText(-150, 50, "@ao: 2", null, "overlayset2");
			t.setAnchorRight(-100.0f);
			t.setAnchorTop(50.0f);
			t = (Text) frame.addText(-150, 50, "@old", null);
			t.setFamily("Roboto Condensed");
			t.setSize(15.0f);
			t.setColor(null);
			t.setWidth(116);
			t.setJustification(Justification.right);
			t.setLinkMark(false);
			t.setAnchorLeft(9.0f);
			t.setAnchorBottom(114.0f);
			
			Text wt;
			
			// Extract marigns from settings
			int lm = ExploratorySearchSettings.BrowserLeftMargin.get();
			int rm = ExploratorySearchSettings.BrowserRightMargin.get();
			int tm = ExploratorySearchSettings.BrowserTopMargin.get();
			int bm = ExploratorySearchSettings.BrowserBottomMargin.get();
			
			// Start Browser in fullscreen or default, depending on settings
			if(ExploratorySearchSettings.BrowserFullScreen.get()) {
				wt = frame.addText(ExploratorySearchSettings.BROWSER_HORZ_OFFSET + lm, ExploratorySearchSettings.BROWSER_VERT_OFFSET + tm, 
						"@iw: org.expeditee.items.widgets.JfxBrowser " 
						+ ("--anchorLeft " + (lm + ExploratorySearchSettings.BROWSER_HORZ_OFFSET) + " --anchorRight " + rm + " --anchorTop " 
						+ (ExploratorySearchSettings.BROWSER_VERT_OFFSET + tm) + " --anchorBottom " + bm + " ")
						+ (Browser._theBrowser.getContentPane().getWidth() - ExploratorySearchSettings.BROWSER_HORZ_OFFSET - lm - rm) + " " 
						+ (Browser._theBrowser.getContentPane().getHeight() - ExploratorySearchSettings.BROWSER_VERT_OFFSET - tm - bm) + " : " + url, null);
			} else {
				wt = frame.addText(ExploratorySearchSettings.BROWSER_HORZ_OFFSET + lm, ExploratorySearchSettings.BROWSER_VERT_OFFSET + tm,
						"@iw: org.expeditee.items.widgets.JfxBrowser " + 
						(ExploratorySearchSettings.BrowserDefaultWidth.get() - lm - rm) + " " + 
						(ExploratorySearchSettings.BrowserDefaultHeight.get() - tm - bm) + " : " + url, null);
			}
			
			FrameIO.SaveFrame(frame);							// save frame to disk
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds a text item to the cursor which is linked to a new frame with the mindmap active overlay.
	 */
	public static void startMindmapSession() {
		// Replace any text item on cursor with link to a new mindmap
		FreeItems.getInstance().clear();
		Text text = DisplayIO.getCurrentFrame().addText(FrameMouseActions.getX(), FrameMouseActions.getY(), " - Mindmap Session", null);
		text.setParent(DisplayIO.getCurrentFrame());
		FrameKeyboardActions.AddDate(text);
		FrameMouseActions.pickup(text);
		Frame frame = FrameIO.CreateNewFrame(text);
		text.setLink("" + frame.getNumber());
		
		// Clear new frame and add active overlay and @old
		frame.removeAllItems(frame.getItems());
		Text t = (Text) frame.addText(-150, 50, "@ao: 2", null, "overlayset3");
		t.setAnchorLeft(-150.0f);
		t.setAnchorTop(50.0f);
		t = (Text) frame.addText(-150, 50, "@old", null);
		t.setFamily("Roboto Condensed");
		t.setSize(15.0f);
		t.setColor(null);
		t.setWidth(116);
		t.setJustification(Justification.right);
		t.setLinkMark(false);
		t.setAnchorLeft(9.0f);
		t.setAnchorBottom(114.0f);
		
		FrameIO.SaveFrame(frame);
	}
	
	/**
	 * Adds a text item to the cursor which is linked to a new frame with the web browser active overlay and a JavaFX browser.
	 * @param caller The item that the action is called from
	 */
	public static void startBrowserWithOverlay(Item caller) { 
		try {
			String url;
			
			Text urlItem = FreeItems.getTextAttachedToCursor();
			
			// If there is a text item attached to the cursor, use it as the URL to load
			if (urlItem != null) {
				url = urlItem.getText();
				
				// Remove the item, since the link to the browser session will added to the cursor
				urlItem.delete();
			} else {
				// Otherwise use the home page specified in the settings
				url = NetworkSettings.HomePage.get();
			}
			
			Text linkToBrowserSession = DisplayIO.getCurrentFrame().addText(FrameMouseActions.getX(), FrameMouseActions.getY(), url, null);
			linkToBrowserSession.setParent(DisplayIO.getCurrentFrame());
			FrameMouseActions.pickup(linkToBrowserSession);
			
			// Create new frame
			Frame frame = FrameIO.CreateNewFrame(linkToBrowserSession);
			
			// link this text item to new frame
			linkToBrowserSession.setLink("" + frame.getNumber());
			
			// Remove everything from new frame
			frame.removeAllItems(frame.getItems());
			
			// Add web browser active overlay and @old
			Text t = (Text) frame.addText(-150, 50, "@ao: 2", null, "overlayset2");
			t.setAnchorLeft(-150.0f);
			t.setAnchorTop(50.0f);
			t = (Text) frame.addText(-150, 50, "@old", null);
			t.setFamily("Roboto Condensed");
			t.setSize(15.0f);
			t.setColor(null);
			t.setWidth(116);
			t.setJustification(Justification.right);
			t.setLinkMark(false);
			t.setAnchorLeft(9.0f);
			t.setAnchorBottom(114.0f);
			
			Text wt;
			
			// Extract marigns from settings
			int lm = ExploratorySearchSettings.BrowserLeftMargin.get();
			int rm = ExploratorySearchSettings.BrowserRightMargin.get();
			int tm = ExploratorySearchSettings.BrowserTopMargin.get();
			int bm = ExploratorySearchSettings.BrowserBottomMargin.get();
			
			// Start Browser in fullscreen or default, depending on settings
			if(ExploratorySearchSettings.BrowserFullScreen.get()) {
				wt = frame.addText(ExploratorySearchSettings.BROWSER_HORZ_OFFSET + lm, ExploratorySearchSettings.BROWSER_VERT_OFFSET + tm, 
						"@iw: org.expeditee.items.widgets.JfxBrowser " 
						+ ("--anchorLeft " + (lm + ExploratorySearchSettings.BROWSER_HORZ_OFFSET) + " --anchorRight " + rm + " --anchorTop " 
						+ (ExploratorySearchSettings.BROWSER_VERT_OFFSET + tm) + " --anchorBottom " + bm + " ")
						+ (Browser._theBrowser.getContentPane().getWidth() - ExploratorySearchSettings.BROWSER_HORZ_OFFSET - lm - rm) + " " 
						+ (Browser._theBrowser.getContentPane().getHeight() - ExploratorySearchSettings.BROWSER_VERT_OFFSET - tm - bm) + " : " + url, null);
			} else {
				wt = frame.addText(ExploratorySearchSettings.BROWSER_HORZ_OFFSET + lm, ExploratorySearchSettings.BROWSER_VERT_OFFSET + tm,
						"@iw: org.expeditee.items.widgets.JfxBrowser " + 
						(ExploratorySearchSettings.BrowserDefaultWidth.get() - lm - rm) + " " + 
						(ExploratorySearchSettings.BrowserDefaultHeight.get() - tm - bm) + " : " + url, null);
			}
			
			FrameIO.SaveFrame(frame);							// save frame to disk
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
