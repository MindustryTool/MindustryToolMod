package mindustrytool.features.teamresource;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.ui.Styles;
import solim.core.BaseComponent;

/**
 * Declarative Solim settings view for Team Resource Tracker.
 */
public class TeamResourceSettingsView extends BaseComponent {
    private final TeamResourceFeature feature;

    public TeamResourceSettingsView(TeamResourceFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    // Opacity Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("team-resources.opacity", "Opacity")).left();
                        spacer();
                        slider(feature.opacityConfig.signal(), 0.1f, 1.0f, 0.05f);
                        row().width(unit(10)).children(() -> {
                            text(feature.opacityConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));
                        });
                    });

                    // Scale Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("team-resources.size", "Scale")).left();
                        spacer();
                        slider(feature.scaleConfig.signal(), 0.3f, 2.0f, 0.05f);
                        row().width(unit(10)).children(() -> {
                            text(feature.scaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"));
                        });
                    });

                    // Overlay Width Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("team-resources.width", "Overlay Width")).left();
                        spacer();
                        slider(feature.overlayWidthConfig.signal(), 0.15f, 1.0f, 0.05f);
                        row().width(unit(10)).children(() -> {
                            text(feature.overlayWidthConfig.signal().map(v -> Math.round((v != null ? v : 0.28f) * 100) + "%"));
                        });
                    });

                    divider();

                    // Display Checkbox Options
                    checkbox(Core.bundle.get("team-resources.show-items", "Show Core Items"), feature.showItemsConfig.signal()).growX();
                    checkbox(Core.bundle.get("team-resources.show-units", "Show Unit Counts"), feature.showUnitsConfig.signal()).growX();
                    checkbox(Core.bundle.get("team-resources.show-power", "Show Power Graph"), feature.showPowerConfig.signal()).growX();
                    checkbox(Core.bundle.get("team-resources.show-stored-power", "Show Stored Power"), feature.showStoredPowerConfig.signal()).growX();
                    checkbox(Core.bundle.get("team-resources.hide-background", "Hide Background"), feature.hideBackgroundConfig.signal()).growX();
                    checkbox(Core.bundle.get("team-resources.always-show-flow-rate", "Always Show Flow Rate"), feature.alwaysShowFlowRateConfig.signal()).growX();

                    divider();

                    // Reset Position Button
                    button(Core.bundle.get("feature.quick-access.settings.reset-position", "Reset Position"), feature::resetPosition)
                            .style(Styles.defaultb).growX();

                    // Reset Defaults Button
                    button(Core.bundle.get("team-resources.reset-to-defaults", "Reset to Defaults"), feature::resetToDefaults)
                            .style(Styles.defaultb).growX();
                });
            });
        }).element();
    }
}
