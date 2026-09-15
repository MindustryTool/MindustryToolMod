package mindustrytool.features.wavepreview;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import solim.core.BaseComponent;

/**
 * Settings view for the Wave Preview feature, offering opacity, scale, and lookahead-depth rows.
 */
public class WavePreviewSettingsView extends BaseComponent {

    private final WavePreviewFeature feature;

    public WavePreviewSettingsView(WavePreviewFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    // Opacity setting
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.wave-preview.settings.opacity")).left();
                        spacer();
                        slider(feature.opacityConfig.signal(), 0.1f, 1.0f, 0.05f);
                        row().width(unit(14)).children(() -> {
                            text(feature.opacityConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));
                        });
                    });

                    divider();

                    // Scale setting
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.wave-preview.settings.scale")).left();
                        spacer();
                        slider(feature.scaleConfig.signal(), 0.5f, 2.0f, 0.1f);
                        row().width(unit(14)).children(() -> {
                            text(feature.scaleConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));
                        });
                    });

                    divider();

                    // Lookahead depth setting
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.wave-preview.settings.depth")).left();
                        spacer();
                        slider(feature.depthConfig.signal(), WavePreviewFeature.MIN_DEPTH, WavePreviewFeature.MAX_DEPTH, 1);
                        row().width(unit(14)).children(() -> {
                            text(feature.depthConfig.signal().map(v -> String.valueOf(v != null ? v : WavePreviewFeature.DEFAULT_DEPTH)));
                        });
                    });
                });
            });
        }).element();
    }
}
