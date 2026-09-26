package com.batiles;

import static org.junit.Assert.assertEquals;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class ProfileGroundMarkersTest
{
	private static final GroundMarkerPoint MARKER = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, "stäck", null, null);
	private static final GroundMarkerPoint W10_MARKER = new GroundMarkerPoint(7508, 30, 20, 0, null, null, null, null);

	/**
	 * Writes a profile file the way RuneLite does (a UTF-8 Properties file).
	 */
	private static File writeProfile(TemporaryFolder folder, Properties properties) throws Exception
	{
		File file = folder.newFile("profile-1.properties");
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
		{
			properties.store(writer, "RuneLite configuration");
		}
		return file;
	}

	public static class ReadMarkers
	{
		@Rule
		public TemporaryFolder folder = new TemporaryFolder();

		@Test
		public void readsGroundMarkersOfTheArenaRegionsFromAProfileFile() throws Exception
		{
			Properties properties = new Properties();
			properties.setProperty("groundMarker.region_7509", RuneLiteAPI.GSON.toJson(List.of(MARKER)));
			properties.setProperty("groundMarker.region_7508", RuneLiteAPI.GSON.toJson(List.of(W10_MARKER)));
			// other regions and plugins are ignored
			properties.setProperty("groundMarker.region_12850", RuneLiteAPI.GSON.toJson(List.of(MARKER.withRegionId(12850))));
			properties.setProperty("baTiles.region_7509", "[]");

			Map<Integer, List<GroundMarkerPoint>> markers = ProfileGroundMarkers.readMarkers(
					writeProfile(folder, properties), RuneLiteAPI.GSON, ArenaMapLayout.REGION_IDS);
			assertEquals(Map.of(7509, List.of(MARKER), 7508, List.of(W10_MARKER)), markers);
		}

		@Test
		public void omitsRegionsWithoutMarkers() throws Exception
		{
			Properties properties = new Properties();
			properties.setProperty("groundMarker.region_7508", "[]");
			assertEquals(Map.of(), ProfileGroundMarkers.readMarkers(writeProfile(folder, properties), RuneLiteAPI.GSON,
					ArenaMapLayout.REGION_IDS));
		}
	}
}
