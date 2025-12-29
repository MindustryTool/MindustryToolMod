package mindustrytool.plugins.autodrill;

import arc.Core;
import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.struct.ObjectIntMap;
import arc.struct.Queue;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.blocks.production.Drill;

/**
 * Utility functions for resource mining automation.
 */
public class DrillUtil {

    /**
     * Get tiles adjacent to a block at position (x, y).
     */
    public static Seq<Tile> getNearbyTiles(int x, int y, Block block) {
        return getNearbyTiles(x, y, block.size);
    }

    /**
     * Get tiles adjacent to a block of given size at position (x, y).
     */
    public static Seq<Tile> getNearbyTiles(int x, int y, int size) {
        Seq<Tile> nearbyTiles = new Seq<>();

        Point2[] nearby = Edges.getEdges(size);
        for (Point2 point2 : nearby) {
            Tile t = Vars.world.tile(x + point2.x, y + point2.y);
            if (t != null) nearbyTiles.add(t);
        }

        return nearbyTiles;
    }

    /**
     * Get tiles adjacent considering two different block sizes.
     */
    public static Seq<Tile> getNearbyTiles(int x, int y, int size1, int size2) {
        int offset1 = (size1 % 2 == 1 && size2 % 2 == 0) ? 1 : 0;
        int offset2 = ((size2 * 2 - 1) / 2);

        return getNearbyTiles(x - offset1, y - offset1, size1 + offset2);
    }

    /**
     * Count ore under a drill placement.
     * Returns the primary item and count at that tile.
     */
    public static ObjectIntMap.Entry<Item> countOre(Tile tile, Drill drill) {
        ObjectIntMap<Item> oreCount = new ObjectIntMap<>();
        Seq<Item> itemArray = new Seq<>();

        for (Tile other : tile.getLinkedTilesAs(drill, new Seq<>())) {
            if (drill.canMine(other)) {
                oreCount.increment(drill.getDrop(other), 0, 1);
            }
        }

        for (Item i : oreCount.keys()) {
            itemArray.add(i);
        }

        itemArray.sort((item1, item2) -> {
            int type = Boolean.compare(!item1.lowPriority, !item2.lowPriority);
            if (type != 0) return type;
            int amounts = Integer.compare(oreCount.get(item1, 0), oreCount.get(item2, 0));
            if (amounts != 0) return amounts;
            return Integer.compare(item1.id, item2.id);
        });

        if (itemArray.size == 0) {
            return null;
        }

        Item item = itemArray.peek();
        int count = oreCount.get(itemArray.peek(), 0);

        ObjectIntMap.Entry<Item> itemAndCount = new ObjectIntMap.Entry<>();
        itemAndCount.key = item;
        itemAndCount.value = count;

        return itemAndCount;
    }

    /**
     * Expand tile area by radius to account for drill coverage.
     */
    public static void expandArea(Seq<Tile> tiles, int radius) {
        Seq<Tile> expandedTiles = new Seq<>();

        for (Tile tile : tiles) {
            for (int dx = -radius; dx < radius; dx++) {
                for (int dy = -radius; dy < radius; dy++) {
                    if (dx == 0 && dy == 0) continue;

                    Tile nearby = tile.nearby(dx, dy);
                    if (nearby == null) continue;

                    if (!tiles.contains(nearby) && !expandedTiles.contains(nearby)) {
                        expandedTiles.add(nearby);
                    }
                }
            }
        }

        tiles.add(expandedTiles);
    }

    /**
     * Get all connected tiles with the same resource using flood-fill.
     */
    public static Seq<Tile> getConnectedTiles(Tile tile, int maxTiles) {
        Queue<Tile> queue = new Queue<>();
        Seq<Tile> tiles = new Seq<>();
        Seq<Tile> visited = new Seq<>();

        queue.addLast(tile);

        Item sourceItem = tile.drop();

        while (!queue.isEmpty() && tiles.size < maxTiles) {
            Tile currentTile = queue.removeFirst();

            Block wallBlock = Blocks.copperWall.environmentBuildable() ? Blocks.copperWall : Blocks.berylliumWall;
            if (!Build.validPlace(wallBlock, Vars.player.team(), currentTile.x, currentTile.y, 0) || visited.contains(currentTile))
                continue;

            if (currentTile.drop() == sourceItem) {
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        if (!(x == 0 && y == 0)) {
                            Tile neighbor = currentTile.nearby(x, y);
                            if (neighbor == null) continue;

                            if (!visited.contains(neighbor)) {
                                queue.addLast(neighbor);
                            }
                        }
                    }
                }

                tiles.add(currentTile);
            }

            visited.add(currentTile);
        }

        tiles.sort(Tile::pos);

        return tiles;
    }

    /**
     * Get rectangle bounds for a block placement.
     */
    public static Rect getBlockRect(Tile tile, Block block) {
        int offset = (block.size - 1) / 2;
        return new Rect(tile.x - offset, tile.y - offset, block.size, block.size);
    }

    /**
     * Convert tile to Point2.
     */
    public static Point2 tileToPoint2(Tile tile) {
        return new Point2(tile.x, tile.y);
    }
}
