package mindustrytool.features.settings;

import arc.Core;
import mindustrytool.features.Feature;
import solim.overlay.SolimDialog;

public class FeatureHelpDialog extends SolimDialog {

    public FeatureHelpDialog(Feature feature) {
        super(Core.bundle.format("feature.help.title", feature.getName()));

        addCloseButton();
        closeOnBack();

        children(() -> new FeatureHelpView(feature));
    }
}
