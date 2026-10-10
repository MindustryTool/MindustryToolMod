package mindustrytool.features.camerazoom;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring camera zoom limits, using SolimDialog.
 */
public class CameraZoomSettingsDialog extends SolimDialog {

    public CameraZoomSettingsDialog(CameraZoomFeature feature) {
        super(Core.bundle != null
                ? Core.bundle.get("feature.camera-zoom.settings.title", "Camera Zoom Settings")
                : "Camera Zoom Settings");

        name("cameraZoomSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new CameraZoomSettingsView(feature));
    }
}
