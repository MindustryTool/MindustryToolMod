package mindustrytool.features.healthbar;

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
import solim.reactive.Signal;

/**
 * Modern declarative Solim settings view for Health Bar options.
 */
public class HealthBarSettingsView extends BaseComponent {

    private final HealthBarFeature feature;

    public HealthBarSettingsView(HealthBarFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().maxWidth(500f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).center().children(() -> {
                    headerCard();
                    targetLayersCard();
                    visualStylingCard();
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
                icon(Icon.defense).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("feature.health-bar.name", "Health Bar")
                        : "Health Bar")
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("TACTICAL").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle != null
                    ? Core.bundle.get("feature.health-bar.description",
                            "Display health bars above damaged units and buildings.")
                    : "Display health bars above damaged units and buildings.")
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component targetLayersCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.units).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("feature.health-bar.settings.section.targets", "Target Layers")
                        : "Target Layers")
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            checkboxRow(Core.bundle != null
                    ? Core.bundle.get("feature.health-bar.settings.show-friendly-units", "Show Friendly Units")
                    : "Show Friendly Units",
                    feature.showFriendlyUnitsConfig.signal());

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkboxRow(Core.bundle != null
                    ? Core.bundle.get("feature.health-bar.settings.show-enemy-units", "Show Enemy Units")
                    : "Show Enemy Units",
                    feature.showEnemyUnitsConfig.signal());

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkboxRow(Core.bundle != null
                    ? Core.bundle.get("feature.health-bar.settings.show-friendly-blocks", "Show Friendly Buildings")
                    : "Show Friendly Buildings",
                    feature.showFriendlyBlocksConfig.signal());

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkboxRow(Core.bundle != null
                    ? Core.bundle.get("feature.health-bar.settings.show-enemy-blocks", "Show Enemy Buildings")
                    : "Show Enemy Buildings",
                    feature.showEnemyBlocksConfig.signal());
        });
    }

    private Component checkboxRow(String label, Signal<Boolean> signal) {
        return row().growX().gap(unit(2)).center().children(() -> {
            text(label).color(Color.white).growX().left();
            spacer();
            checkbox("", signal);
        });
    }

    private Component visualStylingCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.image).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("feature.health-bar.settings.section.visual", "Visual Styling")
                        : "Visual Styling")
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            // Min Zoom
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle != null
                        ? Core.bundle.get("feature.health-bar.settings.min-zoom", "Min Zoom")
                        : "Min Zoom")
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.zoomThresholdConfig.signal(), 0f, 2f, 0.1f);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.zoomThresholdConfig.signal().map(v -> {
                        float val = v != null ? v : 0.5f;
                        return val <= 0.01f
                                ? (Core.bundle != null ? Core.bundle.get("feature.health-bar.settings.off", "Off") : "Off")
                                : String.format(Locale.US, "%.1fx", val);
                    })).color(WebStyles.Colors.SECONDARY);
                });
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Opacity
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle != null
                        ? Core.bundle.get("feature.health-bar.settings.opacity", "Opacity")
                        : "Opacity")
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.opacityConfig.signal(), 0f, 1f, 0.05f);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.opacityConfig.signal().map(v ->
                            String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)))
                            .color(WebStyles.Colors.SECONDARY);
                });
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Scale
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle != null
                        ? Core.bundle.get("feature.health-bar.settings.scale", "Scale")
                        : "Scale")
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.scaleConfig.signal(), 0.5f, 1.5f, 0.1f);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.scaleConfig.signal().map(v ->
                            String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)))
                            .color(WebStyles.Colors.SECONDARY);
                });
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Width
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle != null
                        ? Core.bundle.get("feature.health-bar.settings.width", "Width")
                        : "Width")
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.widthConfig.signal(), 0.5f, 2f, 0.1f);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.widthConfig.signal().map(v ->
                            String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)))
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
                        text(Core.bundle != null
                                ? Core.bundle.get("feature.health-bar.settings.reset", "Reset to Defaults")
                                : "Reset to Defaults");
                    });
        });
    }
}
