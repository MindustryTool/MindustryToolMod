package mindustrytool.features.armycontrol;

import arc.Core;
import solim.overlay.SolimDialog;

public class ArmyControlSettingsDialog extends SolimDialog {

    public ArmyControlSettingsDialog(ArmyControlFeature feature) {
        super(Core.bundle.get("feature.army-control.settings.title", "Army Control Settings"));

        name("armyControlSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new ArmyControlSettingsView(feature));
    }
}
