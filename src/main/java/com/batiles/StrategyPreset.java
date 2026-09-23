package com.batiles;

import lombok.Value;
import lombok.With;

/**
 * A named set of BA Tiles for a single wave / role combination.
 * The tiles themselves are {@link GroundMarkerPoint}s whose presetId is this preset's id.
 */
@Value
@With
class StrategyPreset
{
    private String id;
    private String name;
    private int wave;
    private String role;
}
