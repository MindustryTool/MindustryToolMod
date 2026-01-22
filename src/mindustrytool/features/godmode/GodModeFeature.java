package mindustrytool.features.godmode;

import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class GodModeFeature implements Feature {

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.god-mode.name")
                .description("@feature.god-mode.description")
                .icon(Icon.defense)
                .build();
    }

    @Override
    public void init() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
