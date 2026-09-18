package mindustrytool.features.pathfinding;

import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.struct.IntFloatMap;
import arc.struct.IntIntMap;
import arc.struct.IntQueue;
import arc.struct.IntSeq;
import arc.struct.IntSet;
import arc.struct.LongMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Time;
import java.util.Arrays;
import java.util.PriorityQueue;
import mindustry.Vars;
import mindustry.ai.Pathfinder;
import mindustry.ai.Pathfinder.PathCost;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.Tile;
import mindustry.world.meta.BlockFlag;

public class ClientPathfinder {

    public static class ClientFlowfield {
        public final Team team;
        public final int costType;
        public int[] weights = new int[0];
        public short[] searches = new short[0];
        public int search;
        public float lastUpdateTime;
        public boolean valid;

        public ClientFlowfield(Team team, int costType) {
            this.team = team;
            this.costType = costType;
        }
    }

    private static class AStarNode implements Comparable<AStarNode> {
        final int pos;
        final float gScore;
        final float fScore;

        AStarNode(int pos, float gScore, float fScore) {
            this.pos = pos;
            this.gScore = gScore;
            this.fScore = fScore;
        }

        @Override
        public int compareTo(AStarNode o) {
            return Float.compare(this.fScore, o.fScore);
        }
    }

    private static final float FLOWFIELD_REFRESH_INTERVAL = 60f;
    private static final int MAX_ASTAR_ITERATIONS = 600;

    private final LongMap<ClientFlowfield> flowfields = new LongMap<>();
    private final IntQueue frontier = new IntQueue();

    // Reusable structures for A* to minimize allocations
    private final IntFloatMap gScores = new IntFloatMap();
    private final IntIntMap cameFrom = new IntIntMap();
    private final IntSet closed = new IntSet();
    private final PriorityQueue<AStarNode> open = new PriorityQueue<>();
    private final IntSeq pathNodes = new IntSeq();

    public void clear() {
        flowfields.clear();
        frontier.clear();
        gScores.clear();
        cameFrom.clear();
        closed.clear();
        open.clear();
        pathNodes.clear();
    }

    public @Nullable ClientFlowfield getOrUpdateFlowfield(Team team, int costType) {
        if (Vars.world == null || Vars.state == null) {
            return null;
        }

        long key = (((long) costType) << 32) | (long) team.id;
        ClientFlowfield field = flowfields.get(key);
        if (field == null) {
            field = new ClientFlowfield(team, costType);
            flowfields.put(key, field);
        }

        float currentTime = Time.time;
        if (!field.valid || (currentTime - field.lastUpdateTime) > FLOWFIELD_REFRESH_INTERVAL) {
            updateFlowfield(field);
        }

        return field.valid ? field : null;
    }

    private void updateFlowfield(ClientFlowfield field) {
        int width = Vars.world.width();
        int height = Vars.world.height();
        int totalTiles = width * height;

        if (field.weights.length != totalTiles) {
            field.weights = new int[totalTiles];
            field.searches = new short[totalTiles];
            field.search = 0;
        }

        field.search++;
        if (field.search >= Short.MAX_VALUE) {
            Arrays.fill(field.searches, (short) 0);
            field.search = 1;
        }

        frontier.clear();

        // Targets: enemy cores
        Seq<Building> cores = Vars.indexer.getEnemy(field.team, BlockFlag.core);
        if (cores != null) {
            for (int i = 0; i < cores.size; i++) {
                Building core = cores.get(i);
                if (core != null && core.tile != null) {
                    addCoreTarget(field, core.tile, width);
                }
            }
        }

        // Also add spawn points if this team is the wave enemy team
        if (Vars.state.rules.waves && field.team == Vars.state.rules.defaultTeam && Vars.spawner != null) {
            Seq<Tile> spawns = Vars.spawner.getSpawns();
            if (spawns != null) {
                for (int i = 0; i < spawns.size; i++) {
                    Tile spawn = spawns.get(i);
                    if (spawn != null) {
                        int pos = spawn.x + spawn.y * width;
                        if (pos >= 0 && pos < totalTiles) {
                            field.weights[pos] = 0;
                            field.searches[pos] = (short) field.search;
                            frontier.addFirst(pos);
                        }
                    }
                }
            }
        }

        if (frontier.size == 0) {
            field.valid = false;
            return;
        }

        PathCost pathCost = getPathCost(field.costType);

        while (frontier.size > 0) {
            int pos = frontier.removeLast();
            int x = pos % width;
            int y = pos / width;
            int currentWeight = field.weights[pos];

            for (Point2 p : Geometry.d4) {
                int nx = x + p.x;
                int ny = y + p.y;
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }

                int npos = nx + ny * width;
                if (field.searches[npos] == field.search) {
                    continue;
                }

                int tileData = Vars.pathfinder.get(nx, ny);
                int cost = pathCost.getCost(field.team.id, tileData);
                if (cost == -1) {
                    continue;
                }
                if (field.costType == Pathfinder.costNaval && cost >= 6000) {
                    continue;
                }

                field.weights[npos] = currentWeight + cost;
                field.searches[npos] = (short) field.search;
                frontier.addFirst(npos);
            }
        }

