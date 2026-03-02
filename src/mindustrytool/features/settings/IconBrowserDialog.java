package mindustrytool.features.settings;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.Scaling;
import mindustry.gen.Iconc;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

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
            var declaredFields = Iconc.class.getDeclaredFields();
            int col = 0;

            for (var field : declaredFields) {
                try {
                    field.setAccessible(true);
                    var icon = field.get(null);

                    if (icon.equals(Iconc.all)) {
                        continue;
                    }

                    if (icon instanceof String || icon instanceof Character) {
                        if (!field.getName().toLowerCase().contains(filter[0].toLowerCase())) {
                            continue;
                        }

                        containers.button(String.valueOf(icon) + " " + field.getName(), () -> {
                            Core.app.setClipboardText(String.valueOf(icon));
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
                } catch (Exception e) {
                    Log.err(e);
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
