package mindustrytool.features.armycontrol;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.math.geom.Rect;
import arc.scene.Element;
import arc.struct.IntSeq;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.ResetEvent;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
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
import solim.reactive.Signal;
import solim.reactive.Signals;

/**
 * Feature providing RTS army selection (map/screen), control groups, and unit type deselection.
 */
public class ArmyControlFeature extends Feature {

    public static final String DISPLAY_POPUP = "popup";
    public static final String DISPLAY_HUD = "hud";
    public static final int DEFAULT_MAX_GROUPS = 5;
    private static final int SELECTION_POLL_INTERVAL = 15;

    public final ConfigGroup config;
    public final ConfigValue<String> displayModeConfig;
    public final ConfigValue<Integer> maxGroupsConfig;
    public final ConfigValue<Float> scaleConfig;
    public final ConfigValue<Boolean> hideDragHandleConfig;

    public final ConfigGroup positionGroup;
    public final ContextualConfigValue<Float, Boolean> xConfig;
    public final ContextualConfigValue<Float, Boolean> yConfig;

    public final Signal<Float> xSignal;
    public final Signal<Float> ySignal;

    // Active selection state
    private final Signal<ObjectIntMap<UnitType>> activeSelectionSignal = Signal.of(new ObjectIntMap<>());
    private final Signal<Seq<ControlGroup>> controlGroupsSignal = Signal.of(new Seq<>());

    private @Nullable ArmyControlHudView hudView;
    private @Nullable ArmyControlSettingsDialog settingsDialog;
    private boolean quickAccessHooked = false;
    private int pollTicks = 0;

