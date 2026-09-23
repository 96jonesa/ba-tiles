package com.batiles;

import com.google.common.base.Strings;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.ColorJButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

/**
 * Pop-up window for creating and configuring BA Tiles on a map of the arena, per wave and role, and for managing the
 * strategy presets of each wave / role.
 */
class TileMapEditor extends JDialog
{
	static final int MAX_LABEL_LENGTH = 10;
	private static final int MIN_TILE_SIZE = 8;
	private static final int MAX_TILE_SIZE = 40;
	private static final int DEFAULT_TILE_SIZE = 16;
	private static final int SIDE_WIDTH = 230;
	// in CSS pixels, which Swing renders larger than screen pixels
	private static final int HELP_TEXT_WIDTH = 160;

	private final BATilesStore store;
	private final BATilesConfig config;
	private final ConfigManager configManager;
	private final ColorPickerManager colorPickerManager;
	private final BATilesSharingManager sharingManager;
	private final Runnable storeListener = () -> SwingUtilities.invokeLater(this::refresh);

	private final JComboBox<Integer> waveCombo = new JComboBox<>(IntStream.rangeClosed(1, 10).boxed().toArray(Integer[]::new));
	private final JComboBox<BARole> roleCombo = new JComboBox<>(BARole.values());
	private final JComboBox<Object> presetCombo = new JComboBox<>();
	private final JButton newPresetButton = new JButton("New");
	private final JButton renamePresetButton = new JButton("Rename");
	private final JButton deletePresetButton = new JButton("Delete");
	private final JButton exportPresetButton = new JButton("Export");
	private final JButton importButton = new JButton("Import");
	private final JRadioButton editPresetRadio = new JRadioButton("Preset tiles");
	private final JRadioButton editBaseRadio = new JRadioButton("Non-preset tiles");
	private final JCheckBox showBaseWithPreset = new JCheckBox("Show non-preset tiles when a preset is active");
	private final JComboBox<ArenaMapPanel.View> viewCombo = new JComboBox<>(ArenaMapPanel.View.values());
	private final JSlider zoom = new JSlider(MIN_TILE_SIZE, MAX_TILE_SIZE, DEFAULT_TILE_SIZE);
	private final ArenaMapPanel mapPanel;
	private final JScrollPane mapScrollPane;

	private final JLabel tileTitle = new JLabel();
	private final JComboBox<MarkerOption> markerCombo = new JComboBox<>();
	private final JButton addMarkerButton = new JButton("Add another marker here");
	private final ColorJButton colorButton = new ColorJButton("Color", Color.YELLOW);
	private final JTextField labelField = new JTextField();
	private final JPanel wavesPanel = new JPanel(new GridLayout(2, 5));
	private final List<JCheckBox> waveBoxes = new ArrayList<>();
	private final JPanel rolesPanel = new JPanel(new GridLayout(2, 2));
	private final List<JCheckBox> roleBoxes = new ArrayList<>();
	private final JButton deleteMarkerButton = new JButton("Delete marker");
	private final JPanel markerDetails = new JPanel();

	private List<GroundMarkerPoint> points = new ArrayList<>();
	private int selectedMapX = -1;
	private int selectedMapY = -1;
	@Nullable
	private GroundMarkerPoint selected;
	// the marker whose label the label field currently shows
	@Nullable
	private GroundMarkerPoint labelShownFor;
	private boolean editingPreset = true;
	private boolean refreshing;

	TileMapEditor(Window owner, BATilesStore store, BATilesConfig config, ConfigManager configManager,
				  ColorPickerManager colorPickerManager, BATilesSharingManager sharingManager)
	{
		super(owner, "BA Tiles map editor", ModalityType.MODELESS);
		this.store = store;
		this.config = config;
		this.configManager = configManager;
		this.colorPickerManager = colorPickerManager;
		this.sharingManager = sharingManager;

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		mapPanel = new ArenaMapPanel(
				() -> layers().layout(),
				() -> (ArenaMapPanel.View) viewCombo.getSelectedItem(),
				() -> layers().mapMarkers(points, selected),
				zoom::getValue,
				this::onTileClicked);
		mapScrollPane = new JScrollPane(mapPanel);
		mapScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		mapScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

		JPanel content = new JPanel(new BorderLayout(8, 8));
		content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		content.add(buildTopBar(), BorderLayout.NORTH);
		content.add(mapScrollPane, BorderLayout.CENTER);
		content.add(buildSidePanel(), BorderLayout.EAST);
		setContentPane(content);

		setSize(1100, 800);
		setLocationRelativeTo(owner);
		store.addListener(storeListener);
		refresh();
	}

