package mindustrytool.features.screenshot;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;

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
        return SettingScaffold.build(() -> {
            headerCard();
            optionsCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "MEDIA");
    }

    private Component optionsCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.screenshot.settings.section.options"), () -> {
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
        return SettingCard.card().children(() -> {
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
