package org.expeditee.io;


import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FreeItems;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Item;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Picture;
import org.expeditee.items.StringUtils;
import org.expeditee.items.Text;

/**
 * Allows item data (text) and metadata (position, shapes, etc) to be stored on the clipboard
 * 
 * @author jts21
 */
public class ItemSelection implements Transferable {
	
	/**
	 * Class used for storing data which can be used to reconstruct expeditee objects from the clipboard
	 * Stores data as a String containing .exp save data
	 */
	public static final class ExpDataHandler implements Serializable {
		
		private static final long serialVersionUID = 1L;

		// whether this should be automatically picked up
		public boolean autoPaste = true;
		
		// exp save data
		public String items;
	}
	
	public static final DataFlavor expDataFlavor = new DataFlavor(ExpDataHandler.class, "expDataHandler");
	
	private static final int STRING = 0;
    private static final int IMAGE = 1;
    private static final int EXP_DATA = 2;
	
	private static final DataFlavor[] flavors = {
        DataFlavor.stringFlavor,
        DataFlavor.imageFlavor,
        expDataFlavor
    };
	
	private String data;
	private Image image;
	private ExpDataHandler expData;
	
	public ItemSelection(String data, Image image, ExpDataHandler expData) {
        this.data = data;
        this.image = image;
        this.expData = expData;
    }
	
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return (DataFlavor[])flavors.clone();
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		for (int i = 0; i < flavors.length; i++) {
            if (flavor.equals(flavors[i])) {
                return true;
            }
        }
        return false;
	}

	@Override
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
        if (flavor.equals(flavors[STRING])) {
            return (Object)data;
        } else if (flavor.equals(flavors[IMAGE])) {
        	return (Object)image;
        } else if (flavor.equals(flavors[EXP_DATA])) {
            return (Object)expData;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
	}
	
	private static List<Item> getAllToCopy() {
		List<Item> tmp = new ArrayList<Item>(FreeItems.getInstance());
		List<Item> toCopy = new ArrayList<Item>(tmp);
		for(Item i : tmp) {
			for(Item c : i.getAllConnected()) {
				if(! toCopy.contains(c)) {
					toCopy.add(c);
					FrameMouseActions.pickup(c);
				}
			}
		}
		return toCopy;
	}
	
	public static void cut() {
		
		List<Item> toCopy = getAllToCopy();
		
		copy(toCopy);
		
		// remove the items attached to the cursor
		DisplayIO.getCurrentFrame().removeAllItems(toCopy);
		FreeItems.getInstance().clear();
		
		FrameGraphics.refresh(false);
	}
	
	public static void copyClone() {
		
		copy(ItemUtils.CopyItems(getAllToCopy()));
		
	}
	
	/**
	 * Copies whatever items are attached to the mouse to the clipboard
	 */
	private static void copy(List<Item> items) {
		if(items.size() <= 0) {
			return;
		}
		
		StringBuilder clipboardText = new StringBuilder();
		ExpDataHandler expData = new ExpDataHandler();
		Image image = null;
		
		// get plaintext
		for(Item i : items) {
			if(i instanceof Text) {
				clipboardText.append(((Text)i).getText());
				clipboardText.append("\n\n");
			}
			if(i instanceof Picture) {
				// TODO: merge multiple images if necessary
				image = ((Picture)i).getImage();
			}
		}
		
		// get expeditee item data
		ExpClipWriter ecw = new ExpClipWriter(FrameMouseActions.getX(), FrameMouseActions.getY());
		try {
			ecw.output(items);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		expData.items = ecw.getFileContents();
		// System.out.println(expData.items); 
		
		// write out to clipboard
		ItemSelection selection = new ItemSelection(clipboardText.toString(), image, expData);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
	}
	
	/**
	 * Generates items from the clipboard data
	 *  TODO: Enable pasting raw image data (would require saving the data as an image file and generating a Text item pointing to it)
	 */
	public static void paste() {
		if(FreeItems.itemsAttachedToCursor()) {
			MessageBay.displayMessage("Drop any items being carried on the cursor, then try pasting again");
			return;
		}
		String type = "";
		FreeItems f = FreeItems.getInstance();
		Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable content = c.getContents(null);
		try {
			if(content.isDataFlavorSupported(ItemSelection.expDataFlavor)) {	// Expeditee data
				type = "Expeditee ";
				ExpDataHandler expData = (ExpDataHandler)content.getTransferData(ItemSelection.expDataFlavor);
				if(expData != null) {
					List<Item> items = new ExpClipReader(FrameMouseActions.getX(), FrameMouseActions.getY()).read(expData.items);
					// generate new IDs and pickup
					FrameMouseActions.pickup(ItemUtils.CopyItems(items));
				}
			} else if(content.isDataFlavorSupported(DataFlavor.imageFlavor)) {	// Image data
				// System.out.println("IZ PIKTUR");
				type = "Image ";
				BufferedImage img = (BufferedImage)content.getTransferData(DataFlavor.imageFlavor);
				int hashcode = Arrays.hashCode(img.getData().getPixels(0, 0, img.getWidth(), img.getHeight(), (int[])null));
				File out = new File(FrameIO.IMAGES_PATH + Integer.toHexString(hashcode) + ".png");
				out.mkdirs();
				ImageIO.write(img, "png", out);
				Text item = DisplayIO.getCurrentFrame().createNewText("@i: " + out.getPath());
				f.add(item);
				ExpClipReader.updateItems(f);
			} else if(content.isDataFlavorSupported(DataFlavor.stringFlavor)) {	// Plain text
				type = "Plain Text ";
				String clip = ((String) content.getTransferData(DataFlavor.stringFlavor));
				// Covert the line separator char when pasting in
				// windows (\r\n) or max (\r)
				clip = StringUtils.convertNewLineChars(clip);
				// blank line is an item separator
				String[] items = clip.split("\n\n");
				Item item, prevItem = null;
				for(int i = 0; i < items.length; i++) {
					// System.out.println(items[i]);
					// System.out.println("Created item from string");
					item = DisplayIO.getCurrentFrame().createNewText(items[i]);
					if(prevItem != null){
						item.setY(prevItem.getY() + prevItem.getBoundsHeight());
					}
					f.add(item);
					prevItem = item;
				}
			} /* else if {
				// Next handler
			} */
		} catch (Exception e) {
			System.out.println("Failed to load " + type + "data");
			e.printStackTrace();
		}
	}

}
