package mindustrytool.features.camerazoom;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import solim.core.BaseComponent;

/**
 * Declarative Solim settings view for camera zoom limits.
 */
public class CameraZoomSettingsView extends BaseComponent {

    private final CameraZoomFeature feature;

    public CameraZoomSettingsView(CameraZoomFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.camera-zoom.settings.min-zoom")).left();
                        spacer();
                        slider(feature.minZoomConfig.signal(),
                                CameraZoomFeature.MIN_SLIDER_MIN,
                                CameraZoomFeature.MIN_SLIDER_MAX,
                                CameraZoomFeature.MIN_SLIDER_STEP);
                        row().width(unit(14)).children(() -> {
                            text(feature.minZoomConfig.signal().map(v -> String.format("%.1fx",
                                    v != null ? v : CameraZoomFeature.DEFAULT_MIN_ZOOM)));
                        });
                    });

                    divider();

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.camera-zoom.settings.max-zoom")).left();
                        spacer();
                        slider(feature.maxZoomConfig.signal(),
                                CameraZoomFeature.MAX_SLIDER_MIN,
                                CameraZoomFeature.MAX_SLIDER_MAX,
                                CameraZoomFeature.MAX_SLIDER_STEP);
                        row().width(unit(14)).children(() -> {
                            text(feature.maxZoomConfig.signal().map(v -> String.format("%.1fx",
                                    v != null ? v : CameraZoomFeature.DEFAULT_MAX_ZOOM)));
                        });
                    });
                });
            });
        }).element();
    }
}
