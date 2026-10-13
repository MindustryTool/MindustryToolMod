package mindustrytool.features.settings;

import arc.Core;
import solim.overlay.SolimDialog;

public class GeneralSettingsDialog extends SolimDialog {

    public GeneralSettingsDialog() {
        super(Core.bundle.get("dialog.general-settings.title"));

        name("generalSettingsDialog");
        addCloseButton();
        closeOnBack();

        maxWidth(500f);

        children(GeneralSettingsView::new);
    }
}
