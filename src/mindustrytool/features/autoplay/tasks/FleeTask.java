package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Teamc;
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
    public boolean shouldRun(Unit unit) {
        Teamc enemy = Units.closestEnemy(unit.team, unit.x, unit.y, 300f, u -> !u.dead());
        if (enemy != null) {
            status = Core.bundle.get("autoplay.status.fleeing");
            return true;
        }

        status = Core.bundle.get("autoplay.status.safe");
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

    public static class FleeAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {
            Building core = unit.closestCore();
            if (core != null) {
                moveTo(core, 120f);
            }
        }
    }
}
