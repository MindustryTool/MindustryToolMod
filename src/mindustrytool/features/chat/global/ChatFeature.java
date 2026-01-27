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
import mindustrytool.features.chat.global.dto.ChatMessageReceive;

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
        Events.on(ChatMessageReceive.class, event -> {
            if (overlay == null) {
                return;
            }

            overlay.addMessages(event.messages);
        });

        Events.on(LoginEvent.class, e -> {
            ChatService.getInstance().disconnectStream();
            ChatService.getInstance().connectStream();
        });

        Core.app.addListener(new ApplicationListener() {
            @Override
            public void exit() {
                ChatService.getInstance().disconnectStream();
            }
        });
    }

    @Override
    public void onEnable() {
        overlay = new ChatOverlay();
        ChatService.getInstance().connectStream();

        if (Vars.ui.menuGroup != null) {
            Core.app.post(() -> Core.scene.add(overlay));
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
        if (settingDialog == null) {

            settingDialog = new BaseDialog("@chat.settings.title");
            settingDialog.name = "chatSettingDialog";

            settingDialog.cont.button("@chat.reset-overlay-position", () -> {
                ChatConfig.x(0);
                ChatConfig.y(0);

                if (overlay != null) {
                    overlay.setPosition(0, 0);
                    overlay.keepInScreen();
                }
            }).size(240f, 50f);

            settingDialog.addCloseButton();
            settingDialog.closeOnBack();
        }
        return Optional.of(settingDialog);
    }
}
