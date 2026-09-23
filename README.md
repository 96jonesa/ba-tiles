# BA Tiles

## Basic functionality

This plugin allows the user to ctrl+rightClick any walkable tile to manage BA Tiles marked on that tile.

A BA Tile can be configured to specify which waves of Barbarian Assault it is visible on, and which
Barbarian Assault roles it will be visible for.

The user can also specify a color and label for each BA Tile.

A single tile can have any number of BA Tiles marked on it, allowing the user to specify e.g. different
colored / labelled markers on the same tile for different roles and / or waves.

## Tile map editor

BA Tiles can also be created and configured without being in game, on a map of the arena. Open the BA Tiles
sidebar panel and click **Open tile map editor** to open the editor in a pop-up window.

- Pick a wave (1-9 share one arena map; wave 10 has its own) and a role.
- **Left-click** a tile to mark it, or to select the marker already on it. **Right-click** a tile to delete its markers.
- With a marker selected, set its color and label, and (for tiles that are not part of a strategy preset) the
  waves and roles it is shown on. A tile marked from the map starts out shown only for the selected wave and role.
- **Add another marker here** puts a second marker on the same tile, e.g. with a different label for another wave.
- The map shows the arena's landmarks (cannons, traps, caves, dispensers, logs, hammer, start tiles, and the
  queen's trapdoor on wave 10), and can be zoomed or narrowed to the east side of the arena.

Tiles marked in game and tiles marked on the map are the same tiles: each shows up in the other.

## Strategy presets

For each wave / role combination, you can create any number of named strategy presets, e.g. two different wave 7
defender strategies. A preset is a set of tiles that is only shown while that preset is the **active** preset for its
wave and role. At most one preset is active per wave / role, and "No preset" is always an option.

- Create, rename, delete and select presets in the tile map editor. Active presets can also be switched from the
  sidebar panel: pick a role (it follows your current role in game by default) to get a preset selector for each of
  the 10 waves.
- In the editor, choose whether you are editing the active preset's tiles or the tiles that are not part of any
  preset; the tiles of the other layer are shown faded.
- In game, ctrl+right-click a tile and choose **Mark BA Tile (preset name)** to add it to the active preset for your
  current wave and role.
- Tiles that are not part of a preset keep working as before, and are always shown for a wave / role without an active
  preset. Whether they are also shown alongside an active preset is toggleable, in the editor or in the plugin's config
  ("Show non-preset tiles with a preset").

**Export** in the editor copies the active preset and its tiles to the clipboard, and **Import** adds tiles and presets
from the clipboard.

## Copying and pasting BA Tiles

A BA Tile can be copied, then pasted to a tile to create a BA Tile marker on that tile with the same
color, label, waves, and roles.

The plugin config allows the user to toggle which wave and role BA Tiles are currently visible.

## Toggling BA Tile visibility by wave and role

For waves, each of the following can be toggled: current wave, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).

For roles, each of the following can be toggled: current role, Attacker, Collector, Defender, Healer.

## Importing, exporting, and clearing BA Tiles

BA Tiles in the user's current region can be exported via an export option in the world map's right-click menu.
This will copy a JSON blob representing the exported BA Tiles to the user's clipboard.

BA Tiles currently copied into the user's clipboard can be imported via an import in the world map's right-click menu.

Exports that contain strategy preset tiles also carry the presets themselves. Exports without any preset tiles use the
same format as earlier versions of the plugin, so they can still be imported by those versions.

All the BA Tiles in the user's current region can be deleted at once via a clear option in the world map's
right-click menu.

## Why is this useful?

Barbarian Assault players often mark a different set of several tiles on each wave when playing the Defender role,
typically differentiated by color and / or label. This results in a huge number of tiles competing for the user's
attention, and potentially further confusing the user when the same tile is used for different purposes in different
waves.

Players may also mark (a much smaller number of) different tiles when playing as other roles.

This plugin only shows the user the relevant tile markers for their current wave and role, reducing the total number
of tile markers on the screen from dozens to a handful; and reducing the number of un-used tile markers at a given time
from dozens to zero.

## Credits

The arena map in the tile map editor (map rendering, walkable tiles and landmarks) is adapted from the
[BA Utilities](https://github.com/tcourter1/ba-healer-order) plugin by tcourter1, used under its BSD 2-Clause license.
The adapted files carry its copyright notice.
