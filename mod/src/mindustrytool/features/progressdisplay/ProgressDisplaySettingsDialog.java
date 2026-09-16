package mindustrytool.features.progressdisplay;

import arc.Core;
import mindustry.gen.Icon;
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

        actionButton(Core.bundle.get("feature.progress-display.settings.reset"),
                Icon.refresh, 250f, 64f, feature::resetToDefaults);

        children(() -> new ProgressDisplaySettingsView(feature));
    }
}
