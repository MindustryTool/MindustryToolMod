package mindustrytool.features.progressdisplay;

import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the progress-display feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class ProgressDisplayFeature extends Feature {
    public ProgressDisplayFeature() {
        super(FeatureMetadata.builder()
                .id("progress-display")
                .icon(Icon.chartBar)
                .order(10)
                .development(true)
                .build());
    }
}
