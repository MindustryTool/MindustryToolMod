package mindustrytool.features.teamresource;

import arc.Core;
import mindustry.gen.Icon;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for configuring Team Resource Tracker options, using SolimDialog.
 */
public class TeamResourceSettingsDialog extends SolimDialog {

    public TeamResourceSettingsDialog(TeamResourceFeature feature) {
        super(Core.bundle.get("team-resources.settings.title", "Team Resources Settings"));

        name("teamResourceSettingDialog");
        addCloseButton();
        closeOnBack();

        actionButton(Core.bundle.get("team-resources.reset-to-defaults", "Reset to Defaults"), Icon.refresh, 250f, 64f, feature::resetToDefaults);

        children(() -> new TeamResourceSettingsView(feature));
    }
}
