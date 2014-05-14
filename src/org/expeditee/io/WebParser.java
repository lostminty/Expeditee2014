package org.expeditee.io;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
/*
 * JavaFX is not on the default java classpath until Java 8 (but is still included with Java 7), so your IDE will probably complain that the imports below can't be resolved. 
 * In Eclipse hitting'Proceed' when told 'Errors exist in project' should allow you to run Expeditee without any issues (although the JFX Browser widget will not display),
 * or you can just exclude JfxBrowser, WebParser and JfxbrowserActions from the build path.
 *
 * If you are using Ant to build/run, 'ant build' will try to build with JavaFX jar added to the classpath. 
 * If this fails, 'ant build-nojfx' will build with the JfxBrowser, WebParser and JfxbrowserActions excluded from the build path.
 */
import java.util.Arrays;
import java.util.Date;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.imageio.ImageIO;

import netscape.javascript.JSObject;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FrameUtils;
import org.expeditee.gui.MessageBay;
import org.expeditee.gui.MessageBay.Progress;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Justification;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.JfxBrowser;
import org.w3c.dom.Node;

/**
 * Methods to convert webpages to Expeditee frames
 * 
 * @author ngw8
 * @author jts21
 */
public class WebParser {

	
	/**
	 * Loads a webpage and renders it as Expeditee frame(s)
	 * 
	 * @param URL
	 *            Page to load
	 * @param frame
	 *            The Expeditee frame to output the converted page to
	 */
	public static void parseURL(final String URL, final Frame frame) {
		try {
			Platform.runLater(new Runnable() {
				@Override
                public void run() {
                    try {
						WebEngine webEngine = new WebEngine(URL);
	                    loadPage(webEngine, frame);
                    } catch (Exception e) {
	                    e.printStackTrace();
                    }
                }
			});
        } catch (Exception e) {
	        e.printStackTrace();
        }
	}
	
