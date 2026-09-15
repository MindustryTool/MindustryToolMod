package mindustrytool.features.bridgevisualizer;

import arc.Core;
import mindustry.gen.Icon;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Bridge Visualizer options, using SolimDialog.
 */
public class BridgeVisualizerSettingsDialog extends SolimDialog {

    public BridgeVisualizerSettingsDialog(BridgeVisualizerFeature feature) {
        super(Core.bundle.get("feature.bridge-visualizer.settings.title", "Bridge Visualizer Settings"));

        name("bridgeVisualizerSettingDialog");
        addCloseButton();
        closeOnBack();

        actionButton(Core.bundle.get("feature.bridge-visualizer.settings.reset", "Reset to Defaults"),
                Icon.refresh, 250f, 64f, feature::resetToDefaults);

        children(() -> new BridgeVisualizerSettingsView(feature));
    }
}
