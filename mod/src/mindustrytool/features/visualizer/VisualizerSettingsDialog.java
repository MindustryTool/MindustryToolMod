package mindustrytool.features.visualizer;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring unified Visualizer options, using SolimDialog.
 */
public class VisualizerSettingsDialog extends SolimDialog {

    public VisualizerSettingsDialog(VisualizerFeature feature) {
        super(Core.bundle.get("feature.visualizer.settings.title", "Visualizer Settings"));

        name("visualizerSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new VisualizerSettingsView(feature));
    }
}
