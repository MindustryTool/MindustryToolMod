package mindustrytool.features.teamresource;

import arc.Core;
import solim.overlay.SolimDialog;

public class TeamResourceSettingsDialog extends SolimDialog {

    public TeamResourceSettingsDialog(TeamResourceFeature feature) {
        super(Core.bundle.get("team-resources.settings.title", "Team Resources Settings"));

        name("teamResourceSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> new TeamResourceSettingsView(feature));
    }
}
