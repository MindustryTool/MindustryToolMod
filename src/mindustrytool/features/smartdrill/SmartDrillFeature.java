package mindustrytool.features.smartdrill;

import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class SmartDrillFeature implements Feature {

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("feature.smart-drill.name")
                .description("feature.smart-drill.description")
                .icon(Icon.filter)
                .build();
    }

    @Override
    public void init() {
        // Initialize feature
    }

    @Override
    public void onEnable() {
        // Called when enabled
    }

    @Override
    public void onDisable() {
        // Called when disabled
    }
}
