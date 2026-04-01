package mindustrytool.features.display.healthbar;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class HealthBarSettingsDialog extends BaseDialog {
    public HealthBarSettingsDialog() {
        super("@health-bar.settings.title");
        name = "healthBarSettingDialog";
        addCloseButton();
        buttons.button("@reset", Icon.refresh, () -> {
            HealthBarConfig.reset();
            rebuild();
        }).size(250, 64);
        shown(this::rebuild);
    }

    private void rebuild() {
        Table container = cont;
        container.clear();
        container.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        Slider zoomSlider = new Slider(0f, 2f, 0.1f, false);
        zoomSlider.setValue(HealthBarConfig.zoomThreshold);

        Label zoomValue = new Label(
                HealthBarConfig.zoomThreshold <= 0.01f ? "Off" : String.format("%.1fx", HealthBarConfig.zoomThreshold),
                Styles.outlineLabel);
        zoomValue.setColor(HealthBarConfig.zoomThreshold <= 0.01f ? Color.gray : Color.lightGray);

        Table zoomContent = new Table();
        zoomContent.touchable = Touchable.disabled;
        zoomContent.margin(3f, 33f, 3f, 33f);
        zoomContent.add("@health-bar.min-zoom", Styles.outlineLabel).left().growX();
        zoomContent.add(zoomValue).padLeft(10f).right();

        zoomSlider.changed(() -> {
            HealthBarConfig.zoomThreshold = zoomSlider.getValue();
            zoomValue.setText(HealthBarConfig.zoomThreshold <= 0.01f ? "@off"
                    : String.format("%.1fx", HealthBarConfig.zoomThreshold));
            zoomValue.setColor(HealthBarConfig.zoomThreshold <= 0.01f ? Color.gray : Color.lightGray);
            HealthBarConfig.save();
        });

        container.stack(zoomSlider, zoomContent).width(width).left().padTop(4f).row();

        Slider opacitySlider = new Slider(0f, 1f, 0.05f, false);
        opacitySlider.setValue(HealthBarConfig.opacity);

        Label opacityValue = new Label(
                String.format("%.0f%%", HealthBarConfig.opacity * 100),
                Styles.outlineLabel);
        opacityValue.setColor(Color.lightGray);

        Table opacityContent = new Table();
        opacityContent.touchable = Touchable.disabled;
        opacityContent.margin(3f, 33f, 3f, 33f);
        opacityContent.add("@opacity", Styles.outlineLabel).left().growX();
        opacityContent.add(opacityValue).padLeft(10f).right();

        opacitySlider.changed(() -> {
            HealthBarConfig.opacity = opacitySlider.getValue();
            opacityValue.setText(String.format("%.0f%%", HealthBarConfig.opacity * 100));
            HealthBarConfig.save();
        });

        container.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

        Slider scaleSlider = new Slider(0.5f, 1.5f, 0.1f, false);
        scaleSlider.setValue(HealthBarConfig.scale);

        Label scaleValue = new Label(
                String.format("%.0f%%", HealthBarConfig.scale * 100),
                Styles.outlineLabel);
        scaleValue.setColor(Color.lightGray);

        Table scaleContent = new Table();
        scaleContent.touchable = Touchable.disabled;
        scaleContent.margin(3f, 33f, 3f, 33f);
        scaleContent.add("@scale", Styles.outlineLabel).left().growX();
        scaleContent.add(scaleValue).padLeft(10f).right();

        scaleSlider.changed(() -> {
            HealthBarConfig.scale = scaleSlider.getValue();
            scaleValue.setText(String.format("%.0f%%", HealthBarConfig.scale * 100));
            HealthBarConfig.save();
        });

        container.stack(scaleSlider, scaleContent).width(width).left().padTop(4f).row();

        Slider widthSlider = new Slider(0.5f, 2.0f, 0.1f, false);
        widthSlider.setValue(HealthBarConfig.width);

        Label widthValue = new Label(
                String.format("%.0f%%", HealthBarConfig.width * 100),
                Styles.outlineLabel);
        widthValue.setColor(Color.lightGray);

        Table widthContent = new Table();
        widthContent.touchable = Touchable.disabled;
        widthContent.margin(3f, 33f, 3f, 33f);
        widthContent.add("@width", Styles.outlineLabel).left().growX();
        widthContent.add(widthValue).padLeft(10f).right();

        widthSlider.changed(() -> {
            HealthBarConfig.width = widthSlider.getValue();
            widthValue.setText(String.format("%.0f%%", HealthBarConfig.width * 100));
            HealthBarConfig.save();
        });

        container.stack(widthSlider, widthContent).width(width).left().padTop(4f).row();
    }
}
