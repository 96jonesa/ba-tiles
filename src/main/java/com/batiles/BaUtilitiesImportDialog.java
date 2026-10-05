package com.batiles;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;

/**
 * Pop-up window for importing BA Utilities' tile setup into BA Tiles: says what the import will do, and lets the user
 * choose the waves and roles each strategy becomes presets for, which lineups to import, and rename anything whose
 * name is already taken.
 */
@Slf4j
class BaUtilitiesImportDialog extends JDialog
{
	private static final Color ERROR_COLOR = new Color(255, 110, 110);
	private static final String[] ROLE_LETTERS = {"A", "C", "D", "H"};

	private final BATilesStore store;
	private final Gson gson;
	// names can be taken from the sidebar or editor while this window is open
	private final Runnable storeListener = () -> SwingUtilities.invokeLater(this::update);

	private final JLabel summary = new JLabel();
	private final JPanel strategiesPanel = new JPanel(new GridBagLayout());
	private final JPanel lineupsPanel = new JPanel(new GridBagLayout());
	private final JCheckBox activateBox = new JCheckBox();
	private final JButton importButton = new JButton("Import");

	private BaUtilitiesImport.Plan plan = BaUtilitiesImport.plan(null);
	private final List<StrategyRow> strategyRows = new ArrayList<>();
	private final List<LineupRow> lineupRows = new ArrayList<>();
	private boolean updating;

	BaUtilitiesImportDialog(Window owner, BATilesStore store, Gson gson)
	{
		super(owner, "Import from BA Utilities", ModalityType.MODELESS);
		this.store = store;
		this.gson = gson;
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		JLabel source = new JLabel("<html>Imports this profile's BA Utilities tile setup (only read, never changed),"
				+ " along with BA Utilities' built-in strategies and lineups.</html>");
		source.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel lists = new JPanel();
		lists.setLayout(new BoxLayout(lists, BoxLayout.Y_AXIS));
		lists.add(section("Strategies: each becomes a preset on every ticked wave, for every ticked role"
				+ "<br><span style='font-weight:normal'>Greyed-out ticks are needed by a lineup being imported; untick the lineup"
				+ " below to change them.</span>"));
		lists.add(topLeft(strategiesPanel));
		lists.add(Box.createVerticalStrut(12));
		lists.add(section("Lineups (BA Utilities assignment presets)"));
		lists.add(topLeft(lineupsPanel));
		JPanel listsHolder = new JPanel(new BorderLayout());
		listsHolder.add(lists, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(listsHolder);
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		activateBox.addActionListener(e -> update());
		importButton.addActionListener(e -> doImport());
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> setVisible(false));
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		buttons.add(cancel);
		buttons.add(importButton);

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		source.setAlignmentX(LEFT_ALIGNMENT);
		summary.setAlignmentX(LEFT_ALIGNMENT);
		activateBox.setAlignmentX(LEFT_ALIGNMENT);
		top.add(source);
		top.add(Box.createVerticalStrut(8));
		top.add(summary);
		top.add(Box.createVerticalStrut(4));
		top.add(activateBox);

		JPanel content = new JPanel(new BorderLayout(8, 8));
		content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		content.add(top, BorderLayout.NORTH);
		content.add(scroll, BorderLayout.CENTER);
		content.add(buttons, BorderLayout.SOUTH);
		setContentPane(content);

		setSize(980, 820);
		setLocationRelativeTo(owner);
		store.addListener(storeListener);
	}

	@Override
	public void dispose()
	{
		store.removeListener(storeListener);
		super.dispose();
	}

	void open()
	{
		load();
		setVisible(true);
		toFront();
	}

	/**
	 * Keeps a table at its preferred size in the top-left corner, instead of centered in the space it is given.
	 */
	private static JPanel topLeft(JPanel table)
	{
		JPanel holder = new JPanel(new BorderLayout());
		holder.add(table, BorderLayout.WEST);
		holder.setAlignmentX(LEFT_ALIGNMENT);
		return holder;
	}

