package mindustrytool.features.smartdrill.ui;

import arc.Core;
import mindustrytool.features.smartdrill.SmartDrillFeature;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Smart Drill options.
 */
public class SmartDrillSettingsDialog extends SolimDialog {

    public SmartDrillSettingsDialog(SmartDrillFeature feature) {
        super(Core.bundle != null
                ? Core.bundle.get("feature.smart-drill.settings.title", "Smart Drill Settings")
                : "Smart Drill Settings");

        name("smartDrillSettingsDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new SmartDrillSettingsView(feature));
    }
}
