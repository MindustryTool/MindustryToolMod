package mindustrytool.features.teamresource;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.layout.Card;

/**
 * Modern declarative Solim settings view for Team Resource Tracker.
 */
public class TeamResourceSettingsView extends BaseComponent {

    private final TeamResourceFeature feature;

    public TeamResourceSettingsView(TeamResourceFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).children(() -> {
                    headerCard();
                    layoutCard();
                    metricsCard();
                    appearanceCard();
                    actionsCard();
                });
            });
        }).element();
    }

    private Card sectionCard() {
        return card()
                .growX()
                .padding(unit(2.5f))
                .rounded(8, WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.SECTION_BORDER)
                .gap(unit(2));
    }

    private Card headerCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.layers).size(unit(6)).color(Pal.accent);
                text(Core.bundle.get("feature.team-resources.name", "Team Resources"))
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("HUD OVERLAY").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle.get("feature.team-resources.description"))
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Card layoutCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.settings).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("team-resources.settings.section.layout", "Overlay Layout"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            // Opacity Slider
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("team-resources.opacity", "Opacity")).left();
                spacer();
                slider(feature.opacityConfig.signal(), 0.1f, 1.0f, 0.05f).width(unit(24));
                badge(feature.opacityConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"))
                        .color(WebStyles.Colors.SECONDARY);
            });

            // Scale Slider
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("team-resources.size", "Scale")).left();
                spacer();
                slider(feature.scaleConfig.signal(), 0.3f, 2.0f, 0.05f).width(unit(24));
                badge(feature.scaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"))
                        .color(WebStyles.Colors.SECONDARY);
            });

            // Overlay Width Slider
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("team-resources.width", "Overlay Width")).left();
                spacer();
                slider(feature.overlayWidthConfig.signal(), 0.15f, 1.0f, 0.05f).width(unit(24));
                badge(feature.overlayWidthConfig.signal().map(v -> Math.round((v != null ? v : 0.28f) * 100) + "%"))
                        .color(WebStyles.Colors.SECONDARY);
            });
        });
    }

    private Card metricsCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.tree).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("team-resources.settings.section.metrics", "Tracked Metrics"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            checkbox(Core.bundle.get("team-resources.show-items", "Show Core Items"),
                    feature.showItemsConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.show-units", "Show Unit Counts"),
                    feature.showUnitsConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.show-power", "Show Power Graph"),
                    feature.showPowerConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.show-stored-power", "Show Stored Power"),
                    feature.showStoredPowerConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.always-show-flow-rate", "Always Show Flow Rate"),
                    feature.alwaysShowFlowRateConfig.signal()).growX();
        });
    }

    private Card appearanceCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.eye).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("team-resources.settings.section.appearance", "Appearance & Behavior"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            checkbox(Core.bundle.get("team-resources.hide-background", "Hide Background"),
                    feature.hideBackgroundConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.hide-header", "Hide Title Bar"),
                    feature.hideHeaderConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.hide-zero-units", "Hide Inactive Units (0 count)"),
                    feature.hideZeroUnitsConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.hide-zero-items", "Hide Empty Items (0 count)"),
                    feature.hideZeroItemsConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle", "Hide Drag Handle"),
                    feature.hideDragHandleConfig.signal()).growX();
        });
    }

    private Card actionsCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                button(Core.bundle.get("feature.quick-access.settings.reset-position", "Reset Position"), feature::resetPosition)
                        .style(WebStyles.outlineText())
                        .growX();

                button(Core.bundle.get("team-resources.reset-to-defaults", "Reset to Defaults"), feature::resetToDefaults)
                        .style(WebStyles.outlineText())
                        .growX();
            });
        });
    }
}
