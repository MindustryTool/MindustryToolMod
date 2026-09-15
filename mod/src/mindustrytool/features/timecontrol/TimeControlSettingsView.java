package mindustrytool.features.timecontrol;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.ui.Styles;
import solim.core.BaseComponent;

public class TimeControlSettingsView extends BaseComponent {

    private final TimeControlFeature feature;

    public TimeControlSettingsView(TimeControlFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.time-control.settings.mode")).left();

                        spacer();
                        row().gap(unit(2)).children(() -> {
                            button(Core.bundle.get("feature.time-control.settings.mode.presets"),
                                    () -> feature.modeConfig.set(TimeControlFeature.MODE_PRESETS))
                                            .style(Styles.togglet)
                                            .checked(feature.modeConfig.signal()
                                                    .map(TimeControlFeature.MODE_PRESETS::equals))
                                            .height(unit(8.5f))
                                            .margin(unit(1.5f), unit(4), unit(1.5f), unit(4));
                            button(Core.bundle.get("feature.time-control.settings.mode.slider"),
                                    () -> feature.modeConfig.set(TimeControlFeature.MODE_SLIDER))
                                            .style(Styles.togglet)
                                            .checked(feature.modeConfig.signal()
                                                    .map(TimeControlFeature.MODE_SLIDER::equals))
                                            .height(unit(8.5f))
                                            .margin(unit(1.5f), unit(4), unit(1.5f), unit(4));
                        });
                    });

                    text(Core.bundle.get("feature.time-control.settings.mode.hint")).left().color(Color.lightGray);

                    divider();

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.time-control.settings.display-mode")).left();

                        spacer();
                        row().gap(unit(2)).children(() -> {
                            button(Core.bundle.get("feature.time-control.settings.display-mode.hud"),
                                    () -> feature.displayModeConfig.set(TimeControlFeature.DISPLAY_HUD))
                                            .style(Styles.togglet)
                                            .checked(feature.displayModeConfig.signal()
                                                    .map(TimeControlFeature.DISPLAY_HUD::equals))
                                            .height(unit(8.5f))
                                            .margin(unit(1.5f), unit(4), unit(1.5f), unit(4));
                            button(Core.bundle.get("feature.time-control.settings.display-mode.popup"),
                                    () -> feature.displayModeConfig.set(TimeControlFeature.DISPLAY_POPUP))
                                            .style(Styles.togglet)
                                            .checked(feature.displayModeConfig.signal()
                                                    .map(TimeControlFeature.DISPLAY_POPUP::equals))
                                            .height(unit(8.5f))
                                            .margin(unit(1.5f), unit(4), unit(1.5f), unit(4));
                        });
                    });

                    divider();

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.time-control.settings.scale")).left();

                        spacer();
                        slider(feature.scaleConfig.signal(), 0.5f, 1.5f, 0.1f);

                        row().width(unit(14)).children(() -> {
                            text(feature.scaleConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));
                        });
                    });

                    checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle"),
                            feature.hideDragHandleConfig.signal()).growX();

                    divider();

                    text(Core.bundle.get("feature.time-control.settings.safety")).left().growX()
                            .color(Color.white);

                    divider();

                    button(Core.bundle.get("feature.time-control.settings.reset-speed"), feature::resetSpeed)
                            .style(Styles.defaultb).growX();
                    button(Core.bundle.get("feature.time-control.settings.reset-position"), feature::resetPosition)
                            .style(Styles.defaultb).growX();
                });
            });
        }).element();
    }
}
