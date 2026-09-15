package mindustrytool.features.healthbar;

import arc.Core;
import mindustry.gen.Icon;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Health Bar options, using SolimDialog.
 */
public class HealthBarSettingsDialog extends SolimDialog {

    public HealthBarSettingsDialog(HealthBarFeature feature) {
        super(Core.bundle.get("feature.health-bar.settings.title"));

        name("healthBarSettingDialog");
        addCloseButton();
        closeOnBack();

        actionButton(Core.bundle.get("feature.health-bar.settings.reset"),
                Icon.refresh, 250f, 64f, feature::resetToDefaults);

        children(() -> new HealthBarSettingsView(feature));
    }
}
