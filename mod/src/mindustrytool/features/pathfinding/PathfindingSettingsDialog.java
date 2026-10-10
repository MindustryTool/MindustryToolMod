package mindustrytool.features.pathfinding;

import arc.Core;
import solim.overlay.SolimDialog;

public class PathfindingSettingsDialog extends SolimDialog {

    public PathfindingSettingsDialog(PathfindingFeature feature) {
        super(Core.bundle.get("feature.pathfinding.settings.title", "Pathfinding Settings"));

        name("pathfindingSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new PathfindingSettingsView(feature));
    }
}
