package mindustrytool.features.wavepreview;

import mindustrytool.components.FileIcon;
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
                .icon(FileIcon.of("swords.png"))
                .order(1)
                .quickAccess(true)
                .development(true)
                .build());
    }
}
