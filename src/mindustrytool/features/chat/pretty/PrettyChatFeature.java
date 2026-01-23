package mindustrytool.features.chat.pretty;

import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class PrettyChatFeature implements Feature {

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.prettychat.name")
                .description("@feature.prettychat.description")
                .icon(Icon.chat)
                .build();
    }

    @Override
    public void init() {
        // TODO: Initialize pretty chat components
    }

    @Override
    public void onEnable() {
        // TODO: Enable pretty chat
    }

    @Override
    public void onDisable() {
        // TODO: Disable pretty chat
    }
}
