package mindustrytool.features.togglerendering;

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
 * Modern declarative Solim settings view for Toggle Rendering.
 */
public class ToggleRenderingSettingsView extends BaseComponent {

    private final ToggleRenderingFeature feature;

    public ToggleRenderingSettingsView(ToggleRenderingFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().maxWidth(500f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).center().children(() -> {
                    headerCard();
                    layersCard();
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
                icon(Icon.eye).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("feature.toggle-rendering.name", "Toggle Rendering")
                        : "Toggle Rendering")
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("GRAPHICS").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle != null
                    ? Core.bundle.get("feature.toggle-rendering.description",
                            "Toggle rendering of units and effects for performance or screenshots.")
                    : "Toggle rendering of units and effects for performance or screenshots.")
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component layersCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.eye).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("feature.toggle-rendering.settings.layers", "Render Layers")
                        : "Render Layers")
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            // Draw Allied Units row
            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.toggle-rendering.settings.draw-units-allies", "Draw Allied Units")
                            : "Draw Allied Units")
                            .color(Color.white)
                            .left();
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.toggle-rendering.settings.draw-units-allies.desc",
                                    "Render units belonging to your team or allied forces.")
                            : "Render units belonging to your team or allied forces.")
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.drawUnitsAlliesConfig.signal());
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Draw Enemy Units row
            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.toggle-rendering.settings.draw-units-enemies", "Draw Enemy Units")
                            : "Draw Enemy Units")
                            .color(Color.white)
                            .left();
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.toggle-rendering.settings.draw-units-enemies.desc",
                                    "Render units belonging to enemy teams.")
                            : "Render units belonging to enemy teams.")
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.drawUnitsEnemiesConfig.signal());
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Draw Blocks & Buildings row
            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.toggle-rendering.settings.draw-blocks", "Draw Blocks & Buildings")
                            : "Draw Blocks & Buildings")
                            .color(Color.white)
                            .left();
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.toggle-rendering.settings.draw-blocks.desc",
                                    "Render placed structures, walls, and conveyor belts.")
                            : "Render placed structures, walls, and conveyor belts.")
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.drawBlocksConfig.signal());
            });
        });
    }

    private Component resetButton() {
        return button(Core.bundle != null
                ? Core.bundle.get("feature.toggle-rendering.settings.reset", "Reset to Defaults")
                : "Reset to Defaults",
                feature::resetToDefaults)
                .style(WebStyles.outlineText())
                .height(unit(9))
                .growX();
    }
}
