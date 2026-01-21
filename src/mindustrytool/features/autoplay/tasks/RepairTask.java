package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import mindustry.ai.types.RepairAI;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;

public class RepairTask implements AutoplayTask {
    private boolean enabled = true;
    private String status = "";
    private final RepairAI ai = new RepairAI();

    @Override
    public String getName() {
        return Iconc.hammer + " Auto Repair";
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
    public String getStatus() {
        return status;
    }

    @Override
    public boolean shouldRun(Unit unit) {
        boolean canHeal = unit.type.weapons.contains(w -> w.bullet.heals());

        if (!canHeal) {
            status = Core.bundle.get("autoplay.status.cannot-heal");
            return false;
        }

        // Check for damaged buildings within a reasonable range (e.g. 500 blocks
        // radius)
        boolean found = Units.findDamagedTile(unit.team, unit.x, unit.y) != null;

        if (!found) {
            status = Core.bundle.get("autoplay.status.no-damaged-buildings");
            return false;
        }

        status = Core.bundle.get("autoplay.status.active");
        return true;

    }

    @Override
    public AIController getAI() {
        return ai;
    }
}
