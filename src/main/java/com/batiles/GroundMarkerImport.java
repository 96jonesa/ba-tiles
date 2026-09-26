package com.batiles;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Converts markers of RuneLite's Ground Markers plugin in the Barbarian Assault arena into BA Tiles.
 *
 * <p>Ground Markers stores its markers in the same shape as {@link GroundMarkerPoint} (region, region x/y, plane,
 * color and label), in its own config group, keyed by region.
 */
final class GroundMarkerImport
{
	/**
	 * The Ground Markers plugin's config group; its markers are stored under "region_" + region id.
	 */
	static final String GROUND_MARKER_CONFIG_GROUP = "groundMarker";

	private GroundMarkerImport()
	{
	}

	/**
	 * Parses the markers the Ground Markers plugin stores for one region.
	 */
	static List<GroundMarkerPoint> parse(Gson gson, String json)
	{
		// CHECKSTYLE:OFF
		List<GroundMarkerPoint> markers = gson.fromJson(json, new TypeToken<List<GroundMarkerPoint>>(){}.getType());
		// CHECKSTYLE:ON
		return markers == null ? Collections.emptyList() : markers;
	}

	/**
	 * @param groundMarkers markers read from the Ground Markers plugin
	 * @param existing BA Tiles already stored for the same region
	 * @return new BA Tiles, shown on all waves for all roles with the markers' colors and labels, for every marker
	 *         that does not already have such a tile (so that importing again adds nothing)
	 */
	static List<GroundMarkerPoint> toBaTiles(Collection<GroundMarkerPoint> groundMarkers, Collection<GroundMarkerPoint> existing)
	{
		List<GroundMarkerPoint> known = new ArrayList<>(existing);
		List<GroundMarkerPoint> added = new ArrayList<>();
		for (GroundMarkerPoint marker : groundMarkers)
		{
			if (marker == null)
			{
				continue;
			}

			GroundMarkerPoint tile = new GroundMarkerPoint(marker.getRegionId(), marker.getRegionX(), marker.getRegionY(),
					marker.getZ(), marker.getColor(), marker.getLabel(), BATilesPlugin.ALL_WAVES, BATilesPlugin.ALL_ROLES);
			if (known.stream().noneMatch(k -> isSameMarker(k, tile)))
			{
				known.add(tile);
				added.add(tile);
			}
		}
		return added;
	}

	private static boolean isSameMarker(GroundMarkerPoint a, GroundMarkerPoint b)
	{
		return !a.isPresetTile()
				&& a.isAt(b.getRegionId(), b.getRegionX(), b.getRegionY(), b.getZ())
				&& Objects.equals(a.getColor(), b.getColor())
				&& Objects.equals(a.getLabel(), b.getLabel())
				&& shownEverywhere(a);
	}

	private static boolean shownEverywhere(GroundMarkerPoint point)
	{
		return (point.getWaves() == null || point.getWaves().containsAll(BATilesPlugin.ALL_WAVES))
				&& (point.getRoles() == null || point.getRoles().containsAll(BATilesPlugin.ALL_ROLES));
	}
}
