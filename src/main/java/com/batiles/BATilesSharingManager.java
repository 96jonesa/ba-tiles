package com.batiles;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.Runnables;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;

import javax.inject.Inject;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
class BATilesSharingManager
{
    private static final WidgetMenuOption EXPORT_MARKERS_OPTION = new WidgetMenuOption("Export", "BA Tiles", ComponentID.MINIMAP_WORLDMAP_OPTIONS);
    private static final WidgetMenuOption IMPORT_MARKERS_OPTION = new WidgetMenuOption("Import", "BA Tiles", ComponentID.MINIMAP_WORLDMAP_OPTIONS);
    private static final WidgetMenuOption CLEAR_MARKERS_OPTION = new WidgetMenuOption("Clear", "BA Tiles", ComponentID.MINIMAP_WORLDMAP_OPTIONS);

    private final Client client;
    private final MenuManager menuManager;
    private final ChatMessageManager chatMessageManager;
    private final ChatboxPanelManager chatboxPanelManager;
    private final Gson gson;
    private final BATilesStore store;
    private final ClientThread clientThread;

    @Inject
    private BATilesSharingManager(Client client, MenuManager menuManager,
                                  ChatMessageManager chatMessageManager, ChatboxPanelManager chatboxPanelManager, Gson gson,
                                  BATilesStore store, ClientThread clientThread)
    {
        this.store = store;
        this.clientThread = clientThread;
        this.client = client;
        this.menuManager = menuManager;
        this.chatMessageManager = chatMessageManager;
        this.chatboxPanelManager = chatboxPanelManager;
        this.gson = gson;
    }

    void addImportExportMenuOptions()
    {
        menuManager.addManagedCustomMenu(EXPORT_MARKERS_OPTION, this::exportGroundMarkers);
        menuManager.addManagedCustomMenu(IMPORT_MARKERS_OPTION, this::promptForImport);
    }

    void addClearMenuOption()
    {
        menuManager.addManagedCustomMenu(CLEAR_MARKERS_OPTION, this::promptForClear);
    }

    void removeMenuOptions()
    {
        menuManager.removeManagedCustomMenu(EXPORT_MARKERS_OPTION);
        menuManager.removeManagedCustomMenu(IMPORT_MARKERS_OPTION);
        menuManager.removeManagedCustomMenu(CLEAR_MARKERS_OPTION);
    }

