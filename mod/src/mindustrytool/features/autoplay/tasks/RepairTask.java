package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.type.Weapon;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
import mindustrytool.components.FileIcon;
import solim.reactive.Readable;
import solim.reactive.Signal;

import arc.util.Nullable;
import mindustry.game.Team;

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

    public static boolean isRepairable(@Nullable Building b, @Nullable Team team) {
        return b != null
                && b.isValid()
                && !b.dead()
                && (team == null || b.team == team)
                && !(b instanceof ConstructBuild)
                && b.damaged()
                && b.health() < b.maxHealth() - 0.01f;
    }

    public static boolean canHeal(Unit unit) {
        if (unit == null || unit.type == null) {
            return false;
        }
        return hasHealWeapon(unit);
    }

    public static boolean hasHealWeapon(Unit unit) {
        if (unit == null || unit.type == null || unit.type.weapons == null) {
            return false;
        }
        for (int i = 0; i < unit.type.weapons.size; i++) {
            Weapon w = unit.type.weapons.get(i);
            if (w == null) {
                continue;
            }
            boolean canRepair = w instanceof RepairBeamWeapon
                    ? ((RepairBeamWeapon) w).targetBuildings
                    : (w.bullet != null && w.bullet.heals());
            if (canRepair) {
                return true;
            }
        }
        return false;
    }

    public static float getHealRange(Unit unit) {
        if (unit == null || unit.type == null) {
            return 80f;
        }
        float maxRange = 0f;
        if (unit.type.weapons != null) {
            for (int i = 0; i < unit.type.weapons.size; i++) {
                Weapon w = unit.type.weapons.get(i);
                if (w == null) {
                    continue;
                }
                boolean canRepair = w instanceof RepairBeamWeapon
                        ? ((RepairBeamWeapon) w).targetBuildings
                        : (w.bullet != null && w.bullet.heals());
                if (canRepair) {
                    maxRange = Math.max(maxRange, w.range());
                }
            }
        }
        if (maxRange <= 0f) {
            maxRange = unit.type.range > 0f ? unit.type.range : 80f;
        }
        return maxRange;
    }

    @Override
    public boolean update(Unit unit) {
        if (!canHeal(unit)) {
            status.set(Core.bundle.get("feature.autoplay.status.cannot-heal"));
            return false;
        }

        Building damagedBuilding = Units.findDamagedTile(unit.team, unit.x, unit.y);
        if (!isRepairable(damagedBuilding, unit.team)) {
            damagedBuilding = null;
            if (Vars.indexer != null) {
                Seq<Building> damagedList = Vars.indexer.getDamaged(unit.team);
                if (damagedList != null && !damagedList.isEmpty()) {
                    float minDst = Float.MAX_VALUE;
                    for (int i = 0; i < damagedList.size; i++) {
                        Building b = damagedList.get(i);
                        if (isRepairable(b, unit.team)) {
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
                    b -> isRepairable(b, unit.team));
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
            if (!isRepairable(b, unit.team)) {
                target = null;
                unit.isShooting(false);
                unit.controlWeapons(false, false);
                return;
            }

            float range = getHealRange(unit);
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
