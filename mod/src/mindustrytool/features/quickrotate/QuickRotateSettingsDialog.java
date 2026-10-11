package mindustrytool.features.quickrotate;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring quick rotate options, using SolimDialog.
 */
public class QuickRotateSettingsDialog extends SolimDialog {

    public QuickRotateSettingsDialog(QuickRotateFeature feature) {
        super(Core.bundle.get("feature.quick-rotate.settings.title"));

        name("quickRotateSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new QuickRotateSettingsView(feature));
    }
}
