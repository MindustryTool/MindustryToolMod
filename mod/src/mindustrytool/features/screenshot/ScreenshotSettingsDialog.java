package mindustrytool.features.screenshot;

import arc.Core;
import solim.overlay.SolimDialog;

public class ScreenshotSettingsDialog extends SolimDialog {

    public ScreenshotSettingsDialog(ScreenshotFeature feature) {
        super(Core.bundle.get("feature.screenshot.settings.title"));

        name("screenshotSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new ScreenshotSettingsView(feature));
    }
}
