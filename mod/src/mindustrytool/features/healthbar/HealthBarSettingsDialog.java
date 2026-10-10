package mindustrytool.features.healthbar;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Health Bar options, using SolimDialog.
 */
public class HealthBarSettingsDialog extends SolimDialog {

    public HealthBarSettingsDialog(HealthBarFeature feature) {
        super(Core.bundle != null
                ? Core.bundle.get("feature.health-bar.settings.title", "Health Bar Settings")
                : "Health Bar Settings");

        name("healthBarSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new HealthBarSettingsView(feature));
    }
}
