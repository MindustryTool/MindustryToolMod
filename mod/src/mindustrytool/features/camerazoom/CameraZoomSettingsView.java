package mindustrytool.features.camerazoom;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import java.util.Locale;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Card;

/**
 * Modern declarative Solim settings view for camera zoom limits.
 */
public class CameraZoomSettingsView extends BaseComponent {

    private final CameraZoomFeature feature;

    public CameraZoomSettingsView(CameraZoomFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().maxWidth(500f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).center().children(() -> {
                    headerCard();
                    zoomLimitsCard();
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
                icon(Icon.zoom).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.camera-zoom.name"))
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("GRAPHICS").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle.get("feature.camera-zoom.description"))
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component zoomLimitsCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.zoom).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.camera-zoom.settings.limits"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            // Min Zoom
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.camera-zoom.settings.min-zoom"))
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.minZoomConfig.signal(),
                        CameraZoomFeature.MIN_SLIDER_MIN,
                        CameraZoomFeature.MIN_SLIDER_MAX,
                        CameraZoomFeature.MIN_SLIDER_STEP);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.minZoomConfig.signal().map(v -> String.format(Locale.US, "%.1fx",
                            v != null ? v : CameraZoomFeature.DEFAULT_MIN_ZOOM)))
                            .color(WebStyles.Colors.SECONDARY);
                });
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Max Zoom
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.camera-zoom.settings.max-zoom"))
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.maxZoomConfig.signal(),
                        CameraZoomFeature.MAX_SLIDER_MIN,
                        CameraZoomFeature.MAX_SLIDER_MAX,
                        CameraZoomFeature.MAX_SLIDER_STEP);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.maxZoomConfig.signal().map(v -> String.format(Locale.US, "%.1fx",
                            v != null ? v : CameraZoomFeature.DEFAULT_MAX_ZOOM)))
                            .color(WebStyles.Colors.SECONDARY);
                });
            });
        });
    }

    private Component actionsCard() {
        return sectionCard().children(() -> {
            button(feature::resetToDefaults)
                    .style(WebStyles.outline())
                    .growX()
                    .height(unit(11))
                    .gap(unit(2))
                    .children(() -> {
                        icon(Icon.refresh).size(unit(6));
                        text(Core.bundle.get("feature.camera-zoom.settings.reset"));
                    });
        });
    }
}
