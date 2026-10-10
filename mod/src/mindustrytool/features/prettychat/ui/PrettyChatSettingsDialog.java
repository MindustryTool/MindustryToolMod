package mindustrytool.features.prettychat.ui;

import arc.Core;
import mindustrytool.features.prettychat.PrettyChatFeature;
import solim.overlay.SolimDialog;

/**
 * Solim settings dialog for Pretty Chat.
 */
public class PrettyChatSettingsDialog extends SolimDialog {

    public PrettyChatSettingsDialog(PrettyChatFeature feature) {
        super(Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.title", "Pretty Chat Settings")
                : "Pretty Chat Settings");

        name("prettyChatSettingsDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> {
            new PrettyChatSettingsView(feature);
        });
    }
}
