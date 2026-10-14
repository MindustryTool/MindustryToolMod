package mindustrytool.features.modmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputEvent.InputEventType;
import arc.scene.event.InputListener;
import arc.scene.ui.Dialog;
import arc.scene.ui.Dialog.DialogStyle;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.mod.Mods.LoadedMod;
import mindustry.mod.Mods.ModMeta;
import mindustrytool.features.modmanager.AttachableDialogBridge.AttachedEntry;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solim.UI;
import solim.layout.Column;
import solim.layout.ReactiveGrid;
import solim.reactive.Signal;

class ModManagerSolimReactivityTest extends MindustryTestEnv {

    @Test
    @SuppressWarnings("unchecked")
    void modCardReactsToAvailableUpdatesSignalAndStopsAfterDisposal() {
        ModService service = ModService.getInstance();
        Signal<Map<String, String>> updatesSignal = (Signal<Map<String, String>>) service.availableUpdates();
        updatesSignal.set(Collections.emptyMap());

        ModMeta meta = new ModMeta();
        meta.name = "sample-mod";
        meta.displayName = "Sample Mod";
        meta.version = "1.0.0";
        meta.author = "Tester";
        meta.description = "Sample description";
        meta.repo = "tester/sample-mod";

        LoadedMod mod = new LoadedMod(null, null, null, null, meta);
        AtomicInteger stateChanges = new AtomicInteger();

        ModCard card = new ModCard(mod, stateChanges::incrementAndGet);
        Element root = card.element();
        assertNotNull(root);
        assertSame(root, card.element());

        List<String> textsBefore = collectLabelTexts(root);
        assertTrue(textsBefore.stream().anyMatch(t -> t.contains("Sample Mod")));
        assertFalse(hasImageWithColor(root, ModStatusBadges.LIGHT_BLUE));

        Map<String, String> updates = new HashMap<>();
        updates.put("tester/sample-mod", "2.0.0");
        updatesSignal.set(updates);
        flushEffects();

        assertTrue(hasImageWithColor(root, ModStatusBadges.LIGHT_BLUE));

        updatesSignal.set(Collections.emptyMap());
        flushEffects();
        assertFalse(hasImageWithColor(root, ModStatusBadges.LIGHT_BLUE));

        card.dispose();
        assertTrue(card.isDisposed());

        Map<String, String> postDisposeUpdates = new HashMap<>();
        postDisposeUpdates.put("tester/sample-mod", "3.0.0");
        updatesSignal.set(postDisposeUpdates);
        flushEffects();

        assertFalse(hasImageWithColor(root, ModStatusBadges.LIGHT_BLUE));
    }

    @Test
    @SuppressWarnings("unchecked")
    void remoteModCardUpdatesStarColorAndInstallStateReactively(@TempDir Path tempDir) {
        Vars.dataDirectory = new Fi(new File(tempDir.toFile(), "data"));
        Vars.dataDirectory.mkdirs();

        LocalFavoritesStore favStore = LocalFavoritesStore.getInstance();
        favStore.ensureLoaded();
        for (LocalFavoritesStore.FavoriteFolder folder : new ArrayList<>(favStore.folders().peek())) {
            favStore.deleteFolder(folder.getName());
        }
        favStore.createFolder("Default");

        ModService service = ModService.getInstance();
        Signal<Set<String>> installingSignal = (Signal<Set<String>>) service.installingRepos();
        installingSignal.set(Collections.emptySet());

        ModListing listing = new ModListing(
                "author/cool-mod",
                "Cool Mod",
                "cool-mod",
                "Subtitle",
                "Author",
                "2026-01-01",
                42,
                "146",
                false,
                true,
                "Cool remote mod");

        RemoteModCard card = new RemoteModCard(
                listing,
                () -> favStore.toggleModInFolder("Default", ModName.fromListing(listing)),
                () -> {});
        Element root = card.element();
        assertNotNull(root);

        favStore.toggleModInFolder("Default", ModName.fromListing(listing));
        flushEffects();
        assertTrue(favStore.isFavorited(ModName.fromListing(listing)));
        assertTrue(hasImageWithColor(root, Pal.accent));

        Set<String> installing = new HashSet<>();
        installing.add("author/cool-mod");
        installingSignal.set(installing);
        flushEffects();

        card.dispose();
        assertTrue(card.isDisposed());

        favStore.toggleModInFolder("Default", ModName.fromListing(listing));
        flushEffects();
        assertTrue(hasImageWithColor(root, Pal.accent));
    }

