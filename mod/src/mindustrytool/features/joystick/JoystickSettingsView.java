package mindustrytool.features.joystick;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.freecamera.FreeCameraFeature;
import solim.core.Component;

public class JoystickSettingsView extends Component {

    private final JoystickFeature feature;

    public JoystickSettingsView(JoystickFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        FreeCameraFeature freeCam = FeatureManager.getFeature(FreeCameraFeature.class);

        return SettingScaffold.build(() -> {
            headerCard();
            sizingCard();
            if (freeCam != null) {
                cameraCard(freeCam);
            }
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, Core.bundle.get("feature.joystick.badge.controls", "CONTROLS"));
    }

    private Component sizingCard() {
        return SettingCard.section(Icon.spray,
                Core.bundle.get("feature.joystick.settings.sizing", "Sizing & Appearance"), () -> {

                    SettingSlider.of(
                            Core.bundle.get("feature.joystick.settings.size", "Joystick Size"),
                            feature.sizeConfig.signal(),
                            0.5f,
                            2.5f,
                            0.1f,
                            feature.sizeConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));

                    SettingSlider.of(
                            Core.bundle.get("feature.joystick.settings.opacity", "Opacity"),
                            feature.opacityConfig.signal(),
                            0.2f,
                            1.0f,
                            0.05f,
                            feature.opacityConfig.signal()
                                    .map(v -> String.format("%.0f%%", (v != null ? v : 0.8f) * 100)));

                    divider().color(WebStyles.Colors.SECTION_BORDER);

                    checkbox(Core.bundle.get("feature.joystick.settings.show-handle", "Show Reposition Handle"),
                            feature.showHandleConfig.signal()).growX();
                });
    }

    private Component cameraCard(FreeCameraFeature freeCam) {
        return SettingCard.section(Icon.eye, Core.bundle.get("feature.joystick.settings.camera", "Camera Controls"),
                () -> {
                    checkbox(Core.bundle.get("feature.free-camera.name", "Free Camera"),
                            freeCam.enabled()).growX();
                });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetPosition)
                    .style(WebStyles.outline())
                    .growX()
                    .height(unit(11))
                    .gap(unit(2))
                    .children(() -> {
                        icon(Icon.refresh).size(unit(6));
                        text(Core.bundle.get("feature.joystick.settings.reset-position", "Reset Joystick Position"));
                    });
        });
    }
}
