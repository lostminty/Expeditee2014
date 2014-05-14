package org.expeditee.importer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.imageio.ImageIO;

import org.expeditee.gui.Browser;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameCreator;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.items.Text;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class pdfImporter implements FileImporter {
	
	public Item importFile(final File f, Point location) throws IOException {
		if (location == null || f == null) {
			return null;
		}
		
		final int x = 0;
		final int y = 60;
		final int width = Browser._theBrowser.getSize().width;
		System.out.println(width);
		
		final String name = FrameIO.ConvertToValidFramesetName(f.getName().substring(0,f.getName().lastIndexOf('.'))).toLowerCase();
		
		//check if the file is a pdf
		if(!f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.')+1).toLowerCase().equals("pdf"))
		{
			return null;
		}
		
		final Text link = FrameDNDTransferHandler.importString(name, location);
		link.setLink(name+"1");
		
		final Frame frameset;
		try
		{
			frameset = FrameIO.CreateNewFrameset(name);
		}
		catch(Exception e)
		{
			MessageBay.displayMessage("Frameset \"" + name + "\" already exists, creating a link to it");
			return link;//if the frameset exists just make a link to the existing frameset
		}
		
		final String framesetPath = frameset.getPath()+name+File.separator;
		
		System.out.println("PATH = " + framesetPath);
		
		new Thread() {
			public void run() {
				try {
					MessageBay.displayMessage("Importing " + f.getName() + "...");
					//load a pdf from a byte buffer
		      RandomAccessFile raf = new RandomAccessFile(f, "r");
		      FileChannel channel = raf.getChannel();
		      ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
		      PDFFile pdffile = new PDFFile(buf);
		      int pages = pdffile.getNumPages();
		      
		      Frame currentFrame=frameset;
					Frame _currentFrame=null;
					Text nextButton=null, prevButton=null;
					final float spacing=((Text)FrameCreator.createButton("Next", null, null, 10F, 10F)).getBoundsWidth() + 30F;
		      
		      //make images from the pdf pages, write frames with those images
					for(int i=1; i<=pages; i++)
					{
						//remove the title from the frame
						/* if(FrameDNDTransferHandler.modifier!=1 &&
								currentFrame!=null &&
								currentFrame.getTitleItem()!=null)
							((Text)currentFrame.getTitleItem()).delete(); */
					  //get the pdf page
			      PDFPage page = pdffile.getPage(i);
			      //get the width and height for the page
			      int w=(int)page.getBBox().getWidth(), h=(int)page.getBBox().getHeight();
			      final double res=1080;
			      if(w<res || h<res)
			      {
				      double ws=res/w;
				      double hs=res/h;
				      if(ws>hs)
				      {
					      w*=ws;
					      h*=ws;
				      }
				      else
				      {
					      w*=hs;
					      h*=hs;
				      }
			      }
			      //generate the image
			      Image img = page.getImage(w, h, //width & height
				      new Rectangle(0, 0, (int)page.getBBox().getWidth(), (int)page.getBBox().getHeight()), //clip rect
				      null, //null for the ImageObserver
				      true, //fill background with white
				      true //block until drawing is done
				      );
			      //Create a buffered image to store the image in
			      BufferedImage bimg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
			      Graphics g = bimg.createGraphics();
			      //Paint the image onto the buffered image
			      g.drawImage(img, 0, 0, null);
			      g.dispose();
			      //save it as a file
			      File out = new File(framesetPath+i+".png");
			      ImageIO.write(bimg, "png", out);
			      //generate a frame with that image
			      System.out.println(width);
			      System.out.println("@i: "+framesetPath+i+".png "+width);
						currentFrame.addText(x, y, "@i: "+framesetPath+i+".png "+width, null);
						if(i>1)
						{
						  //put a next button on the previous frame (points to current frame)
						  nextButton=(Text)FrameCreator.createButton("Next", null, null, 10F, 10F);
							nextButton.setID(_currentFrame.getNextItemID());
							nextButton.addAction("next");
							_currentFrame.addItem(nextButton);
							//put a previous button on the current frame (points to previous frame)
							prevButton=(Text)FrameCreator.createButton("Prev", null, null, (i<pages)?spacing:10F, 10F);
							prevButton.setID(currentFrame.getNextItemID());
							prevButton.addAction("previous");
							currentFrame.addItem(prevButton);
						}
						else
						{
							prevButton=(Text)FrameCreator.createButton("Home", null, null, (i<pages)?spacing:10F, 10F);
							prevButton.setID(currentFrame.getNextItemID());
							prevButton.addAction("GotoHome");
							currentFrame.addItem(prevButton);
						}
						FrameIO.SaveFrame(_currentFrame,true);
						_currentFrame=currentFrame;
						if(i<pages) currentFrame = FrameIO.CreateFrame(frameset.getFramesetName(), name, null);
					}
					prevButton=(Text)FrameCreator.createButton("Home", null, null, spacing, 10F);
					prevButton.setID(currentFrame.getNextItemID());
					prevButton.addAction("gotohome");
					currentFrame.addItem(prevButton);
					FrameIO.SaveFrame(currentFrame,true);
					MessageBay.displayMessage(f.getName() + " import complete", Color.GREEN);
					FrameGraphics.requestRefresh(true);
				} catch (Exception e) {
					e.printStackTrace();
					MessageBay.errorMessage(e.getMessage());
				}
			}
		}.start();
		FrameGraphics.refresh(true);
		//return source;
		return link;
	}
}
