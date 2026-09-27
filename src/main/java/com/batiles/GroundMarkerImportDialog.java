package com.batiles;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseEvent;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;

/**
 * Pop-up window showing the Ground Markers plugin's markers on the arena maps (waves 1-9 and wave 10), for converting
 * them into BA Tiles shown on all waves for all roles.
 */
@Slf4j
class GroundMarkerImportDialog extends JDialog
{
	private static final int MIN_TILE_SIZE = 8;
	private static final int MAX_TILE_SIZE = 40;
	private static final int DEFAULT_TILE_SIZE = 16;
	private static final String THIS_PROFILE = "This profile";
	private static final String FLAG_HEX = String.format("#%06x", ArenaMapPanel.FLAG_COLOR.getRGB() & 0xffffff);

	private final BATilesStore store;
	private final Supplier<List<ProfileGroundMarkers.Source>> otherProfiles;
	private final Gson gson;
	private final Runnable storeListener = () -> SwingUtilities.invokeLater(this::refresh);

	private final JComboBox<Object> sourceCombo = new JComboBox<>();
	private final JComboBox<ArenaMapLayout> layoutCombo = new JComboBox<>(ArenaMapLayout.values());
	private final JComboBox<ArenaMapPanel.View> viewCombo = new JComboBox<>(ArenaMapPanel.View.values());
	private final JSlider zoom = new JSlider(MIN_TILE_SIZE, MAX_TILE_SIZE, DEFAULT_TILE_SIZE);
	private final JLabel summary = new JLabel();
	private final JButton selectAllButton = new JButton("Select all new");
	private final JButton clearButton = new JButton("Clear selection");
	private final JButton convertSelectedButton = new JButton();
	private final JButton convertAllButton = new JButton();
	private final ArenaMapPanel mapPanel;

	// ground markers of the layout being shown, and whether each already has its BA Tile
	private List<GroundMarkerPoint> markers = new ArrayList<>();
	private List<GroundMarkerPoint> tiles = new ArrayList<>();
	private final Set<GroundMarkerPoint> selected = new LinkedHashSet<>();
	// ground markers of the selected other profile, by region; null while the source is this profile
	private Map<Integer, List<GroundMarkerPoint>> profileMarkers;
	private boolean refreshingSources;

	GroundMarkerImportDialog(Window owner, BATilesStore store, Supplier<List<ProfileGroundMarkers.Source>> otherProfiles, Gson gson)
	{
		super(owner, "Import BA ground markers", ModalityType.MODELESS);
		this.store = store;
		this.otherProfiles = otherProfiles;
		this.gson = gson;
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		mapPanel = new ArenaMapPanel(
				() -> (ArenaMapLayout) layoutCombo.getSelectedItem(),
				() -> (ArenaMapPanel.View) viewCombo.getSelectedItem(),
				this::mapMarkers,
				zoom::getValue,
				this::onTileClicked);
		JScrollPane mapScrollPane = new JScrollPane(mapPanel);
		mapScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		mapScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

		JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		controls.add(new JLabel("Ground markers from"));
		controls.add(sourceCombo);
		controls.add(Box.createHorizontalStrut(12));
		controls.add(new JLabel("Map"));
		controls.add(layoutCombo);
		controls.add(new JLabel("View"));
		controls.add(viewCombo);
		controls.add(new JLabel("Zoom"));
		controls.add(zoom);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		actions.add(selectAllButton);
		actions.add(clearButton);
		actions.add(convertSelectedButton);
		actions.add(Box.createHorizontalStrut(12));
		actions.add(convertAllButton);

		JLabel help = new JLabel("<html>Ground markers, from this profile or another of your RuneLite profiles, are converted"
				+ " into BA Tiles <b>on this profile</b>, shown on <b>all waves for all roles</b>,"
				+ " with the same color and label. <b>Click</b> a marker to select or deselect it. Faded markers already have"
				+ " their BA Tile. Markers with an <font color='" + FLAG_HEX + "'>orange corner</font> are on a tile that"
				+ " already has a different BA Tile (another color, label, waves or roles), so converting them would stack"
				+ " two markers there. Your ground markers are left as they are.</html>");
		help.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		for (JPanel row : new JPanel[]{controls, actions})
		{
			row.setAlignmentX(LEFT_ALIGNMENT);
			top.add(row);
			top.add(Box.createVerticalStrut(4));
		}
		summary.setAlignmentX(LEFT_ALIGNMENT);
		help.setAlignmentX(LEFT_ALIGNMENT);
		top.add(summary);
		top.add(Box.createVerticalStrut(4));
		top.add(help);

		JPanel content = new JPanel(new BorderLayout(8, 8));
		content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		content.add(top, BorderLayout.NORTH);
		content.add(mapScrollPane, BorderLayout.CENTER);
		setContentPane(content);

		sourceCombo.addActionListener(e ->
		{
			if (!refreshingSources)
			{
				loadSource();
			}
		});
		layoutCombo.addActionListener(e ->
		{
			selected.clear();
			refresh();
		});
		viewCombo.addActionListener(e -> mapChanged());
		zoom.addChangeListener(e -> mapChanged());
		selectAllButton.addActionListener(e ->
		{
			selected.addAll(unconverted(markers, tiles));
			refresh();
		});
		clearButton.addActionListener(e ->
		{
			selected.clear();
			refresh();
		});
		convertSelectedButton.addActionListener(e -> confirmAndConvert(new ArrayList<>(selected)));
		convertAllButton.addActionListener(e -> convertAll());

		setSize(1000, 800);
		setLocationRelativeTo(owner);
		store.addListener(storeListener);
		refresh();
	}

