package com.batiles;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class TileVisibilityTest
{
	private static final StrategyPreset DEF_W3_A = new StrategyPreset("p1", "Stack", 3, "d");
	private static final StrategyPreset DEF_W3_B = new StrategyPreset("p2", "Split", 3, "d");

	private static GroundMarkerPoint baseTile(List<Integer> waves, List<String> roles)
	{
		return new GroundMarkerPoint(7509, 30, 30, 0, Color.RED, null, waves, roles);
	}

	private static GroundMarkerPoint presetTile(StrategyPreset preset)
	{
		return new GroundMarkerPoint(7509, 30, 30, 0, Color.RED, null, List.of(preset.getWave()), List.of(preset.getRole()), preset.getId());
	}

	private static TileVisibility visibility(List<Integer> waves, List<String> roles, Map<String, String> active,
											 boolean withPreset)
	{
		Map<String, StrategyPreset> presets = new HashMap<>();
		presets.put(DEF_W3_A.getId(), DEF_W3_A);
		presets.put(DEF_W3_B.getId(), DEF_W3_B);
		return new TileVisibility(waves, roles, (w, r) -> active.get(w + r), presets::get, withPreset);
	}

	public static class IsVisible
	{
		@Test
		public void baseTileShownForMatchingWaveAndRoleWithoutPreset()
		{
			TileVisibility v = visibility(List.of(3), List.of("d"), Map.of(), true);
			assertTrue(v.isVisible(baseTile(List.of(3), List.of("d"))));
		}

		@Test
		public void baseTileHiddenForOtherWaveOrRole()
		{
			TileVisibility v = visibility(List.of(3), List.of("d"), Map.of(), true);
			assertFalse(v.isVisible(baseTile(List.of(4), List.of("d"))));
			assertFalse(v.isVisible(baseTile(List.of(3), List.of("h"))));
		}

		@Test
		public void baseTileWithNullWavesAndRolesMatchesAnything()
		{
			TileVisibility v = visibility(List.of(7), List.of("c"), Map.of(), true);
			assertTrue(v.isVisible(baseTile(null, null)));
		}

		@Test
		public void baseTileWithNullWavesShownEvenWhenNoWaveIsDisplayed()
		{
			TileVisibility v = visibility(List.of(), List.of("d"), Map.of(), true);
			assertTrue(v.isVisible(baseTile(null, List.of("d"))));
			assertFalse(v.isVisible(baseTile(List.of(3), List.of("d"))));
		}

		@Test
		public void baseTileWithNullWavesUsesPresetToggleOfDisplayedWaves()
		{
			TileVisibility v = visibility(List.of(3), List.of("d"), Map.of("3d", "p1"), false);
			assertFalse(v.isVisible(baseTile(null, null)));
		}

		@Test
		public void baseTileAlwaysShownWithoutPreset()
		{
			TileVisibility v = visibility(List.of(3), List.of("d"), Map.of(), false);
			assertTrue(v.isVisible(baseTile(List.of(3), List.of("d"))));
		}

		@Test
		public void baseTileHiddenWithPresetWhenToggledOff()
		{
			TileVisibility v = visibility(List.of(3), List.of("d"), Map.of("3d", "p1"), false);
			assertFalse(v.isVisible(baseTile(List.of(3), List.of("d"))));
		}

		@Test
		public void baseTileShownWithPresetWhenToggledOn()
		{
			TileVisibility v = visibility(List.of(3), List.of("d"), Map.of("3d", "p1"), true);
			assertTrue(v.isVisible(baseTile(List.of(3), List.of("d"))));
		}

		@Test
		public void baseTileTogglesApplyPerWaveAndRole()
		{
			// wave 3 has an active preset and hides base tiles, but wave 4 (also displayed) has none
			TileVisibility v = visibility(List.of(3, 4), List.of("d"), Map.of("3d", "p1"), false);
			assertFalse(v.isVisible(baseTile(List.of(3), List.of("d"))));
			assertTrue(v.isVisible(baseTile(List.of(3, 4), List.of("d"))));
		}

		@Test
		public void presetTileShownOnlyWhileItsPresetIsActive()
		{
			assertTrue(visibility(List.of(3), List.of("d"), Map.of("3d", "p1"), true).isVisible(presetTile(DEF_W3_A)));
			assertFalse(visibility(List.of(3), List.of("d"), Map.of("3d", "p2"), true).isVisible(presetTile(DEF_W3_A)));
			assertFalse(visibility(List.of(3), List.of("d"), Map.of(), true).isVisible(presetTile(DEF_W3_A)));
		}

		@Test
		public void presetTileHiddenWhenItsWaveOrRoleIsNotDisplayed()
		{
			assertFalse(visibility(List.of(4), List.of("d"), Map.of("3d", "p1"), true).isVisible(presetTile(DEF_W3_A)));
			assertFalse(visibility(List.of(3), List.of("a"), Map.of("3d", "p1"), true).isVisible(presetTile(DEF_W3_A)));
		}

		@Test
		public void presetTileOfUnknownPresetHidden()
		{
			StrategyPreset deleted = new StrategyPreset("gone", "Gone", 3, "d");
			assertFalse(visibility(List.of(3), List.of("d"), Map.of("3d", "gone"), true).isVisible(presetTile(deleted)));
		}

		@Test
		public void presetTileUnaffectedByBaseTileToggles()
		{
			assertTrue(visibility(List.of(3), List.of("d"), Map.of("3d", "p1"), false).isVisible(presetTile(DEF_W3_A)));
		}
	}
}
