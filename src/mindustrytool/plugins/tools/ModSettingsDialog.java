package mindustrytool.plugins.tools;

import arc.Core;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Simple Mod settings dialog (placed under Tools -> MindustryTool Settings).
 * Add more settings here as needed.
 */
public class ModSettingsDialog extends BaseDialog {

    public ModSettingsDialog() {
        super(Core.bundle.get("message.mtool.settings", "MindustryTool Settings"));
        addCloseButton();
        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.defaults().pad(6).fillX();

        // No mod-level settings available yet
        cont.add(Core.bundle.get("mtool.no-settings", "No MindustryTool-specific settings available.")).pad(12).row();
    }
}
