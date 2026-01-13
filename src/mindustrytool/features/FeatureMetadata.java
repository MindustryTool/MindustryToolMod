package mindustrytool.features;

import mindustry.gen.Iconc;

public record FeatureMetadata(String name, String description, char icon, int order, boolean enabledByDefault) {
    public FeatureMetadata(String name, String description, char icon) {
        this(name, description, icon, 0, true);
    }

    public FeatureMetadata(String name, String description, char icon, int order) {
        this(name, description, icon, order, true);
    }

    public FeatureMetadata(String name, String description) {
        this(name, description, Iconc.settings, 0, true);
    }
}
