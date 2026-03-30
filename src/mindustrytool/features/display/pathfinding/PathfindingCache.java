package mindustrytool.features.display.pathfinding;

import lombok.Data;

@Data
public class PathfindingCache {
    public float[] data;
    public int size;
    public float lastUpdateTime;
    public float lastUsedTime;
}
