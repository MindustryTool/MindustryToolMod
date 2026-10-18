package mindustrytool.features.griefalert;

import arc.Core;
import solim.overlay.SolimDialog;

public class GriefAlertSettingsDialog extends SolimDialog {
    public GriefAlertSettingsDialog(GriefAlertFeature feature) {
        super(Core.bundle.get("feature.grief-alert.settings.title"));

        name("griefAlertSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new GriefAlertSettingsView(feature));
    }
}
