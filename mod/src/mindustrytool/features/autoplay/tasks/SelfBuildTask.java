package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.game.Teams.TeamData;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.type.ItemStack;
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
        return Icon.hammer;
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

        for (int i = 0; i < unit.plans.size; i++) {
            BuildPlan plan = unit.plans.get(i);
            if (plan == null) {
                continue;
            }

            if (plan.breaking || canAffordPlan(core, plan)) {
                float dst = unit.dst2(plan.x * Vars.tilesize, plan.y * Vars.tilesize);
                if (dst < minDst) {
                    minDst = dst;
                    bestPlan = plan;
                }
            }
        }

        if (bestPlan == null) {
            status.set(Core.bundle.get("feature.autoplay.status.lacking-materials"));
            return false;
        }

        if (unit.plans.first() != bestPlan) {
            unit.plans.remove(bestPlan);
            unit.plans.addFirst(bestPlan);
        }

        status.set(Core.bundle.format("feature.autoplay.status.building-self", unit.plans.size));
        return true;
    }

    private static boolean canAffordPlan(@Nullable Building core, @Nullable BuildPlan plan) {
        if (plan == null) {
            return false;
        }
        if (plan.breaking) {
            return true;
        }
        if (core == null || plan.block == null) {
            return false;
        }
        if (plan.block.requirements == null) {
            return true;
        }
        for (ItemStack stack : plan.block.requirements) {
            if (core.items.get(stack.item) < Math.min(stack.amount, 5)) {
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
            button(Core.bundle.get("feature.autoplay.settings.self-build.annihilate-derelict"), this::deconstructDerelicts)
                    .style(WebStyles.secondary())
                    .tooltip(Core.bundle.get("feature.autoplay.settings.self-build.annihilate-derelict.tooltip"))
                    .height(unit(10))
                    .growX();
        });
    }

    private void deconstructDerelicts() {
        Unit unit = Vars.player.unit();
        if (unit == null) {
            return;
        }
        TeamData derelictTeam = Vars.state.teams.get(Team.derelict);
        if (derelictTeam != null) {
            derelictTeam.buildings.each(b -> {
                BuildPlan plan = new BuildPlan(b.tile.x, b.tile.y);
                plan.breaking = true;
                unit.plans.add(plan);
            });
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
