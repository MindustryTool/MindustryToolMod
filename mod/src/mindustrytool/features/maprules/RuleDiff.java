package mindustrytool.features.maprules;

import arc.struct.ObjectSet;
import arc.util.Nullable;
import mindustry.game.Rules;
import mindustry.world.Block;
import mindustry.type.UnitType;

/**
 * Compares active rules against a default baseline.
 */
public class RuleDiff {

    private static final float EPSILON = 0.0001f;

    private final Rules baseline;

    public RuleDiff() {
        this(new Rules());
    }

    public RuleDiff(Rules baseline) {
        this.baseline = baseline != null ? baseline : new Rules();
    }

    public Rules baseline() {
        return baseline;
    }

    private static boolean differs(float a, float b) {
        return Math.abs(a - b) > EPSILON;
    }

    private static boolean differs(int a, int b) {
        return a != b;
    }

    private static boolean differs(boolean a, boolean b) {
        return a != b;
    }

    public boolean buildSpeed(@Nullable Rules rules) {
        return rules != null && differs(rules.buildSpeedMultiplier, baseline.buildSpeedMultiplier);
    }

    public boolean buildCost(@Nullable Rules rules) {
        return rules != null && differs(rules.buildCostMultiplier, baseline.buildCostMultiplier);
    }

    public boolean blockHealth(@Nullable Rules rules) {
        return rules != null && differs(rules.blockHealthMultiplier, baseline.blockHealthMultiplier);
    }

    public boolean blockDamage(@Nullable Rules rules) {
        return rules != null && differs(rules.blockDamageMultiplier, baseline.blockDamageMultiplier);
    }

    public boolean unitDamage(@Nullable Rules rules) {
        return rules != null && differs(rules.unitDamageMultiplier, baseline.unitDamageMultiplier);
    }

    public boolean unitHealth(@Nullable Rules rules) {
        return rules != null && differs(rules.unitHealthMultiplier, baseline.unitHealthMultiplier);
    }

    public boolean unitBuildSpeed(@Nullable Rules rules) {
        return rules != null && differs(rules.unitBuildSpeedMultiplier, baseline.unitBuildSpeedMultiplier);
    }

    public boolean unitCost(@Nullable Rules rules) {
        return rules != null && differs(rules.unitCostMultiplier, baseline.unitCostMultiplier);
    }

    public boolean solar(@Nullable Rules rules) {
        return rules != null && differs(rules.solarMultiplier, baseline.solarMultiplier);
    }

    public boolean waveSpacing(@Nullable Rules rules) {
        return rules != null && differs(rules.waveSpacing, baseline.waveSpacing);
    }

    public boolean winWave(@Nullable Rules rules) {
        return rules != null && differs(rules.winWave, baseline.winWave);
    }

    public boolean unitCap(@Nullable Rules rules) {
        return rules != null && differs(rules.unitCap, baseline.unitCap);
    }

    public boolean fog(@Nullable Rules rules) {
        return rules != null && differs(rules.fog, baseline.fog);
    }

    public boolean lighting(@Nullable Rules rules) {
        return rules != null && differs(rules.lighting, baseline.lighting);
    }

    public boolean borderDarkness(@Nullable Rules rules) {
        return rules != null && differs(rules.borderDarkness, baseline.borderDarkness);
    }

    public boolean reactorExplosions(@Nullable Rules rules) {
        return rules != null && differs(rules.reactorExplosions, baseline.reactorExplosions);
    }

    public boolean fire(@Nullable Rules rules) {
        return rules != null && differs(rules.fire, baseline.fire);
    }

    public boolean possession(@Nullable Rules rules) {
        return rules != null && differs(rules.possessionAllowed, baseline.possessionAllowed);
    }

    public boolean schematics(@Nullable Rules rules) {
        return rules != null && differs(rules.schematicsAllowed, baseline.schematicsAllowed);
    }

    public boolean infiniteResources(@Nullable Rules rules) {
        return rules != null && differs(rules.infiniteResources, baseline.infiniteResources);
    }

