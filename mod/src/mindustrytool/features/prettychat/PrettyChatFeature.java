package mindustrytool.features.prettychat;

import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the pretty-chat feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class PrettyChatFeature extends Feature {
    public PrettyChatFeature() {
        super(FeatureMetadata.builder()
                .id("pretty-chat")
                .icon(Icon.chat)
                .development(true)
                .build());
    }
}
