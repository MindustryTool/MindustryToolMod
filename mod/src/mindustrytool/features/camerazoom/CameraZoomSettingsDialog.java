package mindustrytool.features.camerazoom;

import arc.Core;
import mindustry.gen.Icon;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring camera zoom limits, using SolimDialog.
 */
public class CameraZoomSettingsDialog extends SolimDialog {

    public CameraZoomSettingsDialog(CameraZoomFeature feature) {
        super(Core.bundle.get("feature.camera-zoom.settings.title"));

        name("cameraZoomSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        actionButton(Core.bundle.get("feature.camera-zoom.settings.reset"),
                Icon.refresh, 250f, 64f, feature::resetToDefaults);

        children(() -> new CameraZoomSettingsView(feature));
    }
}
