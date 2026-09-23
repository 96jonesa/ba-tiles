package com.batiles;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * Decides which BA Tiles are shown, given the waves and roles being displayed and the active strategy presets.
 */
@Value
class TileVisibility
{
	/**
	 * Waves whose tiles are displayed (e.g. the current wave, plus any waves toggled on in the config).
	 */
	Collection<Integer> waves;
	/**
	 * Role codes whose tiles are displayed.
	 */
	Collection<String> roles;
	/**
	 * Maps (wave, role code) to the id of the active preset for that combination, or null if none is active.
	 */
	BiFunction<Integer, String, String> activePreset;
	/**
	 * Maps a preset id to its preset, or null for an unknown (e.g. deleted) preset.
	 */
	Function<String, StrategyPreset> presetLookup;
	/**
	 * Whether tiles that are not part of a preset are shown for a (wave, role code) that has an active preset.
	 * They are always shown for a wave / role without one.
	 */
	BiPredicate<Integer, String> showBaseTilesWithPreset;

	/**
	 * @return the displayed values the point's values match; a null value list matches every displayed value, or a
	 *         single null (meaning "any") when nothing is displayed
	 */
	private static <T> Collection<T> candidates(Collection<T> pointValues, Collection<T> displayed)
	{
		if (pointValues == null)
		{
			return displayed.isEmpty() ? Collections.singletonList(null) : displayed;
		}
		return displayed.stream().filter(pointValues::contains).collect(Collectors.toList());
	}

	boolean isVisible(GroundMarkerPoint point)
	{
		if (point.isPresetTile())
		{
			StrategyPreset preset = presetLookup.apply(point.getPresetId());
			return preset != null
					&& waves.contains(preset.getWave())
					&& roles.contains(preset.getRole())
					&& preset.getId().equals(activePreset.apply(preset.getWave(), preset.getRole()));
		}

		// null waves / roles (e.g. from older or hand-written imports) match regardless of what is displayed;
		// with nothing of that dimension displayed there is no wave / role, and so no active preset, to consider
		for (Integer wave : candidates(point.getWaves(), waves))
		{
			for (String role : candidates(point.getRoles(), roles))
			{
				boolean presetActive = wave != null && role != null && activePreset.apply(wave, role) != null;
				if (!presetActive || showBaseTilesWithPreset.test(wave, role))
				{
					return true;
				}
			}
		}

		return false;
	}
}
