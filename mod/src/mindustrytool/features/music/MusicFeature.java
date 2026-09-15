package mindustrytool.features.music;

import mindustrytool.components.FileIcon;
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
                .icon(FileIcon.of("music.png"))
                .development(true)
                .build());
    }
}
