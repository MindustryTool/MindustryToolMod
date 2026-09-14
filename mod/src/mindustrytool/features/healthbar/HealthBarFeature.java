package mindustrytool.features.healthbar;

import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the health-bar feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class HealthBarFeature extends Feature {
    public HealthBarFeature() {
        super(FeatureMetadata.builder()
                .id("health-bar")
                .icon(FileIcon.of("healthbar.png"))
                .order(4)
                .enabledByDefault(false)
                .quickAccess(true)
                .development(true)
                .build());
    }
}
