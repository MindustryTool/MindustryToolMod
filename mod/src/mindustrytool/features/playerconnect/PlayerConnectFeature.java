package mindustrytool.features.playerconnect;

import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the player-connect feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class PlayerConnectFeature extends Feature {
    public PlayerConnectFeature() {
        super(FeatureMetadata.builder()
                .id("player-connect")
                .icon(FileIcon.of("signal.png"))
                .order(3)
                .development(true)
                .build());
    }
}
