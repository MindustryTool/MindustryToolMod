package mindustrytool.features.camerazoom;

import arc.Core;
import solim.overlay.SolimDialog;

public class CameraZoomSettingsDialog extends SolimDialog {

    public CameraZoomSettingsDialog(CameraZoomFeature feature) {
        super(Core.bundle.get("feature.camera-zoom.settings.title"));

        name("cameraZoomSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new CameraZoomSettingsView(feature));
    }
}
