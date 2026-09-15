package mindustrytool.features.godmode;

import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the god-mode feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class GodModeFeature extends Feature {
    public GodModeFeature() {
        super(FeatureMetadata.builder()
                .id("god-mode")
                .icon(FileIcon.of("wand-sparkles.png"))
                .enabledByDefault(false)
                .development(true)
                .build());
    }
}