	void open()
	{
		refreshSources();
		loadSource();
		setVisible(true);
		toFront();
	}

	@Override
	public void dispose()
	{
		store.removeListener(storeListener);
		super.dispose();
	}

	/**
	 * Lists this profile and the user's other profiles as sources, keeping the current choice if it still exists.
	 */
	private void refreshSources()
	{
		Object current = sourceCombo.getSelectedItem();
		refreshingSources = true;
		try
		{
			sourceCombo.removeAllItems();
			sourceCombo.addItem(THIS_PROFILE);
			List<ProfileGroundMarkers.Source> profiles;
			try
			{
				profiles = otherProfiles.get();
			}
			catch (RuntimeException ex)
			{
				log.warn("Unable to list RuneLite profiles", ex);
				profiles = List.of();
			}
			for (ProfileGroundMarkers.Source profile : profiles)
			{
				sourceCombo.addItem(profile);
			}
			// the first item (this profile) stays selected if the previous choice no longer exists
			if (current != null)
			{
				sourceCombo.setSelectedItem(current);
			}
		}
		finally
		{
			refreshingSources = false;
		}
	}

	/**
	 * Reads the selected source's ground markers (from its profile file, for another profile) and shows them.
	 */
	private void loadSource()
	{
		selected.clear();
		profileMarkers = null;
		Object source = sourceCombo.getSelectedItem();
		if (source instanceof ProfileGroundMarkers.Source)
		{
			ProfileGroundMarkers.Source profile = (ProfileGroundMarkers.Source) source;
			try
			{
				profileMarkers = ProfileGroundMarkers.readMarkers(profile.getFile(), gson, ArenaMapLayout.REGION_IDS);
			}
			catch (IOException ex)
			{
				log.warn("Unable to read profile {}", profile.getName(), ex);
				profileMarkers = Map.of();
				JOptionPane.showMessageDialog(this, "Unable to read the ground markers of profile \"" + profile.getName() + "\".",
						"Import BA ground markers", JOptionPane.WARNING_MESSAGE);
			}
		}
		refresh();
	}

	/**
	 * @return the selected source's ground markers in the region
	 */
	private List<GroundMarkerPoint> groundMarkers(int regionId)
	{
		return profileMarkers == null ? store.getGroundMarkers(regionId) : profileMarkers.getOrDefault(regionId, List.of());
	}

	private static List<GroundMarkerPoint> unconverted(List<GroundMarkerPoint> markers, List<GroundMarkerPoint> tiles)
	{
		return markers.stream().filter(m -> !GroundMarkerImport.isConverted(m, tiles)).collect(Collectors.toList());
	}

	private List<ArenaMapPanel.MapMarker> mapMarkers()
	{
		ArenaMapLayout layout = (ArenaMapLayout) layoutCombo.getSelectedItem();
		List<ArenaMapPanel.MapMarker> mapMarkers = new ArrayList<>();
		for (GroundMarkerPoint marker : markers)
		{
			if (layout.contains(marker))
			{
				boolean converted = GroundMarkerImport.isConverted(marker, tiles);
				mapMarkers.add(new ArenaMapPanel.MapMarker(marker, converted, selected.contains(marker),
						GroundMarkerImport.overlapsOtherTile(marker, tiles)));
			}
		}
		return mapMarkers;
	}

	private void refresh()
	{
		ArenaMapLayout layout = (ArenaMapLayout) layoutCombo.getSelectedItem();
		markers = new ArrayList<>(groundMarkers(layout.getRegionId()));
		tiles = new ArrayList<>(store.getPoints(layout.getRegionId()));
		List<GroundMarkerPoint> unconverted = unconverted(markers, tiles);
		// forget selections that were converted meanwhile (e.g. from the other button)
		selected.retainAll(unconverted);

		StringBuilder text = new StringBuilder("<html>");
		int allNew = 0;
		for (ArenaMapLayout l : ArenaMapLayout.values())
		{
			List<GroundMarkerPoint> m = l == layout ? markers : groundMarkers(l.getRegionId());
			List<GroundMarkerPoint> t = l == layout ? tiles : store.getPoints(l.getRegionId());
			List<GroundMarkerPoint> freshMarkers = unconverted(m, t);
			int fresh = freshMarkers.size();
			long overlapping = freshMarkers.stream().filter(g -> GroundMarkerImport.overlapsOtherTile(g, t)).count();
			allNew += fresh;
			if (text.length() > "<html>".length())
			{
				text.append(" &nbsp;&middot;&nbsp; ");
			}
			text.append("<b>").append(l.getDisplayName()).append(":</b> ").append(m.size()).append(" ground marker")
					.append(m.size() == 1 ? "" : "s").append(", ").append(fresh).append(" not yet BA Tiles");
			if (overlapping > 0)
			{
				text.append(" (<font color='").append(FLAG_HEX).append("'>").append(overlapping)
						.append(" on a tile that already has a BA Tile</font>)");
			}
		}
		summary.setText(text.append("</html>").toString());

		selectAllButton.setEnabled(!unconverted.isEmpty());
		clearButton.setEnabled(!selected.isEmpty());
		convertSelectedButton.setText("Convert selected (" + selected.size() + ")");
		convertSelectedButton.setEnabled(!selected.isEmpty());
		convertAllButton.setText("Convert all new on both maps (" + allNew + ")");
		convertAllButton.setEnabled(allNew > 0);
		mapChanged();
	}

