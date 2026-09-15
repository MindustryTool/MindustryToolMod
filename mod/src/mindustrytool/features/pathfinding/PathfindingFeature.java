package mindustrytool.features.pathfinding;

import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

/**
 * In-development placeholder for the pathfinding feature.
 * Metadata only; gameplay logic arrives with the full rewrite.
 */
public class PathfindingFeature extends Feature {
    public PathfindingFeature() {
        super(FeatureMetadata.builder()
                .id("pathfinding")
                .icon(FileIcon.of("pathfinding.png"))
                .quickAccess(true)
                .development(true)
                .build());
    }
}
