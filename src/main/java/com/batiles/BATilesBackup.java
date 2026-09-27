package com.batiles;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.Value;

/**
 * A full backup of BA Tiles' config: every key the plugin stores (tiles in every region, strategy presets, active
 * presets, per wave / role settings, and the plugin's options), for moving everything between profiles or machines.
 */
@Value
class BATilesBackup
{
	/**
	 * Identifies the clipboard text as a BA Tiles backup (and its format version), so that other JSON, such as a
	 * tile export, is not mistaken for one.
	 */
	static final String FORMAT_KEY = "baTilesBackup";
	static final int FORMAT_VERSION = 1;
	private static final String ENTRIES_KEY = "entries";
	private static final String REGION_PREFIX = "region_";

	/**
	 * Config keys (without the group) to their stored values, sorted by key.
	 */
	Map<String, String> entries;

	static BATilesBackup of(Map<String, String> entries)
	{
		return new BATilesBackup(Collections.unmodifiableMap(new TreeMap<>(entries)));
	}

	/**
	 * @return the currently stored keys that restoring this backup must remove, so that afterwards exactly the
	 *         backup's keys are stored
	 */
	Set<String> keysToRemove(Collection<String> currentKeys)
	{
		Set<String> remove = new TreeSet<>(currentKeys);
		remove.removeAll(entries.keySet());
		return remove;
	}

	String toJson(Gson gson)
	{
		JsonObject root = new JsonObject();
		root.addProperty(FORMAT_KEY, FORMAT_VERSION);
		root.add(ENTRIES_KEY, gson.toJsonTree(entries));
		return gson.toJson(root);
	}

	/**
	 * @throws JsonParseException if the text is not a BA Tiles backup of a supported version
	 */
	static BATilesBackup fromJson(Gson gson, String json)
	{
		JsonElement root = gson.fromJson(json, JsonElement.class);
		if (root == null || !root.isJsonObject() || !root.getAsJsonObject().has(FORMAT_KEY))
		{
			throw new JsonParseException("Not a BA Tiles backup");
		}

		JsonObject object = root.getAsJsonObject();
		int version = object.get(FORMAT_KEY).getAsInt();
		if (version != FORMAT_VERSION)
		{
			throw new JsonParseException("Unsupported BA Tiles backup version " + version);
		}

		JsonElement entries = object.get(ENTRIES_KEY);
		if (entries == null || !entries.isJsonObject())
		{
			throw new JsonParseException("BA Tiles backup has no entries");
		}

		Map<String, String> parsed = new TreeMap<>();
		for (Map.Entry<String, JsonElement> entry : entries.getAsJsonObject().entrySet())
		{
			if (!entry.getValue().isJsonPrimitive())
			{
				throw new JsonParseException("BA Tiles backup entry " + entry.getKey() + " is not a string");
			}
			parsed.put(entry.getKey(), entry.getValue().getAsString());
		}
		return of(parsed);
	}

	/**
	 * @return a summary for confirmations, e.g. "12 tiles in 2 regions, 3 strategy presets and 5 other settings"
	 */
	String describe(Gson gson)
	{
		int tiles = 0;
		int regions = 0;
		int presets = 0;
		int other = 0;
		for (Map.Entry<String, String> entry : entries.entrySet())
		{
			if (entry.getKey().startsWith(REGION_PREFIX))
			{
				regions++;
				tiles += countJsonArray(gson, entry.getValue());
			}
			else if (entry.getKey().equals("presets"))
			{
				presets += countJsonArray(gson, entry.getValue());
			}
			else
			{
				other++;
			}
		}
		return plural(tiles, "tile") + " in " + plural(regions, "region") + ", " + plural(presets, "strategy preset")
				+ " and " + plural(other, "other setting");
	}

	private static int countJsonArray(Gson gson, String json)
	{
		try
		{
			List<?> list = gson.fromJson(json, List.class);
			return list == null ? 0 : list.size();
		}
		catch (JsonParseException ex)
		{
			return 0;
		}
	}

	private static String plural(int count, String noun)
	{
		return count + " " + noun + (count == 1 ? "" : "s");
	}
}
