package mindustrytool.features.timecontrol;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;

public class TimeControlSettingsView extends BaseComponent {

    private final TimeControlFeature feature;

    public TimeControlSettingsView(TimeControlFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().growX().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.time-control.settings.mode")).wrap().left();

                        spacer();
                        row().gap(unit(2)).children(() -> {
                            button(Core.bundle.get("feature.time-control.settings.mode.presets"),
                                    () -> feature.modeConfig.set(TimeControlFeature.MODE_PRESETS))
                                    .style(WebStyles.filterChipText())
                                    .checked(feature.modeConfig.signal()
                                            .map(TimeControlFeature.MODE_PRESETS::equals))
                                    .height(unit(8.5f));

                            button(Core.bundle.get("feature.time-control.settings.mode.slider"),
                                    () -> feature.modeConfig.set(TimeControlFeature.MODE_SLIDER))
                                    .style(WebStyles.filterChipText())
                                    .checked(feature.modeConfig.signal()
                                            .map(TimeControlFeature.MODE_SLIDER::equals))
                                    .height(unit(8.5f));
                        });
                    });

                    text(Core.bundle.get("feature.time-control.settings.mode.hint")).left().wrap().color(Color.lightGray);

                    divider();

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.time-control.settings.display-mode")).left();

                        spacer();
                        row().gap(unit(2)).children(() -> {
                            button(Core.bundle.get("feature.time-control.settings.display-mode.hud"),
                                    () -> feature.displayModeConfig.set(TimeControlFeature.DISPLAY_HUD))
                                    .style(WebStyles.filterChipText())
                                    .checked(feature.displayModeConfig.signal()
                                            .map(TimeControlFeature.DISPLAY_HUD::equals))
                                    .height(unit(8.5f));
                            button(Core.bundle.get("feature.time-control.settings.display-mode.popup"),
                                    () -> feature.displayModeConfig.set(TimeControlFeature.DISPLAY_POPUP))
                                    .style(WebStyles.filterChipText())
                                    .checked(feature.displayModeConfig.signal()
                                            .map(TimeControlFeature.DISPLAY_POPUP::equals))
                                    .height(unit(8.5f));
                        });
                    });

                    divider();

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.time-control.settings.scale")).left();

                        spacer();
                        slider(feature.scaleConfig.signal(), 0.5f, 1.5f, 0.1f);

                        row().width(unit(14)).children(() -> {
                            text(feature.scaleConfig.signal()
                                    .map(v -> (int) ((v != null ? v : 1f) * 100) + "%"));
                        });
                    });

                    checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle"),
                            feature.hideDragHandleConfig.signal()).growX();

                    divider();

                    text(Core.bundle.get("feature.time-control.settings.safety")).left().growX()
                            .color(Color.white);

                    divider();

                    button(Core.bundle.get("feature.time-control.settings.reset-speed"), feature::resetSpeed)
                            .style(WebStyles.outlineText()).growX();
                    button(Core.bundle.get("feature.time-control.settings.reset-position"), feature::resetPosition)
                            .style(WebStyles.outlineText()).growX();
                });
            });
        }).element();
    }
}
