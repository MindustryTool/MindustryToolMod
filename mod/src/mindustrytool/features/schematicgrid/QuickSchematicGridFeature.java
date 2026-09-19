package mindustrytool.features.schematicgrid;

import arc.Core;
import arc.func.Prov;
import arc.scene.Element;
import arc.struct.Seq;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.quickaccess.QuickAccessFeature;
import mindustrytool.utils.JsonUtils;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.ContextualConfigValue;
import solim.core.Units;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.reactive.Signals;

/**
 * Quick access grid of player-configured schematics with HUD and Popup variants.
 */
public class QuickSchematicGridFeature extends Feature {

    public static final String DISPLAY_HUD = "hud";
    public static final String DISPLAY_POPUP = "popup";

    public static final int MIN_COLS = 1;
    public static final int MAX_COLS = 14;
    public static final int MIN_ROWS = 1;
    public static final int MAX_ROWS = 14;
    public static final int MIN_PAGES = 1;
    public static final int MAX_PAGES = 8;
    public static final float MIN_GAP = 0f;
    public static final float MAX_GAP = 16f;

    public final ConfigGroup config;
    public final ConfigValue<String> displayModeConfig;
    public final ConfigValue<Integer> rowsConfig;
    public final ConfigValue<Integer> colsConfig;
    public final ConfigValue<Integer> pageCountConfig;
    public final ConfigValue<Float> buttonSizeConfig;
    public final ConfigValue<Float> buttonGapConfig;
    public final ConfigValue<Boolean> hideDragHandleConfig;
    public final ConfigValue<String> entriesJsonConfig;
    public final ConfigValue<String> pageIconsJsonConfig;

    public final Signal<Integer> activePage = Signal.of(0);

    public final ConfigGroup positionGroup;
    public final ContextualConfigValue<Float, Boolean> xConfig;
    public final ContextualConfigValue<Float, Boolean> yConfig;

    public final Signal<Float> xSignal;
    public final Signal<Float> ySignal;

    private final Signal<List<QuickSchematicEntry>> entries = Signal.of(new ArrayList<QuickSchematicEntry>());
    private final Signal<List<String>> pageIcons = Signal.of(new ArrayList<String>());

    private @Nullable QuickSchematicGridHudView hudView;
    private @Nullable QuickSchematicGridSettingsDialog settingsDialog;
    private boolean quickAccessHooked = false;
    private boolean syncingEntries = false;
    private boolean syncingPageIcons = false;

