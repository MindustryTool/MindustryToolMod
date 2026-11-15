package mindustrytool.features.smartdrill.plan;

import arc.math.geom.Point2;
import arc.struct.Seq;

public final class SchematicPlanner {

    private static final String AIR = "air";

    private SchematicPlanner() {
    }

    public static DrillPlanResult plan(DrillPlanRequest request, DrillWorldView world) {
        SchematicData schematic = request.schematic;
        BlockSpec drillSpec = world.block(request.drill);
        if (schematic == null || drillSpec == null || !drillSpec.drill) {
            return DrillPlanResult.EMPTY;
        }

        Seq<SchematicData.Tile> drillTiles = new Seq<>();
        for (int i = 0; i < schematic.tiles.size; i++) {
            SchematicData.Tile tile = schematic.tiles.get(i);
            if (request.drill.equals(tile.block)) {
                drillTiles.add(tile);
            }
        }
        if (drillTiles.isEmpty()) {
            return DrillPlanResult.EMPTY;
        }

        DrillPlanResult best = DrillPlanResult.EMPTY;
        int bestCovered = -1;
        int bestBlocks = Integer.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;

        for (int rotation = 0; rotation < 4; rotation++) {
            int[] size = rotatedSize(schematic.width, schematic.height, rotation);
            int minOx = Integer.MIN_VALUE;
            int maxOx = Integer.MAX_VALUE;
            int minOy = Integer.MIN_VALUE;
            int maxOy = Integer.MAX_VALUE;

            for (int i = 0; i < drillTiles.size; i++) {
                int[] local = rotate(drillTiles.get(i).x, drillTiles.get(i).y, schematic.width, schematic.height,
                        rotation);
                int offset = drillSpec.sizeOffset;
                minOx = Math.max(minOx, request.rect.x - local[0] - offset);
                maxOx = Math.min(maxOx, request.rect.right() - local[0] - drillSpec.size + 1 - offset);
                minOy = Math.max(minOy, request.rect.y - local[1] - offset);
                maxOy = Math.min(maxOy, request.rect.top() - local[1] - drillSpec.size + 1 - offset);
            }

            for (int oy = minOy; oy <= maxOy; oy++) {
                for (int ox = minOx; ox <= maxOx; ox++) {
                    if (!fits(world, request, drillSpec, drillTiles, schematic, rotation, ox, oy, size)) {
                        continue;
                    }
                    int covered = countCovered(world, request, drillSpec, drillTiles, schematic, rotation, ox, oy);
                    int blocks = schematic.tiles.size;
                    int distance = Math.abs(ox + size[0] / 2 - request.rect.centerX())
                            + Math.abs(oy + size[1] / 2 - request.rect.centerY());

                    if (covered > bestCovered
                            || (covered == bestCovered && blocks < bestBlocks)
                            || (covered == bestCovered && blocks == bestBlocks && distance < bestDistance)) {
                        bestCovered = covered;
                        bestBlocks = blocks;
                        bestDistance = distance;
                        best = build(world, request, drillSpec, drillTiles, schematic, rotation, ox, oy, covered);
                    }
                }
            }
        }

        return best;
    }

    private static boolean fits(DrillWorldView world, DrillPlanRequest request, BlockSpec drillSpec,
            Seq<SchematicData.Tile> drillTiles, SchematicData schematic, int rotation, int ox, int oy, int[] size) {
        if (ox < 0 || oy < 0 || ox + size[0] > world.width() || oy + size[1] > world.height()) {
            return false;
        }
        for (int i = 0; i < schematic.tiles.size; i++) {
            SchematicData.Tile tile = schematic.tiles.get(i);
            if (AIR.equals(tile.block)) {
                continue;
            }
            int[] local = rotate(tile.x, tile.y, schematic.width, schematic.height, rotation);
            int wx = ox + local[0];
            int wy = oy + local[1];
            int tileRotation = Math.floorMod(tile.rotation + rotation, 4);
            if (!world.canPlace(tile.block, wx, wy, tileRotation)) {
                return false;
            }
        }

        for (int i = 0; i < drillTiles.size; i++) {
            int[] local = rotate(drillTiles.get(i).x, drillTiles.get(i).y, schematic.width, schematic.height,
                    rotation);
            int wx = ox + local[0];
            int wy = oy + local[1];
            if (!PlacementRules.footprintInRect(request.rect, drillSpec, wx, wy)) {
                return false;
            }
            if (!PlacementRules.footprintIsPureTarget(world, drillSpec, wx, wy, request.targetOre)) {
                return false;
            }
        }
        return true;
    }

