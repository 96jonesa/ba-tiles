package com.batiles;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * Sidebar panel: for a role, switch the active strategy preset of each wave; and open the tile map editor.
 */
class BATilesPanel extends PluginPanel
{
	private static final int WAVES = 10;
	private static final String SHOW_BASE_TOOLTIP = "Show tiles that are not part of a preset while this wave's preset is active";

	private final BATilesStore store;
	private final Supplier<TileMapEditor> editor;
	private final Supplier<GroundMarkerImportDialog> importDialog;
	private final Gson gson;
	private final Runnable storeListener = () -> SwingUtilities.invokeLater(this::refresh);

	private final JComboBox<BARole> roleCombo = new JComboBox<>(BARole.values());
	private final List<JComboBox<Object>> presetCombos = new ArrayList<>();
	private final List<JCheckBox> showBaseBoxes = new ArrayList<>();
	// the editor opens on the in-game wave
	private int currentWave = 1;
	private boolean refreshing;

	BATilesPanel(BATilesStore store, Supplier<TileMapEditor> editor, Supplier<GroundMarkerImportDialog> importDialog,
				 Gson gson)
	{
		this.store = store;
		this.editor = editor;
		this.importDialog = importDialog;
		this.gson = gson;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel top = new JPanel(new GridLayout(0, 1, 0, 6));

		JLabel title = new JLabel("BA Tiles");
		title.setForeground(ColorScheme.BRAND_ORANGE);
		top.add(title);

		JButton openEditor = new JButton("Open tile map editor");
		openEditor.addActionListener(e -> editor.get().open(currentWave, role()));
		top.add(openEditor);

		JButton importGroundMarkers = new JButton("Import BA ground markers");
		importGroundMarkers.setToolTipText("Convert Ground Markers markers in the arena into BA Tiles");
		importGroundMarkers.addActionListener(e -> importDialog.get().open());
		top.add(importGroundMarkers);

		JPanel backup = new JPanel(new GridLayout(1, 2, 6, 0));
		JButton exportAll = new JButton("Export all");
		exportAll.setToolTipText("Copy all of BA Tiles' tiles, presets and settings to the clipboard");
		exportAll.addActionListener(e -> exportAll());
		JButton importAll = new JButton("Import all");
		importAll.setToolTipText("Replace all of BA Tiles' tiles, presets and settings with a backup from the clipboard");
		importAll.addActionListener(e -> importAll());
		backup.add(exportAll);
		backup.add(importAll);
		top.add(backup);

		top.add(new JLabel("Active strategy presets"));
		top.add(roleCombo);

		JPanel waves = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 0, 6, 6);
		c.fill = GridBagConstraints.HORIZONTAL;

		c.gridy = 0;
		c.gridx = 1;
		waves.add(columnHeader("Preset", null), c);
		c.gridx = 2;
		c.fill = GridBagConstraints.NONE;
		waves.add(columnHeader("Others", SHOW_BASE_TOOLTIP), c);
		c.fill = GridBagConstraints.HORIZONTAL;

		for (int i = 0; i < WAVES; i++)
		{
			int wave = i + 1;
			JLabel label = new JLabel("Wave " + wave);
			JComboBox<Object> combo = new JComboBox<>();
			combo.addActionListener(e -> onPresetSelected(wave, combo));
			presetCombos.add(combo);
			JCheckBox showBase = new JCheckBox();
			showBase.setToolTipText(SHOW_BASE_TOOLTIP);
			showBase.addActionListener(e ->
			{
				if (!refreshing)
				{
					store.setShowBaseWithPreset(wave, role(), showBase.isSelected());
				}
			});
			showBaseBoxes.add(showBase);

			c.gridy = i + 1;
			c.gridx = 0;
			c.weightx = 0;
			waves.add(label, c);
			c.gridx = 1;
			c.weightx = 1;
			waves.add(combo, c);
			c.gridx = 2;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			waves.add(showBase, c);
			c.fill = GridBagConstraints.HORIZONTAL;
		}

