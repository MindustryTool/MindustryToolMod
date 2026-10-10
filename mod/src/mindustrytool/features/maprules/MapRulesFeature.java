package mindustrytool.features.maprules;

import arc.func.Prov;
import arc.input.KeyCode;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.Rules;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

/**
 * Inspects and edits active map rules with role-based permissions.
 */
public class MapRulesFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Boolean> showPermissionConfig;
    public final ConfigValue<Boolean> showDescriptionsConfig;

    public final RuleDiff diff = new RuleDiff();
    public final RuleMutator mutator = new RuleMutator();
    public final MapSaveService saveService = new MapSaveService();

    private @Nullable Rules snapshot;
    private @Nullable MapRulesDialog mainDialog;
    private @Nullable MapRulesSettingsDialog settingsDialog;

    public MapRulesFeature() {
        super(FeatureMetadata.builder()
                .id("map-rules")
                .icon(Icon.map)
                .order(6)
                .enabledByDefault(false)
                .quickAccess(true)
                .build());

        config = configGroup();
        showPermissionConfig = config.boolValue("show-permission", true);
        showDescriptionsConfig = config.boolValue("show-descriptions", true);

        bindDialog("mapRules", KeyCode.unset, (Prov<SolimDialog>) this::getMainDialogInstance, false);
        bindDialog("mapRulesSettings", KeyCode.unset, getSettingDialog(), false);
    }

    private SolimDialog getMainDialogInstance() {
        if (mainDialog == null) {
            mainDialog = new MapRulesDialog(this);
        }
        return mainDialog;
    }

    @Override
    public void onQuickAccessClick() {
        SolimDialog dialog = getMainDialogInstance();
        captureSnapshot();
        dialog.show();
    }

    @Override
    public void onQuickAccessLongClick() {
        Prov<SolimDialog> dlg = getSettingDialog();
        if (dlg != null) {
            dlg.get().show();
        }
    }

    public void captureSnapshot() {
        try {
            Rules rules = Vars.state != null ? Vars.state.rules : null;
            snapshot = rules != null ? rules.copy() : null;
            if (snapshot == null) {
                return;
            }
            Rules defaults = new Rules();
            snapshot.retainContentFields(defaults);
        } catch (Exception ignored) {
            snapshot = null;
        }
    }

    public void resetToDefaults() {
        Rules rules = Vars.state != null ? Vars.state.rules : null;
        if (rules == null) {
            return;
        }
        RuleEditMode mode = RuleEditMode.resolve();
        if (mode == RuleEditMode.CLIENT_READONLY) {
            return;
        }
        if (snapshot != null) {
            final Rules target = snapshot.copy();
            if (mode == RuleEditMode.CLIENT_ADMIN) {
                mutator.mutateRemote("Vars.state.rules = Vars.state.map.applyRules(Vars.state.rules.mode()); Call.setRules(Vars.state.rules);");
                return;
            }
            mutator.mutate(current -> {
                current.buildSpeedMultiplier = target.buildSpeedMultiplier;
                current.buildCostMultiplier = target.buildCostMultiplier;
                current.blockHealthMultiplier = target.blockHealthMultiplier;
                current.blockDamageMultiplier = target.blockDamageMultiplier;
                current.unitDamageMultiplier = target.unitDamageMultiplier;
                current.unitHealthMultiplier = target.unitHealthMultiplier;
                current.unitBuildSpeedMultiplier = target.unitBuildSpeedMultiplier;
                current.unitCostMultiplier = target.unitCostMultiplier;
                current.solarMultiplier = target.solarMultiplier;
                current.unitMineSpeedMultiplier = target.unitMineSpeedMultiplier;
                current.unitCrashDamageMultiplier = target.unitCrashDamageMultiplier;
                current.deconstructRefundMultiplier = target.deconstructRefundMultiplier;
                current.itemDepositCooldown = target.itemDepositCooldown;
                current.dropZoneRadius = target.dropZoneRadius;
                current.waveSpacing = target.waveSpacing;
                current.winWave = target.winWave;
                current.unitCap = target.unitCap;
                current.waves = target.waves;
                current.waveTimer = target.waveTimer;
                current.waveSending = target.waveSending;
                current.waitEnemies = target.waitEnemies;
                current.wavesSpawnAtCores = target.wavesSpawnAtCores;
                current.hideSpawns = target.hideSpawns;
                current.randomWaveAI = target.randomWaveAI;
                current.airUseSpawns = target.airUseSpawns;
                current.fog = target.fog;
                current.lighting = target.lighting;
                current.borderDarkness = target.borderDarkness;
                current.staticFog = target.staticFog;
                current.unitLight = target.unitLight;
                current.reactorExplosions = target.reactorExplosions;
                current.fire = target.fire;
                current.possessionAllowed = target.possessionAllowed;
                current.schematicsAllowed = target.schematicsAllowed;
                current.infiniteResources = target.infiniteResources;
                current.pvp = target.pvp;
                current.pvpAutoPause = target.pvpAutoPause;
                current.attackMode = target.attackMode;
                current.coreCapture = target.coreCapture;
                current.coreIncinerates = target.coreIncinerates;
                current.canGameOver = target.canGameOver;
                current.pauseDisabled = target.pauseDisabled;
                current.damageExplosions = target.damageExplosions;
                current.derelictRepair = target.derelictRepair;
                current.placeRangeCheck = target.placeRangeCheck;
                current.polygonCoreProtection = target.polygonCoreProtection;
                current.cleanupDeadTeams = target.cleanupDeadTeams;
                current.onlyDepositCore = target.onlyDepositCore;
                current.allowCoreUnloaders = target.allowCoreUnloaders;
                current.coreDestroyClear = target.coreDestroyClear;
                current.hideBannedBlocks = target.hideBannedBlocks;
                current.allowEnvironmentDeconstruct = target.allowEnvironmentDeconstruct;
                current.instantBuild = target.instantBuild;
                current.ghostBlocks = target.ghostBlocks;
                current.logicUnitControl = target.logicUnitControl;
                current.unitPayloadUpdate = target.unitPayloadUpdate;
                current.showOtherTeamPings = target.showOtherTeamPings;
                current.bannedBlocks.clear();
                current.bannedBlocks.addAll(target.bannedBlocks);
                current.bannedUnits.clear();
                current.bannedUnits.addAll(target.bannedUnits);
            });
        } else {
            final Rules defaults = new Rules();
            mutator.mutate(current -> {
                current.buildSpeedMultiplier = defaults.buildSpeedMultiplier;
                current.buildCostMultiplier = defaults.buildCostMultiplier;
                current.blockHealthMultiplier = defaults.blockHealthMultiplier;
                current.blockDamageMultiplier = defaults.blockDamageMultiplier;
                current.unitDamageMultiplier = defaults.unitDamageMultiplier;
                current.unitHealthMultiplier = defaults.unitHealthMultiplier;
                current.unitBuildSpeedMultiplier = defaults.unitBuildSpeedMultiplier;
                current.unitCostMultiplier = defaults.unitCostMultiplier;
                current.solarMultiplier = defaults.solarMultiplier;
                current.unitMineSpeedMultiplier = defaults.unitMineSpeedMultiplier;
                current.unitCrashDamageMultiplier = defaults.unitCrashDamageMultiplier;
                current.deconstructRefundMultiplier = defaults.deconstructRefundMultiplier;
                current.itemDepositCooldown = defaults.itemDepositCooldown;
                current.dropZoneRadius = defaults.dropZoneRadius;
                current.waveSpacing = defaults.waveSpacing;
                current.winWave = defaults.winWave;
                current.unitCap = defaults.unitCap;
                current.waves = defaults.waves;
                current.waveTimer = defaults.waveTimer;
                current.waveSending = defaults.waveSending;
                current.waitEnemies = defaults.waitEnemies;
                current.wavesSpawnAtCores = defaults.wavesSpawnAtCores;
                current.hideSpawns = defaults.hideSpawns;
                current.randomWaveAI = defaults.randomWaveAI;
                current.airUseSpawns = defaults.airUseSpawns;
                current.fog = defaults.fog;
                current.lighting = defaults.lighting;
                current.borderDarkness = defaults.borderDarkness;
                current.staticFog = defaults.staticFog;
                current.unitLight = defaults.unitLight;
                current.reactorExplosions = defaults.reactorExplosions;
                current.fire = defaults.fire;
                current.possessionAllowed = defaults.possessionAllowed;
                current.schematicsAllowed = defaults.schematicsAllowed;
                current.infiniteResources = defaults.infiniteResources;
                current.pvp = defaults.pvp;
                current.pvpAutoPause = defaults.pvpAutoPause;
                current.attackMode = defaults.attackMode;
                current.coreCapture = defaults.coreCapture;
                current.coreIncinerates = defaults.coreIncinerates;
                current.canGameOver = defaults.canGameOver;
                current.pauseDisabled = defaults.pauseDisabled;
                current.damageExplosions = defaults.damageExplosions;
                current.derelictRepair = defaults.derelictRepair;
                current.placeRangeCheck = defaults.placeRangeCheck;
                current.polygonCoreProtection = defaults.polygonCoreProtection;
                current.cleanupDeadTeams = defaults.cleanupDeadTeams;
                current.onlyDepositCore = defaults.onlyDepositCore;
                current.allowCoreUnloaders = defaults.allowCoreUnloaders;
                current.coreDestroyClear = defaults.coreDestroyClear;
                current.hideBannedBlocks = defaults.hideBannedBlocks;
                current.allowEnvironmentDeconstruct = defaults.allowEnvironmentDeconstruct;
                current.instantBuild = defaults.instantBuild;
                current.ghostBlocks = defaults.ghostBlocks;
                current.logicUnitControl = defaults.logicUnitControl;
                current.unitPayloadUpdate = defaults.unitPayloadUpdate;
                current.showOtherTeamPings = defaults.showOtherTeamPings;
            });
        }
    }

    public void resetSettingsToDefaults() {
        showPermissionConfig.reset();
        showDescriptionsConfig.reset();
    }

    @Override
    public @Nullable Prov<SolimDialog> getMainDialog() {
        return this::getMainDialogInstance;
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new MapRulesSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}
