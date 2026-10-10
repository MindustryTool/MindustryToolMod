package mindustrytool.features.callout.ui;

import arc.Core;
import mindustrytool.features.callout.CalloutFeature;
import solim.overlay.SolimDialog;

/**
 * Solim settings dialog for Tactical Callout feature.
 */
public class CalloutSettingsDialog extends SolimDialog {

    public CalloutSettingsDialog(CalloutFeature feature) {
        super(Core.bundle != null
                ? Core.bundle.get("feature.callout.settings.title", "Callout Settings")
                : "Callout Settings");

        name("calloutSettingsDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(550f);

        children(() -> new CalloutSettingsView(feature));
    }
}
