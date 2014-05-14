package org.expeditee.actions;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.FreeItems;
import org.expeditee.gui.MessageBay;
import org.expeditee.io.WebParser;
import org.expeditee.items.Item;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.InteractiveWidget;
import org.expeditee.items.widgets.JfxBrowser;
import org.expeditee.settings.exploratorysearch.ExploratorySearchSettings;
import org.expeditee.settings.network.NetworkSettings;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



/**
 * A list of Actions used with the JFX Browser widget
 * 
 */
public class JfxBrowserActions {
	
	/**
	 * Launches items.widgets.JfxBrowser and uses Text item as URL.
	 * @param text Text item which passes contents as URL for browser.
	 * @throws Exception
	 */
	public static void startBrowser(Item text) throws Exception {
		if (!(text instanceof Text)) {
			MessageBay.errorMessage("Must be a text item.");
			return;
		}
		if(text.getLink() != null) {
			MessageBay.errorMessage("Text item cannot have link.");
			return;
		}
			
		Text wt = new Text("@iw:org.expeditee.items.widgets.JfxBrowser");	// create new text item for browser widget
		
		if(FreeItems.textOnlyAttachedToCursor()) {			// navigates to url specified by the text item
			wt.appendText(":" + text.getText());
		} else {
			wt.appendText(":http://www.waikato.ac.nz");
		}
		
		FreeItems.getInstance().clear();					// remove url text from cursor
		
		wt.setParent(DisplayIO.getCurrentFrame());			// set parent of text source for InteractiveWidget.createWidget()
		wt.setXY(FrameMouseActions.getX(), FrameMouseActions.getY());
		
		// create widget from text item
		JfxBrowser browser = (JfxBrowser) InteractiveWidget.createWidget(wt);
			
		FrameMouseActions.pickup(browser.getItems());		// attach browser widget to mouse
	}
	
	/**
	 * Text item becomes link to new frame containing items.widgets.JfxBrowser and uses Text item as URL for browser.
	 * @param text Text item which passes contents as URL for browser and becomes link to the browser's new frame.
	 * @throws Exception
	 */
	public static void startBrowserNewFrame(Item text) throws Exception {
		if (!(text instanceof Text)) {
			MessageBay.errorMessage("Must be a text item.");
			return;
		}
		if(text.getLink() != null) {						// text item can't already have a link
			MessageBay.errorMessage("Text item already has link.");
			return;
		}
		
		// If no text with url is passed to action create a new text item with http://www.waikato.ac.nz for a default url
		if(!FreeItems.textOnlyAttachedToCursor()) {
			text = DisplayIO.getCurrentFrame().addText(FrameMouseActions.getX(), FrameMouseActions.getY(), 
					NetworkSettings.HomePage.get(), null);
			text.setParent(DisplayIO.getCurrentFrame());	// set parent of text source for InteractiveWidget.createWidget()
			FrameMouseActions.pickup(text);					// Attach new text link to cursor
		}
		
		// Create JfxBrowser widget on a new frame
		Frame frame = FrameIO.CreateNewFrame(text);			// create new frame for browser
		frame.removeAllItems(frame.getItems());
		text.setLink("" + frame.getNumber());				// link this text item to new frame
		
		// Create widget via text annotation
		Text wt = frame.addText(0, 0, "@iw: org.expeditee.items.widgets.JfxBrowser " 
				+ ("--anchorLeft 0 --anchorRight 0 --anchorTop 0 --anchorBottom 0 ")
				+ Browser._theBrowser.getContentPane().getWidth() +  " " + Browser._theBrowser.getContentPane().getHeight() 
				+ " : " + text.getText(), null);
		
		InteractiveWidget.createWidget(wt);
		
		FrameIO.SaveFrame(frame);							// save frame to disk
	}
	
	public static void parsePage(Item text) throws Exception {
		if (!(text instanceof Text) || !FreeItems.textOnlyAttachedToCursor()) {
			MessageBay.errorMessage("Must provide a text item.");
			return;
		}
		if(text.getLink() != null) {						// text item can't already have a link
			MessageBay.errorMessage("Text item already has link.");
			return;
		}
		
		// Create JfxBrowser widget on a new frame
		Frame frame = FrameIO.CreateNewFrame(text);			// create new frame for browser
		text.setLink("" + frame.getNumber());				// link this text item to new frame
		frame.addText(100, 100, "test", null);
		
		WebParser.parseURL(text.getText(), frame);
		System.out.println(text.getText());
	}
	
	public static void gotoURL(Text link, String URL) {
		Frame frame = FrameIO.CreateNewFrame(link);
		link.setAction(null);
		link.setLink("" + frame.getNumber());
		
		WebParser.parseURL(URL, frame);
		
		frame.change();
		
		FrameIO.SaveFrame(frame);
	}