	/**
	 * Shows the editor for the given wave and role.
	 */
	void open(int wave, String role)
	{
		waveCombo.setSelectedItem(wave);
		roleCombo.setSelectedItem(BARole.fromCode(role));
		setVisible(true);
		toFront();
	}

	@Override
	public void dispose()
	{
		store.removeListener(storeListener);
		super.dispose();
	}

	private int wave()
	{
		return (Integer) waveCombo.getSelectedItem();
	}

	private String role()
	{
		return ((BARole) roleCombo.getSelectedItem()).getCode();
	}

	private EditorLayers layers()
	{
		return new EditorLayers(wave(), role(), store.getActivePresetId(wave(), role()), editingPreset);
	}

	// ---- layout ----

	private JPanel buildTopBar()
	{
		JPanel selection = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		selection.add(new JLabel("Wave"));
		selection.add(waveCombo);
		selection.add(new JLabel("Role"));
		selection.add(roleCombo);
		selection.add(Box.createHorizontalStrut(12));
		selection.add(new JLabel("Strategy preset"));
		presetCombo.setPrototypeDisplayValue("A long strategy preset name");
		selection.add(presetCombo);
		selection.add(newPresetButton);
		selection.add(renamePresetButton);
		selection.add(deletePresetButton);
		selection.add(exportPresetButton);
		selection.add(importButton);

		JPanel editing = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		editing.add(new JLabel("Editing"));
		ButtonGroup group = new ButtonGroup();
		group.add(editPresetRadio);
		group.add(editBaseRadio);
		editing.add(editPresetRadio);
		editing.add(editBaseRadio);
		editing.add(Box.createHorizontalStrut(12));
		editing.add(showBaseWithPreset);

		JPanel view = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		view.add(new JLabel("View"));
		view.add(viewCombo);
		view.add(new JLabel("Zoom"));
		view.add(zoom);

		waveCombo.addActionListener(e -> onWaveOrRoleChanged());
		roleCombo.addActionListener(e -> onWaveOrRoleChanged());
		presetCombo.addActionListener(e -> onPresetSelected());
		newPresetButton.addActionListener(e -> createPreset());
		renamePresetButton.addActionListener(e -> renamePreset());
		deletePresetButton.addActionListener(e -> deletePreset());
		exportPresetButton.addActionListener(e -> activePreset().ifPresent(sharingManager::exportPreset));
		importButton.addActionListener(e -> sharingManager.promptForImport());
		editPresetRadio.addActionListener(e -> setEditingPreset(true));
		editBaseRadio.addActionListener(e -> setEditingPreset(false));
		showBaseWithPreset.addActionListener(e -> setConfig(BATilesConfig.SHOW_BASE_TILES_WITH_PRESET_KEY_NAME, showBaseWithPreset.isSelected()));
		viewCombo.addActionListener(e -> mapChanged());
		zoom.addChangeListener(e -> mapChanged());

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		for (JPanel row : new JPanel[]{selection, editing, view})
		{
			row.setAlignmentX(Component.LEFT_ALIGNMENT);
			top.add(row);
			top.add(Box.createVerticalStrut(4));
		}
		return top;
	}

