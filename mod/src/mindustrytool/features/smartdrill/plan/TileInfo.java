package mindustrytool.features.smartdrill.plan;

import arc.util.Nullable;

public final class TileInfo {

    public final @Nullable String floorOre;
    public final @Nullable String wallOre;
    public final boolean solid;

    public TileInfo(@Nullable String floorOre, @Nullable String wallOre, boolean solid) {
        this.floorOre = floorOre;
        this.wallOre = wallOre;
        this.solid = solid;
    }

    public static TileInfo stone() {
        return new TileInfo(null, null, false);
    }

    public static TileInfo wall() {
        return new TileInfo(null, null, true);
    }

    public boolean hasFloorOre() {
        return floorOre != null;
    }

    public boolean hasWallOre() {
        return wallOre != null;
    }

    public @Nullable String ore(OreKind kind) {
        return kind.isFloor() ? floorOre : wallOre;
    }

    public boolean isTarget(OreKind kind, @Nullable String target) {
        if (target == null) {
            return false;
        }
        return target.equals(ore(kind));
    }

    public boolean isAnyOre(OreKind kind) {
        return ore(kind) != null;
    }
}