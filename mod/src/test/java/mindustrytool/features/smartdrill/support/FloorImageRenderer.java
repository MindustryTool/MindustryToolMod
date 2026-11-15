package mindustrytool.features.smartdrill.support;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.util.Nullable;
import mindustrytool.features.smartdrill.plan.DrillWorldView;
import mindustrytool.features.smartdrill.plan.TileInfo;

public final class FloorImageRenderer {

    public static final int SCALE = 8;

    private static final int COLOR_FLOOR = 0xff2b2f36;
    private static final int COLOR_WALL = 0xff5a6068;

    private FloorImageRenderer() {
    }

    public static void write(DrillWorldView world, Fi file) {
        Pixmap pixmap = new Pixmap(world.width() * SCALE, world.height() * SCALE);
        for (int y = 0; y < world.height(); y++) {
            for (int x = 0; x < world.width(); x++) {
                fill(pixmap, x * SCALE, (world.height() - 1 - y) * SCALE, tileColor(world.tile(x, y)));
            }
        }
        PixmapIO.writePng(file, pixmap);
        pixmap.dispose();
    }

    private static int tileColor(@Nullable TileInfo info) {
        if (info == null) {
            return COLOR_FLOOR;
        }
        if (info.solid) {
            return info.wallOre != null ? PlanSchematics.oreColor(info.wallOre) : COLOR_WALL;
        }
        return info.floorOre != null ? PlanSchematics.oreColor(info.floorOre) : COLOR_FLOOR;
    }

    private static void fill(Pixmap pixmap, int x, int y, int color) {
        for (int py = y; py < y + SCALE; py++) {
            for (int px = x; px < x + SCALE; px++) {
                pixmap.set(px, py, color);
            }
        }
    }
}