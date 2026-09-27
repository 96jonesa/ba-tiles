package com.batiles;

import static org.junit.Assert.assertEquals;
import java.awt.Color;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class ColorTileMarkerTest
{
	private static final WorldPoint SPOT = new WorldPoint(1886, 5476, 0);
	private static final ColorTileMarker TILE = new ColorTileMarker(SPOT, Color.CYAN, "1", 10, 0.5f);

	public static class WithoutDuplicates
	{
		@Test
		public void identicalTilesOnTheSameSpotAreDrawnOnce()
		{
			assertEquals(List.of(TILE), ColorTileMarker.withoutDuplicates(List.of(TILE, new ColorTileMarker(SPOT, Color.CYAN, "1", 10, 0.5f))));
		}

		@Test
		public void tilesThatDifferInAnyWayAreAllKept()
		{
			List<ColorTileMarker> different = List.of(
					TILE,
					new ColorTileMarker(SPOT, Color.RED, "1", 10, 0.5f),
					new ColorTileMarker(SPOT, Color.CYAN, "2", 10, 0.5f),
					new ColorTileMarker(SPOT, Color.CYAN, "1", null, 0.5f),
					new ColorTileMarker(SPOT, Color.CYAN, "1", 10, null),
					new ColorTileMarker(SPOT.dx(1), Color.CYAN, "1", 10, 0.5f));
			assertEquals(different, ColorTileMarker.withoutDuplicates(different));
		}

		@Test
		public void keepsTheOriginalOrder()
		{
			ColorTileMarker other = new ColorTileMarker(SPOT, Color.RED, null, null, null);
			assertEquals(List.of(other, TILE), ColorTileMarker.withoutDuplicates(List.of(other, TILE, other)));
		}
	}
}
