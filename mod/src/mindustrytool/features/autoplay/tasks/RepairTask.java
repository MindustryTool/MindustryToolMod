package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import mindustry.entities.Units;
import mindustry.entities.abilities.RepairFieldAbility;
import mindustry.gen.Building;
import mindustry.gen.Healthc;
import mindustry.gen.Icon;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
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
        return Icon.hammer;
    }

    @Override
    public Readable<String> status() {
        return status;
    }

    @Override
    public boolean update(Unit unit) {
        boolean hasHealWeapon = unit.type.weapons.contains(w -> w.bullet.heals() || w instanceof RepairBeamWeapon);
        boolean hasRepairField = false;
        for (int i = 0; i < unit.type.abilities.size; i++) {
            if (unit.type.abilities.get(i) instanceof RepairFieldAbility) {
                hasRepairField = true;
                break;
            }
        }
        boolean canBuild = unit.canBuild();

        if (!hasHealWeapon && !hasRepairField && !canBuild) {
            status.set(Core.bundle.get("feature.autoplay.status.cannot-heal"));
            return false;
        }

        Building damagedBuilding = Units.findDamagedTile(unit.team, unit.x, unit.y);
        if (damagedBuilding instanceof ConstructBuild) {
            damagedBuilding = null;
        }

        Unit damagedAlly = (hasHealWeapon || hasRepairField)
                ? Units.closest(unit.team, unit.x, unit.y, 400f, u -> u != unit && u.damaged() && !u.dead())
                : null;

        Teamc target = damagedBuilding != null ? damagedBuilding : damagedAlly;

        if (target == null) {
            ai.setTarget(null);
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
                return;
            }

            if (target instanceof Building) {
                Building b = (Building) target;
                if (b.health() >= b.maxHealth()) {
                    target = null;
                    return;
                }
                float range = unit.canBuild() ? Math.min(unit.type.buildRange - 10f, 80f) : unit.type.range * 0.7f;
                moveTo(target, Math.max(range, 20f));
                unit.lookAt(target);
                unit.aim(target);
                unit.controlWeapons(unit.within(target, unit.type.range));
            } else if (target instanceof Unit) {
                Unit u = (Unit) target;
                if (u.health() >= u.maxHealth()) {
                    target = null;
                    return;
                }
                moveTo(target, 40f);
                unit.lookAt(target);
                unit.aim(target);
                unit.controlWeapons(unit.within(target, unit.type.range));
            }
        }
    }
}
