package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.abilities.RepairFieldAbility;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.type.Weapon;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
import mindustrytool.components.FileIcon;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class RepairTask implements AutoplayTask {

    public static final String ID = "repair";

    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final RepairAI ai = new RepairAI();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return Core.bundle.get("feature.autoplay.task.repair");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return FileIcon.of("wrench.png", Icon.hammer);
    }

    @Override
    public Readable<String> status() {
        return status;
    }

    public static boolean canHeal(Unit unit) {
        if (unit == null || unit.type == null) {
            return false;
        }
        if (unit.type.canHeal) {
            return true;
        }
        if (hasHealWeapon(unit)) {
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

    public static boolean hasHealWeapon(Unit unit) {
        if (unit == null || unit.type == null || unit.type.weapons == null) {
            return false;
        }
        for (int i = 0; i < unit.type.weapons.size; i++) {
            Weapon w = unit.type.weapons.get(i);
            if (w != null && ((w.bullet != null && w.bullet.heals()) || w instanceof RepairBeamWeapon)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean update(Unit unit) {
        if (!canHeal(unit)) {
            status.set(Core.bundle.get("feature.autoplay.status.cannot-heal"));
            return false;
        }

        Building damagedBuilding = Units.findDamagedTile(unit.team, unit.x, unit.y);
        if (damagedBuilding instanceof ConstructBuild || (damagedBuilding != null && !damagedBuilding.damaged())) {
            damagedBuilding = null;
            if (Vars.indexer != null) {
                Seq<Building> damagedList = Vars.indexer.getDamaged(unit.team);
                if (damagedList != null && !damagedList.isEmpty()) {
                    float minDst = Float.MAX_VALUE;
                    for (int i = 0; i < damagedList.size; i++) {
                        Building b = damagedList.get(i);
                        if (b != null && b.damaged() && !(b instanceof ConstructBuild)) {
                            float dst = unit.dst2(b);
                            if (dst < minDst) {
                                minDst = dst;
                                damagedBuilding = b;
                            }
                        }
                    }
                }
            }
        }

        if (damagedBuilding == null && Vars.indexer != null) {
            damagedBuilding = Units.findAllyTile(unit.team, unit.x, unit.y, 800f,
                    b -> b != null && b.damaged() && !(b instanceof ConstructBuild));
        }

        if (damagedBuilding == null) {
            ai.setTarget(null);
            unit.isShooting(false);
            unit.controlWeapons(false, false);
            status.set(Core.bundle.get("feature.autoplay.status.no-damaged-buildings"));
            return false;
        }

        ai.setTarget(damagedBuilding);
        status.set(Core.bundle.get("feature.autoplay.status.repairing"));
        return true;
    }

    @Override
    public BaseAutoplayAI getAI() {
        return ai;
    }

    public static class RepairAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {
            if (target == null || !target.isAdded() || !(target instanceof Building)) {
                target = null;
                unit.isShooting(false);
                unit.controlWeapons(false, false);
                return;
            }

            Building b = (Building) target;
            if (!b.isValid() || b.health() >= b.maxHealth() || b.team != unit.team) {
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
            if (inRange && hasHealWeapon(unit)) {
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
