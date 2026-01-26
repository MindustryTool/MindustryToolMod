package mindustrytool.features.autoplay;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.Draw;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.Dialog;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Timer;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.core.World;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.graphics.Layer;
import mindustry.input.Binding;
import mindustry.input.InputHandler;
import mindustry.input.MobileInput;
import mindustry.input.PlaceMode;
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
    private InputHandler origInputHandler;

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

        origInputHandler = Vars.control.input;

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

        if (Vars.mobile) {
            Vars.control.input = new MobileInput() {
                @Override
                public void update() {
                    super.update();

                    movement.setZero();

                    boolean locked = locked();

                    if (!commandMode) {
                        queueCommandMode = false;
                    } else {
                        mode = PlaceMode.none;
                        schematicMode = false;
                    }

                    // cannot rebuild and place at the same time
                    if (block != null) {
                        rebuildMode = false;
                    }

                    if (Vars.player.dead()) {
                        mode = PlaceMode.none;
                        manualShooting = false;
                        payloadTarget = null;
                    }

                    if (locked || block != null || Core.scene.hasField() || hasSchematic()) {
                        commandMode = false;
                    }

                    // validate commanding units
                    selectedUnits.removeAll(u -> !u.allowCommand() || !u.isValid() || u.team != Vars.player.team());

                    if (!commandMode) {
                        commandBuildings.clear();
                        selectedUnits.clear();
                    }

                    // zoom camera
                    if (!locked && !Core.scene.hasKeyboard() && !Core.scene.hasScroll()
                            && Math.abs(Core.input.axisTap(Binding.zoom)) > 0
                            && !Core.input.keyDown(Binding.rotatePlaced)
                            && (Core.input.keyDown(Binding.diagonalPlacement)
                                    || ((!Vars.player.isBuilder() || !isPlacing() || !block.rotate)
                                            && selectPlans.isEmpty()))) {
                        Vars.renderer.scaleCamera(Core.input.axisTap(Binding.zoom));
                    }

                    if (!Core.settings.getBool("keyboard") && !locked && !Core.scene.hasKeyboard()) {
                        // move camera around
                        float camSpeed = 6f;
                        Vec2 delta = Tmp.v1.setZero()
                                .add(Core.input.axis(Binding.moveX), Core.input.axis(Binding.moveY)).nor()
                                .scl(Time.delta * camSpeed);
                        Core.camera.position.add(delta);
                        if (!delta.isZero()) {
                            spectating = null;
                        }
                    }

                    if (Core.settings.getBool("keyboard")) {
                        if (Core.input.keyRelease(Binding.select)) {
                            Vars.player.shooting = false;
                        }

                        if (Vars.player.shooting && !canShoot()) {
                            Vars.player.shooting = false;
                        }
                    }

                    if (!Vars.player.dead() && !Vars.state.isPaused() && !locked && !isEnabled) {
                        updateMovement(Vars.player.unit());
                    }

                    // reset state when not placing
                    if (mode == PlaceMode.none) {
                        lineMode = false;
                    }

                    if (lineMode && mode == PlaceMode.placing && block == null) {
                        lineMode = false;
                    }

                    // if there is no mode and there's a recipe, switch to placing
                    if (block != null && mode == PlaceMode.none) {
                        mode = PlaceMode.placing;
                    }

                    if (block == null && mode == PlaceMode.placing) {
                        mode = PlaceMode.none;
                    }

                    // stop schematic when in block mode
                    if (block != null) {
                        schematicMode = false;
                    }

                    // stop select when not in schematic mode
                    if (!schematicMode && (mode == PlaceMode.schematicSelect || mode == PlaceMode.rebuildSelect)) {
                        mode = PlaceMode.none;
                    }

                    if (!rebuildMode && mode == PlaceMode.rebuildSelect) {
                        mode = PlaceMode.none;
                    }

                    if (mode == PlaceMode.schematicSelect || mode == PlaceMode.rebuildSelect) {
                        lastLineX = getRawTileX();
                        lastLineY = getRawTileY();
                        autoPan();
                    }

                    // automatically switch to placing after a new recipe is selected
                    if (lastBlock != block && mode == PlaceMode.breaking && block != null) {
                        mode = PlaceMode.placing;
                        lastBlock = block;
                    }

                    if (lineMode) {
                        lineScale = Mathf.lerpDelta(lineScale, 1f, 0.1f);

                        // When in line mode, pan when near screen edges automatically
                        if (Core.input.isTouched(0)) {
                            autoPan();
                        }

                        int lx = getTileX(Core.input.mouseX()), ly = getTileY(Core.input.mouseY());

                        if ((lastLineX != lx || lastLineY != ly) && isPlacing()) {
                            lastLineX = lx;
                            lastLineY = ly;
                            updateLine(lineStartX, lineStartY, lx, ly);
                        }
                    } else {
                        linePlans.clear();
                        lineScale = 0f;
                    }

                    // remove place plans that have disappeared
                    for (int i = removals.size - 1; i >= 0; i--) {

                        if (removals.get(i).animScale <= 0.0001f) {
                            removals.remove(i);
                            i--;
                        }
                    }

                    if (Vars.player.shooting && !Vars.player.dead()
                            && (Vars.player.unit().activelyBuilding() || Vars.player.unit().mining())) {
                        Vars.player.shooting = false;
                    }
                }

                int getRawTileX() {
                    return World.toTile(Core.input.mouseWorld().x);
                }

                int getRawTileY() {
                    return World.toTile(Core.input.mouseWorld().y);
                }

                int getTileX(float cursorX) {
                    Vec2 vec = Core.input.mouseWorld(cursorX, 0);
                    if (selectedBlock()) {
                        vec.sub(block.offset, block.offset);
                    }
                    return World.toTile(vec.x);
                }

                int getTileY(float cursorY) {
                    Vec2 vec = Core.input.mouseWorld(0, cursorY);
                    if (selectedBlock()) {
                        vec.sub(block.offset, block.offset);
                    }
                    return World.toTile(vec.y);
                }
            };
        }
    }

    @Override
    public void onDisable() {
        isEnabled = false;
        var unit = Vars.player.unit();

        if (unit != null && !unit.dead && currentTask != null) {
            unit.controller(Vars.player);
        }

        currentTask = null;

        if (Vars.mobile) {
            Vars.control.input = origInputHandler;
        }
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

            if (dialog != null && dialog.visible) {
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
