package com.batiles;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Menu;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
		name = "BA Tiles",
		description = "Ground Markers for specific Barbarian Assault waves and roles",
		tags = {"minigame", "overlay", "tiles"}
)
public class BATilesPlugin extends Plugin {
	private static final String WALK_HERE = "Walk here";
	private static final List<Integer> ALL_WAVES = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
	private static final List<String> ALL_ROLES = List.of("a", "c", "d", "h");
	private static final int BA_WAVE_NUM_INDEX = 2;
	private static final int START_WAVE = 1;

	@Getter(AccessLevel.PACKAGE)
	private final List<ColorTileMarker> points = new ArrayList<>();

	@Inject
	private Client client;

	@Inject
	private BATilesConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BATilesOverlay overlay;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private BATilesSharingManager sharingManager;

	@Inject
	private ColorPickerManager colorPickerManager;

	@Inject
	private BATilesStore store;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	private final Runnable storeListener = () -> clientThread.invokeLater(this::loadPoints);
	private BATilesPanel panel;
	private NavigationButton navigationButton;
	private TileMapEditor editor;

	private int currentWave = START_WAVE;
	private String currentRole = "a";
	private GroundMarkerPoint copiedPoint = null;

	@Provides
	BATilesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BATilesConfig.class);
	}

	void loadPoints()
	{
		points.clear();

		int[] regions = client.getMapRegions();

		if (regions == null)
		{
			return;
		}

		List<StrategyPreset> presets = store.getPresets();
		TileVisibility visibility = new TileVisibility(
				wavesToDisplay(),
				rolesToDisplay(),
				store::getActivePresetId,
				id -> presets.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null),
				config.showBaseTilesWithPreset());

		for (int regionId : regions)
		{
			// load points for region
			log.debug("Loading points for region {}", regionId);
			Collection<GroundMarkerPoint> regionPoints = store.getPoints(regionId);

			Collection<GroundMarkerPoint> pointsToLoad = regionPoints.stream()
					.filter(visibility::isVisible)
					.collect(Collectors.toList());

			Collection<ColorTileMarker> colorTileMarkers = translateToColorTileMarker(pointsToLoad);
			points.addAll(colorTileMarkers);
		}
	}

	List<Integer> wavesToDisplay() {
		List<Integer> waves = new ArrayList<Integer>();

		if (config.showTilesForCurrentWave()) {
			waves.add(currentWave);
		}

		if (config.showTilesForWave1()) {
			waves.add(1);
		}

		if (config.showTilesForWave2()) {
			waves.add(2);
		}

		if (config.showTilesForWave3()) {
			waves.add(3);
		}

		if (config.showTilesForWave4()) {
			waves.add(4);
		}

		if (config.showTilesForWave5()) {
			waves.add(5);
		}

		if (config.showTilesForWave6()) {
			waves.add(6);
		}

		if (config.showTilesForWave7()) {
			waves.add(7);
		}

		if (config.showTilesForWave8()) {
			waves.add(8);
		}

		if (config.showTilesForWave9()) {
			waves.add(9);
		}

		if (config.showTilesForWave10()) {
			waves.add(10);
		}

		return waves;
	}

	List<String> rolesToDisplay() {
		List<String> roles = new ArrayList<String>();

		if (config.showTilesForCurrentRole()) {
			roles.add(currentRole);
		}

		if (config.showTilesForAttacker()) {
			roles.add("a");
		}

		if (config.showTilesForCollector()) {
			roles.add("c");
		}

		if (config.showTilesForDefender()) {
			roles.add("d");
		}

		if (config.showTilesForHealer()) {
			roles.add("h");
		}

		return roles;
	}

	/**
	 * Translate a collection of ground marker points to color tile markers, accounting for instances
	 *
	 * @param points {@link GroundMarkerPoint}s to be converted to {@link ColorTileMarker}s
	 * @return A collection of color tile markers, converted from the passed ground marker points, accounting for local
	 *         instance points. See {@link WorldPoint#toLocalInstance(Client, WorldPoint)}
	 */
	private Collection<ColorTileMarker> translateToColorTileMarker(Collection<GroundMarkerPoint> points)
	{
		if (points.isEmpty())
		{
			return Collections.emptyList();
		}

		return points.stream()
				.map(point -> new ColorTileMarker(
						WorldPoint.fromRegion(point.getRegionId(), point.getRegionX(), point.getRegionY(), point.getZ()),
						point.getColor(), point.getLabel()))
				.flatMap(colorTile ->
				{
					final Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(client, colorTile.getWorldPoint());
					return localWorldPoints.stream().map(wp -> new ColorTileMarker(wp, colorTile.getColor(), colorTile.getLabel()));
				})
				.collect(Collectors.toList());
	}

	@Override
	public void startUp()
	{
		overlayManager.add(overlay);
		if (config.showImportExport())
		{
			sharingManager.addImportExportMenuOptions();
			sharingManager.addClearMenuOption();
		}
		store.addListener(storeListener);
		clientThread.invokeLater(this::loadPoints);
		eventBus.register(sharingManager);

		panel = new BATilesPanel(store, config, configManager, this::getEditor);
		navigationButton = NavigationButton.builder()
				.tooltip("BA Tiles")
				.icon(panelIcon())
				.priority(10)
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navigationButton);
	}

	/**
	 * The tile map editor pop-up, created on first use. Must be called on the Swing event thread.
	 */
	private TileMapEditor getEditor()
	{
		if (editor == null)
		{
			editor = new TileMapEditor(SwingUtilities.getWindowAncestor(panel), store, config, configManager,
					colorPickerManager, sharingManager);
		}
		return editor;
	}

	private static BufferedImage panelIcon()
	{
		// a 3x3 grid of tiles with the centre one marked
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setColor(new Color(200, 200, 200));
		for (int i = 0; i < 4; i++)
		{
			g.drawLine(i * 5, 0, i * 5, 15);
			g.drawLine(0, i * 5, 15, i * 5);
		}
		g.setColor(Color.YELLOW);
		g.fillRect(6, 6, 4, 4);
		g.dispose();
		return icon;
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(sharingManager);
		store.removeListener(storeListener);
		clientToolbar.removeNavigation(navigationButton);
		panel.shutDown();
		TileMapEditor openEditor = editor;
		editor = null;
		if (openEditor != null)
		{
			SwingUtilities.invokeLater(openEditor::dispose);
		}
		overlayManager.remove(overlay);
		sharingManager.removeMenuOptions();
		points.clear();
		currentWave = START_WAVE;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		if (event.getType() == ChatMessageType.GAMEMESSAGE
				&& event.getMessage().startsWith("---- Wave:"))
		{
			String[] message = event.getMessage().split(" ");

			try {
				currentWave = Integer.parseInt(message[BA_WAVE_NUM_INDEX]);
			} catch (NumberFormatException e) {
				return;
			}
			onWaveOrRoleChanged();
		}
	}

	private void onWaveOrRoleChanged()
	{
		panel.onGameStateChanged(currentWave, currentRole);
		loadPoints();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		switch (event.getGroupId())
		{
			case InterfaceID.BA_ATTACKER:
			{
				currentRole = "a";
				break;
			}
			case InterfaceID.BA_DEFENDER:
			{
				currentRole = "d";
				break;
			}
			case InterfaceID.BA_HEALER:
			{
				currentRole = "h";
				break;
			}
			case InterfaceID.BA_COLLECTOR:
			{
				currentRole = "c";
				break;
			}
			default:
				return;
		}
		onWaveOrRoleChanged();
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged profileChanged)
	{
		store.fireChanged();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		// map region has just been updated
		loadPoints();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		final boolean hotKeyPressed = client.isKeyPressed(KeyCode.KC_CONTROL);
		if (!hotKeyPressed || !event.getOption().equals(WALK_HERE))
		{
			return;
		}

		final Tile selectedSceneTile = client.getSelectedSceneTile();
		if (selectedSceneTile == null)
		{
			return;
		}

		final WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, selectedSceneTile.getLocalLocation());
		final int regionId = worldPoint.getRegionID();
		var existingPoints = store.getPoints(regionId).stream()
				.filter(p -> p.isAt(regionId, worldPoint.getRegionX(), worldPoint.getRegionY(), worldPoint.getPlane()))
				.collect(Collectors.toList());
		List<StrategyPreset> presets = store.getPresets();
		// preset tiles only make sense inside the arena of the preset's wave (e.g. not in the lobby after a game)
		Optional<StrategyPreset> activePreset = store.getPreset(store.getActivePresetId(currentWave, currentRole))
				.filter(p -> isInPresetArena(p, worldPoint));

		int index = -1;

		client.createMenuEntry(index--)
				.setOption("Mark")
				.setTarget("BA Tile")
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					Tile target = client.getSelectedSceneTile();
					if (target != null)
					{
						markTile(target.getLocalLocation(), null);
					}
				});

		if (activePreset.isPresent())
		{
			StrategyPreset preset = activePreset.get();
			client.createMenuEntry(index--)
					.setOption("Mark")
					.setTarget("BA Tile (" + preset.getName() + ")")
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						Tile target = client.getSelectedSceneTile();
						if (target != null)
						{
							markTile(target.getLocalLocation(), preset);
						}
					});
		}

		if (copiedPoint != null)
		{
			client.createMenuEntry(index--)
					.setOption("Paste")
					.setTarget("BA Tile")
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						Tile target = client.getSelectedSceneTile();
						if (target != null)
						{
							pasteTile(target.getLocalLocation());
						}
					});
		}

		for (GroundMarkerPoint point : existingPoints)
		{
			String presetName = point.isPresetTile()
					? presets.stream().filter(p -> p.getId().equals(point.getPresetId())).map(StrategyPreset::getName).findFirst().orElse("deleted preset")
					: null;
			String target = "BA Tile " + (point.getLabel() == null ? "" : point.getLabel() + " ")
					+ (presetName != null ? "(" + presetName + ")" : point.getWaves() + " " + point.getRoles());

			Menu pointConfigMenu = client.createMenuEntry(index--)
					.setOption(ColorUtil.prependColorTag("Configure", point.getColor()))
					.setTarget(target)
					.setType(MenuAction.RUNELITE)
					.createSubMenu();

			int subIndex = 0;

			// a preset tile always belongs to exactly its preset's wave and role
			if (!point.isPresetTile())
			{
				pointConfigMenu.createMenuEntry(subIndex--)
						.setOption("Set waves")
						.setType(MenuAction.RUNELITE)
						.onClick(e -> setTileWaves(point));

				pointConfigMenu.createMenuEntry(subIndex--)
						.setOption("Set roles")
						.setType(MenuAction.RUNELITE)
						.onClick(e -> setTileRoles(point));
			}

			pointConfigMenu.createMenuEntry(subIndex--)
					.setOption("Set label")
					.setType(MenuAction.RUNELITE)
					.onClick(e -> labelTile(point));

			pointConfigMenu.createMenuEntry(subIndex--)
					.setOption("Pick color")
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						Color color = point.getColor();
						SwingUtilities.invokeLater(() ->
						{
							RuneliteColorPicker colorPicker = colorPickerManager.create(client,
									color, "Tile marker color", false);
							colorPicker.setOnClose(c -> clientThread.invokeLater(() -> colorTile(point, c)));
							colorPicker.setVisible(true);
						});
					});

			pointConfigMenu.createMenuEntry(subIndex--)
					.setOption("Copy")
					.setType(MenuAction.RUNELITE)
					.onClick(e -> copyTile(point));

			pointConfigMenu.createMenuEntry(subIndex--)
					.setOption("Unmark")
					.setType(MenuAction.RUNELITE)
					.onClick(e -> unmarkTile(point));

			var existingColors = points.stream()
					.map(ColorTileMarker::getColor)
					.filter(Objects::nonNull)
					.distinct()
					.collect(Collectors.toList());
			for (Color color : existingColors)
			{
				if (!color.equals(point.getColor()))
				{
					pointConfigMenu.createMenuEntry(subIndex--)
							.setOption(ColorUtil.prependColorTag("Color", color))
							.setType(MenuAction.RUNELITE)
							.onClick(e -> colorTile(point, color));
				}
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals(BATilesConfig.BA_TILES_CONFIG_GROUP)
				&& event.getKey().equals(BATilesConfig.SHOW_IMPORT_EXPORT_KEY_NAME))
		{
			sharingManager.removeMenuOptions();

			if (config.showImportExport())
			{
				sharingManager.addImportExportMenuOptions();
				sharingManager.addClearMenuOption();
			}
		}

		if (event.getGroup().equals(BATilesConfig.BA_TILES_CONFIG_GROUP))
		{
			// reloads the tiles, and refreshes the panel and editor (e.g. for toggles changed in the config panel)
			store.fireChanged();
		}
	}

	private static boolean isInPresetArena(StrategyPreset preset, WorldPoint worldPoint)
	{
		return worldPoint.getPlane() == 0 && worldPoint.getRegionID() == ArenaMapLayout.fromWave(preset.getWave()).getRegionId();
	}

	private void markTile(LocalPoint localPoint, @Nullable StrategyPreset preset)
	{
		if (localPoint == null)
		{
			return;
		}

		WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);

		List<Integer> waves = preset == null ? ALL_WAVES : List.of(preset.getWave());
		List<String> roles = preset == null ? ALL_ROLES : List.of(preset.getRole());
		GroundMarkerPoint point = new GroundMarkerPoint(worldPoint.getRegionID(), worldPoint.getRegionX(), worldPoint.getRegionY(),
				worldPoint.getPlane(), config.markerColor(), null, waves, roles, preset == null ? null : preset.getId());
		log.debug("Updating point: {} - {}", point, worldPoint);

		store.addPoint(point);
	}

	private void unmarkTile(GroundMarkerPoint existing)
	{
		log.debug("Updating point: {}", existing);
		store.removePoint(existing);
	}

	private void labelTile(GroundMarkerPoint existing)
	{
		chatboxPanelManager.openTextInput("Tile label")
				.value(Optional.ofNullable(existing.getLabel()).orElse(""))
				.onDone((input) ->
				{
					input = Strings.emptyToNull(input);

					if (input != null && input.length() > TileMapEditor.MAX_LABEL_LENGTH) {
						input = input.substring(0, TileMapEditor.MAX_LABEL_LENGTH);
					}

					String label = input;
					store.updatePoint(existing, p -> p.withLabel(label));
				})
				.build();
	}

	private void colorTile(GroundMarkerPoint existing, Color newColor)
	{
		store.updatePoint(existing, p -> p.withColor(newColor));
	}

	private void setTileWaves(GroundMarkerPoint existing)
	{
		chatboxPanelManager.openTextInput("Tile waves")
				.value("")
				.onDone((input) ->
				{
					input = Strings.emptyToNull(input);

					String[] tokens;

					if (input == null) {
						tokens = new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
					} else {
						tokens = input.split(",");
					}

					List<Integer> waves = new ArrayList<Integer>();

					for (String token : tokens) {
						int wave;

						try {
							wave = Integer.parseInt(token.trim());
						} catch (NumberFormatException e) {
							return;
						}

						if (wave < 1 || wave > 10) {
							return;
						}

						if (!waves.contains(wave)) {
							waves.add(wave);
						}
					}

					Collections.sort(waves);

					store.updatePoint(existing, p -> p.withWaves(waves));
				})
				.build();
	}

	private void setTileRoles(GroundMarkerPoint existing)
	{
		chatboxPanelManager.openTextInput("Tile roles")
				.value("")
				.onDone((input) ->
				{
					input = Strings.emptyToNull(input);

					String[] tokens;

					if (input == null) {
						tokens = new String[] {"a", "c", "d", "h"};
					} else {
						tokens = input.split(",");
					}

					List<String> roles = new ArrayList<String>();

					for (String token : tokens) {
						String role = token.trim();

						if (BARole.fromCode(role) == null) {
							return;
						}

						if (!roles.contains(role)) {
							roles.add(role);
						}
					}

					Collections.sort(roles);

					store.updatePoint(existing, p -> p.withRoles(roles));
				})
				.build();
	}

	private void copyTile(GroundMarkerPoint existing) {
		copiedPoint = existing;
	}

	private void pasteTile(LocalPoint localPoint) {
		if (copiedPoint == null) {
			return;
		}

		if (localPoint == null)
		{
			return;
		}

		WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);

		// a copied preset tile stays in its preset only if that preset still exists and this tile is in its arena
		GroundMarkerPoint copy = copiedPoint;
		if (copy.isPresetTile() && !store.getPreset(copy.getPresetId()).filter(p -> isInPresetArena(p, worldPoint)).isPresent())
		{
			copy = copy.withPresetId(null);
		}

		store.addPoint(copy
				.withRegionId(worldPoint.getRegionID())
				.withRegionX(worldPoint.getRegionX())
				.withRegionY(worldPoint.getRegionY())
				.withZ(worldPoint.getPlane()));
	}
}