    public QuickSchematicGridFeature() {
        super(FeatureMetadata.builder()
                .id("quick-schematic-grid")
                .icon(FileIcon.of("grid-2x2.png"))
                .order(23)
                .enabledByDefault(false)
                .quickAccess(true)
                .build());

        config = configGroup();

        displayModeConfig = config.stringValue("displayMode", DISPLAY_POPUP);
        rowsConfig = config.intValue("rows", 3);
        colsConfig = config.intValue("cols", 4);
        pageCountConfig = config.intValue("pageCount", 1);
        buttonSizeConfig = config.floatValue("buttonSize", 48f);
        buttonGapConfig = config.floatValue("buttonGap", 4f);
        hideDragHandleConfig = config.boolValue("hideDragHandle", false);
        entriesJsonConfig = config.stringValue("entries", "[]");
        pageIconsJsonConfig = config.stringValue("pageIcons", "[]");

        pageCountConfig.signal().subscribe(count -> {
            int max = Math.max(0, (count != null ? count : 1) - 1);
            if (activePage.get() > max) {
                activePage.set(max);
            }
        });

        positionGroup = config.group("position");

        xConfig = positionGroup.floatValueKeyed(
                "x",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sw = Units.screenWidth();
                    return sw > 0 ? sw / 2f : 400f;
                });
        yConfig = positionGroup.floatValueKeyed(
                "y",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sh = Units.screenHeight();
                    return sh > 0 ? sh / 2f : 250f;
                });

        xSignal = xConfig.signal();
        ySignal = yConfig.signal();

        entries.set(QuickSchematicEntry.fromJson(entriesJsonConfig.get()));

        entriesJsonConfig.signal().subscribe(json -> {
            if (syncingEntries) {
                return;
            }
            syncingEntries = true;
            try {
                entries.set(QuickSchematicEntry.fromJson(json));
            } finally {
                syncingEntries = false;
            }
        });

        pageIcons.set(parsePageIcons(pageIconsJsonConfig.get()));

        pageIconsJsonConfig.signal().subscribe(json -> {
            if (syncingPageIcons) {
                return;
            }
            syncingPageIcons = true;
            try {
                pageIcons.set(parsePageIcons(json));
            } finally {
                syncingPageIcons = false;
            }
        });

        displayModeConfig.signal().subscribe(mode -> updateHud());
    }

    public Readable<List<QuickSchematicEntry>> entries() {
        return entries;
    }

    public List<QuickSchematicEntry> getEntries() {
        List<QuickSchematicEntry> current = entries.peek();
        return current != null ? new ArrayList<>(current) : new ArrayList<QuickSchematicEntry>();
    }

    private void persist(List<QuickSchematicEntry> next) {
        List<QuickSchematicEntry> safe = next != null ? next : new ArrayList<QuickSchematicEntry>();
        syncingEntries = true;
        try {
            entries.set(new ArrayList<>(safe));
            entriesJsonConfig.set(QuickSchematicEntry.toJson(safe));
        } finally {
            syncingEntries = false;
        }
    }

    private static List<String> parsePageIcons(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<String>();
        }
        try {
            List<String> list = JsonUtils.fromJsonArray(String.class, json);
            return list != null ? list : new ArrayList<String>();
        } catch (Exception ignored) {
            return new ArrayList<String>();
        }
    }

    private void persistPageIcons(List<String> next) {
        List<String> safe = next != null ? next : new ArrayList<String>();
        syncingPageIcons = true;
        try {
            pageIcons.set(new ArrayList<>(safe));
            pageIconsJsonConfig.set(JsonUtils.toJson(safe));
        } finally {
            syncingPageIcons = false;
        }
    }

    public Readable<List<String>> pageIcons() {
        return pageIcons;
    }

    public List<String> getPageIcons() {
        List<String> current = pageIcons.peek();
        return current != null ? new ArrayList<>(current) : new ArrayList<String>();
    }

    public @Nullable String getPageIcon(int pageIndex) {
        if (pageIndex < 0) {
            return null;
        }
        List<String> list = pageIcons.peek();
        if (list != null && pageIndex < list.size()) {
            String icon = list.get(pageIndex);
            return icon != null && !icon.trim().isEmpty() ? icon : null;
        }
        return null;
    }

    public void setPageIcon(int pageIndex, @Nullable String icon) {
        if (pageIndex < 0) {
            return;
        }
        List<String> next = getPageIcons();
        while (next.size() <= pageIndex) {
            next.add(null);
        }
        String clean = icon != null && !icon.trim().isEmpty() ? icon : null;
        next.set(pageIndex, clean);
        persistPageIcons(next);
    }

    public void clearPageIcon(int pageIndex) {
        setPageIcon(pageIndex, null);
    }

    public @Nullable QuickSchematicEntry getEntryAt(int page, int row, int col) {
        for (QuickSchematicEntry entry : getEntries()) {
            if (entry != null && entry.page == page && entry.row == row && entry.col == col) {
                return entry;
            }
        }
        return null;
    }

    public void setEntryAt(int page, int row, int col, @Nullable QuickSchematicEntry entry) {
        List<QuickSchematicEntry> next = getEntries();
        for (int i = next.size() - 1; i >= 0; i--) {
            QuickSchematicEntry existing = next.get(i);
            if (existing != null && existing.page == page && existing.row == row && existing.col == col) {
                next.remove(i);
            }
        }
        if (entry != null) {
            entry.page = page;
            entry.row = row;
            entry.col = col;
            next.add(entry);
        }
        persist(next);
    }

    public boolean clearSlotAt(int page, int row, int col) {
        List<QuickSchematicEntry> next = getEntries();
        boolean removed = false;
        for (int i = next.size() - 1; i >= 0; i--) {
            QuickSchematicEntry existing = next.get(i);
            if (existing != null && existing.page == page && existing.row == row && existing.col == col) {
                next.remove(i);
                removed = true;
            }
        }
        if (removed) {
            persist(next);
        }
        return removed;
    }

    public boolean addPage() {
        int current = pageCountConfig.get();
        if (current < MAX_PAGES) {
            pageCountConfig.set(current + 1);
            activePage.set(current);
            return true;
        }
        return false;
    }

    public boolean deletePage(int pageIndex) {
        int count = pageCountConfig.get();
        if (count <= MIN_PAGES || pageIndex < 0 || pageIndex >= count) {
            return false;
        }
        List<QuickSchematicEntry> next = getEntries();
        for (int i = next.size() - 1; i >= 0; i--) {
            QuickSchematicEntry entry = next.get(i);
            if (entry != null) {
                if (entry.page == pageIndex) {
                    next.remove(i);
                } else if (entry.page > pageIndex) {
                    entry.page -= 1;
                }
            }
        }
        List<String> icons = getPageIcons();
        if (pageIndex < icons.size()) {
            icons.remove(pageIndex);
            persistPageIcons(icons);
        }
        pageCountConfig.set(count - 1);
        if (activePage.get() >= count - 1) {
            activePage.set(Math.max(0, count - 2));
        }
        persist(next);
        return true;
    }

    public int getActivePage() {
        return activePage.get();
    }

    public void setActivePage(int page) {
        int max = Math.max(0, pageCountConfig.get() - 1);
        activePage.set(Math.max(0, Math.min(max, page)));
    }

    public List<QuickSchematicEntry> getEntriesForPage(int page) {
        List<QuickSchematicEntry> result = new ArrayList<>();
        for (QuickSchematicEntry entry : getEntries()) {
            if (entry != null && entry.page == page) {
                result.add(entry);
            }
        }
        return result;
    }

    public void addSchematic(int page, int row, int col, String schematicName, @Nullable String schematicFile) {
        setEntryAt(page, row, col, QuickSchematicEntry.of(page, row, col, schematicName, schematicFile));
    }

    public void addEntry(QuickSchematicEntry entry) {
        if (entry == null) {
            return;
        }
        setEntryAt(entry.page, entry.row, entry.col, entry);
    }

    public void addSchematic(String schematicName, @Nullable String schematicFile) {
        int p = activePage.get();
        int rows = Math.max(1, Math.min(MAX_ROWS, rowsConfig.get()));
        int cols = Math.max(1, Math.min(MAX_COLS, colsConfig.get()));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (getEntryAt(p, r, c) == null) {
                    addSchematic(p, r, c, schematicName, schematicFile);
                    return;
                }
            }
        }
        addSchematic(p, 0, 0, schematicName, schematicFile);
    }

    public boolean removeEntry(String id) {
        List<QuickSchematicEntry> next = getEntries();
        boolean removed = removeById(next, id);
        if (removed) {
            persist(next);
        }
        return removed;
    }

    public boolean moveEarlier(String id) {
        List<QuickSchematicEntry> next = getEntries();
        boolean moved = moveById(next, id, -1);
        if (moved) {
            persist(next);
        }
        return moved;
    }

    public boolean moveLater(String id) {
        List<QuickSchematicEntry> next = getEntries();
        boolean moved = moveById(next, id, 1);
        if (moved) {
            persist(next);
        }
        return moved;
    }

    public void replaceEntries(List<QuickSchematicEntry> entries) {
        persist(entries != null ? new ArrayList<>(entries) : new ArrayList<QuickSchematicEntry>());
    }

    public @Nullable QuickSchematicEntry getEntry(String id) {
        if (id == null) {
            return null;
        }
        for (QuickSchematicEntry entry : getEntries()) {
            if (entry != null && id.equals(entry.id)) {
                return entry;
            }
        }
        return null;
    }

    public boolean updateEntry(String id, Consumer<QuickSchematicEntry> mutator) {
        if (id == null || mutator == null) {
            return false;
        }
        List<QuickSchematicEntry> next = getEntries();
        for (QuickSchematicEntry entry : next) {
            if (entry != null && id.equals(entry.id)) {
                mutator.accept(entry);
                persist(next);
                return true;
            }
        }
        return false;
    }

    public static boolean removeById(List<QuickSchematicEntry> list, @Nullable String id) {
        if (list == null || id == null) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            QuickSchematicEntry entry = list.get(i);
            if (entry != null && id.equals(entry.id)) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    public static boolean moveById(List<QuickSchematicEntry> list, @Nullable String id, int delta) {
        if (list == null || id == null) {
            return false;
        }
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            QuickSchematicEntry entry = list.get(i);
            if (entry != null && id.equals(entry.id)) {
                index = i;
                break;
            }
        }
        int target = index + delta;
        if (index < 0 || target < 0 || target >= list.size()) {
            return false;
        }
        QuickSchematicEntry entry = list.remove(index);
        list.add(target, entry);
        return true;
    }

    public static boolean canMoveEarlier(List<QuickSchematicEntry> list, @Nullable String id) {
        if (list == null || id == null) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            QuickSchematicEntry entry = list.get(i);
            if (entry != null && id.equals(entry.id)) {
                return i > 0;
            }
        }
        return false;
    }

    public static boolean canMoveLater(List<QuickSchematicEntry> list, @Nullable String id) {
        if (list == null || id == null) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            QuickSchematicEntry entry = list.get(i);
            if (entry != null && id.equals(entry.id)) {
                return i < list.size() - 1;
            }
        }
        return false;
    }

    public @Nullable Schematic resolveSchematic(@Nullable QuickSchematicEntry entry) {
        if (entry == null) {
            return null;
        }
        return resolveSchematic(entry.schematicFile, entry.schematicName);
    }

    public @Nullable Schematic resolveSchematic(@Nullable String schematicFile, @Nullable String schematicName) {
        try {
            Seq<Schematic> all = Vars.schematics.all();
            if (all == null) {
                return null;
            }
            if (schematicFile != null && !schematicFile.trim().isEmpty()) {
                for (Schematic schematic : all) {
                    if (schematic != null && schematic.file != null
                            && schematicFile.equals(schematic.file.name())) {
                        return schematic;
                    }
                }
            }
            if (schematicName != null && !schematicName.trim().isEmpty()) {
                for (Schematic schematic : all) {
                    if (schematic != null && schematicName.equals(schematic.name())) {
                        return schematic;
                    }
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static @Nullable Schematic resolveIn(
            Seq<Schematic> all,
            @Nullable String schematicFile,
            @Nullable String schematicName) {
        if (all == null) {
            return null;
        }
        if (schematicFile != null && !schematicFile.trim().isEmpty()) {
            for (Schematic schematic : all) {
                if (schematic != null && schematic.file != null
                        && schematicFile.equals(schematic.file.name())) {
                    return schematic;
                }
            }
        }
        if (schematicName != null && !schematicName.trim().isEmpty()) {
            for (Schematic schematic : all) {
                if (schematic != null && schematicName.equals(schematic.name())) {
                    return schematic;
                }
            }
        }
        return null;
    }

    public void useEntry(@Nullable QuickSchematicEntry entry) {
        Schematic schematic = resolveSchematic(entry);
        if (schematic == null) {
            Vars.ui.showInfo(Core.bundle.get("feature.quick-schematic-grid.warning.missing"));
            return;
        }
        useSchematic(schematic);
    }

    public void useSchematic(@Nullable Schematic schematic) {
        if (schematic == null) {
            return;
        }
        if (Vars.state.isMenu()) {
            Vars.ui.schematics.showInfo(schematic);
            return;
        }
        if (!Vars.state.rules.schematicsAllowed) {
            Vars.ui.showInfo(Core.bundle.get("feature.quick-schematic-grid.warning.disabled"));
            return;
        }
        Vars.control.input.useSchematic(schematic);
    }

    public boolean isPopupMode() {
        return DISPLAY_POPUP.equals(displayModeConfig.get());
    }

    public boolean isPopupActive() {
        if (!isPopupMode()) {
            return false;
        }
        try {
            QuickAccessFeature quickAccess = FeatureManager.getFeature(QuickAccessFeature.class);
            return quickAccess != null && quickAccess.isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void onQuickAccessClick(@Nullable Element anchor) {
        if (isPopupMode()) {
            togglePopup(anchor);
            return;
        }
        super.onQuickAccessClick(anchor);
    }

    public void togglePopup(@Nullable Element quickAccessBar) {
        QuickSchematicGridPopup.toggle(this, quickAccessBar);
    }

    public void resetPosition() {
        float sw = Units.screenWidth();
        float sh = Units.screenHeight();
        float cx = sw > 0 ? sw / 2f : 400f;
        float cy = sh > 0 ? sh / 2f : 250f;

        Core.settings.put("mindustrytool.quick-schematic-grid.position.x.portrait", cx);
        Core.settings.put("mindustrytool.quick-schematic-grid.position.x.landscape", cx);
        Core.settings.put("mindustrytool.quick-schematic-grid.position.y.portrait", cy);
        Core.settings.put("mindustrytool.quick-schematic-grid.position.y.landscape", cy);

        xConfig.reset();
        yConfig.reset();

        if (hudView != null) {
            Core.app.post(hudView::keepInScreen);
        }
    }

    @Override
    public void onEnable() {
        updateHud();
    }

    @Override
    public void onDisable() {
        QuickSchematicGridPopup.hide();
        removeHud();
    }

    private void updateHud() {
        ensureQuickAccessHook();
        if (!isEnabled() || isPopupActive()) {
            removeHud();
            return;
        }
        ensureHud();
    }

    private void ensureQuickAccessHook() {
        if (quickAccessHooked) {
            return;
        }
        try {
            QuickAccessFeature quickAccess = FeatureManager.getFeature(QuickAccessFeature.class);
            if (quickAccess == null) {
                return;
            }
            quickAccessHooked = true;
            quickAccess.enabled().subscribe(value -> updateHud());
        } catch (Exception ignored) {
        }
    }

    private void ensureHud() {
        if (hudView != null) {
            return;
        }
        hudView = new QuickSchematicGridHudView(this);
        Element el = hudView.element();
        el.name = "quick-schematic-grid-hud";
        el.visible(() -> Vars.ui.hudfrag.shown && Vars.state.isGame());

        Core.app.post(() -> {
            if (hudView != null && !isPopupActive()) {
                Vars.ui.hudGroup.addChild(el);
            }
        });
    }

    private void removeHud() {
        if (hudView == null) {
            return;
        }
        QuickSchematicGridHudView view = hudView;
        hudView = null;
        Core.app.post(() -> {
            view.element().remove();
            view.dispose();
        });
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new QuickSchematicGridSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}
