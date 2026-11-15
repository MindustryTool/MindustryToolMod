package mindustrytool.features.smartdrill.plan;

public enum OreKind {

    FLOOR,
    WALL;

    public boolean isFloor() {
        return this == FLOOR;
    }
}