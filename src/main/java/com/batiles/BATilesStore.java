package com.batiles;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

	private final ConfigManager configManager;
	private final Gson gson;
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

	@Inject
	BATilesStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
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
		String json = configManager.getConfiguration(BATilesConfig.BA_TILES_CONFIG_GROUP, REGION_PREFIX + regionId);
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
			configManager.unsetConfiguration(BATilesConfig.BA_TILES_CONFIG_GROUP, REGION_PREFIX + regionId);
		}
		else
		{
			configManager.setConfiguration(BATilesConfig.BA_TILES_CONFIG_GROUP, REGION_PREFIX + regionId, gson.toJson(points));
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
		String json = configManager.getConfiguration(BATilesConfig.BA_TILES_CONFIG_GROUP, PRESETS_KEY);
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
			configManager.unsetConfiguration(BATilesConfig.BA_TILES_CONFIG_GROUP, PRESETS_KEY);
		}
		else
		{
			configManager.setConfiguration(BATilesConfig.BA_TILES_CONFIG_GROUP, PRESETS_KEY, gson.toJson(presets));
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

		if (preset.getId().equals(getActivePresetId(preset.getWave(), preset.getRole())))
		{
			configManager.unsetConfiguration(BATilesConfig.BA_TILES_CONFIG_GROUP, activePresetKey(preset.getWave(), preset.getRole()));
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
		return Strings.emptyToNull(configManager.getConfiguration(BATilesConfig.BA_TILES_CONFIG_GROUP, activePresetKey(wave, role)));
	}

	void setActivePreset(int wave, String role, @Nullable StrategyPreset preset)
	{
		if (Objects.equals(preset == null ? null : preset.getId(), getActivePresetId(wave, role)))
		{
			return;
		}

		if (preset == null)
		{
			configManager.unsetConfiguration(BATilesConfig.BA_TILES_CONFIG_GROUP, activePresetKey(wave, role));
		}
		else
		{
			configManager.setConfiguration(BATilesConfig.BA_TILES_CONFIG_GROUP, activePresetKey(wave, role), preset.getId());
		}
		fireChanged();
	}
}
