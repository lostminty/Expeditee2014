package org.expeditee.gui;

import java.awt.Color;

public class ColorUtils {

	/**
	 * Gets the next color in the specified color wheel.
	 * 
	 * @param color
	 *            the current color
	 * @param wheel
	 *            the color wheel from which to find the next color
	 * @param skip
	 *            a color to ignore, for example the current background color
	 *            when getting the next foreground color or null if no colors
	 *            should be skipped
	 * @return the next color on the color wheel
	 */
	public static Color getNextColor(Color color, Color[] wheel, Color skip) {
		if (color == null) {
			color = wheel[0];
		} else {
			// search through the colour wheel to find the next colour
			int pos = -1;
			for (int i = 0; i < wheel.length; i++) {
				if (color.equals(wheel[i])) {
					pos = i;
					break;
				}
			}
			pos++;
			pos = pos % wheel.length;
			color = wheel[pos];
		}
		
		if (skip != null && skip.equals(color))
			return getNextColor(color, wheel, skip);

		return color;
	}
}