	private JPanel buildSidePanel()
	{
		JPanel side = new JPanel(new BorderLayout());
		side.setPreferredSize(new Dimension(SIDE_WIDTH, 0));

		JPanel column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

		column.add(leftAligned(tileTitle));
		column.add(Box.createVerticalStrut(6));

		markerDetails.setLayout(new BoxLayout(markerDetails, BoxLayout.Y_AXIS));
		markerDetails.add(leftAligned(new JLabel("Marker")));
		markerDetails.add(leftAligned(markerCombo));
		markerDetails.add(leftAligned(addMarkerButton));
		markerDetails.add(Box.createVerticalStrut(8));
		markerDetails.add(leftAligned(colorButton));
		markerDetails.add(Box.createVerticalStrut(8));
		markerDetails.add(leftAligned(new JLabel("Label (max " + MAX_LABEL_LENGTH + " characters)")));
		markerDetails.add(leftAligned(labelField));
		markerDetails.add(Box.createVerticalStrut(8));
		markerDetails.add(leftAligned(new JLabel("Shown on waves")));
		// waves 1-9 only: tiles for wave 10 are on the wave 10 map, which has no wave choice
		for (int wave = 1; wave <= 9; wave++)
		{
			JCheckBox box = new JCheckBox(Integer.toString(wave));
			box.addActionListener(e -> applyWaves());
			waveBoxes.add(box);
			wavesPanel.add(box);
		}
		markerDetails.add(leftAligned(wavesPanel));
		markerDetails.add(Box.createVerticalStrut(8));
		markerDetails.add(leftAligned(new JLabel("Shown for roles")));
		for (BARole role : BARole.values())
		{
			JCheckBox box = new JCheckBox(role.getDisplayName());
			box.addActionListener(e -> applyRoles());
			roleBoxes.add(box);
			rolesPanel.add(box);
		}
		markerDetails.add(leftAligned(rolesPanel));
		markerDetails.add(Box.createVerticalStrut(8));
		markerDetails.add(leftAligned(deleteMarkerButton));
		column.add(leftAligned(markerDetails));

		column.add(Box.createVerticalStrut(16));
		JLabel help = new JLabel("<html><div style='width:" + HELP_TEXT_WIDTH + "px'><b>Left-click</b> a tile to mark it, or to select its marker.<br>"
				+ "<b>Right-click</b> a tile to delete its markers.<br><br>"
				+ "Tiles of the layer not being edited are shown faded.<br><br>"
				+ "A preset's tiles are only shown while it is the active preset for its wave and role.</div></html>");
		help.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		column.add(leftAligned(help));

		markerCombo.addActionListener(e ->
		{
			if (!refreshing)
			{
				applyLabel();
				MarkerOption option = (MarkerOption) markerCombo.getSelectedItem();
				selected = option == null ? null : option.point;
				refresh();
			}
		});
		addMarkerButton.addActionListener(e -> addMarker(selectedMapX, selectedMapY));
		colorButton.addActionListener(e -> pickColor());
		labelField.addActionListener(e -> applyLabel());
		labelField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				applyLabel();
			}
		});
		deleteMarkerButton.addActionListener(e ->
		{
			if (selected != null)
			{
				GroundMarkerPoint toDelete = selected;
				selected = null;
				store.removePoint(toDelete);
			}
		});

		side.add(column, BorderLayout.NORTH);
		return side;
	}

	private static <T extends javax.swing.JComponent> T leftAligned(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (component instanceof JComboBox || component instanceof JTextField)
		{
			component.setMaximumSize(new Dimension(SIDE_WIDTH, component.getPreferredSize().height));
		}
		return component;
	}

	// ---- state ----

	private java.util.Optional<StrategyPreset> activePreset()
	{
		return store.getPreset(store.getActivePresetId(wave(), role()));
	}

	/**
	 * Re-reads tiles and presets from the store and updates every control. Safe to call at any time.
	 */
	private void refresh()
	{
		refreshing = true;
		try
		{
			points = new ArrayList<>(store.getPoints(layers().layout().getRegionId()));

			// preset selector
			List<StrategyPreset> presets = store.getPresets(wave(), role());
			String activeId = store.getActivePresetId(wave(), role());
			presetCombo.removeAllItems();
			presetCombo.addItem(PresetOption.NO_PRESET);
			Object activeItem = PresetOption.NO_PRESET;
			for (StrategyPreset preset : presets)
			{
				PresetOption option = new PresetOption(preset);
				presetCombo.addItem(option);
				if (preset.getId().equals(activeId))
				{
					activeItem = option;
				}
			}
			presetCombo.setSelectedItem(activeItem);
			boolean hasPreset = activeId != null;
			renamePresetButton.setEnabled(hasPreset);
			deletePresetButton.setEnabled(hasPreset);
			exportPresetButton.setEnabled(hasPreset);

			editPresetRadio.setEnabled(hasPreset);
			editPresetRadio.setSelected(layers().isEditingPreset());
			editBaseRadio.setSelected(!layers().isEditingPreset());
			showBaseWithPreset.setSelected(config.showBaseTilesWithPreset());

			// selection: keep the selected marker if it still exists and is editable, else fall back to the tile's first marker
			EditorLayers layers = layers();
			if (selected != null && !(points.contains(selected) && layers.isEditable(selected)))
			{
				selected = null;
			}
			List<GroundMarkerPoint> atTile = layers.editableAt(points, selectedMapX, selectedMapY);
			if (selected == null && !atTile.isEmpty())
			{
				selected = atTile.get(0);
			}
			refreshSidePanel(layers, atTile);
		}
		finally
		{
			refreshing = false;
		}
		mapChanged();
	}

	private void refreshSidePanel(EditorLayers layers, List<GroundMarkerPoint> atTile)
	{
		boolean tileSelected = selectedMapX >= 0;
		tileTitle.setText(tileSelected
				? "<html><b>Tile " + selectedMapX + ", " + selectedMapY + "</b> ("
					+ (layers.isEditingPreset() ? "preset tiles" : "non-preset tiles") + ")</html>"
				: "<html><b>No tile selected</b></html>");
		addMarkerButton.setEnabled(tileSelected);

		markerCombo.removeAllItems();
		for (int i = 0; i < atTile.size(); i++)
		{
			markerCombo.addItem(new MarkerOption(atTile.get(i), i + 1));
		}
		for (int i = 0; i < markerCombo.getItemCount(); i++)
		{
			if (markerCombo.getItemAt(i).point.equals(selected))
			{
				markerCombo.setSelectedIndex(i);
			}
		}

		markerDetails.setVisible(selected != null);
		if (selected == null)
		{
			return;
		}

		colorButton.setColor(selected.getColor() == null ? config.markerColor() : selected.getColor());
		// don't clobber a label being typed, unless the selection moved on to another marker
		if (!labelField.isFocusOwner() || !selected.equals(labelShownFor))
		{
			labelField.setText(Strings.nullToEmpty(selected.getLabel()));
		}
		labelShownFor = selected;

		// preset tiles belong to exactly the preset's wave and role; wave 10 tiles live on their own map
		boolean base = !selected.isPresetTile();
		boolean waveTenMap = layers.layout() == ArenaMapLayout.WAVE_10;
		wavesPanel.setVisible(base && !waveTenMap);
		for (int i = 0; i < waveBoxes.size(); i++)
		{
			int wave = i + 1;
			JCheckBox box = waveBoxes.get(i);
			box.setSelected(selected.getWaves() == null || selected.getWaves().contains(wave));
		}
		rolesPanel.setVisible(base);
		for (int i = 0; i < roleBoxes.size(); i++)
		{
			String code = BARole.values()[i].getCode();
			roleBoxes.get(i).setSelected(selected.getRoles() == null || selected.getRoles().contains(code));
		}
	}

	private void mapChanged()
	{
		mapPanel.revalidate();
		mapPanel.repaint();
	}

	private void onWaveOrRoleChanged()
	{
		if (refreshing)
		{
			return;
		}
		applyLabel();
		selected = null;
		selectedMapX = -1;
		selectedMapY = -1;
		editingPreset = true;
		refresh();
	}

	private void onPresetSelected()
	{
		if (refreshing)
		{
			return;
		}
		applyLabel();
		Object item = presetCombo.getSelectedItem();
		StrategyPreset preset = item instanceof PresetOption ? ((PresetOption) item).getPreset() : null;
		editingPreset = preset != null;
		selected = null;
		store.setActivePreset(wave(), role(), preset);
		refresh();
	}

	private void setEditingPreset(boolean editingPreset)
	{
		if (refreshing)
		{
			return;
		}
		applyLabel();
		this.editingPreset = editingPreset;
		selected = null;
		refresh();
	}

	private void setConfig(String key, boolean value)
	{
		if (!refreshing)
		{
			configManager.setConfiguration(BATilesConfig.BA_TILES_CONFIG_GROUP, key, value);
		}
	}

	// ---- presets ----

	private void createPreset()
	{
		String name = promptForName("New " + roleCombo.getSelectedItem() + " strategy preset for wave " + wave(), "");
		if (name == null)
		{
			return;
		}
		StrategyPreset preset = store.createPreset(name, wave(), role());
		editingPreset = true;
		store.setActivePreset(wave(), role(), preset);
		refresh();
	}

	private void renamePreset()
	{
		activePreset().ifPresent(preset ->
		{
			String name = promptForName("Rename strategy preset", preset.getName());
			if (name != null)
			{
				store.renamePreset(preset, name);
			}
		});
	}

	private void deletePreset()
	{
		activePreset().ifPresent(preset ->
		{
			long tiles = points.stream().filter(p -> preset.getId().equals(p.getPresetId())).count();
			int result = JOptionPane.showConfirmDialog(this,
					"Delete strategy preset \"" + preset.getName() + "\" and its " + tiles + " tile" + (tiles == 1 ? "" : "s") + "?",
					"Delete strategy preset", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (result == JOptionPane.OK_OPTION)
			{
				selected = null;
				store.deletePreset(preset, ArenaMapLayout.REGION_IDS);
			}
		});
	}

	@Nullable
	private String promptForName(String title, String initial)
	{
		String name = (String) JOptionPane.showInputDialog(this, "Name", title, JOptionPane.PLAIN_MESSAGE, null, null, initial);
		name = name == null ? null : name.trim();
		return Strings.isNullOrEmpty(name) ? null : name;
	}

	// ---- tiles ----

	private void onTileClicked(int mapX, int mapY, MouseEvent event)
	{
		applyLabel();
		selectedMapX = mapX;
		selectedMapY = mapY;
		EditorLayers layers = layers();
		List<GroundMarkerPoint> atTile = layers.editableAt(points, mapX, mapY);

		if (SwingUtilities.isRightMouseButton(event))
		{
			selected = null;
			if (atTile.isEmpty())
			{
				refresh();
				return;
			}
			// re-read: applyLabel above may have just saved a change that `points` does not reflect yet
			List<GroundMarkerPoint> remaining = new ArrayList<>(store.getPoints(layers.layout().getRegionId()));
			remaining.removeIf(p -> layers.layout().isAt(p, mapX, mapY) && layers.isEditable(p));
			store.savePoints(layers.layout().getRegionId(), remaining);
			return;
		}

		if (atTile.isEmpty())
		{
			addMarker(mapX, mapY);
		}
		else
		{
			selected = atTile.get(0);
			refresh();
		}
	}

	private void addMarker(int mapX, int mapY)
	{
		GroundMarkerPoint point = layers().newPoint(mapX, mapY, config.markerColor());
		selected = point;
		store.addPoint(point);
	}

	private void updateSelected(java.util.function.UnaryOperator<GroundMarkerPoint> update)
	{
		if (selected == null || refreshing)
		{
			return;
		}
		updatePoint(selected, update);
	}

	private void updatePoint(GroundMarkerPoint point, java.util.function.UnaryOperator<GroundMarkerPoint> update)
	{
		GroundMarkerPoint updated = update.apply(point);
		if (updated.equals(point))
		{
			return;
		}
		if (point.equals(selected))
		{
			selected = updated;
		}
		store.updatePoint(point, p -> updated);
	}

	private void pickColor()
	{
		if (selected == null)
		{
			return;
		}
		// the picker is modeless: apply its color to the marker it was opened for, even if the selection moved on
		GroundMarkerPoint target = selected;
		RuneliteColorPicker picker = colorPickerManager.create(this, colorButton.getColor(), "Tile marker color", false);
		picker.setOnClose(color -> updatePoint(target, p -> p.withColor(color)));
		picker.setVisible(true);
	}

	private void applyLabel()
	{
		String label = Strings.emptyToNull(labelField.getText().trim());
		if (label != null && label.length() > MAX_LABEL_LENGTH)
		{
			label = label.substring(0, MAX_LABEL_LENGTH);
		}
		String newLabel = label;
		updateSelected(p -> Objects.equals(p.getLabel(), newLabel) ? p : p.withLabel(newLabel));
	}

	private void applyWaves()
	{
		List<Integer> waves = new ArrayList<>();
		for (int i = 0; i < waveBoxes.size(); i++)
		{
			if (waveBoxes.get(i).isSelected())
			{
				waves.add(i + 1);
			}
		}
		if (waves.isEmpty())
		{
			refresh();
			return;
		}
		updateSelected(p -> p.withWaves(waves));
	}

	private void applyRoles()
	{
		List<String> roles = new ArrayList<>();
		for (int i = 0; i < roleBoxes.size(); i++)
		{
			if (roleBoxes.get(i).isSelected())
			{
				roles.add(BARole.values()[i].getCode());
			}
		}
		if (roles.isEmpty())
		{
			refresh();
			return;
		}
		updateSelected(p -> p.withRoles(roles.stream().sorted().collect(Collectors.toList())));
	}

	private static final class MarkerOption
	{
		private final GroundMarkerPoint point;
		private final int number;

		private MarkerOption(GroundMarkerPoint point, int number)
		{
			this.point = point;
			this.number = number;
		}

		@Override
		public String toString()
		{
			return "Marker " + number + (point.getLabel() == null ? "" : " - " + point.getLabel());
		}
	}
}
