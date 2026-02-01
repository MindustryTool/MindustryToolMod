package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;

public class SelfHealTask implements AutoplayTask {
    private boolean enabled = true;
    private String status = "";
    private final SelfHealAI ai = new SelfHealAI();

    @Override
    public String getName() {
        return Iconc.refresh + " " + Core.bundle.get("autoplay.task.self-heal.name");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return Icon.refresh;
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
        if (unit.health < unit.maxHealth * 0.6f) {
            status = Core.bundle.get("autoplay.status.low-hp");
            return true;
        }

        if (unit.health < unit.maxHealth && status.equals(Core.bundle.get("autoplay.status.low-hp"))) {
            return true; // Keep healing until full
        }

        status = Core.bundle.get("autoplay.status.healthy");
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

    public static class SelfHealAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {
            Building core = unit.closestCore();
            if (core != null) {
                moveTo(core, 50f);
            }
        }
    }
}
