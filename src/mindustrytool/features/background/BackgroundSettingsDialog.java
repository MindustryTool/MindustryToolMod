package mindustrytool.features.background;

import arc.Core;
import arc.files.Fi;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Main;

public class BackgroundSettingsDialog extends BaseDialog {
    public BackgroundSettingsDialog(BackgroundFeature backgroundFeature) {
        super("Background Settings");

        addCloseButton();
        name = "backgroundSettingDialog";

        Table table = cont;
        table.button("Select Background Image", Icon.file, () -> {
            Vars.platform.showFileChooser(true, "png", file -> {
                try {
                    if (file != null) {
                        Fi dest = Main.backgroundsDir.child(file.name());
                        file.copyTo(dest);
                        Core.settings.put(BackgroundFeature.SETTING_KEY, dest.name());
                        Core.settings.forceSave();
                        backgroundFeature.applyBackground(dest);
                    }
                } catch (Exception e) {
                    Vars.ui.showException("Failed to apply background", e);
                }
            });
        }).size(250, 60);

        table.row();
        table.slider(5, 100, 5, Core.settings.getInt(BackgroundFeature.SETTING_OPACITY_KEY, 100), value -> {
            Core.settings.put(BackgroundFeature.SETTING_OPACITY_KEY, (int) value);
        }).width(180).padTop(10);
        table.label(() -> Core.settings.getInt(BackgroundFeature.SETTING_OPACITY_KEY, 100) + "%").padTop(10)
                .padLeft(10);
    }
}
