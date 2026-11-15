package mindustrytool.features.smartdrill.plan;

import arc.util.Nullable;

public final class DrillPlanRequest {

    public static final int DEFAULT_MAX_TILES = 100;
    public static final int DEFAULT_MAX_RECT_SIZE = 10;

    public final String targetOre;
    public final OreKind targetKind;
    public final Rect rect;
    public final int facing;
    public final @Nullable String drill;
    public final @Nullable SchematicData schematic;
    public final int maxFloodTiles;
    public final int maxRectSize;

    private DrillPlanRequest(Builder builder) {
        this.targetOre = builder.targetOre;
        this.targetKind = builder.targetKind;
        this.rect = builder.rect;
        this.facing = builder.facing;
        this.drill = builder.drill;
        this.schematic = builder.schematic;
        this.maxFloodTiles = builder.maxFloodTiles;
        this.maxRectSize = builder.maxRectSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSchematicMode() {
        return schematic != null;
    }

    public static final class Builder {
        private String targetOre = "";
        private OreKind targetKind = OreKind.FLOOR;
        private Rect rect = new Rect(0, 0, 1, 1);
        private int facing = 0;
        private @Nullable String drill;
        private @Nullable SchematicData schematic;
        private int maxFloodTiles = DEFAULT_MAX_TILES;
        private int maxRectSize = DEFAULT_MAX_RECT_SIZE;

        public Builder target(String ore, OreKind kind) {
            this.targetOre = ore;
            this.targetKind = kind;
            return this;
        }

        public Builder rect(Rect value) {
            this.rect = value;
            return this;
        }

        public Builder facing(int value) {
            this.facing = Math.floorMod(value, 4);
            return this;
        }

        public Builder drill(String value) {
            this.drill = value;
            return this;
        }

        public Builder schematic(@Nullable SchematicData value) {
            this.schematic = value;
            return this;
        }

        public Builder maxFloodTiles(int value) {
            this.maxFloodTiles = Math.max(1, value);
            return this;
        }

        public Builder maxRectSize(int value) {
            this.maxRectSize = Math.max(1, value);
            return this;
        }

        public DrillPlanRequest build() {
            return new DrillPlanRequest(this);
        }
    }
}