    @Test
    void reactiveGridPreservesModCardInstancesAcrossListUpdates() {
        ModMeta metaA = new ModMeta();
        metaA.name = "mod-a";
        metaA.displayName = "Mod A";
        LoadedMod modA = new LoadedMod(null, null, null, null, metaA);

        ModMeta metaB = new ModMeta();
        metaB.name = "mod-b";
        metaB.displayName = "Mod B";
        LoadedMod modB = new LoadedMod(null, null, null, null, metaB);

        List<LoadedMod> initial = new ArrayList<>();
        initial.add(modA);
        Signal<List<LoadedMod>> modsSignal = Signal.of(initial);
        Map<String, ModCard> createdCards = new HashMap<>();

        ReactiveGrid<LoadedMod> grid = ReactiveGrid.of(modsSignal)
                .columns(1)
                .key(m -> m.name);
        grid.children(mod -> {
            ModCard c = new ModCard(mod, () -> {});
            createdCards.put(mod.name, c);
            return c;
        });

        Element table = grid.element();
        assertNotNull(table);
        ModCard firstCardA = createdCards.get("mod-a");
        assertNotNull(firstCardA);

        List<LoadedMod> updated = new ArrayList<>();
        updated.add(modA);
        updated.add(modB);
        modsSignal.set(updated);
        flushEffects();

        assertSame(firstCardA, createdCards.get("mod-a"));
        assertNotNull(createdCards.get("mod-b"));
        assertFalse(firstCardA.isDisposed());

        grid.dispose();
        assertTrue(firstCardA.isDisposed());
        assertTrue(createdCards.get("mod-b").isDisposed());
    }

    @Test
    void attachableDialogBridgeRegistersSharedEntriesAndExecutesFallback() {
        FontData fontData = new FontData() {
            @Override
            public boolean hasGlyph(char ch) {
                return true;
            }
        };
        DialogStyle dialogStyle = new DialogStyle();
        dialogStyle.titleFont = new Font(fontData, new TextureRegion(), false);

        AtomicBoolean fallbackSeenOnShow = new AtomicBoolean(false);
        Dialog vanillaMods = new Dialog("Mods", dialogStyle) {
            @Override
            public Dialog show() {
                fallbackSeenOnShow.set(AttachableDialogBridge.isFallbackActive());
                return this;
            }
        };
        Dialog heliumDialog = new Dialog("Helium", dialogStyle) {
            @Override
            public Dialog show() {
                return this;
            }
        };
        Dialog mindustryToolDialog = new Dialog("Mindustry Tool", dialogStyle) {
            @Override
            public Dialog show() {
                return this;
            }
        };

        AttachableDialogBridge.attach(vanillaMods, heliumDialog, Icon.wrench, "Helium", () -> true);
        AttachableDialogBridge.attach(vanillaMods, mindustryToolDialog, Icon.book, "Mindustry Tool", () -> true);

        List<AttachedEntry> entries = AttachableDialogBridge.getEntries(vanillaMods);
        assertEquals(2, entries.size());
        assertSame(mindustryToolDialog, entries.get(0).dialog);
        assertEquals("Mindustry Tool", entries.get(0).description);
        assertSame(heliumDialog, entries.get(1).dialog);
        assertEquals("Helium", entries.get(1).description);

        assertSame(mindustryToolDialog, AttachableDialogBridge.showFirstAttached(vanillaMods));

        AttachableDialogBridge.openFallback(vanillaMods);
        assertTrue(fallbackSeenOnShow.get());
        assertFalse(AttachableDialogBridge.isFallbackActive());
    }

