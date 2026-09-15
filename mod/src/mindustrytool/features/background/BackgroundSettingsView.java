package mindustrytool.features.background;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.Vars;
import mindustry.ui.FileChooser;
import mindustry.ui.Styles;
import mindustrytool.Folders;
import solim.core.BaseComponent;
import solim.signal.Readable;

public class BackgroundSettingsView extends BaseComponent {

    private final BackgroundFeature feature;

    public BackgroundSettingsView(BackgroundFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<String> currentFileText = feature.pathConfig.signal().map(path -> {
            String fileName = (path != null && !path.trim().isEmpty())
                    ? path
                    : Core.bundle.get("feature.background.settings.no-file");
            return Core.bundle.format("feature.background.settings.current-file", fileName);
        });

        Readable<String> opacityText = feature.opacityConfig.signal().map(v -> (v != null ? v : 100) + "%");

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    button(Core.bundle.get("feature.background.settings.select-image"), this::selectImage)
                            .style(Styles.defaultb)
                            .growX();

                    text(currentFileText)
                            .color(Color.lightGray)
                            .fontScale(0.9f)
                            .left();

                    divider();

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.background.settings.opacity")).left();

                        spacer();

                        slider(feature.opacityConfig.signal(), 5, 100, 5);

                        row().width(unit(14)).children(() -> {
                            text(opacityText);
                        });
                    });

                    divider();

                    button(Core.bundle.get("feature.background.settings.reset"), feature::resetBackground)
                            .style(Styles.defaultb)
                            .growX();
                });
            });
        }).element();
    }

    private void selectImage() {
        FileChooser.open("png").submit(file -> {
            try {
                if (file != null) {
                    Fi dest = Folders.backgroundsDir.child(file.name());
                    file.copyTo(dest);
                    feature.pathConfig.set(dest.name());
                }
            } catch (Exception e) {
                Vars.ui.showException(Core.bundle.get("feature.background.error.apply"), e);
            }
        });
    }
}
