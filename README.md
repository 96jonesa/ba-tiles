# BA Tiles

## Basic functionality

This plugin allows the user to ctrl+rightClick any walkable tile to manage BA Tiles marked on that tile.

A BA Tile can be configured to specify which waves of Barbarian Assault it is visible on, and which
Barbarian Assault roles it will be visible for.

The user can also specify a color and label for each BA Tile.

A single tile can have any number of BA Tiles marked on it, allowing the user to specify e.g. different
colored / labelled markers on the same tile for different roles and / or waves.

## Copying and pasting BA Tiles

A BA Tile can be copied, then pasted to a tile to create a BA Tile marker on that tile with the same
color, label, waves, and roles.

The plugin sidebar allows the user to toggle which wave and role BA Tiles are currently visible.

## Toggling BA Tile visibility by wave and role

For waves, each of the following can be toggled: current wave, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).

For roles, each of the following can be toggled: current role, Attacker, Collector, Defender, Healer.

## Importing, exporting, and clearing BA Tiles

BA Tiles in the user's current region can be exported via an export option in the world map's right-click menu.
This will copy a JSON blob representing the exported BA Tiles to the user's clipboard.

BA Tiles currently copied into the user's clipboard can be imported via an import in the world map's right-click menu.

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