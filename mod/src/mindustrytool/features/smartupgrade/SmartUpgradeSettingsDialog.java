package mindustrytool.features.smartupgrade;

import arc.Core;
import solim.overlay.SolimDialog;

public class SmartUpgradeSettingsDialog extends SolimDialog {

    public SmartUpgradeSettingsDialog(SmartUpgradeFeature feature) {
        super(Core.bundle.get("feature.smart-upgrade.settings.title"));

        name("smartUpgradeSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new SmartUpgradeSettingsView(feature));
    }
}
