package mindustrytool.features.background;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.scene.Element;
import arc.util.Nullable;
import java.util.Locale;
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
            mediaSelectionCard();
            appearanceCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, Core.bundle.get("feature.background.badge.graphics", "GRAPHICS"));
    }

    private Component mediaSelectionCard() {
        Readable<String> currentFileText = feature.pathConfig.signal().map(path -> {
            String fileName = path != null && !path.trim().isEmpty()
                    ? path
                    : Core.bundle.get("feature.background.settings.no-file");
            return Core.bundle.format("feature.background.settings.current-file", fileName);
        });
        Readable<String> mediaFormat = feature.pathConfig.signal().map(BackgroundSettingsView::formatOf);

        return SettingCard.section(Icon.fileVideo,
                Core.bundle.get("feature.background.settings.media-selection", "Media Selection"), () -> {
                    text(Core.bundle.get("feature.background.settings.media-desc",
                            "Select a PNG or JPG image, or an H.264 MP4 video, to replace the main menu background."))
                            .props(color(WebStyles.Colors.GHOST_FG), textWrap(), growX(), left());

                    button(this::selectMedia)
                            .props(style(WebStyles.outline()), growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                            .children(() -> {
                                icon(Icon.file).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                                text(Core.bundle.get("feature.background.settings.select-media",
                                        "Select Background Media"));
                            });

                    row().props(growX(), gap(unit(1)), center()).children(() -> {
                        text(currentFileText).props(color(WebStyles.Colors.GHOST_FG), left(), growX());
                        badge(mediaFormat).props(color(WebStyles.Colors.PRIMARY));
                    });
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

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    scalingRow();

                    when(feature.pathConfig.signal()
                            .map(path -> BackgroundMediaType.of(path).isVideo()))
                            .thenDo(this::videoVolumeRow);
                });
    }

    private void scalingRow() {
        row().props(growX(), gap(unit(2)), center()).children(() -> {
            text(Core.bundle.get("feature.background.settings.scaling", "Scaling Mode"))
                    .props(color(WebStyles.Colors.GHOST_FG), left(), textWrap(true));

            spacer();
            row().props(gap(unit(1.5f))).children(() -> {
                button(Core.bundle.get("feature.background.settings.scaling.crop", "Crop to Fill"),
                        () -> feature.scalingConfig.set(BackgroundScaling.CROP))
                        .props(style(WebStyles.filterChipText()),
                                checked(feature.scalingConfig.signal().map(BackgroundScaling.CROP::equals)),
                                height(unit(8)), prefHeight(unit(8)), paddingX(unit(2.5f)));

                button(Core.bundle.get("feature.background.settings.scaling.stretch", "Stretch"),
                        () -> feature.scalingConfig.set(BackgroundScaling.STRETCH))
                        .props(style(WebStyles.filterChipText()),
                                checked(feature.scalingConfig.signal().map(BackgroundScaling.STRETCH::equals)),
                                height(unit(8)), prefHeight(unit(8)), paddingX(unit(2.5f)));
            });
        });
    }

    private void videoVolumeRow() {
        divider().props(color(WebStyles.Colors.SECTION_BORDER));

        SettingSlider.of(
                Core.bundle.get("feature.background.settings.video-volume", "Video Volume"),
                Core.bundle.get("feature.background.settings.video-volume-desc",
                        "Volume of the video's audio. Menu music plays again when this is 0%."),
                feature.videoVolumeConfig.signal(),
                0,
                100,
                5,
                feature.videoVolumeConfig.signal().map(v -> (v != null ? v : 100) + "%"));
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetBackground)
                    .props(style(WebStyles.outline()), growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                    .children(() -> {
                        icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text(Core.bundle.get("feature.background.settings.reset", "Reset Background"));
                    });
        });
    }

    private static String formatOf(@Nullable String path) {
        if (path == null || path.trim().isEmpty()) {
            return Core.bundle.get("feature.background.settings.media-type.default", "Default");
        }
        int dot = path.lastIndexOf('.');
        String extension = dot >= 0 && dot + 1 < path.length() ? path.substring(dot + 1) : "";
        return extension.isEmpty()
                ? Core.bundle.get("feature.background.settings.media-type.unknown", "Unknown Format")
                : extension.toUpperCase(Locale.ROOT);
    }

    private void selectMedia() {
        FileChooser.open("png", "jpg", "jpeg", "mp4").submit(file -> {
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
