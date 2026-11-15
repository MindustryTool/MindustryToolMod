package mindustrytool.features.smartdrill.plan;

import arc.util.Nullable;

public interface DrillWorldView {

    int width();

    int height();

    @Nullable TileInfo tile(int x, int y);

    @Nullable BlockSpec block(@Nullable String name);

    boolean canPlace(@Nullable String block, int x, int y, int rotation);

    default @Nullable String oreAt(int x, int y, OreKind kind) {
        TileInfo info = tile(x, y);
        return info == null ? null : info.ore(kind);
    }

    default boolean isSolid(int x, int y) {
        TileInfo info = tile(x, y);
        return info == null || info.solid;
    }
}