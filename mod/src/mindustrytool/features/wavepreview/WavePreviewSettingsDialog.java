package mindustrytool.features.wavepreview;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for Wave Preview.
 */
public class WavePreviewSettingsDialog extends SolimDialog {

    public WavePreviewSettingsDialog(WavePreviewFeature feature) {
        super(Core.bundle != null
                ? Core.bundle.get("feature.wave-preview.settings.title", "Wave Preview Settings")
                : "Wave Preview Settings");

        name("wavePreviewSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new WavePreviewSettingsView(feature));
    }
}
