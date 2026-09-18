package mindustrytool.features.schematicgrid;

import arc.Core;
import solim.overlay.SolimDialog;

public class QuickSchematicGridSettingsDialog extends SolimDialog {

    public QuickSchematicGridSettingsDialog(QuickSchematicGridFeature feature) {
        super(Core.bundle.get("feature.quick-schematic-grid.settings.title"));

        name("quickSchematicGridSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new QuickSchematicGridSettingsView(feature));
    }
}
