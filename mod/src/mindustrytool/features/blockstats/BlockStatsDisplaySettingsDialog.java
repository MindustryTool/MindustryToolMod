package mindustrytool.features.blockstats;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Block Stats Display options, using SolimDialog.
 */
public class BlockStatsDisplaySettingsDialog extends SolimDialog {

    public BlockStatsDisplaySettingsDialog(BlockStatsDisplayFeature feature) {
        super(Core.bundle.get("feature.blockstats-display.settings.title"));

        name("blockStatsDisplaySettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new BlockStatsDisplaySettingsView(feature));
    }
}
