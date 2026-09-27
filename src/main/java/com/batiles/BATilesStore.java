package com.batiles;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Persists BA Tiles and strategy presets in the RuneLite config, and notifies listeners when they change.
 */
@Slf4j
@Singleton
class BATilesStore
{
	private static final String REGION_PREFIX = "region_";
	private static final String PRESETS_KEY = "presets";
	private static final String ACTIVE_PRESET_PREFIX = "activePreset_";
	private static final String SHOW_BASE_WITH_PRESET_PREFIX = "showBaseWithPreset_";
	private static final String LINEUPS_KEY = "lineups";
	private static final String ACTIVE_LINEUP_PREFIX = "activeLineup_";
	static final int WAVES = 10;

	private final ConfigAccess config;
	private final Gson gson;
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

	@Inject
	BATilesStore(ConfigManager configManager, Gson gson)
	{
		this(ConfigAccess.of(configManager), gson);
	}

	BATilesStore(ConfigAccess config, Gson gson)
	{
		this.config = config;
		this.gson = gson;
	}

	void addListener(Runnable listener)
	{
		listeners.add(listener);
	}

	void removeListener(Runnable listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Notifies listeners that tiles or presets changed. Called after every mutation made through this store; also
	 * called by the plugin when the config changes underneath it (e.g. a profile switch).
	 */
	void fireChanged()
	{
		for (Runnable listener : listeners)
		{
			listener.run();
		}
	}

	// ---- tiles ----

	List<GroundMarkerPoint> getPoints(int regionId)
	{
		String json = config.get(BATilesConfig.BA_TILES_CONFIG_GROUP, REGION_PREFIX + regionId);
		if (Strings.isNullOrEmpty(json))
		{
			return Collections.emptyList();
		}

		// CHECKSTYLE:OFF
		List<GroundMarkerPoint> points = gson.fromJson(json, new TypeToken<List<GroundMarkerPoint>>(){}.getType());
		// CHECKSTYLE:ON
		return points == null ? Collections.emptyList() : points;
	}

	void savePoints(int regionId, @Nullable Collection<GroundMarkerPoint> points)
	{
		writePoints(regionId, points);
		fireChanged();
	}

	private void writePoints(int regionId, @Nullable Collection<GroundMarkerPoint> points)
	{
		if (points == null || points.isEmpty())
		{
			config.unset(BATilesConfig.BA_TILES_CONFIG_GROUP, REGION_PREFIX + regionId);
		}
		else
		{
			config.set(BATilesConfig.BA_TILES_CONFIG_GROUP, REGION_PREFIX + regionId, gson.toJson(points));
		}
	}

	void addPoint(GroundMarkerPoint point)
	{
		List<GroundMarkerPoint> points = new ArrayList<>(getPoints(point.getRegionId()));
		points.add(point);
		savePoints(point.getRegionId(), points);
	}

	void removePoint(GroundMarkerPoint point)
	{
		List<GroundMarkerPoint> points = new ArrayList<>(getPoints(point.getRegionId()));
		points.remove(point);
		savePoints(point.getRegionId(), points);
	}

	/**
	 * Replaces {@code existing} with {@code update.apply(existing)}, keeping its position in the region's list.
	 *
	 * @return the updated point, or {@code existing} if it is no longer stored
	 */
	GroundMarkerPoint updatePoint(GroundMarkerPoint existing, UnaryOperator<GroundMarkerPoint> update)
	{
		List<GroundMarkerPoint> points = new ArrayList<>(getPoints(existing.getRegionId()));
		int index = points.indexOf(existing);
		if (index < 0)
		{
			return existing;
		}

		GroundMarkerPoint updated = update.apply(existing);
		points.set(index, updated);
		savePoints(existing.getRegionId(), points);
		return updated;
	}

	// ---- presets ----

	List<StrategyPreset> getPresets()
	{
		String json = config.get(BATilesConfig.BA_TILES_CONFIG_GROUP, PRESETS_KEY);
		if (Strings.isNullOrEmpty(json))
		{
			return Collections.emptyList();
		}

		// CHECKSTYLE:OFF
		List<StrategyPreset> presets = gson.fromJson(json, new TypeToken<List<StrategyPreset>>(){}.getType());
		// CHECKSTYLE:ON
		return presets == null ? Collections.emptyList() : presets;
	}

	List<StrategyPreset> getPresets(int wave, String role)
	{
		return getPresets().stream()
				.filter(p -> p.getWave() == wave && p.getRole().equals(role))
				.collect(Collectors.toList());
	}

	Optional<StrategyPreset> getPreset(@Nullable String id)
	{
		return getPresets().stream().filter(p -> p.getId().equals(id)).findFirst();
	}

	private void savePresets(List<StrategyPreset> presets)
	{
		if (presets.isEmpty())
		{
			config.unset(BATilesConfig.BA_TILES_CONFIG_GROUP, PRESETS_KEY);
		}
		else
		{
			config.set(BATilesConfig.BA_TILES_CONFIG_GROUP, PRESETS_KEY, gson.toJson(presets));
		}
	}

	StrategyPreset createPreset(String name, int wave, String role)
	{
		StrategyPreset preset = new StrategyPreset(UUID.randomUUID().toString(), name, wave, role);
		List<StrategyPreset> presets = new ArrayList<>(getPresets());
		presets.add(preset);
		savePresets(presets);
		fireChanged();
		return preset;
	}

	/**
	 * Adds presets that are not already stored (matched by id), e.g. presets that arrived with an import.
	 *
	 * @return the number of presets added
	 */
	int addPresetsIfAbsent(Collection<StrategyPreset> toAdd)
	{
		List<StrategyPreset> presets = new ArrayList<>(getPresets());
		int added = 0;
		for (StrategyPreset preset : toAdd)
		{
			if (presets.stream().noneMatch(p -> p.getId().equals(preset.getId())))
			{
				presets.add(preset);
				added++;
			}
		}

		if (added > 0)
		{
			savePresets(presets);
			fireChanged();
		}
		return added;
	}

	void renamePreset(StrategyPreset preset, String name)
	{
		List<StrategyPreset> presets = getPresets().stream()
				.map(p -> p.getId().equals(preset.getId()) ? p.withName(name) : p)
				.collect(Collectors.toList());
		savePresets(presets);
		fireChanged();
	}

	/**
	 * Deletes a preset along with all of its tiles in the given regions.
	 */
	void deletePreset(StrategyPreset preset, int[] regionIds)
	{
		for (int regionId : regionIds)
		{
			List<GroundMarkerPoint> points = getPoints(regionId);
			List<GroundMarkerPoint> kept = points.stream()
					.filter(p -> !preset.getId().equals(p.getPresetId()))
					.collect(Collectors.toList());
			if (kept.size() != points.size())
			{
				writePoints(regionId, kept);
			}
		}

		List<StrategyPreset> presets = getPresets().stream()
				.filter(p -> !p.getId().equals(preset.getId()))
				.collect(Collectors.toList());
		savePresets(presets);

		// the preset's wave simply has no preset in lineups that used it
		saveLineups(getLineups().stream()
				.map(l -> l.getPresetIds().containsValue(preset.getId())
						? l.withPresetIds(without(l.getPresetIds(), preset.getId())) : l)
				.collect(Collectors.toList()));

		if (preset.getId().equals(getActivePresetId(preset.getWave(), preset.getRole())))
		{
			config.unset(BATilesConfig.BA_TILES_CONFIG_GROUP, activePresetKey(preset.getWave(), preset.getRole()));
		}
		fireChanged();
	}

	// ---- active preset selection ----

	private static String activePresetKey(int wave, String role)
	{
		return ACTIVE_PRESET_PREFIX + wave + "_" + role;
	}

	/**
	 * @return the id of the active preset for the wave and role, or null if tiles are shown without a preset
	 */
	@Nullable
	String getActivePresetId(int wave, String role)
	{
		return Strings.emptyToNull(config.get(BATilesConfig.BA_TILES_CONFIG_GROUP, activePresetKey(wave, role)));
	}

	/**
	 * Makes the preset the active one for the wave and role (or none, for null). Changing a wave's preset this way
	 * means the role no longer follows a lineup.
	 */
	void setActivePreset(int wave, String role, @Nullable StrategyPreset preset)
	{
		if (Objects.equals(preset == null ? null : preset.getId(), getActivePresetId(wave, role)))
		{
			return;
		}

		writeActivePreset(wave, role, preset == null ? null : preset.getId());
		config.unset(BATilesConfig.BA_TILES_CONFIG_GROUP, activeLineupKey(role));
		fireChanged();
	}

	private void writeActivePreset(int wave, String role, @Nullable String presetId)
	{
		if (presetId == null)
		{
			config.unset(BATilesConfig.BA_TILES_CONFIG_GROUP, activePresetKey(wave, role));
		}
		else
		{
			config.set(BATilesConfig.BA_TILES_CONFIG_GROUP, activePresetKey(wave, role), presetId);
		}
	}

	// ---- non-preset tiles while a preset is active ----

	private static String showBaseWithPresetKey(int wave, String role)
	{
		return SHOW_BASE_WITH_PRESET_PREFIX + wave + "_" + role;
	}

	/**
	 * @return whether tiles that are not part of a preset are shown for the wave and role while it has an active
	 *         preset (they are always shown while it has none). Defaults to true.
	 */
	boolean isShowBaseWithPreset(int wave, String role)
	{
		String value = config.get(BATilesConfig.BA_TILES_CONFIG_GROUP, showBaseWithPresetKey(wave, role));
		return value == null || Boolean.parseBoolean(value);
	}

	void setShowBaseWithPreset(int wave, String role, boolean show)
	{
		if (show == isShowBaseWithPreset(wave, role))
		{
			return;
		}

		if (show)
		{
			config.unset(BATilesConfig.BA_TILES_CONFIG_GROUP, showBaseWithPresetKey(wave, role));
		}
		else
		{
			config.set(BATilesConfig.BA_TILES_CONFIG_GROUP, showBaseWithPresetKey(wave, role), "false");
		}
		fireChanged();
	}

	// ---- ground markers ----

	/**
	 * @return the Ground Markers plugin's markers stored for the region
	 */
	List<GroundMarkerPoint> getGroundMarkers(int regionId)
	{
		String json = config.get(GroundMarkerImport.GROUND_MARKER_CONFIG_GROUP, REGION_PREFIX + regionId);
		return Strings.isNullOrEmpty(json) ? Collections.emptyList() : GroundMarkerImport.parse(gson, json);
	}

	/**
	 * Adds a BA Tile, shown on all waves for all roles, for each of the given Ground Markers markers that does not
	 * already have one. The ground markers themselves are left untouched.
	 *
	 * @return the number of BA Tiles added
	 */
	int convertGroundMarkers(Collection<GroundMarkerPoint> markers)
	{
		int added = 0;
		for (int regionId : markers.stream().map(GroundMarkerPoint::getRegionId).distinct().collect(Collectors.toList()))
		{
			List<GroundMarkerPoint> points = new ArrayList<>(getPoints(regionId));
			List<GroundMarkerPoint> tiles = GroundMarkerImport.toBaTiles(
					markers.stream().filter(m -> m.getRegionId() == regionId).collect(Collectors.toList()), points);
			if (!tiles.isEmpty())
			{
				points.addAll(tiles);
				writePoints(regionId, points);
				added += tiles.size();
			}
		}

		if (added > 0)
		{
			fireChanged();
		}
		return added;
	}

	// ---- full backup ----

	/**
	 * @return every key BA Tiles stores in the current profile, with its value
	 */
	BATilesBackup exportAll()
	{
		Map<String, String> entries = new HashMap<>();
		for (String key : config.keys(BATilesConfig.BA_TILES_CONFIG_GROUP))
		{
			String value = config.get(BATilesConfig.BA_TILES_CONFIG_GROUP, key);
			if (value != null)
			{
				entries.put(key, value);
			}
		}
		return BATilesBackup.of(entries);
	}

	/**
	 * Replaces everything BA Tiles stores in the current profile with the backup's contents.
	 */
	void replaceAll(BATilesBackup backup)
	{
		for (String key : backup.keysToRemove(exportAll().getEntries().keySet()))
		{
			config.unset(BATilesConfig.BA_TILES_CONFIG_GROUP, key);
		}
		for (Map.Entry<String, String> entry : backup.getEntries().entrySet())
		{
			config.set(BATilesConfig.BA_TILES_CONFIG_GROUP, entry.getKey(), entry.getValue());
		}
		fireChanged();
	}

	// ---- lineups ----

	List<Lineup> getLineups()
	{
		String json = config.get(BATilesConfig.BA_TILES_CONFIG_GROUP, LINEUPS_KEY);
		if (Strings.isNullOrEmpty(json))
		{
			return Collections.emptyList();
		}

		// CHECKSTYLE:OFF
		List<Lineup> lineups = gson.fromJson(json, new TypeToken<List<Lineup>>(){}.getType());
		// CHECKSTYLE:ON
		return lineups == null ? Collections.emptyList() : lineups;
	}

	List<Lineup> getLineups(String role)
	{
		return getLineups().stream().filter(l -> l.getRole().equals(role)).collect(Collectors.toList());
	}

	Optional<Lineup> getLineup(@Nullable String id)
	{
		return getLineups().stream().filter(l -> l.getId().equals(id)).findFirst();
	}

	/**
	 * @return whether the role already has a lineup with this name (ignoring case)
	 */
	boolean hasLineupNamed(String role, String name)
	{
		return getLineups(role).stream().anyMatch(l -> l.getName().equalsIgnoreCase(name));
	}

	private void saveLineups(List<Lineup> lineups)
	{
		if (lineups.isEmpty())
		{
			config.unset(BATilesConfig.BA_TILES_CONFIG_GROUP, LINEUPS_KEY);
		}
		else
		{
			config.set(BATilesConfig.BA_TILES_CONFIG_GROUP, LINEUPS_KEY, gson.toJson(lineups));
		}
	}

	/**
	 * Saves the role's currently active presets, for every wave, as a new lineup, which becomes the role's active
	 * lineup (it matches what is active).
	 */
	Lineup saveCurrentAsLineup(String name, String role)
	{
		Map<Integer, String> presetIds = new TreeMap<>();
		for (int wave = 1; wave <= WAVES; wave++)
		{
			String presetId = getActivePresetId(wave, role);
			if (presetId != null)
			{
				presetIds.put(wave, presetId);
			}
		}
		Lineup lineup = addLineup(name, role, presetIds);
		config.set(BATilesConfig.BA_TILES_CONFIG_GROUP, activeLineupKey(role), lineup.getId());
		fireChanged();
		return lineup;
	}

	/**
	 * Stores a new lineup without applying it.
	 */
	Lineup addLineup(String name, String role, Map<Integer, String> presetIds)
	{
		Lineup lineup = new Lineup(UUID.randomUUID().toString(), name, role, new TreeMap<>(presetIds));
		List<Lineup> lineups = new ArrayList<>(getLineups());
		lineups.add(lineup);
		saveLineups(lineups);
		fireChanged();
		return lineup;
	}

	/**
	 * Stores the lineup, keeping its id, unless a lineup with that id already exists.
	 */
	void addLineupIfAbsent(Lineup lineup)
	{
		if (getLineup(lineup.getId()).isPresent())
		{
			return;
		}
		List<Lineup> lineups = new ArrayList<>(getLineups());
		lineups.add(lineup);
		saveLineups(lineups);
		fireChanged();
	}

	void renameLineup(Lineup lineup, String name)
	{
		saveLineups(getLineups().stream()
				.map(l -> l.getId().equals(lineup.getId()) ? l.withName(name) : l)
				.collect(Collectors.toList()));
		fireChanged();
	}

	/**
	 * Deletes the lineup. The presets it made active stay active.
	 */
	void deleteLineup(Lineup lineup)
	{
		saveLineups(getLineups().stream()
				.filter(l -> !l.getId().equals(lineup.getId()))
				.collect(Collectors.toList()));
		if (lineup.getId().equals(getActiveLineupId(lineup.getRole())))
		{
			config.unset(BATilesConfig.BA_TILES_CONFIG_GROUP, activeLineupKey(lineup.getRole()));
		}
		fireChanged();
	}

	/**
	 * Makes the lineup's presets active for its role on every wave (waves it has no preset for get none), and makes
	 * it the role's active lineup.
	 */
	void applyLineup(Lineup lineup)
	{
		for (int wave = 1; wave <= WAVES; wave++)
		{
			String presetId = lineup.getPresetIds().get(wave);
			// a lineup can outlive a preset it names only through hand-edited config; treat that wave as empty
			writeActivePreset(wave, lineup.getRole(), getPreset(presetId).isPresent() ? presetId : null);
		}
		config.set(BATilesConfig.BA_TILES_CONFIG_GROUP, activeLineupKey(lineup.getRole()), lineup.getId());
		fireChanged();
	}

	/**
	 * Stops following a lineup for the role, leaving its active presets as they are.
	 */
	void clearActiveLineup(String role)
	{
		if (getActiveLineupId(role) != null)
		{
			config.unset(BATilesConfig.BA_TILES_CONFIG_GROUP, activeLineupKey(role));
			fireChanged();
		}
	}

	/**
	 * @return the id of the lineup the role follows, or null once any of its waves' presets was changed by hand
	 */
	@Nullable
	String getActiveLineupId(String role)
	{
		String id = Strings.emptyToNull(config.get(BATilesConfig.BA_TILES_CONFIG_GROUP, activeLineupKey(role)));
		return getLineup(id).isPresent() ? id : null;
	}

	private static String activeLineupKey(String role)
	{
		return ACTIVE_LINEUP_PREFIX + role;
	}

	private static Map<Integer, String> without(Map<Integer, String> presetIds, String presetId)
	{
		Map<Integer, String> kept = new TreeMap<>(presetIds);
		kept.values().removeIf(presetId::equals);
		return kept;
	}

	// ---- BA Utilities ----

	/**
	 * @return BA Utilities' saved tile setup on the current profile (its raw JSON), or null if it has none
	 */
	@Nullable
	String getBaUtilitiesSetupJson()
	{
		return Strings.emptyToNull(config.get(BaUtilitiesData.CONFIG_GROUP, BaUtilitiesData.STORE_KEY));
	}
}
