package mindustrytool.features.screenshot;

import arc.Core;
import mindustry.gen.Icon;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring screenshot capture options, using SolimDialog.
 */
public class ScreenshotSettingsDialog extends SolimDialog {

    public ScreenshotSettingsDialog(ScreenshotFeature feature) {
        super(Core.bundle.get("feature.screenshot.settings.title"));

        name("screenshotSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        actionButton(Core.bundle.get("feature.screenshot.settings.reset"),
                Icon.refresh, 250f, 64f, feature::resetToDefaults);

        children(() -> new ScreenshotSettingsView(feature));
    }
}
