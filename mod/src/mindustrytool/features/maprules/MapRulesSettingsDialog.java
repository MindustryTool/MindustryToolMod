package mindustrytool.features.maprules;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Feature settings dialog.
 */
public class MapRulesSettingsDialog extends SolimDialog {

    public MapRulesSettingsDialog(MapRulesFeature feature) {
        super(Core.bundle.get("feature.map-rules.settings.title"));

        name("mapRulesSettingDialog");
        maxWidth(500f);
        addCloseButton();
        closeOnBack();

        children(() -> new MapRulesSettingsView(feature));
    }
}
