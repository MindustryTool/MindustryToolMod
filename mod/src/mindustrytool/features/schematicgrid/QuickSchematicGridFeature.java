package mindustrytool.features.schematicgrid;

import arc.Core;
import arc.func.Prov;
import arc.scene.Element;
import arc.struct.Seq;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.quickaccess.QuickAccessFeature;
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
    public static final int MAX_COLS = 10;
    public static final float MIN_GAP = 0f;
    public static final float MAX_GAP = 16f;

    public final ConfigGroup config;
    public final ConfigValue<String> displayModeConfig;
    public final ConfigValue<Integer> colsConfig;
    public final ConfigValue<Float> buttonSizeConfig;
    public final ConfigValue<Float> buttonGapConfig;
    public final ConfigValue<Boolean> hideDragHandleConfig;
    public final ConfigValue<String> entriesJsonConfig;

    public final ConfigGroup positionGroup;
    public final ContextualConfigValue<Float, Boolean> xConfig;
    public final ContextualConfigValue<Float, Boolean> yConfig;

    public final Signal<Float> xSignal;
    public final Signal<Float> ySignal;

    private final Signal<List<QuickSchematicEntry>> entries = Signal.of(new ArrayList<QuickSchematicEntry>());

    private @Nullable QuickSchematicGridHudView hudView;
    private @Nullable QuickSchematicGridSettingsDialog settingsDialog;
    private boolean quickAccessHooked = false;
    private boolean syncingEntries = false;

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
        colsConfig = config.intValue("cols", 5);
        buttonSizeConfig = config.floatValue("buttonSize", 48f);
        buttonGapConfig = config.floatValue("buttonGap", 4f);
        hideDragHandleConfig = config.boolValue("hideDragHandle", false);
        entriesJsonConfig = config.stringValue("entries", "[]");

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

    public void addEntry(QuickSchematicEntry entry) {
        if (entry == null) {
            return;
        }
        List<QuickSchematicEntry> next = getEntries();
        next.add(entry);
        persist(next);
    }

    public void addSchematic(String schematicName, @Nullable String schematicFile) {
        addEntry(QuickSchematicEntry.of(schematicName, schematicFile));
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
