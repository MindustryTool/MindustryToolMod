package mindustrytool.features.quickaccess;

import arc.Core;
import solim.overlay.SolimDialog;

public class QuickAccessSettingsDialog extends SolimDialog {

    public QuickAccessSettingsDialog(QuickAccessFeature feature) {
        super(Core.bundle.get("feature.quick-access.settings.title"));

        name("quickAccessSettingDialog");
        addCloseButton();
        closeOnBack();

        children(() -> new QuickAccessSettingsView(feature));
    }
}
