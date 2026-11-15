package mindustrytool.features.smartdrill.plan;

import arc.struct.Seq;

public final class FloorDrillPlanner {

    private static final class Candidate {
        int x;
        int y;
        int score;

        Candidate(int x, int y, int score) {
            this.x = x;
            this.y = y;
            this.score = score;
        }
    }

    private FloorDrillPlanner() {
    }

    public static DrillPlanResult plan(DrillPlanRequest request, DrillWorldView world) {
        BlockSpec drill = world.block(request.drill);
        if (drill == null || !drill.drill) {
            return DrillPlanResult.EMPTY;
        }

        State state = new State(world, request.rect, request.targetOre, drill);
        if (drill.size == 2) {
            placeLattice(state, request.facing);
        } else {
            placeGreedy(state, request.facing);
        }
        return state.toResult();
    }

    private static void placeLattice(State state, int facing) {
        Rect rect = state.rect;
        for (int y = rect.y; y <= rect.top(); y++) {
            for (int x = rect.x; x <= rect.right(); x++) {
                if (!PlacementRules.isAnchor(x, y)) {
                    continue;
                }
                state.tryPlace(x, y, facing);
            }
        }
    }

    private static void placeGreedy(State state, int facing) {
        Seq<Candidate> candidates = new Seq<>();
        Rect rect = state.rect;
        for (int y = rect.y; y <= rect.top(); y++) {
            for (int x = rect.x; x <= rect.right(); x++) {
                int score = PlacementRules.countTargetOre(state.world, state.drill, x, y, state.target);
                if (score > 0) {
                    candidates.add(new Candidate(x, y, score));
                }
            }
        }

        candidates.sort((a, b) -> {
            int byScore = Integer.compare(b.score, a.score);
            if (byScore != 0) {
                return byScore;
            }
            int byY = Integer.compare(a.y, b.y);
            return byY != 0 ? byY : Integer.compare(a.x, b.x);
        });

        for (int i = 0; i < candidates.size; i++) {
            Candidate candidate = candidates.get(i);
            state.tryPlace(candidate.x, candidate.y, facing);
        }
    }

    private static final class State {
        final DrillWorldView world;
        final Rect rect;
        final String target;
        final BlockSpec drill;
        final boolean[] occupied;
        final boolean[] covered;
        final Seq<PlannedBuild> builds = new Seq<>();
        int coveredOre;

        State(DrillWorldView world, Rect rect, String target, BlockSpec drill) {
            this.world = world;
            this.rect = rect;
            this.target = target;
            this.drill = drill;
            this.occupied = new boolean[world.width() * world.height()];
            this.covered = new boolean[world.width() * world.height()];
        }

        boolean tryPlace(int x, int y, int facing) {
            if (!PlacementRules.canPlaceDrill(world, drill, rect, x, y, facing, target)) {
                return false;
            }
            for (int dx = 0; dx < drill.size; dx++) {
                for (int dy = 0; dy < drill.size; dy++) {
                    int index = index(world, x + dx + drill.sizeOffset, y + dy + drill.sizeOffset);
                    if (index < 0 || occupied[index]) {
                        return false;
                    }
                }
            }
            for (int dx = 0; dx < drill.size; dx++) {
                for (int dy = 0; dy < drill.size; dy++) {
                    int index = index(world, x + dx + drill.sizeOffset, y + dy + drill.sizeOffset);
                    if (index >= 0) {
                        occupied[index] = true;
                    }
                }
            }
            for (int dx = 0; dx < drill.size; dx++) {
                for (int dy = 0; dy < drill.size; dy++) {
                    int tx = x + dx + drill.sizeOffset;
                    int ty = y + dy + drill.sizeOffset;
                    int index = index(world, tx, ty);
                    if (index < 0 || covered[index]) {
                        continue;
                    }
                    TileInfo info = world.tile(tx, ty);
                    if (info != null && target.equals(info.floorOre)) {
                        covered[index] = true;
                        coveredOre++;
                    }
                }
            }
            builds.add(new PlannedBuild(drill.name, x, y, facing, null, PlannedBuild.Role.DRILL));
            return true;
        }

        DrillPlanResult toResult() {
            return new DrillPlanResult(builds, builds.size, coveredOre, 0, builds.size);
        }
    }

    static int index(DrillWorldView world, int x, int y) {
        if (x < 0 || y < 0 || x >= world.width() || y >= world.height()) {
            return -1;
        }
        return y * world.width() + x;
    }
}