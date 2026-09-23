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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

class ArenaMapPanel extends JPanel
{
	static final Color TRAP_COLOR = new Color(190, 70, 70);
	static final Color LOGS_COLOR = new Color(210, 180, 80);
	static final Color HAMMER_COLOR = new Color(145, 105, 55);
	static final Color RANGER_CAVE_COLOR = new Color(0xFF, 0x50, 0x78);
	static final Color FIGHTER_CAVE_COLOR = new Color(0x79, 0x18, 0x2F);
	static final Color RUNNER_CAVE_COLOR = new Color(125, 125, 220);
	static final Color HEALER_CAVE_COLOR = new Color(75, 175, 125);
	static final Color QUEEN_TRAPDOOR_COLOR = new Color(120, 80, 45);
	static final Color DISABLED_TILE_COLOR = Color.BLACK;
	static final Color CANNON_HILL_COLOR = new Color(190, 160, 105);
	static final Color CANNON_COLOR = new Color(95, 95, 95);
	static final Color DISPENSER_COLOR = new Color(180, 180, 180);
	static final Color HORN_COLOR = new Color(205, 155, 60);
	static final Color START_TILE_COLOR = new Color(115, 190, 205);
	private static final Color SELECTED_MARKER_BORDER_COLOR = new Color(0, 175, 255);
	private static final Color DEFAULT_MARKER_COLOR = Color.YELLOW;
	private static final int FILL_ALPHA = 110;
	private static final int DIMMED_FILL_ALPHA = 35;
	private static final int DIMMED_BORDER_ALPHA = 110;

	private static final MapBounds EAST_SIDE_MAP_BOUNDS = new MapBounds(24, 50, 15, 43);
	private static final MapBounds FULL_ARENA_MAP_BOUNDS = new MapBounds(8, 56, 5, 43);
	private static final int WEST_CANNON_X = 21;
	private static final int WEST_CANNON_Y = 26;
	private static final int EAST_CANNON_X = 40;
	private static final int EAST_CANNON_Y = 26;
	private static final int WEST_CANNON_HILL_X = 19;
	private static final int EAST_CANNON_HILL_X = 38;
	private static final int CANNON_HILL_Y = 24;
	private static final int HORN_OF_GLORY_X = 52;
	private static final int HORN_OF_GLORY_Y = 10;
	private static final int PRIORITY_CHUNK_SIZE = 8;
	private static final int PRIORITY_CHUNK_ORIGIN_X = 40;
	private static final int PRIORITY_CHUNK_ORIGIN_Y = 40;

	private final Supplier<ArenaMapLayout> layoutSupplier;
	private final Supplier<View> mapModeSupplier;
	private final Supplier<List<MapMarker>> markersSupplier;
	private final java.util.function.IntSupplier tileSizeSupplier;
	private final TileClickHandler tileClicked;

