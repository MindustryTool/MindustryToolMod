package mindustrytool.features.maprules;

import arc.Core;
import mindustry.gen.Icon;
import solim.overlay.SolimDialog;

/**
 * Feature settings dialog.
 */
public class MapRulesSettingsDialog extends SolimDialog {

    public MapRulesSettingsDialog(MapRulesFeature feature) {
        super(Core.bundle != null ? Core.bundle.get("feature.map-rules.settings.title") : "Map Rules Settings");

        name("mapRulesSettingDialog");
        maxWidth(500f);
        addCloseButton();
        closeOnBack();

        actionButton(Core.bundle != null ? Core.bundle.get("feature.map-rules.settings.reset") : "Reset to Defaults",
                Icon.refresh, 250f, 64f, feature::resetSettingsToDefaults);

        children(() -> new MapRulesSettingsView(feature));
    }
}
