package mindustrytool.features.togglerendering;

import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the toggle-rendering feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class ToggleRenderingFeature extends Feature {
    public ToggleRenderingFeature() {
        super(FeatureMetadata.builder()
                .id("toggle-rendering")
                .icon(Icon.eye)
                .order(5)
                .enabledByDefault(false)
                .quickAccess(true)
                .development(true)
                .build());
    }
}
