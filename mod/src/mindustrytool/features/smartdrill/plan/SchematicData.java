package mindustrytool.features.smartdrill.plan;

import arc.struct.Seq;

public final class SchematicData {

    public static final class Tile {
        public final String block;
        public final int x;
        public final int y;
        public final int rotation;
        public final Object config;

        public Tile(String block, int x, int y, int rotation, Object config) {
            this.block = block;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.config = config;
        }
    }

    public final String name;
    public final int width;
    public final int height;
    public final Seq<Tile> tiles;

    public SchematicData(String name, int width, int height, Seq<Tile> tiles) {
        this.name = name;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.tiles = tiles;
    }
}