package mindustrytool.features.godmode;

import arc.scene.ui.ButtonGroup;
import arc.scene.ui.TextButton;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class GodModeSettingsDialog extends BaseDialog {
    public GodModeSettingsDialog(GodModeFeature feature) {
        super("@feature.god-mode.settings");
        addCloseButton();

        cont.table(t -> {
            t.add("Provider: ").padRight(10);

            ButtonGroup<TextButton> group = new ButtonGroup<>();

            t.button("Internal", Styles.togglet, () -> {
                feature.useJS = false;
                feature.provider = feature.internal;
                feature.rebuild();
            }).group(group).checked(!feature.useJS).disabled(b -> !feature.internal.isAvailable()).size(120, 50);

            t.button("JS", Styles.togglet, () -> {
                feature.useJS = true;
                feature.provider = feature.js;
                feature.rebuild();
            }).group(group).checked(feature.useJS).disabled(b -> !feature.js.isAvailable()).size(120, 50);

        }).row();
    }
}
