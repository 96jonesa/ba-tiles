/*
 * Adapted from BA Utilities (https://github.com/tcourter1/ba-healer-order, commit 676d4d7; its built-in strategies and assignment presets, transcribed whole).
 *
 * Copyright (c) 2026, tcourter1
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.batiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BA Utilities' built-in strategies and assignment presets, transcribed whole from its TileMarkerBuiltInStrategies.
 */
final class BaUtilitiesBuiltInStrategies
{
	static final String BEGINNER_TILES_STRATEGY_ID =
			"built-in:tile-marker-strategy:beginner-tiles";
	static final String W10_BEGINNER_STRATEGY_ID =
			"built-in:tile-marker-strategy:w10-beginner";
	static final String CENTER_FOOD_STRATEGY_ID =
			"built-in:tile-marker-strategy:center-food";
	static final String NW_FOOD_STRATEGY_ID =
			"built-in:tile-marker-strategy:nw-food";
	static final String NO_LOG_STRATEGY_ID =
			"built-in:tile-marker-strategy:no-log";
	static final String WALL_SPLIT_STRATEGY_ID =
			"built-in:tile-marker-strategy:wall-split";
	static final String HENDI_TRIANGLE_STRATEGY_ID =
			"built-in:tile-marker-strategy:hendi-triangle";
	static final String TWO_ONE_FIVE_STAR_TWO_STRATEGY_ID =
			"built-in:tile-marker-strategy:two-one-five-star-two";
	static final String AUK_W10_STRATEGY_ID =
			"built-in:tile-marker-strategy:auk-w10";

	private static final String WALL_SPLIT_NOTES = "44.4 - Move 2E of trap, soft crash\n"
			+ "48.0 - Move to 1E of trap, drop food\n"
			+ "54.0 - Step 1 south and multi";
	private static final String TWO_ONE_FIVE_NOTES = "38.4 (soft crash) - Move 4N of trap, drop 1. Pick up at 43.8. Return to trap.\n\n"
			+ "If 48s runner is W, drop 6 good\n"
			+ "54.0 - Multi, repair\n\n"
			+ "If 48s runner is S/E, drop 6 good + 1 bad\n"
			+ "60.0 - Multi, call, repair, drop 1 S";
	private static final String AUK_W10_NOTES = "12.0 - Delay, then drop 3 good NW\n"
			+ "24.0 - Delay at cave, then get logs. \n"
			+ "Drop 3 good W\n"
			+ "45.0 - Move N of trap, slow multi. Repair.\n"
			+ "Drop 1 more good W.\n"
			+ "Run to cannon and split reserves.";

	private BaUtilitiesBuiltInStrategies()
	{
	}

