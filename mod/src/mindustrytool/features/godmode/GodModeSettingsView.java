package mindustrytool.features.godmode;

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
 * Modern declarative Solim settings view for God Mode sandbox controls.
 */
public class GodModeSettingsView extends BaseComponent {

    private final GodModeFeature feature;

    public GodModeSettingsView(GodModeFeature feature) {
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
        Readable<String> scaleText = feature.scaleConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100));

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).padding(unit(2)).children(() -> {
                    // Header Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(2)).center().children(() -> {
                            icon(Icon.admin).size(unit(6)).color(Pal.accent);
                            column().growX().gap(unit(0.5f)).left().children(() -> {
                                row().growX().gap(unit(1.5f)).center().children(() -> {
                                    text(Core.bundle.get("feature.god-mode.settings.title", "God Mode Settings"))
                                            .color(Pal.accent)
                                            .left();
                                    badge(Core.bundle.get("feature.god-mode.badge.sandbox", "SANDBOX"))
                                            .color(WebStyles.Colors.PRIMARY)
                                            .textColor(WebStyles.Colors.PRIMARY_FG);
                                });
                                text(Core.bundle.get("feature.god-mode.description", "Developer and sandbox tools for unit spawning, core filling, team changing, and status effects."))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .wrap()
                                        .left()
                                        .fontScale(0.9f);
                            });
                        });
                    });

                    // Display & Layout Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(1.5f)).center().children(() -> {
                            icon(Icon.layers).size(unit(5)).color(Pal.accent);
                            text(Core.bundle.get("feature.god-mode.settings.display-layout", "Display & Layout"))
                                    .color(Pal.accent)
                                    .growX()
                                    .left();
                        });

                        column().growX().gap(unit(2)).children(() -> {
                            // Display Mode Chips
                            row().growX().center().children(() -> {
                                text(Core.bundle.get("feature.god-mode.settings.display-mode", "Display Mode"))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .left();
                                spacer();
                                row().gap(unit(1.5f)).children(() -> {
                                    button(() -> feature.displayModeConfig.set(GodModeFeature.DISPLAY_HUD))
                                            .style(WebStyles.filterChip())
                                            .checked(feature.displayModeConfig.signal()
                                                    .map(GodModeFeature.DISPLAY_HUD::equals))
                                            .padding(unit(1.5f))
                                            .children(() -> text(Core.bundle.get("feature.god-mode.settings.display-mode.hud", "HUD")));

                                    button(() -> feature.displayModeConfig.set(GodModeFeature.DISPLAY_POPUP))
                                            .style(WebStyles.filterChip())
                                            .checked(feature.displayModeConfig.signal()
                                                    .map(GodModeFeature.DISPLAY_POPUP::equals))
                                            .padding(unit(1.5f))
                                            .children(() -> text(Core.bundle.get("feature.god-mode.settings.display-mode.popup", "Popup")));
                                });
                            });

                            // Scale Slider
                            row().growX().gap(unit(2)).center().children(() -> {
                                text(Core.bundle.get("feature.god-mode.settings.scale", "HUD Scale"))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .left();
                                spacer();
                                slider(feature.scaleConfig.signal(), 0.5f, 1.5f, 0.1f);
                                badge(scaleText)
                                        .color(WebStyles.Colors.SECONDARY)
                                        .textColor(WebStyles.Colors.SECONDARY_FG)
                                        .minWidth(unit(14));
                            });

                            // Hide Drag Handle Toggle
                            checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle", "Hide Drag Handle"),
                                    feature.hideDragHandleConfig.signal()).growX();
                        });
                    });

                    // Execution Provider Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(1.5f)).center().children(() -> {
                            icon(Icon.settings).size(unit(5)).color(Pal.accent);
                            text(Core.bundle.get("feature.god-mode.settings.provider", "Execution Provider"))
                                    .color(Pal.accent)
                                    .growX()
                                    .left();
                        });

                        text(Core.bundle.get("feature.god-mode.settings.provider-desc", "Select how sandbox commands and actions are executed in the game."))
                                .color(WebStyles.Colors.GHOST_FG)
                                .wrap()
                                .left()
                                .fontScale(0.9f);

                        wrap().gap(unit(1.5f)).children(() -> {
                            button(() -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_AUTO))
                                    .style(WebStyles.filterChip())
                                    .checked(feature.providerModeConfig.signal()
                                            .map(GodModeFeature.PROVIDER_AUTO::equals))
                                    .padding(unit(1.5f))
                                    .children(() -> text(Core.bundle.get("feature.god-mode.settings.provider.auto", "Auto")));

                            button(() -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_INTERNAL))
                                    .style(WebStyles.filterChip())
                                    .checked(feature.providerModeConfig.signal()
                                            .map(GodModeFeature.PROVIDER_INTERNAL::equals))
                                    .padding(unit(1.5f))
                                    .children(() -> text(Core.bundle.get("feature.god-mode.settings.provider.internal", "Internal")));

                            button(() -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_JS))
                                    .style(WebStyles.filterChip())
                                    .checked(feature.providerModeConfig.signal()
                                            .map(GodModeFeature.PROVIDER_JS::equals))
                                    .padding(unit(1.5f))
                                    .children(() -> text(Core.bundle.get("feature.god-mode.settings.provider.js", "JS (/js)")));
                        });
                    });

                    // Actions Card
                    sectionCard().children(() -> {
                        button(feature::resetPosition)
                                .style(WebStyles.outline())
                                .growX()
                                .padding(unit(2))
                                .children(() -> {
                                    row().gap(unit(1.5f)).center().children(() -> {
                                        icon(Icon.refresh).size(unit(4));
                                        text(Core.bundle.get("feature.god-mode.settings.reset-position", "Reset HUD Position"));
                                    });
                                });
                    });
                });
            });
        }).element();
    }
}
