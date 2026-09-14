package mindustrytool.features.autoplay;

import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the autoplay feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class AutoplayFeature extends Feature {
    public AutoplayFeature() {
        super(FeatureMetadata.builder()
                .id("autoplay")
                .icon(FileIcon.of("autoplay.png"))
                .enabledByDefault(false)
                .quickAccess(true)
                .development(true)
                .build());
    }
}
