package com.batiles;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

/**
 * Which map tiles belong to the arena, and so can be marked on the map: walkable tiles, and unwalkable tiles the arena
 * encloses (cannons, rocks, cones, ...). Everything from the perimeter outwards does not.
 */
final class ArenaArea
{
	private static final int WIDTH = BaArenaMapTopology.WIDTH;
	private static final int HEIGHT = BaArenaMapTopology.HEIGHT;
	private static final Map<ArenaMapLayout, BitSet> AREAS = new EnumMap<>(ArenaMapLayout.class);

	private ArenaArea()
	{
	}

	static boolean contains(ArenaMapLayout layout, int mapX, int mapY)
	{
		return BaArenaMapTopology.contains(mapX, mapY) && area(layout).get(index(mapX, mapY));
	}

	private static synchronized BitSet area(ArenaMapLayout layout)
	{
		return AREAS.computeIfAbsent(layout, ArenaArea::compute);
	}

	/**
	 * Everything not reachable from the map's edge through unwalkable tiles.
	 */
	private static BitSet compute(ArenaMapLayout layout)
	{
		BitSet outside = new BitSet(WIDTH * HEIGHT);
		Deque<int[]> queue = new ArrayDeque<>();
		for (int x = 0; x < WIDTH; x++)
		{
			queue.add(new int[]{x, 0});
			queue.add(new int[]{x, HEIGHT - 1});
		}
		for (int y = 0; y < HEIGHT; y++)
		{
			queue.add(new int[]{0, y});
			queue.add(new int[]{WIDTH - 1, y});
		}

		while (!queue.isEmpty())
		{
			int[] tile = queue.poll();
			int x = tile[0];
			int y = tile[1];
			if (!BaArenaMapTopology.contains(x, y) || outside.get(index(x, y)) || BaArenaMapTopology.isUsableTile(layout, x, y))
			{
				continue;
			}
			outside.set(index(x, y));
			queue.add(new int[]{x + 1, y});
			queue.add(new int[]{x - 1, y});
			queue.add(new int[]{x, y + 1});
			queue.add(new int[]{x, y - 1});
		}

		BitSet area = new BitSet(WIDTH * HEIGHT);
		area.set(0, WIDTH * HEIGHT);
		area.andNot(outside);
		return area;
	}

	private static int index(int x, int y)
	{
		return y * WIDTH + x;
	}
}
