package mindustrytool.features.display.pathfinding;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class PathfindingSettingsUI {
    private BaseDialog settingsDialog;

    public BaseDialog getDialog() {
        if (settingsDialog == null) {
            settingsDialog = new BaseDialog("@pathfinding.settings.title");
            settingsDialog.name = "pathfindingSettingDialog";
            settingsDialog.addCloseButton();
            settingsDialog.shown(this::rebuildSettings);
            settingsDialog.buttons.button("@reset", Icon.refresh, () -> {
                PathfindingConfig.setZoomThreshold(0.5f);
                rebuildSettings();
            }).size(250, 64);
        }
        return settingsDialog;
    }

    private void rebuildSettings() {
        Table settingsContainer = settingsDialog.cont;
        settingsContainer.clear();
        settingsContainer.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);
        float currentZoom = PathfindingConfig.getZoomThreshold();

        Slider zoomSlider = new Slider(0f, 5f, 0.1f, false);
        zoomSlider.setValue(currentZoom);

        Label zoomValueLabel = new Label(
                currentZoom <= 0.01f ? "@off" : String.format("%.1fx", currentZoom),
                Styles.outlineLabel);
        zoomValueLabel.setColor(currentZoom <= 0.01f ? Color.gray : Color.lightGray);

        Table zoomContent = new Table();
        zoomContent.touchable = Touchable.disabled;
        zoomContent.margin(3f, 33f, 3f, 33f);
        zoomContent.add("@health-bar.min-zoom", Styles.outlineLabel).left().growX();
        zoomContent.add(zoomValueLabel).padLeft(10f).right();

        zoomSlider.changed(() -> {
            float newZoomValue = zoomSlider.getValue();
            PathfindingConfig.setZoomThreshold(newZoomValue);
            zoomValueLabel.setText(newZoomValue <= 0.01f ? "@off" : String.format("%.1fx", newZoomValue));
            zoomValueLabel.setColor(newZoomValue <= 0.01f ? Color.gray : Color.lightGray);
        });

        settingsContainer.stack(zoomSlider, zoomContent).width(width).left().padTop(4f).row();

        Slider opacitySlider = new Slider(0f, 1f, 0.05f, false);
        opacitySlider.setValue(PathfindingConfig.getOpacity());

        Label opacityValue = new Label(
                String.format("%.0f%%", PathfindingConfig.getOpacity() * 100),
                Styles.outlineLabel);
        opacityValue.setColor(Color.lightGray);

        Table opacityContent = new Table();
        opacityContent.touchable = Touchable.disabled;
        opacityContent.margin(3f, 33f, 3f, 33f);
        opacityContent.add("@opacity", Styles.outlineLabel).left().growX();
        opacityContent.add(opacityValue).padLeft(10f).right();

        opacitySlider.changed(() -> {
            PathfindingConfig.setOpacity(opacitySlider.getValue());
            opacityValue.setText(String.format("%.0f%%", PathfindingConfig.getOpacity() * 100));
        });

        settingsContainer.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

        settingsContainer.check("@pathfinding.draw-unit-path", PathfindingConfig.isDrawUnitPath(), (checked) -> {
            PathfindingConfig.setDrawUnitPath(checked);
        }).left().row();

        settingsContainer
                .check("@pathfinding.draw-spawn-point-path", PathfindingConfig.isDrawSpawnPointPath(), (checked) -> {
                    PathfindingConfig.setDrawSpawnPointPath(checked);
                    rebuildSettings();
                }).left().row();

        if (PathfindingConfig.isDrawSpawnPointPath()) {
            Table costTable = new Table();
            costTable.left().defaults().left().padLeft(16);

            String[] costNames = { "@pathfinding.cost.ground", "@pathfinding.cost.legs", "@pathfinding.cost.water",
                    "@pathfinding.cost.neoplasm", "@pathfinding.cost.flat", "@pathfinding.cost.hover" };

            for (int i = 0; i < costNames.length; i++) {
                int index = i;
                costTable.check(costNames[i], PathfindingConfig.isCostTypeEnabled(index), c -> {
                    PathfindingConfig.setCostTypeEnabled(index, c);
                }).padBottom(4).row();
            }

            settingsContainer.add(costTable).left().row();
        }
    }
}