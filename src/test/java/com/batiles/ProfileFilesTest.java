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
import net.runelite.client.util.Filepath;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class ProfileFilesTest
{
	private static final GroundMarkerPoint MARKER = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, "stäck", null, null);
	private static final GroundMarkerPoint OTHER = new GroundMarkerPoint(7509, 31, 28, 0, Color.BLUE, null, null, null);
	private static final GroundMarkerPoint W10_MARKER = new GroundMarkerPoint(7508, 30, 20, 0, null, null, null, null);

	private static Properties profile(Map<String, List<GroundMarkerPoint>> markers)
	{
		Properties properties = new Properties();
		markers.forEach((key, list) -> properties.setProperty(key, RuneLiteAPI.GSON.toJson(list)));
		return properties;
	}

	public static class ProfileName
	{
		@Test
		public void isTheFileNameWithoutTheIdAndExtension()
		{
			assertEquals("healer", ProfileFiles.profileName("healer-3509817840025541.properties"));
			assertEquals("my-ba-profile", ProfileFiles.profileName("my-ba-profile-72047370338875.properties"));
		}

		@Test
		public void otherFileNamesAreKept()
		{
			assertEquals("notes.properties", ProfileFiles.profileName("notes.properties"));
			assertEquals("-12.properties", ProfileFiles.profileName("-12.properties"));
		}
	}

	public static class Load
	{
		@Rule
		public TemporaryFolder folder = new TemporaryFolder();

		@Test
		public void readsAProfileFileThroughFilepathAsUtf8() throws Exception
		{
			Properties written = profile(Map.of("groundMarker.region_7509", List.of(MARKER)));
			File file = folder.newFile("healer-1.properties");
			// the way RuneLite writes profiles
			try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
			{
				written.store(writer, "RuneLite configuration");
			}

			ProfileFiles.Profile profile = ProfileFiles.load(Filepath.Unchecked.getRooted(file.toPath()));
			assertEquals("healer", profile.getName());
			assertEquals(Map.of(7509, List.of(MARKER)),
					ProfileFiles.readMarkers(List.of(profile.getProperties()), RuneLiteAPI.GSON, ArenaMapLayout.REGION_IDS));
		}
	}

	public static class ReadMarkers
	{
		@Test
		public void readsTheArenaRegionsOnly()
		{
			Properties properties = profile(Map.of(
					"groundMarker.region_7509", List.of(MARKER),
					"groundMarker.region_7508", List.of(W10_MARKER),
					"groundMarker.region_12850", List.of(MARKER.withRegionId(12850)),
					"baTiles.region_7509", List.of(OTHER)));
			assertEquals(Map.of(7509, List.of(MARKER), 7508, List.of(W10_MARKER)),
					ProfileFiles.readMarkers(List.of(properties), RuneLiteAPI.GSON, ArenaMapLayout.REGION_IDS));
		}

		@Test
		public void combinesProfilesAndKeepsAMarkerSeveralHaveOnce()
		{
			Properties healer = profile(Map.of("groundMarker.region_7509", List.of(MARKER, OTHER)));
			Properties defender = profile(Map.of("groundMarker.region_7509", List.of(MARKER), "groundMarker.region_7508", List.of(W10_MARKER)));
			assertEquals(Map.of(7509, List.of(MARKER, OTHER), 7508, List.of(W10_MARKER)),
					ProfileFiles.readMarkers(List.of(healer, defender), RuneLiteAPI.GSON, ArenaMapLayout.REGION_IDS));
		}

		@Test
		public void omitsRegionsWithoutMarkers()
		{
			Properties properties = new Properties();
			properties.setProperty("groundMarker.region_7508", "[]");
			assertEquals(Map.of(), ProfileFiles.readMarkers(List.of(properties), RuneLiteAPI.GSON, ArenaMapLayout.REGION_IDS));
		}
	}
}
