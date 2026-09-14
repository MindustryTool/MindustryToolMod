package mindustrytool.features.timecontrol;

import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the time-control feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class TimeControlFeature extends Feature {
    public TimeControlFeature() {
        super(FeatureMetadata.builder()
                .id("time-control")
                .icon(FileIcon.of("clock.png"))
                .order(1)
                .enabledByDefault(false)
                .development(true)
                .build());
    }
}
