package mindustrytool.features.camerazoom;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import java.util.Locale;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;

public class CameraZoomSettingsView extends Component {

    private final CameraZoomFeature feature;

    public CameraZoomSettingsView(CameraZoomFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            zoomLimitsCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "GRAPHICS");
    }

    private Component zoomLimitsCard() {
        return SettingCard.section(Icon.zoom, Core.bundle.get("feature.camera-zoom.settings.limits"), () -> {

            SettingSlider.of(
                    Core.bundle.get("feature.camera-zoom.settings.min-zoom"),
                    feature.minZoomConfig.signal(),
                    CameraZoomFeature.MIN_SLIDER_MIN,
                    CameraZoomFeature.MIN_SLIDER_MAX,
                    CameraZoomFeature.MIN_SLIDER_STEP,
                    feature.minZoomConfig.signal().map(v -> String.format(Locale.US, "%.1fx",
                            v != null ? v : CameraZoomFeature.DEFAULT_MIN_ZOOM)));

            SettingSlider.of(
                    Core.bundle.get("feature.camera-zoom.settings.max-zoom"),
                    feature.maxZoomConfig.signal(),
                    CameraZoomFeature.MAX_SLIDER_MIN,
                    CameraZoomFeature.MAX_SLIDER_MAX,
                    CameraZoomFeature.MAX_SLIDER_STEP,
                    feature.maxZoomConfig.signal().map(v -> String.format(Locale.US, "%.1fx",
                            v != null ? v : CameraZoomFeature.DEFAULT_MAX_ZOOM)));
        });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetToDefaults)
                    .props(style(WebStyles.outline()), growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                    .children(() -> {
                        icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text(Core.bundle.get("feature.camera-zoom.settings.reset"));
                    });
        });
    }
}
