package mindustrytool.features.smartconveyor;

import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class SmartConveyorFeature implements Feature {

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("feature.smart-conveyor.name")
                .description("feature.smart-conveyor.description")
                .icon(Icon.distribution)
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
