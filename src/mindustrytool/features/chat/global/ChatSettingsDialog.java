package mindustrytool.features.chat.global;

import arc.Core;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.features.chat.global.ui.ChatOverlay;

public class ChatSettingsDialog extends BaseDialog {
    private final ChatFeature feature;

    public ChatSettingsDialog(ChatFeature feature) {
        super("@chat.settings.title");
        this.feature = feature;

        name = "chatSettingDialog";
        addCloseButton();
        closeOnBack();
        shown(this::rebuildSettings);
    }

    private void rebuildSettings() {
        Table container = cont;
        container.clear();
        container.defaults().pad(6).left();
        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        container.button("@chat.reset-overlay-position", () -> {
            ChatConfig.x(0);
            ChatConfig.y(0);

            ChatOverlay overlay = feature.getOverlay();
            if (overlay != null) {
                overlay.setPosition(0, 0);
                overlay.keepInScreen();
            }
        }).size(240f, 50f).row();

        Slider opacitySlider = new Slider(0.05f, 1f, 0.05f, false);
        opacitySlider.setValue(ChatConfig.opacity());
        Label opacityValue = new Label(String.format("%.0f%%", ChatConfig.opacity() * 100));

        Table opacityContent = new Table();
        opacityContent.touchable = Touchable.disabled;
        opacityContent.add("@opacity").left().growX().padLeft(10).padRight(10);
        opacityContent.add(opacityValue).padLeft(10f).right();

        opacitySlider.changed(() -> {
            ChatConfig.opacity(opacitySlider.getValue());
            opacityValue.setText(String.format("%.0f%%", ChatConfig.opacity() * 100));
            feature.rebuildOverlay();
        });

        container.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

        Slider scaleSlider = new Slider(0.5f, 1.5f, 0.1f, false);
        scaleSlider.setValue(ChatConfig.scale());
        Label scaleValue = new Label(String.format("%.0f%%", ChatConfig.scale() * 100));

        Table scaleContent = new Table();
        scaleContent.touchable = Touchable.disabled;
        scaleContent.add("@scale").left().growX().padLeft(10).padRight(10);
        scaleContent.add(scaleValue).padLeft(10f).right();

        scaleSlider.changed(() -> {
            ChatConfig.scale(scaleSlider.getValue());
            scaleValue.setText(String.format("%.0f%%", ChatConfig.scale() * 100));
            feature.rebuildOverlay();
        });

        container.stack(scaleSlider, scaleContent).width(width).left().padTop(4f).row();

        Slider widthSlider = new Slider(0.1f, 1.0f, 0.1f, false);
        widthSlider.setValue(ChatConfig.width());
        Label widthValue = new Label(String.format("%.0f%%", ChatConfig.width() * 100));

        Table widthContent = new Table();
        widthContent.touchable = Touchable.disabled;
        widthContent.add("@width").left().growX().padLeft(10).padRight(10);
        widthContent.add(widthValue).padLeft(10f).right();

        widthSlider.changed(() -> {
            ChatConfig.width(widthSlider.getValue());
            widthValue.setText(String.format("%.0f%%", ChatConfig.width() * 100));
            feature.rebuildOverlay();
        });

        container.stack(widthSlider, widthContent).width(width).left().padTop(4f).row();

        Slider heightSlider = new Slider(0.1f, 1.0f, 0.1f, false);
        heightSlider.setValue(ChatConfig.height());
        Label heightValue = new Label(String.format("%.0f%%", ChatConfig.height() * 100));

        Table heightContent = new Table();
        heightContent.touchable = Touchable.disabled;
        heightContent.add("@height").left().growX().padLeft(10).padRight(10);
        heightContent.add(heightValue).padLeft(10f).right();

        heightSlider.changed(() -> {
            ChatConfig.height(heightSlider.getValue());
            heightValue.setText(String.format("%.0f%%", ChatConfig.height() * 100));
            feature.rebuildOverlay();
        });

        container.stack(heightSlider, heightContent).width(width).left().padTop(4f).row();

        container.check("@chat.status", ChatConfig.status(), ChatConfig::status).row();
    }
}
