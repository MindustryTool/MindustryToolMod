package mindustrytool.features.wavepreview;

import arc.Core;
import solim.overlay.SolimDialog;

public class WavePreviewSettingsDialog extends SolimDialog {

    public WavePreviewSettingsDialog(WavePreviewFeature feature) {
        super(Core.bundle.get("feature.wave-preview.settings.title"));

        name("wavePreviewSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new WavePreviewSettingsView(feature));
    }
}
