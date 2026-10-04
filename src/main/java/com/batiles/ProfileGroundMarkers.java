package com.batiles;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.Value;
import net.runelite.client.config.ConfigProfile;
import net.runelite.client.config.ProfileManager;
import net.runelite.client.util.Filepath;

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
		Filepath file;

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
					// other profiles' files are outside this plugin's directory, so they can only be reached unchecked
					sources.add(new Source(profile.getName(),
							Filepath.Unchecked.getRooted(ProfileManager.profileConfigFile(profile).toPath())));
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
	static Map<Integer, List<GroundMarkerPoint>> readMarkers(Filepath profileFile, Gson gson, int... regionIds) throws IOException
	{
		Properties properties = new Properties();
		// openReader reads UTF-8, the encoding RuneLite writes profiles in
		try (Reader reader = profileFile.openReader())
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
