package mindustrytool.features.pathfinding;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.layout.Card;
import solim.reactive.Readable;

/**
 * Modern declarative Solim settings view for unit pathfinding visualization.
 */
public class PathfindingSettingsView extends BaseComponent {

    private final PathfindingFeature feature;

    private static final String[] COST_KEY_SUFFIXES = {
        "ground", "legs", "naval", "neoplasm", "none", "hover"
    };

    public PathfindingSettingsView(PathfindingFeature feature) {
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
        Readable<String> zoomText = feature.zoomThresholdConfig.signal().map(v -> {
            float zoom = v != null ? v : 0.5f;
            return zoom <= 0.01f
                    ? Core.bundle.get("feature.pathfinding.settings.off", "Off")
                    : String.format("%.1fx", zoom);
        });

        Readable<String> opacityText = feature.opacityConfig.signal().map(v -> {
            float op = v != null ? v : 1.0f;
            return Math.round(op * 100) + "%";
        });

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).padding(unit(2)).children(() -> {
                    // Header Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(2)).center().children(() -> {
                            icon(Icon.map).size(unit(6)).color(Pal.accent);
                            column().growX().gap(unit(0.5f)).left().children(() -> {
                                row().growX().gap(unit(1.5f)).center().children(() -> {
                                    text(Core.bundle.get("feature.pathfinding.settings.title", "Pathfinding Settings"))
                                            .color(Pal.accent)
                                            .left();
                                    badge(Core.bundle.get("feature.pathfinding.badge.navigation", "NAVIGATION"))
                                            .color(WebStyles.Colors.PRIMARY)
                                            .textColor(WebStyles.Colors.PRIMARY_FG);
                                });
                                text(Core.bundle.get("feature.pathfinding.description", "Visualize unit pathfinding flow fields on the map."))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .wrap()
                                        .left()
                                        .fontScale(0.9f);
                            });
                        });
                    });

                    // Display Layers Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(1.5f)).center().children(() -> {
                            icon(Icon.layers).size(unit(5)).color(Pal.accent);
                            text(Core.bundle.get("feature.pathfinding.settings.layers", "Display Layers"))
                                    .color(Pal.accent)
                                    .growX()
                                    .left();
                        });

                        column().growX().gap(unit(1.5f)).children(() -> {
                            checkbox(Core.bundle.get("feature.pathfinding.settings.draw-unit-path", "Draw Unit Paths"),
                                    feature.drawUnitPathConfig.signal()).growX();

                            checkbox(Core.bundle.get("feature.pathfinding.settings.draw-spawn-path", "Draw Spawn Point Paths"),
                                    feature.drawSpawnPathConfig.signal()).growX();

                            checkbox(Core.bundle.get("feature.pathfinding.settings.draw-allies", "Draw Ally Unit Paths"),
                                    feature.drawAlliesConfig.signal()).growX();
                        });
                    });

                    // Visual Styling Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(1.5f)).center().children(() -> {
                            icon(Icon.spray).size(unit(5)).color(Pal.accent);
                            text(Core.bundle.get("feature.pathfinding.settings.visual-styling", "Visual Styling"))
                                    .color(Pal.accent)
                                    .growX()
                                    .left();
                        });

                        column().growX().gap(unit(2)).children(() -> {
                            row().growX().gap(unit(2)).center().children(() -> {
                                text(Core.bundle.get("feature.pathfinding.settings.min-zoom", "Min Zoom"))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .left();
                                spacer();
                                slider(feature.zoomThresholdConfig.signal(), 0.0f, 5.0f, 0.1f);
                                badge(zoomText)
                                        .color(WebStyles.Colors.SECONDARY)
                                        .textColor(WebStyles.Colors.SECONDARY_FG)
                                        .minWidth(unit(14));
                            });

                            row().growX().gap(unit(2)).center().children(() -> {
                                text(Core.bundle.get("feature.pathfinding.settings.opacity", "Opacity"))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .left();
                                spacer();
                                slider(feature.opacityConfig.signal(), 0.05f, 1.0f, 0.05f);
                                badge(opacityText)
                                        .color(WebStyles.Colors.SECONDARY)
                                        .textColor(WebStyles.Colors.SECONDARY_FG)
                                        .minWidth(unit(14));
                            });
                        });
                    });

                    // Movement Cost Types Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(1.5f)).center().children(() -> {
                            icon(Icon.terrain).size(unit(5)).color(Pal.accent);
                            text(Core.bundle.get("feature.pathfinding.settings.cost-types", "Movement Cost Types"))
                                    .color(Pal.accent)
                                    .growX()
                                    .left();
                        });

                        wrap().gap(unit(1.5f)).children(() -> {
                            for (int i = 0; i < feature.costTypeConfigs.length; i++) {
                                final int index = i;
                                String bundleKey = "feature.pathfinding.settings.cost." + COST_KEY_SUFFIXES[index];
                                button(() -> feature.costTypeConfigs[index].set(!Boolean.TRUE.equals(feature.costTypeConfigs[index].signal().peek())))
                                        .style(WebStyles.filterChip())
                                        .checked(feature.costTypeConfigs[index].signal().map(v -> Boolean.TRUE.equals(v)))
                                        .padding(unit(1.5f))
                                        .children(() -> text(Core.bundle.get(bundleKey, COST_KEY_SUFFIXES[index])));
                            }
                        });
                    });

                    // Actions Card
                    sectionCard().children(() -> {
                        button(feature::resetToDefaults)
                                .style(WebStyles.outline())
                                .growX()
                                .padding(unit(2))
                                .children(() -> {
                                    row().gap(unit(1.5f)).center().children(() -> {
                                        icon(Icon.refresh).size(unit(4));
                                        text(Core.bundle.get("feature.pathfinding.settings.reset", "Reset to Defaults"));
                                    });
                                });
                    });
                });
            });
        }).element();
    }
}