	protected static void loadPage(final WebEngine webEngine, final Frame frame) throws Exception {
		webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {

			@Override
			public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {

		        switch (newState) {
				case READY: // READY
					// MessageBay.displayMessage("WebEngine ready");
					break;
				case SCHEDULED: // SCHEDULED
					// MessageBay.displayMessage("Scheduled page load");
					break;
				case RUNNING: // RUNNING
					System.out.println("Loading page!");
					// MessageBay.displayMessage("WebEngine running");
					break;
				case SUCCEEDED: // SUCCEEDED
					// MessageBay.displayMessage("Finished loading page");
					System.out.println("Parsing page!");
					webEngine.executeScript("window.resizeTo(800, 800);"
							+ "document.body.style.width = '1000px'");
					parsePage(webEngine, frame);
					System.out.println("Parsed page!");
					break;
				case CANCELLED: // CANCELLED
					MessageBay.displayMessage("Cancelled loading page");
					break;
				case FAILED: // FAILED
					MessageBay.displayMessage("Failed to load page");
					break;
		        }	
			}
		});
	}
	
	/**
	 * Converts a loaded page to Expeditee frame(s)
	 * 
	 * @param webEngine
	 *            The JavaFX WebEngine in which the page to be converted is loaded
	 * @param frame
	 *            The Expeditee frame to output the converted page to
	 */
	public static void parsePage(final WebEngine webEngine, final Frame frame) {
		try {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					try {
						Progress progressBar = MessageBay.displayProgress("Converting web page");

						Node doc = (Node) webEngine.executeScript("document.body");

						JSObject window = (JSObject) webEngine.executeScript("window");

						frame.setBackgroundColor(rgbStringToColor((String) ((JSObject) (window.call("getComputedStyle", new Object[] { doc }))).call("getPropertyValue",
								new Object[] { "background-color" })));
						
						// Functions to be used later in JavaScript
						webEngine.executeScript(""
										+ "function addToSpan(text) {"
										+ "		span = document.createElement('wordSpan');"
										+ "		span.textContent = text;"
										+ "		par.insertBefore(span, refNode);" 
												// Checking if the current word is on a new line (i.e. lower than the previous word)
										+ "		if (prevSpan !== null && span.getBoundingClientRect().top > prevSpan.getBoundingClientRect().top) {"
													// If it is, prepend a new line character to it. The new line characters doesn't affect the rendered HTML
										+ "			span.textContent = '\\n' + span.textContent;"

													// Checking if the previous word is horizontally aligned with the one before it.
													// If it is, merge the text of the two spans
										+ "			if ( prevPrevSpan !== null && prevPrevSpan.getBoundingClientRect().left == prevSpan.getBoundingClientRect().left) {"
										+ "				prevPrevSpan.textContent = prevPrevSpan.textContent + prevSpan.textContent;" 
										+ "				par.removeChild(prevSpan);" 
										+ "			} else {"
										+ "				prevPrevSpan = prevSpan;" 
										+ "			}" 
										+ "			prevSpan = span;"
										+ "		} else if ( prevSpan !== null) {"
													// Word is on the same line as the previous one, so merge the second into the span of the first
										+ "			prevSpan.textContent = prevSpan.textContent + span.textContent;" 
										+ "			par.removeChild(span);" 
										+ "		} else {" 
										+ "			prevSpan = span;" 
										+ "		}"
										+ "}"

										+ "function splitIntoWords(toSplit) {"
										+ "		var words = [];"
										+ "		var pattern = /\\s+/g;"
										+ "		var words = toSplit.split(pattern);"
										+ ""
										+ "		for (var i = 0; i < words.length - 1; i++) {"
										+ "			words[i] = words[i] + ' ';"
										+ "		}"
										+ "		return words;"
										+ "}"
						);
					
						// Using Javascript to get an array of all the text nodes in the document so they can be wrapped in spans. Have to
						// loop through twice (once to build the array and once actually going through the array, otherwise when the
						// textnode is removed from the document items end up being skipped)
						JSObject textNodes = (JSObject) webEngine.executeScript(""
							+ "function getTextNodes(rootNode){" 
								+ "var node;" 
								+ "var textNodes=[];"
								+ "var walk = document.createTreeWalker(rootNode, NodeFilter.SHOW_TEXT);" 
								+ "while(node=walk.nextNode()) {" 
									+ "if((node.textContent.trim().length > 0)) { "
										+ "textNodes.push(node);" 
									+ "}" 
								+ "}" 
								+ "return textNodes;" 
							+ "}; " 
							+ "getTextNodes(document.body)"
							);
						
						int nodesLength = (Integer) textNodes.getMember("length");

						// Looping through all the text nodes in the document
						for (int j = 0; j < nodesLength; j++) {
							Node currentNode = (Node) textNodes.getSlot(j);

							// Making the current node accessible in JavaScript
							window.setMember("currentNode", currentNode);
						
							webEngine.executeScript(""
									+ "var span = null, prevSpan = null, prevPrevSpan = null;"
									
									// Removing repeated whitespace from the text node's content then splitting it into individual words
									+ "var textContent  = currentNode.textContent.replace(/\\n|\\r/g, '').replace(/\\s+/g, ' ');"
									+ "var words = splitIntoWords(textContent);"
									
									+ "var refNode = currentNode.nextSibling;"
									+ "var par = currentNode.parentElement;"
									+ "currentNode.parentElement.removeChild(currentNode);"
									
									+ "for (var i = 0; i < words.length; i++) {" 
									+ "		addToSpan(words[i]);"
									+ "}"
									
									+ "if (prevPrevSpan !== null && prevPrevSpan.getBoundingClientRect().left == prevSpan.getBoundingClientRect().left) {"
									+ "		prevPrevSpan.textContent = prevPrevSpan.textContent + prevSpan.textContent;"
									+ "		par.removeChild(prevSpan);"
									+ "}"
									);

							// Will never reach 100% here, as the processing is not quite finished - progress is set to 100% at the end of
							// the addPageToFrame loop below
							progressBar.set((100 * (j)) / nodesLength);
						}

						// Finding all links within the page, then setting the href attribute of all their descendants to be the same
						// link/URL.
						// This is needed because there is no apparent and efficient way to check if an element is a child of a link when
						// running through the document when added each element to Expeditee
						webEngine.executeScript(""
								+ "var anchors = document.getElementsByTagName('a');"
								+ ""
								+ "for (var i = 0; i < anchors.length; i++) {"
									+ "var currentAnchor = anchors.item(i);"
									+ "var anchorDescendants = currentAnchor.querySelectorAll('*');"
									+ "for (var j = 0; j < anchorDescendants.length; j++) {"
										+ "anchorDescendants.item(j).href = currentAnchor.href;"
									+ "}"
								+ "}"
								);

						WebParser.addPageToFrame(doc, window, webEngine, frame);

						progressBar.set(100);

					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("Parsed frame");
					FrameUtils.Parse(frame);
					frame.setChanged(true);
					FrameIO.SaveFrame(frame);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	/**
	 * Converts a loaded page to Expeditee frame(s)
	 * 
	 * @param webEngine
	 *            The JavaFX WebEngine in which the page to be converted is loaded
	 * @param frame
	 *            The Expeditee frame to output the converted page to
	 */
	public static void parsePageSimple(final JfxBrowser browserWidget, final WebEngine webEngine, final WebView webView, final Frame frame) {
		try {
			
			final int verticalScrollPerPage = (int) (FrameGraphics.getMaxFrameSize().getHeight() * 0.85);
			final int horizontalScrollPerPage = (int) (FrameGraphics.getMaxFrameSize().getWidth() * 0.85);
						
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					browserWidget.setOverlayVisible(true);
					
					// Webview area is set to slightly larger than the size of a converted page, to give some overlap between each page
					browserWidget.setWebViewSize(horizontalScrollPerPage * 1.1, verticalScrollPerPage * 1.1);
					browserWidget.setScrollbarsVisible(false);
				}
			});

			final Object notifier = new Object();
						
			final MutableInt verticalCount = new MutableInt(0);
			final MutableInt horizontalCount = new MutableInt(0);
			
			final MutableInt pagesVertical = new MutableInt(1);
			final MutableInt pagesHorizontal = new MutableInt(1);

			final String pageTitle;
			
			if (webEngine.getTitle() != null) {
				pageTitle = webEngine.getTitle();
			} else {
				pageTitle = "Untitled Page";
			}

			final Progress progressBar = MessageBay.displayProgress("Converting web page");

			final Frame frameset = FrameIO.CreateNewFrameset(FrameIO.ConvertToValidFramesetName((new SimpleDateFormat("yy-MM-dd-HH-mm-ss").format(new Date())) + pageTitle));

			frameset.setTitle(pageTitle);
			frameset.getTitleItem().setSize(14);

			WebParser.addButton("Return to original frame", frame.getName(), null, 200, frameset, null, 0f, 10f, null);

			Text link = DisplayIO.getCurrentFrame().addText(FrameMouseActions.getX(), FrameMouseActions.getY(), pageTitle, null);
			link.setLink(frameset.getName());

			FrameMouseActions.pickup(link);

			// Timer that fires every time JFX is redrawn. After a few redraws, the handle method of this takes a screenshot of the page,
			// adds it to the frame, then adds the text on top
			AnimationTimer timer = new AnimationTimer() {

				int frameCount = 0;

				Frame frameToAddTo = frameset;
				int thumbWidth = 100;

				@Override
				public void handle(long arg0) {
					// Must wait 2 frames before taking a snapshot of the webview, otherwise JavaFX won't have redrawn
					if (frameCount++ > 1) {
						frameCount = 0;
						this.stop();
						
						verticalCount.setValue(verticalCount.getValue() + 1);

						frameToAddTo = FrameIO.CreateFrame(frameToAddTo.getFramesetName(), pageTitle, null);
						frameToAddTo.removeAllItems(frameToAddTo.getItems());

						try {
							// removing the CSS that hides the text (otherwise the text would not pass the visibility check that is run on
							// it before adding it to the frame)
							webEngine.executeScript("cssHide.innerHTML = '';");

							JSObject window = (JSObject) webEngine.executeScript("window");

							int visibleWidth = (Integer) webEngine.executeScript("window.innerWidth");
							int visibleHeight = (Integer) webEngine.executeScript("window.innerHeight");

							WebParser.addTextToFrame(visibleWidth, visibleHeight, window, webEngine, frameToAddTo);

							FrameIO.SaveFrame(frameToAddTo);
						} catch (Exception ex) {
							ex.printStackTrace();
						}

						webEngine.executeScript(""
								// Setting all text to be hidden before taking the screenshot
								+ "cssHide.appendChild(document.createTextNode(wordSpanHiddenStyle));");
						
						WritableImage img = new WritableImage((int)webView.getWidth(), (int)webView.getHeight());

						webView.snapshot(new SnapshotParameters(), img);

						// Getting a BufferedImage from the JavaFX image
						BufferedImage image = SwingFXUtils.fromFXImage(img, null);

						try {
							int hashcode = Arrays.hashCode(image.getData().getPixels(0, 0, image.getWidth(), image.getHeight(), (int[]) null));

							File out = new File(FrameIO.IMAGES_PATH + "webpage-" + Integer.toHexString(hashcode) + ".png");
							out.mkdirs();
							ImageIO.write(image, "png", out);

							// Adding the image to the frame
							frameToAddTo.addText(0, 0, "@i: " + out.getName(), null);
							
							// Adding thumbnail to the overview page
							Text thumb = frameset.addText((int) (thumbWidth * 1.1 * horizontalCount.getValue()) + 10,
									(int) ((((float) thumbWidth / image.getWidth()) * image.getHeight()) * 1.1 * verticalCount.getValue()),
									"@i: " + out.getName() + " " + thumbWidth, 
									null);
							
							thumb.setLink(frameToAddTo.getName());
							thumb.setBorderColor(Color.lightGray);
							thumb.setThickness(1);

							// Button to go to the next frame/page
							WebParser.addButton("Next", null, "next", 70, frameToAddTo, null, 0f, 10f, null);

							// Button to go to the previous frame/page
							if (verticalCount.getValue() > 1 || horizontalCount.getValue() > 0) {
								WebParser.addButton("Previous", null, "previous", 70, frameToAddTo, null, 85f, 10f, null);
							}

							// Button to return to the index/overview page
							WebParser.addButton("Index", frameset.getName(), null, 70, frameToAddTo, null, null, 10f, 5f);

							FrameIO.SaveFrame(frameToAddTo);
							FrameIO.SaveFrame(frameset);

						} catch (IOException e) {
							e.printStackTrace();
						}

						image.flush();

						synchronized (notifier) {
							// Notifying that the timer has finished
							notifier.notify();
						}
					}
				}
			};

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					try {
						JSObject window = (JSObject) webEngine.executeScript("window");

						webEngine.executeScript(""
								// Initializing the counter used when scrolling the page
								+ "var scrollCounter = 0;"
								+ "var scrollCounterHorizontal = 0;"
								
								// Storing the current scroll position
								+ "var originalScrollX = window.pageXOffset;"
								+ "var originalScrollY = window.pageYOffset;");
												
						window.setMember("horizontalScrollPerPage", horizontalScrollPerPage);
						window.setMember("verticalScrollPerPage", verticalScrollPerPage);
						
						
						
						// The scrollPerPage will always be less than the page's height, due to the overlap being added/allowed for between pages, 
						// but if the webpage fits in a single converted page, there's no need for any overlap, so just use 1 as the number of pages
						if((Boolean) webEngine.executeScript("document.documentElement.scrollHeight > window.innerHeight")) {
							pagesVertical.setValue((int) Math.ceil((Integer) webEngine.executeScript("document.documentElement.scrollHeight") / (float) verticalScrollPerPage));
						}
						
						if((Boolean) webEngine.executeScript("document.documentElement.scrollWidth > window.innerWidth")) {
							pagesHorizontal.setValue((int) Math.ceil((Integer) webEngine.executeScript("document.documentElement.scrollWidth") / (float) horizontalScrollPerPage));
						}
						
						System.out.println(webEngine.executeScript("document.documentElement.scrollWidth") + "/" + horizontalScrollPerPage);
						System.out.println(pagesVertical.getValue() + "x" + pagesHorizontal.getValue());
						
						// Setting up the element that contains the CSS to hide all text. Also hiding readability mode buttons.
						// This is wiped before the text is converted, then re-added before taking the screenshot
						webEngine.executeScript(""
								+ "var cssHide = document.createElement('style');"
								+ "cssHide.type = 'text/css';"
								+ "var wordSpanHiddenStyle = 'WordSpan, #readOverlay #readTools { visibility: hidden !important;}';"
								+ "cssHide.appendChild(document.createTextNode(wordSpanHiddenStyle));"
								+ "document.getElementsByTagName('head')[0].appendChild(cssHide);"
								);
						
						// Replacing line breaks in all <pre> tags with <br> tags, otherwise they are lost during the conversion
						webEngine.executeScript(""
								+ "var pres = document.getElementsByTagName ('pre');"
								+ "for(var i = 0; i < pres.length; i++){"
								+ "		pres[i].innerHTML = pres[i].innerHTML.replace(/\\n|\\r/g, '<br />');"
								+ "}");

						// Functions to be used later in JavaScript
						webEngine.executeScript(""
										+ "function addToSpan(text) {"
										+ "		span = document.createElement('wordSpan');"
										+ "		span.textContent = text;"
										+ "		par.insertBefore(span, refNode);" 
										+ "		if (prevSpan !== null && span.getBoundingClientRect().top > prevSpan.getBoundingClientRect().top) {"
										+ "			span.textContent = '\\n' + span.textContent;"
										+ "			if ( prevPrevSpan !== null && prevPrevSpan.getBoundingClientRect().left == prevSpan.getBoundingClientRect().left) {"
										+ "				prevPrevSpan.textContent = prevPrevSpan.textContent + prevSpan.textContent;" 
										+ "				par.removeChild(prevSpan);" 
										+ "			} else {"
										+ "				prevPrevSpan = prevSpan;" 
										+ "			}" 
										+ "			prevSpan = span;"
										+ "		} else if ( prevSpan !== null) {"
										+ "			prevSpan.textContent = prevSpan.textContent + span.textContent;" 
										+ "			par.removeChild(span);" 
										+ "		} else {" 
										+ "			prevSpan = span;" 
										+ "		}"
										+ "}"

										+ "function splitIntoWords(toSplit) {"
										+ "		var words = [];"
										+ "		var pattern = /\\s+/g;"
										+ "		var words = toSplit.split(pattern);"
										+ ""
										+ "		for (var i = 0; i < words.length - 1; i++) {"
										+ "			words[i] = words[i] + ' ';"
										+ "		}"
										+ "		return words;"
										+ "}"
						);
					
						// Using Javascript to get an array of all the text nodes in the document so they can be wrapped in spans. Have to
						// loop through twice (once here to build the array and once later actually going through the array, otherwise when the
						// textnode is removed from the document items end up being skipped)
						webEngine.executeScript("" 
									+ "var node;" 
									+ "var textNodes=[];"
									+ "var walk = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);"
									);					
						
						while(webEngine.executeScript("node=walk.nextNode()") != null && browserWidget.isParserRunning()) {
							
							webEngine.executeScript(""
									+ "if((node.textContent.trim().length > 0)) { "
										+ "textNodes.push(node);" 
									+ "}"
							);
						}
						
						JSObject textNodes = (JSObject) webEngine.executeScript("textNodes");
			
						int nodesLength = (Integer) textNodes.getMember("length");

						// Looping through all the text nodes in the document
						for (int j = 0; j < nodesLength && browserWidget.isParserRunning(); j++) {
							Node currentNode = (Node) textNodes.getSlot(j);

							// Making the current node accessible in JavaScript
							window.setMember("currentNode", currentNode);
						
							webEngine.executeScript(""
									+ "var span = null, prevSpan = null, prevPrevSpan = null;"
									
									// Removing repeated whitespace from the text node's content then splitting it into individual words
									+ "var textContent  = currentNode.textContent.replace(/\\n|\\r/g, '').replace(/\\s+/g, ' ');"
									+ "var words = splitIntoWords(textContent);"
									
									+ "var refNode = currentNode.nextSibling;"
									+ "var par = currentNode.parentElement;"
									+ "currentNode.parentElement.removeChild(currentNode);"
									
									+ "for (var i = 0; i < words.length; i++) {" 
									+ "		addToSpan(words[i]);"
									+ "}"
									
									+ "if (prevPrevSpan !== null && prevPrevSpan.getBoundingClientRect().left == prevSpan.getBoundingClientRect().left) {"
									+ "		prevPrevSpan.textContent = prevPrevSpan.textContent + prevSpan.textContent;"
									+ "		par.removeChild(prevSpan);"
									+ "}"
									);

							// Will never reach 100% here, as the processing is not quite finished - progress is set to 100% at the end of
							// the addPageToFrame loop below
							try {
								progressBar.set((50 * (j + 1)) / nodesLength);
							} catch (Exception e) {
								// Seems to be a bug somewhere along the line when updating the progressbar, so am catching any exception 
								// thrown here to avoid it stuffing up the rest of the parsing 
								e.printStackTrace();
							}
						}

						// Finding all links within the page, then setting the href attribute of all their descendants to be the same
						// link/URL.
						// This is needed because there is no apparent and efficient way to check if an element is a child of a link when
						// running through the document when added each element to Expeditee
						webEngine.executeScript(""
								+ "var anchors = document.getElementsByTagName('a');"
								+ ""
								+ "for (var i = 0; i < anchors.length; i++) {"
									+ "var currentAnchor = anchors.item(i);"
									+ "var anchorDescendants = currentAnchor.querySelectorAll('*');"
									+ "for (var j = 0; j < anchorDescendants.length; j++) {"
										+ "anchorDescendants.item(j).href = currentAnchor.href;"
									+ "}"
								+ "}"
								);

					} catch (Exception ex) {
						ex.printStackTrace();
					}

					synchronized (notifier) {
						notifier.notify();
					}
				}
			});

			synchronized (notifier) {
				try {
					// Waiting for the page setup (splitting into spans) to finish
					notifier.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// Loop that scrolls the page horizontally
			for(int i = 0; i < pagesHorizontal.getValue() && browserWidget.isParserRunning(); i++) {
								
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						try {
							// Scrolling down the page
							webEngine.executeScript(""
									+ "scrollCounter = 0;"
									+ "window.scrollTo(scrollCounterHorizontal * horizontalScrollPerPage, 0);"
									+ "scrollCounterHorizontal = scrollCounterHorizontal+1;");

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				
				// Loop that scrolls the page vertically (for each horizontal scroll position)
				for(int j = 0; j < pagesVertical.getValue()  && browserWidget.isParserRunning(); j++) {
					
					try {
						progressBar.set((int) (50 + ((float)(j+1)/(pagesVertical.getValue() * pagesHorizontal.getValue())  + ((float)(i) / pagesHorizontal.getValue())) * 50));
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							try {
								// Scrolling down the page
								webEngine.executeScript(""
										+ "window.scrollTo(window.pageXOffset, scrollCounter * verticalScrollPerPage);"
										+ "scrollCounter = scrollCounter+1;");
	
								synchronized (notifier) {
									notifier.notify();
								}
	
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
	
					synchronized (notifier) {
						try {
							// Waiting for the page to be scrolled
							notifier.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
	
					timer.start();
	
					synchronized (notifier) {
						try {
							// Waiting for the timer thread to finish before looping again
							notifier.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
	
				}

				horizontalCount.setValue(horizontalCount.getValue() + 1);
				verticalCount.setValue(0);
			}

			if(browserWidget.isParserRunning()) {
				progressBar.set(100);
			} else {
				MessageBay.displayMessage("Web page conversion cancelled");
			}
			
			browserWidget.parserFinished();

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					// Scrolling to the original position on the page
					webEngine.executeScript("window.scrollTo(originalScrollX, originalScrollY)");
					// Reloading the page once the parsing is done - only realistic way to reset (i.e. remove all the added WordSpan tags)
					// the page
					webEngine.reload();
				}
			});

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Platform.runLater(new Runnable() {

			@Override
			public void run() {		
				browserWidget.setOverlayVisible(false); 
				browserWidget.rebindWebViewSize();
				browserWidget.setScrollbarsVisible(true);
			}
		});

	}

	/**
	 * @param rgbString
	 *            string in the format <i>rgb(x,x,x)</i> or <i>rgba(x,x,x,x)</i>
	 * @return A Color object that should match the rgb string passed int. Returns null if alpha is 0
	 */
	private static Color rgbStringToColor(String rgbString) {

		if (rgbString == null) {
			return null;
		}

		// Splitting the string into 'rgb' and 'x, x, x'
		String[] tmpStrings = rgbString.split("\\(|\\)");

		// Splitting up the RGB(A) components into an array
		tmpStrings = tmpStrings[1].split(",");

		int[] components = new int[4];
		Arrays.fill(components, 255);

		for (int i = 0; i < tmpStrings.length; i++) {
			Float d = Float.parseFloat(tmpStrings[i].trim());

			components[i] = Math.round(d);
		}

		if (components[3] > 0) {
			return new Color(components[0], components[1], components[2], components[3]);
		} else {
			return null;
		}
	}
	
	/**
	 * @param rootElement
	 *            Element that will be converted (including all sub-elements)
	 * @param backgroundColor
	 *            String to be used as the background color of this element when added. In the format "rgb(x,x,x)" or "rgba(x,x,x,x)"
	 * @param window
	 *            'window' from Javascript
	 * @param webEngine
	 *            Web engine that the page is loaded in
	 * @param frame
	 *            Expeditee frame to add the converted page to
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private static void addPageToFrame(Node rootElement, JSObject window, WebEngine webEngine, Frame frame) throws InvocationTargetException, IllegalAccessException,
			IllegalArgumentException {
		
		Node currentNode = rootElement;

		if (currentNode.getNodeType() == Node.TEXT_NODE || currentNode.getNodeType() == Node.ELEMENT_NODE) {
			
			JSObject style;
			JSObject bounds;

			if (currentNode.getNodeType() == Node.TEXT_NODE) {
				// CSS style for the element
				style = (JSObject) window.call("getComputedStyle", new Object[] { currentNode.getParentNode() });

				// Getting a rectangle that represents the area and position of the element
				bounds = (JSObject) ((JSObject) currentNode.getParentNode()).call("getBoundingClientRect", new Object[] {});
			} else {
				style = (JSObject) window.call("getComputedStyle", new Object[] { currentNode });

				bounds = (JSObject) ((JSObject) currentNode).call("getBoundingClientRect", new Object[] {});
			}
			
			// Bounding rectangle position is relative to the current view, so scroll position must be added to x/y
			// TODO: This doesn't check if an element or any of its parent elements have position:fixed set - the only
			// way to check seems to be to walking through the element's parents until the document root is reached
			float x = Float.valueOf(bounds.getMember("left").toString()) + Float.valueOf(webEngine.executeScript("window.pageXOffset").toString());
			float y = Float.valueOf(bounds.getMember("top").toString()) + Float.valueOf(webEngine.executeScript("window.pageYOffset").toString());

			float width = Float.valueOf(bounds.getMember("width").toString());
			float height = Float.valueOf(bounds.getMember("height").toString());

			// Checking if the element is actually visible on the page
			if (WebParser.elementVisible(x, y, width, height, style)) {

				// Filtering the node type, starting with text nodes
				if (currentNode.getNodeType() == Node.TEXT_NODE) {
					
					String fontSize = ((String) style.call("getPropertyValue", new Object[] { "font-size" }));

					// Trimming off the units (always px) from the font size
					fontSize = fontSize.substring(0, fontSize.length() - 2);
					
					// Always returns in format "rgb(x,x,x)" or "rgba(x,x,x,x)"
					String color = (String) style.call("getPropertyValue", new Object[] { "color" });
					
					// Always returns in format "rgb(x,x,x)" or "rgba(x,x,x,x)"
					String bgColorString = (String) style.call("getPropertyValue", new Object[] { "background-color" });

					String align = (String) style.call("getPropertyValue", new Object[] { "text-align" });
					
					// Returns comma-separated list of typefaces
					String typeface = (String) style.call("getPropertyValue", new Object[] { "font-family" });

					String[] typefaces = typeface.split(", |,");
					
					String weight = (String) style.call("getPropertyValue", new Object[] { "font-weight" });
					
					String fontStyle = (String) style.call("getPropertyValue", new Object[] { "font-style" });

					// Returns "normal" or a value in pixels (e.g. "10px")
					String letterSpacing = (String) style.call("getPropertyValue", new Object[] { "letter-spacing" });

					// Returns a value in pixels (e.g. "10px")
					String lineHeight = (String) style.call("getPropertyValue", new Object[] { "line-height" });

					String textTransform = (String) style.call("getPropertyValue", new Object[] { "text-transform" });

					String linkUrl = (String) ((JSObject) currentNode.getParentNode()).getMember("href");

					Boolean fontFound = false;
					Font font = new Font(null);

					// Looping through all font-families listed in the element's CSS until one that is installed is
					// found, or the end of the list is reached, in which case the default font is used
					for (int j = 0; j < typefaces.length && !fontFound; j++) {
						if (typefaces[j].toLowerCase().equals("sans-serif")) {
							typefaces[j] = "Arial Unicode MS";
						} else if (typefaces[j].toLowerCase().equals("serif")) {
							typefaces[j] = "Times New Roman";
						} else if ((typefaces[j].toLowerCase().equals("arial"))) {
							// Have to use Arial Unicode, otherwise unicode characters display incorrectly
							typefaces[j] = "Arial Unicode MS";
						}
						
						// Regex will remove any inverted commas surrounding multi-word typeface names
						font = new Font(typefaces[j].replaceAll("^'|'$", ""), Font.PLAIN, 12);
						
						// If the font isn't found, Java just uses Font.DIALOG, so this check checks whether the font was found
						if (!(font.getFamily().toLowerCase().equals(Font.DIALOG.toLowerCase()))) {
							fontFound = true;
						}
					}

					if (font.getFamily().toLowerCase().equals(Font.DIALOG.toLowerCase())) {
						font = new Font("Times New Roman", Font.PLAIN, 12);
					}

					String fontStyleComplete = "";

					int weightInt = 0;

					try {
						weightInt = Integer.parseInt(weight);
					} catch (NumberFormatException nfe) {
						// Use default value as set above
					}

					// checking if font is bold - i.e. 'bold', 'bolder' or weight over 500
					if (weight.toLowerCase().startsWith("bold") || weightInt > 500) {
						fontStyleComplete = fontStyleComplete.concat("bold");
					}

					if (fontStyle.toLowerCase().equals("italic") || fontStyle.toLowerCase().equals("oblique")) {
						fontStyleComplete = fontStyleComplete.concat("italic");
					}

					float fontSizeFloat = 12;

					try {
						fontSizeFloat = Float.valueOf(fontSize);
					} catch (NumberFormatException nfe) {
						// Use default value as set above
					}

					float letterSpacingFloat = -0.008f;

					try {
						letterSpacingFloat = (Integer.parseInt(letterSpacing.substring(0, letterSpacing.length() - 2)) / (fontSizeFloat));
					} catch (NumberFormatException nfe) {
						// Use default value as set above
					}

					float lineHeightInt = -1;
					
					try {
						lineHeightInt = (Float.parseFloat(lineHeight.substring(0, lineHeight.length() - 2)));
					} catch (NumberFormatException nfe) {
						// Use default value as set above
					}

					Text t;

					String textContent = currentNode.getTextContent().replaceAll("[^\\S\\n]+", " ");
					textContent = textContent.replaceAll("^(\\s)(\\n|\\r)", "");

					if (textTransform.equals("uppercase")) {
						textContent = textContent.toUpperCase();
					} else if (textTransform.equals("lowercase")) {
						textContent = textContent.toUpperCase();
					}

					// Adding the text to the frame. Expeditee text seems to be positioned relative to the baseline of the first line, so
					// the font size has to be added to the y-position
					t = frame.addText(Math.round(x), Math.round(y + fontSizeFloat), textContent, null);

					t.setColor(rgbStringToColor(color));
					t.setBackgroundColor(rgbStringToColor(bgColorString));
					t.setFont(font);
					t.setSize(fontSizeFloat);
					t.setFontStyle(fontStyleComplete);
					t.setLetterSpacing(letterSpacingFloat);

					// Removing any spacing between lines allowing t.getLineHeight() to be used to get the actual height
					// of just the characters (i.e. distance from ascenders to descenders)
					t.setSpacing(0);

					t.setSpacing(lineHeightInt - t.getLineHeight());

					if (align.equals("left")) {
						t.setJustification(Justification.left);
					} else if (align.equals("right")) {
						t.setJustification(Justification.right);
					} else if (align.equals("center")) {
						t.setJustification(Justification.center);
					} else if (align.equals("justify")) {
						t.setJustification(Justification.full);
					}

					// Font size is added to the item width to give a little breathing room
					t.setWidth(Math.round(width + (t.getSize())));

					if (!linkUrl.equals("undefined")) {
						t.setAction("gotourl " + linkUrl);
						t.setActionMark(false);
					}

				} else if (currentNode.getNodeType() == Node.ELEMENT_NODE) {

					// Always returns in format "rgb(x,x,x)" or "rgba(x,x,x,x)"
					String bgColorString = (String) style.call("getPropertyValue", new Object[] { "background-color" });

					Color bgColor = rgbStringToColor(bgColorString);

					// If the element has a background color then add it (to Expeditee) as a rectangle with that background color
					if (bgColor != null) {
						frame.addRectangle(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 0, null, bgColor);
					}
					
					String linkUrl = (String) ((JSObject) currentNode).getMember("href");

					// background image, returns in format "url(protocol://absolute/path/to/img.extension)" for images,
					// may also return gradients, data, etc. (not handled yet). Only need to add bg image on
					// 'ELEMENT_NODE' (and not 'TEXT_NODE' otherwise there would be double-ups
					if (((String) style.call("getPropertyValue", new Object[] { "background-image" })).startsWith("url(")) {

						try {
							WebParser.addBackgroundImageFromNode(currentNode, style, frame, linkUrl, x, y, width, height);
						} catch (MalformedURLException mue) {
							// probably a 'data:' url, not supported yet
							mue.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					String imgSrc;

					if (currentNode.getNodeName().toLowerCase().equals("img") && (imgSrc = ((JSObject) currentNode).getMember("src").toString()) != null) {
						try {
							WebParser.addImageFromUrl(imgSrc, linkUrl, frame, x, y, (int) width, null, null, null, null, null, 0, 0);
						} catch (MalformedURLException mue) {
							// probably a 'data:' url, not supported yet
							mue.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}

			Node childNode = currentNode.getFirstChild();

			while (childNode != null) {
				addPageToFrame(childNode, window, webEngine, frame);
				childNode = childNode.getNextSibling();
			}
		}
	}

	private static boolean elementVisible(float x, float y, float width, float height, JSObject style) {
		if (width <= 0 || height <= 0 || x + width <= 0 || y + height <= 0 || ((String) style.call("getPropertyValue", new Object[] { "visibility" })).equals("hidden")
				|| ((String) style.call("getPropertyValue", new Object[] { "display" })).equals("none")) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * @param imgSrc
	 *            URL of the image to add
	 * @param linkUrl
	 *            Absolute URL that the image should link to when clicked
	 * @param frame
	 *            Frame to add the image to
	 * @param x
	 *            X-coordinate at which the image should be placed on the frame
	 * @param y
	 *            Y-coordinate at which the image should be placed on the frame
	 * @param width
	 *            Width of the image once added to the frame. Negative 1 (-1) will cause the actual width of the image file to be used
	 * 
	 * @param cropStartX
	 *            X-coordinate at which to start crop, or null for no crop
	 * @param cropStartY
	 *            Y-coordinate at which to start crop, or null for no crop
	 * @param cropEndX
	 *            X-coordinate at which to end the crop, or null for no crop
	 * @param cropEndY
	 *            Y-coordinate at which to end the crop, or null for no crop
	 * 
	 * @param repeat
	 *            String determining how the image should be tiled/repeated. Valid strings are: <i>no-repeat</i>, <i>repeat-x</i>, or
	 *            <i>repeat-y</i>. All other values (including null) will cause the image to repeat in both directions
	 * 
	 * @param originXPercent
	 *            Percentage into the image to use as the x coordinate of the image's origin point
	 * @param originYPercent
	 *            Percentage into the image to use as the y coordinate of the image's origin point
	 * 
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static Picture getImageFromUrl(String imgSrc, String linkUrl, final Frame frame, float x, float y, int width,
			Integer cropStartX, Integer cropStartY, Integer cropEndX, Integer cropEndY, String repeat, float originXPercent, float originYPercent)
					throws IOException {

		URL imgUrl = new URL(imgSrc);

		HttpURLConnection connection = (HttpURLConnection) (imgUrl.openConnection());

		// Spoofing a widely accepted User Agent, since some sites refuse to serve non-webbrowser clients
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");

		BufferedImage img = ImageIO.read(connection.getInputStream());

		int hashcode = Arrays.hashCode(img.getData().getPixels(0, 0, img.getWidth(), img.getHeight(), (int[]) null));
		File out = new File(FrameIO.IMAGES_PATH + Integer.toHexString(hashcode) + ".png");
		out.mkdirs();
		ImageIO.write(img, "png", out);

		if (repeat == null && cropEndX == null && cropStartX == null && cropEndY == null && cropStartY == null) {
			repeat = "no-repeat";
		}
		
		if (cropEndX == null || cropStartX == null || cropEndY == null || cropStartY == null) {
			cropStartX = 0;
			cropStartY = 0;
			cropEndX = img.getWidth();
			cropEndY = img.getHeight();
		} else if (cropStartX < 0) {
			cropEndX = cropEndX - cropStartX;
			x = x + Math.abs(cropStartX);
			cropStartX = 0;
		}

		if (cropStartY < 0) {
			cropEndY = cropEndY - cropStartY;
			y = y + Math.abs(cropStartY);
			cropStartY = 0;
		}

		if (width < 0) {
			width = img.getWidth();
		}

		if (repeat != null) {
			if (repeat.equals("no-repeat")) {
				int tmpCropEndY = (int) (cropStartY + ((float) width / img.getWidth()) * img.getHeight());
				int tmpCropEndX = cropStartX + width;

				cropEndX = (cropEndX < tmpCropEndX) ? cropEndX : tmpCropEndX;
				cropEndY = (cropEndY < tmpCropEndY) ? cropEndY : tmpCropEndY;
			} else if (repeat.equals("repeat-x")) {
				int tmpCropEndY = (int) (cropStartY + ((float) width / img.getWidth()) * img.getHeight());
				cropEndY = (cropEndY < tmpCropEndY) ? cropEndY : tmpCropEndY;
			} else if (repeat.equals("repeat-y")) {
				int tmpCropEndX = cropStartX + width;
				cropEndX = (cropEndX < tmpCropEndX) ? cropEndX : tmpCropEndX;
			}
		}

		if (originXPercent > 0) {
			int actualWidth = cropEndX - cropStartX;

			int originXPixels = Math.round(originXPercent * actualWidth);

			x = x - originXPixels;

			cropStartX = (int) (cropStartX + (width - actualWidth) * originXPercent);
			cropEndX = (int) (cropEndX + (width - actualWidth) * originXPercent);
		}

		if (originYPercent > 0) {
			int height = (int) ((img.getHeight() / (float) img.getWidth()) * width);
			int actualHeight = (cropEndY - cropStartY);
			int originYPixels = Math.round(originYPercent * actualHeight);

			y = y - originYPixels;

			cropStartY = (int) (cropStartY + (height - actualHeight) * originYPercent);
			cropEndY = (int) (cropEndY + (height - actualHeight) * originYPercent);
		}

		Text text = new Text("@i: " + out.getName() + " " + width);
		text.setPosition(x, y);

		Picture pic = ItemUtils.CreatePicture(text, frame);
		
		float invScale = 1 / pic.getScale();

		pic.setCrop((int)(cropStartX * invScale), (int)(cropStartY * invScale), (int)(cropEndX * invScale), (int)(cropEndY * invScale));
		
		if (linkUrl != null && !linkUrl.equals("undefined")) {
			pic.setAction("goto " + linkUrl);
			pic.setActionMark(false);
		}

		return pic;
	}
	
	private static void addImageFromUrl(String imgSrc, String linkUrl, final Frame frame, float x, float y, int width, Integer cropStartX, Integer cropStartY, Integer cropEndX, Integer cropEndY, String repeat,
			float originXPercent, float originYPercent)
			throws IOException {
		Picture pic = getImageFromUrl(imgSrc, linkUrl, frame, x, y, width, cropStartX, cropStartY, cropEndX, cropEndY, repeat, originXPercent, originYPercent);
		frame.addItem(pic);
		pic.anchor();
		pic.getSource().anchor();
	}
	
	public static Picture getBackgroundImageFromNode(Node node, JSObject style, final Frame frame, String linkUrl, float x, float y, float width, float height) throws IOException {
		
		
		String bgImage = (String) style.call("getPropertyValue", new Object[] { "background-image" });
		bgImage = bgImage.substring(4, bgImage.length() - 1);

    	String bgSize = ((String) style.call("getPropertyValue", new Object[] { "background-size" })).toLowerCase();
    	String bgRepeat = ((String) style.call("getPropertyValue", new Object[] { "background-repeat" })).toLowerCase();
    
    	// Returns "[x]px [y]px", "[x]% [y]%", "[x]px [y]%" or "[x]% [y]px"
    	String bgPosition = ((String) style.call("getPropertyValue", new Object[] { "background-position" })).toLowerCase();
    
    	String[] bgOffsetCoords = bgPosition.split(" ");
    
    	int bgOffsetX = 0, bgOffsetY = 0;
    
    	float originXPercent = 0, originYPercent = 0;
    
    	int cropStartX, cropStartY, cropEndX, cropEndY;
    
    	// Converting the x and y offset values to integers (and from % to px if needed)
    	if (bgOffsetCoords[0].endsWith("%")) {
    		bgOffsetX = (int) ((Integer.valueOf(bgOffsetCoords[0].substring(0, bgOffsetCoords[0].length() - 1)) / 100.0) * width);
    		originXPercent = (Integer.valueOf(bgOffsetCoords[0].substring(0, bgOffsetCoords[0].length() - 1))) / 100f;
    	} else if (bgOffsetCoords[0].endsWith("px")) {
    		bgOffsetX = (int) (Integer.valueOf(bgOffsetCoords[0].substring(0, bgOffsetCoords[0].length() - 2)));
    	}
    
    	if (bgOffsetCoords[1].endsWith("%")) {
    		bgOffsetY = (int) ((Integer.valueOf(bgOffsetCoords[1].substring(0, bgOffsetCoords[1].length() - 1)) / 100.0) * height);
    		originYPercent = (Integer.valueOf(bgOffsetCoords[1].substring(0, bgOffsetCoords[1].length() - 1))) / 100f;
    	} else if (bgOffsetCoords[1].endsWith("px")) {
    		bgOffsetY = (int) (Integer.valueOf(bgOffsetCoords[1].substring(0, bgOffsetCoords[1].length() - 2)));
    	}
    
    	// Converting from an offset to crop coords
    	cropStartX = -1 * bgOffsetX;
    	cropEndX = (int) (cropStartX + width);
    
    	cropStartY = -1 * bgOffsetY;
    	cropEndY = (int) (cropStartY + height);
    
    	int bgWidth = -1;
    
    	if (bgSize.equals("cover")) {
    		bgWidth = (int) width;
    	} else if (bgSize.equals("contain")) {
    		// TODO: actually compute the appropriate width
    		bgWidth = (int) width;
    	} else if (bgSize.equals("auto")) {
    		bgWidth = -1;
    	} else {
    		bgSize = bgSize.split(" ")[0];
    
    		if (bgSize.endsWith("%")) {
    			bgWidth = (int) ((Integer.parseInt(bgSize.replaceAll("\\D", "")) / 100.0) * width);
    		} else if (bgSize.endsWith("px")) {
    			bgWidth = Integer.parseInt(bgSize.replaceAll("\\D", ""));
    		}
    	}
    	
    	return getImageFromUrl(bgImage, linkUrl, frame, x, y, bgWidth, cropStartX, cropStartY, cropEndX, cropEndY, bgRepeat, originXPercent, originYPercent);
	}
	
	private static void addBackgroundImageFromNode(Node node, JSObject style, final Frame frame, String linkUrl, float x, float y, float width, float height) throws IOException {
		Picture pic = getBackgroundImageFromNode(node, style, frame, linkUrl, x, y, width, height);
		frame.addItem(pic);
		pic.anchor();
		pic.getSource().anchor();
	}

	/**
	 * @param rootElement
	 *            Element that will be converted (including all sub-elements)
	 * @param backgroundColor
	 *            String to be used as the background color of this element when added. In the format "rgb(x,x,x)" or "rgba(x,x,x,x)"
	 * @param window
	 *            'window' from Javascript
	 * @param webEngine
	 *            Web engine that the page is loaded in
	 * @param frame
	 *            Expeditee frame to add the converted page to
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private static void addTextToFrame(int visibleWidth, int visibleHeight, JSObject window, WebEngine webEngine, Frame frame) throws InvocationTargetException,
			IllegalAccessException, IllegalArgumentException {

		webEngine.executeScript("var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);");
		
		Node currentNode;
		
		while ((currentNode = (Node) webEngine.executeScript("walker.nextNode()")) != null) {	
			JSObject style;
			JSObject bounds;

			// CSS style for the element
			style = (JSObject) window.call("getComputedStyle", new Object[] { currentNode.getParentNode() });

			// Getting a rectangle that represents the area and position of the element
			bounds = (JSObject) ((JSObject) currentNode.getParentNode()).call("getBoundingClientRect", new Object[] {});

			// TODO: This doesn't check if an element or any of its parent elements have position:fixed set - the only way to check seems to
			// be to walking through the element's parents until the document root is reached (or a recursive function)
			float x = Float.valueOf(bounds.getMember("left").toString());
			float y = Float.valueOf(bounds.getMember("top").toString());

			float width = Float.valueOf(bounds.getMember("width").toString());
			float height = Float.valueOf(bounds.getMember("height").toString());

			// Checking if the element is actually visible on the page
			if (width > 0 && height > 0 && x + width > 0 && y + height > 0 && x <= visibleWidth && y <= visibleHeight
					&& !(((String) style.call("getPropertyValue", new Object[] { "display" })).equals("none"))
					&& !(((String) style.call("getPropertyValue", new Object[] { "visibility" })).equals("hidden"))) {

				String fontSize = ((String) style.call("getPropertyValue", new Object[] { "font-size" }));

				// Trimming off the units (always px) from the font size
				fontSize = fontSize.substring(0, fontSize.length() - 2);

				// Always returns in format "rgb(x,x,x)" or "rgba(x,x,x,x)"
				String color = (String) style.call("getPropertyValue", new Object[] { "color" });

				// Always returns in format "rgb(x,x,x)" or "rgba(x,x,x,x)"
				String bgColorString = (String) style.call("getPropertyValue", new Object[] { "background-color" });

				String align = (String) style.call("getPropertyValue", new Object[] { "text-align" });

				// Returns comma-separated list of typefaces
				String typeface = (String) style.call("getPropertyValue", new Object[] { "font-family" });

				String[] typefaces = typeface.split(", |,");

				String weight = (String) style.call("getPropertyValue", new Object[] { "font-weight" });

				String fontStyle = (String) style.call("getPropertyValue", new Object[] { "font-style" });

				// Returns "normal" or a value in pixels (e.g. "10px")
				String letterSpacing = (String) style.call("getPropertyValue", new Object[] { "letter-spacing" });

				// Returns a value in pixels (e.g. "10px")
				String lineHeight = (String) style.call("getPropertyValue", new Object[] { "line-height" });

				String textTransform = (String) style.call("getPropertyValue", new Object[] { "text-transform" });

				String linkUrl = (String) ((JSObject) currentNode.getParentNode()).getMember("href");

				Boolean fontFound = false;
				Font font = new Font(null);

				// Looping through all font-families listed in the element's CSS until one that is installed is
				// found, or the end of the list is reached, in which case the default font is used
				for (int j = 0; j < typefaces.length && !fontFound; j++) {
					if (typefaces[j].toLowerCase().equals("sans-serif")) {
						typefaces[j] = "SansSerif";
					} else if ((typefaces[j].toLowerCase().equals("arial"))) {
						// Have to use Arial Unicode, otherwise unicode characters display incorrectly
						// It seems that not all systems have this font (including some Windows machines), 
						// but as long as the website has a general font type specified (e.g. "font-family: Arial, Sans-Serif"),
						// there should be no noticeable difference.
						typefaces[j] = "Arial Unicode MS";
					} else if ((typefaces[j].toLowerCase().equals("monospace"))) {
						typefaces[j] = "monospaced";
					}

					// Regex will remove any inverted commas surrounding multi-word typeface names
					font = new Font(typefaces[j].replaceAll("^'|'$", ""), Font.PLAIN, 12);

					// If the font isn't found, Java just uses Font.DIALOG, so this check checks whether the font was found
					if (!(font.getFamily().toLowerCase().equals(Font.DIALOG.toLowerCase()))) {
						fontFound = true;
					}
				}

				if (font.getFamily().toLowerCase().equals(Font.DIALOG.toLowerCase())) {
					font = new Font("Times New Roman", Font.PLAIN, 12);
				}

				String fontStyleComplete = "";

				int weightInt = 0;

				try {
					weightInt = Integer.parseInt(weight);
				} catch (NumberFormatException nfe) {
					// Use default value as set above
				}

				// checking if font is bold - i.e. 'bold', 'bolder' or weight over 500
				if (weight.toLowerCase().startsWith("bold") || weightInt > 500) {
					fontStyleComplete = fontStyleComplete.concat("bold");
				}

				if (fontStyle.toLowerCase().equals("italic") || fontStyle.toLowerCase().equals("oblique")) {
					fontStyleComplete = fontStyleComplete.concat("italic");
				}

				float fontSizeFloat = 12;

				try {
					fontSizeFloat = Float.valueOf(fontSize);
				} catch (NumberFormatException nfe) {
					// Use default value as set above
				}

				float letterSpacingFloat = -0.008f;

				try {
					letterSpacingFloat = (Integer.parseInt(letterSpacing.substring(0, letterSpacing.length() - 2)) / (fontSizeFloat));
				} catch (NumberFormatException nfe) {
					// Use default value as set above
				}

				float lineHeightInt = -1;

				try {
					lineHeightInt = (Float.parseFloat(lineHeight.substring(0, lineHeight.length() - 2)));
				} catch (NumberFormatException nfe) {
					// Use default value as set above
				}

				Text t;

				String textContent = currentNode.getTextContent().replaceAll("[^\\S\\n]+", " ");
				textContent = textContent.replaceAll("^(\\s)(\\n|\\r)", "");

				if (textTransform.equals("uppercase")) {
					textContent = textContent.toUpperCase();
				} else if (textTransform.equals("lowercase")) {
					textContent = textContent.toUpperCase();
				}

				// Adding the text to the frame. Expeditee text seems to be positioned relative to the baseline of the first line, so
				// the font size has to be added to the y-position
				t = frame.addText(Math.round(x), Math.round(y + fontSizeFloat), textContent, null);

				t.setColor(rgbStringToColor(color));
				t.setBackgroundColor(rgbStringToColor(bgColorString));
				t.setFont(font);
				t.setSize(fontSizeFloat);
				t.setFontStyle(fontStyleComplete);
				t.setLetterSpacing(letterSpacingFloat);

				// Removing any spacing between lines allowing t.getLineHeight() to be used to get the actual height
				// of just the characters (i.e. distance from ascenders to descenders)
				t.setSpacing(0);

				t.setSpacing(lineHeightInt - t.getLineHeight());

				if (align.equals("left")) {
					t.setJustification(Justification.left);
				} else if (align.equals("right")) {
					t.setJustification(Justification.right);
				} else if (align.equals("center")) {
					t.setJustification(Justification.center);
				} else if (align.equals("justify")) {
					t.setJustification(Justification.full);
				}

				// Font size is added to the item width to give a little breathing room
				t.setWidth(Math.round(width + (t.getSize())));

				if (!linkUrl.equals("undefined")) {
					t.setAction("createFrameWithBrowser " + linkUrl);
					t.setActionMark(false);
				}
			}
		}
	}

	/**
	 * Used by the web parser to add Next, Previous, etc. buttons to the converted pages
	 * 
	 * @param text
	 *            text to display on the button
	 * @param link
	 *            Frame that the button will link to
	 * @param action
	 *            Action to run when button is clicked
	 * @param width
	 *            Width of the button
	 * @param toAddTo
	 *            Frame to add the button to
	 * @param anchorTop
	 * @param anchorRight
	 * @param anchorBottom
	 * @param anchorLeft
	 */
	private static void addButton(String text, String link, String action, int width, Frame toAddTo, Float anchorTop, Float anchorRight, Float anchorBottom, Float anchorLeft) {
		// Button to go to the next frame/page
		Text button = new Text(text);

		button.setLink(link);
		button.addAction(action);
		button.setBorderColor(new Color(0.7f, 0.7f, 0.7f));
		button.setBackgroundColor(new Color(0.9f, 0.9f, 0.9f));
		button.setThickness(1);
		button.setLinkMark(false);
		button.setActionMark(false);
		button.setFamily("Roboto Condensed Light");
		button.setJustification(Justification.center);
		button.setWidth(width);

		if (anchorTop != null) {
			button.setAnchorTop(anchorTop);
		}

		if (anchorRight != null) {
			button.setAnchorRight(anchorRight);
		}

		if (anchorBottom != null) {
			button.setAnchorBottom(anchorBottom);
		}

		if (anchorLeft != null) {
			button.setAnchorLeft(anchorLeft);
		}

		button.setID(toAddTo.getNextItemID());
		toAddTo.addItem(button);

	}


	private static class MutableBool {
		private boolean value;

		public MutableBool(boolean value) {
			this.value = value;
		}

		public boolean getValue() {
			return value;
		}

		public void setValue(boolean value) {
			this.value = value;
		}
	}

	private static class MutableInt {
		private int value;

		public MutableInt(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
	}
}