    public ArmyControlFeature() {
        super(FeatureMetadata.builder()
                .id("army-control")
                .icon(FileIcon.of("swords.png"))
                .order(6)
                .enabledByDefault(false)
                .quickAccess(true)
                .build());

        config = configGroup();
        displayModeConfig = config.stringValue("displayMode", DISPLAY_POPUP);
        maxGroupsConfig = config.intValue("maxGroups", DEFAULT_MAX_GROUPS);
        scaleConfig = config.floatValue("scale", 1f);
        hideDragHandleConfig = config.boolValue("hideDragHandle", false);

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
                    return sh > 0 ? sh / 2f : 200f;
                });

        xSignal = xConfig.signal();
        ySignal = yConfig.signal();

        Events.on(WorldLoadEvent.class, e -> onWorldReset());
        Events.on(ResetEvent.class, e -> onWorldReset());
        // The game loop mutates selectedUnits behind our back (per-frame commandable
        // validation, vanilla box-selects, unit deaths). Poll lightly so the chips
        // never show a stale selection.
        Events.run(Trigger.update, () -> {
            if (++pollTicks < SELECTION_POLL_INTERVAL) {
                return;
            }
            pollTicks = 0;
            if (isEnabled()) {
                refreshActiveSelection();
            }
        });
    }

    private void onWorldReset() {
        // Clear ephemeral control groups on new match or reset
        controlGroupsSignal.set(new Seq<>());
        activeSelectionSignal.set(new ObjectIntMap<>());
    }

    public Signal<ObjectIntMap<UnitType>> activeSelectionSignal() {
        return activeSelectionSignal;
    }

    public Signal<Seq<ControlGroup>> controlGroupsSignal() {
        return controlGroupsSignal;
    }

    public boolean isPopupMode() {
        return DISPLAY_POPUP.equals(displayModeConfig.get());
    }

    public boolean isPopupActive() {
        if (!isPopupMode()) {
            return false;
        }
        try {
            QuickAccessFeature qa = FeatureManager.getFeature(QuickAccessFeature.class);
            return qa != null && qa.isEnabled();
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
        ArmyControlPopup.toggle(this, quickAccessBar);
    }

    public void openPopup(@Nullable Element quickAccessBar) {
        ArmyControlPopup.show(this, quickAccessBar);
    }

    @Override
    public void onEnable() {
        updateHud();
    }

    @Override
    public void onDisable() {
        ArmyControlPopup.hide();
        removeHud();
    }

    public void updateHud() {
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
            QuickAccessFeature qa = FeatureManager.getFeature(QuickAccessFeature.class);
            if (qa == null) {
                return;
            }
            quickAccessHooked = true;
            qa.enabled().subscribe(v -> updateHud());
        } catch (Exception ignored) {
        }
    }

    private void ensureHud() {
        if (hudView != null) {
            return;
        }
        hudView = new ArmyControlHudView(this);
        Element el = hudView.element();
        el.name = "army-control-hud";
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
        ArmyControlHudView view = hudView;
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
                settingsDialog = new ArmyControlSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    // --- Core Army Selection Actions ---

    /**
     * Selects all living units on the player's team across the map, excluding the player's unit,
     * and activates RTS Command Mode without moving the camera.
     */
    public void selectAllMap() {
        if (Vars.control == null || Vars.control.input == null || Vars.player == null) {
            return;
        }
        Seq<Unit> selected = Vars.control.input.selectedUnits;
        selected.clear();
        if (Groups.unit != null) {
            for (Unit u : Groups.unit) {
                // Match vanilla select-all: only commandable units stick, the game loop
                // prunes the rest every frame and stale chips would confuse deselection.
                if (u.team == Vars.player.team() && u != Vars.player.unit() && u.isValid() && u.allowCommand()) {
                    selected.add(u);
                }
            }
        }
        Vars.control.input.commandMode = true;
        refreshActiveSelection();
    }

    /**
     * Selects all living units on the player's team within the camera viewport, excluding player's unit,
     * and activates RTS Command Mode without moving the camera.
     */
    public void selectScreen() {
        if (Vars.control == null || Vars.control.input == null || Vars.player == null || Core.camera == null) {
            return;
        }
        Seq<Unit> selected = Vars.control.input.selectedUnits;
        selected.clear();
        Rect bounds = new Rect();
        Core.camera.bounds(bounds);
        if (Groups.unit != null) {
            for (Unit u : Groups.unit) {
                if (u.team == Vars.player.team() && u != Vars.player.unit() && u.isValid() && u.allowCommand()
                        && bounds.contains(u.x, u.y)) {
                    selected.add(u);
                }
            }
        }
        Vars.control.input.commandMode = true;
        refreshActiveSelection();
    }

    /**
     * Deselects all currently selected units.
     */
    public void deselectAll() {
        if (Vars.control == null || Vars.control.input == null) {
            return;
        }
        Vars.control.input.selectedUnits.clear();
        refreshActiveSelection();
    }

    /**
     * Deselects / excludes all units of the given UnitType from the current selection.
     * Keeps RTS Command Mode active so the remaining selection stays commanded,
     * matching select/recall behavior.
     */
    public void deselectUnitType(UnitType type) {
        if (Vars.control == null || Vars.control.input == null || type == null) {
            return;
        }
        Seq<Unit> selected = Vars.control.input.selectedUnits;
        for (int i = selected.size - 1; i >= 0; i--) {
            Unit u = selected.get(i);
            if (u.type == type) {
                selected.remove(i);
            }
        }
        Vars.control.input.commandMode = true;
        refreshActiveSelection();
    }

    /**
     * Updates the reactive summary of currently selected units.
     * Skips the signal update when nothing changed so the periodic poll
     * never rebuilds the selection UI for an identical selection.
     */
    public void refreshActiveSelection() {
        ObjectIntMap<UnitType> map = new ObjectIntMap<>();
        if (Vars.control != null && Vars.control.input != null) {
            Seq<Unit> selected = Vars.control.input.selectedUnits;
            for (int i = 0; i < selected.size; i++) {
                Unit u = selected.get(i);
                if (u != null && u.isValid() && u.type != null) {
                    map.put(u.type, map.get(u.type, 0) + 1);
                }
            }
        }
        if (!sameSelection(activeSelectionSignal.peek(), map)) {
            activeSelectionSignal.set(map);
        }
    }

    private static boolean sameSelection(@Nullable ObjectIntMap<UnitType> a, @Nullable ObjectIntMap<UnitType> b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.size != b.size) {
            return false;
        }
        for (UnitType key : a.keys()) {
            if (a.get(key, 0) != b.get(key, 0)) {
                return false;
            }
        }
        return true;
    }

    // --- Control Group Management ---

    /**
     * Creates a new control group from the current selection or candidate IDs.
     */
    public @Nullable ControlGroup createGroup(String name, IntSeq unitIds) {
        Seq<ControlGroup> groups = controlGroupsSignal.peek();
        Seq<ControlGroup> next = groups != null ? new Seq<>(groups) : new Seq<>();
        int nextId = 1;
        for (ControlGroup cg : next) {
            if (cg.getId() >= nextId) {
                nextId = cg.getId() + 1;
            }
        }
        ControlGroup group = new ControlGroup(nextId, name);
        group.setUnits(unitIds);
        next.add(group);
        controlGroupsSignal.set(next);
        return group;
    }

    /**
     * Recalls a saved control group: selects all living members and turns on RTS command mode.
     */
    public void recallGroup(ControlGroup group) {
        if (Vars.control == null || Vars.control.input == null || group == null) {
            return;
        }
        group.pruneDeadUnits();
        Seq<Unit> selected = Vars.control.input.selectedUnits;
        selected.clear();
        IntSeq ids = group.getUnitIds();
        for (int i = 0; i < ids.size; i++) {
            Unit u = Groups.unit != null ? Groups.unit.getByID(ids.get(i)) : null;
            if (u != null && u.isValid() && u.allowCommand()) {
                selected.add(u);
            }
        }
        Vars.control.input.commandMode = true;
        refreshActiveSelection();
    }

    /**
     * Deletes a control group.
     */
    public void deleteGroup(ControlGroup group) {
        Seq<ControlGroup> groups = controlGroupsSignal.peek();
        if (groups == null || group == null) {
            return;
        }
        Seq<ControlGroup> next = new Seq<>(groups);
        next.remove(group);
        controlGroupsSignal.set(next);
    }

    /**
     * Returns an IntSeq of unit IDs currently selected in Mindustry.
     */
    public IntSeq getSelectedUnitIds() {
        IntSeq ids = new IntSeq();
        if (Vars.control != null && Vars.control.input != null) {
            for (Unit u : Vars.control.input.selectedUnits) {
                if (u != null && u.isValid()) {
                    ids.add(u.id);
                }
            }
        }
        return ids;
    }
}
