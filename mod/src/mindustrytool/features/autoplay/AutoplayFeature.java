package mindustrytool.features.autoplay;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.graphics.g2d.Draw;
import arc.math.geom.Vec2;
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
import mindustrytool.features.FeatureMetadata;
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
import solim.config.OrderedSeqPersister;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

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

    public final ConfigValue<Boolean> followUnit;
    public final ConfigValue<Seq<String>> taskOrder;
    public final ConfigValue<Seq<String>> disabledTasks;

    private final ObjectMap<String, AutoplayTask> taskMap = new ObjectMap<>();
    private final Signal<Seq<AutoplayTask>> tasksSignal = Signal.of(new Seq<>());
    private final Signal<String> currentTaskIdSignal = Signal.of(null);
    private @Nullable AutoplayTask currentTask;

    public AutoplayFeature() {
        super(FeatureMetadata.builder()
                .id("autoplay")
                .icon(FileIcon.of("autoplay.png"))
                .enabledByDefault(false)
                .quickAccess(true)
                .build());

        ConfigGroup config = configGroup();
        OrderedSeqPersister seqPersister = new OrderedSeqPersister();
        followUnit = config.boolValue("follow-unit", false);
        taskOrder = config.value("task-order", new Seq<>(), seqPersister);
        disabledTasks = config.value("disabled-tasks", new Seq<>(), seqPersister);

        taskMap.put(SelfHealTask.ID, new SelfHealTask(this));
        taskMap.put(FleeTask.ID, new FleeTask(this));
        taskMap.put(AttackTask.ID, new AttackTask());
        taskMap.put(RepairTask.ID, new RepairTask());
        taskMap.put(FollowAssistTask.ID, new FollowAssistTask(this));
        taskMap.put(SelfBuildTask.ID, new SelfBuildTask());
        taskMap.put(RebuildTask.ID, new RebuildTask());
        taskMap.put(MiningTask.ID, new MiningTask(this));

        syncOrderedTasks();

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
    public void onDisable() {
        Unit unit = Vars.player.unit();
        if (unit != null && unit.isValid()) {
            resetUnitState(unit);
            unit.controller(Vars.player);
        }
        setCurrentTask(null);
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

            if (Boolean.TRUE.equals(followUnit.get())) {
                Core.camera.position.lerp(unit.x, unit.y, 0.1f);
            }
        }
    }

    private void resetUnitState(Unit unit) {
        if (unit != null) {
            unit.isShooting(false);
            unit.mineTile = null;
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
