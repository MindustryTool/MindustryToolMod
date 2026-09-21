package mindustrytool.features.smartupgrade;

import arc.Core;
import mindustry.gen.Icon;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Smart Upgrade feature options.
 */
public class SmartUpgradeSettingsDialog extends SolimDialog {

    public SmartUpgradeSettingsDialog(SmartUpgradeFeature feature) {
        super(Core.bundle.get("feature.smart-upgrade.settings.title", "Smart Upgrade Settings"));

        name("smartUpgradeSettingDialog");
        addCloseButton();
        closeOnBack();

        actionButton(Core.bundle.get("feature.smart-upgrade.settings.reset", "Reset to Defaults"),
                Icon.refresh, 250f, 64f, feature::resetToDefaults);

        children(() -> new SmartUpgradeSettingsView(feature));
    }
}
