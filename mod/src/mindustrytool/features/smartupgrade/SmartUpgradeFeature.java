package mindustrytool.features.smartupgrade;

import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the smart-upgrade feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class SmartUpgradeFeature extends Feature {
    public SmartUpgradeFeature() {
        super(FeatureMetadata.builder()
                .id("smart-upgrade")
                .icon(FileIcon.of("chevrons-up.png"))
                .quickAccess(true)
                .development(true)
                .build());
    }
}
