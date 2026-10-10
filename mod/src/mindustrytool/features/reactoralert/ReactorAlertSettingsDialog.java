package mindustrytool.features.reactoralert;

import arc.Core;
import solim.overlay.SolimDialog;

public class ReactorAlertSettingsDialog extends SolimDialog {
    public ReactorAlertSettingsDialog(ReactorAlertFeature feature) {
        super(Core.bundle.get("feature.reactor-alert.settings.title"));

        name("reactorAlertSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new ReactorAlertSettingsView(feature));
    }
}
