package mindustrytool.features.settings;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Scaling;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.IconUtils;

public class IconBrowserDialog extends BaseDialog {

    public IconBrowserDialog() {
        super("Icon");
        addCloseButton();
        closeOnBack();

        setup();
    }

    private void setup() {
        var containers = new Table();
        int width = 400;
        int cols = Math.max((int) (Core.graphics.getWidth() * 0.9 / (width + 20)), 1);
        String[] filter = { "" };

        Runnable build = () -> {
            containers.clear();
            int col = 0;

            for (var icon : IconUtils.iconcs) {
                if (!icon.getName().toLowerCase().contains(filter[0].toLowerCase())) {
                    continue;
                }

                containers.button(String.valueOf(icon.getValue()) + " " + icon.getName(), () -> {
                    Core.app.setClipboardText(String.valueOf(icon.getValue()));
                })
                        .width(width)
                        .scaling(Scaling.fill)
                        .growX()
                        .padRight(8)
                        .padBottom(8)
                        .labelAlign(Align.left)
                        .top()
                        .left();

                if (++col % cols == 0) {
                    containers.row();
                }
            }

        };

        cont.field(filter[0], Styles.defaultField, (t) -> {
            filter[0] = t;
            build.run();
        }).width(Math.min(Core.graphics.getWidth() * 0.9f, cols * (width + 20) - 20)).growX().row();

        build.run();
        cont.pane(containers).scrollX(false).top();
    }
}
