package mindustrytool.features.chat;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.ui.Styles;
import solim.core.BaseComponent;

public class ChatSettingsView extends BaseComponent {

    private final ChatFeature feature;

    public ChatSettingsView(ChatFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    // Opacity
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.chat.settings.opacity", "Opacity")).left();
                        spacer();
                        slider(feature.opacityConfig.signal(), 0.2f, 1.0f, 0.05f);
                        row().width(unit(14)).children(() -> {
                            text(feature.opacityConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));
                        });
                    });

                    // Width Ratio
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.chat.settings.width", "Width")).left();
                        spacer();
                        slider(feature.widthRatioConfig.signal(), 0.4f, 1.0f, 0.05f);
                        row().width(unit(14)).children(() -> {
                            text(feature.widthRatioConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 0.6f) * 100)));
                        });
                    });

                    // Height Ratio
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.chat.settings.height", "Height")).left();
                        spacer();
                        slider(feature.heightRatioConfig.signal(), 0.4f, 1.0f, 0.05f);
                        row().width(unit(14)).children(() -> {
                            text(feature.heightRatioConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 0.6f) * 100)));
                        });
                    });

                    divider();

                    button(Core.bundle.get("feature.chat.settings.reset-position", "Reset Position"), feature::resetPosition)
                            .style(Styles.defaultb)
                            .growX();

                    button(Core.bundle.get("feature.chat.settings.reset-appearance", "Reset Appearance"), feature::resetAppearance)
                            .style(Styles.defaultb)
                            .growX();
                });
            });
        }).element();
    }
}