    private static int countCovered(DrillWorldView world, DrillPlanRequest request, BlockSpec drillSpec,
            Seq<SchematicData.Tile> drillTiles, SchematicData schematic, int rotation, int ox, int oy) {
        boolean[] seen = new boolean[world.width() * world.height()];
        int covered = 0;
        for (int i = 0; i < drillTiles.size; i++) {
            int[] local = rotate(drillTiles.get(i).x, drillTiles.get(i).y, schematic.width, schematic.height,
                    rotation);
            int wx = ox + local[0];
            int wy = oy + local[1];
            for (int dx = 0; dx < drillSpec.size; dx++) {
                for (int dy = 0; dy < drillSpec.size; dy++) {
                    int tx = wx + dx + drillSpec.sizeOffset;
                    int ty = wy + dy + drillSpec.sizeOffset;
                    int index = FloorDrillPlanner.index(world, tx, ty);
                    if (index < 0 || seen[index]) {
                        continue;
                    }
                    TileInfo info = world.tile(tx, ty);
                    if (info != null && request.targetOre.equals(info.floorOre)) {
                        seen[index] = true;
                        covered++;
                    }
                }
            }
        }
        return covered;
    }

    private static DrillPlanResult build(DrillWorldView world, DrillPlanRequest request, BlockSpec drillSpec,
            Seq<SchematicData.Tile> drillTiles, SchematicData schematic, int rotation, int ox, int oy,
            int covered) {
        Seq<PlannedBuild> builds = new Seq<>();
        for (int i = 0; i < schematic.tiles.size; i++) {
            SchematicData.Tile tile = schematic.tiles.get(i);
            if (AIR.equals(tile.block)) {
                continue;
            }
            int[] local = rotate(tile.x, tile.y, schematic.width, schematic.height, rotation);
            int wx = ox + local[0];
            int wy = oy + local[1];
            int tileRotation = Math.floorMod(tile.rotation + rotation, 4);
            PlannedBuild.Role role = request.drill.equals(tile.block)
                    ? (drillSpec.beamDrill ? PlannedBuild.Role.BEAM_DRILL : PlannedBuild.Role.DRILL)
                    : PlannedBuild.Role.RIG;
            builds.add(new PlannedBuild(tile.block, wx, wy, tileRotation,
                    rotateConfig(tile.config, rotation), role));
        }
        int drillCount = 0;
        for (int i = 0; i < builds.size; i++) {
            if (builds.get(i).role == PlannedBuild.Role.DRILL
                    || builds.get(i).role == PlannedBuild.Role.BEAM_DRILL) {
                drillCount++;
            }
        }
        return new DrillPlanResult(builds, drillCount, covered, 0, builds.size);
    }

    private static int[] rotatedSize(int width, int height, int rotation) {
        boolean swap = rotation == 1 || rotation == 3;
        return swap ? new int[] {height, width} : new int[] {width, height};
    }

    private static int[] rotate(int x, int y, int width, int height, int rotation) {
        switch (rotation) {
            case 1:
                return new int[] {height - 1 - y, x};
            case 2:
                return new int[] {width - 1 - x, height - 1 - y};
            case 3:
                return new int[] {y, width - 1 - x};
            default:
                return new int[] {x, y};
        }
    }

    private static Object rotateConfig(Object config, int rotation) {
        if (config == null || rotation == 0) {
            return config;
        }
        if (config instanceof Point2) {
            return rotateDelta((Point2) config, rotation);
        }
        if (config instanceof Point2[]) {
            Point2[] source = (Point2[]) config;
            Point2[] result = new Point2[source.length];
            for (int i = 0; i < source.length; i++) {
                result[i] = rotateDelta(source[i], rotation);
            }
            return result;
        }
        return config;
    }

    private static Point2 rotateDelta(Point2 source, int rotation) {
        int dx = (int) source.x;
        int dy = (int) source.y;
        switch (rotation) {
            case 1:
                return new Point2(-dy, dx);
            case 2:
                return new Point2(-dx, -dy);
            case 3:
                return new Point2(dy, -dx);
            default:
                return new Point2(dx, dy);
        }
    }
}