    public boolean bannedBlocks(@Nullable Rules rules) {
        if (rules == null) {
            return false;
        }
        ObjectSet<Block> current = rules.bannedBlocks;
        ObjectSet<Block> def = baseline.bannedBlocks;
        if (current == null) {
            return def != null && def.size > 0;
        }
        if (def == null) {
            return current.size > 0;
        }
        return current.size != def.size;
    }

    public boolean bannedUnits(@Nullable Rules rules) {
        if (rules == null) {
            return false;
        }
        ObjectSet<UnitType> current = rules.bannedUnits;
        ObjectSet<UnitType> def = baseline.bannedUnits;
        if (current == null) {
            return def != null && def.size > 0;
        }
        if (def == null) {
            return current.size > 0;
        }
        return current.size != def.size;
    }

    public boolean unitMineSpeed(@Nullable Rules rules) {
        return rules != null && differs(rules.unitMineSpeedMultiplier, baseline.unitMineSpeedMultiplier);
    }

    public boolean unitCrashDamage(@Nullable Rules rules) {
        return rules != null && differs(rules.unitCrashDamageMultiplier, baseline.unitCrashDamageMultiplier);
    }

    public boolean deconstructRefund(@Nullable Rules rules) {
        return rules != null && differs(rules.deconstructRefundMultiplier, baseline.deconstructRefundMultiplier);
    }

    public boolean itemDepositCooldown(@Nullable Rules rules) {
        return rules != null && differs(rules.itemDepositCooldown, baseline.itemDepositCooldown);
    }

    public boolean waves(@Nullable Rules rules) {
        return rules != null && differs(rules.waves, baseline.waves);
    }

    public boolean waveTimer(@Nullable Rules rules) {
        return rules != null && differs(rules.waveTimer, baseline.waveTimer);
    }

    public boolean waveSending(@Nullable Rules rules) {
        return rules != null && differs(rules.waveSending, baseline.waveSending);
    }

    public boolean waitEnemies(@Nullable Rules rules) {
        return rules != null && differs(rules.waitEnemies, baseline.waitEnemies);
    }

    public boolean wavesSpawnAtCores(@Nullable Rules rules) {
        return rules != null && differs(rules.wavesSpawnAtCores, baseline.wavesSpawnAtCores);
    }

    public boolean hideSpawns(@Nullable Rules rules) {
        return rules != null && differs(rules.hideSpawns, baseline.hideSpawns);
    }

    public boolean randomWaveAI(@Nullable Rules rules) {
        return rules != null && differs(rules.randomWaveAI, baseline.randomWaveAI);
    }

    public boolean airUseSpawns(@Nullable Rules rules) {
        return rules != null && differs(rules.airUseSpawns, baseline.airUseSpawns);
    }

    public boolean dropZoneRadius(@Nullable Rules rules) {
        return rules != null && differs(rules.dropZoneRadius, baseline.dropZoneRadius);
    }

    public boolean staticFog(@Nullable Rules rules) {
        return rules != null && differs(rules.staticFog, baseline.staticFog);
    }

    public boolean unitLight(@Nullable Rules rules) {
        return rules != null && differs(rules.unitLight, baseline.unitLight);
    }

    public boolean pvp(@Nullable Rules rules) {
        return rules != null && differs(rules.pvp, baseline.pvp);
    }

    public boolean pvpAutoPause(@Nullable Rules rules) {
        return rules != null && differs(rules.pvpAutoPause, baseline.pvpAutoPause);
    }

    public boolean attackMode(@Nullable Rules rules) {
        return rules != null && differs(rules.attackMode, baseline.attackMode);
    }

    public boolean coreCapture(@Nullable Rules rules) {
        return rules != null && differs(rules.coreCapture, baseline.coreCapture);
    }

    public boolean coreIncinerates(@Nullable Rules rules) {
        return rules != null && differs(rules.coreIncinerates, baseline.coreIncinerates);
    }

    public boolean canGameOver(@Nullable Rules rules) {
        return rules != null && differs(rules.canGameOver, baseline.canGameOver);
    }

