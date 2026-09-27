package com.batiles;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
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

    /**
     * Visible tiles that would look exactly the same on the same spot (same color, label, fill and border width) are
     * drawn once, e.g. a tile shown both as part of a preset and as a tile shown alongside it. Tiles that differ in any
     * way are all kept, and stack when drawn.
     *
     * @return the markers without such duplicates, in their original order
     */
    static List<ColorTileMarker> withoutDuplicates(Collection<ColorTileMarker> markers)
    {
        return new ArrayList<>(new LinkedHashSet<>(markers));
    }
}
