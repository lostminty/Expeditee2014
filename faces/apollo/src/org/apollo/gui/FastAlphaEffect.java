package org.apollo.gui;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

/**
 * I remember this little trick back in days of DirectDraw:
 * 
 * A fast technique for drawing a 50% alpha blend.
 * 
 * @author Brook Novak
 *
 */
public final class FastAlphaEffect {

	private FastAlphaEffect() {
	}

	/**
	 * @see FastAlphaEffect#fastBlendSingleColor(WritableRaster, ColorModel, int, int, int, int, int)
	 * @param raster
	 * @param colorModel
	 * @param area
	 * @param rgb
	 */
	public static final void fastBlendSingleColor(
			WritableRaster raster,
			ColorModel colorModel,
			Rectangle area,
			int rgb) {
		fastBlendSingleColor(raster, colorModel, area.x, area.y, area.width, area.height, rgb);
	}
	
	/**
	 * 
	 * @param raster
	 * 
	 * @param colorModel
	 * 
	 * @param x
	 * 
	 * @param y
	 * 
	 * @param width
	 * 
	 * @param height
	 * 
	 * @param rgb
	 * 		The color to blend.
	 * 
	 * 
	 * @throws NullPointerException
	 * 			If raster or colorModel is null
	 * 
	 */
	public static final void fastBlendSingleColor(
			WritableRaster raster,
			ColorModel colorModel,
			int x, int y, 
			int width, int height,
			int rgb) {
		
		if (raster == null)
			throw new NullPointerException("raster");
		else if (colorModel == null)
			throw new NullPointerException("raster");
		
		int endcol = x + width;
		int endrow = y + height;
		
		// Clamp bounds
		if (x < 0) x = 0;
		if (y < 0) y = 0;
		if (endcol >= raster.getWidth()) endcol = raster.getWidth() - 1;
		if (endrow >= raster.getHeight()) endrow = raster.getHeight() - 1;
		
		// Safe note: If endrow <= y or endcol <= x then the loops wont be entered

		boolean offsetStart = false;
		
		Object colorData = colorModel.getDataElements(rgb, null);

		for (int row = y; row < endrow; row++) {
			
				
			for (int col = (offsetStart) ? x + 1 : x; col < endcol; col+=2) { // paint every second pixel
		
					raster.setDataElements(col, row, colorData);
				
			}
			
			// Get checker effect
			offsetStart = !offsetStart;
		
		}
		
	}
}
