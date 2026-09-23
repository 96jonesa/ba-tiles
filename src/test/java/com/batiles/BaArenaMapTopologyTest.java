package com.batiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

/**
 * Pins the walkable-tile tables. The counts were cross-checked against the BA simulators' collision data
 * (every tile without a full movement-blocking flag), which matches tile for tile.
 */
@RunWith(Enclosed.class)
public class BaArenaMapTopologyTest
{
	private static int usableTiles(ArenaMapLayout layout)
	{
		int count = 0;
		for (int x = 0; x < BaArenaMapTopology.WIDTH; x++)
		{
			for (int y = 0; y < BaArenaMapTopology.HEIGHT; y++)
			{
				if (BaArenaMapTopology.isUsableTile(layout, x, y))
				{
					count++;
				}
			}
		}
		return count;
	}

	public static class IsUsableTile
	{
		@Test
		public void usableTileCountsArePinned()
		{
			assertEquals(1070, usableTiles(ArenaMapLayout.WAVES_1_TO_9));
			assertEquals(1117, usableTiles(ArenaMapLayout.WAVE_10));
		}

		@Test
		public void startTilesAreUsable()
		{
			assertTrue(BaArenaMapTopology.isUsableTile(ArenaMapLayout.WAVES_1_TO_9, 31, 10));
			assertTrue(BaArenaMapTopology.isUsableTile(ArenaMapLayout.WAVE_10, 30, 10));
		}

		@Test
		public void outsideTheArenaIsNotUsable()
		{
			assertFalse(BaArenaMapTopology.isUsableTile(ArenaMapLayout.WAVES_1_TO_9, 0, 0));
			assertFalse(BaArenaMapTopology.isUsableTile(ArenaMapLayout.WAVES_1_TO_9, -1, 10));
			assertFalse(BaArenaMapTopology.isUsableTile(ArenaMapLayout.WAVES_1_TO_9, 64, 10));
		}
	}
}
