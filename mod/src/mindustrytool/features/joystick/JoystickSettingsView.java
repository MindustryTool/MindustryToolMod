package mindustrytool.features.joystick;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.freecamera.FreeCameraFeature;
import solim.core.BaseComponent;
import solim.layout.Card;
import solim.reactive.Readable;

/**
 * Modern declarative Solim settings view for the Virtual Joystick feature.
 */
public class JoystickSettingsView extends BaseComponent {

    private final JoystickFeature feature;

    public JoystickSettingsView(JoystickFeature feature) {
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
        Readable<String> sizeText = feature.sizeConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100));
        Readable<String> opacityText = feature.opacityConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 0.8f) * 100));

        FreeCameraFeature freeCam = FeatureManager.getFeature(FreeCameraFeature.class);

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).padding(unit(2)).children(() -> {
                    // Header Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(2)).center().children(() -> {
                            icon(Icon.move).size(unit(6)).color(Pal.accent);
                            column().growX().gap(unit(0.5f)).left().children(() -> {
                                row().growX().gap(unit(1.5f)).center().children(() -> {
                                    text(Core.bundle.get("feature.joystick.settings.title", "Virtual Joystick Settings"))
                                            .color(Pal.accent)
                                            .left();
                                    badge(Core.bundle.get("feature.joystick.badge.controls", "CONTROLS"))
                                            .color(WebStyles.Colors.PRIMARY)
                                            .textColor(WebStyles.Colors.PRIMARY_FG);
                                });
                                text(Core.bundle.get("feature.joystick.description", "Move your unit with an on-screen joystick while panning the camera freely across the map."))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .wrap()
                                        .left()
                                        .fontScale(0.9f);
                            });
                        });
                    });

                    // Sizing & Appearance Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(1.5f)).center().children(() -> {
                            icon(Icon.spray).size(unit(5)).color(Pal.accent);
                            text(Core.bundle.get("feature.joystick.settings.sizing", "Sizing & Appearance"))
                                    .color(Pal.accent)
                                    .growX()
                                    .left();
                        });

                        column().growX().gap(unit(2)).children(() -> {
                            row().growX().gap(unit(2)).center().children(() -> {
                                text(Core.bundle.get("feature.joystick.settings.size", "Joystick Size"))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .left();
                                spacer();
                                slider(feature.sizeConfig.signal(), 0.5f, 2.5f, 0.1f);
                                badge(sizeText)
                                        .color(WebStyles.Colors.SECONDARY)
                                        .textColor(WebStyles.Colors.SECONDARY_FG)
                                        .minWidth(unit(14));
                            });

                            row().growX().gap(unit(2)).center().children(() -> {
                                text(Core.bundle.get("feature.joystick.settings.opacity", "Opacity"))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .left();
                                spacer();
                                slider(feature.opacityConfig.signal(), 0.2f, 1.0f, 0.05f);
                                badge(opacityText)
                                        .color(WebStyles.Colors.SECONDARY)
                                        .textColor(WebStyles.Colors.SECONDARY_FG)
                                        .minWidth(unit(14));
                            });

                            checkbox(Core.bundle.get("feature.joystick.settings.show-handle", "Show Reposition Handle"),
                                    feature.showHandleConfig.signal()).growX();
                        });
                    });

                    // Camera Card (if FreeCameraFeature available)
                    if (freeCam != null) {
                        sectionCard().children(() -> {
                            row().growX().gap(unit(1.5f)).center().children(() -> {
                                icon(Icon.eye).size(unit(5)).color(Pal.accent);
                                text(Core.bundle.get("feature.joystick.settings.camera", "Camera Controls"))
                                        .color(Pal.accent)
                                        .growX()
                                        .left();
                            });

                            checkbox(Core.bundle.get("feature.free-camera.name", "Free Camera"),
                                    freeCam.enabled()).growX();
                        });
                    }

                    // Actions Card
                    sectionCard().children(() -> {
                        button(feature::resetPosition)
                                .style(WebStyles.outline())
                                .growX()
                                .padding(unit(2))
                                .children(() -> {
                                    row().gap(unit(1.5f)).center().children(() -> {
                                        icon(Icon.refresh).size(unit(4));
                                        text(Core.bundle.get("feature.joystick.settings.reset-position", "Reset Joystick Position"));
                                    });
                                });
                    });
                });
            });
        }).element();
    }
}
