package mindustrytool.features.togglerendering;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Toggle Rendering options using SolimDialog.
 */
public class ToggleRenderingSettingsDialog extends SolimDialog {

    public ToggleRenderingSettingsDialog(ToggleRenderingFeature feature) {
        super(Core.bundle != null
                ? Core.bundle.get("feature.toggle-rendering.settings.title", "Toggle Rendering Settings")
                : "Toggle Rendering Settings");

        name("toggleRenderingSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new ToggleRenderingSettingsView(feature));
    }
}
