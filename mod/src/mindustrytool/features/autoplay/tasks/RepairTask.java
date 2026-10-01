package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.abilities.RepairFieldAbility;
import mindustry.gen.Building;
import mindustry.gen.Healthc;
import mindustry.gen.Icon;
import mindustry.gen.Teamc;
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

    @Override
    public boolean update(Unit unit) {
        boolean hasHealWeapon = unit.type != null && unit.type.weapons != null
                && unit.type.weapons.contains(w -> (w.bullet != null && w.bullet.heals()) || w instanceof RepairBeamWeapon);
        boolean hasRepairField = false;
        if (unit.type != null && unit.type.abilities != null) {
            for (int i = 0; i < unit.type.abilities.size; i++) {
                if (unit.type.abilities.get(i) instanceof RepairFieldAbility) {
                    hasRepairField = true;
                    break;
                }
            }
        }
        if (!hasHealWeapon && !hasRepairField) {
            status.set(Core.bundle.get("feature.autoplay.status.cannot-heal"));
            return false;
        }

        Building damagedBuilding = null;
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

        if (damagedBuilding == null && Vars.indexer != null) {
            damagedBuilding = Units.findAllyTile(unit.team, unit.x, unit.y, 800f,
                    b -> b != null && b.damaged() && !(b instanceof ConstructBuild));
        }

        Unit damagedAlly = (hasHealWeapon || hasRepairField)
                ? Units.closest(unit.team, unit.x, unit.y, 400f, u -> u != unit && u.damaged() && !u.dead())
                : null;

        Teamc target = damagedBuilding != null ? damagedBuilding : damagedAlly;

        if (target == null) {
            ai.setTarget(null);
            unit.isShooting(false);
            unit.controlWeapons(false, false);
            status.set(Core.bundle.get("feature.autoplay.status.no-damaged-buildings"));
            return false;
        }

        ai.setTarget(target);
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
            if (target == null || !target.isAdded() || (target instanceof Healthc && ((Healthc) target).dead())) {
                target = null;
                unit.isShooting(false);
                unit.controlWeapons(false, false);
                return;
            }

            boolean hasHealWeapon = false;
            float maxWeaponRange = unit.range() > 0 ? unit.range() : (unit.type != null ? unit.type.range : 0f);
            if (unit.type != null && unit.type.weapons != null) {
                for (int i = 0; i < unit.type.weapons.size; i++) {
                    Weapon w = unit.type.weapons.get(i);
                    if (w != null && ((w.bullet != null && w.bullet.heals()) || w instanceof RepairBeamWeapon)) {
                        hasHealWeapon = true;
                        maxWeaponRange = Math.max(maxWeaponRange, w.range());
                    }
                }
            }

            RepairFieldAbility repairField = null;
            if (unit.type != null && unit.type.abilities != null) {
                for (int i = 0; i < unit.type.abilities.size; i++) {
                    if (unit.type.abilities.get(i) instanceof RepairFieldAbility) {
                        repairField = (RepairFieldAbility) unit.type.abilities.get(i);
                        break;
                    }
                }
            }

            if (target instanceof Building) {
                Building b = (Building) target;
                if (b.health() >= b.maxHealth()) {
                    target = null;
                    unit.isShooting(false);
                    unit.controlWeapons(false, false);
                    return;
                }
                float healRange = hasHealWeapon ? Math.max(maxWeaponRange * 0.7f, 20f) : (repairField != null ? Math.min(repairField.range * 0.8f, 50f) : 30f);
                moveTo(target, healRange, 30f);
                if (hasHealWeapon) {
                    boolean inRange = unit.within(target, Math.max(maxWeaponRange, 20f));
                    unit.lookAt(target);
                    unit.aim(target);
                    unit.controlWeapons(inRange, inRange);
                    unit.isShooting(inRange);
                } else {
                    unit.controlWeapons(false, false);
                    unit.isShooting(false);
                }
            } else if (target instanceof Unit) {
                Unit u = (Unit) target;
                if (u.health() >= u.maxHealth()) {
                    target = null;
                    unit.isShooting(false);
                    unit.controlWeapons(false, false);
                    return;
                }
                float healRange = hasHealWeapon ? Math.max(maxWeaponRange * 0.7f, 20f) : (repairField != null ? Math.min(repairField.range * 0.8f, 50f) : 30f);
                moveTo(target, healRange, 30f);
                if (hasHealWeapon) {
                    boolean inRange = unit.within(target, Math.max(maxWeaponRange, 20f));
                    unit.lookAt(target);
                    unit.aim(target);
                    unit.controlWeapons(inRange, inRange);
                    unit.isShooting(inRange);
                } else {
                    unit.controlWeapons(false, false);
                    unit.isShooting(false);
                }
            }
        }
    }
}
