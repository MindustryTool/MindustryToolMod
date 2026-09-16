package mindustrytool.features.joystick;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for the Virtual Joystick feature.
 */
public class JoystickSettingsDialog extends SolimDialog {

    public JoystickSettingsDialog(JoystickFeature feature) {
        super(Core.bundle.get("feature.joystick.settings.title"));

        name("joystickSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new JoystickSettingsView(feature));
    }
}
