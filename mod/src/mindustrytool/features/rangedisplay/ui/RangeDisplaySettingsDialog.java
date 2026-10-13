package mindustrytool.features.rangedisplay.ui;

import arc.Core;
import mindustrytool.features.rangedisplay.RangeDisplayFeature;
import solim.overlay.SolimDialog;

public class RangeDisplaySettingsDialog extends SolimDialog {

    public RangeDisplaySettingsDialog(RangeDisplayFeature feature) {
        super(Core.bundle.get("feature.range-display.settings.title", "Range Display Settings"));

        name("rangeDisplaySettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new RangeDisplaySettingsView(feature));
    }
}
