package mindustrytool.features.progressdisplay;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Progress Display options, using SolimDialog.
 */
public class ProgressDisplaySettingsDialog extends SolimDialog {

    public ProgressDisplaySettingsDialog(ProgressDisplayFeature feature) {
        super(Core.bundle.get("feature.progress-display.settings.title"));

        name("progressDisplaySettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new ProgressDisplaySettingsView(feature));
    }
}
