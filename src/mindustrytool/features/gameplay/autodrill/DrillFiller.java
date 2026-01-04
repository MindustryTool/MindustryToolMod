package mindustrytool.features.gameplay.autodrill;

import arc.Core;
import arc.math.geom.Point2;
import arc.struct.ObjectIntMap;
import arc.struct.Queue;
import arc.struct.Seq;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;
import mindustry.world.meta.Attribute;

/**
 * Core drill placement algorithms for resource mining automation.
 * Supports Bridge Drill (Serpulo) and Wall Drill (Erekir).
 */
public class DrillFiller {

    /**
     * Fill resource patch with size-2 drills (Mechanical/Pneumatic) and bridge
     * layout.
     */
    public static Seq<BuildPlan> fillBridgeDrill(Tile tile, Drill drill, Direction direction) {
        Seq<BuildPlan> plans = new Seq<>();

        if (drill.size != 2)
            return plans;

        String maxTilesKey = SmartDrillManager.SETTING_MECH_MAX_TILES;
        String minOresKey = SmartDrillManager.SETTING_MECH_MIN_ORES;
        int defaultMax = 200;
        int defaultMin = 1;

        if (drill == Blocks.pneumaticDrill) {
            maxTilesKey = SmartDrillManager.SETTING_PNEU_MAX_TILES;
            minOresKey = SmartDrillManager.SETTING_PNEU_MIN_ORES;
            defaultMax = 150;
            defaultMin = 2;
        }

        int maxTiles = Core.settings.getInt(maxTilesKey, defaultMax);
        int minOresSetting = Core.settings.getInt(minOresKey, defaultMin);

        // Fix: Cap minOres to max possible tiles for this drill (avoid user setting > 4
        // for size 2)
        int minOres = Math.min(minOresSetting, drill.size * drill.size);

        Seq<Tile> tiles = DrillUtil.getConnectedTiles(tile, maxTiles);
        DrillUtil.expandArea(tiles, drill.size / 2);

        Point2 directionConfig = new Point2(direction.p.x * 3, direction.p.y * 3);

        Seq<Tile> drillTiles = tiles.select(DrillFiller::isBridgeDrillTile);
        Seq<Tile> bridgeTiles = tiles.select(DrillFiller::isBridgeTile);

        drillTiles.retainAll(t -> {
            ObjectIntMap.Entry<Item> itemAndCount = DrillUtil.countOre(t, drill);

            if (itemAndCount == null || itemAndCount.key != tile.drop() || itemAndCount.value < minOres) {
                return false;
            }

            Seq<Tile> neighbors = DrillUtil.getNearbyTiles(t.x, t.y, drill);
            neighbors.retainAll(DrillFiller::isBridgeTile);

            for (Tile neighbor : neighbors) {
                if (bridgeTiles.contains(neighbor))
                    return true;
            }

            neighbors.retainAll(n -> {
                BuildPlan buildPlan = new BuildPlan(n.x, n.y, 0, Blocks.itemBridge);
                return buildPlan.placeable(Vars.player.team());
            });

            if (!neighbors.isEmpty()) {
                bridgeTiles.add(neighbors);
                return true;
            }

            return false;
        });

        Tile outerMost = bridgeTiles.max((t) -> direction.p.x == 0 ? t.y * direction.p.y : t.x * direction.p.x);
        if (outerMost == null)
            return plans;

        Tile outlet = outerMost.nearby(directionConfig);
        bridgeTiles.add(outlet);

        bridgeTiles.sort(t -> t.dst2(outlet.worldx(), outlet.worldy()));

        for (Tile drillTile : drillTiles) {
            plans.add(new BuildPlan(drillTile.x, drillTile.y, 0, drill));
        }

        for (Tile bridgeTile : bridgeTiles) {
            Tile neighbor = bridgeTiles.find(t -> Math.abs(t.x - bridgeTile.x) + Math.abs(t.y - bridgeTile.y) == 3);

            Point2 config = new Point2();
            if (bridgeTile != outlet && neighbor != null) {
                config = new Point2(neighbor.x - bridgeTile.x, neighbor.y - bridgeTile.y);
            }

            plans.add(new BuildPlan(bridgeTile.x, bridgeTile.y, 0, Blocks.itemBridge, config));
        }

        return plans;
    }

