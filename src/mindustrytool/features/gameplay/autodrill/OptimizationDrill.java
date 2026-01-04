package mindustrytool.features.gameplay.autodrill;

import arc.Core;
import arc.math.geom.Rect;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.type.Item;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.production.Drill;

/**
 * Ported optimization logic for future use.
 * Allows recursive search for optimal drill placement.
 */
public class OptimizationDrill {

    // Default settings if not present in Core.settings

    private static final String SETTING_QUALITY = "smart-drill.opt.quality";

    public static Seq<BuildPlan> getOptimizedPlan(Tile tile, Drill drill) {
        return getOptimizedPlan(tile, drill, true);
    }

    public static Seq<BuildPlan> getOptimizedPlan(Tile tile, Drill drill, boolean waterExtractorsAndPowerNodes) {
        Seq<BuildPlan> plans = new Seq<>();

        // Use mechanical drill settings as base or default
        int maxTiles = Core.settings.getInt(SmartDrillManager.SETTING_MECH_MAX_TILES, 200);

        Seq<Tile> tiles = DrillUtil.getConnectedTiles(tile, maxTiles);
        DrillUtil.expandArea(tiles, drill.size / 2);

        // Minimum ores - default to 1 if not set
        int minOresPerDrill = 1;

        Floor floor = tile.overlay() != Blocks.air ? tile.overlay() : tile.floor();

        ObjectMap<Tile, ObjectIntMap.Entry<Item>> tilesItemAndCount = new ObjectMap<>();
        for (Tile t : tiles) {
            tilesItemAndCount.put(t, DrillUtil.countOre(t, drill));
        }

        tiles.retainAll(t -> {
            ObjectIntMap.Entry<Item> itemAndCount = tilesItemAndCount.get(t);
            return itemAndCount != null && itemAndCount.key == floor.itemDrop && itemAndCount.value >= minOresPerDrill;
        }).sort(t -> {
            ObjectIntMap.Entry<Item> itemAndCount = tilesItemAndCount.get(t);
            return itemAndCount == null ? Integer.MIN_VALUE : -itemAndCount.value;
        });

        Seq<Tile> selection = new Seq<>();
        int qualityMult = Core.settings.getInt(SETTING_QUALITY, 2);
        int maxTries = qualityMult * 1000;

        recursiveMaxSearch(tiles, drill, tilesItemAndCount, selection, new Seq<>(), 0, new Seq<>(), maxTries, 0);

        // Optional: Water and Power
        if (waterExtractorsAndPowerNodes) {
            // Logic can be enabled via flag or future setting
            // placeWaterExtractorsAndPowerNodes(selection, drill, plans);
            // For now, we just add the drills to kept it simple for the port
        }

        for (Tile t : selection) {
            plans.add(new BuildPlan(t.x, t.y, 0, drill));
        }

        return plans;
    }

    private static int recursiveMaxSearch(Seq<Tile> tiles, Drill drill,
            ObjectMap<Tile, ObjectIntMap.Entry<Item>> tilesItemAndCount, Seq<Tile> selection, Seq<Rect> rects, int sum,
            Seq<Integer> triesPerLevel, final int maxTries, final int level) {
        int max = sum;
        Seq<Tile> maxSelection = selection.copy();

        if (triesPerLevel.size < level + 1) {
            triesPerLevel.setSize(level + 1);
            triesPerLevel.set(level, 0);
        }

        for (Tile tile : tiles) {
            Rect rect = DrillUtil.getBlockRect(tile, drill);

            if ((rects.isEmpty() || rects.find(r -> r.overlaps(rect)) == null)
                    && Build.validPlace(drill, Vars.player.team(), tile.x, tile.y, 0)) {
                int newSum = sum + tilesItemAndCount.get(tile).value;

                Seq<Tile> newSelection = selection.copy().add(tile);
                Seq<Rect> newRects = rects.copy().add(rect);

                int newMax = recursiveMaxSearch(tiles, drill, tilesItemAndCount, newSelection, newRects, newSum,
                        triesPerLevel, maxTries, level + 1);

                if (newMax > max) {
                    max = newMax;
                    maxSelection = newSelection.copy();
                }

                triesPerLevel.set(level, triesPerLevel.get(level) + 1);
                if (triesPerLevel.get(level) >= maxTries / Math.pow(2, level + 1))
                    break;
            }
        }

        selection.clear();
        selection.addAll(maxSelection);

        return max;
    }
}
