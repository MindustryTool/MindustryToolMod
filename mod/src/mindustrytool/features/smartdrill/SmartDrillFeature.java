package mindustrytool.features.smartdrill;

import mindustrytool.components.FileIcon;
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
                .icon(FileIcon.of("pickaxe.png"))
                .quickAccess(true)
                .development(true)
                .build());
    }
}
