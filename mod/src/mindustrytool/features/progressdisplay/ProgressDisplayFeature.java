package mindustrytool.features.progressdisplay;

import mindustrytool.components.FileIcon;
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
                .icon(FileIcon.of("hourglass.png"))
                .order(10)
                .development(true)
                .build());
    }
}
