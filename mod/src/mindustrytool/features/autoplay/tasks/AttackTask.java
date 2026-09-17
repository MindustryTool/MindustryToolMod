package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Healthc;
import mindustry.gen.Icon;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class AttackTask implements AutoplayTask {

    public static final String ID = "attack";

    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final AttackAI ai = new AttackAI();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return Core.bundle.get("feature.autoplay.task.attack");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return Icon.warning;
    }

    @Override
    public Readable<String> status() {
        return status;
    }

    @Override
    public boolean update(Unit unit) {
        if (!unit.canShoot() || unit.type.weapons.isEmpty() || !unit.type.hasWeapons()) {
            status.set(Core.bundle.get("feature.autoplay.status.unarmed"));
            unit.isShooting(false);
            return false;
        }

        float searchRange = Math.max(unit.range() * 1.2f, 400f);
        Teamc enemyUnit = Units.closestEnemy(unit.team, unit.x, unit.y, searchRange, u -> !u.dead());
        Building enemyBuilding = Vars.indexer.findEnemyTile(unit.team, unit.x, unit.y, searchRange, b -> true);

        Teamc target;
        if (enemyUnit != null && enemyBuilding != null) {
            target = unit.dst2(enemyUnit) < unit.dst2(enemyBuilding) ? enemyUnit : enemyBuilding;
        } else {
            target = enemyUnit != null ? enemyUnit : enemyBuilding;
        }

        if (target == null) {
            ai.setTarget(null);
            status.set(Core.bundle.get("feature.autoplay.status.no-enemies"));
            unit.isShooting(false);
            return false;
        }

        ai.setTarget(target);
        status.set(Core.bundle.get("feature.autoplay.status.attacking"));
        return true;
    }

    @Override
    public BaseAutoplayAI getAI() {
        return ai;
    }

    public static class AttackAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {
            if (unit == null) {
                return;
            }
            if (target == null || !target.isAdded() || (target instanceof Healthc && ((Healthc) target).dead())) {
                target = null;
                unit.isShooting(false);
                return;
            }

            float weaponRange = unit.range() > 0 ? unit.range() : unit.type.range;
            float kiteDistance = Math.max(weaponRange * 0.85f, 20f);
            moveTo(target, kiteDistance, 40f, true, null);
            unit.lookAt(target);
            unit.aim(target);
            unit.controlWeapons(unit.within(target, weaponRange));
            unit.isShooting(true);
        }
    }
}
