package mindustrytool.features.togglerendering;

import arc.Core;
import solim.overlay.SolimDialog;

public class ToggleRenderingSettingsDialog extends SolimDialog {

    public ToggleRenderingSettingsDialog(ToggleRenderingFeature feature) {
        super(Core.bundle.get("feature.toggle-rendering.settings.title"));

        name("toggleRenderingSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new ToggleRenderingSettingsView(feature));
    }
}
