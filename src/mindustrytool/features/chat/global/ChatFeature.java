package mindustrytool.features.chat.global;

import java.util.Optional;

import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.scene.ui.Dialog;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.auth.dto.LoginEvent;

public class ChatFeature implements Feature {
    private ChatOverlay overlay;

    Dialog settingDialog;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.chat.name")
                .description("@feature.chat.description")
                .icon(Icon.chat)
                .enabledByDefault(true)
                .build();
    }

    @Override
    public void init() {
        overlay = new ChatOverlay();

        ChatService.getInstance().setListener(messages -> {
            overlay.addMessages(messages);
        });

        Events.on(LoginEvent.class, e -> {
            ChatService.getInstance().disconnectStream();
            ChatService.getInstance().connectStream();
        });

        settingDialog = new BaseDialog("@chat.settings.title");
        settingDialog.cont.button("@chat.reset-overlay-position", () -> {
            ChatConfig config = new ChatConfig();

            config.x(0);
            config.y(0);

            if (overlay != null) {
                overlay.setPosition(0, 0);
                overlay.keepInScreen();
            }
        }).size(240f, 50f);

        settingDialog.addCloseButton();
        settingDialog.closeOnBack();

        Core.app.addListener(new ApplicationListener() {
            @Override
            public void exit() {
                ChatService.getInstance().disconnectStream();
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
            Core.scene.add(overlay);
        }
    }

    @Override
    public void onDisable() {
        ChatService.getInstance().disconnectStream();

        if (overlay != null) {
            overlay.remove();
        }
    }

    @Override
    public Optional<Dialog> setting() {
        return Optional.of(settingDialog);
    }
}
