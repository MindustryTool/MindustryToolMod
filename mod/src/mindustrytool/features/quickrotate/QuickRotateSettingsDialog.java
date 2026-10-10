package mindustrytool.features.quickrotate;

import arc.Core;
import mindustry.gen.Icon;
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

        actionButton(Core.bundle.get("feature.quick-rotate.settings.reset"),
                Icon.refresh, 250f, 64f, feature::resetToDefaults);

        children(() -> new QuickRotateSettingsView(feature));
    }
}
