/*
 * Adapted from BA Utilities (https://github.com/tcourter1/ba-healer-order, commit 676d4d7; mirrors of its tile marker model, for reading its saved config).
 *
 * Copyright (c) 2026, tcourter1
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.batiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Mirrors of BA Utilities' tile marker model, with the same field names, so its saved config
 * ({@code bahealerorder.generalTileMarkerStore}) can be read with Gson. Only the parts that describe tiles, strategies
 * and their wave / role assignments are mirrored.
 */
final class BaUtilitiesData
{
	static final String CONFIG_GROUP = "bahealerorder";
	static final String STORE_KEY = "generalTileMarkerStore";

	private BaUtilitiesData()
	{
	}

	/**
	 * Arena maps; BA Utilities stores these by name.
	 */
	enum WaveMap
	{
		WAVES_1_TO_9,
		WAVE_10;

		static WaveMap fromWave(int wave)
		{
			return wave == 10 ? WAVE_10 : WAVES_1_TO_9;
		}

		/**
		 * @return the map, WAVES_1_TO_9 for null (as BA Utilities), or null for an unknown name
		 */
		static WaveMap fromName(String name)
		{
			if (name == null)
			{
				return WAVES_1_TO_9;
			}
			for (WaveMap map : values())
			{
				if (map.name().equals(name))
				{
					return map;
				}
			}
			return null;
		}
	}

	/**
	 * Only used to build the built-in sets; its value does not matter for BA Tiles.
	 */
	enum MapMode
	{
		FULL_MAP,
		EAST_SIDE_ONLY
	}

	/**
	 * Who a wave selection or assignment preset is for. GLOBAL applies to every role.
	 */
	enum RoleContext
	{
		DEFENDER,
		COLLECTOR,
		HEALER,
		ATTACKER,
		GLOBAL;

		/**
		 * @return the context, DEFENDER for null or unknown names (as BA Utilities)
		 */
		static RoleContext fromName(String name)
		{
			if (name != null)
			{
				for (RoleContext context : values())
				{
					if (context.name().equals(name))
					{
						return context;
					}
				}
			}
			return DEFENDER;
		}

		/**
		 * @return the BA Tiles role code, or null for GLOBAL
		 */
		String roleCode()
		{
			switch (this)
			{
				case DEFENDER:
					return BARole.DEFENDER.getCode();
				case COLLECTOR:
					return BARole.COLLECTOR.getCode();
				case HEALER:
					return BARole.HEALER.getCode();
				case ATTACKER:
					return BARole.ATTACKER.getCode();
				default:
					return null;
			}
		}
	}

	@Getter
	@NoArgsConstructor
	static class Store
	{
		private List<MarkerSet> markerSets = new ArrayList<>();
		private List<WaveSelection> waveSelections = new ArrayList<>();
		private List<StrategyPreset> strategyPresets = new ArrayList<>();
		private List<AssignmentPreset> assignmentPresets = new ArrayList<>();
		private Map<String, String> activeAssignmentPresetIds = new HashMap<>();
	}

	@Getter
	@NoArgsConstructor
	static class Tile
	{
		private int regionId;
		private int regionX;
		private int regionY;
		private int z;

		Tile(int regionId, int regionX, int regionY, int z)
		{
			this.regionId = regionId;
			this.regionX = regionX;
			this.regionY = regionY;
			this.z = z;
		}
	}

	@Getter
	@NoArgsConstructor
	static class Marker
	{
		static final int DEFAULT_OPACITY_PERCENT = 22;
		static final float DEFAULT_BORDER_WIDTH = 1.0f;

		private String id;
		private Tile tile;
		private String name;
		private String label;
		private String color;
		private Integer opacityPercent;
		private Float borderWidth;

		Marker(String id, Tile tile, String name, String label, String color, Integer opacityPercent, Float borderWidth)
		{
			this.id = id;
			this.tile = tile;
			this.name = name;
			this.label = label;
			this.color = color;
			this.opacityPercent = opacityPercent;
			this.borderWidth = borderWidth;
		}

		int getOpacityPercentOrDefault()
		{
			return opacityPercent == null ? DEFAULT_OPACITY_PERCENT : opacityPercent;
		}

		float getBorderWidthOrDefault()
		{
			return borderWidth == null ? DEFAULT_BORDER_WIDTH : borderWidth;
		}
	}

	@Getter
	@NoArgsConstructor
	static class MarkerSet
	{
		private String id;
		private String name;
		private String mapMode;
		private String waveMap;
		private boolean builtIn;
		private List<Marker> markers = new ArrayList<>();

		MarkerSet(String id, String name, MapMode mapMode, WaveMap waveMap, List<Marker> markers, boolean builtIn)
		{
			this.id = id;
			this.name = name;
			this.mapMode = mapMode.name();
			this.waveMap = waveMap.name();
			this.markers = new ArrayList<>(markers);
			this.builtIn = builtIn;
		}

		WaveMap getWaveMapOrNull()
		{
			return WaveMap.fromName(waveMap);
		}
	}

	@Getter
	@NoArgsConstructor
	static class StrategyPreset
	{
		private String id;
		private String name;
		private String notes;
		private String waveMap;
		private boolean builtIn;
		private List<String> markerSetIds = new ArrayList<>();

		StrategyPreset(String id, String name, String notes, WaveMap waveMap, List<String> markerSetIds, boolean builtIn)
		{
			this.id = id;
			this.name = name;
			this.notes = notes;
			this.waveMap = waveMap.name();
			this.markerSetIds = new ArrayList<>(markerSetIds);
			this.builtIn = builtIn;
		}

		WaveMap getWaveMapOrNull()
		{
			return WaveMap.fromName(waveMap);
		}
	}

	@Getter
	@NoArgsConstructor
	static class WaveSelection
	{
		private String id;
		private String roleContext;
		private int wave;
		private String strategyId;

		RoleContext getRoleContextOrDefault()
		{
			return RoleContext.fromName(roleContext);
		}
	}

	@Getter
	@NoArgsConstructor
	static class AssignmentPreset
	{
		private String id;
		private String name;
		private String roleContext;
		private boolean builtIn;
		private Map<Integer, String> waveSelections = new HashMap<>();

		AssignmentPreset(String id, String name, RoleContext roleContext, Map<Integer, String> waveSelections, boolean builtIn)
		{
			this.id = id;
			this.name = name;
			this.roleContext = roleContext.name();
			this.waveSelections = new HashMap<>(waveSelections);
			this.builtIn = builtIn;
		}

		RoleContext getRoleContextOrDefault()
		{
			return RoleContext.fromName(roleContext);
		}
	}
}
