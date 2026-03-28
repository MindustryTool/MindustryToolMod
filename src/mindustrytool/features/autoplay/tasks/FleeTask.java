package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import mindustry.entities.Units;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;

public class FleeTask implements AutoplayTask {
    private boolean enabled = true;
    private String status = "";
    private final FleeAI ai = new FleeAI();

    @Override
    public String getName() {
        return Iconc.move + " " + Core.bundle.get("autoplay.task.flee.name");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return Icon.move;
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
    public boolean update(Unit unit) {
        Unit enemy = Units.closestEnemy(unit.team, unit.x, unit.y, 300f, u -> !u.dead());
        if (enemy != null && enemy.inRange(enemy)) {
            ai.fleeFrom = enemy;
            status = Core.bundle.get("autoplay.status.fleeing");
            return true;
        }

        ai.fleeFrom = null;
        status = Core.bundle.get("autoplay.status.safe");
        return false;
    }

    @Override
    public FleeAI getAI() {
        return ai;
    }

    @Override
    public String getStatus() {
        return status;
    }

    public static class FleeAI extends BaseAutoplayAI {
        private Unit fleeFrom;

        @Override
        public void updateMovement() {
            if (fleeFrom != null) {
                moveTo(fleeFrom, fleeFrom.range());
            }
        }
    }
}
