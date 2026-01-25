package mindustrytool.features.autoplay;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.Draw;
import arc.input.KeyCode;
import arc.scene.ui.Dialog;
import arc.struct.Seq;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.graphics.Layer;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.autoplay.tasks.AssistTask;
import mindustrytool.features.autoplay.tasks.AutoplayTask;
import mindustrytool.features.autoplay.tasks.MiningTask;
import mindustrytool.features.autoplay.tasks.RepairTask;

import java.util.Optional;

public class AutoplayFeature implements Feature {
    private final Seq<AutoplayTask> tasks = new Seq<>();
    private AutoplaySettingDialog dialog;
    private AutoplayTask currentTask;
    private boolean isEnabled = false;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.autoplay.name")
                .description("@feature.autoplay.description")
                .icon(Utils.icons("autoplay.png"))
                .quickAccess(true)
                .enabledByDefault(false)
                .build();
    }

    @Override
    public void init() {
        tasks.add(new RepairTask());
        tasks.add(new AssistTask());
        tasks.add(new MiningTask());

        // Load task order

        @SuppressWarnings("unchecked")
        Seq<String> savedOrder = Core.settings.getJson("mindustrytool.autoplay.taskOrder", Seq.class, Seq::new);

        if (savedOrder != null && savedOrder.size > 0) {
            Seq<AutoplayTask> ordered = new Seq<>();

            for (String id : savedOrder) {
                AutoplayTask t = tasks.find(task -> task.getId().equals(id));

                if (t != null) {
                    ordered.add(t);
                    tasks.remove(t);
                }
            }
            // Add remaining tasks (newly added ones not in save)
            ordered.addAll(tasks);
            tasks.clear();
            tasks.addAll(ordered);
        }

        tasks.each(AutoplayTask::init);

        Events.run(Trigger.update, this::updateUnit);
        Events.run(Trigger.draw, this::draw);

        Timer.schedule(() -> {
            updateTask();
        }, 0, 0.2f);
    }

    @Override
    public void onEnable() {
        isEnabled = true;
    }

    @Override
    public void onDisable() {
        isEnabled = false;
        var unit = Vars.player.unit();

        if (unit != null && !unit.dead && currentTask != null) {
            unit.controller(Vars.player);
        }

        currentTask = null;
    }

    public AutoplayTask getCurrentTask() {
        return currentTask;
    }

    private void draw() {
        if (!isEnabled) {
            return;
        }

        var unit = Vars.player.unit();

        if (unit == null) {
            return;
        }

        if (currentTask == null) {
            Draw.z(Layer.overlayUI);
            Draw.rect(Icon.none.getRegion(), unit.x, unit.y + unit.hitSize * 2f, 10f, 10f);
            Draw.reset();
            return;
        }

        var icon = currentTask.getIcon();

        if (icon == null) {
            return;
        }

        Draw.z(Layer.overlayUI);
        Draw.rect(icon.getRegion(), unit.x, unit.y + unit.hitSize * 2f, 10f, 10f);
        Draw.reset();
    }

    private void updateTask() {
        if (!isEnabled) {
            return;
        }

        var unit = Vars.player.unit();

        if (unit == null) {
            currentTask = null;
            return;
        }

        if (unit.dead) {
            currentTask = null;
            return;
        }

        AutoplayTask nextTask = null;

        for (AutoplayTask task : tasks) {
            if (task.isEnabled() && task.shouldRun(unit)) {
                nextTask = task;
                break;
            }
        }

        if (nextTask != currentTask) {
            if (nextTask != null) {
                var ai = nextTask.getAI();
                ai.unit(unit);
            }

            currentTask = nextTask;

            if (dialog.visible) {
                dialog.rebuild();
            }
        }
    }

    private void updateUnit() {
        if (!isEnabled) {
            return;
        }

        if (Core.input.isTouched() || Core.input.keyDown(KeyCode.anyKey)) {
            return;
        }

        var unit = Vars.player.unit();

        if (unit == null) {
            return;
        }

        if (currentTask != null) {
            currentTask.update(unit);

            if (Vars.state.isGame()) {
                currentTask.getAI().updateUnit();
            }
        }
    }

    @Override
    public Optional<Dialog> setting() {
        if (dialog == null) {
            dialog = new AutoplaySettingDialog(this);
        }
        return Optional.of(dialog);
    }

    public Seq<AutoplayTask> getTasks() {
        return tasks;
    }

    public void saveTaskOrder() {
        Seq<String> ids = new Seq<>();
        tasks.each(t -> ids.add(t.getId()));
        Core.settings.putJson("mindustrytool.autoplay.taskOrder", ids.toArray(String.class));
    }
}
