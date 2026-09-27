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
- By default a tile is drawn like any BA Tile: an outline in its color, filled black at the plugin's **Fill Opacity**,
  with the plugin's **Border Width**. In the editor, a tile can instead be **filled with its own color** at an opacity
  you choose, and can have its **own border width**.
- If several BA Tiles shown at the same time on one tile look exactly the same (same color, label, fill and border
  width), only one is drawn. Tiles that differ in any way are all drawn, on top of each other.
- **Add another marker here** puts a second marker on the same tile, e.g. with a different label for another wave.
- The map shows the arena's landmarks (cannons, traps, caves, dispensers, logs, hammer, start tiles, and the
  queen's trapdoor on wave 10), and can be zoomed or narrowed to the east side of the arena.

Tiles marked in game and tiles marked on the map are the same tiles: each shows up in the other.

## Strategy presets

For each wave / role combination, you can create any number of named strategy presets, e.g. two different wave 7
defender strategies. A preset is a set of tiles that is only shown while that preset is the **active** preset for its
wave and role. At most one preset is active per wave / role, and "No preset" is always an option.

- Create, rename, delete and select presets in the tile map editor. Active presets can also be switched from the
  sidebar panel: pick a role to get a preset selector for each of the 10 waves.
- In the editor, choose whether you are editing the active preset's tiles or the tiles that are not part of any
  preset; the tiles of the other layer are shown faded.
- In game, ctrl+right-click a tile and choose **Mark BA Tile (preset name)** to add it to the active preset for your
  current wave and role.
- Tiles that are not part of a preset keep working as before, and are always shown for a wave / role without an active
  preset. Whether they are also shown alongside an active preset is set per wave and role: the **Others** checkbox
  next to each wave's preset selector in the sidebar panel, or the matching checkbox in the editor. It is on by default.

### Lineups

A lineup is a named choice of preset for every wave, for one role; for example, a "69 setup" and a "66 setup" for
Defender. In the sidebar panel, pick a role, set up its wave presets, and click **Save** under the lineup selector to
save them as a lineup. Choosing a lineup later switches every wave's preset for that role at once; waves the lineup
has no preset for get none. Changing any wave's preset by hand afterwards means no lineup is active (the selector
shows "No lineup"), until you choose one again. **Rename** and **Delete** act on the active lineup; deleting one keeps
its presets active.

**Export** in the editor copies the active preset and its tiles to the clipboard, and **Import** adds tiles and presets
from the clipboard.

## Importing BA ground markers

If you have marked BA tiles with RuneLite's Ground Markers plugin, click **Import BA ground markers** in the BA Tiles
sidebar panel. A pop-up shows your ground markers on the arena map, for waves 1-9 and for wave 10, with a count of how
many are on each map and how many are not yet BA Tiles. Ground markers that already have their BA Tile are shown faded.

- **Ground markers from** picks where the ground markers come from: this profile, or any of your other RuneLite
  profiles. Either way, they are converted into BA Tiles on the profile you are using now; other profiles are only
  read, never changed.
- Click markers to select them, then **Convert selected**, or use **Convert all new on both maps**.
- Markers with an orange corner are on a tile that already has a different BA Tile (another color, label, waves or
  roles). If any are included when you convert, the confirmation lets you convert them anyway or skip them.
- Each converted marker becomes a BA Tile shown on all waves for all roles, with the same color and label.
- Converting again adds nothing for markers that already have their BA Tile.
- Your ground markers are left as they are; remove them (or turn off Ground Markers) so they are not drawn twice.

## Copying and pasting BA Tiles

A BA Tile can be copied, then pasted to a tile to create a BA Tile marker on that tile with the same
color, label, waves, and roles.

The plugin config allows the user to toggle which wave and role BA Tiles are currently visible.

## Moving over from BA Utilities

BA Utilities' tile markers are moving to BA Tiles. **Import from BA Utilities** in the sidebar panel reads BA Utilities'
tile setup (from this profile, or another of your RuneLite profiles; it is only read, never changed) and shows what
it will import before doing anything:

- Each BA Utilities **strategy**, including its built-in ones, becomes a BA Tiles strategy preset on every wave and role
  you tick, holding the tiles of its sets with their colors, labels, fill opacity and border width. Your own strategies
  start out ticked for every wave of their arena map (waves 1-9, or wave 10) and every role; built-in ones for
  Defender only.
- Each **assignment preset** becomes a lineup, and so does each role's current setup when it is not one of them. A
  GLOBAL (all roles) assignment preset becomes a lineup for each role. You can switch each role to its BA Utilities
  setup right away.
- **GLOBAL** (all roles) selections become tiles that are not part of a preset, shown for all roles on their waves, with
  **Others** turned on for those waves so they also show alongside presets.
- **Strategy notes are not imported**; BA Tiles has no notes.
- Names already used on a wave and role (or by a lineup for that role) must be renamed before importing.
- Nothing already in BA Tiles is removed, and importing again skips what an earlier import created.

## Backing up and restoring everything

**Export all** in the BA Tiles sidebar panel copies *everything* BA Tiles stores on the current profile to the
clipboard: tiles in every region, strategy presets, which presets are active, the per-wave settings, and the plugin's
options. **Import all** restores such a backup from the clipboard, **replacing** everything BA Tiles has stored on the
current profile. It asks first, showing what the backup contains and what will be replaced. Use it to move your whole
setup to another profile or computer.

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
