package mindustrytool.features.screenshot;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.scene.Element;
import mindustry.Vars;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;

/**
 * Tiny declarative Solim settings view for screenshot capture mode.
 */
public class ScreenshotSettingsView extends BaseComponent {

    private final ScreenshotFeature feature;

    public ScreenshotSettingsView(ScreenshotFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().grow().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    checkbox(Core.bundle.get("feature.screenshot.settings.include-ui"),
                            feature.includeUiConfig.signal()).growX();

                    divider();

                    button(Core.bundle.get("feature.screenshot.settings.capture-now"), feature::capture)
                            .style(WebStyles.primary())
                            .growX()
                            .height(unit(11));

                    button(Core.bundle.get("feature.screenshot.settings.open-folder"), this::openScreenshotsFolder)
                            .style(WebStyles.outline())
                            .growX()
                            .height(unit(11));
                });
            });
        }).element();
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
