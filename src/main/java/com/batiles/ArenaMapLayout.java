/*
 * Adapted from BA Utilities (https://github.com/tcourter1/ba-healer-order, commit 676d4d7).
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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The two Barbarian Assault arena layouts, and the mapping between arena map coordinates and the instance template
 * coordinates BA Tiles are stored in (what {@link net.runelite.api.coords.WorldPoint#fromLocalInstance} returns).
 */
@Getter
@RequiredArgsConstructor
enum ArenaMapLayout
{
	WAVES_1_TO_9("Waves 1-9", 7509),
	WAVE_10("Wave 10", 7508);

	static final int WIDTH = 64;
	static final int HEIGHT = 48;
	private static final int REGION_Y_OFFSET = 8;
	static final int[] REGION_IDS = {WAVES_1_TO_9.regionId, WAVE_10.regionId};

	private final String displayName;
	private final int regionId;

	static ArenaMapLayout fromWave(int wave)
	{
		return wave == 10 ? WAVE_10 : WAVES_1_TO_9;
	}

	int toRegionX(int mapX)
	{
		return mapX;
	}

	int toRegionY(int mapY)
	{
		return mapY + REGION_Y_OFFSET;
	}

	int toMapX(GroundMarkerPoint point)
	{
		return point.getRegionX();
	}

	int toMapY(GroundMarkerPoint point)
	{
		return point.getRegionY() - REGION_Y_OFFSET;
	}

	boolean contains(GroundMarkerPoint point)
	{
		if (point == null || point.getRegionId() != regionId || point.getZ() != 0)
		{
			return false;
		}
		int x = toMapX(point);
		int y = toMapY(point);
		return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
	}

	boolean isAt(GroundMarkerPoint point, int mapX, int mapY)
	{
		return contains(point) && toMapX(point) == mapX && toMapY(point) == mapY;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
