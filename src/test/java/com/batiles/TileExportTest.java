package com.batiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.awt.Color;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class TileExportTest
{
	// the Gson instance RuneLite injects into plugins
	private static final Gson GSON = RuneLiteAPI.GSON;
	private static final StrategyPreset PRESET = new StrategyPreset("p1", "Stack", 3, "d");
	private static final StrategyPreset OTHER_PRESET = new StrategyPreset("p2", "Split", 3, "d");
	private static final GroundMarkerPoint BASE = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, "A", List.of(3), List.of("d"));
	private static final GroundMarkerPoint PRESET_TILE = BASE.withPresetId("p1");

	// spans TileExport.of, toJson and fromJson
	public static class RoundTrip
	{
		@Test
		public void roundTripsTilesWithPresets()
		{
			TileExport export = TileExport.of(List.of(BASE, PRESET_TILE), List.of(PRESET, OTHER_PRESET));
			TileExport imported = TileExport.fromJson(GSON, export.toJson(GSON));
			assertEquals(List.of(BASE, PRESET_TILE), imported.getTiles());
			assertEquals(List.of(PRESET), imported.getPresets());
		}

		@Test
		public void roundTripsTilesWithoutPresets()
		{
			TileExport export = TileExport.of(List.of(BASE), List.of(PRESET));
			TileExport imported = TileExport.fromJson(GSON, export.toJson(GSON));
			assertEquals(List.of(BASE), imported.getTiles());
			assertEquals(List.of(), imported.getPresets());
		}
	}

	public static class Of
	{
		@Test
		public void includesOnlyReferencedPresets()
		{
			assertEquals(List.of(PRESET), TileExport.of(List.of(PRESET_TILE), List.of(PRESET, OTHER_PRESET)).getPresets());
		}
	}

	public static class ToJson
	{
		@Test
		public void withoutPresetsWritesTheLegacyArrayFormat()
		{
			assertTrue(TileExport.of(List.of(BASE), List.of()).toJson(GSON).startsWith("["));
		}

		@Test
		public void withPresetsWritesAnObject()
		{
			assertTrue(TileExport.of(List.of(PRESET_TILE), List.of(PRESET)).toJson(GSON).startsWith("{"));
		}
	}

	public static class FromJson
	{
		@Test
		public void readsTilesExportedByOlderVersions()
		{
			String legacy = "[{\"regionId\":7509,\"regionX\":30,\"regionY\":28,\"z\":0,"
					+ "\"color\":\"#FFFF0000\",\"label\":\"A\",\"waves\":[3],\"roles\":[\"d\"]}]";
			TileExport imported = TileExport.fromJson(GSON, legacy);
			assertEquals(List.of(BASE), imported.getTiles());
		}

		@Test
		public void dropsPresetTilesWhosePresetIsMissing()
		{
			String json = GSON.toJson(new TileExport(List.of(BASE, BASE.withPresetId("missing")), List.of()));
			assertEquals(List.of(BASE), TileExport.fromJson(GSON, json).getTiles());
		}

		@Test(expected = JsonParseException.class)
		public void rejectsNonExports()
		{
			TileExport.fromJson(GSON, "\"hello\"");
		}

		@Test(expected = JsonParseException.class)
		public void rejectsEmptyText()
		{
			TileExport.fromJson(GSON, "");
		}
	}
}
