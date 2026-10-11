package mindustrytool.features.smartupgrade;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Card;

/**
 * Modern declarative Solim settings view for Smart Upgrade.
 */
public class SmartUpgradeSettingsView extends BaseComponent {

    private final SmartUpgradeFeature feature;

    public SmartUpgradeSettingsView(SmartUpgradeFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().maxWidth(500f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).center().children(() -> {
                    headerCard();
                    rulesCard();
                    modeAndLimitsCard();
                    resetButton();
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

    private Component headerCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.up).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.smart-upgrade.name"))
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("BUILDING").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle.get("feature.smart-upgrade.description"))
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component rulesCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.wrench).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.smart-upgrade.settings.rules"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            // Only Replace Exact Block Type
            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.smart-upgrade.settings.only-same-type"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.smart-upgrade.settings.only-same-type.desc"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.onlySameTypeConfig.signal());
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Traverse Bridges & Junctions
            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.smart-upgrade.settings.traverse-bridges"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.smart-upgrade.settings.traverse-bridges.desc"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.traverseBridgesConfig.signal());
            });
        });
    }

    private Component modeAndLimitsCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.settings).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.smart-upgrade.settings.limits"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            // Trigger Mode selection
            column().growX().gap(unit(1)).left().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.smart-upgrade.settings.trigger-mode"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.smart-upgrade.settings.trigger-mode.desc"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });

                row().gap(unit(1.5f)).children(() -> {
                    button(Core.bundle.get("feature.smart-upgrade.settings.trigger-mode.one-shot"),
                            () -> feature.triggerModeConfig.set(SmartUpgradeFeature.MODE_ONE_SHOT))
                            .style(WebStyles.filterChipText())
                            .checked(feature.triggerModeConfig.signal()
                                    .map(SmartUpgradeFeature.MODE_ONE_SHOT::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));

                    button(Core.bundle.get("feature.smart-upgrade.settings.trigger-mode.persistent"),
                            () -> feature.triggerModeConfig.set(SmartUpgradeFeature.MODE_PERSISTENT))
                            .style(WebStyles.filterChipText())
                            .checked(feature.triggerModeConfig.signal()
                                    .map(SmartUpgradeFeature.MODE_PERSISTENT::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));
                });
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Max Upgraded Blocks
            column().growX().gap(unit(1)).left().children(() -> {
                row().growX().center().children(() -> {
                    text(Core.bundle.get("feature.smart-upgrade.settings.max-upgrades"))
                            .color(Color.white)
                            .left();
                    spacer();
                    badge(feature.maxUpdatesConfig.signal().map(v -> String.valueOf(v != null ? v : 500)))
                            .color(WebStyles.Colors.SECONDARY);
                });

                text(Core.bundle.get("feature.smart-upgrade.settings.max-upgrades.desc"))
                        .color(WebStyles.Colors.GHOST_FG)
                        .left()
                        .wrap()
                        .growX();

                slider(feature.maxUpdatesConfig.signal(), 50, 2000, 50).growX();
            });
        });
    }

    private Component resetButton() {
        return button(Core.bundle.get("feature.smart-upgrade.settings.reset"),
                feature::resetToDefaults)
                .style(WebStyles.outlineText())
                .height(unit(9))
                .growX();
    }
}
