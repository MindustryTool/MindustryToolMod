package mindustrytool.features.healthbar;

import arc.Core;
import solim.overlay.SolimDialog;

public class HealthBarSettingsDialog extends SolimDialog {

    public HealthBarSettingsDialog(HealthBarFeature feature) {
        super(Core.bundle.get("feature.health-bar.settings.title"));

        name("healthBarSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new HealthBarSettingsView(feature));
    }
}
