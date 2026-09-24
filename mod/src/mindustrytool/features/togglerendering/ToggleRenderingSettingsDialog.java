package mindustrytool.features.togglerendering;

import arc.Core;
import mindustry.gen.Icon;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Toggle Rendering options using SolimDialog.
 */
public class ToggleRenderingSettingsDialog extends SolimDialog {

    public ToggleRenderingSettingsDialog(ToggleRenderingFeature feature) {
        super(Core.bundle != null ? Core.bundle.get("feature.toggle-rendering.settings.title") : "Toggle Rendering Settings");

        name("toggleRenderingSettingDialog");
        maxWidth(500f);
        addCloseButton();
        closeOnBack();

        actionButton(Core.bundle != null ? Core.bundle.get("feature.toggle-rendering.settings.reset") : "Reset to Defaults",
                Icon.refresh, 250f, 64f, feature::resetToDefaults);

        children(() -> new ToggleRenderingSettingsView(feature));
    }
}
