package mindustrytool.features.autoplay;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.graphics.g2d.Draw;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.freecamera.FreeCameraFeature;
import mindustrytool.features.quickaccess.QuickAccessFeature;
import mindustrytool.features.autoplay.tasks.AttackTask;
import mindustrytool.features.autoplay.tasks.AutoplayTask;
import mindustrytool.features.autoplay.tasks.BaseAutoplayAI;
import mindustrytool.features.autoplay.tasks.FleeTask;
import mindustrytool.features.autoplay.tasks.FollowAssistTask;
import mindustrytool.features.autoplay.tasks.MiningTask;
import mindustrytool.features.autoplay.tasks.RebuildTask;
import mindustrytool.features.autoplay.tasks.RepairTask;
import mindustrytool.features.autoplay.tasks.SelfBuildTask;
import mindustrytool.features.autoplay.tasks.SelfHealTask;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.ContextualConfigValue;
import solim.config.OrderedSeqPersister;
import solim.core.Units;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.reactive.Signals;

public class AutoplayFeature extends Feature {

    public static final Seq<String> DEFAULT_ORDER = Seq.with(
            SelfHealTask.ID,
            FleeTask.ID,
            AttackTask.ID,
            RepairTask.ID,
            FollowAssistTask.ID,
            SelfBuildTask.ID,
            RebuildTask.ID,
            MiningTask.ID);

    public static final String DISPLAY_HUD = "hud";
    public static final String DISPLAY_POPUP = "popup";

    public final ConfigGroup config;
    public final ConfigValue<Boolean> followUnit;
    public final ConfigValue<Seq<String>> taskOrder;
    public final ConfigValue<Seq<String>> disabledTasks;
    public final ConfigValue<String> displayModeConfig;
    public final ConfigValue<Float> scaleConfig;
    public final ConfigValue<Boolean> hideDragHandleConfig;

    public final ConfigGroup positionGroup;
    public final ContextualConfigValue<Float, Boolean> xConfig;
    public final ContextualConfigValue<Float, Boolean> yConfig;

    public final Signal<Float> xSignal;
    public final Signal<Float> ySignal;

    private final ObjectMap<String, AutoplayTask> taskMap = new ObjectMap<>();
    private final Signal<Seq<AutoplayTask>> tasksSignal = Signal.of(new Seq<>());
    private final Signal<String> currentTaskIdSignal = Signal.of(null);
    private @Nullable AutoplayTask currentTask;
    private @Nullable AutoplayHudView hudView;
    private boolean quickAccessHooked = false;

