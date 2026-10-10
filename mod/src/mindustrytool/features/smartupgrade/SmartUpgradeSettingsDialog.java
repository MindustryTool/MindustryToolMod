package mindustrytool.features.smartupgrade;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Smart Upgrade feature options.
 */
public class SmartUpgradeSettingsDialog extends SolimDialog {

    public SmartUpgradeSettingsDialog(SmartUpgradeFeature feature) {
        super(Core.bundle != null
                ? Core.bundle.get("feature.smart-upgrade.settings.title", "Smart Upgrade Settings")
                : "Smart Upgrade Settings");

        name("smartUpgradeSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new SmartUpgradeSettingsView(feature));
    }
}
