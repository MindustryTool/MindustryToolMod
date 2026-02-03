package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;

public class AttackTask implements AutoplayTask {
    private boolean enabled = true;
    private String status = "";
    private final AttackAI ai = new AttackAI();

    @Override
    public String getName() {
        return Iconc.warning + " " + Core.bundle.get("autoplay.task.attack.name");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return Icon.warning;
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
        Teamc target = Units.closestEnemy(unit.team, unit.x, unit.y, 400f, u -> !u.dead());
        if (target != null) {
            ai.setTarget(target);
            status = Core.bundle.get("autoplay.status.attacking");
            return true;
        }

        status = Core.bundle.get("autoplay.status.no-enemies");
        unit.isShooting(false);
        return false;
    }

    @Override
    public AIController getAI() {
        return ai;
    }

    @Override
    public String getStatus() {
        return status;
    }

    public static class AttackAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {
            if (target == null || !target.isAdded() || (target instanceof mindustry.gen.Healthc h && h.dead())) {
                target = null;
                return;
            }

            float range = unit.type.range * 0.9f;
            moveTo(target, range);
            unit.lookAt(target);
            unit.controlWeapons(unit.within(target, unit.type.range));
            unit.isShooting(true);
        }
    }
}
