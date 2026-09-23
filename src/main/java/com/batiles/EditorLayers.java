package com.batiles;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * What the tile map editor shows and edits for one wave / role: the tiles of the active preset (if any) and the tiles
 * that are not part of any preset. One of the two is the layer being edited; the other is shown dimmed for context.
 */
@Value
class EditorLayers
{
	int wave;
	String role;
	/**
	 * The active preset for the wave and role, or null if none is active.
	 */
	@Nullable
	String activePresetId;
	/**
	 * Whether the preset layer (rather than the non-preset layer) is being edited. Ignored without an active preset.
	 */
	boolean editingPreset;

	ArenaMapLayout layout()
	{
		return ArenaMapLayout.fromWave(wave);
	}

	boolean isEditingPreset()
	{
		return editingPreset && activePresetId != null;
	}

	/**
	 * @return whether the point belongs on this wave / role's map, in either layer
	 */
	boolean isShown(GroundMarkerPoint point)
	{
		return isPresetLayer(point) || isBaseLayer(point);
	}

	/**
	 * @return whether the point is in the layer being edited
	 */
	boolean isEditable(GroundMarkerPoint point)
	{
		return isEditingPreset() ? isPresetLayer(point) : isBaseLayer(point);
	}

	private boolean isPresetLayer(GroundMarkerPoint point)
	{
		return activePresetId != null && activePresetId.equals(point.getPresetId()) && layout().contains(point);
	}

	private boolean isBaseLayer(GroundMarkerPoint point)
	{
		return !point.isPresetTile()
				&& layout().contains(point)
				&& (point.getWaves() == null || point.getWaves().contains(wave))
				&& (point.getRoles() == null || point.getRoles().contains(role));
	}

	List<ArenaMapPanel.MapMarker> mapMarkers(Collection<GroundMarkerPoint> points, @Nullable GroundMarkerPoint selected)
	{
		List<ArenaMapPanel.MapMarker> markers = new ArrayList<>();
		for (GroundMarkerPoint point : points)
		{
			if (isShown(point))
			{
				boolean editable = isEditable(point);
				markers.add(new ArenaMapPanel.MapMarker(point, !editable, editable && point.equals(selected)));
			}
		}
		return markers;
	}

	/**
	 * @return the editable points on the given map tile, in stored order
	 */
	List<GroundMarkerPoint> editableAt(Collection<GroundMarkerPoint> points, int mapX, int mapY)
	{
		return points.stream()
				.filter(p -> layout().isAt(p, mapX, mapY) && isEditable(p))
				.collect(Collectors.toList());
	}

	/**
	 * Creates a new point on the given map tile in the layer being edited.
	 * A non-preset tile starts out shown only for this wave and role; that can be widened afterwards.
	 */
	GroundMarkerPoint newPoint(int mapX, int mapY, Color color)
	{
		ArenaMapLayout layout = layout();
		return new GroundMarkerPoint(layout.getRegionId(), layout.toRegionX(mapX), layout.toRegionY(mapY), 0, color, null,
				List.of(wave), List.of(role), isEditingPreset() ? activePresetId : null);
	}
}
