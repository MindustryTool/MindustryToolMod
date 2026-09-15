package mindustrytool.features.rangedisplay;

import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the range-display feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class RangeDisplayFeature extends Feature {
    public RangeDisplayFeature() {
        super(FeatureMetadata.builder()
                .id("range-display")
                .icon(FileIcon.of("range-display.png"))
                .order(5)
                .quickAccess(true)
                .development(true)
                .build());
    }
}
