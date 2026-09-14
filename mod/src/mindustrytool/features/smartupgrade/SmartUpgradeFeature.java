package mindustrytool.features.smartupgrade;

import mindustry.gen.Icon;
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
                .icon(Icon.up)
                .quickAccess(true)
                .development(true)
                .build());
    }
}
