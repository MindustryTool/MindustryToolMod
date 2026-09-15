package mindustrytool.features.wavepreview;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for Wave Preview.
 */
public class WavePreviewSettingsDialog extends SolimDialog {

    public WavePreviewSettingsDialog(WavePreviewFeature feature) {
        super(Core.bundle.get("feature.wave-preview.settings.title"));

        name("wavePreviewSettingDialog");
        addCloseButton();
        closeOnBack();

        children(() -> new WavePreviewSettingsView(feature));
    }
}
