package mindustrytool.features.smartdrill.plan;

import arc.math.geom.Point2;

public final class BridgePlanner {

    private static final String ITEM_BRIDGE = "item-bridge";
    private static final String DUCT = "duct";
    private static final int MARGIN = 12;

    private BridgePlanner() {
    }

    public static DrillPlanResult attach(DrillPlanResult result, DrillWorldView world, DrillPlanRequest request) {
        if (result.drillCount <= 0) {
            return result;
        }

        Rect area = new Rect(request.rect.x - MARGIN, request.rect.y - MARGIN,
                request.rect.width + MARGIN * 2, request.rect.height + MARGIN * 2);
        int minGridX = Math.floorDiv(area.x, 3);
        int minGridY = Math.floorDiv(area.y, 3);
        int maxGridX = Math.floorDiv(area.right(), 3);
        int maxGridY = Math.floorDiv(area.top(), 3);
        int gw = maxGridX - minGridX + 1;
        int gh = maxGridY - minGridY + 1;
        if (gw <= 0 || gh <= 0) {
            return result;
        }

        boolean[] node = new boolean[gw * gh];
        for (int gy = 0; gy < gh; gy++) {
            for (int gx = 0; gx < gw; gx++) {
                int wx = (minGridX + gx) * 3;
                int wy = (minGridY + gy) * 3;
                node[gy * gw + gx] = world.tile(wx, wy) != null && world.canPlace(ITEM_BRIDGE, wx, wy, 0);
            }
        }

        boolean[] required = new boolean[gw * gh];
        for (int i = 0; i < result.builds.size; i++) {
            PlannedBuild build = result.builds.get(i);
            if (build.role != PlannedBuild.Role.DRILL) {
                continue;
            }
            markAdjacentNodes(world, node, required, minGridX, minGridY, gw, gh, build);
        }

        int root = findRoot(node, minGridX, minGridY, gw, gh, request.facing);
        if (root < 0) {
            return result;
        }

        int[] parent = new int[gw * gh];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = -2;
        }
        parent[root] = -1;

        int[] queue = new int[gw * gh];
        int head = 0;
        int tail = 0;
        queue[tail++] = root;
        while (head < tail) {
            int current = queue[head++];
            int gx = current % gw;
            int gy = current / gw;
            tail = visitNeighbor(node, parent, queue, gw, gh, gx + 1, gy, current, tail);
            tail = visitNeighbor(node, parent, queue, gw, gh, gx - 1, gy, current, tail);
            tail = visitNeighbor(node, parent, queue, gw, gh, gx, gy + 1, current, tail);
            tail = visitNeighbor(node, parent, queue, gw, gh, gx, gy - 1, current, tail);
        }

        boolean[] needed = new boolean[gw * gh];
        needed[root] = true;
        for (int i = 0; i < required.length; i++) {
            if (!required[i]) {
                continue;
            }
            int cursor = i;
            int guard = 0;
            while (cursor >= 0 && guard++ < needed.length) {
                needed[cursor] = true;
                cursor = parent[cursor];
            }
        }

        prune(node, needed, required, gw, gh);

        int rootX = (minGridX + root % gw) * 3;
        int rootY = (minGridY + root / gw) * 3;
        int stubX = rootX + PlacementRules.directionX(request.facing);
        int stubY = rootY + PlacementRules.directionY(request.facing);
        boolean stubPlaced = world.canPlace(DUCT, stubX, stubY, request.facing);

        int bridges = 0;
        for (int i = 0; i < needed.length; i++) {
            if (!needed[i] || i == root) {
                continue;
            }
            int wx = (minGridX + i % gw) * 3;
            int wy = (minGridY + i / gw) * 3;
            int p = parent[i];
            Point2 config = p >= 0
                    ? new Point2((minGridX + p % gw) * 3 - wx, (minGridY + p / gw) * 3 - wy)
                    : null;
            result.builds.add(new PlannedBuild(ITEM_BRIDGE, wx, wy, 0, config, PlannedBuild.Role.BRIDGE));
            bridges++;
        }

