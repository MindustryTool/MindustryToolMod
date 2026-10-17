package mindustrytool.features.background;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;

import arc.Core;
import arc.files.Fi;
import arc.scene.Element;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.FileChooser;
import mindustrytool.Folders;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.reactive.Readable;

public class BackgroundSettingsView extends Component {

    private final BackgroundFeature feature;

    public BackgroundSettingsView(BackgroundFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            imageSelectionCard();
            appearanceCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, Core.bundle.get("feature.background.badge.graphics", "GRAPHICS"));
    }

    private Component imageSelectionCard() {
        Readable<String> currentFileText = feature.pathConfig.signal().map(path -> {
            String fileName = (path != null && !path.trim().isEmpty())
                    ? path
                    : Core.bundle.get("feature.background.settings.no-file");
            return Core.bundle.format("feature.background.settings.current-file", fileName);
        });

        return SettingCard.section(Icon.image,
                Core.bundle.get("feature.background.settings.image-selection", "Image Selection"), () -> {
                    text(Core.bundle.get("feature.background.settings.image-desc",
                            "Select a custom PNG image to replace the main menu background."))
                            .color(WebStyles.Colors.GHOST_FG)
                            .wrap()
                            .growX()
                            .left();

                    button(this::selectImage)
                            .style(WebStyles.outline())
                            .props(growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                            .children(() -> {
                                icon(Icon.file).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                                text(Core.bundle.get("feature.background.settings.select-image",
                                        "Select Background Image"));
                            });

                    text(currentFileText)
                            .color(WebStyles.Colors.GHOST_FG)
                            .left();
                });
    }

    private Component appearanceCard() {
        return SettingCard.section(Icon.spray, Core.bundle.get("feature.background.settings.appearance", "Appearance"),
                () -> {
                    SettingSlider.of(
                            Core.bundle.get("feature.background.settings.opacity", "Opacity"),
                            feature.opacityConfig.signal(),
                            5,
                            100,
                            5,
                            feature.opacityConfig.signal().map(v -> (v != null ? v : 100) + "%"));
                });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetBackground)
                    .style(WebStyles.outline())
                    .props(growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                    .children(() -> {
                        icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text(Core.bundle.get("feature.background.settings.reset", "Reset Background"));
                    });
        });
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
