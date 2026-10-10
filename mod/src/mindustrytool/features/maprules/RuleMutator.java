package mindustrytool.features.maprules;

import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.Rules;
import mindustry.gen.Call;

/**
 * Applies rule changes according to the current edit mode.
 * Continuous controls (sliders) are debounced so dragging applies once the
 * value settles instead of spamming mutations or network broadcasts.
 */
public class RuleMutator {

    private static final float DEBOUNCE_SECONDS = 0.4f;

    private final ObjectMap<String, Timer.Task> pendingTasks = new ObjectMap<>();
    private final ObjectMap<String, Runnable> pendingActions = new ObjectMap<>();
    private @Nullable Runnable resyncHandler;

    public void setResyncHandler(@Nullable Runnable handler) {
        this.resyncHandler = handler;
    }

    public void scheduleResync() {
        if (resyncHandler == null) {
            return;
        }
        float[] delays = new float[]{0.5f, 1.5f, 3f};
        for (int i = 0; i < delays.length; i++) {
            final float delay = delays[i];
            Timer.schedule(() -> {
                if (resyncHandler != null) {
                    resyncHandler.run();
                }
            }, delay);
        }
    }

    public @Nullable Rules currentRules() {
        return Vars.state != null ? Vars.state.rules : null;
    }

    public void mutate(Cons<Rules> change) {
        mutate(change, true);
    }

    public void mutateVisual(Cons<Rules> change) {
        Rules rules = currentRules();
        if (rules == null || change == null) {
            return;
        }
        try {
            change.get(rules);
        } catch (Exception e) {
            Log.err("MapRules: failed to apply visual override", e);
        }
    }

    public void mutate(Cons<Rules> change, boolean broadcast) {
        Rules rules = currentRules();
        if (rules == null || change == null) {
            return;
        }
        RuleEditMode mode = RuleEditMode.resolve();
        if (mode == RuleEditMode.CLIENT_READONLY) {
            return;
        }
        if (mode == RuleEditMode.CLIENT_ADMIN) {
            return;
        }
        try {
            change.get(rules);
        } catch (Exception e) {
            Log.err("MapRules: failed to mutate rules", e);
            return;
        }
        if (broadcast && mode == RuleEditMode.HOST) {
            try {
                Call.setRules(rules);
            } catch (Exception e) {
                Log.err("MapRules: failed to broadcast rules", e);
            }
        }
    }

    public void mutateRemote(String jsStatement) {
        if (jsStatement == null || jsStatement.trim().isEmpty()) {
            return;
        }
        try {
            Call.sendChatMessage("/js " + Strings.format(jsStatement, new Object[0]));
        } catch (Exception e) {
            Log.err("MapRules: failed to send remote rule change", e);
        }
    }

    public void setFloat(String field, float value) {
        RuleEditMode mode = RuleEditMode.resolve();
        if (mode == RuleEditMode.CLIENT_READONLY) {
            return;
        }
        if (mode == RuleEditMode.CLIENT_ADMIN) {
            final float next = value;
            debounce("js:" + field, () -> {
                mutateVisual(rules -> setFloatField(rules, field, next));
                mutateRemote("Vars.state.rules." + field + " = " + next + "; Call.setRules(Vars.state.rules);");
                scheduleResync();
            });
            return;
        }
        final float next = value;
        debounce(field, () -> mutate(rules -> setFloatField(rules, field, next)));
    }

    public void setInt(String field, int value) {
        RuleEditMode mode = RuleEditMode.resolve();
        if (mode == RuleEditMode.CLIENT_READONLY) {
            return;
        }
        if (mode == RuleEditMode.CLIENT_ADMIN) {
            final int next = value;
            debounce("js:" + field, () -> {
                mutateVisual(rules -> setIntField(rules, field, next));
                mutateRemote("Vars.state.rules." + field + " = " + next + "; Call.setRules(Vars.state.rules);");
                scheduleResync();
            });
            return;
        }
        final int next = value;
        debounce(field, () -> mutate(rules -> setIntField(rules, field, next)));
    }

    private void debounce(String key, Runnable action) {
        Timer.Task old = pendingTasks.remove(key);
        if (old != null) {
            old.cancel();
        }
        pendingActions.put(key, action);
        pendingTasks.put(key, Timer.schedule(() -> {
            pendingTasks.remove(key);
            Runnable run = pendingActions.remove(key);
            if (run != null) {
                run.run();
            }
        }, DEBOUNCE_SECONDS));
    }

    public void flushPending() {
        Seq<String> keys = new Seq<>();
        for (String key : pendingTasks.keys()) {
            keys.add(key);
        }
        for (int i = 0; i < keys.size; i++) {
            String key = keys.get(i);
            Timer.Task task = pendingTasks.remove(key);
            if (task != null) {
                task.cancel();
            }
            Runnable run = pendingActions.remove(key);
            if (run != null) {
                run.run();
            }
        }
    }

    public void setBool(String field, boolean value) {
        RuleEditMode mode = RuleEditMode.resolve();
        if (mode == RuleEditMode.CLIENT_ADMIN) {
            mutateVisual(rules -> setBoolField(rules, field, value));
            mutateRemote("Vars.state.rules." + field + " = " + value + "; Call.setRules(Vars.state.rules);");
            scheduleResync();
            return;
        }
        final boolean next = value;
        mutate(rules -> setBoolField(rules, field, next));
    }

    public void setVisualBool(String field, boolean value) {
        final boolean next = value;
        mutateVisual(rules -> setBoolField(rules, field, next));
    }

