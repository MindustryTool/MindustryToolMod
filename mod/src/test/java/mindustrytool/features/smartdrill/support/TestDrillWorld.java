package mindustrytool.features.smartdrill.support;

import arc.math.Rand;
import arc.struct.ObjectMap;
import arc.util.Nullable;
import mindustrytool.features.smartdrill.plan.BlockSpec;
import mindustrytool.features.smartdrill.plan.DrillWorldView;
import mindustrytool.features.smartdrill.plan.TileInfo;

public final class TestDrillWorld implements DrillWorldView {

    private final int width;
    private final int height;
    private final TileInfo[] tiles;
    private final ObjectMap<String, BlockSpec> blocks = new ObjectMap<>();

    public TestDrillWorld(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new TileInfo[width * height];
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = TileInfo.stone();
        }
    }

    public static Builder create(int width, int height) {
        return new Builder(new TestDrillWorld(width, height));
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public @Nullable TileInfo tile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        return tiles[y * width + x];
    }

    @Override
    public @Nullable BlockSpec block(@Nullable String name) {
        return name == null ? null : blocks.get(name);
    }

    @Override
    public boolean canPlace(@Nullable String block, int x, int y, int rotation) {
        if (block == null || x < 0 || y < 0 || x >= width || y >= height) {
            return false;
        }
        TileInfo info = tiles[y * width + x];
        return info != null && !info.solid;
    }

    public void set(int x, int y, @Nullable String floorOre, @Nullable String wallOre, boolean solid) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        tiles[y * width + x] = new TileInfo(floorOre, wallOre, solid);
    }

    public void register(String name, int size, boolean drill, boolean beamDrill, int tier, int range) {
        blocks.put(name, new BlockSpec(name, size, drill, beamDrill, tier, range, true));
    }

    public void wallBand(int x, int y, int width, int height, @Nullable String wallOre) {
        for (int ty = y; ty < y + height; ty++) {
            for (int tx = x; tx < x + width; tx++) {
                set(tx, ty, null, wallOre, true);
            }
        }
    }

    public void wall(int x, int y) {
        set(x, y, null, null, true);
    }

    public static final class Builder {

        private final TestDrillWorld world;

        Builder(TestDrillWorld world) {
            this.world = world;
        }

        public Builder fill(@Nullable String floorOre) {
            for (int y = 0; y < world.height; y++) {
                for (int x = 0; x < world.width; x++) {
                    world.set(x, y, floorOre, null, false);
                }
            }
            return this;
        }

        public Builder rect(int x, int y, int width, int height, @Nullable String floorOre) {
            for (int ty = y; ty < y + height; ty++) {
                for (int tx = x; tx < x + width; tx++) {
                    world.set(tx, ty, floorOre, null, false);
                }
            }
            return this;
        }

        public Builder scatter(long seed, @Nullable String floorOre, float density) {
            Rand rand = new Rand(seed);
            for (int ty = 0; ty < world.height; ty++) {
                for (int tx = 0; tx < world.width; tx++) {
                    TileInfo info = world.tile(tx, ty);
                    if (info.solid || info.floorOre != null) {
                        continue;
                    }
                    if (rand.nextFloat() < density) {
                        world.set(tx, ty, floorOre, null, false);
                    }
                }
            }
            return this;
        }

        public Builder wallBand(int x, int y, int width, int height, @Nullable String wallOre) {
            world.wallBand(x, y, width, height, wallOre);
            return this;
        }

        public Builder wall(int x, int y) {
            world.wall(x, y);
            return this;
        }

        public Builder register(String name, int size, boolean drill, boolean beamDrill, int tier, int range) {
            world.register(name, size, drill, beamDrill, tier, range);
            return this;
        }

        public TestDrillWorld build() {
            return world;
        }
    }
}