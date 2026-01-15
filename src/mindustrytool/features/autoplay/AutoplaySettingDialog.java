package mindustrytool.features.autoplay;

import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.features.autoplay.tasks.AutoplayTask;

public class AutoplaySettingDialog extends BaseDialog {
    private final AutoplayFeature feature;

    public AutoplaySettingDialog(AutoplayFeature feature) {
        super("Autoplay Settings");
        this.feature = feature;
        addCloseButton();
        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.pane(t -> {
            t.top();

            for (int i = 0; i < feature.getTasks().size; i++) {
                AutoplayTask task = feature.getTasks().get(i);
                int index = i;

                t.table(Styles.black6, container -> {
                    container.left();

                    container.table(header -> {
                        header.left();
                        // Toggle
                        header.check("", task.isEnabled(), task::setEnabled).padRight(10);

                        // Name
                        header.label(() -> task.getName()).growX().left();

                        // Reorder buttons
                        if (index > 0) {
                            header.button(Icon.up, Styles.clearNonei, () -> {
                                feature.getTasks().swap(index, index - 1);
                                rebuild();
                            }).size(40);
                        } else {
                            header.add().size(40);
                        }

                        if (index < feature.getTasks().size - 1) {
                            header.button(Icon.down, Styles.clearNonei, () -> {
                                feature.getTasks().swap(index, index + 1);
                                rebuild();
                            }).size(40);
                        } else {
                            header.add().size(40);
                        }
                    }).growX().row();

                    // Settings (if any)
                    if (task.hasSettings()) {
                        container.image().color(mindustry.graphics.Pal.gray).growX().height(2).pad(5).row();
                        container.table(settings -> {
                            settings.left();
                            task.buildSettings(settings);
                        }).growX().pad(5).left();
                    }

                }).growX().pad(5).row();
            }
        }).grow();
    }
}
