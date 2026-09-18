package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.util.Nullable;
import java.util.Iterator;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Teams.BlockPlan;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.ItemStack;
import mindustry.world.Build;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class RebuildTask implements AutoplayTask {

    public static final String ID = "rebuild";

    private static final ObjectSet<BuildPlan> rebuildPlans = new ObjectSet<>();

    public static void registerRebuildPlan(BuildPlan plan) {
        rebuildPlans.add(plan);
    }

    public static boolean isRebuildPlan(@Nullable BuildPlan plan) {
        return plan != null && rebuildPlans.contains(plan);
    }

    public static void cleanStalePlans(@Nullable Unit unit) {
        if (rebuildPlans.isEmpty() || unit == null || unit.plans == null) {
            return;
        }
        Iterator<BuildPlan> it = rebuildPlans.iterator();
        while (it.hasNext()) {
            BuildPlan bp = it.next();
            if (!unit.plans.contains(bp)) {
                it.remove();
            }
        }
    }

    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final RebuildAI ai = new RebuildAI();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return Core.bundle.get("feature.autoplay.task.rebuild");
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

        cleanStalePlans(unit);

        if (unit.buildPlan() != null && isRebuildPlan(unit.buildPlan())) {
            status.set(Core.bundle.get("feature.autoplay.status.rebuilding"));
            return true;
        }

        if (unit.team.data() == null) {
            status.set(Core.bundle.get("feature.autoplay.status.no-build-plans"));
            return false;
        }

        Queue<BlockPlan> plans = unit.team.data().plans;
        if (plans == null || plans.isEmpty()) {
            status.set(Core.bundle.get("feature.autoplay.status.no-build-plans"));
            return false;
        }

        Building core = unit.closestCore();

        BlockPlan bestPlan = null;
        int bestIndex = -1;
        float minDst = Float.MAX_VALUE;

        for (int i = 0; i < plans.size; i++) {
            BlockPlan plan = plans.get(i);
            if (plan == null) {
                continue;
            }

            if (canAffordPlan(core, plan)) {
                float dst = unit.dst2(plan.x * Vars.tilesize, plan.y * Vars.tilesize);
                if (dst < minDst) {
                    minDst = dst;
                    bestPlan = plan;
                    bestIndex = i;
                }
            }
        }

        if (bestPlan == null) {
            status.set(Core.bundle.get("feature.autoplay.status.lacking-materials"));
            return false;
        }

        if (bestIndex > 0) {
            plans.removeIndex(bestIndex);
            plans.addFirst(bestPlan);
        }

        status.set(Core.bundle.get("feature.autoplay.status.rebuilding"));
        return true;
    }

    private static boolean canAffordPlan(@Nullable Building core, @Nullable BlockPlan plan) {
        if (plan == null || core == null || plan.block == null) {
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

    public static class RebuildAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {
            if (unit == null || unit.team.data() == null) {
                return;
            }
            Queue<BlockPlan> plans = unit.team.data().plans;
            if (plans == null) {
                return;
            }
            if (unit.buildPlan() == null && !plans.isEmpty()) {
                BlockPlan block = plans.first();

                if (Vars.world.tile(block.x, block.y) != null && Vars.world.tile(block.x, block.y).block() == block.block) {
                    plans.removeFirst();
                } else if (Build.validPlace(block.block, unit.team(), block.x, block.y, block.rotation)) {
                    BuildPlan bp = new BuildPlan(block.x, block.y, block.rotation, block.block, block.config);
                    registerRebuildPlan(bp);
                    unit.addBuild(bp);
                    plans.addLast(plans.removeFirst());
                } else {
                    plans.addLast(plans.removeFirst());
                }
            }

            BuildPlan req = unit.buildPlan();
            if (req != null) {
                if (!req.breaking && timer.get(timerTarget2, 40f)) {
                    for (Player player : Groups.player) {
                        if (player.isBuilder() && player.unit() != null && player.unit().activelyBuilding()
                                && player.unit().buildPlan() != null && player.unit().buildPlan().samePos(req)
                                && player.unit().buildPlan().breaking) {
                            unit.plans.removeFirst();
                            BlockPlan found = unit.team.data().plans.find(p -> p.x == req.x && p.y == req.y);
                            if (found != null) {
                                unit.team.data().plans.remove(found, true);
                            }
                            return;
                        }
                    }
                }

                Position target = req.tile() != null ? req.tile() : new Vec2(req.x * Vars.tilesize, req.y * Vars.tilesize);
                float range = Math.min(unit.type.buildRange - 20f, 100f);
                moveTo(target, Math.max(range - 10f, 20f), 20f);
            }
        }
    }
}
