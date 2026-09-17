package mindustrytool.features.autoplay;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for Autoplay feature.
 */
public class AutoplaySettingsDialog extends SolimDialog {

    public AutoplaySettingsDialog(AutoplayFeature feature) {
        super(Core.bundle.get("feature.autoplay.settings.title"));

        name("autoplaySettingsDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new AutoplaySettingsView(feature));
    }
}
