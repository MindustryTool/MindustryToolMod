package mindustrytool.features.rangedisplay.ui;

import arc.Core;
import mindustry.gen.Icon;
import mindustrytool.features.rangedisplay.RangeDisplayFeature;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Range Display options.
 */
public class RangeDisplaySettingsDialog extends SolimDialog {

    public RangeDisplaySettingsDialog(RangeDisplayFeature feature) {
        super(Core.bundle.get("feature.range-display.settings.title", "Range Display Settings"));

        name("rangeDisplaySettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        actionButton(Core.bundle.get("feature.range-display.settings.reset", "Reset to Defaults"),
                Icon.refresh, 250f, 64f, feature::resetToDefaults);

        children(() -> new RangeDisplaySettingsView(feature));
    }
}