    private void setFloatField(Rules rules, String field, float value) {
        if ("buildSpeedMultiplier".equals(field)) {
            rules.buildSpeedMultiplier = value;
        } else if ("buildCostMultiplier".equals(field)) {
            rules.buildCostMultiplier = value;
        } else if ("blockHealthMultiplier".equals(field)) {
            rules.blockHealthMultiplier = value;
        } else if ("blockDamageMultiplier".equals(field)) {
            rules.blockDamageMultiplier = value;
        } else if ("unitDamageMultiplier".equals(field)) {
            rules.unitDamageMultiplier = value;
        } else if ("unitHealthMultiplier".equals(field)) {
            rules.unitHealthMultiplier = value;
        } else if ("unitBuildSpeedMultiplier".equals(field)) {
            rules.unitBuildSpeedMultiplier = value;
        } else if ("unitCostMultiplier".equals(field)) {
            rules.unitCostMultiplier = value;
        } else if ("solarMultiplier".equals(field)) {
            rules.solarMultiplier = value;
        } else if ("unitMineSpeedMultiplier".equals(field)) {
            rules.unitMineSpeedMultiplier = value;
        } else if ("unitCrashDamageMultiplier".equals(field)) {
            rules.unitCrashDamageMultiplier = value;
        } else if ("deconstructRefundMultiplier".equals(field)) {
            rules.deconstructRefundMultiplier = value;
        } else if ("itemDepositCooldown".equals(field)) {
            rules.itemDepositCooldown = value;
        } else if ("dropZoneRadius".equals(field)) {
            rules.dropZoneRadius = value;
        } else if ("waveSpacing".equals(field)) {
            rules.waveSpacing = value;
        }
    }

    private void setIntField(Rules rules, String field, int value) {
        if ("winWave".equals(field)) {
            rules.winWave = value;
        } else if ("unitCap".equals(field)) {
            rules.unitCap = value;
        }
    }

    private void setBoolField(Rules rules, String field, boolean value) {
        if ("fog".equals(field)) {
            rules.fog = value;
        } else if ("lighting".equals(field)) {
            rules.lighting = value;
        } else if ("borderDarkness".equals(field)) {
            rules.borderDarkness = value;
        } else if ("reactorExplosions".equals(field)) {
            rules.reactorExplosions = value;
        } else if ("fire".equals(field)) {
            rules.fire = value;
        } else if ("possessionAllowed".equals(field)) {
            rules.possessionAllowed = value;
        } else if ("schematicsAllowed".equals(field)) {
            rules.schematicsAllowed = value;
        } else if ("infiniteResources".equals(field)) {
            rules.infiniteResources = value;
        } else if ("waves".equals(field)) {
            rules.waves = value;
        } else if ("waveTimer".equals(field)) {
            rules.waveTimer = value;
        } else if ("waveSending".equals(field)) {
            rules.waveSending = value;
        } else if ("waitEnemies".equals(field)) {
            rules.waitEnemies = value;
        } else if ("wavesSpawnAtCores".equals(field)) {
            rules.wavesSpawnAtCores = value;
        } else if ("hideSpawns".equals(field)) {
            rules.hideSpawns = value;
        } else if ("randomWaveAI".equals(field)) {
            rules.randomWaveAI = value;
        } else if ("airUseSpawns".equals(field)) {
            rules.airUseSpawns = value;
        } else if ("staticFog".equals(field)) {
            rules.staticFog = value;
        } else if ("unitLight".equals(field)) {
            rules.unitLight = value;
        } else if ("pvp".equals(field)) {
            rules.pvp = value;
        } else if ("pvpAutoPause".equals(field)) {
            rules.pvpAutoPause = value;
        } else if ("attackMode".equals(field)) {
            rules.attackMode = value;
        } else if ("coreCapture".equals(field)) {
            rules.coreCapture = value;
        } else if ("coreIncinerates".equals(field)) {
            rules.coreIncinerates = value;
        } else if ("canGameOver".equals(field)) {
            rules.canGameOver = value;
        } else if ("pauseDisabled".equals(field)) {
            rules.pauseDisabled = value;
        } else if ("damageExplosions".equals(field)) {
            rules.damageExplosions = value;
        } else if ("derelictRepair".equals(field)) {
            rules.derelictRepair = value;
        } else if ("placeRangeCheck".equals(field)) {
            rules.placeRangeCheck = value;
        } else if ("polygonCoreProtection".equals(field)) {
            rules.polygonCoreProtection = value;
        } else if ("cleanupDeadTeams".equals(field)) {
            rules.cleanupDeadTeams = value;
        } else if ("onlyDepositCore".equals(field)) {
            rules.onlyDepositCore = value;
        } else if ("allowCoreUnloaders".equals(field)) {
            rules.allowCoreUnloaders = value;
        } else if ("coreDestroyClear".equals(field)) {
            rules.coreDestroyClear = value;
        } else if ("hideBannedBlocks".equals(field)) {
            rules.hideBannedBlocks = value;
        } else if ("allowEnvironmentDeconstruct".equals(field)) {
            rules.allowEnvironmentDeconstruct = value;
        } else if ("instantBuild".equals(field)) {
            rules.instantBuild = value;
        } else if ("ghostBlocks".equals(field)) {
            rules.ghostBlocks = value;
        } else if ("logicUnitControl".equals(field)) {
            rules.logicUnitControl = value;
        } else if ("unitPayloadUpdate".equals(field)) {
            rules.unitPayloadUpdate = value;
        } else if ("showOtherTeamPings".equals(field)) {
            rules.showOtherTeamPings = value;
        }
    }
}
