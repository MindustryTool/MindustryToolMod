package mindustrytool.features.display.progress;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class ProgressDisplaySettingsDialog extends BaseDialog {
    public ProgressDisplaySettingsDialog() {
        super("@progress.settings.title");
        name = "progressSettingDialog";
        addCloseButton();
        shown(this::rebuildSettings);
        buttons.button("@reset", Icon.refresh, () -> {
            ProgressConfig.reset();
            rebuildSettings();
        }).size(250, 64);
    }

    private void rebuildSettings() {
        Table settingsContainer = cont;
        settingsContainer.clear();
        settingsContainer.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        Slider opacitySlider = new Slider(0f, 1f, 0.05f, false);
        opacitySlider.setValue(ProgressConfig.opacity);

        Label opacityValue = new Label(
                String.format("%.0f%%", ProgressConfig.opacity * 100),
                Styles.outlineLabel);
        opacityValue.setColor(Color.lightGray);

        Table opacityContent = new Table();
        opacityContent.touchable = Touchable.disabled;
        opacityContent.margin(3f, 33f, 3f, 33f);
        opacityContent.add("@opacity", Styles.outlineLabel).left().growX();
        opacityContent.add(opacityValue).padLeft(10f).right();

        opacitySlider.changed(() -> {
            ProgressConfig.opacity = opacitySlider.getValue();
            opacityValue.setText(String.format("%.0f%%", ProgressConfig.opacity * 100));
            ProgressConfig.save();
        });

        settingsContainer.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

        Slider scaleSlider = new Slider(0.5f, 1.5f, 0.1f, false);
        scaleSlider.setValue(ProgressConfig.scale);

        Label scaleValue = new Label(
                String.format("%.0f%%", ProgressConfig.scale * 100),
                Styles.outlineLabel);
        scaleValue.setColor(Color.lightGray);

        Table scaleContent = new Table();
        scaleContent.touchable = Touchable.disabled;
        scaleContent.margin(3f, 33f, 3f, 33f);
        scaleContent.add("@scale", Styles.outlineLabel).left().growX();
        scaleContent.add(scaleValue).padLeft(10f).right();

        scaleSlider.changed(() -> {
            ProgressConfig.scale = scaleSlider.getValue();
            scaleValue.setText(String.format("%.0f%%", ProgressConfig.scale * 100));
            ProgressConfig.save();
        });

        settingsContainer.stack(scaleSlider, scaleContent).width(width).left().padTop(4f).row();

        Slider widthSlider = new Slider(0.5f, 2.0f, 0.1f, false);
        widthSlider.setValue(ProgressConfig.width);

        Label widthValue = new Label(
                String.format("%.0f%%", ProgressConfig.width * 100),
                Styles.outlineLabel);
        widthValue.setColor(Color.lightGray);

        Table widthContent = new Table();
        widthContent.touchable = Touchable.disabled;
        widthContent.margin(3f, 33f, 3f, 33f);
        widthContent.add("@width", Styles.outlineLabel).left().growX();
        widthContent.add(widthValue).padLeft(10f).right();

        widthSlider.changed(() -> {
            ProgressConfig.width = widthSlider.getValue();
            widthValue.setText(String.format("%.0f%%", ProgressConfig.width * 100));
            ProgressConfig.save();
        });

        settingsContainer.stack(widthSlider, widthContent).width(width).left().padTop(4f).row();
    }
}
