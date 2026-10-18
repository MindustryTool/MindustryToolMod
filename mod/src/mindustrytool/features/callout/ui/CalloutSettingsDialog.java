package mindustrytool.features.callout.ui;

import arc.Core;
import mindustrytool.features.callout.CalloutFeature;
import solim.overlay.SolimDialog;

public class CalloutSettingsDialog extends SolimDialog {

    public CalloutSettingsDialog(CalloutFeature feature) {
        super(Core.bundle.get("feature.callout.settings.title"));

        name("calloutSettingsDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new CalloutSettingsView(feature));
    }
}
