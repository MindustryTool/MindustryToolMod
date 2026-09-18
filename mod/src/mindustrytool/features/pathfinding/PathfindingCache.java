package mindustrytool.features.pathfinding;

public class PathfindingCache {
    public float[] data = new float[0];
    public int size;
    public float lastUpdateTime;
    public float lastUsedTime;

    public PathfindingCache() {
    }

    public PathfindingCache(int initialCapacity) {
        this.data = new float[initialCapacity];
    }
}
