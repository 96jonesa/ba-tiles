package com.batiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class EditorLayersTest
{
	// map tile (30, 20) is region tile (30, 28)
	private static final GroundMarkerPoint BASE_W3_D = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, null, List.of(3), List.of("d"));
	private static final GroundMarkerPoint BASE_ALL = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, null, null, null);
	private static final GroundMarkerPoint BASE_W4 = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, null, List.of(4), List.of("d"));
	private static final GroundMarkerPoint PRESET_P1 = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, null, List.of(3), List.of("d"), "p1");
	private static final GroundMarkerPoint PRESET_P2 = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, null, List.of(3), List.of("d"), "p2");
	private static final GroundMarkerPoint WAVE_10_TILE = new GroundMarkerPoint(7508, 30, 28, 0, Color.RED, null, List.of(10), List.of("d"));
	private static final List<GroundMarkerPoint> ALL = List.of(BASE_W3_D, BASE_ALL, BASE_W4, PRESET_P1, PRESET_P2, WAVE_10_TILE);

	public static class IsShown
	{
		@Test
		public void showsBaseTilesOfTheWaveAndRoleAndTheActivePresetsTiles()
		{
			EditorLayers layers = new EditorLayers(3, "d", "p1", true);
			assertTrue(layers.isShown(BASE_W3_D));
			assertTrue(layers.isShown(BASE_ALL));
			assertTrue(layers.isShown(PRESET_P1));
			assertFalse(layers.isShown(BASE_W4));
			assertFalse(layers.isShown(PRESET_P2));
			assertFalse(layers.isShown(WAVE_10_TILE));
		}

		@Test
		public void waveTenShowsOnlyTheWaveTenRegion()
		{
			EditorLayers layers = new EditorLayers(10, "d", null, false);
			assertTrue(layers.isShown(WAVE_10_TILE));
			assertFalse(layers.isShown(BASE_ALL));
		}
	}

	public static class IsEditable
	{
		@Test
		public void editingPresetMakesOnlyPresetTilesEditable()
		{
			EditorLayers layers = new EditorLayers(3, "d", "p1", true);
			assertTrue(layers.isEditable(PRESET_P1));
			assertFalse(layers.isEditable(BASE_W3_D));
		}

		@Test
		public void editingBaseMakesOnlyBaseTilesEditable()
		{
			EditorLayers layers = new EditorLayers(3, "d", "p1", false);
			assertFalse(layers.isEditable(PRESET_P1));
			assertTrue(layers.isEditable(BASE_W3_D));
		}

		@Test
		public void withoutActivePresetBaseTilesAreEdited()
		{
			EditorLayers layers = new EditorLayers(3, "d", null, true);
			assertFalse(layers.isEditingPreset());
			assertTrue(layers.isEditable(BASE_W3_D));
		}
	}

	public static class MapMarkers
	{
		@Test
		public void dimsTheLayerNotBeingEditedAndMarksTheSelection()
		{
			EditorLayers layers = new EditorLayers(3, "d", "p1", true);
			List<ArenaMapPanel.MapMarker> markers = layers.mapMarkers(ALL, PRESET_P1);
			assertEquals(List.of(
					new ArenaMapPanel.MapMarker(BASE_W3_D, true, false),
					new ArenaMapPanel.MapMarker(BASE_ALL, true, false),
					new ArenaMapPanel.MapMarker(PRESET_P1, false, true)), markers);
		}
	}

	public static class EditableAt
	{
		@Test
		public void returnsEditableTilesOnTheMapTile()
		{
			EditorLayers layers = new EditorLayers(3, "d", "p1", false);
			assertEquals(List.of(BASE_W3_D, BASE_ALL), layers.editableAt(ALL, 30, 20));
			assertEquals(List.of(), layers.editableAt(ALL, 31, 20));
		}
	}

	public static class NewPoint
	{
		@Test
		public void newBaseTileIsForThisWaveAndRoleOnly()
		{
			GroundMarkerPoint point = new EditorLayers(3, "d", "p1", false).newPoint(30, 20, Color.BLUE);
			assertEquals(new GroundMarkerPoint(7509, 30, 28, 0, Color.BLUE, null, List.of(3), List.of("d"), null), point);
		}

		@Test
		public void newPresetTileBelongsToTheActivePreset()
		{
			GroundMarkerPoint point = new EditorLayers(3, "d", "p1", true).newPoint(30, 20, Color.BLUE);
			assertEquals("p1", point.getPresetId());
		}

		@Test
		public void waveTenTilesGoInTheWaveTenRegion()
		{
			GroundMarkerPoint point = new EditorLayers(10, "a", null, true).newPoint(30, 20, Color.BLUE);
			assertEquals(7508, point.getRegionId());
			assertNull(point.getPresetId());
		}
	}
}
