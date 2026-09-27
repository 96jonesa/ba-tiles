package com.batiles;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Pins literals of the built-in strategies and assignment presets transcribed from BA Utilities.
 */
public class BaUtilitiesBuiltInStrategiesTest
{
	@Test
	public void hasNineStrategies()
	{
		assertEquals(9, BaUtilitiesBuiltInStrategies.createStrategyPresets().size());
	}

	@Test
	public void wallSplitIsPinned()
	{
		BaUtilitiesData.StrategyPreset wallSplit = BaUtilitiesBuiltInStrategies.createStrategyPresets().stream()
				.filter(s -> s.getName().equals("Wall Split")).findFirst().orElseThrow();
		assertEquals("built-in:tile-marker-strategy:wall-split", wallSplit.getId());
		assertEquals(List.of("built-in:tile-marker-set:n-trap-food", "built-in:tile-marker-set:shir-mainstack-trail",
				"built-in:tile-marker-set:wall-split-multi"), wallSplit.getMarkerSetIds());
		assertEquals(BaUtilitiesData.WaveMap.WAVES_1_TO_9, wallSplit.getWaveMapOrNull());
	}

	@Test
	public void theTwoLineupsAreDefenderOnly()
	{
		List<BaUtilitiesData.AssignmentPreset> lineups = BaUtilitiesBuiltInStrategies.createAssignmentPresets();
		assertEquals(List.of("Beginner", "Intermediate"), List.of(lineups.get(0).getName(), lineups.get(1).getName()));
		for (BaUtilitiesData.AssignmentPreset lineup : lineups)
		{
			assertEquals(BaUtilitiesData.RoleContext.DEFENDER, lineup.getRoleContextOrDefault());
		}
	}

	@Test
	public void intermediateWavesArePinned()
	{
		Map<Integer, String> waves = BaUtilitiesBuiltInStrategies.createAssignmentPresets().get(1).getWaveSelections();
		assertEquals(10, waves.size());
		assertEquals("built-in:tile-marker-strategy:center-food", waves.get(1));
		assertEquals("built-in:tile-marker-strategy:wall-split", waves.get(6));
		assertEquals("built-in:tile-marker-strategy:two-one-five-star-two", waves.get(9));
		assertEquals("built-in:tile-marker-strategy:auk-w10", waves.get(10));
	}
}
