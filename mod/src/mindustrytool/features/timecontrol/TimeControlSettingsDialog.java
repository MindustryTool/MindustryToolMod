package mindustrytool.features.timecontrol;

import arc.Core;
import solim.overlay.SolimDialog;

public class TimeControlSettingsDialog extends SolimDialog {

    public TimeControlSettingsDialog(TimeControlFeature feature) {
        super(Core.bundle.get("feature.time-control.settings.title"));

        name("timeControlSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new TimeControlSettingsView(feature));
    }
}
