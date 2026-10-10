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
        if (mode == RuleEditMode.CLIENT_ADMIN) {
            mutator.mutateRemote("Vars.state.rules = Vars.state.map.applyRules(Vars.state.rules.mode()); Call.setRules(Vars.state.rules);");
            return;
        }
        final Rules target = snapshot != null ? snapshot.copy() : new Rules();
        mutator.mutate(current -> copyRules(current, target));
    }

    private void copyRules(Rules dest, Rules src) {
        for (RuleDescriptor def : RuleDescriptors.ALL) {
            switch (def.kind) {
                case FLOAT:
                    def.floatSetter.get(dest, def.floatGetter.get(src));
                    break;
                case INT:
                    def.intSetter.get(dest, def.intGetter.get(src));
                    break;
                case BOOL:
                    def.boolSetter.get(dest, def.boolGetter.get(src));
                    break;
                case BAN_BLOCKS:
                    dest.bannedBlocks.clear();
                    dest.bannedBlocks.addAll(src.bannedBlocks);
                    break;
                case BAN_UNITS:
                    dest.bannedUnits.clear();
                    dest.bannedUnits.addAll(src.bannedUnits);
                    break;
                default:
                    break;
            }
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