    @Test
    void modManagerDialogSwitcherRemainsCompactAndTogglesLabelsOnHover() {
        ModManagerDialog.ModManagerView view = new ModManagerDialog.ModManagerView(
                null,
                Signal.of(0),
                () -> {},
                r -> {
                    if (r != null) {
                        r.run();
                    }
                },
                () -> {},
                () -> {},
                () -> {});
        Column overlayLayer = UI.column()
                .grow()
                .top()
                .left()
                .children(view::landscapeSwitcher);
        Table cont = overlayLayer.table();
        assertNotNull(cont);
        cont.setSize(1920f, 1080f);
        cont.validate();

        Table switcherTable = findForEachContainer(cont);
        assertNotNull(switcherTable);

        float collapsedWidth = switcherTable.getPrefWidth();
        assertTrue(collapsedWidth <= 60f, "Expected compact collapsed switcher width <= 60f but got " + collapsedWidth);
        assertTrue(switcherTable.getWidth() <= 60f, "Expected laid-out switcher width <= 60f in 1920px viewport but got " + switcherTable.getWidth());

        fireHoverEvent(switcherTable, true);
        flushEffects();
        cont.act(0.1f);
        cont.validate();
        float midExpandWidth = switcherTable.getPrefWidth();
        assertTrue(midExpandWidth > collapsedWidth);

        cont.act(0.3f);
        cont.validate();
        float expandedWidth = switcherTable.getPrefWidth();
        assertTrue(expandedWidth > midExpandWidth);
        assertTrue(switcherTable.getWidth() < 400f);
        assertTrue(collectLabelTexts(switcherTable).size() >= 2);

        fireHoverEvent(switcherTable, false);
        flushEffects();
        cont.act(0.1f);
        cont.validate();
        float midCollapseWidth = switcherTable.getPrefWidth();
        assertTrue(midCollapseWidth < expandedWidth && midCollapseWidth > collapsedWidth);

        cont.act(0.3f);
        cont.validate();
        assertEquals(collapsedWidth, switcherTable.getPrefWidth(), 0.5f);
        assertEquals(collapsedWidth, switcherTable.getWidth(), 0.5f);

        fireHoverEvent(switcherTable, true);
        flushEffects();
        cont.act(0.4f);
        cont.validate();
        assertEquals(expandedWidth, switcherTable.getPrefWidth(), 0.5f);

        overlayLayer.dispose();
        view.dispose();
    }

    private static Table findForEachContainer(Element root) {
        if (root instanceof Table
                && root.name != null
                && root.name.contains("foreach")) {
            return (Table) root;
        }
        if (root instanceof Group) {
            for (Element child : ((Group) root).getChildren()) {
                Table found = findForEachContainer(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void fireHoverEvent(Element target, boolean enter) {
        InputEvent event = new InputEvent();
        event.listenerActor = target;
        event.targetActor = target;
        event.pointer = -1;
        event.type = enter ? InputEventType.enter : InputEventType.exit;
        for (EventListener listener : target.getListeners()) {
            if (listener instanceof InputListener) {
                InputListener input = (InputListener) listener;
                if (enter) {
                    input.enter(event, 0f, 0f, -1, null);
                } else {
                    input.exit(event, 0f, 0f, -1, null);
                }
            }
        }
    }

    private static List<String> collectLabelTexts(Element element) {
        List<String> result = new ArrayList<>();
        if (element instanceof Label) {
            CharSequence text = ((Label) element).getText();
            if (text != null) {
                result.add(text.toString());
            }
        }
        if (element instanceof Group) {
            for (Element child : ((Group) element).getChildren()) {
                result.addAll(collectLabelTexts(child));
            }
        }
        return result;
    }

    private static boolean hasImageWithColor(Element element, Color target) {
        if (element instanceof Image) {
            Color c = element.color;
            if (c != null
                    && Math.abs(c.r - target.r) < 0.01f
                    && Math.abs(c.g - target.g) < 0.01f
                    && Math.abs(c.b - target.b) < 0.01f) {
                return true;
            }
        }
        if (element instanceof Group) {
            for (Element child : ((Group) element).getChildren()) {
                if (hasImageWithColor(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }
}
