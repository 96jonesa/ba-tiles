package com.batiles;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * The clipboard format for sharing BA Tiles.
 *
 * <p>Tiles without presets are exported as a bare JSON array of tiles, the format older versions of the plugin read
 * and write. When any exported tile belongs to a strategy preset, the export is an object holding both the tiles and
 * the presets they belong to, so that the presets arrive together with their tiles.
 */
@Value
class TileExport
{
	List<GroundMarkerPoint> tiles;
	List<StrategyPreset> presets;

	/**
	 * @param allPresets every known preset; only those referenced by {@code tiles} are included
	 */
	static TileExport of(List<GroundMarkerPoint> tiles, Collection<StrategyPreset> allPresets)
	{
		Set<String> referenced = tiles.stream()
				.map(GroundMarkerPoint::getPresetId)
				.filter(id -> id != null)
				.collect(Collectors.toSet());
		List<StrategyPreset> presets = allPresets.stream()
				.filter(p -> referenced.contains(p.getId()))
				.collect(Collectors.toList());
		return new TileExport(tiles, presets);
	}

	String toJson(Gson gson)
	{
		return presets.isEmpty() ? gson.toJson(tiles) : gson.toJson(this);
	}

	/**
	 * Parses either export format. Preset tiles whose preset is not part of the import are dropped, since they could
	 * never be shown.
	 *
	 * @throws JsonParseException if the text is not a BA Tiles export
	 */
	static TileExport fromJson(Gson gson, String json)
	{
		JsonElement root = gson.fromJson(json, JsonElement.class);
		if (root == null)
		{
			throw new JsonParseException("Empty BA Tiles export");
		}
		if (root.isJsonArray())
		{
			// CHECKSTYLE:OFF
			List<GroundMarkerPoint> tiles = gson.fromJson(root, new TypeToken<List<GroundMarkerPoint>>(){}.getType());
			// CHECKSTYLE:ON
			return new TileExport(orEmpty(tiles).stream().filter(t -> !t.isPresetTile()).collect(Collectors.toList()),
					Collections.emptyList());
		}

		if (!root.isJsonObject())
		{
			throw new JsonParseException("Not a BA Tiles export");
		}

		TileExport export = gson.fromJson(root, TileExport.class);
		List<StrategyPreset> presets = orEmpty(export.presets).stream()
				.filter(p -> p.getId() != null && p.getName() != null && p.getRole() != null)
				.collect(Collectors.toList());
		Set<String> presetIds = presets.stream().map(StrategyPreset::getId).collect(Collectors.toSet());
		List<GroundMarkerPoint> tiles = orEmpty(export.tiles).stream()
				.filter(t -> !t.isPresetTile() || presetIds.contains(t.getPresetId()))
				.collect(Collectors.toList());
		return new TileExport(tiles, presets);
	}

	private static <T> List<T> orEmpty(List<T> list)
	{
		return list == null ? Collections.emptyList() : list;
	}
}
