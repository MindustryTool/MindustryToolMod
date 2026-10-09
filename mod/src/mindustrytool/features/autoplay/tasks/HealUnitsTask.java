package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import mindustry.entities.Units;
import mindustry.entities.abilities.RepairFieldAbility;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.type.Weapon;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustrytool.components.FileIcon;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class HealUnitsTask implements AutoplayTask {

    public static final String ID = "heal-units";

    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final HealUnitsAI ai = new HealUnitsAI();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return Core.bundle.get("feature.autoplay.task.heal-units");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return FileIcon.of("sparkles.png", Icon.add);
    }

    @Override
    public Readable<String> status() {
        return status;
    }

    public static boolean canHealUnits(Unit unit) {
        if (unit == null || unit.type == null) {
            return false;
        }
        if (hasUnitHealWeapon(unit)) {
            return true;
        }
        if (unit.type.abilities != null) {
            for (int i = 0; i < unit.type.abilities.size; i++) {
                if (unit.type.abilities.get(i) instanceof RepairFieldAbility) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasUnitHealWeapon(Unit unit) {
        if (unit == null || unit.type == null || unit.type.weapons == null) {
            return false;
        }
        for (int i = 0; i < unit.type.weapons.size; i++) {
            Weapon w = unit.type.weapons.get(i);
            if (w == null) {
                continue;
            }
            boolean canHeal = w instanceof RepairBeamWeapon
                    ? ((RepairBeamWeapon) w).targetUnits
                    : (w.bullet != null && w.bullet.heals());
            if (canHeal) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean update(Unit unit) {
        if (!canHealUnits(unit)) {
            status.set(Core.bundle.get("feature.autoplay.status.cannot-heal"));
            return false;
        }

        float searchRange = unit.type != null ? Math.max(unit.type.range * 2.5f, 500f) : 500f;
        Unit damagedAlly = Units.closest(unit.team, unit.x, unit.y, searchRange,
                u -> u != unit && u.damaged() && !u.dead());

        if (damagedAlly == null) {
            ai.setTarget(null);
            unit.isShooting(false);
            unit.controlWeapons(false, false);
            status.set(Core.bundle.get("feature.autoplay.status.no-damaged-allies"));
            return false;
        }

        ai.setTarget(damagedAlly);
        status.set(Core.bundle.get("feature.autoplay.status.healing-allies"));
        return true;
    }

    @Override
    public BaseAutoplayAI getAI() {
        return ai;
    }

    public static class HealUnitsAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {
            if (target == null || !target.isAdded() || !(target instanceof Unit)) {
                target = null;
                unit.isShooting(false);
                unit.controlWeapons(false, false);
                return;
            }

            Unit ally = (Unit) target;
            if (!ally.isValid() || ally.dead() || ally.health() >= ally.maxHealth() || ally.team != unit.team) {
                target = null;
                unit.isShooting(false);
                unit.controlWeapons(false, false);
                return;
            }

            float range = unit.type != null ? unit.type.range : 80f;
            float approachRange = range * 0.65f;

            if (!target.within(unit, approachRange)) {
                moveTo(target, approachRange);
            } else {
                targetPos.set(target.getX(), target.getY());
                hasTargetPos = true;
            }

            unit.lookAt(target);

            boolean inRange = target.within(unit, range);
            if (inRange && hasUnitHealWeapon(unit)) {
                unit.aim(target);
                unit.controlWeapons(true, true);
                unit.isShooting(true);
            } else {
                unit.controlWeapons(false, false);
                unit.isShooting(false);
            }
        }
    }
}