	private void mapChanged()
	{
		mapPanel.revalidate();
		mapPanel.repaint();
	}

	private void onTileClicked(int mapX, int mapY, MouseEvent event)
	{
		ArenaMapLayout layout = (ArenaMapLayout) layoutCombo.getSelectedItem();
		List<GroundMarkerPoint> atTile = unconverted(markers, tiles).stream()
				.filter(m -> layout.isAt(m, mapX, mapY))
				.collect(Collectors.toList());
		if (atTile.isEmpty())
		{
			return;
		}

		if (selected.containsAll(atTile))
		{
			atTile.forEach(selected::remove);
		}
		else
		{
			selected.addAll(atTile);
		}
		refresh();
	}

	private void convertAll()
	{
		List<GroundMarkerPoint> all = new ArrayList<>();
		for (ArenaMapLayout layout : ArenaMapLayout.values())
		{
			all.addAll(unconverted(groundMarkers(layout.getRegionId()), store.getPoints(layout.getRegionId())));
		}
		confirmAndConvert(all);
	}

	/**
	 * Asks for confirmation, offering to skip markers on tiles that already have a different BA Tile, then converts.
	 */
	private void confirmAndConvert(List<GroundMarkerPoint> toConvert)
	{
		List<GroundMarkerPoint> clean = new ArrayList<>();
		for (GroundMarkerPoint marker : toConvert)
		{
			if (!GroundMarkerImport.overlapsOtherTile(marker, store.getPoints(marker.getRegionId())))
			{
				clean.add(marker);
			}
		}
		int overlapping = toConvert.size() - clean.size();

		Object source = sourceCombo.getSelectedItem();
		String from = source instanceof ProfileGroundMarkers.Source
				? " from profile \"" + ((ProfileGroundMarkers.Source) source).getName() + "\"" : "";
		String what = "Convert " + plural(toConvert.size(), "ground marker") + from
				+ " into BA Tiles on this profile, shown on all waves for all roles?";
		if (overlapping == 0)
		{
			int result = JOptionPane.showConfirmDialog(this, what, "Import BA ground markers", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION)
			{
				convert(toConvert);
			}
			return;
		}

		String message = "<html><div style='width:300px'>" + what + "<br><br><font color='" + FLAG_HEX + "'>"
				+ plural(overlapping, "of them is", "of them are") + " on a tile that already has a different BA Tile.</font>"
				+ " Converting " + (overlapping == 1 ? "it" : "them") + " stacks a second marker on that tile.</div></html>";
		List<String> options = new ArrayList<>();
		String convertEverything = "Convert all " + toConvert.size();
		String skipOverlapping = "Skip overlapping, convert " + clean.size();
		options.add(convertEverything);
		if (!clean.isEmpty())
		{
			options.add(skipOverlapping);
		}
		options.add("Cancel");

		int choice = JOptionPane.showOptionDialog(this, message, "Import BA ground markers", JOptionPane.DEFAULT_OPTION,
				JOptionPane.WARNING_MESSAGE, null, options.toArray(), clean.isEmpty() ? convertEverything : skipOverlapping);
		if (choice < 0)
		{
			return;
		}
		String picked = options.get(choice);
		if (picked.equals(convertEverything))
		{
			convert(toConvert);
		}
		else if (picked.equals(skipOverlapping))
		{
			convert(clean);
		}
	}

	private static String plural(int count, String noun)
	{
		return count + " " + noun + (count == 1 ? "" : "s");
	}

	private static String plural(int count, String singular, String pluralForm)
	{
		return count + " " + (count == 1 ? singular : pluralForm);
	}

	private void convert(List<GroundMarkerPoint> toConvert)
	{
		int added = store.convertGroundMarkers(toConvert);
		selected.clear();
		refresh();
		showResult(added);
	}

	private void showResult(int added)
	{
		JOptionPane.showMessageDialog(this, "<html><div style='width:260px'>Converted " + added + " ground marker"
						+ (added == 1 ? "" : "s") + " into BA Tiles, shown on all waves for all roles.<br><br>Your ground"
						+ " markers were left as they are; you may want to remove them (or turn off Ground Markers) so they"
						+ " are not drawn twice.</div></html>",
				"Import BA ground markers", JOptionPane.INFORMATION_MESSAGE);
	}
}
