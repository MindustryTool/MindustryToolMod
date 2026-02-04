package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;

public class RepairTask implements AutoplayTask {
    private boolean enabled = true;
    private String status = "";
    private final CustomRepairAI ai = new CustomRepairAI();

    @Override
    public String getName() {
        return Iconc.hammer + " " + Core.bundle.get("autoplay.task.repair.name");
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

        Building tile = Units.findDamagedTile(unit.team, unit.x, unit.y);
        if (tile instanceof ConstructBuild)
            tile = null;

        if (tile == null) {
            status = Core.bundle.get("autoplay.status.no-damaged-buildings");
            return false;
        }

        ai.setTarget(tile);
        status = Core.bundle.get("autoplay.status.active");
        return true;
    }

    @Override
    public AIController getAI() {
        return ai;
    }

    public static class CustomRepairAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {

            float range = unit.type.range * 0.65f;
            moveTo(target, range);
            unit.aim(target);
            unit.controlWeapons(unit.within(target, unit.type.range));
        }
    }
}
