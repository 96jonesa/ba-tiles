package com.batiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class ArenaAreaTest
{
	private static int count(ArenaMapLayout layout)
	{
		int count = 0;
		for (int x = 0; x < BaArenaMapTopology.WIDTH; x++)
		{
			for (int y = 0; y < BaArenaMapTopology.HEIGHT; y++)
			{
				if (ArenaArea.contains(layout, x, y))
				{
					count++;
				}
			}
		}
		return count;
	}

	public static class Contains
	{
		@Test
		public void areaSizesArePinned()
		{
			assertEquals(1259, count(ArenaMapLayout.WAVES_1_TO_9));
			assertEquals(1373, count(ArenaMapLayout.WAVE_10));
		}

		@Test
		public void includesEveryWalkableTile()
		{
			for (ArenaMapLayout layout : ArenaMapLayout.values())
			{
				for (int x = 0; x < BaArenaMapTopology.WIDTH; x++)
				{
					for (int y = 0; y < BaArenaMapTopology.HEIGHT; y++)
					{
						if (BaArenaMapTopology.isUsableTile(layout, x, y))
						{
							assertTrue(layout + " " + x + "," + y, ArenaArea.contains(layout, x, y));
						}
					}
				}
			}
		}

		@Test
		public void includesUnwalkableLandmarksInAndOnTheWalls()
		{
			ArenaMapLayout map = ArenaMapLayout.WAVES_1_TO_9;
			// cannons (enclosed), dispensers (in the south wall), horn of glory
			for (int[] tile : new int[][]{{21, 26}, {40, 26}, {33, 6}, {34, 6}, {35, 6}, {36, 6}, {52, 10}})
			{
				assertFalse(BaArenaMapTopology.isUsableTile(map, tile[0], tile[1]));
				assertTrue(tile[0] + "," + tile[1], ArenaArea.contains(map, tile[0], tile[1]));
			}
			assertTrue(ArenaArea.contains(ArenaMapLayout.WAVE_10, 34, 6));
		}

		@Test
		public void excludesOutsideTheWalls()
		{
			for (ArenaMapLayout layout : ArenaMapLayout.values())
			{
				assertFalse(ArenaArea.contains(layout, 0, 0));
				assertFalse(ArenaArea.contains(layout, 63, 47));
				assertFalse(ArenaArea.contains(layout, 30, 46));
				assertFalse(ArenaArea.contains(layout, -1, 20));
			}
		}
	}
}
