package com.batiles;

import java.awt.Color;
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * Used to denote marked tiles and their colors.
 * Note: This is not used for serialization of ground markers; see {@link GroundMarkerPoint}
 */
@Value
class ColorTileMarker
{
    private WorldPoint worldPoint;
    @Nullable
    private Color color;
    @Nullable
    private String label;
    /**
     * See {@link GroundMarkerPoint#getFillOpacityPercent()}.
     */
    @Nullable
    private Integer fillOpacityPercent;
    /**
     * See {@link GroundMarkerPoint#getBorderWidth()}.
     */
    @Nullable
    private Float borderWidth;
}
