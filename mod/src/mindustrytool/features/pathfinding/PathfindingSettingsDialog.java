package mindustrytool.features.pathfinding;

import arc.Core;
import mindustry.gen.Icon;
import solim.overlay.SolimDialog;

public class PathfindingSettingsDialog extends SolimDialog {

    public PathfindingSettingsDialog(PathfindingFeature feature) {
        super(Core.bundle.get("feature.pathfinding.settings.title", "Pathfinding Settings"));

        name("pathfindingSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        actionButton(Core.bundle.get("feature.pathfinding.settings.reset", "Reset to Defaults"),
                Icon.refresh, 250f, 64f, feature::resetToDefaults);

        children(() -> new PathfindingSettingsView(feature));
    }
}
