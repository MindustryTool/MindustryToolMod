package mindustrytool.features.godmode;

import mindustry.gen.Iconc;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class GodModeFeature implements Feature {

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("God Mode")
                .description("Not coded yet.")
                .icon(Iconc.defense)
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
