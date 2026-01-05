package mindustrytool.features.gameplay.autoconveyor;

import arc.struct.Seq;
import arc.struct.IntSet;
import java.util.PriorityQueue;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.Duct;
import mindustry.world.blocks.distribution.StackConveyor;
import java.util.Comparator;

/**
 * Advanced Pathfinding with configurable algorithms and blocks.
 */
public class ConveyorPathfinder {

    public static class PathNode {
        public Tile tile;
        public PathNode parent;
        public float g;
        public float h;
        public float f;
        public Block blockType;
        public int rotation;

        public PathNode(Tile tile, PathNode parent, float g, float h, Block blockType, int rotation) {
            this.tile = tile;
            this.parent = parent;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.blockType = blockType;
            this.rotation = rotation;
        }
    }

    public Seq<PathNode> findPath(Tile start, Tile end, Block mainBlock) {
        if (start == null || end == null || mainBlock == null)
            return new Seq<>();

        PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingDouble((PathNode n) -> n.f));
        IntSet closedSet = new IntSet();

        // Start Node
        openSet.add(new PathNode(start, null, 0, heuristic(start, end), mainBlock, 0));

        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();

            if (current.tile == end) {
                return reconstructPath(current);
            }

            int packed = current.tile.pos();
            if (closedSet.contains(packed))
                continue;
            closedSet.add(packed);

