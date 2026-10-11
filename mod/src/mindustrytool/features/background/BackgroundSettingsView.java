package mindustrytool.features.background;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.scene.Element;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.FileChooser;
import mindustrytool.Folders;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.layout.Card;
import solim.reactive.Readable;

/**
 * Modern declarative Solim settings view for main menu background customization.
 */
public class BackgroundSettingsView extends BaseComponent {

    private final BackgroundFeature feature;

    public BackgroundSettingsView(BackgroundFeature feature) {
        this.feature = feature;
    }

    private Card sectionCard() {
        return card()
                .padding(unit(2))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1f, WebStyles.Colors.SECTION_BORDER)
                .growX()
                .gap(unit(2));
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
                column().growX().gap(unit(2)).padding(unit(2)).children(() -> {
                    // Header Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(2)).center().children(() -> {
                            icon(Icon.image).size(unit(6)).color(Pal.accent);
                            column().growX().gap(unit(0.5f)).left().children(() -> {
                                row().growX().gap(unit(1.5f)).center().children(() -> {
                                    text(Core.bundle.get("feature.background.settings.title", "Background Settings"))
                                            .color(Pal.accent)
                                            .left();
                                    badge(Core.bundle.get("feature.background.badge.graphics", "GRAPHICS"))
                                            .color(WebStyles.Colors.PRIMARY)
                                            .textColor(WebStyles.Colors.PRIMARY_FG);
                                });
                                text(Core.bundle.get("feature.background.description", "Customize the game main menu background image."))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .wrap()
                                        .left()
                                        .fontScale(0.9f);
                            });
                        });
                    });

                    // Image Selection Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(1.5f)).center().children(() -> {
                            icon(Icon.image).size(unit(5)).color(Pal.accent);
                            text(Core.bundle.get("feature.background.settings.image-selection", "Image Selection"))
                                    .color(Pal.accent)
                                    .growX()
                                    .left();
                        });

                        text(Core.bundle.get("feature.background.settings.image-desc", "Select a custom PNG image to replace the main menu background."))
                                .color(WebStyles.Colors.GHOST_FG)
                                .wrap()
                                .left()
                                .fontScale(0.9f);

                        button(this::selectImage)
                                .style(WebStyles.outline())
                                .growX()
                                .padding(unit(2))
                                .children(() -> {
                                    row().gap(unit(1.5f)).center().children(() -> {
                                        icon(Icon.file).size(unit(4));
                                        text(Core.bundle.get("feature.background.settings.select-image", "Select Background Image"));
                                    });
                                });

                        text(currentFileText)
                                .color(WebStyles.Colors.GHOST_FG)
                                .fontScale(0.9f)
                                .left();
                    });

                    // Appearance Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(1.5f)).center().children(() -> {
                            icon(Icon.spray).size(unit(5)).color(Pal.accent);
                            text(Core.bundle.get("feature.background.settings.appearance", "Appearance"))
                                    .color(Pal.accent)
                                    .growX()
                                    .left();
                        });

                        row().growX().gap(unit(2)).center().children(() -> {
                            text(Core.bundle.get("feature.background.settings.opacity", "Opacity"))
                                    .color(WebStyles.Colors.GHOST_FG)
                                    .left();
                            spacer();
                            slider(feature.opacityConfig.signal(), 5, 100, 5);
                            badge(opacityText)
                                    .color(WebStyles.Colors.SECONDARY)
                                    .textColor(WebStyles.Colors.SECONDARY_FG)
                                    .minWidth(unit(14));
                        });
                    });

                    // Actions Card
                    sectionCard().children(() -> {
                        button(feature::resetBackground)
                                .style(WebStyles.outline())
                                .growX()
                                .padding(unit(2))
                                .children(() -> {
                                    row().gap(unit(1.5f)).center().children(() -> {
                                        icon(Icon.refresh).size(unit(4));
                                        text(Core.bundle.get("feature.background.settings.reset", "Reset Background"));
                                    });
                                });
                    });
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
