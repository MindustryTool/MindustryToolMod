package mindustrytool.features.chat;

import arc.Core;
import solim.overlay.SolimDialog;

public class ChatSettingsDialog extends SolimDialog {

    public ChatSettingsDialog(ChatFeature feature) {
        super(Core.bundle.get("feature.chat.settings.title", "Chat Settings"));

        name("chatSettingsDialog");
        addCloseButton();
        closeOnBack();

        children(() -> new ChatSettingsView(feature));
    }
}
