package com.batiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.google.gson.Gson;
import java.awt.Color;
import java.util.List;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

/**
 * Persistence of {@link GroundMarkerPoint} through the Gson RuneLite injects.
 */
public class GroundMarkerPointTest
{
	private static final Gson GSON = RuneLiteAPI.GSON;

	@Test
	public void tilesSavedByOlderVersionsLoadWithTheDefaultStyle()
	{
		String saved = "{\"regionId\":7509,\"regionX\":30,\"regionY\":28,\"z\":0,\"color\":\"#FFFF0000\","
				+ "\"label\":\"A\",\"waves\":[3],\"roles\":[\"d\"]}";
		GroundMarkerPoint point = GSON.fromJson(saved, GroundMarkerPoint.class);
		assertNull(point.getPresetId());
		assertNull(point.getFillOpacityPercent());
		assertNull(point.getBorderWidth());
		assertEquals(new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, "A", List.of(3), List.of("d")), point);
	}

	@Test
	public void ownFillAndBorderWidthRoundTrip()
	{
		GroundMarkerPoint point = new GroundMarkerPoint(7509, 30, 28, 0, Color.RED, "A", List.of(3), List.of("d"))
				.withFillOpacityPercent(10)
				.withBorderWidth(0.5f);
		assertEquals(point, GSON.fromJson(GSON.toJson(point), GroundMarkerPoint.class));
	}
}
