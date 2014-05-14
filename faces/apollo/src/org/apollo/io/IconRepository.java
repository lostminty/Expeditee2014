package org.apollo.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apollo.gui.Strokes;

/**
 * An icon provider. Only loads icons once and when they are requested (somple proxy approach).
 * 
 * @author Brook Novak
 *
 */
public final class IconRepository {
	
	private static HashMap<String, Icon> _loadedIcons = new HashMap<String, Icon>();
	private static Icon missingIcon = null;

	private IconRepository() {}
	
	private static Icon getMissingIcon() {
		
		if (missingIcon == null) {
			
			Image image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = (Graphics2D)image.getGraphics();
			
			g.setStroke(Strokes.SOLID_2);
			
			// White square with X
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, 16, 16);
			
			g.setColor(Color.RED);
			g.drawLine(0, 0, 16, 16);
			g.drawLine(0, 16, 16, 0);
			
			missingIcon = new ImageIcon(image);
		}
			
		return missingIcon;
	}
	
	/**
	 * 
	 * @param name Exclude path / package
	 * @return
	 */
	public static Icon getIcon(String name) {
		Icon icon = _loadedIcons.get(name);
		
		if (icon == null) {
			try {
			        URL url;
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				if (classLoader!=null) {
				    // Applet friendly
				    url = classLoader.getResource("org/apollo/assets/icons/" + name);
				}
				else {
				    url = ClassLoader.getSystemResource("org/apollo/assets/icons/" + name);
				}

				if (url != null) {
					icon = new ImageIcon(url);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (icon == null) { 
				System.err.println("WARNING: Cannot find icon named \"" + name +"\"");
				return getMissingIcon();
			}
		} 
		
		return icon;
	}
	
}
