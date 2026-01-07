package mindustrytool.features;

public record FeatureMetadata(String name, String description, String icon) {
    public FeatureMetadata(String name, String description) {
        this(name, description, "settings");
    }
}
