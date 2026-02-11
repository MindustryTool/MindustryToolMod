package mindustrytool.features.chat.global;

import java.util.Optional;

import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.scene.ui.Dialog;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.auth.dto.LoginEvent;
import mindustrytool.features.chat.global.dto.ChatMessageReceive;

public class ChatFeature implements Feature {
    private ChatOverlay overlay;

    BaseDialog settingDialog;

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
                try {
                    ChatService.getInstance().disconnectStream();
                } catch (Throwable e) {
                    Log.err(e);
                }
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
            settingDialog.addCloseButton();
            settingDialog.closeOnBack();
            settingDialog.shown(this::rebuildSettings);
        }
        return Optional.of(settingDialog);
    }

    private void rebuildSettings() {
        Table cont = settingDialog.cont;
        cont.clear();
        cont.defaults().pad(6).left();
        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        // Reset Position
        cont.button("@chat.reset-overlay-position", () -> {
            ChatConfig.x(0);
            ChatConfig.y(0);

            if (overlay != null) {
                overlay.setPosition(0, 0);
                overlay.keepInScreen();
            }
        }).size(240f, 50f).row();

        // Opacity
        Slider opacitySlider = new Slider(0.05f, 1f, 0.05f, false);
        opacitySlider.setValue(ChatConfig.opacity());
        Label opacityValue = new Label(String.format("%.0f%%", ChatConfig.opacity() * 100));

        Table opacityContent = new Table();
        opacityContent.add("@opacity").left().growX();
        opacityContent.add(opacityValue).padLeft(10f).right();

        opacitySlider.changed(() -> {
            ChatConfig.opacity(opacitySlider.getValue());
            opacityValue.setText(String.format("%.0f%%", ChatConfig.opacity() * 100));
            if (overlay != null)
                overlay.rebuild();
        });

        cont.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

        // Scale
        Slider scaleSlider = new Slider(0.5f, 1.5f, 0.1f, false);
        scaleSlider.setValue(ChatConfig.scale());
        Label scaleValue = new Label(String.format("%.0f%%", ChatConfig.scale() * 100));

        Table scaleContent = new Table();
        scaleContent.add("@scale").left().growX();
        scaleContent.add(scaleValue).padLeft(10f).right();

        scaleSlider.changed(() -> {
            ChatConfig.scale(scaleSlider.getValue());
            scaleValue.setText(String.format("%.0f%%", ChatConfig.scale() * 100));
            if (overlay != null)
                overlay.rebuild();
        });

        cont.stack(scaleSlider, scaleContent).width(width).left().padTop(4f).row();

        // Width
        Slider widthSlider = new Slider(0.5f, 2.0f, 0.1f, false);
        widthSlider.setValue(ChatConfig.width());
        Label widthValue = new Label(String.format("%.0f%%", ChatConfig.width() * 100));

        Table widthContent = new Table();
        widthContent.add("@width").left().growX();
        widthContent.add(widthValue).padLeft(10f).right();

        widthSlider.changed(() -> {
            ChatConfig.width(widthSlider.getValue());
            widthValue.setText(String.format("%.0f%%", ChatConfig.width() * 100));
            if (overlay != null)
                overlay.rebuild();
        });

        cont.stack(widthSlider, widthContent).width(width).left().padTop(4f).row();
    }
}
