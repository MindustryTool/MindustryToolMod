package mindustrytool.features.wavepreview;

import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the wave-preview feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class WavePreviewFeature extends Feature {
    public WavePreviewFeature() {
        super(FeatureMetadata.builder()
                .id("wave-preview")
                .icon(Icon.units)
                .order(1)
                .quickAccess(true)
                .development(true)
                .build());
    }
}