	ArenaMapPanel(
			Supplier<ArenaMapLayout> layoutSupplier,
			Supplier<View> mapModeSupplier,
			Supplier<List<MapMarker>> markersSupplier,
			java.util.function.IntSupplier tileSizeSupplier,
			TileClickHandler tileClicked)
	{
		this.layoutSupplier = layoutSupplier;
		this.mapModeSupplier = mapModeSupplier;
		this.markersSupplier = markersSupplier;
		this.tileSizeSupplier = tileSizeSupplier;
		this.tileClicked = tileClicked;

		setBackground(new Color(31, 31, 31));
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				if (!SwingUtilities.isLeftMouseButton(event) && !SwingUtilities.isRightMouseButton(event)) return;
				if (event.getX() >= getMapWidthPixels() || event.getY() >= getMapHeightPixels()) return;

				int x = toMapX(event.getX());
				int y = toMapY(event.getY());

				if (isSelectableMapTile(currentLayout(), x, y))
				{
					tileClicked.accept(x, y, event);
				}
			}
		});
	}

	int getMapWidthPixels()
	{
		return mapBounds().width() * getTileSize();
	}

	int getMapHeightPixels()
	{
		return mapBounds().height() * getTileSize();
	}

	@Override
	public java.awt.Dimension getPreferredSize()
	{
		return new java.awt.Dimension(getMapWidthPixels(), getMapHeightPixels());
	}

	int getTileSize()
	{
		return tileSizeSupplier.getAsInt();
	}

	boolean isSelectableMapTile(ArenaMapLayout layout, int mapX, int mapY)
	{
		if (!mapBounds().contains(mapX, mapY)) return false;

		return BaArenaMapTopology.isUsableTile(layout, mapX, mapY);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		ArenaMapLayout layout = currentLayout();
		int tileSize = getTileSize();
		int width = getMapWidthPixels();
		int height = getMapHeightPixels();

		graphics.setColor(new Color(43, 43, 43));
		graphics.fillRect(0, 0, width, height);
		drawGrid(graphics, tileSize, width, height);
		drawDisabledTiles(graphics, layout);
		drawCannonHills(graphics, layout);
		drawLandmarks(graphics, layout);
		drawMarkers(graphics, layout);
	}

	private void drawGrid(Graphics graphics, int tileSize, int width, int height)
	{
		MapBounds bounds = mapBounds();

		for (int x = bounds.minX; x <= bounds.maxX; x++)
		{
			graphics.setColor(Math.floorMod(x - PRIORITY_CHUNK_ORIGIN_X, PRIORITY_CHUNK_SIZE) == 0
					? new Color(82, 82, 82)
					: new Color(58, 58, 58));
			int screenX = (x - bounds.minX) * tileSize;
			graphics.drawLine(screenX, 0, screenX, height);
		}

		for (int y = bounds.minY; y <= bounds.maxY; y++)
		{
			graphics.setColor(Math.floorMod(y - PRIORITY_CHUNK_ORIGIN_Y, PRIORITY_CHUNK_SIZE) == 0
					? new Color(82, 82, 82)
					: new Color(58, 58, 58));
			int screenY = (bounds.maxY - y) * tileSize;
			graphics.drawLine(0, screenY, width, screenY);
		}
	}

	private void drawMarkers(Graphics graphics, ArenaMapLayout layout)
	{
		// dimmed markers first so that markers of the layer being edited are drawn on top of them
		for (boolean dimmedPass : new boolean[]{true, false})
		{
			for (MapMarker marker : markersSupplier.get())
			{
				if (marker.isDimmed() != dimmedPass) continue;

				GroundMarkerPoint point = marker.getPoint();
				int mapX = layout.toMapX(point);
				int mapY = layout.toMapY(point);
				if (!layout.contains(point) || !mapBounds().contains(mapX, mapY) || !isSelectableMapTile(layout, mapX, mapY)) continue;

				int x = toScreenX(mapX);
				int y = toScreenY(mapY);
				Color color = point.getColor() == null ? DEFAULT_MARKER_COLOR : point.getColor();
				graphics.setColor(withAlpha(color, marker.isDimmed() ? DIMMED_FILL_ALPHA : FILL_ALPHA));
				graphics.fillRect(x, y, getTileSize(), getTileSize());
				drawMarkerBorder(graphics, x, y, marker.isDimmed() ? withAlpha(color, DIMMED_BORDER_ALPHA) : color,
						marker.isDimmed() ? 1f : 2f);
				if (!marker.isDimmed())
				{
					drawMarkerText(graphics, point, x, y);
				}

				if (marker.isSelected())
				{
					drawMarkerBorder(graphics, x, y, SELECTED_MARKER_BORDER_COLOR, selectedMarkerBorderWidth());
				}
			}
		}
	}

	private float selectedMarkerBorderWidth()
	{
		return Math.max(3f, Math.min(5f, getTileSize() / 8f));
	}

	private static Color withAlpha(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private void drawMarkerBorder(Graphics graphics, int x, int y, Color color, float borderWidth)
	{
		float width = borderWidth;

		Graphics2D graphics2D = (Graphics2D) graphics.create();
		Stroke originalStroke = graphics2D.getStroke();
		graphics2D.setColor(color);
		graphics2D.setStroke(new BasicStroke(width));
		graphics2D.drawRect(x, y, getTileSize() - 1, getTileSize() - 1);
		graphics2D.setStroke(originalStroke);
		graphics2D.dispose();
	}

	private void drawLandmarks(Graphics graphics, ArenaMapLayout layout)
	{
		if (mapMode().isFullArena())
		{
			drawFullArenaLandmarks(graphics, layout);
			return;
		}

		drawEastSideLandmarks(graphics, layout);
	}

	private void drawEastSideLandmarks(Graphics graphics, ArenaMapLayout layout)
	{
		if (layout == ArenaMapLayout.WAVE_10)
		{
			drawQueenTrapdoor(graphics);
			drawLandmark(graphics, 45, 26, TRAP_COLOR);
			drawLandmark(graphics, 29, 39, LOGS_COLOR);
			drawLandmark(graphics, 30, 38, LOGS_COLOR);
			drawLandmark(graphics, 32, 34, HAMMER_COLOR);
			drawLandmark(graphics, 42, 38, RUNNER_CAVE_COLOR);
			drawLandmark(graphics, 36, 39, HEALER_CAVE_COLOR);
			return;
		}

		drawLandmark(graphics, 45, 26, TRAP_COLOR);
		drawLandmark(graphics, 28, 39, LOGS_COLOR);
		drawLandmark(graphics, 29, 38, LOGS_COLOR);
		drawLandmark(graphics, 32, 34, HAMMER_COLOR);
		drawLandmark(graphics, 36, 39, RUNNER_CAVE_COLOR);
		drawLandmark(graphics, 42, 37, HEALER_CAVE_COLOR);
		drawLandmark(graphics, EAST_CANNON_X, EAST_CANNON_Y, CANNON_COLOR);
	}

	private void drawFullArenaLandmarks(Graphics graphics, ArenaMapLayout layout)
	{
		drawTrapLandmarks(graphics);
		drawResourceLandmarks(graphics, layout);
		drawCaveLandmarks(graphics, layout);
		drawStartLandmarks(graphics, layout);
		drawRoleDispensers(graphics);
		drawLandmark(graphics, HORN_OF_GLORY_X, HORN_OF_GLORY_Y, HORN_COLOR);
		drawLandmark(graphics, WEST_CANNON_X, WEST_CANNON_Y, CANNON_COLOR);

		if (layout == ArenaMapLayout.WAVE_10)
		{
			drawQueenTrapdoor(graphics);
			return;
		}

		drawLandmark(graphics, EAST_CANNON_X, EAST_CANNON_Y, CANNON_COLOR);
	}

	private void drawTrapLandmarks(Graphics graphics)
	{
		drawLandmark(graphics, 15, 25, TRAP_COLOR);
		drawLandmark(graphics, 45, 26, TRAP_COLOR);
	}

	private void drawResourceLandmarks(Graphics graphics, ArenaMapLayout layout)
	{
		if (layout == ArenaMapLayout.WAVE_10)
		{
			drawLandmark(graphics, 29, 39, LOGS_COLOR);
			drawLandmark(graphics, 30, 38, LOGS_COLOR);
		}
		else
		{
			drawLandmark(graphics, 28, 39, LOGS_COLOR);
			drawLandmark(graphics, 29, 38, LOGS_COLOR);
		}

		drawLandmark(graphics, 32, 34, HAMMER_COLOR);
	}

	private void drawCaveLandmarks(Graphics graphics, ArenaMapLayout layout)
	{
		drawLandmark(graphics, 18, layout == ArenaMapLayout.WAVE_10 ? 38 : 37, RANGER_CAVE_COLOR);
		drawLandmark(graphics, 24, 39, FIGHTER_CAVE_COLOR);

		if (layout == ArenaMapLayout.WAVE_10)
		{
			drawLandmark(graphics, 42, 38, RUNNER_CAVE_COLOR);
			drawLandmark(graphics, 36, 39, HEALER_CAVE_COLOR);
		}
		else
		{
			drawLandmark(graphics, 36, 39, RUNNER_CAVE_COLOR);
			drawLandmark(graphics, 42, 37, HEALER_CAVE_COLOR);
		}
	}

	private void drawStartLandmarks(Graphics graphics, ArenaMapLayout layout)
	{
		if (layout == ArenaMapLayout.WAVE_10)
		{
			drawLandmark(graphics, 28, 8, START_TILE_COLOR);
			drawLandmark(graphics, 32, 8, START_TILE_COLOR);
			drawLandmark(graphics, 30, 10, START_TILE_COLOR);
			drawLandmark(graphics, 29, 9, START_TILE_COLOR);
			drawLandmark(graphics, 31, 9, START_TILE_COLOR);
			return;
		}

		drawLandmark(graphics, 33, 8, START_TILE_COLOR);
		drawLandmark(graphics, 29, 8, START_TILE_COLOR);
		drawLandmark(graphics, 31, 10, START_TILE_COLOR);
		drawLandmark(graphics, 30, 9, START_TILE_COLOR);
		drawLandmark(graphics, 32, 9, START_TILE_COLOR);
	}

	private void drawRoleDispensers(Graphics graphics)
	{
		drawLandmark(graphics, 33, 6, DISPENSER_COLOR);
		drawLandmark(graphics, 34, 6, DISPENSER_COLOR);
		drawLandmark(graphics, 35, 6, DISPENSER_COLOR);
		drawLandmark(graphics, 36, 6, DISPENSER_COLOR);
	}

	private void drawQueenTrapdoor(Graphics graphics)
	{
		drawTileGroupOutline(graphics, QUEEN_TRAPDOOR_COLOR, this::isQueenTrapdoorTile, 27, 35, 20, 28);
	}

	private void drawDisabledTiles(Graphics graphics, ArenaMapLayout layout)
	{
		int tileSize = getTileSize();
		MapBounds bounds = mapBounds();

		for (int x = bounds.minX; x < bounds.maxX; x++)
		{
			for (int y = bounds.minY; y < bounds.maxY; y++)
			{
				if (isSelectableMapTile(layout, x, y)) continue;

				graphics.setColor(DISABLED_TILE_COLOR);
				graphics.fillRect(toScreenX(x), toScreenY(y), tileSize, tileSize);
			}
		}
	}

	private void drawCannonHills(Graphics graphics, ArenaMapLayout layout)
	{
		if (mapMode().isFullArena())
		{
			drawCannonHill(graphics, WEST_CANNON_HILL_X);
			if (layout != ArenaMapLayout.WAVE_10)
			{
				drawCannonHill(graphics, EAST_CANNON_HILL_X);
			}
			return;
		}

		if (layout != ArenaMapLayout.WAVE_10)
		{
			drawCannonHill(graphics, EAST_CANNON_HILL_X);
		}
	}

	private void drawCannonHill(Graphics graphics, int hillX)
	{
		drawTileGroupOutline(
				graphics,
				CANNON_HILL_COLOR,
				(mapX, mapY) -> isCannonHillShapeTile(hillX, mapX, mapY),
				hillX,
				hillX + 5,
				CANNON_HILL_Y - 2,
				CANNON_HILL_Y + 4
		);
	}

	private void drawTileGroupOutline(Graphics graphics, Color color, TileGroup group, int minX, int maxX, int minY, int maxY)
	{
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		graphics2D.setColor(color);
		graphics2D.setStroke(new BasicStroke(2f));

		for (int x = minX; x < maxX; x++)
		{
			for (int y = minY; y < maxY; y++)
			{
				if (!group.contains(x, y)) continue;

				drawPerimeterEdges(graphics2D, group, x, y);
			}
		}

		graphics2D.dispose();
	}

	private void drawPerimeterEdges(Graphics graphics, TileGroup group, int mapX, int mapY)
	{
		int tileSize = getTileSize();
		int x = toScreenX(mapX);
		int y = toScreenY(mapY);

		if (!group.contains(mapX, mapY + 1))
		{
			graphics.drawLine(x, y, x + tileSize, y);
		}

		if (!group.contains(mapX, mapY - 1))
		{
			graphics.drawLine(x, y + tileSize, x + tileSize, y + tileSize);
		}

		if (!group.contains(mapX - 1, mapY))
		{
			graphics.drawLine(x, y, x, y + tileSize);
		}

		if (!group.contains(mapX + 1, mapY))
		{
			graphics.drawLine(x + tileSize, y, x + tileSize, y + tileSize);
		}
	}

	private void drawLandmark(Graphics graphics, int mapX, int mapY, Color color)
	{
		drawFilledTile(graphics, mapX, mapY, color);
	}

	private boolean drawFilledTile(Graphics graphics, int mapX, int mapY, Color color)
	{
		if (!mapBounds().contains(mapX, mapY)) return false;

		graphics.setColor(color);
		graphics.fillRect(toScreenX(mapX), toScreenY(mapY), getTileSize(), getTileSize());
		return true;
	}

	private void drawMarkerText(Graphics graphics, GroundMarkerPoint point, int x, int y)
	{
		String text = point.getLabel();
		if (text == null || text.isBlank()) return;

		drawCenteredText(graphics, text.trim(), x, y, Color.WHITE);
	}

	private void drawCenteredText(Graphics graphics, String text, int x, int y, Color color)
	{
		int tileSize = getTileSize();
		graphics.setColor(color);
		int textWidth = graphics.getFontMetrics().stringWidth(text);
		int textX = x + Math.max(2, (tileSize - textWidth) / 2);
		int textY = y + ((tileSize - graphics.getFontMetrics().getHeight()) / 2) + graphics.getFontMetrics().getAscent();
		graphics.drawString(text, textX, textY);
	}

	private int toScreenX(int mapX)
	{
		return (mapX - mapBounds().minX) * getTileSize();
	}

	private int toScreenY(int mapY)
	{
		return (mapBounds().maxY - mapY - 1) * getTileSize();
	}

	private int toMapX(int screenX)
	{
		int clampedX = Math.max(0, Math.min(getMapWidthPixels() - 1, screenX));
		return mapBounds().minX + clampedX / getTileSize();
	}

	private int toMapY(int screenY)
	{
		int clampedY = Math.max(0, Math.min(getMapHeightPixels() - 1, screenY));
		return mapBounds().maxY - 1 - clampedY / getTileSize();
	}

	private MapBounds mapBounds()
	{
		return mapMode().isFullArena() ? FULL_ARENA_MAP_BOUNDS : EAST_SIDE_MAP_BOUNDS;
	}

	private ArenaMapLayout currentLayout()
	{
		ArenaMapLayout layout = layoutSupplier.get();
		return layout == null ? ArenaMapLayout.WAVES_1_TO_9 : layout;
	}

	private View mapMode()
	{
		View mode = mapModeSupplier.get();
		return mode == null ? View.FULL_MAP : mode;
	}

	private boolean isCannonHillShapeTile(int hillX, int mapX, int mapY)
	{
		return mapX >= hillX && mapX < hillX + 5 && mapY >= CANNON_HILL_Y && mapY < CANNON_HILL_Y + 4
				|| mapX >= hillX + 1 && mapX < hillX + 4 && mapY >= CANNON_HILL_Y - 2 && mapY < CANNON_HILL_Y;
	}

	private boolean isQueenTrapdoorTile(int mapX, int mapY)
	{
		return mapX >= 27 && mapX < 35 && mapY >= 20 && mapY < 28;
	}

	private static class MapBounds
	{
		private final int minX;
		private final int maxX;
		private final int minY;
		private final int maxY;

		private MapBounds(int minX, int maxX, int minY, int maxY)
		{
			this.minX = minX;
			this.maxX = maxX;
			this.minY = minY;
			this.maxY = maxY;
		}

		private int width()
		{
			return maxX - minX;
		}

		private int height()
		{
			return maxY - minY;
		}

		private boolean contains(int x, int y)
		{
			return x >= minX && x < maxX && y >= minY && y < maxY;
		}
	}

	private interface TileGroup
	{
		boolean contains(int mapX, int mapY);
	}

	interface TileClickHandler
	{
		void accept(int mapX, int mapY, MouseEvent event);
	}

	/**
	 * Which part of the arena the map shows.
	 */
	@Getter
	@RequiredArgsConstructor
	enum View
	{
		FULL_MAP("Full map", true),
		EAST_SIDE_ONLY("East side only", false);

		private final String displayName;
		private final boolean fullArena;

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	/**
	 * A BA Tile drawn on the map. Dimmed markers are shown for context only (e.g. tiles of the layer not being edited).
	 */
	@Value
	static class MapMarker
	{
		GroundMarkerPoint point;
		boolean dimmed;
		boolean selected;
	}
}
