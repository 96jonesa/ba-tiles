package com.batiles;

import static org.junit.Assert.assertEquals;
import java.awt.Color;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class GroundMarkerImportTest
{
	private static final GroundMarkerPoint MARKER = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, "stack", null, null);
	private static final GroundMarkerPoint IMPORTED = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, "stack",
			List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), List.of("a", "c", "d", "h"));

	public static class Parse
	{
		@Test
		public void readsTheGroundMarkersPluginFormat()
		{
			// as the core Ground Markers plugin saves it, through RuneLite's Gson
			String json = "[{\"regionId\":7509,\"regionX\":30,\"regionY\":28,\"z\":0,\"color\":\"#FFFF0000\",\"label\":\"stack\"},"
					+ "{\"regionId\":7509,\"regionX\":31,\"regionY\":28,\"z\":0}]";
			assertEquals(List.of(MARKER, new GroundMarkerPoint(7509, 31, 28, 0, null, null, null, null)),
					GroundMarkerImport.parse(RuneLiteAPI.GSON, json));
		}
	}

	// spans parse and toBaTiles
	public static class Import
	{
		@Test
		public void parsedMarkersImportAsBaTiles()
		{
			String json = RuneLiteAPI.GSON.toJson(List.of(MARKER));
			assertEquals(List.of(IMPORTED), GroundMarkerImport.toBaTiles(GroundMarkerImport.parse(RuneLiteAPI.GSON, json), List.of()));
		}
	}

	public static class ToBaTiles
	{
		@Test
		public void importsOnAllWavesForAllRolesWithSameColorAndLabel()
		{
			assertEquals(List.of(IMPORTED), GroundMarkerImport.toBaTiles(List.of(MARKER), List.of()));
		}

		@Test
		public void importingAgainAddsNothing()
		{
			assertEquals(List.of(), GroundMarkerImport.toBaTiles(List.of(MARKER), List.of(IMPORTED)));
		}

		@Test
		public void duplicateMarkersImportOnce()
		{
			assertEquals(List.of(IMPORTED), GroundMarkerImport.toBaTiles(List.of(MARKER, MARKER), List.of()));
		}

		@Test
		public void existingTileWithOtherColorLabelOrWavesDoesNotCount()
		{
			List<GroundMarkerPoint> existing = List.of(
					IMPORTED.withColor(Color.BLUE),
					IMPORTED.withLabel("other"),
					IMPORTED.withWaves(List.of(3)),
					IMPORTED.withPresetId("p1"));
			assertEquals(List.of(IMPORTED), GroundMarkerImport.toBaTiles(List.of(MARKER), existing));
		}

		@Test
		public void existingTileWithNullWavesAndRolesCounts()
		{
			GroundMarkerPoint everywhere = IMPORTED.withWaves(null).withRoles(null);
			assertEquals(List.of(), GroundMarkerImport.toBaTiles(List.of(MARKER), List.of(everywhere)));
		}

		@Test
		public void keepsMarkersWithoutColorOrLabel()
		{
			GroundMarkerPoint plain = new GroundMarkerPoint(7508, 40, 30, 0, null, null, null, null);
			assertEquals(List.of(plain.withWaves(IMPORTED.getWaves()).withRoles(IMPORTED.getRoles())),
					GroundMarkerImport.toBaTiles(List.of(plain), List.of()));
		}
	}
}
