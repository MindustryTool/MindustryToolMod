package mindustrytool.features.music;

import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the music feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class MusicFeature extends Feature {
    public MusicFeature() {
        super(FeatureMetadata.builder()
                .id("music")
                .icon(Icon.play)
                .development(true)
                .build());
    }
}