    public boolean pauseDisabled(@Nullable Rules rules) {
        return rules != null && differs(rules.pauseDisabled, baseline.pauseDisabled);
    }

    public boolean damageExplosions(@Nullable Rules rules) {
        return rules != null && differs(rules.damageExplosions, baseline.damageExplosions);
    }

    public boolean derelictRepair(@Nullable Rules rules) {
        return rules != null && differs(rules.derelictRepair, baseline.derelictRepair);
    }

    public boolean placeRangeCheck(@Nullable Rules rules) {
        return rules != null && differs(rules.placeRangeCheck, baseline.placeRangeCheck);
    }

    public boolean coreProtection(@Nullable Rules rules) {
        return rules != null && differs(rules.polygonCoreProtection, baseline.polygonCoreProtection);
    }

    public boolean cleanupDeadTeams(@Nullable Rules rules) {
        return rules != null && differs(rules.cleanupDeadTeams, baseline.cleanupDeadTeams);
    }

    public boolean onlyDepositCore(@Nullable Rules rules) {
        return rules != null && differs(rules.onlyDepositCore, baseline.onlyDepositCore);
    }

    public boolean allowCoreUnloaders(@Nullable Rules rules) {
        return rules != null && differs(rules.allowCoreUnloaders, baseline.allowCoreUnloaders);
    }

    public boolean coreDestroyClear(@Nullable Rules rules) {
        return rules != null && differs(rules.coreDestroyClear, baseline.coreDestroyClear);
    }

    public boolean hideBannedBlocks(@Nullable Rules rules) {
        return rules != null && differs(rules.hideBannedBlocks, baseline.hideBannedBlocks);
    }

    public boolean allowEnvDeconstruct(@Nullable Rules rules) {
        return rules != null && differs(rules.allowEnvironmentDeconstruct, baseline.allowEnvironmentDeconstruct);
    }

    public boolean instantBuild(@Nullable Rules rules) {
        return rules != null && differs(rules.instantBuild, baseline.instantBuild);
    }

    public boolean ghostBlocks(@Nullable Rules rules) {
        return rules != null && differs(rules.ghostBlocks, baseline.ghostBlocks);
    }

    public boolean logicUnitControl(@Nullable Rules rules) {
        return rules != null && differs(rules.logicUnitControl, baseline.logicUnitControl);
    }

    public boolean unitPayloadUpdate(@Nullable Rules rules) {
        return rules != null && differs(rules.unitPayloadUpdate, baseline.unitPayloadUpdate);
    }

    public boolean showTeamPings(@Nullable Rules rules) {
        return rules != null && differs(rules.showOtherTeamPings, baseline.showOtherTeamPings);
    }

    public boolean anyModified(@Nullable Rules rules) {
        return rules != null && (buildSpeed(rules) || buildCost(rules) || blockHealth(rules)
                || blockDamage(rules) || unitDamage(rules) || unitHealth(rules)
                || unitBuildSpeed(rules) || unitCost(rules) || solar(rules)
                || unitMineSpeed(rules) || unitCrashDamage(rules) || deconstructRefund(rules)
                || itemDepositCooldown(rules)
                || waveSpacing(rules) || winWave(rules) || unitCap(rules)
                || waves(rules) || waveTimer(rules) || waveSending(rules) || waitEnemies(rules)
                || wavesSpawnAtCores(rules) || hideSpawns(rules) || randomWaveAI(rules)
                || airUseSpawns(rules) || dropZoneRadius(rules)
                || fog(rules) || lighting(rules) || borderDarkness(rules)
                || staticFog(rules) || unitLight(rules)
                || reactorExplosions(rules) || fire(rules) || possession(rules)
                || schematics(rules) || infiniteResources(rules)
                || pvp(rules) || pvpAutoPause(rules) || attackMode(rules) || coreCapture(rules)
                || coreIncinerates(rules) || canGameOver(rules) || pauseDisabled(rules)
                || damageExplosions(rules) || derelictRepair(rules) || placeRangeCheck(rules)
                || coreProtection(rules) || cleanupDeadTeams(rules) || onlyDepositCore(rules)
                || allowCoreUnloaders(rules) || coreDestroyClear(rules) || hideBannedBlocks(rules)
                || allowEnvDeconstruct(rules) || instantBuild(rules) || ghostBlocks(rules)
                || logicUnitControl(rules) || unitPayloadUpdate(rules) || showTeamPings(rules)
                || bannedBlocks(rules) || bannedUnits(rules));
    }