            // Explore neighbors (0:right, 1:up, 2:left, 3:down)
            for (int i = 0; i < 4; i++) {
                exploreNeighbor(current, end, i, openSet, mainBlock);
            }
        }
        return new Seq<>();
    }

    private void exploreNeighbor(PathNode current, Tile end, int dir, PriorityQueue<PathNode> openSet,
            Block mainBlock) {
        // --- 1. Normal Step ---
        Tile neighbor = current.tile.nearby(dir);
        if (neighbor != null) {
            float moveCost = getCost(current, neighbor, dir, end);
            if (moveCost < Float.MAX_VALUE) {
                Block nextBlock = determineBlock(current, neighbor, mainBlock);
                float newG = current.g + moveCost;
                float newH = heuristic(neighbor, end);
                openSet.add(new PathNode(neighbor, current, newG, newH, nextBlock, dir));
            }
        }

        // --- 2. Bridge Logic ---
        // Supports Bridge Conveyor (Serpulo) and Duct Bridge (Erekir)
        boolean useBridge = AutoConveyorSettings.isUseBridge() && Blocks.itemBridge.unlockedNow();
        boolean useDuctBridge = AutoConveyorSettings.isUseDuctBridge() && Blocks.ductBridge.unlockedNow();

        // Determine potential bridge based on context or blindly try both if enabled
        if (useBridge) {
            attemptBridge(current, end, dir, openSet, Blocks.itemBridge, 4, 6.0f);
        }
        if (useDuctBridge) {
            attemptBridge(current, end, dir, openSet, Blocks.ductBridge, 4, 6.0f);
        }

        // --- 3. Phase Logic ---
        if (AutoConveyorSettings.isUsePhase() && Blocks.phaseConveyor.unlockedNow()) {
            // Range 5 to 12
            for (int dist = 5; dist <= 12; dist++) {
                Tile phaseEnd = getRelativeTile(current.tile, dir, dist);
                if (phaseEnd != null && isPassable(phaseEnd, end)) {
                    float cost = dist * 1.5f; // Cost
                    openSet.add(new PathNode(phaseEnd, current, current.g + cost, heuristic(phaseEnd, end),
                            Blocks.phaseConveyor, dir));
                }
            }
        }
    }

    private void attemptBridge(PathNode current, Tile end, int dir, PriorityQueue<PathNode> openSet, Block block,
            int range, float cost) {
        Tile bridgeEnd = getRelativeTile(current.tile, dir, range);
        if (bridgeEnd != null && isPassable(bridgeEnd, end)) {
            openSet.add(new PathNode(bridgeEnd, current, current.g + cost, heuristic(bridgeEnd, end), block, dir));
        }
    }

    private Tile getRelativeTile(Tile start, int dir, int dist) {
        int dx = 0, dy = 0;
        if (dir == 0)
            dx = 1;
        else if (dir == 2)
            dx = -1;
        if (dir == 1)
            dy = 1;
        else if (dir == 3)
            dy = -1;
        return Vars.world.tile(start.x + dx * dist, start.y + dy * dist);
    }

    private boolean isPassable(Tile tile, Tile end) {
        if (tile == end)
            return true;
        if (tile.block() == Blocks.air)
            return true;
        if (tile.block() instanceof Conveyor || tile.block() instanceof Duct)
            return true;
        return false;
    }

    private float getCost(PathNode current, Tile target, int dir, Tile end) {
        float cost = 1.0f;
        AutoConveyorSettings.Algorithm algo = AutoConveyorSettings.getAlgorithm();

        // 1. Turn Penalty
        if (current.parent != null) {
            int prevDir = calculateDirection(current.parent.tile, current.tile);
            if (prevDir != -1 && prevDir != dir) {
                // Beautiful: Huge penalty for turns
                if (algo == AutoConveyorSettings.Algorithm.BEAUTIFUL)
                    cost += 5.0f;
                // Fast: Small penalty
                else if (algo == AutoConveyorSettings.Algorithm.FAST)
                    cost += 0.1f;
                // Scientific: Moderate
                else
                    cost += 0.5f;
            }
        }

        // 2. Obstacle Check
        if (target.block() != Blocks.air) {
            if (target == end)
                return cost;
            if (target.block() instanceof Conveyor || target.block() instanceof Duct)
                return cost + 2.0f;

            // Destructive
            if (AutoConveyorSettings.isDestructive() && target.build != null
                    && target.build.team == Vars.player.team()) {
                if (target.block().size <= 2)
                    return cost + 20.0f;
            }
            return Float.MAX_VALUE;
        }

        // 3. Terrain
        if (target.floor().isLiquid && !target.block().isFloor())
            return Float.MAX_VALUE;

        return cost;
    }

    private float heuristic(Tile a, Tile b) {
        float manhattan = Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
        AutoConveyorSettings.Algorithm algo = AutoConveyorSettings.getAlgorithm();

        if (algo == AutoConveyorSettings.Algorithm.FAST)
            return manhattan * 1.5f;
        return manhattan;
    }

    private Block determineBlock(PathNode current, Tile target, Block mainBlock) {
        // If crossing a line, use Junction if enabled
        boolean connected = target.block() instanceof Conveyor || target.block() instanceof Duct
                || target.block() instanceof StackConveyor;
        if (connected && AutoConveyorSettings.isUseJunction()) {
            return Blocks.junction;
        }
        return mainBlock;
    }

    private Seq<PathNode> reconstructPath(PathNode endNode) {
        Seq<PathNode> path = new Seq<>();
        PathNode current = endNode;
        while (current != null) {
            path.add(current);
            current = current.parent;
        }
        path.reverse();

        // Post-processing
        for (int i = 0; i < path.size - 1; i++) {
            PathNode curr = path.get(i);
            PathNode next = path.get(i + 1);
            int dir = calculateDirection(curr.tile, next.tile);
            curr.rotation = dir;

            int dist = Math.abs(next.tile.x - curr.tile.x) + Math.abs(next.tile.y - curr.tile.y);

            // Handle Jumps (Bridge/Phase)
            if (dist > 1) {
                if (next.blockType == Blocks.phaseConveyor) {
                    curr.blockType = Blocks.phaseConveyor;
                } else if (next.blockType == Blocks.itemBridge || next.blockType == Blocks.ductBridge) {
                    curr.blockType = next.blockType;
                }
                next.rotation = dir;
            }

            // Junction placement refinment
            if (curr.blockType instanceof Conveyor || curr.blockType instanceof Duct) {
                if (curr.tile.build != null && AutoConveyorSettings.isUseJunction()) {
                    int existingRot = curr.tile.build.rotation;
                    if ((dir - existingRot) % 2 != 0) {
                        curr.blockType = Blocks.junction;
                    }
                }
            }
        }
        return path;
    }

    private int calculateDirection(Tile from, Tile to) {
        if (to.x > from.x)
            return 0;
        if (to.x < from.x)
            return 2;
        if (to.y > from.y)
            return 1;
        if (to.y < from.y)
            return 3;
        return 0;
    }
}