    private void exportGroundMarkers(MenuEntry menuEntry)
    {
        int[] regions = client.getMapRegions();
        if (regions == null)
        {
            return;
        }

        List<GroundMarkerPoint> activePoints = Arrays.stream(regions)
                .mapToObj(regionId -> store.getPoints(regionId).stream())
                .flatMap(Function.identity())
                .collect(Collectors.toList());

        if (activePoints.isEmpty())
        {
            sendChatMessage("You have no BA Tiles to export.");
            return;
        }

        final String exportDump = TileExport.of(activePoints, store.getPresets()).toJson(gson);

        log.debug("Exported BA Tiles: {}", exportDump);

        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(exportDump), null);
        sendChatMessage(activePoints.size() + " BA Tiles were copied to your clipboard.");
    }

    /**
     * Copies a single strategy preset and its tiles to the clipboard.
     */
    void exportPreset(StrategyPreset preset)
    {
        List<GroundMarkerPoint> presetPoints = Arrays.stream(ArenaMapLayout.REGION_IDS)
                .mapToObj(regionId -> store.getPoints(regionId).stream())
                .flatMap(Function.identity())
                .filter(p -> preset.getId().equals(p.getPresetId()))
                .collect(Collectors.toList());

        final String exportDump = TileExport.of(presetPoints, List.of(preset)).toJson(gson);
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(exportDump), null);
        sendChatMessage("Strategy preset \"" + preset.getName() + "\" (" + presetPoints.size()
                + " BA Tiles) was copied to your clipboard.");
    }

    private void promptForImport(MenuEntry menuEntry)
    {
        promptForImport();
    }

    void promptForImport()
    {
        final String clipboardText;
        try
        {
            clipboardText = Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor)
                    .toString();
        }
        catch (IOException | UnsupportedFlavorException ex)
        {
            sendChatMessage("Unable to read system clipboard.");
            log.warn("error reading clipboard", ex);
            return;
        }

        log.debug("Clipboard contents: {}", clipboardText);
        if (Strings.isNullOrEmpty(clipboardText))
        {
            sendChatMessage("You do not have any BA Tiles copied in your clipboard.");
            return;
        }

        TileExport imported;
        try
        {
            imported = TileExport.fromJson(gson, clipboardText);
        }
        catch (JsonParseException | IllegalStateException e)
        {
            log.debug("Malformed JSON for clipboard import", e);
            sendChatMessage("You do not have any BA Tiles copied in your clipboard.");
            return;
        }

        if (imported.getTiles().isEmpty() && imported.getPresets().isEmpty())
        {
            sendChatMessage("You do not have any BA Tiles copied in your clipboard.");
            return;
        }

        String question = "Are you sure you want to import " + imported.getTiles().size() + " BA Tiles"
                + (imported.getPresets().isEmpty() ? "?" : "<br>and " + imported.getPresets().size() + " strategy preset"
                + (imported.getPresets().size() == 1 ? "?" : "s?"));
        clientThread.invokeLater(() -> chatboxPanelManager.openTextMenuInput(question)
                .option("Yes", () -> importGroundMarkers(imported))
                .option("No", Runnables.doNothing())
                .build());
    }

    private void importGroundMarkers(TileExport imported)
    {
        int addedPresets = store.addPresetsIfAbsent(imported.getPresets());
        List<GroundMarkerPoint> importPoints = imported.getTiles();

        // regions being imported may not be loaded on client,
        // so need to import each bunch directly into the config
        // first, collate the list of unique region ids in the import
        Map<Integer, List<GroundMarkerPoint>> regionGroupedPoints = importPoints.stream()
                .collect(Collectors.groupingBy(GroundMarkerPoint::getRegionId));

        // now import each region into the config
        regionGroupedPoints.forEach((regionId, groupedPoints) ->
        {
            // combine imported points with existing region points
            log.debug("Importing {} points to region {}", groupedPoints.size(), regionId);
            Collection<GroundMarkerPoint> regionPoints = store.getPoints(regionId);

            List<GroundMarkerPoint> mergedList = new ArrayList<>(regionPoints.size() + groupedPoints.size());
            // add existing points
            mergedList.addAll(regionPoints);

            // add new points
            for (GroundMarkerPoint point : groupedPoints)
            {
                // filter out duplicates
                if (!mergedList.contains(point))
                {
                    mergedList.add(point);
                }
            }

            store.savePoints(regionId, mergedList);
        });

        sendChatMessage(importPoints.size() + " BA Tiles"
                + (addedPresets == 0 ? "" : " and " + addedPresets + " strategy preset" + (addedPresets == 1 ? "" : "s"))
                + " were imported from the clipboard.");
    }

    private void promptForClear(MenuEntry entry)
    {
        int[] regions = client.getMapRegions();
        if (regions == null)
        {
            return;
        }

        long numActivePoints = Arrays.stream(regions)
                .mapToLong(regionId -> store.getPoints(regionId).size())
                .sum();

        if (numActivePoints == 0)
        {
            sendChatMessage("You have no BA Tiles to clear.");
            return;
        }

        chatboxPanelManager.openTextMenuInput("Are you sure you want to clear the<br>" + numActivePoints + " currently loaded BA Tiles?")
                .option("Yes", () ->
                {
                    for (int regionId : regions)
                    {
                        store.savePoints(regionId, null);
                    }

                    sendChatMessage(numActivePoints + " BA Tile"
                            + (numActivePoints == 1 ? " was cleared." : "s were cleared."));

                })
                .option("No", Runnables.doNothing())
                .build();
    }

    private void sendChatMessage(final String message)
    {
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(message)
                .build());
    }
}
