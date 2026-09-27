package com.batiles;

import java.awt.Color;
import javax.annotation.Nullable;

/**
 * How a BA Tile's fill and border are drawn.
 *
 * <p>By default a tile is filled black at the plugin's Fill Opacity, with a border of the plugin's Border Width. A tile
 * can instead be filled with its own color at its own opacity, and can have its own border width.
 */
final class TileStyle
{
	static final int MIN_FILL_OPACITY_PERCENT = 0;
	static final int MAX_FILL_OPACITY_PERCENT = 100;
	static final float MIN_BORDER_WIDTH = 0f;
	static final float MAX_BORDER_WIDTH = 8f;

	private TileStyle()
	{
	}

	/**
	 * @param tileColor the tile's color (its alpha is the border's, and is ignored for the fill)
	 * @param fillOpacityPercent the tile's own fill opacity, or null for the default black fill
	 * @param defaultFillAlpha the plugin's Fill Opacity (0-255), for the default black fill
	 */
	static Color fill(Color tileColor, @Nullable Integer fillOpacityPercent, int defaultFillAlpha)
	{
		if (fillOpacityPercent == null)
		{
			return new Color(0, 0, 0, clamp(defaultFillAlpha, 0, 255));
		}
		int percent = clamp(fillOpacityPercent, MIN_FILL_OPACITY_PERCENT, MAX_FILL_OPACITY_PERCENT);
		return new Color(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue(), Math.round(percent * 255 / 100f));
	}

	/**
	 * @param tileBorderWidth the tile's own border width, or null for the plugin's
	 * @param defaultBorderWidth the plugin's Border Width
	 */
	static float borderWidth(@Nullable Float tileBorderWidth, double defaultBorderWidth)
	{
		float width = tileBorderWidth == null ? (float) defaultBorderWidth : tileBorderWidth;
		return Math.max(MIN_BORDER_WIDTH, Math.min(MAX_BORDER_WIDTH, width));
	}

	private static int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}
}
