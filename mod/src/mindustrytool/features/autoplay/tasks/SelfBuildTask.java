package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.math.geom.Point2;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.game.Teams.TeamData;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.type.ItemStack;
import mindustry.world.Build;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.AutoplayFeature;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class SelfBuildTask implements AutoplayTask {

    public static final String ID = "self-build";

    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final SelfBuildAI ai = new SelfBuildAI();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return Core.bundle.get("feature.autoplay.task.self-build");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return FileIcon.of("hammer.png", Icon.hammer);
    }

    @Override
    public Readable<String> status() {
        return status;
    }

    @Override
    public boolean update(Unit unit) {
        if (!unit.canBuild()) {
            status.set(Core.bundle.get("feature.autoplay.status.cannot-build"));
            return false;
        }

        if (unit.plans.isEmpty()) {
            status.set(Core.bundle.get("feature.autoplay.status.no-build-plans"));
            return false;
        }

        Building core = unit.closestCore();

        BuildPlan bestPlan = null;
        float minDst = Float.MAX_VALUE;
        int personalPlanCount = 0;

        for (int i = 0; i < unit.plans.size; i++) {
            BuildPlan plan = unit.plans.get(i);
            if (plan == null || RebuildTask.isRebuildPlan(plan)) {
                continue;
            }
            personalPlanCount++;

            if (canAffordPlan(unit, core, plan)) {
                float dst = unit.dst2(plan.x * Vars.tilesize, plan.y * Vars.tilesize);
                if (dst < minDst) {
                    minDst = dst;
                    bestPlan = plan;
                }
            }
        }

        if (personalPlanCount == 0) {
            status.set(Core.bundle.get("feature.autoplay.status.no-build-plans"));
            return false;
        }

        if (bestPlan == null) {
            status.set(Core.bundle.get("feature.autoplay.status.lacking-materials"));
            return false;
        }

        if (unit.plans.first() != bestPlan) {
            unit.plans.remove(bestPlan);
            unit.plans.addFirst(bestPlan);
        }

        status.set(Core.bundle.format("feature.autoplay.status.building-self", personalPlanCount));
        return true;
    }

    private static boolean canAffordPlan(@Nullable Unit unit, @Nullable Building core, @Nullable BuildPlan plan) {
        if (plan == null) {
            return false;
        }
        if (plan.breaking) {
            return true;
        }
        if (plan.isDerelictRepair()) {
            return true;
        }
        if (Vars.state.rules.infiniteResources) {
            return true;
        }
        if (unit != null && unit.team != null) {
            if (plan.isRotation(unit.team)) {
                return true;
            }
            if (unit.team.rules().infiniteResources) {
                return true;
            }
        }
        if (core == null || plan.block == null) {
            return false;
        }
        if (plan.block.requirements == null) {
            return true;
        }
        for (ItemStack stack : plan.block.requirements) {
            if (core.items.get(stack.item) < Math.min(stack.amount, 50)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BaseAutoplayAI getAI() {
        return ai;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public void buildSettings(AutoplayFeature feature) {
        column().growX().gap(unit(1)).children(() -> {
            button(Core.bundle.get("feature.autoplay.settings.self-build.reclaim-derelict"), this::reclaimDerelicts)
                    .style(WebStyles.secondary())
                    .tooltip(Core.bundle.get("feature.autoplay.settings.self-build.reclaim-derelict.tooltip"))
                    .height(unit(10))
                    .growX();
            button(Core.bundle.get("feature.autoplay.settings.self-build.annihilate-derelict"), this::deconstructDerelicts)
                    .style(WebStyles.secondary())
                    .tooltip(Core.bundle.get("feature.autoplay.settings.self-build.annihilate-derelict.tooltip"))
                    .height(unit(10))
                    .growX();
        });
    }

    private void deconstructDerelicts() {
        Unit unit = Vars.player.unit();
        if (unit == null || !unit.isValid() || !unit.canBuild() || unit.team == Team.derelict) {
            return;
        }
        TeamData derelictTeam = Vars.state.teams.get(Team.derelict);
        if (derelictTeam == null) {
            return;
        }
        IntSet targets = new IntSet();
        derelictTeam.buildings.each(b -> targets.add(Point2.pack(b.tileX(), b.tileY())));
        removeNonRebuildPlansAt(unit, targets);
        derelictTeam.buildings.each(b -> {
            BuildPlan plan = new BuildPlan(b.tileX(), b.tileY());
            plan.breaking = true;
            unit.plans.add(plan);
        });
    }

    private void reclaimDerelicts() {
        Unit unit = Vars.player.unit();
        if (unit == null || !unit.isValid() || !unit.canBuild() || unit.team == Team.derelict) {
            return;
        }
        if (!Vars.state.rules.derelictRepair) {
            return;
        }
        TeamData derelictTeam = Vars.state.teams.get(Team.derelict);
        if (derelictTeam == null) {
            return;
        }
        Seq<Building> eligible = new Seq<>();
        derelictTeam.buildings.each(b -> {
            if (b != null && b.block != null && b.block.allowDerelictRepair
                    && Build.validPlaceIgnoreUnits(b.block, unit.team, b.tileX(), b.tileY(), b.rotation, true, true)) {
                eligible.add(b);
            }
        });
        if (eligible.isEmpty()) {
            return;
        }
        IntSet targets = new IntSet();
        eligible.each(b -> targets.add(Point2.pack(b.tileX(), b.tileY())));
        removeNonRebuildPlansAt(unit, targets);
        eligible.each(b -> unit.plans.add(new BuildPlan(b.tileX(), b.tileY(), b.rotation, b.block, b.config())));
    }

    private static void removeNonRebuildPlansAt(Unit unit, IntSet targets) {
        if (unit.plans == null || targets.isEmpty()) {
            return;
        }
        for (int i = unit.plans.size - 1; i >= 0; i--) {
            BuildPlan existing = unit.plans.get(i);
            if (existing != null && !RebuildTask.isRebuildPlan(existing)
                    && targets.contains(Point2.pack(existing.x, existing.y))) {
                unit.plans.removeIndex(i);
            }
        }
    }

    public static class SelfBuildAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {
            if (unit == null) {
                return;
            }
            BuildPlan req = unit.buildPlan();
            if (req != null) {
                Position target = req.tile() != null ? req.tile() : new Vec2(req.x * Vars.tilesize, req.y * Vars.tilesize);
                float range = Math.min(unit.type.buildRange - 20f, 100f);
                moveTo(target, Math.max(range - 10f, 20f), 20f);
            }
        }
    }
}
