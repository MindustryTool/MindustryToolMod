package mindustrytool.features.savesync;

import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the save-sync feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class SaveSyncFeature extends Feature {
    public SaveSyncFeature() {
        super(FeatureMetadata.builder()
                .id("save-sync")
                .icon(Icon.save)
                .order(10)
                .enabledByDefault(false)
                .development(true)
                .build());
    }
}
