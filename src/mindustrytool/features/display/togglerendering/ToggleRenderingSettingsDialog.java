package mindustrytool.features.display.togglerendering;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

public class ToggleRenderingSettingsDialog extends BaseDialog {
    public ToggleRenderingSettingsDialog() {
        super("@toggle-rendering.settings.title");
        name = "toggleRenderingSettingDialog";
        addCloseButton();
        buttons.button("@reset", Icon.refresh, () -> {
            ToggleRenderingConfig.reset();
            rebuild();
        }).size(250, 64);
        shown(this::rebuild);
    }

    private void rebuild() {
        Table container = cont;
        container.clear();
        container.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        container.check("@toggle-rendering.draw-blocks", ToggleRenderingConfig.drawBlocks, v -> {
            ToggleRenderingConfig.drawBlocks = v;
            ToggleRenderingConfig.save();
        }).width(width).left().row();

        container.check("@toggle-rendering.draw-units-allies", ToggleRenderingConfig.drawUnitsAllies, v -> {
            ToggleRenderingConfig.drawUnitsAllies = v;
            ToggleRenderingConfig.save();
        }).width(width).left().row();

        container.check("@toggle-rendering.draw-units-enemies", ToggleRenderingConfig.drawUnitsEnemies, v -> {
            ToggleRenderingConfig.drawUnitsEnemies = v;
            ToggleRenderingConfig.save();
        }).width(width).left().row();
    }
}
