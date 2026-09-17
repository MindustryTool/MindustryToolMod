package mindustrytool.features.settings;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Dialog hosting mod-wide preferences and diagnostics.
 * Width is constrained to 500f and content is delegated to {@link GeneralSettingsView}.
 */
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
