package com.batiles;

import lombok.Value;

/**
 * A {@link StrategyPreset} as an item of a preset selector.
 */
@Value
class PresetOption
{
	static final String NO_PRESET = "No preset";

	StrategyPreset preset;

	@Override
	public String toString()
	{
		return preset.getName();
	}
}
