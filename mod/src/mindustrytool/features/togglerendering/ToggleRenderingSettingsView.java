package mindustrytool.features.togglerendering;

import static solim.UI.*;
import static solim.modifier.Values.color;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.left;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.style;
import static solim.modifier.Values.wrap;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.WebStyles;
import solim.core.Component;

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

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-units-allies"))
                            .props(color(Color.white), left());
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-units-allies.desc"))
                            .props(color(WebStyles.Colors.GHOST_FG), left(), wrap(), growX());
                });
                spacer();
                checkbox("", feature.drawUnitsAlliesConfig.signal());
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-units-enemies"))
                            .props(color(Color.white), left());
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-units-enemies.desc"))
                            .props(color(WebStyles.Colors.GHOST_FG), left(), wrap(), growX());
                });
                spacer();
                checkbox("", feature.drawUnitsEnemiesConfig.signal());
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-blocks"))
                            .props(color(Color.white), left());
                    text(Core.bundle.get("feature.toggle-rendering.settings.draw-blocks.desc"))
                            .props(color(WebStyles.Colors.GHOST_FG), left(), wrap(), growX());
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
                    .props(style(WebStyles.outlineText()), height(unit(11)), prefHeight(unit(11)), growX());
        });
    }
}