	static List<BaUtilitiesData.StrategyPreset> createStrategyPresets()
	{
		return Arrays.asList(
				strategy(BEGINNER_TILES_STRATEGY_ID, "Beginner Defender Tiles", "", BaUtilitiesData.WaveMap.WAVES_1_TO_9,
						BaUtilitiesBuiltInSets.BEGINNER_TILES_SET_ID),
				strategy(W10_BEGINNER_STRATEGY_ID, "W10 Beginner", "", BaUtilitiesData.WaveMap.WAVE_10,
						BaUtilitiesBuiltInSets.W10_BEGINNER_TILES_SET_ID),
				strategy(CENTER_FOOD_STRATEGY_ID, "Center Food", "", BaUtilitiesData.WaveMap.WAVES_1_TO_9,
						BaUtilitiesBuiltInSets.CENTER_TRAP_FOOD_SET_ID,
						BaUtilitiesBuiltInSets.SHIR_MAINSTACK_TRAIL_SET_ID),
				strategy(NW_FOOD_STRATEGY_ID, "NW Food", "", BaUtilitiesData.WaveMap.WAVES_1_TO_9,
						BaUtilitiesBuiltInSets.NW_TRAP_FOOD_SET_ID,
						BaUtilitiesBuiltInSets.HENKE_MAINSTACK_TRAIL_SET_ID),
				strategy(NO_LOG_STRATEGY_ID, "No Log", "", BaUtilitiesData.WaveMap.WAVES_1_TO_9,
						BaUtilitiesBuiltInSets.N_TRAP_FOOD_SET_ID,
						BaUtilitiesBuiltInSets.SHIR_MAINSTACK_TRAIL_SET_ID),
				strategy(WALL_SPLIT_STRATEGY_ID, "Wall Split", WALL_SPLIT_NOTES, BaUtilitiesData.WaveMap.WAVES_1_TO_9,
						BaUtilitiesBuiltInSets.N_TRAP_FOOD_SET_ID,
						BaUtilitiesBuiltInSets.SHIR_MAINSTACK_TRAIL_SET_ID,
						BaUtilitiesBuiltInSets.WALL_SPLIT_MULTI_SET_ID),
				strategy(HENDI_TRIANGLE_STRATEGY_ID, "Hendi Triangle", "", BaUtilitiesData.WaveMap.WAVES_1_TO_9,
						BaUtilitiesBuiltInSets.N_TRAP_FOOD_SET_ID,
						BaUtilitiesBuiltInSets.SHIR_MAINSTACK_TRAIL_SET_ID,
						BaUtilitiesBuiltInSets.HENDI_TRIANGLE_SET_ID),
				strategy(TWO_ONE_FIVE_STAR_TWO_STRATEGY_ID, "2-1-5*-2", TWO_ONE_FIVE_NOTES, BaUtilitiesData.WaveMap.WAVES_1_TO_9,
						BaUtilitiesBuiltInSets.N_TRAP_FOOD_SET_ID,
						BaUtilitiesBuiltInSets.SHIR_MAINSTACK_TRAIL_SET_ID,
						BaUtilitiesBuiltInSets.TWO_ONE_FIVE_SOFT_CRASH_SET_ID),
				strategy(AUK_W10_STRATEGY_ID, "Auk W10", AUK_W10_NOTES, BaUtilitiesData.WaveMap.WAVE_10,
						BaUtilitiesBuiltInSets.AUK_W10_TILES_SET_ID)
		);
	}

	static List<BaUtilitiesData.AssignmentPreset> createAssignmentPresets()
	{
		List<BaUtilitiesData.AssignmentPreset> presets = new ArrayList<>();
		presets.add(new BaUtilitiesData.AssignmentPreset(
				"built-in:tile-marker-assignment-preset:defender-beginner",
				"Beginner",
				BaUtilitiesData.RoleContext.DEFENDER,
				beginnerWaves(),
				true
		));
		presets.add(new BaUtilitiesData.AssignmentPreset(
				"built-in:tile-marker-assignment-preset:defender-intermediate",
				"Intermediate",
				BaUtilitiesData.RoleContext.DEFENDER,
				intermediateWaves(),
				true
		));
		return presets;
	}

	private static BaUtilitiesData.StrategyPreset strategy(
			String id,
			String name,
			String notes,
			BaUtilitiesData.WaveMap waveMap,
			String... markerSetIds)
	{
		return new BaUtilitiesData.StrategyPreset(id, name, notes, waveMap, Arrays.asList(markerSetIds), true);
	}

	private static Map<Integer, String> beginnerWaves()
	{
		Map<Integer, String> waves = new HashMap<>();
		for (int wave = 1; wave <= 9; wave++)
		{
			waves.put(wave, BEGINNER_TILES_STRATEGY_ID);
		}
		waves.put(10, W10_BEGINNER_STRATEGY_ID);
		return waves;
	}

	private static Map<Integer, String> intermediateWaves()
	{
		Map<Integer, String> waves = new HashMap<>();
		waves.put(1, CENTER_FOOD_STRATEGY_ID);
		waves.put(2, NW_FOOD_STRATEGY_ID);
		waves.put(3, NW_FOOD_STRATEGY_ID);
		waves.put(4, NO_LOG_STRATEGY_ID);
		waves.put(5, NO_LOG_STRATEGY_ID);
		waves.put(6, WALL_SPLIT_STRATEGY_ID);
		waves.put(7, HENDI_TRIANGLE_STRATEGY_ID);
		waves.put(8, HENDI_TRIANGLE_STRATEGY_ID);
		waves.put(9, TWO_ONE_FIVE_STAR_TWO_STRATEGY_ID);
		waves.put(10, AUK_W10_STRATEGY_ID);
		return waves;
	}
}
