package mindustrytool.features.progressdisplay;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Progress Display options, using SolimDialog.
 */
public class ProgressDisplaySettingsDialog extends SolimDialog {

    public ProgressDisplaySettingsDialog(ProgressDisplayFeature feature) {
        super(Core.bundle != null
                ? Core.bundle.get("feature.progress-display.settings.title", "Progress Display Settings")
                : "Progress Display Settings");

        name("progressDisplaySettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new ProgressDisplaySettingsView(feature));
    }
}
