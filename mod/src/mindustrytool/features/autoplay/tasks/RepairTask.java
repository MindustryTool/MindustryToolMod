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
import arc.util.Log;
import arc.util.Time;
import mindustry.game.Team;

public class RepairTask implements AutoplayTask {

    public static final String ID = "repair";

    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final RepairAI ai = new RepairAI();
    private float lastLogTime = -1000f;

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
        boolean shouldLog = Time.time - lastLogTime > 60f;
        if (shouldLog) {
            lastLogTime = Time.time;
        }
        if (!canHeal(unit)) {
            status.set(Core.bundle.get("feature.autoplay.status.cannot-heal"));
            if (shouldLog) {
                Log.info("[RepairTask] cannot-heal unit=@ weapons=@", unit == null ? "null"
                        : unit.type == null ? "no-type" : unit.type.name,
                        unit == null || unit.type == null || unit.type.weapons == null ? -1
                                : unit.type.weapons.size);
            }
            return false;
        }

        Building damagedBuilding = Units.findDamagedTile(unit.team, unit.x, unit.y);
        if (shouldLog) {
            Log.info("[RepairTask] findDamagedTile=@ valid=@ unit=(@,@) team=@", describe(damagedBuilding),
                    damagedBuilding == null ? "n/a" : notRepairableReason(damagedBuilding, unit.team),
                    unit.x, unit.y, unit.team);
        }
        if (!isRepairable(damagedBuilding, unit.team)) {
            damagedBuilding = null;
            if (Vars.indexer != null) {
                Seq<Building> damagedList = Vars.indexer.getDamaged(unit.team);
                if (damagedList != null && !damagedList.isEmpty()) {
                    float minDst = Float.MAX_VALUE;
                    int repairableCount = 0;
                    for (int i = 0; i < damagedList.size; i++) {
                        Building b = damagedList.get(i);
                        if (isRepairable(b, unit.team)) {
                            repairableCount++;
                            float dst = unit.dst2(b);
                            if (dst < minDst) {
                                minDst = dst;
                                damagedBuilding = b;
                            }
                        }
                    }
                    if (shouldLog) {
                        Log.info("[RepairTask] indexer damaged=@ repairable=@ picked=@", damagedList.size,
                                repairableCount, describe(damagedBuilding));
                    }
                } else if (shouldLog) {
                    Log.info("[RepairTask] indexer damaged=@ (empty or null)",
                            damagedList == null ? "null" : damagedList.size);
                }
            } else if (shouldLog) {
                Log.info("[RepairTask] indexer is null");
            }
        }

        if (damagedBuilding == null && Vars.indexer != null) {
            damagedBuilding = Units.findAllyTile(unit.team, unit.x, unit.y, 800f,
                    b -> isRepairable(b, unit.team));
            if (shouldLog) {
                Log.info("[RepairTask] findAllyTile(800f)=@", describe(damagedBuilding));
            }
        }

        if (damagedBuilding == null) {
            ai.setTarget(null);
            unit.isShooting(false);
            unit.controlWeapons(false, false);
            status.set(Core.bundle.get("feature.autoplay.status.no-damaged-buildings"));
            if (shouldLog) {
                Log.info("[RepairTask] no target, idle");
            }
            return false;
        }

        if (shouldLog) {
            Log.info("[RepairTask] repairing target=@ hp=@/@ range=@ dist=@", describe(damagedBuilding),
                    damagedBuilding.health, damagedBuilding.maxHealth, getHealRange(unit),
                    unit.dst(damagedBuilding));
        }
        ai.setTarget(damagedBuilding);
        status.set(Core.bundle.get("feature.autoplay.status.repairing"));
        return true;
    }

    @Override
    public BaseAutoplayAI getAI() {
        return ai;
    }

    private static String describe(@Nullable Building b) {
        return b == null ? "null"
                : b.block.name + "@(" + b.tileX() + "," + b.tileY() + ") hp=" + b.health + "/" + b.maxHealth;
    }

    private static String notRepairableReason(@Nullable Building b, @Nullable Team team) {
        if (b == null) {
            return "null";
        }
        if (!b.isValid()) {
            return "invalid";
        }
        if (b.dead()) {
            return "dead";
        }
        if (team != null && b.team != team) {
            return "wrong-team";
        }
        if (b instanceof ConstructBuild) {
            return "construct-block";
        }
        if (!b.damaged()) {
            return "not-damaged";
        }
        return "hp=" + b.health + "/" + b.maxHealth;
    }

    public static class RepairAI extends BaseAutoplayAI {
        private float lastLogTime = -1000f;

        @Override
        public void updateMovement() {
            boolean shouldLog = Time.time - lastLogTime > 60f;
            if (shouldLog) {
                lastLogTime = Time.time;
            }
            if (target == null || !target.isAdded() || !(target instanceof Building)) {
                if (shouldLog) {
                    Log.info("[RepairAI] target lost: null=@ added=@ isBuilding=@", target == null,
                            target == null ? "n/a" : target.isAdded(), target instanceof Building);
                }
                target = null;
                unit.isShooting(false);
                unit.controlWeapons(false, false);
                return;
            }

            Building b = (Building) target;
            if (!isRepairable(b, unit.team)) {
                if (shouldLog) {
                    Log.info("[RepairAI] target no longer repairable: @ reason=@", describe(b),
                            notRepairableReason(b, unit.team));
                }
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
            boolean healWeapon = hasHealWeapon(unit);
            if (shouldLog) {
                Log.info("[RepairAI] target=@ dist=@ range=@ approach=@ inRange=@ healWeapon=@ shooting=@",
                        describe(b), unit.dst(target), range, approachRange, inRange, healWeapon,
                        unit.isShooting());
            }
            if (inRange && healWeapon) {
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
