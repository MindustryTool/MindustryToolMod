package mindustrytool.features.displaygroup;

import arc.Core;
import solim.overlay.SolimDialog;

public class DisplayGroupSettingsDialog extends SolimDialog {

    public DisplayGroupSettingsDialog(DisplayGroupFeature feature) {
        super(Core.bundle.get("feature.display-group.settings.title"));

        name("displayGroupSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new DisplayGroupSettingsView(feature));
    }
}
