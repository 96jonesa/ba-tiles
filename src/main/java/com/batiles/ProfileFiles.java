package com.batiles;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;
import net.runelite.client.RuneLite;
import net.runelite.client.util.Filepath;

/**
 * Reads RuneLite profile files that the user picks in a file dialog, for importing their ground markers into BA Tiles
 * on the current profile. Files are only ever read, never written, and only through {@link Filepath}.
 */
final class ProfileFiles
{
	private static final String REGION_PREFIX = "region_";
	// RuneLite names profile files "<profile name>-<profile id>.properties"
	private static final Pattern PROFILE_FILE_NAME = Pattern.compile("^(.*)-\\d+\\.properties$");

	private ProfileFiles()
	{
	}

	/**
	 * A profile file the user picked.
	 */
	@Value
	static class Profile
	{
		String name;
		Properties properties;
	}

	/**
	 * @return where RuneLite keeps profile files, to tell the user where to look (this only builds the path)
	 */
	static String profilesFolder()
	{
		return new File(RuneLite.RUNELITE_DIR, "profiles2").getPath();
	}

	/**
	 * Lets the user pick one or more profile files. Must be called on the Swing event thread.
	 *
	 * @return the picked files, or an empty list if the user cancelled
	 */
	static List<Filepath> choose(Component parent)
	{
		List<Filepath> files = new Filepath.Chooser()
				.setIsOpen()
				.setAcceptsFiles()
				.setMultiSelectionEnabled(true)
				// RuneLite's folder (.runelite) is hidden on macOS and Linux
				.setFileHidingEnabled(false)
				.addExtensionFilter("RuneLite profiles (.properties)", "properties")
				.setDialogTitle("Choose the RuneLite profiles to import ground markers from")
				.showDialog(parent);
		return files == null ? Collections.emptyList() : files;
	}

	static Profile load(Filepath file) throws IOException
	{
		Properties properties = new Properties();
		try (Reader reader = file.openReader())
		{
			properties.load(reader);
		}
		return new Profile(profileName(file.getFileName()), properties);
	}

	/**
	 * @return the profile's name from its file name ("healer-3509817840025541.properties" is "healer"), or the
	 *         file name if it isn't named like a profile file
	 */
	static String profileName(String fileName)
	{
		Matcher matcher = PROFILE_FILE_NAME.matcher(fileName);
		return matcher.matches() && !matcher.group(1).isEmpty() ? matcher.group(1) : fileName;
	}

	/**
	 * Reads the Ground Markers plugin's markers in the given regions from profiles. A marker that more than one
	 * profile has, identically, is included once.
	 *
	 * @return markers by region id; regions without markers are omitted
	 */
	static Map<Integer, List<GroundMarkerPoint>> readMarkers(Collection<Properties> profiles, Gson gson, int... regionIds)
	{
		Map<Integer, List<GroundMarkerPoint>> markers = new LinkedHashMap<>();
		for (int regionId : regionIds)
		{
			Set<GroundMarkerPoint> regionMarkers = new LinkedHashSet<>();
			for (Properties properties : profiles)
			{
				String json = properties.getProperty(GroundMarkerImport.GROUND_MARKER_CONFIG_GROUP + "." + REGION_PREFIX + regionId);
				if (!Strings.isNullOrEmpty(json))
				{
					regionMarkers.addAll(GroundMarkerImport.parse(gson, json));
				}
			}
			if (!regionMarkers.isEmpty())
			{
				markers.put(regionId, new ArrayList<>(regionMarkers));
			}
		}
		return markers;
	}
}
