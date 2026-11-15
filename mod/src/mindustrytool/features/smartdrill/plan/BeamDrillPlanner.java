package mindustrytool.features.smartdrill.plan;

import arc.struct.Seq;

public final class BeamDrillPlanner {

    private static final int LANE_NONE = 0;
    private static final int LANE_TARGET = 1;
    private static final int LANE_OTHER = 2;

    private static final String DUCT = "duct";
    private static final String BEAM_NODE = "beam-node";

    private BeamDrillPlanner() {
    }

    public static DrillPlanResult plan(DrillPlanRequest request, DrillWorldView world) {
        BlockSpec drill = world.block(request.drill);
        if (drill == null || !drill.beamDrill) {
            return DrillPlanResult.EMPTY;
        }

        Rect rect = request.rect;
        int reach = drill.range + drill.size + 1;
        Rect scan = new Rect(rect.x - reach, rect.y - reach, rect.width + reach * 2, rect.height + reach * 2);

        boolean[] occupied = new boolean[world.width() * world.height()];
        boolean[] covered = new boolean[world.width() * world.height()];
        Seq<PlannedBuild> builds = new Seq<>();
        int coveredOre = 0;

        int backRotation = (request.facing + 2) % 4;
        int backX = PlacementRules.directionX(backRotation);
        int backY = PlacementRules.directionY(backRotation);

        for (int y = scan.y; y <= scan.top(); y++) {
            for (int x = scan.x; x <= scan.right(); x++) {
                if (!isUnitFree(world, drill, occupied, x, y, backX, backY)) {
                    continue;
                }
                if (!lanesResolveToTarget(world, drill, rect, request, x, y)) {
                    continue;
                }
                if (!world.canPlace(drill.name, x, y, request.facing)) {
                    continue;
                }

                int ductX = x + backX;
                int ductY = y + backY;
                int nodeX = x + backX * 2;
                int nodeY = y + backY * 2;
                if (!world.canPlace(DUCT, ductX, ductY, backRotation)) {
                    continue;
                }
                if (!world.canPlace(BEAM_NODE, nodeX, nodeY, request.facing)) {
                    continue;
                }

                markOccupied(world, occupied, x, y, drill.size, drill.sizeOffset);
                markOccupied(world, occupied, ductX, ductY, 1, 0);
                markOccupied(world, occupied, nodeX, nodeY, 1, 0);

                coveredOre += markCovered(world, drill, rect, request, x, y, covered);

                builds.add(new PlannedBuild(drill.name, x, y, request.facing, null,
                        PlannedBuild.Role.BEAM_DRILL));
                builds.add(new PlannedBuild(DUCT, ductX, ductY, backRotation, null, PlannedBuild.Role.DUCT));
                builds.add(new PlannedBuild(BEAM_NODE, nodeX, nodeY, request.facing, null, PlannedBuild.Role.NODE));
            }
        }

        int drillCount = builds.count(build -> build.role == PlannedBuild.Role.BEAM_DRILL);
        return new DrillPlanResult(builds, drillCount, coveredOre, 0, builds.size);
    }

    private static boolean isUnitFree(DrillWorldView world, BlockSpec drill, boolean[] occupied, int x, int y,
            int backX, int backY) {
        for (int dx = 0; dx < drill.size; dx++) {
            for (int dy = 0; dy < drill.size; dy++) {
                int index = FloorDrillPlanner.index(world, x + dx + drill.sizeOffset, y + dy + drill.sizeOffset);
                if (index < 0 || occupied[index]) {
                    return false;
                }
            }
        }
        return FloorDrillPlanner.index(world, x + backX, y + backY) >= 0
                && FloorDrillPlanner.index(world, x + backX * 2, y + backY * 2) >= 0;
    }

    private static boolean lanesResolveToTarget(DrillWorldView world, BlockSpec drill, Rect rect,
            DrillPlanRequest request, int x, int y) {
        for (int lane = 0; lane < drill.size; lane++) {
            int result = resolveLane(world, drill, rect, request, x, y, lane);
            if (result != LANE_TARGET) {
                return false;
            }
        }
        return true;
    }

    private static int resolveLane(DrillWorldView world, BlockSpec drill, Rect rect, DrillPlanRequest request,
            int x, int y, int lane) {
        int startX = PlacementRules.laneTileX(drill, x, y, request.facing, lane);
        int startY = PlacementRules.laneTileY(drill, x, y, request.facing, lane);
        int dx = PlacementRules.directionX(request.facing);
        int dy = PlacementRules.directionY(request.facing);

        for (int step = 0; step < drill.range; step++) {
            int tx = startX + dx * step;
            int ty = startY + dy * step;
            TileInfo info = world.tile(tx, ty);
            if (info == null) {
                return LANE_NONE;
            }
            if (!info.solid) {
                continue;
            }
            if (!rect.containsTile(tx, ty)) {
                return LANE_OTHER;
            }
            return request.targetOre.equals(info.wallOre) ? LANE_TARGET : LANE_OTHER;
        }
        return LANE_NONE;
    }

    private static int markCovered(DrillWorldView world, BlockSpec drill, Rect rect, DrillPlanRequest request,
            int x, int y, boolean[] covered) {
        int added = 0;
        for (int lane = 0; lane < drill.size; lane++) {
            int startX = PlacementRules.laneTileX(drill, x, y, request.facing, lane);
            int startY = PlacementRules.laneTileY(drill, x, y, request.facing, lane);
            int dx = PlacementRules.directionX(request.facing);
            int dy = PlacementRules.directionY(request.facing);

            for (int step = 0; step < drill.range; step++) {
                int tx = startX + dx * step;
                int ty = startY + dy * step;
                TileInfo info = world.tile(tx, ty);
                if (info == null || !info.solid) {
                    continue;
                }
                int index = FloorDrillPlanner.index(world, tx, ty);
                if (index >= 0 && !covered[index] && request.targetOre.equals(info.wallOre)) {
                    covered[index] = true;
                    added++;
                }
                break;
            }
        }
        return added;
    }

    private static void markOccupied(DrillWorldView world, boolean[] occupied, int x, int y, int size,
            int offset) {
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                int index = FloorDrillPlanner.index(world, x + dx + offset, y + dy + offset);
                if (index >= 0) {
                    occupied[index] = true;
                }
            }
        }
    }
}