package mindustrytool.features.logicvisualizer;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Logic Visualizer options, using SolimDialog.
 */
public class LogicVisualizerSettingsDialog extends SolimDialog {

    public LogicVisualizerSettingsDialog(LogicVisualizerFeature feature) {
        super(Core.bundle.get("feature.logic-visualizer.settings.title"));

        name("logicVisualizerSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new LogicVisualizerSettingsView(feature));
    }
}
