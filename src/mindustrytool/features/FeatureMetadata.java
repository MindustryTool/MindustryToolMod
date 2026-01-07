package mindustrytool.features;

public record FeatureMetadata(String name, String description, String icon, int order, boolean enabledByDefault) {
    public FeatureMetadata(String name, String description, String icon) {
        this(name, description, icon, 0, true);
    }

    public FeatureMetadata(String name, String description, String icon, int order) {
        this(name, description, icon, order, true);
    }

    public FeatureMetadata(String name, String description) {
        this(name, description, "settings", 0, true);
    }
}