    public int modifiedCount(@Nullable Rules rules) {
        if (rules == null) {
            return 0;
        }
        int count = 0;
        if (buildSpeed(rules)) {
            count++;
        }
        if (buildCost(rules)) {
            count++;
        }
        if (blockHealth(rules)) {
            count++;
        }
        if (blockDamage(rules)) {
            count++;
        }
        if (unitDamage(rules)) {
            count++;
        }
        if (unitHealth(rules)) {
            count++;
        }
        if (unitBuildSpeed(rules)) {
            count++;
        }
        if (unitCost(rules)) {
            count++;
        }
        if (solar(rules)) {
            count++;
        }
        if (unitMineSpeed(rules)) {
            count++;
        }
        if (unitCrashDamage(rules)) {
            count++;
        }
        if (deconstructRefund(rules)) {
            count++;
        }
        if (itemDepositCooldown(rules)) {
            count++;
        }
        if (waveSpacing(rules)) {
            count++;
        }
        if (winWave(rules)) {
            count++;
        }
        if (unitCap(rules)) {
            count++;
        }
        if (waves(rules)) {
            count++;
        }
        if (waveTimer(rules)) {
            count++;
        }
        if (waveSending(rules)) {
            count++;
        }
        if (waitEnemies(rules)) {
            count++;
        }
        if (wavesSpawnAtCores(rules)) {
            count++;
        }
        if (hideSpawns(rules)) {
            count++;
        }
        if (randomWaveAI(rules)) {
            count++;
        }
        if (airUseSpawns(rules)) {
            count++;
        }
        if (dropZoneRadius(rules)) {
            count++;
        }
        if (fog(rules)) {
            count++;
        }
        if (lighting(rules)) {
            count++;
        }
        if (borderDarkness(rules)) {
            count++;
        }
        if (staticFog(rules)) {
            count++;
        }
        if (unitLight(rules)) {
            count++;
        }
        if (reactorExplosions(rules)) {
            count++;
        }
        if (fire(rules)) {
            count++;
        }
        if (possession(rules)) {
            count++;
        }
        if (schematics(rules)) {
            count++;
        }
        if (infiniteResources(rules)) {
            count++;
        }
        if (pvp(rules)) {
            count++;
        }
        if (pvpAutoPause(rules)) {
            count++;
        }
        if (attackMode(rules)) {
            count++;
        }
        if (coreCapture(rules)) {
            count++;
        }
        if (coreIncinerates(rules)) {
            count++;
        }
        if (canGameOver(rules)) {
            count++;
        }
        if (pauseDisabled(rules)) {
            count++;
        }
        if (damageExplosions(rules)) {
            count++;
        }
        if (derelictRepair(rules)) {
            count++;
        }
        if (placeRangeCheck(rules)) {
            count++;
        }
        if (coreProtection(rules)) {
            count++;
        }
        if (cleanupDeadTeams(rules)) {
            count++;
        }
        if (onlyDepositCore(rules)) {
            count++;
        }
        if (allowCoreUnloaders(rules)) {
            count++;
        }
        if (coreDestroyClear(rules)) {
            count++;
        }
        if (hideBannedBlocks(rules)) {
            count++;
        }
        if (allowEnvDeconstruct(rules)) {
            count++;
        }
        if (instantBuild(rules)) {
            count++;
        }
        if (ghostBlocks(rules)) {
            count++;
        }
        if (logicUnitControl(rules)) {
            count++;
        }
        if (unitPayloadUpdate(rules)) {
            count++;
        }
        if (showTeamPings(rules)) {
            count++;
        }
        if (bannedBlocks(rules)) {
            count++;
        }
        if (bannedUnits(rules)) {
            count++;
        }
        return count;
    }
}
