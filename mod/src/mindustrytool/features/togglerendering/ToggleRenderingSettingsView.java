package mindustrytool.features.togglerendering;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.WebStyles;
import solim.core.Component;

/**
 * Modern declarative Solim settings view for Toggle Rendering.
 */
public class ToggleRenderingSettingsView extends Component {

    private final ToggleRenderingFeature feature;

    public ToggleRenderingSettingsView(ToggleRenderingFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            layersCard();
            keybindsCard();
            resetButtonCard();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, "GRAPHICS");
    }

    private Component layersCard() {
        return SettingCard.section(Icon.eye, Core.bundle.get("feature.toggle-rendering.settings.layers"), () -> {
            // Draw Allied Units row
            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(1.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-units-allies"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-units-allies.desc"))
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
                column().growX().gap(unit(1.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-units-enemies"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-units-enemies.desc"))
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
                column().growX().gap(unit(1.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-blocks"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-blocks.desc"))
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

    private Component resetButtonCard() {
        return SettingCard.card().children(() -> {
            button(Core.bundle.get("feature.toggle-rendering.settings.reset"),
                    feature::resetToDefaults)
                    .style(WebStyles.outlineText())
                    .height(unit(11))
                    .growX();
        });
    }
}
