package com.batiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

/**
 * Pins the arena coordinate mapping. These values must match where BA Tiles marked in game are stored
 * (the instance template coordinates), and the mapping used by the BA simulators' ground marker import/export.
 */
@RunWith(Enclosed.class)
public class ArenaMapLayoutTest
{
	public static class FromWave
	{
		@Test
		public void wavesOneToNineShareAnArenaAndWaveTenHasItsOwn()
		{
			for (int wave = 1; wave <= 9; wave++)
			{
				assertEquals(ArenaMapLayout.WAVES_1_TO_9, ArenaMapLayout.fromWave(wave));
			}
			assertEquals(ArenaMapLayout.WAVE_10, ArenaMapLayout.fromWave(10));
		}
	}

	public static class GetRegionId
	{
		@Test
		public void regionIdsArePinned()
		{
			assertEquals(7509, ArenaMapLayout.WAVES_1_TO_9.getRegionId());
			assertEquals(7508, ArenaMapLayout.WAVE_10.getRegionId());
		}
	}

	public static class ToRegionY
	{
		@Test
		public void mapRowZeroIsRegionRowEight()
		{
			assertEquals(8, ArenaMapLayout.WAVES_1_TO_9.toRegionY(0));
			assertEquals(30, ArenaMapLayout.WAVES_1_TO_9.toRegionX(30));
		}
	}

	public static class Contains
	{
		@Test
		public void requiresTheLayoutsRegionAndPlaneZero()
		{
			assertTrue(ArenaMapLayout.WAVES_1_TO_9.contains(new GroundMarkerPoint(7509, 30, 28, 0, null, null, null, null)));
			assertFalse(ArenaMapLayout.WAVES_1_TO_9.contains(new GroundMarkerPoint(7508, 30, 28, 0, null, null, null, null)));
			assertFalse(ArenaMapLayout.WAVES_1_TO_9.contains(new GroundMarkerPoint(7509, 30, 28, 1, null, null, null, null)));
			assertFalse(ArenaMapLayout.WAVES_1_TO_9.contains(new GroundMarkerPoint(7509, 30, 7, 0, null, null, List.of(), List.of())));
		}
	}

	public static class ToMapY
	{
		@Test
		public void invertsToRegionY()
		{
			GroundMarkerPoint point = new GroundMarkerPoint(7509, 12, 40, 0, null, null, null, null);
			assertEquals(32, ArenaMapLayout.WAVES_1_TO_9.toMapY(point));
			assertEquals(12, ArenaMapLayout.WAVES_1_TO_9.toMapX(point));
		}
	}
}
