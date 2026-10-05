package com.batiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class BaUtilitiesImportTest
{
	private static final String WALL_SPLIT = "built-in:tile-marker-strategy:wall-split";
	private static final String BEGINNER_LINEUP = "bau-lineup:built-in:tile-marker-assignment-preset:defender-beginner:d";

	/**
	 * A saved BA Utilities setup, in the shape its Gson writes:
	 * a user set "Mine" (1-9) and a wave 10 set, a user strategy "My Stack" using "Mine" plus the built-in N Trap set
	 * (and, ignored, the wave 10 set), a saved copy of the built-in Wall Split (ignored), healer and GLOBAL current
	 * selections, a healer lineup that is active, and a defender wave 3 selection without a lineup.
	 */
	static final String STORE_JSON = "{"
			+ "\"markerSets\":["
			+ "{\"id\":\"set-mine\",\"name\":\"Mine\",\"mapMode\":\"FULL_MAP\",\"waveMap\":\"WAVES_1_TO_9\",\"builtIn\":false,\"markers\":["
			+ "{\"id\":\"m1\",\"tile\":{\"regionId\":7509,\"regionX\":40,\"regionY\":30,\"z\":0},\"name\":\"Stack\",\"label\":\"S\",\"color\":\"#ff0000\",\"opacityPercent\":50,\"borderWidth\":3.0},"
			+ "{\"id\":\"m2\",\"tile\":{\"regionId\":7509,\"regionX\":41,\"regionY\":30,\"z\":0},\"name\":\"Plain\",\"label\":\"\",\"color\":\"#00ff00\"}]},"
			+ "{\"id\":\"set-w10\",\"name\":\"W10\",\"waveMap\":\"WAVE_10\",\"markers\":["
			+ "{\"id\":\"m3\",\"tile\":{\"regionId\":7508,\"regionX\":40,\"regionY\":30,\"z\":0},\"label\":\"q\",\"color\":\"#0000ff\"}]}],"
			+ "\"strategyPresets\":["
			+ "{\"id\":\"strat-stack\",\"name\":\"My Stack\",\"notes\":\"12.0 - do things\",\"waveMap\":\"WAVES_1_TO_9\",\"builtIn\":false,"
			+ "\"markerSetIds\":[\"set-mine\",\"built-in:tile-marker-set:n-trap-food\",\"set-w10\",\"set-mine\"]},"
			+ "{\"id\":\"" + WALL_SPLIT + "\",\"name\":\"My edited Wall Split\",\"waveMap\":\"WAVES_1_TO_9\",\"builtIn\":true,\"markerSetIds\":[\"set-mine\"]},"
			+ "{\"id\":\"strat-w10\",\"name\":\"My W10\",\"waveMap\":\"WAVE_10\",\"markerSetIds\":[\"set-w10\"]}],"
			+ "\"waveSelections\":["
			+ "{\"id\":\"s1\",\"roleContext\":\"HEALER\",\"wave\":2,\"strategyId\":\"strat-stack\"},"
			+ "{\"id\":\"s2\",\"roleContext\":\"GLOBAL\",\"wave\":4,\"strategyId\":\"strat-stack\"},"
			+ "{\"id\":\"s3\",\"roleContext\":\"DEFENDER\",\"wave\":3,\"strategyId\":\"" + WALL_SPLIT + "\"},"
			+ "{\"id\":\"s4\",\"roleContext\":\"DEFENDER\",\"wave\":10,\"strategyId\":\"strat-stack\"}],"
			+ "\"assignmentPresets\":["
			+ "{\"id\":\"lineup-heal\",\"name\":\"Heal setup\",\"roleContext\":\"HEALER\",\"builtIn\":false,\"waveSelections\":{\"2\":\"strat-stack\"}},"
			+ "{\"id\":\"lineup-all\",\"name\":\"Everyone\",\"roleContext\":\"GLOBAL\",\"waveSelections\":{\"5\":\"strat-stack\",\"10\":\"strat-w10\"}}],"
			+ "\"activeAssignmentPresetIds\":{\"HEALER\":\"lineup-heal\"}"
			+ "}";

	static BaUtilitiesData.Store store()
	{
		return RuneLiteAPI.GSON.fromJson(STORE_JSON, BaUtilitiesData.Store.class);
	}

	static BATilesStore newStore()
	{
		return new BATilesStore(new BATilesStoreTest.MemoryConfig(), RuneLiteAPI.GSON);
	}

	public static class Plan
	{
		@Test
		public void withoutASavedSetupHasTheBuiltIns()
		{
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(null);
			assertEquals(9, plan.getStrategies().size());
			assertTrue(plan.getStrategies().stream().allMatch(BaUtilitiesImport.Strategy::isBuiltIn));
			assertEquals(List.of("Beginner", "Intermediate"),
					plan.getLineups().stream().map(BaUtilitiesImport.LineupSource::getName).collect(Collectors.toList()));
			assertTrue(plan.getLineups().stream().allMatch(l -> l.getRole().equals("d") && !l.isActive()));
			assertTrue(plan.getGlobalSelections().isEmpty());
		}

		@Test
		public void userStrategyGetsTheTilesOfItsSetsOnItsMapEachSetOnce()
		{
			BaUtilitiesImport.Strategy stack = BaUtilitiesImport.plan(store()).strategy("strat-stack");
			// "Mine" (2 tiles) and the built-in N Trap (1 tile); the wave 10 set and the repeated "Mine" are skipped
			assertEquals(List.of("m1", "m2", "built-in:marker:food:n-trap"),
					stack.getMarkers().stream().map(BaUtilitiesData.Marker::getId).collect(Collectors.toList()));
			assertTrue(stack.isHasNotes());
			assertFalse(stack.isBuiltIn());
		}

		@Test
		public void builtInsWinOverSavedCopies()
		{
			BaUtilitiesImport.Strategy wallSplit = BaUtilitiesImport.plan(store()).strategy(WALL_SPLIT);
			assertEquals("Wall Split", wallSplit.getName());
			assertTrue(wallSplit.isBuiltIn());
			// the built-in's sets (N Trap 1 + Shir 2 + E Multi 1), not the saved copy's "Mine"
			assertEquals(4, wallSplit.getMarkers().size());
		}

		@Test
		public void globalSelectionsAreKeptApart()
		{
			assertEquals(Map.of(4, "strat-stack"), BaUtilitiesImport.plan(store()).getGlobalSelections());
		}

		@Test
		public void activeLineupIsMarkedAndCurrentSetupOnlyWithoutOne()
		{
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			Map<String, BaUtilitiesImport.LineupSource> byId = plan.getLineups().stream()
					.collect(Collectors.toMap(BaUtilitiesImport.LineupSource::getLineupId, l -> l));
			assertTrue(byId.get("bau-lineup:lineup-heal:h").isActive());
			// the healer's current selection is its active lineup, so no separate current setup for healer
			assertFalse(byId.containsKey("bau-current:h"));
			// the defender's wave 3 selection has no lineup; wave 10 with a waves 1-9 strategy is dropped
			assertEquals(Map.of(3, WALL_SPLIT), byId.get("bau-current:d").getStrategyIds());
			assertTrue(byId.get("bau-current:d").isActive());
		}

		@Test
		public void globalLineupBecomesOnePerRole()
		{
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			List<BaUtilitiesImport.LineupSource> everyone = plan.getLineups().stream()
					.filter(l -> l.getName().equals("Everyone")).collect(Collectors.toList());
			assertEquals(List.of("a", "c", "d", "h"), everyone.stream().map(BaUtilitiesImport.LineupSource::getRole).collect(Collectors.toList()));
			assertEquals(Map.of(5, "strat-stack", 10, "strat-w10"), everyone.get(0).getStrategyIds());
		}
	}

	public static class Defaults
	{
		@Test
		public void builtInsAreForDefenderAndOthersForEveryRoleOnTheirMapsWaves()
		{
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.Choices choices = BaUtilitiesImport.Choices.defaults(plan);
			assertEquals(Set.of("d"), choices.getRoles().get("built-in:tile-marker-strategy:hendi-triangle"));
			assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9), choices.getWaves().get("built-in:tile-marker-strategy:hendi-triangle"));
			assertEquals(Set.of(10), choices.getWaves().get("built-in:tile-marker-strategy:auk-w10"));
			assertEquals(Set.of("a", "c", "d", "h"), choices.getRoles().get("strat-stack"));
			assertTrue(choices.isActivateCurrentSetup());
		}

		@Test
		public void importedLineupsForceTheWavesAndRolesTheyUse()
		{
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.Choices defaults = BaUtilitiesImport.Choices.defaults(plan);
			Map<String, Set<Integer>> waves = new LinkedHashMap<>(defaults.getWaves());
			Map<String, Set<String>> roles = new LinkedHashMap<>(defaults.getRoles());
			waves.put("strat-stack", new TreeSet<>(Set.of(1)));
			roles.put("strat-stack", new TreeSet<>(Set.of("a")));
			BaUtilitiesImport.Choices narrowed = new BaUtilitiesImport.Choices(defaults.getPresetNames(), waves, roles,
					defaults.getLineupNames(), true).withRequirements(plan);
			// the healer lineup (wave 2) and the "Everyone" lineups (wave 5, every role) need these back
			assertEquals(Set.of(1, 2, 5), narrowed.getWaves().get("strat-stack"));
			assertEquals(Set.of("a", "c", "d", "h"), narrowed.getRoles().get("strat-stack"));
		}
	}

	public static class PresetNameCollisions
	{
		@Test
		public void existingPresetWithTheNameOnATargetWaveAndRoleCollides()
		{
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.Choices choices = BaUtilitiesImport.Choices.defaults(plan);
			Map<String, String> collisions = BaUtilitiesImport.presetNameCollisions(plan, choices,
					List.of(new StrategyPreset("mine", "wall split", 7, "d"), new StrategyPreset("other", "My Stack", 10, "d")));
			assertEquals(Set.of(WALL_SPLIT), collisions.keySet());
			assertEquals("\"Wall Split\" is already used by an existing preset on wave 7 Defender", collisions.get(WALL_SPLIT));
		}

		@Test
		public void twoImportedStrategiesWithTheSameNameOnAWaveAndRoleCollide()
		{
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.Choices defaults = BaUtilitiesImport.Choices.defaults(plan);
			Map<String, String> names = new LinkedHashMap<>(defaults.getPresetNames());
			names.put("strat-stack", "Wall Split");
			BaUtilitiesImport.Choices choices = new BaUtilitiesImport.Choices(names, defaults.getWaves(), defaults.getRoles(),
					defaults.getLineupNames(), true);
			assertTrue(BaUtilitiesImport.presetNameCollisions(plan, choices, List.of()).containsKey("strat-stack"));
		}

		@Test
		public void aRenamedPresetFromAnEarlierImportStillCollides()
		{
			BATilesStore store = newStore();
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			store.addPresetsIfAbsent(List.of(new StrategyPreset(BaUtilitiesImport.presetId("strat-stack", 1, "d"), "Foo", 1, "d")));
			BaUtilitiesImport.Choices defaults = BaUtilitiesImport.Choices.defaults(plan);
			Map<String, String> names = new LinkedHashMap<>(defaults.getPresetNames());
			names.put(WALL_SPLIT, "Foo");
			BaUtilitiesImport.Choices choices = new BaUtilitiesImport.Choices(names, defaults.getWaves(), defaults.getRoles(),
					defaults.getLineupNames(), true);
			assertEquals("\"Foo\" is already used by an existing preset on wave 1 Defender",
					BaUtilitiesImport.presetNameCollisions(plan, choices, store.getPresets()).get(WALL_SPLIT));
		}

		@Test
		public void wavesAndRolesAnEarlierImportCoversAreNotChecked()
		{
			// the earlier import's preset was renamed, and the user made their own "My Stack" there: import skips it anyway
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			List<StrategyPreset> existing = List.of(
					new StrategyPreset(BaUtilitiesImport.presetId("strat-stack", 1, "d"), "Bar", 1, "d"),
					new StrategyPreset("mine", "My Stack", 1, "d"));
			assertEquals(Map.of(), BaUtilitiesImport.presetNameCollisions(plan, BaUtilitiesImport.Choices.defaults(plan), existing));
		}

		@Test
		public void presetsFromAnEarlierImportDoNotCollide()
		{
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.Choices choices = BaUtilitiesImport.Choices.defaults(plan);
			StrategyPreset earlier = new StrategyPreset(BaUtilitiesImport.presetId(WALL_SPLIT, 7, "d"), "Wall Split", 7, "d");
			assertEquals(Map.of(), BaUtilitiesImport.presetNameCollisions(plan, choices, List.of(earlier)));
		}
	}

	public static class LineupNameCollisions
	{
		@Test
		public void existingLineupWithTheNameForTheRoleCollides()
		{
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.Choices choices = BaUtilitiesImport.Choices.defaults(plan);
			Map<String, String> collisions = BaUtilitiesImport.lineupNameCollisions(plan, choices,
					List.of(new Lineup("x", "beginner", "d", Map.of()), new Lineup("y", "Heal setup", "a", Map.of())));
			assertEquals(Set.of(BEGINNER_LINEUP), collisions.keySet());
		}
	}

	public static class Reimport
	{
		@Test
		public void aRenamedLineupFromAnEarlierImportStillCollidesWithANewOne()
		{
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			List<Lineup> existing = List.of(new Lineup("bau-lineup:gone:d", "BA Utilities current setup", "d", Map.of()));
			assertTrue(BaUtilitiesImport.lineupNameCollisions(plan, BaUtilitiesImport.Choices.defaults(plan), existing)
					.containsKey("bau-current:d"));
		}

		@Test
		public void lineupsFollowBaUtilitiesCurrentAssignments()
		{
			BATilesStore store = newStore();
			BaUtilitiesImport.Plan first = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.apply(store, first, BaUtilitiesImport.Choices.defaults(first));

			// the defender's wave 3 selection changes in BA Utilities, then everything is imported again
			BaUtilitiesImport.Plan second = BaUtilitiesImport.plan(RuneLiteAPI.GSON.fromJson(
					STORE_JSON.replace("\"wave\":3,\"strategyId\":\"" + WALL_SPLIT + "\"",
							"\"wave\":3,\"strategyId\":\"built-in:tile-marker-strategy:hendi-triangle\""),
					BaUtilitiesData.Store.class));
			BaUtilitiesImport.apply(store, second, BaUtilitiesImport.Choices.defaults(second));

			assertEquals(BaUtilitiesImport.presetId("built-in:tile-marker-strategy:hendi-triangle", 3, "d"), store.getActivePresetId(3, "d"));
			assertEquals("bau-current:d", store.getActiveLineupId("d"));
		}

		@Test
		public void editedOrDeletedGlobalTilesAreNotAddedAgain()
		{
			BATilesStore store = newStore();
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.apply(store, plan, BaUtilitiesImport.Choices.defaults(plan));
			List<GroundMarkerPoint> global = store.getPoints(7509).stream().filter(p -> !p.isPresetTile()).collect(Collectors.toList());
			store.updatePoint(global.get(0), p -> p.withLabel("edited"));
			store.removePoint(global.get(1));

			BaUtilitiesImport.Result again = BaUtilitiesImport.apply(store, plan, BaUtilitiesImport.Choices.defaults(plan));
			assertEquals(0, again.getGlobalTiles());
			assertEquals(2, store.getPoints(7509).stream().filter(p -> !p.isPresetTile()).count());
		}
	}

	public static class Apply
	{
		@Test
		public void createsPresetsWithStyledTilesForEachChosenWaveAndRole()
		{
			BATilesStore store = newStore();
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.apply(store, plan, BaUtilitiesImport.Choices.defaults(plan));

			String id = BaUtilitiesImport.presetId("strat-stack", 6, "c");
			assertEquals(new StrategyPreset(id, "My Stack", 6, "c"), store.getPreset(id).orElseThrow());
			List<GroundMarkerPoint> tiles = store.getPoints(7509).stream().filter(p -> id.equals(p.getPresetId())).collect(Collectors.toList());
			assertEquals(3, tiles.size());
			assertEquals(new GroundMarkerPoint(7509, 40, 30, 0, Color.RED, "S", List.of(6), List.of("c"), id, 50, 3.0f), tiles.get(0));
			// BA Utilities' defaults, and an empty label as none
			assertEquals(new GroundMarkerPoint(7509, 41, 30, 0, Color.GREEN, null, List.of(6), List.of("c"), id, 22, 1.0f), tiles.get(1));

			// built-ins: defender only
			assertTrue(store.getPreset(BaUtilitiesImport.presetId(WALL_SPLIT, 6, "d")).isPresent());
			assertFalse(store.getPreset(BaUtilitiesImport.presetId(WALL_SPLIT, 6, "a")).isPresent());
			// wave 10 strategies only on wave 10
			assertTrue(store.getPreset(BaUtilitiesImport.presetId("built-in:tile-marker-strategy:auk-w10", 10, "d")).isPresent());
		}

		@Test
		public void globalSelectionsBecomeTilesForAllRolesShownAlongsidePresets()
		{
			BATilesStore store = newStore();
			store.setShowBaseWithPreset(4, "h", false);
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.Result result = BaUtilitiesImport.apply(store, plan, BaUtilitiesImport.Choices.defaults(plan));

			List<GroundMarkerPoint> global = store.getPoints(7509).stream().filter(p -> !p.isPresetTile()).collect(Collectors.toList());
			assertEquals(3, global.size());
			assertEquals(List.of(4), global.get(0).getWaves());
			assertEquals(List.of("a", "c", "d", "h"), global.get(0).getRoles());
			assertEquals(3, result.getGlobalTiles());
			assertTrue(store.isShowBaseWithPreset(4, "h"));
		}

		@Test
		public void importsLineupsAndSwitchesToTheCurrentSetup()
		{
			BATilesStore store = newStore();
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.apply(store, plan, BaUtilitiesImport.Choices.defaults(plan));

			assertEquals("bau-lineup:lineup-heal:h", store.getActiveLineupId("h"));
			assertEquals(BaUtilitiesImport.presetId("strat-stack", 2, "h"), store.getActivePresetId(2, "h"));
			assertEquals("bau-current:d", store.getActiveLineupId("d"));
			assertEquals(BaUtilitiesImport.presetId(WALL_SPLIT, 3, "d"), store.getActivePresetId(3, "d"));
			assertEquals(Map.of(5, BaUtilitiesImport.presetId("strat-stack", 5, "a"), 10, BaUtilitiesImport.presetId("strat-w10", 10, "a")),
					store.getLineup("bau-lineup:lineup-all:a").orElseThrow().getPresetIds());
		}

		@Test
		public void canLeaveTheCurrentSetupAlone()
		{
			BATilesStore store = newStore();
			StrategyPreset mine = store.createPreset("Mine", 3, "d");
			store.setActivePreset(3, "d", mine);
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.Choices defaults = BaUtilitiesImport.Choices.defaults(plan);
			BaUtilitiesImport.apply(store, plan, new BaUtilitiesImport.Choices(defaults.getPresetNames(), defaults.getWaves(),
					defaults.getRoles(), defaults.getLineupNames(), false));

			assertEquals(mine.getId(), store.getActivePresetId(3, "d"));
		}

		@Test
		public void importingAgainAddsNothing()
		{
			BATilesStore store = newStore();
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(store());
			BaUtilitiesImport.Result first = BaUtilitiesImport.apply(store, plan, BaUtilitiesImport.Choices.defaults(plan));
			int presets = store.getPresets().size();
			int tiles = store.getPoints(7509).size() + store.getPoints(7508).size();
			int lineups = store.getLineups().size();

			BaUtilitiesImport.Result second = BaUtilitiesImport.apply(store, plan, BaUtilitiesImport.Choices.defaults(plan));
			assertEquals(0, second.getPresetsCreated());
			assertEquals(first.getPresetsCreated(), second.getPresetsAlreadyImported());
			assertEquals(0, second.getLineupsCreated());
			assertEquals(0, second.getGlobalTiles());
			assertEquals(presets, store.getPresets().size());
			assertEquals(tiles, store.getPoints(7509).size() + store.getPoints(7508).size());
			assertEquals(lineups, store.getLineups().size());
		}

		@Test
		public void unimportedStrategiesAndLineupsAreSkipped()
		{
			BATilesStore store = newStore();
			BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(null);
			BaUtilitiesImport.Choices choices = new BaUtilitiesImport.Choices(Map.of(), Map.of(), Map.of(), Map.of(), true);
			BaUtilitiesImport.Result result = BaUtilitiesImport.apply(store, plan, choices);
			assertEquals(0, result.getPresetsCreated());
			assertEquals(List.of(), store.getPresets());
			assertEquals(List.of(), store.getLineups());
		}
	}
}