    /**
     * Fill wall resources with beam drills (Erekir).
     * Full implementation with duct routing and beam node placement.
     */
    public static Seq<BuildPlan> fillWallDrill(Tile tile, Block drill, Direction outputDirection) {
        // AutoDrill uses "Facing Direction", but we use "Output Direction".
        // Convert Output -> Facing.
        Direction direction = Direction.getOpposite(outputDirection);

        Seq<BuildPlan> plans = new Seq<>();

        String maxTilesKey = SmartDrillManager.SETTING_CLIFF_MAX_TILES;
        int defaultMax = 100;

        if (drill == Blocks.plasmaBore) {
            maxTilesKey = SmartDrillManager.SETTING_PLASMA_MAX_TILES;
        }

        int maxTiles = Core.settings.getInt(maxTilesKey, defaultMax);

        Seq<Tile> tiles = getConnectedWallTiles(tile, direction, maxTiles);
        if (tiles.isEmpty())
            return plans;

        Seq<Tile> boreTiles = new Seq<>();
        Seq<Integer> occupiedSecondaryAxis = new Seq<>();

        Direction directionOpposite = Direction.getOpposite(direction);
        Point2 offset = getDirectionOffset(direction, drill);
        Point2 offsetOpposite = getDirectionOffset(directionOpposite, drill);

        int range = 1; // Default for WallCrafter (adjacent)
        if (drill instanceof BeamDrill) {
            range = (int) ((BeamDrill) drill).range;
        }

        for (Tile tile1 : tiles) {
            for (int i = 0; i < range; i++) {
                // boreTile is placed away from the wall (in the -facing direction)
                Tile boreTile = tile1.nearby((int) ((i + 1) * -direction.p.x + offset.x),
                        (int) ((i + 1) * -direction.p.y + offset.y));
                if (boreTile == null)
                    continue;

                // Use direction.r for rotation (facing the wall)
                BuildPlan buildPlan = new BuildPlan(boreTile.x, boreTile.y, direction.r, drill);
                if (buildPlan.placeable(Vars.player.team())) {
                    int sa = direction.secondaryAxis(new Point2(boreTile.x, boreTile.y));

                    boolean occupied = false;
                    for (int j = -(drill.size - 1) / 2; j <= drill.size / 2; j++) {
                        if (occupiedSecondaryAxis.contains(sa + j)) {
                            occupied = true;
                            break;
                        }
                    }
                    if (occupied)
                        continue;

                    for (int j = -(drill.size - 1) / 2; j <= drill.size / 2; j++) {
                        occupiedSecondaryAxis.add(sa + j);
                    }

                    boreTiles.add(boreTile);
                    break;
                }
            }
        }
        if (boreTiles.isEmpty())
            return plans;

        // Place ducts for output
        Seq<Tile> ductTiles = new Seq<>();
        for (Tile boreTile : boreTiles) {
            for (int i = -(drill.size - 1) / 2; i <= drill.size / 2; i++) {
                // Place ducts on the output side of the drill
                Tile ductTile = boreTile.nearby(new Point2(
                        -offsetOpposite.x + directionOpposite.p.x + (i * Math.abs(direction.p.y)),
                        -offsetOpposite.y + directionOpposite.p.y + (i * Math.abs(direction.p.x))));
                if (ductTile == null)
                    continue;
                ductTiles.add(ductTile);
            }
        }

        if (!ductTiles.isEmpty()) {
            Tile outerMostDuctTile = ductTiles
                    .select(t -> boreTiles.find(bt -> direction.secondaryAxis(new Point2(bt.x, bt.y)) == direction
                            .secondaryAxis(new Point2(t.x, t.y))) == null)
                    .max(t -> -direction.primaryAxis(new Point2(t.x, t.y)));

            if (outerMostDuctTile != null) {
                ductTiles.sort(t -> t.dst2(outerMostDuctTile));
                Seq<Tile> connectingTiles = new Seq<>();
                connectingTiles.add(outerMostDuctTile);

                for (Tile ductTile : ductTiles) {
                    if (connectingTiles.contains(ductTile))
                        continue;

                    Tile closestDuctTile = connectingTiles.min(t -> t.dst2(ductTile));
                    if (closestDuctTile == null)
                        continue;

                    Point2 currentPoint = new Point2(ductTile.x, ductTile.y);
                    int paGoal = direction.primaryAxis(new Point2(closestDuctTile.x, closestDuctTile.y));
                    int saGoal = direction.secondaryAxis(new Point2(closestDuctTile.x, closestDuctTile.y));

                    while (currentPoint.x != closestDuctTile.x || currentPoint.y != closestDuctTile.y) {
                        int pa = direction.primaryAxis(currentPoint);
                        int sa = direction.secondaryAxis(currentPoint);

                        Tile currentTile = Vars.world.tile(currentPoint.x, currentPoint.y);
                        if (currentTile != null && !connectingTiles.contains(currentTile)) {
                            connectingTiles.add(currentTile);
                        }

                        if ((pa < paGoal && sa == saGoal) || pa > paGoal) {
                            if (Math.abs(pa) < Math.abs(paGoal))
                                currentPoint.add(Math.abs(direction.p.x), Math.abs(direction.p.y));
                            else
                                currentPoint.add(-Math.abs(direction.p.x), -Math.abs(direction.p.y));
                        } else {
                            if (Math.abs(sa) < Math.abs(saGoal))
                                currentPoint.add(Math.abs(direction.p.y), Math.abs(direction.p.x));
                            else
                                currentPoint.add(-Math.abs(direction.p.y), -Math.abs(direction.p.x));
                        }
                    }
                }

                // Place ducts
                connectingTiles.sort(t -> outerMostDuctTile.dst(t));
                Seq<Tile> visitedTiles = new Seq<>();
                visitedTiles.add(outerMostDuctTile);

                while (!connectingTiles.isEmpty()) {
                    Tile tile1 = null, tile2 = null;
                    for (Tile connectingTile : connectingTiles) {
                        Tile adjacent = visitedTiles.find(t -> connectingTile.relativeTo(t) != -1);
                        if (adjacent != null) {
                            tile1 = adjacent;
                            tile2 = connectingTile;
                            visitedTiles.add(connectingTile);
                            connectingTiles.remove(connectingTile);
                            break;
                        }
                    }
                    if (tile1 == null || tile2 == null) {
                        if (!connectingTiles.isEmpty())
                            connectingTiles.remove(0);
                        continue;
                    }

                    if (tile2.equals(outerMostDuctTile)) {
                        plans.add(new BuildPlan(tile2.x, tile2.y, directionOpposite.r, Blocks.duct));
                        plans.add(new BuildPlan(tile2.x + directionOpposite.p.x, tile2.y + directionOpposite.p.y,
                                directionOpposite.r, Blocks.duct));
                    } else {
                        plans.add(new BuildPlan(tile2.x, tile2.y, tile2.relativeTo(tile1), Blocks.duct));
                    }
                }
            }
        }

        // Place beam nodes for power
        if (drill.consumesPower) {
            Tile outerMost = boreTiles.max(t -> -direction.primaryAxis(new Point2(t.x, t.y)));
            for (Tile boreTile : boreTiles) {
                Tile beamNodeTile = Vars.world.tile(
                        Math.abs(direction.p.x) * outerMost.x + Math.abs(direction.p.y) * boreTile.x - offsetOpposite.x
                                + directionOpposite.p.x * 2,
                        Math.abs(direction.p.y) * outerMost.y + Math.abs(direction.p.x) * boreTile.y - offsetOpposite.y
                                + directionOpposite.p.y * 2);
                if (beamNodeTile != null) {
                    plans.add(new BuildPlan(beamNodeTile.x, beamNodeTile.y, 0, Blocks.beamNode));
                    while (beamNodeTile.dst(boreTile) > 10 * Vars.tilesize) {
                        beamNodeTile = beamNodeTile.nearby(direction.p.x * 5, direction.p.y * 5);
                        if (beamNodeTile == null)
                            break;
                        plans.add(new BuildPlan(beamNodeTile.x, beamNodeTile.y, 0, Blocks.beamNode));
                    }
                }
            }
        }

        // Place drills
        for (Tile boreTile : boreTiles) {
            plans.add(new BuildPlan(boreTile.x, boreTile.y, direction.r, drill));
        }

        return plans;
    }