		JLabel help = new JLabel("<html>A preset's tiles are only shown while it is the active preset for its wave and role."
				+ "<br><br><b>Others</b>: also show tiles that are not part of a preset while a preset is active."
				+ " They are always shown while no preset is.</html>");
		help.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel column = new JPanel(new BorderLayout(0, 8));
		column.add(top, BorderLayout.NORTH);
		column.add(waves, BorderLayout.CENTER);
		column.add(help, BorderLayout.SOUTH);
		add(column, BorderLayout.NORTH);

		roleCombo.addActionListener(e -> refresh());
		store.addListener(storeListener);
		refresh();
	}

	void shutDown()
	{
		store.removeListener(storeListener);
	}

	/**
	 * Called when the in-game wave or role changes.
	 */
	void onGameStateChanged(int wave, String role)
	{
		SwingUtilities.invokeLater(() -> currentWave = wave);
	}

	private static JLabel columnHeader(String text, String tooltip)
	{
		JLabel header = new JLabel(text);
		header.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		header.setToolTipText(tooltip);
		return header;
	}

	private void exportAll()
	{
		BATilesBackup backup = store.exportAll();
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(backup.toJson(gson)), null);
		message("Copied all of BA Tiles to the clipboard: " + backup.describe(gson) + ".", JOptionPane.INFORMATION_MESSAGE);
	}

	private void importAll()
	{
		BATilesBackup backup;
		try
		{
			String text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			backup = BATilesBackup.fromJson(gson, text);
		}
		catch (IOException | UnsupportedFlavorException | IllegalStateException | ClassCastException | JsonParseException ex)
		{
			message("The clipboard does not contain a BA Tiles backup. Use \"Export all\" to make one.",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String current = store.exportAll().describe(gson);
		int result = JOptionPane.showOptionDialog(this,
				html("Replace <b>all</b> of BA Tiles on this profile with the backup from the clipboard?<br><br>"
						+ "<b>Backup:</b> " + backup.describe(gson) + "<br>"
						+ "<b>Replaced:</b> " + current + "<br><br>"
						+ "This can't be undone. Use \"Export all\" first if you want to keep what you have."),
				"Import all BA Tiles", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
				new Object[]{"Replace everything", "Cancel"}, "Cancel");
		if (result == 0)
		{
			store.replaceAll(backup);
			message("Imported the backup: " + backup.describe(gson) + ".", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void message(String text, int type)
	{
		JOptionPane.showMessageDialog(this, html(text), "BA Tiles", type);
	}

	private static String html(String text)
	{
		return "<html><div style='width:260px'>" + text + "</div></html>";
	}

	private String role()
	{
		return ((BARole) roleCombo.getSelectedItem()).getCode();
	}

	private void onPresetSelected(int wave, JComboBox<Object> combo)
	{
		if (refreshing)
		{
			return;
		}
		Object item = combo.getSelectedItem();
		store.setActivePreset(wave, role(), item instanceof PresetOption ? ((PresetOption) item).getPreset() : null);
	}

	private void refresh()
	{
		refreshing = true;
		try
		{
			for (int i = 0; i < WAVES; i++)
			{
				int wave = i + 1;
				JComboBox<Object> combo = presetCombos.get(i);
				List<StrategyPreset> presets = store.getPresets(wave, role());
				String activeId = store.getActivePresetId(wave, role());
				combo.removeAllItems();
				combo.addItem(PresetOption.NO_PRESET);
				Object active = PresetOption.NO_PRESET;
				for (StrategyPreset preset : presets)
				{
					PresetOption option = new PresetOption(preset);
					combo.addItem(option);
					if (preset.getId().equals(activeId))
					{
						active = option;
					}
				}
				combo.setSelectedItem(active);
				// nothing to choose between, and no preset to show other tiles alongside, until the wave has a preset for this role
				combo.setEnabled(!presets.isEmpty());
				JCheckBox showBase = showBaseBoxes.get(i);
				showBase.setSelected(store.isShowBaseWithPreset(wave, role()));
				showBase.setEnabled(!presets.isEmpty());
			}
		}
		finally
		{
			refreshing = false;
		}
	}
}