        if (stubPlaced) {
            Point2 rootConfig = new Point2(stubX - rootX, stubY - rootY);
            result.builds.add(new PlannedBuild(ITEM_BRIDGE, rootX, rootY, 0, rootConfig,
                    PlannedBuild.Role.BRIDGE));
            result.builds.add(new PlannedBuild(DUCT, stubX, stubY, request.facing, null,
                    PlannedBuild.Role.DUCT));
            bridges++;
        }

        return new DrillPlanResult(result.builds, result.drillCount, result.coveredOre, bridges,
                result.builds.size);
    }

    private static int visitNeighbor(boolean[] node, int[] parent, int[] queue, int gw, int gh, int gx, int gy,
            int current, int tail) {
        if (gx < 0 || gy < 0 || gx >= gw || gy >= gh) {
            return tail;
        }
        int index = gy * gw + gx;
        if (!node[index] || parent[index] != -2) {
            return tail;
        }
        parent[index] = current;
        queue[tail] = index;
        return tail + 1;
    }

    private static void markAdjacentNodes(DrillWorldView world, boolean[] node, boolean[] required, int minGridX,
            int minGridY, int gw, int gh, PlannedBuild drill) {
        BlockSpec spec = world.block(drill.block);
        if (spec == null) {
            return;
        }
        for (int dx = 0; dx < spec.size; dx++) {
            for (int dy = 0; dy < spec.size; dy++) {
                int tx = drill.x + dx + spec.sizeOffset;
                int ty = drill.y + dy + spec.sizeOffset;
                markNode(node, required, minGridX, minGridY, gw, gh, tx + 1, ty);
                markNode(node, required, minGridX, minGridY, gw, gh, tx - 1, ty);
                markNode(node, required, minGridX, minGridY, gw, gh, tx, ty + 1);
                markNode(node, required, minGridX, minGridY, gw, gh, tx, ty - 1);
            }
        }
    }

    private static void markNode(boolean[] node, boolean[] required, int minGridX, int minGridY, int gw, int gh,
            int wx, int wy) {
        if (Math.floorMod(wx, 3) != 0 || Math.floorMod(wy, 3) != 0) {
            return;
        }
        int gx = Math.floorDiv(wx, 3) - minGridX;
        int gy = Math.floorDiv(wy, 3) - minGridY;
        if (gx < 0 || gy < 0 || gx >= gw || gy >= gh) {
            return;
        }
        int index = gy * gw + gx;
        if (node[index]) {
            required[index] = true;
        }
    }

    private static int findRoot(boolean[] node, int minGridX, int minGridY, int gw, int gh, int facing) {
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        int dx = PlacementRules.directionX(facing);
        int dy = PlacementRules.directionY(facing);
        for (int gy = 0; gy < gh; gy++) {
            for (int gx = 0; gx < gw; gx++) {
                int index = gy * gw + gx;
                if (!node[index]) {
                    continue;
                }
                int wx = (minGridX + gx) * 3;
                int wy = (minGridY + gy) * 3;
                int score = wx * dx + wy * dy;
                if (score > bestScore) {
                    bestScore = score;
                    best = index;
                }
            }
        }
        return best;
    }

    private static void prune(boolean[] node, boolean[] needed, boolean[] required, int gw, int gh) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int gy = 0; gy < gh; gy++) {
                for (int gx = 0; gx < gw; gx++) {
                    int index = gy * gw + gx;
                    if (!needed[index] || required[index]) {
                        continue;
                    }
                    if (degree(node, needed, gw, gh, gx, gy) <= 1) {
                        needed[index] = false;
                        changed = true;
                    }
                }
            }
        }
    }

    private static int degree(boolean[] node, boolean[] needed, int gw, int gh, int gx, int gy) {
        int count = 0;
        count += neighbor(node, needed, gw, gh, gx + 1, gy) ? 1 : 0;
        count += neighbor(node, needed, gw, gh, gx - 1, gy) ? 1 : 0;
        count += neighbor(node, needed, gw, gh, gx, gy + 1) ? 1 : 0;
        count += neighbor(node, needed, gw, gh, gx, gy - 1) ? 1 : 0;
        return count;
    }

    private static boolean neighbor(boolean[] node, boolean[] needed, int gw, int gh, int gx, int gy) {
        if (gx < 0 || gy < 0 || gx >= gw || gy >= gh) {
            return false;
        }
        int index = gy * gw + gx;
        return node[index] && needed[index];
    }
}