	/**
	 * Creates a frame containing a JFXBrowser pointed to the specified URL, then navigates to that frame
	 * 
	 * @param link
	 *            Item that will link to the new frame
	 * @param url
	 *            URL to load in the Web Browser of the new frame
	 * @throws Exception
	 */
	public static void createFrameWithBrowser(Item link, String url) throws Exception {
		Frame frame = FrameIO.CreateNewFrame(link);
		frame.removeAllItems(frame.getItems());

		link.setLink("" + frame.getNumber());
		link.setAction(null);

		// Create widget via text annotation
		frame.addText(10, 10, "@iw: org.expeditee.items.widgets.JfxBrowser " + (int) (FrameGraphics.getMaxFrameSize().getWidth() * 0.9) + " "
				+ (int) (FrameGraphics.getMaxFrameSize().getHeight() * 0.9) + " : " + url, null);

		FrameIO.SaveFrame(frame);

		FrameUtils.DisplayFrame(link.getAbsoluteLink());
	}

	/**
	 * Uses the DuckDuckGo search API to provide short answers (e.g. definitions
	 * 
	 * @param input
	 */
	public static void askTheDuck(Text input) {

		final String query = input.getText();
		System.out.println(query);
		input.delete();


		final JSONParser parser = new JSONParser();


		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					String queryForUrl = URLEncoder.encode(query.trim(), "UTF-8");

					HttpURLConnection connection = (HttpURLConnection) (new URL("http://api.duckduckgo.com/?q=" + queryForUrl + "&format=json&no_html=1&no_redirect=1&t=expeditee").openConnection());

					Object parsedObject = parser.parse(new InputStreamReader(connection.getInputStream()));

					JSONObject jsonObject = (JSONObject) parsedObject;

					String abstractText = (String) jsonObject.get("AbstractText");
					String definition = (String) jsonObject.get("Definition");
					String answer = (String) jsonObject.get("Answer");

					String title = "";
					String mainText = "";
					String sourceUrl = "";
					String sourceName = "";

					if (!abstractText.equals("")) {

						sourceUrl = (String) jsonObject.get("AbstractURL");
						sourceName = "Via " + (String) jsonObject.get("AbstractSource");
						title = (String) jsonObject.get("Heading");
						mainText = abstractText;

					} else if (!definition.equals("")) {

						sourceUrl = (String) jsonObject.get("DefinitionURL");
						sourceName = "Via " + (String) jsonObject.get("DefinitionSource");
						title = query;
						mainText = definition;

					} else if (!answer.equals("")) {

						sourceUrl = (String) jsonObject.get("AbstractURL");
						sourceName = "Via " + (String) jsonObject.get("AbstractSource");
						title = (String) jsonObject.get("AnswerType") + " " + query;
						mainText = answer;

					} else {
						title = query;

						mainText = "No instant answer available";

						sourceUrl = (String) jsonObject.get("Redirect");
						sourceName = "View Search Results";
					}

					if (sourceUrl.equals("")) {
						sourceUrl = "http://duckduckgo.com/?q=" + queryForUrl;
					}

					String picSource = (String) jsonObject.get("Image");

					// List to hold the items that will be attached to the cursor
					ArrayList<Item> items = new ArrayList<Item>();

					Text titleItem = new Text(title);
					Text mainTextItem = new Text(mainText);
					Text sourceItem = new Text(sourceName);
					Text ddgLink = new Text("Results from DuckDuckGo");

					sourceItem.setAction("createFrameWithBrowser " + sourceUrl);
					ddgLink.setAction("createFrameWithBrowser " + "http://duckduckgo.com/?q=" + queryForUrl);

					titleItem.setFamily("sans-serif");
					titleItem.setSize(16);
					titleItem.setFontStyle("bold");
					titleItem.setWidth(400);

					mainTextItem.setFamily("sans-serif");
					mainTextItem.setSize(14);
					mainTextItem.setWidth(400);

					sourceItem.setFamily("sans-serif");
					sourceItem.setSize(12);
					sourceItem.setWidth(400);
					sourceItem.setFontStyle("italic");

					ddgLink.setFamily("sans-serif");
					ddgLink.setSize(12);
					ddgLink.setWidth(400);
					ddgLink.setFontStyle("italic");


					Picture pic = null;

					if (!picSource.equals("")) {
						pic = WebParser.getImageFromUrl(picSource, null, DisplayIO.getCurrentFrame(), 0, 0, 50, null, null,
								null, null, null, 0, 0);
						items.add(pic);
						items.add(pic.getSource());
					}

					titleItem.setPosition(FrameMouseActions.getPosition());
					mainTextItem.setPosition(titleItem.getX(), titleItem.getY() + titleItem.getBoundsHeight());
					sourceItem.setPosition(mainTextItem.getX(), mainTextItem.getY() + mainTextItem.getBoundsHeight());
					ddgLink.setPosition(sourceItem.getX() + sourceItem.getBoundsWidth(), sourceItem.getY());

					if (pic != null) {
						pic.getSource().setPosition(titleItem.getX() - 55, titleItem.getY() - 14);
					}

					items.add(titleItem);
					items.add(mainTextItem);
					items.add(sourceItem);
					items.add(ddgLink);
					FrameMouseActions.pickup(items);
				} catch (IOException e) {
					MessageBay.displayMessage("Problem loading results");
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
