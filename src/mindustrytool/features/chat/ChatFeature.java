package mindustrytool.features.chat;

import mindustry.Vars;
import mindustry.gen.Iconc;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.chat.dto.ChatMessage;
import mindustrytool.services.UserService;

public class ChatFeature implements Feature {
    private ChatOverlay overlay;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("Chat")
                .description("Global chat")
                .icon(Iconc.chat)
                .enabledByDefault(true)
                .build();
    }

    @Override
    public void init() {
        overlay = new ChatOverlay();

        ChatService.getInstance().setListener(messages -> {
            if (Vars.ui.chatfrag != null) {
                for (ChatMessage message : messages) {
                    UserService.findUserById(message.createdBy, (user) -> {
                        String content = "[cyan][Global][] " + user.name() + "[] " + message.content;
                        Vars.ui.chatfrag.addMessage(content);
                    });
                }
            }

            if (overlay != null) {
                overlay.addMessages(messages);
            }
        });
    }

    @Override
    public void onEnable() {
        ChatService.getInstance().connectStream();

        // Add to menu group
        // We want it to be visible only in menu
        if (Vars.ui.menuGroup != null) {
            // Position it on the left or right?
            // Let's put it on the bottom left, but above other buttons if possible.
            // Or maybe a fixed size window on the right.

            // Check if already added
            if (Vars.ui.menuGroup.find("mdt-chat-overlay") != null) {
                Vars.ui.menuGroup.find("mdt-chat-overlay").remove();
            }

            overlay.name = "mdt-chat-overlay";
            Vars.ui.menuGroup.addChild(overlay);

            overlay.visible(() -> Vars.state.isMenu());
        }
    }

    @Override
    public void onDisable() {
        ChatService.getInstance().disconnectStream();

        if (overlay != null) {
            overlay.remove();
        }
    }
}
