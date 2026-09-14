package mindustrytool.features.smartdrill;

import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the smart-drill feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class SmartDrillFeature extends Feature {
    public SmartDrillFeature() {
        super(FeatureMetadata.builder()
                .id("smart-drill")
                .icon(Icon.filter)
                .quickAccess(true)
                .development(true)
                .build());
    }
}
