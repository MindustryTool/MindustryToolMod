package mindustrytool.features.joystick;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustrytool.components.WebStyles;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.freecamera.FreeCameraFeature;
import solim.core.BaseComponent;

/**
 * Settings view for the Virtual Joystick feature: size and opacity sliders,
 * handle visibility toggle, and a reset position button.
 */
public class JoystickSettingsView extends BaseComponent {

    private final JoystickFeature feature;

    public JoystickSettingsView(JoystickFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(3)).children(() -> {
                    row().growX().gap(unit(2)).center().children(() -> {
                        text(Core.bundle.get("feature.joystick.settings.size")).left()
                                .color(WebStyles.Colors.GHOST_FG);

                        spacer();
                        slider(feature.sizeConfig.signal(), 0.5f, 2.5f, 0.1f);

                        row().width(unit(14)).children(() -> {
                            text(feature.sizeConfig.signal()
                                    .map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));
                        });
                    });

                    divider();

                    row().growX().gap(unit(2)).center().children(() -> {
                        text(Core.bundle.get("feature.joystick.settings.opacity")).left()
                                .color(WebStyles.Colors.GHOST_FG);

                        spacer();
                        slider(feature.opacityConfig.signal(), 0.2f, 1.0f, 0.05f);

                        row().width(unit(14)).children(() -> {
                            text(feature.opacityConfig.signal()
                                    .map(v -> String.format("%.0f%%", (v != null ? v : 0.8f) * 100)));
                        });
                    });

                    divider();

                    checkbox(Core.bundle.get("feature.joystick.settings.show-handle"),
                            feature.showHandleConfig.signal()).growX();

                    divider();

                    FreeCameraFeature freeCam = FeatureManager.getFeature(FreeCameraFeature.class);
                    if (freeCam != null) {
                        checkbox(Core.bundle.get("feature.free-camera.name", "Free Camera"),
                                freeCam.enabled()).growX();

                        divider();
                    }

                    button(feature::resetPosition)
                            .style(WebStyles.secondary())
                            .growX()
                            .padding(unit(2))
                            .children(() -> text(Core.bundle.get("feature.joystick.settings.reset-position")));
                });
            });
        }).element();
    }
}
