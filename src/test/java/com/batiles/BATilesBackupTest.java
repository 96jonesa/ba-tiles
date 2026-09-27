package com.batiles;

import static org.junit.Assert.assertEquals;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class BATilesBackupTest
{
	// the Gson instance RuneLite injects into plugins
	private static final Gson GSON = RuneLiteAPI.GSON;
	private static final GroundMarkerPoint TILE = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, "A", List.of(3), List.of("d"));
	private static final Map<String, String> ENTRIES = Map.of(
			"region_7509", GSON.toJson(List.of(TILE, TILE.withRegionX(31))),
			"region_7508", GSON.toJson(List.of(TILE.withRegionId(7508))),
			"presets", GSON.toJson(List.of(new StrategyPreset("p1", "Stack", 3, "d"))),
			"activePreset_3_d", "p1",
			"showBaseWithPreset_3_d", "false",
			// values that are themselves JSON-ish or contain quotes must survive untouched
			"markerColor", "#FFFFFF00");

	// spans toJson and fromJson
	public static class RoundTrip
	{
		@Test
		public void restoresEveryEntryExactly()
		{
			BATilesBackup backup = BATilesBackup.of(ENTRIES);
			assertEquals(ENTRIES, BATilesBackup.fromJson(GSON, backup.toJson(GSON)).getEntries());
		}

		@Test
		public void emptyBackupRoundTrips()
		{
			assertEquals(Map.of(), BATilesBackup.fromJson(GSON, BATilesBackup.of(Map.of()).toJson(GSON)).getEntries());
		}
	}

	public static class FromJson
	{
		@Test(expected = JsonParseException.class)
		public void rejectsATileExport()
		{
			BATilesBackup.fromJson(GSON, GSON.toJson(List.of(TILE)));
		}

		@Test(expected = JsonParseException.class)
		public void rejectsAnObjectWithoutTheFormatKey()
		{
			BATilesBackup.fromJson(GSON, "{\"entries\":{}}");
		}

		@Test(expected = JsonParseException.class)
		public void rejectsAnotherFormatVersion()
		{
			BATilesBackup.fromJson(GSON, "{\"baTilesBackup\":2,\"entries\":{}}");
		}

		@Test(expected = JsonParseException.class)
		public void rejectsNonStringEntries()
		{
			BATilesBackup.fromJson(GSON, "{\"baTilesBackup\":1,\"entries\":{\"region_7509\":[1,2]}}");
		}

		@Test(expected = JsonParseException.class)
		public void rejectsEmptyText()
		{
			BATilesBackup.fromJson(GSON, "");
		}
	}

	public static class KeysToRemove
	{
		@Test
		public void removesStoredKeysTheBackupDoesNotHave()
		{
			BATilesBackup backup = BATilesBackup.of(Map.of("region_7509", "[]", "presets", "[]"));
			assertEquals(java.util.Set.of("region_7508", "activePreset_3_d"),
					backup.keysToRemove(List.of("region_7509", "region_7508", "activePreset_3_d")));
		}

		@Test
		public void emptyBackupRemovesEverything()
		{
			assertEquals(java.util.Set.of("region_7509", "markerColor"),
					BATilesBackup.of(Map.of()).keysToRemove(List.of("region_7509", "markerColor")));
		}
	}

	public static class Describe
	{
		@Test
		public void countsTilesRegionsPresetsAndOtherSettings()
		{
			assertEquals("3 tiles in 2 regions, 1 strategy preset and 3 other settings",
					BATilesBackup.of(ENTRIES).describe(GSON));
		}

		@Test
		public void describesAnEmptyBackup()
		{
			assertEquals("0 tiles in 0 regions, 0 strategy presets and 0 other settings",
					BATilesBackup.of(Map.of()).describe(GSON));
		}
	}
}
