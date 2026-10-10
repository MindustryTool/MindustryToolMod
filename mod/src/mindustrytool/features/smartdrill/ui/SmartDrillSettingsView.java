package mindustrytool.features.smartdrill.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import mindustrytool.features.smartdrill.SmartDrillFeature;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Card;

/**
 * Modern declarative Solim settings view for Smart Drill.
 */
public class SmartDrillSettingsView extends BaseComponent {

    private final SmartDrillFeature feature;

    public SmartDrillSettingsView(SmartDrillFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().maxWidth(500f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).center().children(() -> {
                    headerCard();
                    settingsCard();
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
                icon(Icon.crafting).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("feature.smart-drill.name", "Smart Drill")
                        : "Smart Drill")
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("AUTOMATION").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle != null
                    ? Core.bundle.get("feature.smart-drill.description", "Optimize drill placement with smart selection tools.")
                    : "Optimize drill placement with smart selection tools.")
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component settingsCard() {
        return sectionCard().children(() -> {
            // Auto-Disable After Placement row
            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.smart-drill.settings.auto-disable", "Auto-Disable After Placement")
                            : "Auto-Disable After Placement")
                            .color(Color.white)
                            .left();
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.smart-drill.settings.auto-disable.desc",
                                    "Automatically turn off Smart Drill mode after placing drills to return to normal building.")
                            : "Automatically turn off Smart Drill mode after placing drills to return to normal building.")
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.autoDisableConfig.signal());
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Max Ore Tiles
            column().growX().gap(unit(1)).left().children(() -> {
                row().growX().center().children(() -> {
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.smart-drill.settings.max-tiles", "Max Ore Tiles")
                            : "Max Ore Tiles")
                            .color(Color.white)
                            .left();
                    spacer();
                    badge(feature.maxTilesConfig.signal().map(v -> String.valueOf(v != null ? v : 100)))
                            .color(WebStyles.Colors.SECONDARY);
                });

                text(Core.bundle != null
                        ? Core.bundle.get("feature.smart-drill.settings.max-tiles.desc",
                                "Maximum number of connected ore tiles to cover with drills per placement.")
                        : "Maximum number of connected ore tiles to cover with drills per placement.")
                        .color(WebStyles.Colors.GHOST_FG)
                        .left()
                        .wrap()
                        .growX();

                slider(feature.maxTilesConfig.signal(), 20, 300, 10).growX();
            });
        });
    }

    private Component resetButton() {
        return button(Core.bundle != null
                ? Core.bundle.get("feature.smart-drill.settings.reset", "Reset to Defaults")
                : "Reset to Defaults",
                feature::resetToDefaults)
                .style(WebStyles.outlineText())
                .height(unit(9))
                .growX();
    }
}