        field.valid = true;
        field.lastUpdateTime = Time.time;
    }

    private void addCoreTarget(ClientFlowfield field, Tile centerTile, int width) {
        int pos = centerTile.x + centerTile.y * width;
        if (pos >= 0 && pos < field.weights.length) {
            field.weights[pos] = 0;
            field.searches[pos] = (short) field.search;
            frontier.addFirst(pos);
        }
    }

    public @Nullable Tile getTargetTile(Tile tile, @Nullable ClientFlowfield field) {
        if (tile == null || field == null || !field.valid || field.weights == null) {
            return null;
        }

        int width = Vars.world.width();
        int height = Vars.world.height();
        int pos = tile.x + tile.y * width;

        if (pos < 0 || pos >= field.weights.length || field.searches[pos] != field.search) {
            return null;
        }

        int currentWeight = field.weights[pos];
        if (currentWeight == 0) {
            return null;
        }

        Tile bestTile = null;
        int minWeight = currentWeight;

        for (Point2 p : Geometry.d8) {
            int nx = tile.x + p.x;
            int ny = tile.y + p.y;
            if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                continue;
            }

            int npos = nx + ny * width;
            if (field.searches[npos] != field.search) {
                continue;
            }

            int weight = field.weights[npos];
            if (weight < minWeight) {
                if (p.x != 0 && p.y != 0) {
                    int p1 = (tile.x + p.x) + tile.y * width;
                    int p2 = tile.x + (tile.y + p.y) * width;
                    if (!isPassable(field, p1) || !isPassable(field, p2)) {
                        continue;
                    }
                }
                minWeight = weight;
                bestTile = Vars.world.tile(nx, ny);
            }
        }

        return bestTile;
    }

    private boolean isPassable(ClientFlowfield field, int pos) {
        if (pos < 0 || pos >= field.weights.length) {
            return false;
        }
        return field.searches[pos] == field.search;
    }

    public void findCommandedPath(Unit unit, Vec2 targetPos, int maxSteps, PathfindingCache cacheEntry) {
        Tile start = unit.tileOn();
        Tile end = Vars.world.tileWorld(targetPos.x, targetPos.y);

        if (start == null || end == null || start == end || unit.isFlying()) {
            ensureCapacity(cacheEntry, 4);
            cacheEntry.data[0] = unit.x;
            cacheEntry.data[1] = unit.y;
            cacheEntry.data[2] = targetPos.x;
            cacheEntry.data[3] = targetPos.y;
            cacheEntry.size = 4;
            return;
        }

        int width = Vars.world.width();
        int height = Vars.world.height();
        int startPos = start.x + start.y * width;
        int endPos = end.x + end.y * width;

        int costType = unit.type.flowfieldPathType;
        PathCost pathCost = getPathCost(costType);

        gScores.clear();
        cameFrom.clear();
        closed.clear();
        open.clear();

        float startH = Mathf.dst(start.x, start.y, end.x, end.y);
        open.add(new AStarNode(startPos, 0f, startH));
        gScores.put(startPos, 0f);

        int bestPos = startPos;
        float bestDist = startH;
        int iterations = 0;
        boolean reached = false;

        while (!open.isEmpty() && iterations++ < MAX_ASTAR_ITERATIONS) {
            AStarNode current = open.poll();
            if (current == null) {
                break;
            }

            if (current.pos == endPos) {
                reached = true;
                bestPos = endPos;
                break;
            }

            if (closed.contains(current.pos)) {
                continue;
            }
            closed.add(current.pos);

            int cx = current.pos % width;
            int cy = current.pos / width;
            float currentDist = Mathf.dst(cx, cy, end.x, end.y);
            if (currentDist < bestDist) {
                bestDist = currentDist;
                bestPos = current.pos;
            }

            for (Point2 p : Geometry.d8) {
                int nx = cx + p.x;
                int ny = cy + p.y;
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }

                int npos = nx + ny * width;
                if (closed.contains(npos)) {
                    continue;
                }

                int tileData = Vars.pathfinder.get(nx, ny);
                int cost = pathCost.getCost(unit.team.id, tileData);
                if (cost == -1) {
                    continue;
                }
                if (costType == Pathfinder.costNaval && cost >= 6000) {
                    continue;
                }

                if (p.x != 0 && p.y != 0) {
                    int p1Data = Vars.pathfinder.get(cx + p.x, cy);
                    int p2Data = Vars.pathfinder.get(cx, cy + p.y);
                    if (pathCost.getCost(unit.team.id, p1Data) == -1 || pathCost.getCost(unit.team.id, p2Data) == -1) {
                        continue;
                    }
                }

                float stepDist = (p.x != 0 && p.y != 0) ? 1.414f : 1.0f;
                float tentativeG = current.gScore + stepDist * (1f + cost * 0.05f);

                if (tentativeG < gScores.get(npos, Float.MAX_VALUE)) {
                    cameFrom.put(npos, current.pos);
                    gScores.put(npos, tentativeG);
                    float h = Mathf.dst(nx, ny, end.x, end.y);
                    open.add(new AStarNode(npos, tentativeG, tentativeG + h));
                }
            }
        }

        pathNodes.clear();
        int curr = bestPos;
        pathNodes.add(curr);
        while (curr != startPos && cameFrom.containsKey(curr)) {
            curr = cameFrom.get(curr);
            pathNodes.add(curr);
        }

        int pointCount = pathNodes.size;
        int requiredFloats = (pointCount + 1) * 2;
        ensureCapacity(cacheEntry, requiredFloats);

        int idx = 0;
        cacheEntry.data[idx++] = unit.x;
        cacheEntry.data[idx++] = unit.y;

        for (int i = pointCount - 1; i >= 0; i--) {
            int node = pathNodes.get(i);
            int nx = node % width;
            int ny = node / width;
            cacheEntry.data[idx++] = nx * Vars.tilesize + Vars.tilesize / 2f;
            cacheEntry.data[idx++] = ny * Vars.tilesize + Vars.tilesize / 2f;
            if (idx >= maxSteps * 2) {
                break;
            }
        }

        if (reached && idx < cacheEntry.data.length - 1) {
            cacheEntry.data[idx++] = targetPos.x;
            cacheEntry.data[idx++] = targetPos.y;
        }

        cacheEntry.size = idx;
    }

    private void ensureCapacity(PathfindingCache cacheEntry, int requiredCapacity) {
        if (cacheEntry.data.length < requiredCapacity) {
            cacheEntry.data = Arrays.copyOf(cacheEntry.data, Math.max(cacheEntry.data.length * 2, requiredCapacity));
        }
    }

    private PathCost getPathCost(int costType) {
        return (costType >= 0 && costType < Pathfinder.costTypes.size)
                ? Pathfinder.costTypes.get(costType)
                : Pathfinder.costTypes.get(0);
    }
}
