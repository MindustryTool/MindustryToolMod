package mindustrytool.features.chat;

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
 * Modern declarative Solim settings view for Chat overlay layout and appearance.
 */
public class ChatSettingsView extends BaseComponent {

    private final ChatFeature feature;

    public ChatSettingsView(ChatFeature feature) {
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
        Readable<String> opacityText = feature.opacityConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100));
        Readable<String> widthText = feature.widthRatioConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 0.6f) * 100));
        Readable<String> heightText = feature.heightRatioConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 0.6f) * 100));

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).padding(unit(2)).children(() -> {
                    // Header Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(2)).center().children(() -> {
                            icon(Icon.chat).size(unit(6)).color(Pal.accent);
                            column().growX().gap(unit(0.5f)).left().children(() -> {
                                row().growX().gap(unit(1.5f)).center().children(() -> {
                                    text(Core.bundle.get("feature.chat.settings.title", "Chat Settings"))
                                            .color(Pal.accent)
                                            .left();
                                    badge(Core.bundle.get("feature.chat.badge.communication", "COMMUNICATION"))
                                            .color(WebStyles.Colors.PRIMARY)
                                            .textColor(WebStyles.Colors.PRIMARY_FG);
                                });
                                text(Core.bundle.get("feature.chat.description", "Live in-game chat connected to the Mindustry Tool community."))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .wrap()
                                        .left()
                                        .fontScale(0.9f);
                            });
                        });
                    });

                    // Overlay Layout Card
                    sectionCard().children(() -> {
                        row().growX().gap(unit(1.5f)).center().children(() -> {
                            icon(Icon.layers).size(unit(5)).color(Pal.accent);
                            text(Core.bundle.get("feature.chat.settings.overlay-layout", "Overlay Layout"))
                                    .color(Pal.accent)
                                    .growX()
                                    .left();
                        });

                        column().growX().gap(unit(2)).children(() -> {
                            // Opacity
                            row().growX().gap(unit(2)).center().children(() -> {
                                text(Core.bundle.get("feature.chat.settings.opacity", "Opacity"))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .left();
                                spacer();
                                slider(feature.opacityConfig.signal(), 0.2f, 1.0f, 0.05f);
                                badge(opacityText)
                                        .color(WebStyles.Colors.SECONDARY)
                                        .textColor(WebStyles.Colors.SECONDARY_FG)
                                        .minWidth(unit(14));
                            });

                            // Width Ratio
                            row().growX().gap(unit(2)).center().children(() -> {
                                text(Core.bundle.get("feature.chat.settings.width", "Width"))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .left();
                                spacer();
                                slider(feature.widthRatioConfig.signal(), 0.4f, 1.0f, 0.05f);
                                badge(widthText)
                                        .color(WebStyles.Colors.SECONDARY)
                                        .textColor(WebStyles.Colors.SECONDARY_FG)
                                        .minWidth(unit(14));
                            });

                            // Height Ratio
                            row().growX().gap(unit(2)).center().children(() -> {
                                text(Core.bundle.get("feature.chat.settings.height", "Height"))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .left();
                                spacer();
                                slider(feature.heightRatioConfig.signal(), 0.4f, 1.0f, 0.05f);
                                badge(heightText)
                                        .color(WebStyles.Colors.SECONDARY)
                                        .textColor(WebStyles.Colors.SECONDARY_FG)
                                        .minWidth(unit(14));
                            });
                        });
                    });

                    // Actions Card
                    sectionCard().children(() -> {
                        column().growX().gap(unit(1.5f)).children(() -> {
                            button(feature::resetPosition)
                                    .style(WebStyles.outline())
                                    .growX()
                                    .padding(unit(2))
                                    .children(() -> {
                                        row().gap(unit(1.5f)).center().children(() -> {
                                            icon(Icon.move).size(unit(4));
                                            text(Core.bundle.get("feature.chat.settings.reset-position", "Reset Position"));
                                        });
                                    });

                            button(feature::resetAppearance)
                                    .style(WebStyles.outline())
                                    .growX()
                                    .padding(unit(2))
                                    .children(() -> {
                                        row().gap(unit(1.5f)).center().children(() -> {
                                            icon(Icon.refresh).size(unit(4));
                                            text(Core.bundle.get("feature.chat.settings.reset-appearance", "Reset Appearance"));
                                        });
                                    });
                        });
                    });
                });
            });
        }).element();
    }
}
