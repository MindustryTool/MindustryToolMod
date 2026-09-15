package mindustrytool.features.godmode;

import arc.Core;
import solim.overlay.SolimDialog;

public class GodModeSettingsDialog extends SolimDialog {

    public GodModeSettingsDialog(GodModeFeature feature) {
        super(Core.bundle.get("feature.god-mode.settings.title"));

        name("godModeSettingDialog");
        addCloseButton();
        closeOnBack();

        children(() -> new GodModeSettingsView(feature));
    }
}
