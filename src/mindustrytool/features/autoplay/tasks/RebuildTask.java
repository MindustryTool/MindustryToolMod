package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import mindustry.Vars;
import mindustry.entities.units.AIController;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.world.Build;

public class RebuildTask implements AutoplayTask {
    private boolean enabled = true;
    private String status = "";
    private final RebuildAI ai = new RebuildAI();

    @Override
    public String getName() {
        return Iconc.hammer + " " + Core.bundle.get("autoplay.task.rebuild.name");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return Icon.hammer;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean shouldRun(Unit unit) {
        if (!unit.canBuild()) {
            status = Core.bundle.get("autoplay.status.cannot-build");
            return false;
        }

        if (unit.team.data().plans.isEmpty()) {
            status = Core.bundle.get("autoplay.status.no-build-plans");
            return false;
        }

        status = Core.bundle.get("autoplay.status.building");
        return true;
    }

    @Override
    public AIController getAI() {
        return ai;
    }

    @Override
    public String getStatus() {
        return status;
    }

    public static class RebuildAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {
            if (unit.buildPlan() == null && !unit.team.data().plans.isEmpty()) {
                var blocks = unit.team.data().plans;
                var block = blocks.first();

                if (Vars.world.tile(block.x, block.y) != null
                        && Vars.world.tile(block.x, block.y).block() == block.block) {
                    blocks.removeFirst();
                } else if (Build.validPlace(block.block, unit.team(), block.x, block.y, block.rotation)) {
                    unit.addBuild(new BuildPlan(block.x, block.y, block.rotation, block.block, block.config));
                    blocks.addLast(blocks.removeFirst());
                } else {
                    blocks.addLast(blocks.removeFirst());
                }
            }

            var req = unit.buildPlan();
            if (req != null) {
                // Conflict detection (from behavior.txt)
                if (!req.breaking && timer.get(timerTarget2, 40f)) {
                    for (Player player : Groups.player) {
                        if (player.isBuilder() && player.unit() != null && player.unit().activelyBuilding()
                                && player.unit().buildPlan().samePos(req) && player.unit().buildPlan().breaking) {
                            unit.plans.removeFirst();
                            unit.team.data().plans.remove(p -> p.x == req.x && p.y == req.y);
                            return;
                        }
                    }
                }

                float range = Math.min(unit.type.buildRange - 20f, 100f);
                moveTo(req.tile(), range - 10f, 20f);
            }
        }
    }
}
