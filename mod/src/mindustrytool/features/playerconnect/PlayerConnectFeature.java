package mindustrytool.features.playerconnect;

import mindustry.gen.Icon;
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
                .icon(Icon.planet)
                .order(3)
                .development(true)
                .build());
    }
}
