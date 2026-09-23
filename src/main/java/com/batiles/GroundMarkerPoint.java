package com.batiles;

import java.awt.Color;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

/**
 * Used for serialization of ground marker points.
 */
@Value
@With
@AllArgsConstructor
@EqualsAndHashCode()
class GroundMarkerPoint
{
    private int regionId;
    private int regionX;
    private int regionY;
    private int z;
    @Nullable
    private Color color;
    @Nullable
    private String label;
    @Nullable
    private List<Integer> waves;
    @Nullable
    private List<String> roles;
    /**
     * Id of the {@link StrategyPreset} this tile belongs to, or null for a tile that is not part of any preset.
     * A preset tile is only shown while its preset is the active preset for the preset's wave and role.
     */
    @Nullable
    private String presetId;

    GroundMarkerPoint(int regionId, int regionX, int regionY, int z, @Nullable Color color, @Nullable String label,
                      @Nullable List<Integer> waves, @Nullable List<String> roles)
    {
        this(regionId, regionX, regionY, z, color, label, waves, roles, null);
    }

    boolean isPresetTile()
    {
        return presetId != null;
    }

    boolean isAt(int regionId, int regionX, int regionY, int z)
    {
        return this.regionId == regionId && this.regionX == regionX && this.regionY == regionY && this.z == z;
    }
}
