package com.batiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class BATilesStoreTest
{
	/**
	 * An in-memory config, keyed by "group.key" like RuneLite's.
	 */
	static class MemoryConfig implements ConfigAccess
	{
		final Map<String, String> values = new TreeMap<>();

		@Override
		public String get(String group, String key)
		{
			return values.get(group + "." + key);
		}

		@Override
		public void set(String group, String key, String value)
		{
			values.put(group + "." + key, value);
		}

		@Override
		public void unset(String group, String key)
		{
			values.remove(group + "." + key);
		}

		@Override
		public List<String> keys(String group)
		{
			List<String> keys = new ArrayList<>();
			for (String key : values.keySet())
			{
				if (key.startsWith(group + "."))
				{
					keys.add(key.substring(group.length() + 1));
				}
			}
			return keys;
		}
	}

	private static BATilesStore newStore()
	{
		return new BATilesStore(new MemoryConfig(), RuneLiteAPI.GSON);
	}

	private static Map<Integer, String> activePresets(BATilesStore store, String role)
	{
		Map<Integer, String> active = new TreeMap<>();
		for (int wave = 1; wave <= 10; wave++)
		{
			String id = store.getActivePresetId(wave, role);
			if (id != null)
			{
				active.put(wave, id);
			}
		}
		return active;
	}

	public static class TestSetActivePreset
	{
		@Test
		public void changingAWaveByHandEndsTheActiveLineup()
		{
			BATilesStore store = newStore();
			StrategyPreset stack = store.createPreset("Stack", 3, "d");
			Lineup lineup = store.addLineup("69", "d", Map.of(3, stack.getId()));
			store.applyLineup(lineup);
			assertEquals(lineup.getId(), store.getActiveLineupId("d"));

			store.setActivePreset(3, "d", null);
			assertNull(store.getActiveLineupId("d"));
		}

		@Test
		public void settingTheSamePresetAgainKeepsTheLineup()
		{
			BATilesStore store = newStore();
			StrategyPreset stack = store.createPreset("Stack", 3, "d");
			Lineup lineup = store.addLineup("69", "d", Map.of(3, stack.getId()));
			store.applyLineup(lineup);

			store.setActivePreset(3, "d", stack);
			assertEquals(lineup.getId(), store.getActiveLineupId("d"));
		}

		@Test
		public void otherRolesKeepTheirLineup()
		{
			BATilesStore store = newStore();
			StrategyPreset def = store.createPreset("Stack", 3, "d");
			StrategyPreset heal = store.createPreset("Heal", 3, "h");
			Lineup defLineup = store.addLineup("69", "d", Map.of(3, def.getId()));
			store.applyLineup(defLineup);

			store.setActivePreset(3, "h", heal);
			assertEquals(defLineup.getId(), store.getActiveLineupId("d"));
		}
	}

	public static class TestApplyLineup
	{
		@Test
		public void activatesItsPresetsAndClearsOtherWaves()
		{
			BATilesStore store = newStore();
			StrategyPreset w3 = store.createPreset("W3", 3, "d");
			StrategyPreset w7 = store.createPreset("W7", 7, "d");
			StrategyPreset w5 = store.createPreset("W5", 5, "d");
			store.setActivePreset(5, "d", w5);

			store.applyLineup(store.addLineup("69", "d", Map.of(3, w3.getId(), 7, w7.getId())));
			assertEquals(Map.of(3, w3.getId(), 7, w7.getId()), activePresets(store, "d"));
		}

		@Test
		public void leavesOtherRolesAlone()
		{
			BATilesStore store = newStore();
			StrategyPreset def = store.createPreset("Def", 3, "d");
			StrategyPreset heal = store.createPreset("Heal", 3, "h");
			store.setActivePreset(3, "h", heal);

			store.applyLineup(store.addLineup("69", "d", Map.of(3, def.getId())));
			assertEquals(heal.getId(), store.getActivePresetId(3, "h"));
		}
	}

	public static class TestSaveCurrentAsLineup
	{
		@Test
		public void snapshotsEveryWaveAndBecomesActive()
		{
			BATilesStore store = newStore();
			StrategyPreset w1 = store.createPreset("W1", 1, "a");
			StrategyPreset w10 = store.createPreset("W10", 10, "a");
			store.setActivePreset(1, "a", w1);
			store.setActivePreset(10, "a", w10);

			Lineup lineup = store.saveCurrentAsLineup("Main", "a");
			assertEquals(Map.of(1, w1.getId(), 10, w10.getId()), lineup.getPresetIds());
			assertEquals(lineup.getId(), store.getActiveLineupId("a"));
			// and it round-trips through the config, wave numbers included
			assertEquals(List.of(lineup), store.getLineups("a"));
		}
	}

	public static class TestHasLineupNamed
	{
		@Test
		public void isPerRoleAndIgnoresCase()
		{
			BATilesStore store = newStore();
			store.addLineup("Main", "d", Map.of());
			assertTrue(store.hasLineupNamed("d", "main"));
			assertFalse(store.hasLineupNamed("h", "Main"));
		}
	}

	public static class TestDeleteLineup
	{
		@Test
		public void keepsTheActivePresetsButEndsTheLineup()
		{
			BATilesStore store = newStore();
			StrategyPreset w3 = store.createPreset("W3", 3, "d");
			Lineup lineup = store.addLineup("69", "d", Map.of(3, w3.getId()));
			store.applyLineup(lineup);

			store.deleteLineup(lineup);
			assertEquals(List.of(), store.getLineups());
			assertNull(store.getActiveLineupId("d"));
			assertEquals(w3.getId(), store.getActivePresetId(3, "d"));
		}
	}

	public static class TestDeletePreset
	{
		@Test
		public void removesThePresetFromLineups()
		{
			BATilesStore store = newStore();
			StrategyPreset w3 = store.createPreset("W3", 3, "d");
			StrategyPreset w4 = store.createPreset("W4", 4, "d");
			store.addLineup("69", "d", Map.of(3, w3.getId(), 4, w4.getId()));

			store.deletePreset(w3, ArenaMapLayout.REGION_IDS);
			assertEquals(Map.of(4, w4.getId()), store.getLineups("d").get(0).getPresetIds());
		}
	}

	// spans exportAll and replaceAll
	public static class FullBackup
	{
		@Test
		public void replaceAllRestoresExactlyWhatWasExported()
		{
			BATilesStore source = newStore();
			StrategyPreset stack = source.createPreset("Stack", 3, "d");
			source.addPoint(new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, "A", List.of(3), List.of("d"), stack.getId()));
			source.setActivePreset(3, "d", stack);
			source.saveCurrentAsLineup("69", "d");
			BATilesBackup backup = source.exportAll();

			MemoryConfig targetConfig = new MemoryConfig();
			BATilesStore target = new BATilesStore(targetConfig, RuneLiteAPI.GSON);
			target.createPreset("Old", 5, "h");
			targetConfig.set("groundMarker", "region_7509", "[]");

			target.replaceAll(backup);
			assertEquals(backup.getEntries(), target.exportAll().getEntries());
			// other plugins' config is untouched
			assertEquals("[]", targetConfig.get("groundMarker", "region_7509"));
		}
	}
}
