package com.batiles;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
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
 * Sidebar panel: for a role, switch the active strategy preset of each wave; and open the tile map editor.
 */
class BATilesPanel extends PluginPanel
{
	private static final int WAVES = 10;

	private final BATilesStore store;
	private final Supplier<TileMapEditor> editor;
	private final Runnable storeListener = () -> SwingUtilities.invokeLater(this::refresh);

	private final JComboBox<BARole> roleCombo = new JComboBox<>(BARole.values());
	private final List<JComboBox<Object>> presetCombos = new ArrayList<>();
	private final JCheckBox followGame = new JCheckBox("Follow current role", true);
	// the editor opens on the in-game wave
	private int currentWave = 1;
	private boolean refreshing;

	BATilesPanel(BATilesStore store, Supplier<TileMapEditor> editor)
	{
		this.store = store;
		this.editor = editor;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel top = new JPanel(new GridLayout(0, 1, 0, 6));

		JLabel title = new JLabel("BA Tiles");
		title.setForeground(ColorScheme.BRAND_ORANGE);
		top.add(title);

		JButton openEditor = new JButton("Open tile map editor");
		openEditor.addActionListener(e -> editor.get().open(currentWave, role()));
		top.add(openEditor);

		top.add(new JLabel("Active strategy presets"));
		top.add(roleCombo);
		top.add(followGame);

		JPanel waves = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 0, 6, 6);
		c.fill = GridBagConstraints.HORIZONTAL;
		for (int i = 0; i < WAVES; i++)
		{
			int wave = i + 1;
			JLabel label = new JLabel("Wave " + wave);
			JComboBox<Object> combo = new JComboBox<>();
			combo.addActionListener(e -> onPresetSelected(wave, combo));
			presetCombos.add(combo);

			c.gridy = i;
			c.gridx = 0;
			c.weightx = 0;
			waves.add(label, c);
			c.gridx = 1;
			c.weightx = 1;
			waves.add(combo, c);
		}

		JLabel help = new JLabel("<html>A preset's tiles are only shown while it is the active preset for its wave and role.</html>");
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
		SwingUtilities.invokeLater(() ->
		{
			currentWave = wave;
			if (followGame.isSelected())
			{
				roleCombo.setSelectedItem(BARole.fromCode(role));
			}
		});
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
				// nothing to choose between until the wave has a preset for this role
				combo.setEnabled(!presets.isEmpty());
			}
		}
		finally
		{
			refreshing = false;
		}
	}
}
