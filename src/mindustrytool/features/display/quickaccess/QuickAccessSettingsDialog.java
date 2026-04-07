package mindustrytool.features.display.quickaccess;

import arc.graphics.Color;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;

public class QuickAccessSettingsDialog extends BaseDialog {

    public QuickAccessSettingsDialog(QuickAccessFeature quickAccessHud) {
        super("@settings");

        name = "quickAccessSettingDialog";
        addCloseButton();
        closeOnBack();

        Table table = new Table();

        cont.pane(table)
                .center()
                .maxWidth(800)
                .grow();

        table.add("@settings").style(Styles.outlineLabel).left().pad(5).row();

        Table opacityTable = new Table();
        opacityTable.left();
        opacityTable.add("@opacity").left().padRight(10);
        Slider opacitySlider = new Slider(0.05f, 1f, 0.05f, false);
        opacitySlider.setValue(QuickAccessConfig.opacity());
        Label opacityLabel = new Label(String.format("%.0f%%", QuickAccessConfig.opacity() * 100));
        opacitySlider.changed(() -> {
            QuickAccessConfig.opacity(opacitySlider.getValue());
            opacityLabel.setText(String.format("%.0f%%", QuickAccessConfig.opacity() * 100));
            quickAccessHud.rebuild();
        });
        opacityTable.add(opacitySlider).width(200f);
        opacityTable.add(opacityLabel).padLeft(10);
        table.add(opacityTable).left().pad(5).row();

        Table scaleTable = new Table();
        scaleTable.left();
        scaleTable.add("@scale").left().padRight(10);
        Slider scaleSlider = new Slider(0.5f, 1.5f, 0.1f, false);
        scaleSlider.setValue(QuickAccessConfig.scale());
        Label scaleLabel = new Label(String.format("%.0f%%", QuickAccessConfig.scale() * 100));
        scaleSlider.changed(() -> {
            QuickAccessConfig.scale(scaleSlider.getValue());
            scaleLabel.setText(String.format("%.0f%%", QuickAccessConfig.scale() * 100));
            quickAccessHud.rebuild();
        });
        scaleTable.add(scaleSlider).width(200f);
        scaleTable.add(scaleLabel).padLeft(10);
        table.add(scaleTable).left().pad(5).row();

        Table widthTable = new Table();
        widthTable.left();
        widthTable.add("@width").left().padRight(10);
        Slider widthSlider = new Slider(0.5f, 2.0f, 0.1f, false);
        widthSlider.setValue(QuickAccessConfig.width());
        Label widthLabel = new Label(String.format("%.0f%%", QuickAccessConfig.width() * 100));
        widthSlider.changed(() -> {
            QuickAccessConfig.width(widthSlider.getValue());
            widthLabel.setText(String.format("%.0f%%", QuickAccessConfig.width() * 100));
            quickAccessHud.rebuild();
        });
        widthTable.add(widthSlider).width(200f);
        widthTable.add(widthLabel).padLeft(10);
        table.add(widthTable).left().pad(5).row();

        Table colsTable = new Table();
        colsTable.left();
        colsTable.add("@columns").left().padRight(10);
        Slider colsSlider = new Slider(1, 9, 1, false);
        colsSlider.setValue(QuickAccessConfig.cols());
        Label colsLabel = new Label(String.valueOf(QuickAccessConfig.cols()));
        colsSlider.changed(() -> {
            QuickAccessConfig.cols((int) colsSlider.getValue());
            colsLabel.setText(String.valueOf((int) colsSlider.getValue()));
            quickAccessHud.rebuild();
        });
        colsTable.add(colsSlider).width(200f);
        colsTable.add(colsLabel).padLeft(10);
        table.add(colsTable).left().pad(5).row();

        table.image().color(Color.gray).height(2).growX().pad(5).row();
        table.add("@features").style(Styles.outlineLabel).left().pad(5).row();

        Seq<Feature> features = FeatureManager.getInstance().getFeatures();
        for (Feature feature : features) {
            if (feature == quickAccessHud) {
                continue;
            }

            FeatureMetadata meta = feature.getMetadata();

            if (!meta.quickAccess()) {
                continue;
            }

            table.check(meta.name(), QuickAccessConfig.isFeatureVisible(meta.name()), visible -> {
                QuickAccessConfig.setFeatureVisible(meta.name(), visible);
                quickAccessHud.rebuild();
            }).fillX().top().left().pad(5).get().left();

            table.row();
        }

        table.button("@reset", () -> {
            QuickAccessConfig.x(0);
            QuickAccessConfig.y(0);
        }).fillX().top().left().pad(5).get().left();
    }
}