    private static boolean isBridgeDrillTile(Tile tile) {
        short x = tile.x;
        short y = tile.y;
        switch (x % 6) {
            case 0:
            case 2:
                return (y - 1) % 6 == 0;
            case 1:
                return (y - 3) % 6 == 0 || (y - 3) % 6 == 2;
            case 3:
            case 5:
                return (y - 4) % 6 == 0;
            case 4:
                return (y) % 6 == 0 || (y) % 6 == 2;
        }
        return false;
    }

    private static boolean isBridgeTile(Tile tile) {
        return tile.x % 3 == 0 && tile.y % 3 == 0;
    }

    private static Seq<Tile> getConnectedWallTiles(Tile tile, Direction direction, int maxTiles) {
        Queue<Tile> queue = new Queue<>();
        Seq<Tile> tiles = new Seq<>();
        Seq<Tile> visited = new Seq<>();

        queue.addLast(tile);

        Item sourceItem = tile.wallDrop();
        boolean isSandAttribute = tile.block().attributes.get(Attribute.sand) > 0;

        if (sourceItem == null && !isSandAttribute)
            return tiles;

        while (!queue.isEmpty() && tiles.size < maxTiles) {
            Tile currentTile = queue.removeFirst();
            if (visited.contains(currentTile))
                continue;

            boolean match = false;
            if (sourceItem != null) {
                match = currentTile.wallDrop() == sourceItem;
            } else if (isSandAttribute) {
                match = currentTile.block().attributes.get(Attribute.sand) > 0;
            }

            if (match) {
                for (int x = -2; x <= 2; x++) {
                    for (int y = -2; y <= 2; y++) {
                        if (x == 0 && y == 0)
                            continue;
                        Tile neighbor = currentTile.nearby(x, y);
                        if (neighbor == null)
                            continue;

                        // Check if there's open space on the output side of the wall
                        Tile nearby = neighbor.nearby(new Point2(-direction.p.x, -direction.p.y));
                        if (!visited.contains(neighbor) && nearby != null && !nearby.solid()) {
                            queue.addLast(neighbor);
                        }
                    }
                }
                tiles.add(currentTile);
            }
            visited.add(currentTile);
        }

        // Filter to keep only the outermost wall tiles (closest to output side)
        Seq<Tile> tilesCopy = tiles.copy();
        tiles.retainAll(t1 -> {
            Point2 pT1 = DrillUtil.tileToPoint2(t1);
            int paT1 = direction.primaryAxis(pT1);
            int saT1 = direction.secondaryAxis(pT1);
            return !tilesCopy.contains(t2 -> {
                Point2 pT2 = DrillUtil.tileToPoint2(t2);
                return t2 != t1 && direction.secondaryAxis(pT2) == saT1 && direction.primaryAxis(pT2) < paT1;
            });
        });

        tiles.sort(t -> direction.secondaryAxis(DrillUtil.tileToPoint2(t)));
        return tiles;
    }

    private static Point2 getDirectionOffset(Direction direction, Block block) {
        int offset1 = (block.size - 1) / 2;
        int offset2 = block.size / 2;
        switch (direction) {
            case RIGHT:
                return new Point2(-offset2, 0);
            case UP:
                return new Point2(0, -offset2);
            case LEFT:
                return new Point2(offset1, 0);
            default:
                return new Point2(0, offset1);
        }
    }
}