    public AutoplayFeature() {
        super(FeatureMetadata.builder()
                .id("autoplay")
                .icon(FileIcon.of("autoplay.png"))
                .enabledByDefault(false)
                .quickAccess(true)
                .build());

        ConfigGroup config = configGroup();
        this.config = config;
        OrderedSeqPersister seqPersister = new OrderedSeqPersister();
        followUnit = config.boolValue("follow-unit", false);
        taskOrder = config.value("task-order", new Seq<>(), seqPersister);
        disabledTasks = config.value("disabled-tasks", new Seq<>(), seqPersister);
        displayModeConfig = config.stringValue("displayMode", DISPLAY_POPUP);
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
                    return sh > 0 ? sh / 2f : 250f;
                });

        xSignal = xConfig.signal();
        ySignal = yConfig.signal();

        displayModeConfig.signal().subscribe(mode -> updateHud());

        taskMap.put(SelfHealTask.ID, new SelfHealTask(this));
        taskMap.put(FleeTask.ID, new FleeTask(this));
        taskMap.put(AttackTask.ID, new AttackTask());
        taskMap.put(RepairTask.ID, new RepairTask());
        taskMap.put(FollowAssistTask.ID, new FollowAssistTask(this));
        taskMap.put(SelfBuildTask.ID, new SelfBuildTask());
        taskMap.put(RebuildTask.ID, new RebuildTask());
        taskMap.put(MiningTask.ID, new MiningTask(this));

        syncOrderedTasks();

        bindToggle("autoPlay", KeyCode.unset);

        Events.run(Trigger.update, this::update);
        Events.run(Trigger.draw, this::draw);
    }

    public Readable<Seq<AutoplayTask>> tasks() {
        return tasksSignal;
    }

    public boolean isTaskEnabled(String taskId) {
        Seq<String> disabled = disabledTasks.get();
        return disabled == null || !disabled.contains(taskId);
    }

    public void setTaskEnabled(String taskId, boolean enabled) {
        Seq<String> disabled = new Seq<>();
        Seq<String> stored = disabledTasks.get();
        if (stored != null) {
            disabled.addAll(stored);
        }

        if (enabled) {
            disabled.remove(taskId);
        } else if (!disabled.contains(taskId)) {
            disabled.add(taskId);
        }
        disabledTasks.set(disabled);
        syncOrderedTasks();
    }

    public void moveTaskUp(String taskId) {
        Seq<String> current = getEffectiveOrder();
        int index = current.indexOf(taskId);
        if (index > 0) {
            current.swap(index, index - 1);
            taskOrder.set(current);
            syncOrderedTasks();
        }
    }

    public void moveTaskDown(String taskId) {
        Seq<String> current = getEffectiveOrder();
        int index = current.indexOf(taskId);
        if (index >= 0 && index < current.size - 1) {
            current.swap(index, index + 1);
            taskOrder.set(current);
            syncOrderedTasks();
        }
    }

    private Seq<String> getEffectiveOrder() {
        Seq<String> result = new Seq<>();
        Seq<String> saved = taskOrder.get();
        if (saved != null) {
            for (String id : saved) {
                if (taskMap.containsKey(id) && !result.contains(id)) {
                    result.add(id);
                }
            }
        }
        for (String id : DEFAULT_ORDER) {
            if (!result.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    private void syncOrderedTasks() {
        Seq<String> order = getEffectiveOrder();
        Seq<AutoplayTask> orderedList = new Seq<>();
        for (String id : order) {
            AutoplayTask task = taskMap.get(id);
            if (task != null) {
                orderedList.add(task);
            }
        }
        tasksSignal.set(orderedList);
    }

    @Override
    public void onEnable() {
        updateHud();
    }

    @Override
    public void onDisable() {
        Unit unit = Vars.player.unit();
        if (unit != null && unit.isValid()) {
            resetUnitState(unit);
            unit.controller(Vars.player);
        }
        setCurrentTask(null);
        AutoplayPopup.hide();
        removeHud();
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
        AutoplayPopup.toggle(this, quickAccessBar);
    }

    public void openPopup(@Nullable Element quickAccessBar) {
        AutoplayPopup.toggle(this, quickAccessBar);
    }

    public void resetPosition() {
        float sw = Units.screenWidth();
        float sh = Units.screenHeight();
        float cx = sw > 0 ? sw / 2f : 400f;
        float cy = sh > 0 ? sh / 2f : 250f;

        Core.settings.put("mindustrytool.autoplay.position.x.portrait", cx);
        Core.settings.put("mindustrytool.autoplay.position.x.landscape", cx);
        Core.settings.put("mindustrytool.autoplay.position.y.portrait", cy);
        Core.settings.put("mindustrytool.autoplay.position.y.landscape", cy);

        xConfig.reset();
        yConfig.reset();

        if (hudView != null) {
            Core.app.post(hudView::keepInScreen);
        }
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
        hudView = new AutoplayHudView(this);
        Element el = hudView.element();
        el.name = "autoplay-hud";
        el.visible(() -> Vars.ui != null && Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown
                && Vars.state != null && Vars.state.isGame());

        Core.app.post(() -> {
            if (hudView != null && Vars.ui != null && Vars.ui.hudGroup != null && !isPopupActive()) {
                Vars.ui.hudGroup.addChild(el);
            }
        });
    }

    private void removeHud() {
        if (hudView == null) {
            return;
        }
        AutoplayHudView view = hudView;
        hudView = null;
        Core.app.post(() -> {
            view.element().remove();
            view.dispose();
        });
    }

    public @Nullable AutoplayTask getCurrentTask() {
        return currentTask;
    }

    public Readable<String> currentTaskId() {
        return currentTaskIdSignal;
    }

    private void setCurrentTask(@Nullable AutoplayTask task) {
        currentTask = task;
        currentTaskIdSignal.set(task != null ? task.getId() : null);
    }

    private void update() {
        if (!isEnabled() || !Vars.state.isPlaying()) {
            return;
        }

        Unit unit = Vars.player.unit();
        if (unit == null || !unit.isValid()) {
            setCurrentTask(null);
            return;
        }

        if (currentTask != null && currentTask.getAI().unit() != unit) {
            resetUnitState(unit);
            setCurrentTask(null);
        }

        AutoplayTask nextTask = null;
        for (AutoplayTask task : tasksSignal.peek()) {
            if (isTaskEnabled(task.getId()) && task.update(unit)) {
                nextTask = task;
                break;
            }
        }

        if (nextTask != currentTask) {
            if (currentTask != null) {
                resetUnitState(unit);
            }
            if (nextTask != null) {
                BaseAutoplayAI ai = nextTask.getAI();
                ai.unit(unit);
            }
            setCurrentTask(nextTask);
        }

        if (currentTask != null) {
            if (Vars.state.isGame()) {
                currentTask.getAI().updateUnit();
            }

            if (Boolean.TRUE.equals(followUnit.get()) && !FreeCameraFeature.isFreeCam()) {
                Core.camera.position.lerp(unit.x, unit.y, 0.1f);
            }
        }
    }

    void resetUnitState(@Nullable Unit unit) {
        if (unit != null) {
            unit.isShooting(false);
            unit.mineTile = null;
        }
        if (currentTask != null && currentTask.getAI() != null) {
            currentTask.getAI().clearTargetPos();
        }
    }

    private void draw() {
        if (!isEnabled() || !Vars.state.isPlaying()) {
            return;
        }

        Unit unit = Vars.player.unit();
        if (unit == null || !unit.isValid()) {
            return;
        }

        if (currentTask == null) {
            Draw.z(Layer.overlayUI);
            Draw.rect(Icon.none.getRegion(), unit.x, unit.y + unit.hitSize * 2f, 10f, 10f);
            Draw.reset();
            return;
        }

        TextureRegionDrawable icon = currentTask.getIcon();
        if (icon != null) {
            Draw.z(Layer.overlayUI);
            Draw.rect(icon.getRegion(), unit.x, unit.y + unit.hitSize * 2f, 10f, 10f);
            Draw.reset();
        }

        Vec2 targetPos = currentTask.getTargetPos();
        if (targetPos != null) {
            Draw.z(Layer.overlayUI);
            Drawf.dashLine(Pal.accent, unit.x, unit.y, targetPos.x, targetPos.y);
            Draw.reset();
        }
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> new AutoplaySettingsDialog(this);
    }
}
