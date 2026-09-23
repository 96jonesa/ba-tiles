package com.batiles;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * Sidebar panel: switch the active strategy preset for a wave / role, and open the tile map editor.
 */
class BATilesPanel extends PluginPanel
{
	private final BATilesStore store;
	private final Supplier<TileMapEditor> editor;
	private final Runnable storeListener = () -> SwingUtilities.invokeLater(this::refresh);

	private final JComboBox<Integer> waveCombo = new JComboBox<>(IntStream.rangeClosed(1, 10).boxed().toArray(Integer[]::new));
	private final JComboBox<BARole> roleCombo = new JComboBox<>(BARole.values());
	private final JComboBox<Object> presetCombo = new JComboBox<>();
	private final JCheckBox followGame = new JCheckBox("Follow current wave and role", true);
	private boolean refreshing;

	BATilesPanel(BATilesStore store, Supplier<TileMapEditor> editor)
	{
		this.store = store;
		this.editor = editor;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel column = new JPanel(new GridLayout(0, 1, 0, 6));

		JLabel title = new JLabel("BA Tiles");
		title.setForeground(ColorScheme.BRAND_ORANGE);
		column.add(title);

		JButton openEditor = new JButton("Open tile map editor");
		openEditor.addActionListener(e -> editor.get().open((Integer) waveCombo.getSelectedItem(), role()));
		column.add(openEditor);

		column.add(new JLabel("Active strategy preset"));
		JPanel waveRole = new JPanel(new GridLayout(1, 2, 6, 0));
		waveRole.add(waveCombo);
		waveRole.add(roleCombo);
		column.add(waveRole);
		column.add(presetCombo);
		column.add(followGame);

		JLabel help = new JLabel("<html>A preset's tiles are only shown while it is the active preset for its wave and role.</html>");
		help.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		column.add(help);

		waveCombo.addActionListener(e -> refresh());
		roleCombo.addActionListener(e -> refresh());
		presetCombo.addActionListener(e ->
		{
			if (refreshing)
			{
				return;
			}
			Object item = presetCombo.getSelectedItem();
			store.setActivePreset((Integer) waveCombo.getSelectedItem(), role(),
					item instanceof PresetOption ? ((PresetOption) item).getPreset() : null);
		});

		add(column, BorderLayout.NORTH);
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
		SwingUtilities.invokeLater(() ->
		{
			if (followGame.isSelected())
			{
				waveCombo.setSelectedItem(wave);
				roleCombo.setSelectedItem(BARole.fromCode(role));
			}
		});
	}

	private String role()
	{
		return ((BARole) roleCombo.getSelectedItem()).getCode();
	}

	private void refresh()
	{
		refreshing = true;
		try
		{
			int wave = (Integer) waveCombo.getSelectedItem();
			List<StrategyPreset> presets = store.getPresets(wave, role());
			String activeId = store.getActivePresetId(wave, role());
			presetCombo.removeAllItems();
			presetCombo.addItem(PresetOption.NO_PRESET);
			Object active = PresetOption.NO_PRESET;
			for (StrategyPreset preset : presets)
			{
				PresetOption option = new PresetOption(preset);
				presetCombo.addItem(option);
				if (preset.getId().equals(activeId))
				{
					active = option;
				}
			}
			presetCombo.setSelectedItem(active);
		}
		finally
		{
			refreshing = false;
		}
	}
}
