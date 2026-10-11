package mindustrytool.features.screenshot;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Card;

/**
 * Modern declarative Solim settings view for screenshot capture options.
 */
public class ScreenshotSettingsView extends BaseComponent {

    private final ScreenshotFeature feature;

    public ScreenshotSettingsView(ScreenshotFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().maxWidth(500f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).center().children(() -> {
                    headerCard();
                    optionsCard();
                    actionsCard();
                });
            });
        }).element();
    }

    private Card sectionCard() {
        return card()
                .growX()
                .padding(unit(2.5f))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1f, WebStyles.Colors.SECTION_BORDER)
                .gap(unit(2));
    }

    private Component headerCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(FileIcon.of("camera.png", Icon.image)).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.screenshot.name"))
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("MEDIA").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle.get("feature.screenshot.description"))
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component optionsCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.settings).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.screenshot.settings.section.options"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.screenshot.settings.include-ui"))
                        .color(Color.white)
                        .growX()
                        .left();
                spacer();
                checkbox("", feature.includeUiConfig.signal());
            });
        });
    }

    private Component actionsCard() {
        return sectionCard().children(() -> {
            button(feature::capture)
                    .style(WebStyles.primary())
                    .growX()
                    .height(unit(11))
                    .gap(unit(2))
                    .children(() -> {
                        icon(FileIcon.of("camera.png", Icon.image)).size(unit(6));
                        text(Core.bundle.get("feature.screenshot.settings.capture-now"));
                    });

            button(this::openScreenshotsFolder)
                    .style(WebStyles.outline())
                    .growX()
                    .height(unit(11))
                    .gap(unit(2))
                    .children(() -> {
                        icon(Icon.link).size(unit(6));
                        text(Core.bundle.get("feature.screenshot.settings.open-folder"));
                    });
        });
    }

    private void openScreenshotsFolder() {
        try {
            Fi directory = Vars.screenshotDirectory;
            if (!Core.app.openFolder(directory.absolutePath())) {
                Core.app.setClipboardText(directory.absolutePath());
                Vars.ui.showInfoFade(Core.bundle.get("feature.screenshot.success.copied"));
            }
        } catch (Exception e) {
            Vars.ui.showException(e);
        }
    }
}
