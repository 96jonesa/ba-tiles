package com.batiles;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.Value;
import net.runelite.client.config.ConfigProfile;
import net.runelite.client.config.ProfileManager;

/**
 * Reads the Ground Markers plugin's arena markers from another RuneLite profile, for converting them into BA Tiles
 * on the current profile. Other profiles are only ever read, never written.
 */
final class ProfileGroundMarkers
{
	private static final String REGION_PREFIX = "region_";

	private ProfileGroundMarkers()
	{
	}

	/**
	 * A RuneLite profile other than the active one.
	 */
	@Value
	static class Source
	{
		String name;
		File file;

		@Override
		public String toString()
		{
			return "Profile: " + name;
		}
	}

	/**
	 * @return the user's profiles other than the active one (and RuneLite's internal profiles), by name
	 */
	static List<Source> otherProfiles(ProfileManager profileManager)
	{
		List<Source> sources = new ArrayList<>();
		// the lock only saves profiles.json if something was modified, which this never does
		try (ProfileManager.Lock lock = profileManager.lock())
		{
			for (ConfigProfile profile : lock.getProfiles())
			{
				if (!profile.isInternal() && !profile.isActive())
				{
					sources.add(new Source(profile.getName(), ProfileManager.profileConfigFile(profile)));
				}
			}
		}
		sources.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
		return sources;
	}

	/**
	 * Reads a profile's Ground Markers markers in the given regions.
	 *
	 * @return markers by region id; regions without markers are omitted
	 * @throws IOException if the profile's file cannot be read
	 */
	static Map<Integer, List<GroundMarkerPoint>> readMarkers(File profileFile, Gson gson, int... regionIds) throws IOException
	{
		Properties properties = new Properties();
		// the same encoding RuneLite writes profiles in
		try (Reader reader = new InputStreamReader(new FileInputStream(profileFile), StandardCharsets.UTF_8))
		{
			properties.load(reader);
		}
		return readMarkers(properties, gson, regionIds);
	}

	static Map<Integer, List<GroundMarkerPoint>> readMarkers(Properties properties, Gson gson, int... regionIds)
	{
		Map<Integer, List<GroundMarkerPoint>> markers = new HashMap<>();
		for (int regionId : regionIds)
		{
			String json = properties.getProperty(GroundMarkerImport.GROUND_MARKER_CONFIG_GROUP + "." + REGION_PREFIX + regionId);
			List<GroundMarkerPoint> regionMarkers = Strings.isNullOrEmpty(json) ? Collections.emptyList() : GroundMarkerImport.parse(gson, json);
			if (!regionMarkers.isEmpty())
			{
				markers.put(regionId, regionMarkers);
			}
		}
		return markers;
	}
}
