package mindustrytool.features.display.pathfinding;

import mindustry.ui.dialogs.BaseDialog;

public class PathfindingSettingsUI {
    private PathfindingSettingsDialog settingsDialog;

    public BaseDialog getDialog() {
        if (settingsDialog == null) {
            settingsDialog = new PathfindingSettingsDialog();
        }
        return settingsDialog;
    }
}
