package com.batiles;

import java.util.Map;
import lombok.Value;
import lombok.With;

/**
 * A named choice of strategy preset for each wave, for one role. Applying it makes those presets active, and leaves
 * the role's other waves without a preset.
 */
@Value
@With
class Lineup
{
	String id;
	String name;
	/**
	 * Role code.
	 */
	String role;
	/**
	 * Wave to the id of that wave's preset; waves without an entry get no preset.
	 */
	Map<Integer, String> presetIds;

	@Override
	public String toString()
	{
		return name;
	}
}