	private static JLabel section(String text)
	{
		JLabel label = new JLabel("<html><b>" + text + "</b></html>");
		label.setAlignmentX(LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		return label;
	}

	/**
	 * Reads this profile's BA Utilities setup and rebuilds the lists with the default choices.
	 */
	private void load()
	{
		BaUtilitiesData.Store saved = null;
		try
		{
			String json = store.getBaUtilitiesSetupJson();
			saved = json == null ? null : gson.fromJson(json, BaUtilitiesData.Store.class);
		}
		catch (JsonParseException | IllegalStateException ex)
		{
			log.warn("Unable to read BA Utilities' setup", ex);
			JOptionPane.showMessageDialog(this, "Unable to read BA Utilities' setup on this profile."
					+ " Only BA Utilities' built-in strategies and lineups can be imported.", getTitle(), JOptionPane.WARNING_MESSAGE);
		}

		plan = BaUtilitiesImport.plan(saved);
		BaUtilitiesImport.Choices defaults = BaUtilitiesImport.Choices.defaults(plan);
		buildStrategyRows(defaults);
		buildLineupRows(defaults);
		activateBox.setSelected(defaults.isActivateCurrentSetup());
		update();
	}

	// ---- rows ----

	private final class StrategyRow
	{
		final BaUtilitiesImport.Strategy strategy;
		final JTextField name = new JTextField(14);
		final List<JCheckBox> waves = new ArrayList<>();
		final List<JCheckBox> roles = new ArrayList<>();
		final JLabel error = new JLabel();

		StrategyRow(BaUtilitiesImport.Strategy strategy, BaUtilitiesImport.Choices defaults)
		{
			this.strategy = strategy;
			name.setText(defaults.getPresetNames().get(strategy.getId()));
			name.getDocument().addDocumentListener(onChange());
			for (int wave : strategy.possibleWaves())
			{
				JCheckBox box = new JCheckBox(strategy.possibleWaves().size() == 1 ? "Wave 10 only" : Integer.toString(wave));
				box.setSelected(defaults.getWaves().get(strategy.getId()).contains(wave));
				box.putClientProperty("wave", wave);
				box.addActionListener(e -> update());
				waves.add(box);
			}
			for (int i = 0; i < BATilesPlugin.ALL_ROLES.size(); i++)
			{
				String role = BATilesPlugin.ALL_ROLES.get(i);
				JCheckBox box = new JCheckBox(ROLE_LETTERS[i]);
				box.setToolTipText(BARole.fromCode(role).getDisplayName());
				box.setSelected(defaults.getRoles().get(strategy.getId()).contains(role));
				box.putClientProperty("role", role);
				box.addActionListener(e -> update());
				roles.add(box);
			}
			error.setForeground(ERROR_COLOR);
		}

		Set<Integer> chosenWaves()
		{
			return waves.stream().filter(JCheckBox::isSelected).map(b -> (Integer) b.getClientProperty("wave")).collect(Collectors.toCollection(TreeSet::new));
		}

		Set<String> chosenRoles()
		{
			return roles.stream().filter(JCheckBox::isSelected).map(b -> (String) b.getClientProperty("role")).collect(Collectors.toCollection(TreeSet::new));
		}
	}

	private final class LineupRow
	{
		final BaUtilitiesImport.LineupSource lineup;
		final JCheckBox include = new JCheckBox();
		final JTextField name = new JTextField(14);
		final JLabel error = new JLabel();

		LineupRow(BaUtilitiesImport.LineupSource lineup, BaUtilitiesImport.Choices defaults)
		{
			this.lineup = lineup;
			include.setSelected(defaults.getLineupNames().containsKey(lineup.getLineupId()));
			include.addActionListener(e -> update());
			name.setText(lineup.getName());
			name.getDocument().addDocumentListener(onChange());
			error.setForeground(ERROR_COLOR);
		}
	}

	private DocumentListener onChange()
	{
		return new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				update();
			}
		};
	}

	private void buildStrategyRows(BaUtilitiesImport.Choices defaults)
	{
		strategiesPanel.removeAll();
		strategyRows.clear();
		GridBagConstraints c = constraints();
		String[] headers = {"Preset name", "", "Tiles", "Waves", "Roles"};
		for (int i = 0; i < headers.length; i++)
		{
			c.gridx = i;
			strategiesPanel.add(header(headers[i]), c);
		}

		for (BaUtilitiesImport.Strategy strategy : plan.getStrategies())
		{
			StrategyRow row = new StrategyRow(strategy, defaults);
			strategyRows.add(row);
			c.gridy++;
			c.gridx = 0;
			strategiesPanel.add(row.name, c);
			c.gridx = 1;
			String tags = (strategy.isBuiltIn() ? "built-in" : "") + (strategy.isHasNotes() ? (strategy.isBuiltIn() ? "<br>" : "") + "has notes" : "");
			strategiesPanel.add(dim("<html>" + tags + "</html>"), c);
			c.gridx = 2;
			strategiesPanel.add(new JLabel(Integer.toString(strategy.getMarkers().size())), c);
			c.gridx = 3;
			strategiesPanel.add(boxes(row.waves), c);
			c.gridx = 4;
			strategiesPanel.add(boxes(row.roles), c);
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 5;
			strategiesPanel.add(row.error, c);
			c.gridwidth = 1;
		}
		strategiesPanel.revalidate();
	}

	private void buildLineupRows(BaUtilitiesImport.Choices defaults)
	{
		lineupsPanel.removeAll();
		lineupRows.clear();
		GridBagConstraints c = constraints();
		String[] headers = {"Import", "Lineup name", "Role", "", "Waves"};
		for (int i = 0; i < headers.length; i++)
		{
			c.gridx = i;
			lineupsPanel.add(header(headers[i]), c);
		}

		if (plan.getLineups().isEmpty())
		{
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 5;
			lineupsPanel.add(dim("None"), c);
			c.gridwidth = 1;
		}
		for (BaUtilitiesImport.LineupSource lineup : plan.getLineups())
		{
			LineupRow row = new LineupRow(lineup, defaults);
			lineupRows.add(row);
			c.gridy++;
			c.gridx = 0;
			lineupsPanel.add(row.include, c);
			c.gridx = 1;
			lineupsPanel.add(row.name, c);
			c.gridx = 2;
			lineupsPanel.add(new JLabel(BARole.fromCode(lineup.getRole()).getDisplayName()), c);
			c.gridx = 3;
			List<String> tags = new ArrayList<>();
			if (lineup.isActive())
			{
				tags.add("active in BA Utilities");
			}
			if (lineup.isFromGlobal())
			{
				tags.add("from a GLOBAL lineup");
			}
			lineupsPanel.add(dim(String.join(", ", tags)), c);
			c.gridx = 4;
			lineupsPanel.add(dim("<html><div style='width:320px'>" + describeWaves(lineup) + "</div></html>"), c);
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 5;
			lineupsPanel.add(row.error, c);
			c.gridwidth = 1;
		}
		lineupsPanel.revalidate();
	}

	private String describeWaves(BaUtilitiesImport.LineupSource lineup)
	{
		if (lineup.getStrategyIds().isEmpty())
		{
			return "no waves";
		}
		return lineup.getStrategyIds().entrySet().stream()
				.map(e ->
				{
					BaUtilitiesImport.Strategy strategy = plan.strategy(e.getValue());
					return "w" + e.getKey() + " " + (strategy == null ? "?" : strategy.getName());
				})
				.collect(Collectors.joining(", "));
	}

	private static GridBagConstraints constraints()
	{
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(1, 0, 1, 10);
		return c;
	}

	private static JLabel header(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private static JLabel dim(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private static JPanel boxes(List<JCheckBox> boxes)
	{
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		boxes.forEach(panel::add);
		return panel;
	}

	// ---- choices ----

	/**
	 * @return the choices as the rows show them (before forcing what imported lineups need)
	 */
	private BaUtilitiesImport.Choices choices()
	{
		Map<String, String> names = new LinkedHashMap<>();
		Map<String, Set<Integer>> waves = new LinkedHashMap<>();
		Map<String, Set<String>> roles = new LinkedHashMap<>();
		for (StrategyRow row : strategyRows)
		{
			names.put(row.strategy.getId(), row.name.getText().trim());
			waves.put(row.strategy.getId(), row.chosenWaves());
			roles.put(row.strategy.getId(), row.chosenRoles());
		}
		Map<String, String> lineupNames = new LinkedHashMap<>();
		for (LineupRow row : lineupRows)
		{
			if (row.include.isSelected())
			{
				lineupNames.put(row.lineup.getLineupId(), row.name.getText().trim());
			}
		}
		return new BaUtilitiesImport.Choices(names, waves, roles, lineupNames, activateBox.isSelected());
	}

	/**
	 * Applies what imported lineups need to the checkboxes, then shows collisions and the summary.
	 */
	private void update()
	{
		if (updating)
		{
			return;
		}
		updating = true;
		try
		{
			BaUtilitiesImport.Choices raw = choices();
			Map<String, Set<Integer>> requiredWaves = BaUtilitiesImport.requiredWaves(plan, raw);
			Map<String, Set<String>> requiredRoles = BaUtilitiesImport.requiredRoles(plan, raw);
			for (StrategyRow row : strategyRows)
			{
				Set<Integer> waves = requiredWaves.getOrDefault(row.strategy.getId(), Set.of());
				Set<String> roles = requiredRoles.getOrDefault(row.strategy.getId(), Set.of());
				for (JCheckBox box : row.waves)
				{
					force(box, waves.contains((Integer) box.getClientProperty("wave")));
				}
				for (JCheckBox box : row.roles)
				{
					force(box, roles.contains((String) box.getClientProperty("role")));
				}
			}

			BaUtilitiesImport.Choices choices = choices();
			Map<String, String> presetCollisions = BaUtilitiesImport.presetNameCollisions(plan, choices, store.getPresets());
			Map<String, String> lineupCollisions = BaUtilitiesImport.lineupNameCollisions(plan, choices, store.getLineups());
			for (StrategyRow row : strategyRows)
			{
				String collision = presetCollisions.get(row.strategy.getId());
				row.error.setText(collision == null ? "" : "Rename: " + collision + ".");
				row.error.setVisible(collision != null);
				row.name.setEnabled(choices.includes(row.strategy.getId()));
			}
			for (LineupRow row : lineupRows)
			{
				String collision = lineupCollisions.get(row.lineup.getLineupId());
				row.error.setText(collision == null ? "" : "Rename: " + collision + ".");
				row.error.setVisible(collision != null);
				row.name.setEnabled(row.include.isSelected());
			}

			summary.setText(summaryText(choices));
			boolean anything = strategyRows.stream().anyMatch(r -> choices.includes(r.strategy.getId()))
					|| !choices.getLineupNames().isEmpty() || !plan.getGlobalSelections().isEmpty();
			importButton.setEnabled(anything && presetCollisions.isEmpty() && lineupCollisions.isEmpty());
			importButton.setToolTipText(presetCollisions.isEmpty() && lineupCollisions.isEmpty() ? null : "Rename everything marked in red first");
		}
		finally
		{
			updating = false;
		}
	}

	/**
	 * Ticks and locks a checkbox an imported lineup needs; unlocks it otherwise.
	 */
	private static void force(JCheckBox box, boolean required)
	{
		if (required)
		{
			box.setSelected(true);
		}
		box.setEnabled(!required);
		box.setToolTipText(required ? "Needed by an imported lineup" : box.getClientProperty("role") == null ? null
				: BARole.fromCode((String) box.getClientProperty("role")).getDisplayName());
	}

	private String summaryText(BaUtilitiesImport.Choices choices)
	{
		int presets = 0;
		int tiles = 0;
		for (StrategyRow row : strategyRows)
		{
			if (choices.includes(row.strategy.getId()))
			{
				int count = row.chosenWaves().size() * row.chosenRoles().size();
				presets += count;
				tiles += count * row.strategy.getMarkers().size();
			}
		}
		long notes = plan.getStrategies().stream().filter(BaUtilitiesImport.Strategy::isHasNotes).count();
		Set<String> switchedRoles = new TreeSet<>();
		for (LineupRow row : lineupRows)
		{
			if (row.include.isSelected() && row.lineup.isActive())
			{
				switchedRoles.add(BARole.fromCode(row.lineup.getRole()).getDisplayName());
			}
		}
		activateBox.setText("Switch " + (switchedRoles.isEmpty() ? "roles" : String.join(", ", switchedRoles))
				+ " to their BA Utilities setup now (changes which presets are active)");
		activateBox.setEnabled(!switchedRoles.isEmpty());

		StringBuilder text = new StringBuilder("<html><div style='width:680px'><b>This import will:</b><ul style='margin-left:16px'>");
		text.append("<li>Create <b>").append(presets).append(" BA Tiles strategy presets</b> (").append(tiles)
				.append(" tiles): each strategy below becomes a preset, with the same name, on every ticked wave for every ticked role."
						+ " Tiles keep their colors, labels, fill opacity and border width. Built-in strategies start out ticked for Defender only.</li>");
		text.append("<li>Create <b>").append(choices.getLineupNames().size()).append(" lineups</b> from BA Utilities' assignment presets"
				+ " and each role's current setup. A GLOBAL (all roles) assignment preset becomes a lineup for each role.</li>");
		if (!plan.getGlobalSelections().isEmpty())
		{
			text.append("<li>Add the tiles of BA Utilities' <b>GLOBAL</b> (all roles) selections, on wave")
					.append(plan.getGlobalSelections().size() == 1 ? " " : "s ")
					.append(plan.getGlobalSelections().keySet().stream().map(String::valueOf).collect(Collectors.joining(", ")))
					.append(", as tiles that are <b>not part of a preset</b>, shown for all roles on those waves, and turn on"
							+ " <b>Others</b> for those waves so they also show alongside presets.</li>");
		}
		text.append("<li><b>Not import strategy notes</b>: BA Tiles has no notes")
				.append(notes > 0 ? " (" + notes + " strateg" + (notes == 1 ? "y has" : "ies have") + " notes, marked \"has notes\" below)" : "")
				.append(".</li>");
		text.append("<li>Keep everything already in BA Tiles; nothing is removed. If you imported before, presets and GLOBAL"
				+ " tiles from that import are kept as they are (including any changes you made), and lineups from it are"
				+ " updated to BA Utilities' current wave assignments.</li>");
		text.append("</ul></div></html>");
		return text.toString();
	}

	// ---- import ----

	private void doImport()
	{
		BaUtilitiesImport.Choices choices = choices();
		// re-check against what BA Tiles has now, in case something was created or renamed since the last update
		if (!BaUtilitiesImport.presetNameCollisions(plan, choices, store.getPresets()).isEmpty()
				|| !BaUtilitiesImport.lineupNameCollisions(plan, choices, store.getLineups()).isEmpty())
		{
			update();
			JOptionPane.showMessageDialog(this, "Some names are now already taken in BA Tiles. Rename everything marked in red first.",
					getTitle(), JOptionPane.WARNING_MESSAGE);
			return;
		}
		BaUtilitiesImport.Result result = BaUtilitiesImport.apply(store, plan, choices);
		StringBuilder text = new StringBuilder("<html><div style='width:320px'>Imported from BA Utilities:<ul>");
		text.append("<li>").append(result.getPresetsCreated()).append(" presets with ").append(result.getPresetTiles()).append(" tiles</li>");
		text.append("<li>").append(result.getLineupsCreated()).append(" lineups</li>");
		if (result.getGlobalTiles() > 0)
		{
			text.append("<li>").append(result.getGlobalTiles()).append(" GLOBAL tiles, not part of a preset</li>");
		}
		if (result.getLineupsActivated() > 0)
		{
			text.append("<li>").append(result.getLineupsActivated()).append(" roles switched to their BA Utilities setup</li>");
		}
		text.append("</ul>");
		if (result.getPresetsAlreadyImported() > 0)
		{
			text.append("Kept ").append(result.getPresetsAlreadyImported())
					.append(" presets from an earlier import as they are. ");
		}
		if (result.getLineupsAlreadyImported() > 0)
		{
			text.append("Updated ").append(result.getLineupsAlreadyImported())
					.append(" lineups from an earlier import to BA Utilities' current wave assignments.");
		}
		text.append("</div></html>");
		setVisible(false);
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(getOwner(), text.toString(), getTitle(), JOptionPane.INFORMATION_MESSAGE));
	}
}
