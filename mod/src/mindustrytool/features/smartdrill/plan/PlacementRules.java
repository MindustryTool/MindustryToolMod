package mindustrytool.features.smartdrill.plan;

public final class PlacementRules {

    private PlacementRules() {
    }

    public static boolean isAnchor(int x, int y) {
        int mx = Math.floorMod(x, 6);
        int my = Math.floorMod(y, 6);
        switch (mx) {
            case 0:
            case 2:
                return my == 1;
            case 1:
                return my == 3 || my == 5;
            case 3:
            case 5:
                return my == 4;
            case 4:
                return my == 0 || my == 2;
            default:
                return false;
        }
    }

    public static boolean isBridgeNode(int x, int y) {
        return Math.floorMod(x, 3) == 0 && Math.floorMod(y, 3) == 0;
    }

    public static boolean footprintIsPureTarget(DrillWorldView world, BlockSpec drill, int x, int y,
            String target) {
        int found = 0;
        for (int dx = 0; dx < drill.size; dx++) {
            for (int dy = 0; dy < drill.size; dy++) {
                TileInfo info = world.tile(x + dx + drill.sizeOffset, y + dy + drill.sizeOffset);
                if (info == null) {
                    return false;
                }
                String ore = info.floorOre;
                if (ore == null) {
                    continue;
                }
                if (!ore.equals(target)) {
                    return false;
                }
                found++;
            }
        }
        return found > 0;
    }

    public static int countTargetOre(DrillWorldView world, BlockSpec drill, int x, int y, String target) {
        int found = 0;
        for (int dx = 0; dx < drill.size; dx++) {
            for (int dy = 0; dy < drill.size; dy++) {
                TileInfo info = world.tile(x + dx + drill.sizeOffset, y + dy + drill.sizeOffset);
                if (info != null && target.equals(info.floorOre)) {
                    found++;
                }
            }
        }
        return found;
    }

    public static boolean footprintInRect(Rect rect, BlockSpec drill, int x, int y) {
        return rect.containsFootprint(x, y, drill.size, drill.sizeOffset);
    }

    public static boolean canPlaceDrill(DrillWorldView world, BlockSpec drill, Rect rect, int x, int y,
            int rotation, String target) {
        if (!footprintInRect(rect, drill, x, y)) {
            return false;
        }
        if (!footprintIsPureTarget(world, drill, x, y, target)) {
            return false;
        }
        return world.canPlace(drill.name, x, y, rotation);
    }

    public static int laneTileX(BlockSpec drill, int x, int y, int rotation, int lane) {
        int cornerX = x - (drill.size - 1) / 2;
        int cornerY = y - (drill.size - 1) / 2;
        int size = drill.size;
        switch (rotation) {
            case 0:
                return cornerX + size;
            case 1:
                return cornerX + lane;
            case 2:
                return cornerX - 1;
            case 3:
                return cornerX + lane;
            default:
                return x;
        }
    }

    public static int laneTileY(BlockSpec drill, int x, int y, int rotation, int lane) {
        int cornerX = x - (drill.size - 1) / 2;
        int cornerY = y - (drill.size - 1) / 2;
        int size = drill.size;
        switch (rotation) {
            case 0:
                return cornerY + lane;
            case 1:
                return cornerY + size;
            case 2:
                return cornerY + lane;
            case 3:
                return cornerY - 1;
            default:
                return y;
        }
    }

    public static int directionX(int rotation) {
        switch (rotation) {
            case 0:
                return 1;
            case 2:
                return -1;
            default:
                return 0;
        }
    }

    public static int directionY(int rotation) {
        switch (rotation) {
            case 1:
                return 1;
            case 3:
                return -1;
            default:
                return 0;
        }
    }
}