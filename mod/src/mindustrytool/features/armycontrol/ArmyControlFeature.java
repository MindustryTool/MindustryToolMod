package mindustrytool.features.armycontrol;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.input.KeyCode;
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
import mindustrytool.components.SettingKeybindRow;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.quickaccess.QuickAccessFeature;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.ContextualConfigValue;
import solim.core.Units;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.reactive.Signals;

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

    private final Seq<Unit> poolUnits = new Seq<>();
    private final Signal<ObjectIntMap<UnitType>> discoveredTypesSignal = Signal.of(new ObjectIntMap<>());
    private final Signal<ObjectIntMap<UnitType>> activeSelectionSignal = Signal.of(new ObjectIntMap<>());
    private final Signal<Seq<ControlGroup>> controlGroupsSignal = Signal.of(new Seq<>());
    private final Computed<Boolean> contentEmpty = Signal.computed(() -> {
        ObjectIntMap<UnitType> pool = discoveredTypesSignal.get();
        Seq<ControlGroup> groups = controlGroupsSignal.get();
        return (pool == null || pool.size == 0) && (groups == null || groups.size == 0);
    });

    private boolean noneMode = false;
    private boolean uiSelectionAction = false;

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

        bindDialog("armycontrolsettings", KeyCode.unset, getSettingDialog(), false);

        Events.on(WorldLoadEvent.class, e -> onWorldReset());
        Events.on(ResetEvent.class, e -> onWorldReset());

        Effect.of(displayModeConfig.signal(), mode -> {
            ArmyControlPopup.hide();
            updateHud();
        });

        Events.run(Trigger.update, () -> {
            if (isEnabled() && Vars.state != null && Vars.state.isGame() && !Vars.mobile) {
                checkControlGroupShortcuts();
            }
            if (++pollTicks < SELECTION_POLL_INTERVAL) {
                return;
            }
            pollTicks = 0;
            if (isEnabled()) {
                refreshActiveSelection();
            }
        });
    }

    private void checkControlGroupShortcuts() {
        if (Core.scene.hasField() || SettingKeybindRow.isAnyListening()) {
            return;
        }

        boolean ctrl = Core.input.ctrl();
        for (int i = 1; i <= 9; i++) {
            KeyCode numKey = KeyCode.byOrdinal(KeyCode.num1.ordinal() + (i - 1));
            if (Core.input.keyRelease(numKey)) {
                if (ctrl) {
                    saveControlGroupSlot(i);
                } else {
                    recallControlGroupSlot(i);
                }
                break;
            }
        }
    }

    public void saveControlGroupSlot(int slot) {
        IntSeq selected = getSelectedUnitIds();
        Seq<ControlGroup> groups = controlGroupsSignal.peek();
        Seq<ControlGroup> next = groups != null ? new Seq<>(groups) : new Seq<>();

        ControlGroup existing = null;
        for (int i = 0; i < next.size; i++) {
            if (next.get(i).getId() == slot) {
                existing = next.get(i);
                break;
            }
        }

        if (selected.isEmpty()) {
            if (existing != null) {
                next.remove(existing);
                controlGroupsSignal.set(next);
            }
            return;
        }

        if (existing != null) {
            existing.setUnits(selected);
            controlGroupsSignal.set(new Seq<>(next));
        } else {
            ControlGroup created = new ControlGroup(slot, String.valueOf(slot));
            created.setUnits(selected);
            next.add(created);
            controlGroupsSignal.set(next);
        }
    }

    public void recallControlGroupSlot(int slot) {
        Seq<ControlGroup> groups = controlGroupsSignal.peek();
        if (groups == null) {
            return;
        }
        for (int i = 0; i < groups.size; i++) {
            ControlGroup g = groups.get(i);
            if (g.getId() == slot) {
                recallGroup(g);
                return;
            }
        }
    }

    private void onWorldReset() {
        poolUnits.clear();
        noneMode = false;
        uiSelectionAction = false;
        controlGroupsSignal.set(new Seq<>());
        discoveredTypesSignal.set(new ObjectIntMap<>());
        activeSelectionSignal.set(new ObjectIntMap<>());
    }

    public Signal<ObjectIntMap<UnitType>> discoveredTypesSignal() {
        return discoveredTypesSignal;
    }

    public Signal<ObjectIntMap<UnitType>> activeSelectionSignal() {
        return activeSelectionSignal;
    }

    public Signal<Seq<ControlGroup>> controlGroupsSignal() {
        return controlGroupsSignal;
    }

    public Readable<Boolean> contentEmptySignal() {
        return contentEmpty;
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
            Effect.of(qa.enabled(), v -> updateHud());
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

    public void selectAllMap() {
        if (Vars.control == null || Vars.control.input == null || Vars.player == null) {
            return;
        }
        Seq<Unit> selected = Vars.control.input.selectedUnits;
        selected.clear();
        poolUnits.clear();
        if (Groups.unit != null) {
            for (Unit u : Groups.unit) {
                if (u.team == Vars.player.team() && u != Vars.player.unit() && u.isValid() && u.allowCommand()) {
                    selected.add(u);
                    poolUnits.add(u);
                }
            }
        }
        noneMode = false;
        uiSelectionAction = true;
        Vars.control.input.commandMode = true;
        refreshActiveSelection();
    }

    public void selectScreen() {
        if (Vars.control == null || Vars.control.input == null || Vars.player == null || Core.camera == null) {
            return;
        }
        Seq<Unit> selected = Vars.control.input.selectedUnits;
        selected.clear();
        poolUnits.clear();
        Rect bounds = new Rect();
        Core.camera.bounds(bounds);
        if (Groups.unit != null) {
            for (Unit u : Groups.unit) {
                if (u.team == Vars.player.team() && u != Vars.player.unit() && u.isValid() && u.allowCommand()
                        && bounds.contains(u.x, u.y)) {
                    selected.add(u);
                    poolUnits.add(u);
                }
            }
        }
        noneMode = false;
        uiSelectionAction = true;
        Vars.control.input.commandMode = true;
        refreshActiveSelection();
    }

    public void deselectAll() {
        if (Vars.control != null && Vars.control.input != null) {
            Vars.control.input.selectedUnits.clear();
        }
        poolUnits.clear();
        noneMode = false;
        uiSelectionAction = false;
        refreshActiveSelection();
    }

    public void deselectUnitType(UnitType type) {
        if (Vars.control == null || Vars.control.input == null || type == null) {
            return;
        }
        uiSelectionAction = true;
        Seq<Unit> selected = Vars.control.input.selectedUnits;
        for (int i = selected.size - 1; i >= 0; i--) {
            Unit u = selected.get(i);
            if (u != null && u.type == type) {
                selected.remove(i);
            }
        }
        if (selected.isEmpty()) {
            noneMode = true;
        }
        Vars.control.input.commandMode = true;
        refreshActiveSelection();
    }

    public void toggleUnitType(UnitType type) {
        if (Vars.control == null || Vars.control.input == null || type == null) {
            return;
        }
        uiSelectionAction = true;
        Seq<Unit> selected = Vars.control.input.selectedUnits;
        boolean currentlyActive = false;
        for (int i = 0; i < selected.size; i++) {
            Unit u = selected.get(i);
            if (u != null && u.type == type) {
                currentlyActive = true;
                break;
            }
        }

        if (currentlyActive) {
            for (int i = selected.size - 1; i >= 0; i--) {
                Unit u = selected.get(i);
                if (u != null && u.type == type) {
                    selected.remove(i);
                }
            }
            if (selected.isEmpty()) {
                noneMode = true;
            }
        } else {
            for (int i = 0; i < poolUnits.size; i++) {
                Unit u = poolUnits.get(i);
                if (u != null && u.isValid() && u.allowCommand() && u.type == type && !selected.contains(u, true)) {
                    selected.add(u);
                }
            }
            noneMode = false;
        }
        Vars.control.input.commandMode = true;
        refreshActiveSelection();
    }

    public void selectAllPool() {
        if (Vars.control == null || Vars.control.input == null) {
            return;
        }
        uiSelectionAction = true;
        noneMode = false;
        Seq<Unit> selected = Vars.control.input.selectedUnits;
        selected.clear();
        for (int i = 0; i < poolUnits.size; i++) {
            Unit u = poolUnits.get(i);
            if (u != null && u.isValid() && u.allowCommand()) {
                selected.add(u);
            }
        }
        Vars.control.input.commandMode = true;
        refreshActiveSelection();
    }

    public void selectNonePool() {
        if (Vars.control == null || Vars.control.input == null) {
            return;
        }
        uiSelectionAction = true;
        noneMode = true;
        Vars.control.input.selectedUnits.clear();
        refreshActiveSelection();
    }

    public void invertPool() {
        if (Vars.control == null || Vars.control.input == null) {
            return;
        }
        uiSelectionAction = true;
        Seq<Unit> selected = Vars.control.input.selectedUnits;
        Seq<Unit> previouslySelected = new Seq<>(selected);
        selected.clear();
        for (int i = 0; i < poolUnits.size; i++) {
            Unit u = poolUnits.get(i);
            if (u != null && u.isValid() && u.allowCommand() && !previouslySelected.contains(u, true)) {
                selected.add(u);
            }
        }
        noneMode = selected.isEmpty();
        Vars.control.input.commandMode = true;
        refreshActiveSelection();
    }

    public void refreshActiveSelection() {
        Seq<Unit> selected = Vars.control != null && Vars.control.input != null ? Vars.control.input.selectedUnits : null;

        if (selected != null && selected.size > 0) {
            if (!uiSelectionAction) {
                boolean containsNew = false;
                for (int i = 0; i < selected.size; i++) {
                    Unit u = selected.get(i);
                    if (u != null && u.isValid() && !poolUnits.contains(u, true)) {
                        containsNew = true;
                        break;
                    }
                }
                if (containsNew || poolUnits.isEmpty()) {
                    poolUnits.clear();
                    for (int i = 0; i < selected.size; i++) {
                        Unit u = selected.get(i);
                        if (u != null && u.isValid()) {
                            poolUnits.add(u);
                        }
                    }
                }
            }
            noneMode = false;
        } else if (!noneMode) {
            poolUnits.clear();
        }
        uiSelectionAction = false;

        for (int i = poolUnits.size - 1; i >= 0; i--) {
            Unit u = poolUnits.get(i);
            if (u == null || !u.isValid()) {
                poolUnits.remove(i);
            }
        }

        ObjectIntMap<UnitType> poolMap = new ObjectIntMap<>();
        for (int i = 0; i < poolUnits.size; i++) {
            Unit u = poolUnits.get(i);
            if (u != null && u.isValid() && u.type != null) {
                poolMap.put(u.type, poolMap.get(u.type, 0) + 1);
            }
        }

        ObjectIntMap<UnitType> activeMap = new ObjectIntMap<>();
        if (selected != null) {
            for (int i = 0; i < selected.size; i++) {
                Unit u = selected.get(i);
                if (u != null && u.isValid() && u.type != null) {
                    activeMap.put(u.type, activeMap.get(u.type, 0) + 1);
                }
            }
        }

        if (!sameSelection(discoveredTypesSignal.peek(), poolMap)) {
            discoveredTypesSignal.set(poolMap);
        }
        if (!sameSelection(activeSelectionSignal.peek(), activeMap)) {
            activeSelectionSignal.set(activeMap);
        }
    }

    static boolean sameSelection(@Nullable ObjectIntMap<UnitType> a, @Nullable ObjectIntMap<UnitType> b) {
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

    public void recallGroup(ControlGroup group) {
        if (Vars.control == null || Vars.control.input == null || group == null) {
            return;
        }
        group.pruneDeadUnits();
        Seq<Unit> selected = Vars.control.input.selectedUnits;
        selected.clear();
        poolUnits.clear();
        IntSeq ids = group.getUnitIds();
        for (int i = 0; i < ids.size; i++) {
            Unit u = Groups.unit != null ? Groups.unit.getByID(ids.get(i)) : null;
            if (u != null && u.isValid() && u.allowCommand()) {
                selected.add(u);
                poolUnits.add(u);
            }
        }
        noneMode = false;
        uiSelectionAction = true;
        Vars.control.input.commandMode = true;
        refreshActiveSelection();
    }

    public void deleteGroup(ControlGroup group) {
        Seq<ControlGroup> groups = controlGroupsSignal.peek();
        if (groups == null || group == null) {
            return;
        }
        Seq<ControlGroup> next = new Seq<>(groups);
        next.remove(group);
        controlGroupsSignal.set(next);
    }

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
