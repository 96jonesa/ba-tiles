package com.batiles;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Pins literals of the built-in sets transcribed from BA Utilities, so that a drift from the source fails here.
 */
public class BaUtilitiesBuiltInSetsTest
{
	private static BaUtilitiesData.MarkerSet set(String id)
	{
		return BaUtilitiesBuiltInSets.create().stream().filter(s -> s.getId().equals(id)).findFirst().orElseThrow();
	}

	@Test
	public void hasFifteenSetsWithThirtyTiles()
	{
		List<BaUtilitiesData.MarkerSet> sets = BaUtilitiesBuiltInSets.create();
		assertEquals(15, sets.size());
		assertEquals(30, sets.stream().mapToInt(s -> s.getMarkers().size()).sum());
	}

	@Test
	public void beginnerTrapFoodIsPinned()
	{
		BaUtilitiesData.Marker marker = set("built-in:tile-marker-set:beginner-tiles").getMarkers().get(0);
		assertEquals("built-in:marker:beginner:trap-food", marker.getId());
		assertEquals(7509, marker.getTile().getRegionId());
		assertEquals(45, marker.getTile().getRegionX());
		assertEquals(35, marker.getTile().getRegionY());
		assertEquals("4 good", marker.getLabel());
		assertEquals("#50aaff", marker.getColor());
		// a regular marker uses BA Utilities' default style
		assertEquals(22, marker.getOpacityPercentOrDefault());
		assertEquals(1.0f, marker.getBorderWidthOrDefault(), 0f);
	}

	@Test
	public void softMarkersArePinned()
	{
		BaUtilitiesData.Marker marker = set("built-in:tile-marker-set:hendi-triangle").getMarkers().get(2);
		assertEquals(46, marker.getTile().getRegionX());
		assertEquals(36, marker.getTile().getRegionY());
		assertEquals("#59d4fd", marker.getColor());
		assertEquals(Integer.valueOf(10), marker.getOpacityPercent());
		assertEquals(0.5f, marker.getBorderWidth(), 0f);
	}

	@Test
	public void waveTenSetsAreOnTheWaveTenRegion()
	{
		BaUtilitiesData.MarkerSet auk = set("built-in:tile-marker-set:auk-w10-tiles");
		assertEquals(BaUtilitiesData.WaveMap.WAVE_10, auk.getWaveMapOrNull());
		assertEquals(List.of(7508), auk.getMarkers().stream().map(m -> m.getTile().getRegionId()).distinct().collect(Collectors.toList()));
		assertEquals("3+1", auk.getMarkers().get(4).getLabel());
		assertEquals("#78ff87", auk.getMarkers().get(4).getColor());
	}
}
