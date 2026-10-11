package mindustrytool.features.wavepreview;

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

/**
 * Modern declarative Solim settings view for Wave Preview feature.
 */
public class WavePreviewSettingsView extends BaseComponent {

    private final WavePreviewFeature feature;

    public WavePreviewSettingsView(WavePreviewFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().maxWidth(500f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).center().children(() -> {
                    headerCard();
                    settingsCard();
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
                icon(Icon.waves).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.wave-preview.name"))
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("TACTICAL").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle.get("feature.wave-preview.description"))
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component settingsCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.settings).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.wave-preview.settings.section.settings"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            // Opacity
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.wave-preview.settings.opacity"))
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.opacityConfig.signal(), 0.1f, 1.0f, 0.05f);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.opacityConfig.signal().map(v ->
                            String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)))
                            .color(WebStyles.Colors.SECONDARY);
                });
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Scale
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.wave-preview.settings.scale"))
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.scaleConfig.signal(), 0.5f, 2.0f, 0.1f);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.scaleConfig.signal().map(v ->
                            String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)))
                            .color(WebStyles.Colors.SECONDARY);
                });
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Lookahead depth
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.wave-preview.settings.depth"))
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.depthConfig.signal(), WavePreviewFeature.MIN_DEPTH, WavePreviewFeature.MAX_DEPTH, 1);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.depthConfig.signal().map(v ->
                            String.valueOf(v != null ? v : WavePreviewFeature.DEFAULT_DEPTH)))
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
                        text(Core.bundle.get("feature.wave-preview.settings.reset"));
                    });
        });
    }
}
