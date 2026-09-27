package com.batiles;

import com.google.common.base.Strings;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Moves BA Utilities' tile setup into BA Tiles.
 *
 * <p>{@link #plan} reads BA Utilities' saved setup (plus its built-in sets, strategies and defender lineups) and
 * resolves it the way BA Utilities does. {@link #apply} then merges the user's choices into BA Tiles:
 * <ul>
 * <li>every strategy becomes a BA Tiles preset for each chosen wave (of its arena map) and role, holding the tiles of
 * its sets, with their colors, labels, fill opacity and border width;</li>
 * <li>every assignment preset becomes a lineup (a GLOBAL one becomes a lineup for each role), and so does each role's
 * current wave selection when it is not one of its assignment presets;</li>
 * <li>strategies selected for GLOBAL (every role) become tiles that are not part of a preset, shown on that wave for
 * all roles, and shown alongside presets on that wave;</li>
 * <li>strategy notes are not imported.</li>
 * </ul>
 * Ids of everything created are derived from BA Utilities' ids, so importing again skips what is already there.
 */
final class BaUtilitiesImport
{
	static final String CURRENT_SETUP_NAME = "BA Utilities current setup";
	private static final String PRESET_ID_PREFIX = "bau:";
	private static final String LINEUP_ID_PREFIX = "bau-lineup:";
	private static final String CURRENT_LINEUP_ID_PREFIX = "bau-current:";
	private static final Color DEFAULT_MARKER_COLOR = new Color(80, 170, 255);
	private static final List<String> ROLE_CODES = BATilesPlugin.ALL_ROLES;

	private BaUtilitiesImport()
	{
	}

	/**
	 * A BA Utilities strategy, with the tiles of its sets.
	 */
	@Value
	static class Strategy
	{
		String id;
		String name;
		BaUtilitiesData.WaveMap waveMap;
		boolean builtIn;
		boolean hasNotes;
		List<BaUtilitiesData.Marker> markers;

		/**
		 * @return the waves the strategy can be used on: 1-9, or 10
		 */
		List<Integer> possibleWaves()
		{
			return waveMap == BaUtilitiesData.WaveMap.WAVE_10 ? List.of(10) : List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
		}

		/**
		 * @return the roles to create presets for unless the user chooses otherwise: Defender for built-ins, else all
		 */
		List<String> defaultRoles()
		{
			return builtIn ? List.of(BARole.DEFENDER.getCode()) : ROLE_CODES;
		}
	}

	/**
	 * A lineup to create: a BA Utilities assignment preset (or current wave selection) for one role.
	 */
	@Value
	static class LineupSource
	{
		/**
		 * The id of the BA Tiles lineup to create.
		 */
		String lineupId;
		String name;
		String role;
		/**
		 * Wave to BA Utilities strategy id.
		 */
		Map<Integer, String> strategyIds;
		/**
		 * Whether this was the role's active setup in BA Utilities.
		 */
		boolean active;
		/**
		 * Whether this came from a GLOBAL assignment preset.
		 */
		boolean fromGlobal;
	}

	/**
	 * What BA Utilities has, resolved.
	 */
	@Value
	static class Plan
	{
		List<Strategy> strategies;
		List<LineupSource> lineups;
		/**
		 * Wave to strategy id, for GLOBAL's current wave selections.
		 */
		Map<Integer, String> globalSelections;

		Strategy strategy(String id)
		{
			return strategies.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
		}

		boolean isEmpty()
		{
			return strategies.isEmpty() && lineups.isEmpty() && globalSelections.isEmpty();
		}
	}

	/**
	 * The user's choices in the import confirmation.
	 */
	@Value
	static class Choices
	{
		/**
		 * Strategy id to the name its presets get.
		 */
		Map<String, String> presetNames;
		/**
		 * Strategy id to the waves to create its presets for (strategies without an entry are not imported).
		 */
		Map<String, Set<Integer>> waves;
		/**
		 * Strategy id to the role codes to create its presets for.
		 */
		Map<String, Set<String>> roles;
		/**
		 * Lineup id to the name it gets; lineups without an entry are not imported.
		 */
		Map<String, String> lineupNames;
		/**
		 * Whether to switch each role to the lineup that was its active setup in BA Utilities.
		 */
		boolean activateCurrentSetup;

		/**
		 * The default choices: every strategy on all its waves (built-ins for Defender, others for all roles), every
		 * lineup, names as in BA Utilities, and switching to BA Utilities' current setup.
		 */
		static Choices defaults(Plan plan)
		{
			Map<String, String> names = new LinkedHashMap<>();
			Map<String, Set<Integer>> waves = new LinkedHashMap<>();
			Map<String, Set<String>> roles = new LinkedHashMap<>();
			for (Strategy strategy : plan.getStrategies())
			{
				names.put(strategy.getId(), strategy.getName());
				waves.put(strategy.getId(), new TreeSet<>(strategy.possibleWaves()));
				roles.put(strategy.getId(), new TreeSet<>(strategy.defaultRoles()));
			}
			Map<String, String> lineupNames = new LinkedHashMap<>();
			for (LineupSource lineup : plan.getLineups())
			{
				lineupNames.put(lineup.getLineupId(), lineup.getName());
			}
			Choices choices = new Choices(names, waves, roles, lineupNames, true);
			return choices.withRequirements(plan);
		}

		/**
		 * @return these choices with every wave and role that an imported lineup needs a preset for turned on
		 */
		Choices withRequirements(Plan plan)
		{
			Map<String, Set<Integer>> newWaves = copy(waves);
			Map<String, Set<String>> newRoles = copy(roles);
			for (Map.Entry<String, Set<Integer>> required : requiredWaves(plan, this).entrySet())
			{
				newWaves.computeIfAbsent(required.getKey(), k -> new TreeSet<>()).addAll(required.getValue());
			}
			for (Map.Entry<String, Set<String>> required : requiredRoles(plan, this).entrySet())
			{
				newRoles.computeIfAbsent(required.getKey(), k -> new TreeSet<>()).addAll(required.getValue());
			}
			return new Choices(presetNames, newWaves, newRoles, lineupNames, activateCurrentSetup);
		}

		boolean includes(String strategyId)
		{
			return !waves.getOrDefault(strategyId, Set.of()).isEmpty() && !roles.getOrDefault(strategyId, Set.of()).isEmpty();
		}

		private static <T> Map<String, Set<T>> copy(Map<String, Set<T>> map)
		{
			Map<String, Set<T>> copy = new LinkedHashMap<>();
			map.forEach((k, v) -> copy.put(k, new TreeSet<>(v)));
			return copy;
		}
	}

	// ---- planning ----

	/**
	 * @param store BA Utilities' saved setup, or null if it has none
	 */
	static Plan plan(@Nullable BaUtilitiesData.Store store)
	{
		Resolver resolver = new Resolver(store);

		List<Strategy> strategies = new ArrayList<>();
		for (BaUtilitiesData.StrategyPreset preset : resolver.allStrategies())
		{
			BaUtilitiesData.WaveMap waveMap = preset.getWaveMapOrNull();
			strategies.add(new Strategy(preset.getId(), Strings.isNullOrEmpty(preset.getName()) ? "Unnamed strategy" : preset.getName().trim(),
					waveMap, resolver.isBuiltInStrategy(preset.getId()), !Strings.isNullOrEmpty(preset.getNotes()) && !preset.getNotes().isBlank(),
					resolver.markers(preset)));
		}

		// assignment presets become lineups; GLOBAL ones become one per role
		List<LineupSource> lineups = new ArrayList<>();
		Map<String, String> activeAssignments = store == null || store.getActiveAssignmentPresetIds() == null
				? Map.of() : store.getActiveAssignmentPresetIds();
		for (BaUtilitiesData.AssignmentPreset preset : resolver.allAssignmentPresets())
		{
			BaUtilitiesData.RoleContext context = preset.getRoleContextOrDefault();
			Map<Integer, String> selections = resolver.validSelections(preset.getWaveSelections());
			String name = Strings.isNullOrEmpty(preset.getName()) ? "Unnamed lineup" : preset.getName().trim();
			if (context == BaUtilitiesData.RoleContext.GLOBAL)
			{
				for (String role : ROLE_CODES)
				{
					lineups.add(new LineupSource(LINEUP_ID_PREFIX + preset.getId() + ":" + role, name, role, selections, false, true));
				}
			}
			else
			{
				boolean active = preset.getId().equals(activeAssignments.get(context.name()));
				lineups.add(new LineupSource(LINEUP_ID_PREFIX + preset.getId() + ":" + context.roleCode(), name, context.roleCode(),
						selections, active, false));
			}
		}

		// a role's current wave selections, when they are not one of its assignment presets
		Map<Integer, String> globalSelections = new TreeMap<>();
		for (BaUtilitiesData.RoleContext context : BaUtilitiesData.RoleContext.values())
		{
			Map<Integer, String> current = resolver.currentSelections(context);
			if (context == BaUtilitiesData.RoleContext.GLOBAL)
			{
				globalSelections.putAll(current);
				continue;
			}
			String role = context.roleCode();
			boolean hasActiveAssignment = lineups.stream().anyMatch(l -> l.getRole().equals(role) && l.isActive());
			if (!hasActiveAssignment && !current.isEmpty())
			{
				lineups.add(new LineupSource(CURRENT_LINEUP_ID_PREFIX + role, CURRENT_SETUP_NAME, role, current, true, false));
			}
		}

		return new Plan(strategies, lineups, globalSelections);
	}

	/**
	 * Waves each strategy needs a preset on, for the lineups being imported.
	 */
	static Map<String, Set<Integer>> requiredWaves(Plan plan, Choices choices)
	{
		Map<String, Set<Integer>> required = new HashMap<>();
		for (LineupSource lineup : plan.getLineups())
		{
			if (choices.getLineupNames().containsKey(lineup.getLineupId()))
			{
				lineup.getStrategyIds().forEach((wave, strategyId) -> required.computeIfAbsent(strategyId, k -> new TreeSet<>()).add(wave));
			}
		}
		return required;
	}

	/**
	 * Roles each strategy needs a preset for, for the lineups being imported.
	 */
	static Map<String, Set<String>> requiredRoles(Plan plan, Choices choices)
	{
		Map<String, Set<String>> required = new HashMap<>();
		for (LineupSource lineup : plan.getLineups())
		{
			if (choices.getLineupNames().containsKey(lineup.getLineupId()))
			{
				for (String strategyId : lineup.getStrategyIds().values())
				{
					required.computeIfAbsent(strategyId, k -> new TreeSet<>()).add(lineup.getRole());
				}
			}
		}
		return required;
	}

	// ---- collisions ----

	static String presetId(String strategyId, int wave, String role)
	{
		return PRESET_ID_PREFIX + strategyId + ":" + wave + ":" + role;
	}

	/**
	 * @return strategy id to a description of where its preset name is already used (on a wave and role it would be
	 *         created for), for every imported strategy whose name must be changed
	 */
	static Map<String, String> presetNameCollisions(Plan plan, Choices choices, Collection<StrategyPreset> existing)
	{
		Map<String, String> collisions = new LinkedHashMap<>();
		// wave + role -> names taken there, by presets that are not this import's own
		Map<String, Map<String, String>> taken = new HashMap<>();
		for (StrategyPreset preset : existing)
		{
			if (!preset.getId().startsWith(PRESET_ID_PREFIX))
			{
				taken.computeIfAbsent(preset.getWave() + preset.getRole(), k -> new HashMap<>())
						.put(preset.getName().toLowerCase(), "an existing preset");
			}
		}

		for (Strategy strategy : plan.getStrategies())
		{
			if (!choices.includes(strategy.getId()))
			{
				continue;
			}
			String name = choices.getPresetNames().getOrDefault(strategy.getId(), strategy.getName()).trim();
			for (int wave : choices.getWaves().get(strategy.getId()))
			{
				for (String role : choices.getRoles().get(strategy.getId()))
				{
					String combo = wave + role;
					String owner = taken.computeIfAbsent(combo, k -> new HashMap<>()).get(name.toLowerCase());
					if (name.isEmpty())
					{
						collisions.putIfAbsent(strategy.getId(), "the name is empty");
					}
					else if (owner != null && !owner.equals(strategy.getId()))
					{
						String by = owner.equals("an existing preset") ? owner : "another imported strategy";
						collisions.putIfAbsent(strategy.getId(), "\"" + name + "\" is already used by " + by + " on wave " + wave
								+ " " + BARole.fromCode(role).getDisplayName());
					}
					else
					{
						taken.get(combo).put(name.toLowerCase(), strategy.getId());
					}
				}
			}
		}
		return collisions;
	}

	/**
	 * @return lineup id to a description of the collision, for every imported lineup whose name must be changed
	 */
	static Map<String, String> lineupNameCollisions(Plan plan, Choices choices, Collection<Lineup> existing)
	{
		Map<String, String> collisions = new LinkedHashMap<>();
		Map<String, Map<String, String>> taken = new HashMap<>();
		for (Lineup lineup : existing)
		{
			if (!lineup.getId().startsWith(LINEUP_ID_PREFIX) && !lineup.getId().startsWith(CURRENT_LINEUP_ID_PREFIX))
			{
				taken.computeIfAbsent(lineup.getRole(), k -> new HashMap<>()).put(lineup.getName().toLowerCase(), "an existing lineup");
			}
		}

		for (LineupSource lineup : plan.getLineups())
		{
			String name = choices.getLineupNames().get(lineup.getLineupId());
			if (name == null)
			{
				continue;
			}
			name = name.trim();
			String owner = taken.computeIfAbsent(lineup.getRole(), k -> new HashMap<>()).get(name.toLowerCase());
			if (name.isEmpty())
			{
				collisions.put(lineup.getLineupId(), "the name is empty");
			}
			else if (owner != null)
			{
				String by = owner.equals("an existing lineup") ? owner : "another imported lineup";
				collisions.put(lineup.getLineupId(), "\"" + name + "\" is already used by " + by + " for "
						+ BARole.fromCode(lineup.getRole()).getDisplayName());
			}
			else
			{
				taken.get(lineup.getRole()).put(name.toLowerCase(), lineup.getLineupId());
			}
		}
		return collisions;
	}

	// ---- applying ----

	@Value
	static class Result
	{
		int presetsCreated;
		int presetsAlreadyImported;
		int presetTiles;
		int lineupsCreated;
		int lineupsAlreadyImported;
		int globalTiles;
		int lineupsActivated;
	}

	/**
	 * Merges the chosen parts of the plan into BA Tiles. Nothing already in BA Tiles is removed or changed, except
	 * that each role's active presets switch to its BA Utilities setup when {@link Choices#isActivateCurrentSetup()},
	 * and GLOBAL's waves show tiles that are not part of a preset alongside presets.
	 */
	static Result apply(BATilesStore store, Plan plan, Choices choices)
	{
		Choices resolved = choices.withRequirements(plan);
		Set<String> existingPresetIds = store.getPresets().stream().map(StrategyPreset::getId).collect(Collectors.toSet());

		// presets and their tiles
		List<StrategyPreset> newPresets = new ArrayList<>();
		Map<Integer, List<GroundMarkerPoint>> newTiles = new TreeMap<>();
		int alreadyImported = 0;
		int presetTiles = 0;
		for (Strategy strategy : plan.getStrategies())
		{
			if (!resolved.includes(strategy.getId()))
			{
				continue;
			}
			String name = resolved.getPresetNames().getOrDefault(strategy.getId(), strategy.getName()).trim();
			for (int wave : resolved.getWaves().get(strategy.getId()))
			{
				if (!strategy.possibleWaves().contains(wave))
				{
					continue;
				}
				for (String role : resolved.getRoles().get(strategy.getId()))
				{
					String id = presetId(strategy.getId(), wave, role);
					if (existingPresetIds.contains(id))
					{
						alreadyImported++;
						continue;
					}
					newPresets.add(new StrategyPreset(id, name, wave, role));
					for (BaUtilitiesData.Marker marker : strategy.getMarkers())
					{
						GroundMarkerPoint tile = toTile(marker, List.of(wave), List.of(role), id);
						newTiles.computeIfAbsent(tile.getRegionId(), k -> new ArrayList<>()).add(tile);
						presetTiles++;
					}
				}
			}
		}

		// GLOBAL's current selections: tiles that are not part of a preset, for all roles on that wave
		int globalTiles = 0;
		for (Map.Entry<Integer, String> selection : plan.getGlobalSelections().entrySet())
		{
			Strategy strategy = plan.strategy(selection.getValue());
			if (strategy == null)
			{
				continue;
			}
			for (BaUtilitiesData.Marker marker : strategy.getMarkers())
			{
				GroundMarkerPoint tile = toTile(marker, List.of(selection.getKey()), ROLE_CODES, null);
				List<GroundMarkerPoint> region = newTiles.computeIfAbsent(tile.getRegionId(), k -> new ArrayList<>());
				if (!store.getPoints(tile.getRegionId()).contains(tile) && !region.contains(tile))
				{
					region.add(tile);
					globalTiles++;
				}
			}
		}

		store.addPresetsIfAbsent(newPresets);
		for (Map.Entry<Integer, List<GroundMarkerPoint>> region : newTiles.entrySet())
		{
			if (!region.getValue().isEmpty())
			{
				List<GroundMarkerPoint> points = new ArrayList<>(store.getPoints(region.getKey()));
				points.addAll(region.getValue());
				store.savePoints(region.getKey(), points);
			}
		}
		for (int wave : plan.getGlobalSelections().keySet())
		{
			for (String role : ROLE_CODES)
			{
				store.setShowBaseWithPreset(wave, role, true);
			}
		}

		// lineups
		Set<String> existingLineupIds = store.getLineups().stream().map(Lineup::getId).collect(Collectors.toSet());
		int lineupsCreated = 0;
		int lineupsAlreadyImported = 0;
		List<Lineup> toActivate = new ArrayList<>();
		for (LineupSource source : plan.getLineups())
		{
			String name = resolved.getLineupNames().get(source.getLineupId());
			if (name == null)
			{
				continue;
			}
			Lineup lineup = new Lineup(source.getLineupId(), name.trim(), source.getRole(), presetIds(source));
			if (existingLineupIds.contains(lineup.getId()))
			{
				lineupsAlreadyImported++;
				lineup = store.getLineup(lineup.getId()).orElse(lineup);
			}
			else
			{
				store.addLineupIfAbsent(lineup);
				lineupsCreated++;
			}
			if (source.isActive())
			{
				toActivate.add(lineup);
			}
		}

		int activated = 0;
		if (resolved.isActivateCurrentSetup())
		{
			for (Lineup lineup : toActivate)
			{
				store.applyLineup(lineup);
				activated++;
			}
		}

		return new Result(newPresets.size(), alreadyImported, presetTiles, lineupsCreated, lineupsAlreadyImported, globalTiles, activated);
	}

	private static Map<Integer, String> presetIds(LineupSource source)
	{
		Map<Integer, String> ids = new TreeMap<>();
		source.getStrategyIds().forEach((wave, strategyId) -> ids.put(wave, presetId(strategyId, wave, source.getRole())));
		return ids;
	}

	static GroundMarkerPoint toTile(BaUtilitiesData.Marker marker, List<Integer> waves, List<String> roles, @Nullable String presetId)
	{
		BaUtilitiesData.Tile tile = marker.getTile();
		String label = marker.getLabel() == null || marker.getLabel().isBlank() ? null : marker.getLabel().trim();
		return new GroundMarkerPoint(tile.getRegionId(), tile.getRegionX(), tile.getRegionY(), tile.getZ(), parseColor(marker.getColor()),
				label, waves, roles, presetId,
				Math.max(TileStyle.MIN_FILL_OPACITY_PERCENT, Math.min(TileStyle.MAX_FILL_OPACITY_PERCENT, marker.getOpacityPercentOrDefault())),
				TileStyle.borderWidth(marker.getBorderWidthOrDefault(), 0));
	}

	/**
	 * Parses a color the way BA Utilities does ("#rrggbb"), falling back to its default marker color.
	 */
	static Color parseColor(@Nullable String color)
	{
		try
		{
			return color == null || color.isBlank() ? DEFAULT_MARKER_COLOR : Color.decode(color);
		}
		catch (RuntimeException ex)
		{
			return DEFAULT_MARKER_COLOR;
		}
	}

	/**
	 * Looks things up the way BA Utilities does: built-ins win over saved copies with the same id, a strategy only
	 * uses sets of its own arena map, and a wave selection only counts if its strategy fits that wave's map.
	 */
	private static final class Resolver
	{
		private final Map<String, BaUtilitiesData.MarkerSet> builtInSets = byId(BaUtilitiesBuiltInSets.create(), BaUtilitiesData.MarkerSet::getId);
		private final Map<String, BaUtilitiesData.StrategyPreset> builtInStrategies = byId(BaUtilitiesBuiltInStrategies.createStrategyPresets(), BaUtilitiesData.StrategyPreset::getId);
		private final Map<String, BaUtilitiesData.AssignmentPreset> builtInAssignments = byId(BaUtilitiesBuiltInStrategies.createAssignmentPresets(), BaUtilitiesData.AssignmentPreset::getId);
		private final Map<String, BaUtilitiesData.MarkerSet> userSets;
		private final Map<String, BaUtilitiesData.StrategyPreset> userStrategies;
		private final Map<String, BaUtilitiesData.AssignmentPreset> userAssignments;
		private final List<BaUtilitiesData.WaveSelection> waveSelections;

		Resolver(@Nullable BaUtilitiesData.Store store)
		{
			userSets = byId(store == null ? null : store.getMarkerSets(), BaUtilitiesData.MarkerSet::getId);
			userStrategies = byId(store == null ? null : store.getStrategyPresets(), BaUtilitiesData.StrategyPreset::getId);
			userAssignments = byId(store == null ? null : store.getAssignmentPresets(), BaUtilitiesData.AssignmentPreset::getId);
			waveSelections = store == null || store.getWaveSelections() == null ? List.of() : store.getWaveSelections();
		}

		boolean isBuiltInStrategy(String id)
		{
			return builtInStrategies.containsKey(id);
		}

		/**
		 * @return built-in strategies, then the user's own (saved copies of built-ins are ignored), with a known map
		 */
		List<BaUtilitiesData.StrategyPreset> allStrategies()
		{
			List<BaUtilitiesData.StrategyPreset> all = new ArrayList<>(builtInStrategies.values());
			userStrategies.values().stream().filter(s -> !builtInStrategies.containsKey(s.getId())).forEach(all::add);
			all.removeIf(s -> s.getWaveMapOrNull() == null);
			return all;
		}

		List<BaUtilitiesData.AssignmentPreset> allAssignmentPresets()
		{
			List<BaUtilitiesData.AssignmentPreset> all = new ArrayList<>(builtInAssignments.values());
			userAssignments.values().stream().filter(a -> !builtInAssignments.containsKey(a.getId())).forEach(all::add);
			return all;
		}

		BaUtilitiesData.StrategyPreset strategy(String id)
		{
			BaUtilitiesData.StrategyPreset builtIn = builtInStrategies.get(id);
			BaUtilitiesData.StrategyPreset preset = builtIn != null ? builtIn : userStrategies.get(id);
			return preset == null || preset.getWaveMapOrNull() == null ? null : preset;
		}

		private BaUtilitiesData.MarkerSet set(String id)
		{
			BaUtilitiesData.MarkerSet builtIn = builtInSets.get(id);
			return builtIn != null ? builtIn : userSets.get(id);
		}

		/**
		 * @return the tiles of the strategy's sets that are on its map, each set once, in order
		 */
		List<BaUtilitiesData.Marker> markers(BaUtilitiesData.StrategyPreset strategy)
		{
			List<BaUtilitiesData.Marker> markers = new ArrayList<>();
			Set<String> seen = new LinkedHashSet<>();
			for (String setId : strategy.getMarkerSetIds() == null ? List.<String>of() : strategy.getMarkerSetIds())
			{
				BaUtilitiesData.MarkerSet set = setId == null ? null : set(setId);
				if (set == null || set.getWaveMapOrNull() != strategy.getWaveMapOrNull() || !seen.add(setId) || set.getMarkers() == null)
				{
					continue;
				}
				for (BaUtilitiesData.Marker marker : set.getMarkers())
				{
					if (marker != null && marker.getTile() != null)
					{
						markers.add(marker);
					}
				}
			}
			return markers;
		}

		/**
		 * @return the selections whose wave is 1-10 and whose strategy exists and fits that wave's map
		 */
		Map<Integer, String> validSelections(@Nullable Map<Integer, String> selections)
		{
			Map<Integer, String> valid = new TreeMap<>();
			if (selections != null)
			{
				selections.forEach((wave, strategyId) ->
				{
					if (wave != null && wave >= 1 && wave <= 10 && strategyId != null)
					{
						BaUtilitiesData.StrategyPreset strategy = strategy(strategyId);
						if (strategy != null && strategy.getWaveMapOrNull() == BaUtilitiesData.WaveMap.fromWave(wave))
						{
							valid.put(wave, strategyId);
						}
					}
				});
			}
			return valid;
		}

		/**
		 * @return the context's current wave selections (the first one per wave, as BA Utilities finds it), validated
		 */
		Map<Integer, String> currentSelections(BaUtilitiesData.RoleContext context)
		{
			Map<Integer, String> current = new TreeMap<>();
			for (BaUtilitiesData.WaveSelection selection : waveSelections)
			{
				if (selection != null && selection.getRoleContextOrDefault() == context && !Strings.isNullOrEmpty(selection.getStrategyId()))
				{
					current.putIfAbsent(selection.getWave(), selection.getStrategyId());
				}
			}
			return validSelections(current);
		}

		private static <T> Map<String, T> byId(@Nullable List<T> items, java.util.function.Function<T, String> id)
		{
			Map<String, T> map = new LinkedHashMap<>();
			for (T item : items == null ? List.<T>of() : items)
			{
				if (item != null && !Strings.isNullOrEmpty(id.apply(item)))
				{
					// the first one with an id wins, as BA Utilities' lookups do
					map.putIfAbsent(id.apply(item), item);
				}
			}
			return map;
		}
	}
}
