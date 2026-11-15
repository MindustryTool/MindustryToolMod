package mindustrytool.features.resourcemonitor;

import arc.Core;
import solim.overlay.SolimDialog;

public class ResourceMonitorSettingsDialog extends SolimDialog {

    public ResourceMonitorSettingsDialog(ResourceMonitorFeature feature) {
        super(Core.bundle.get("team-resources.settings.title"));

        name("resourceMonitorSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);
        blurBackdrop(12f);

        children(() -> new ResourceMonitorSettingsView(feature));
    }
}
