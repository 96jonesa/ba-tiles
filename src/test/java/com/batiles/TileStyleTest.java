package com.batiles;

import static org.junit.Assert.assertEquals;
import java.awt.Color;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class TileStyleTest
{
	public static class Fill
	{
		@Test
		public void withoutOwnFillIsBlackAtThePluginFillOpacity()
		{
			assertEquals(new Color(0, 0, 0, 50), TileStyle.fill(Color.CYAN, null, 50));
		}

		@Test
		public void ownFillUsesTheTileColorAtItsOpacity()
		{
			assertEquals(new Color(0, 255, 255, 26), TileStyle.fill(Color.CYAN, 10, 50));
			assertEquals(new Color(0, 255, 255, 255), TileStyle.fill(Color.CYAN, 100, 50));
		}

		@Test
		public void ignoresTheTileColorsOwnAlpha()
		{
			// the color's alpha is the border's; the fill's comes from the fill opacity alone
			assertEquals(new Color(255, 0, 0, 128), TileStyle.fill(new Color(255, 0, 0, 40), 50, 50));
		}

		@Test
		public void clampsOutOfRangeOpacity()
		{
			assertEquals(255, TileStyle.fill(Color.RED, 250, 0).getAlpha());
			assertEquals(0, TileStyle.fill(Color.RED, -5, 0).getAlpha());
		}
	}

	public static class HasBorder
	{
		@Test
		public void onlyAnOwnWidthOfZeroMeansNoBorder()
		{
			assertEquals(true, TileStyle.hasBorder(null));
			assertEquals(true, TileStyle.hasBorder(0.5f));
			assertEquals(false, TileStyle.hasBorder(0f));
		}
	}

	public static class BorderWidth
	{
		@Test
		public void defaultsToThePluginBorderWidth()
		{
			assertEquals(2f, TileStyle.borderWidth(null, 2.0), 0f);
		}

		@Test
		public void usesTheTilesOwnWidthClamped()
		{
			assertEquals(0.5f, TileStyle.borderWidth(0.5f, 2.0), 0f);
			assertEquals(8f, TileStyle.borderWidth(20f, 2.0), 0f);
			assertEquals(0f, TileStyle.borderWidth(-1f, 2.0), 0f);
		}
	}
}
